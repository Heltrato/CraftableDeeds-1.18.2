package de.ellpeck.craftabledeeds;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.item.HangingEntity;
import net.minecraft.entity.item.TNTEntity;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.monster.EndermanEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.world.PistonEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.function.Function;

@Mod.EventBusSubscriber
public final class Events {

    @SubscribeEvent
    public static void onPlayerJoin(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (!entity.world.isRemote) {
            if (entity instanceof PlayerEntity) {
                PacketHandler.sendDeeds((PlayerEntity) entity);
            } else if (entity instanceof IronGolemEntity || entity instanceof SnowGolemEntity || entity instanceof WolfEntity || CraftableDeeds.additionalLoyalMobs.get().contains(entity.getType().getRegistryName().toString())) {
                MobEntity mob = ((MobEntity) entity);
                mob.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(mob, PlayerEntity.class, 10, false, false, p -> {
                    if (mob instanceof TameableEntity && !((TameableEntity) mob).isTamed())
                        return false;
                    if (isDisallowedHere(p, p.getPosition(), s -> !s.loyalMobsAttack)) {
                        // hack that allows iron golems to attack players, bleh
                        if (mob instanceof IronGolemEntity)
                            ((IronGolemEntity) mob).setPlayerCreated(false);
                        return true;
                    }
                    return false;
                }));
            }
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END)
            DeedStorage.get(event.world).update();
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (isDisallowedHere(event.getPlayer(), event.getPos(), s -> s.canPlaceBreak)) {
            if (isExemptConfig(CraftableDeeds.breakableBlocks.get(), event.getState().getBlock()))
                return;
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof PlayerEntity && isDisallowedHere(event.getEntity(), event.getPos(), s -> s.canPlaceBreak))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (isDisallowedHere(event.getPlayer(), event.getPos(), s -> s.canOpenContainers)) {
            BlockState state = event.getWorld().getBlockState(event.getPos());
            // always allow interacting with the pedestal!
            if (state.getBlock() == CraftableDeeds.DEED_PEDESTAL_BLOCK.get())
                return;
            if (isExemptConfig(CraftableDeeds.interactableBlocks.get(), state.getBlock()))
                return;

            if (!CraftableDeeds.allowOpeningBlocks.get())
                event.setUseBlock(Event.Result.DENY);
        }
        if (isDisallowedHere(event.getPlayer(), event.getPos(), s -> s.canPlaceBreak))
            event.setUseItem(Event.Result.DENY);
    }

    @SubscribeEvent
    public static void onBlockClick(PlayerInteractEvent.LeftClickBlock event) {
        if (isDisallowedHere(event.getPlayer(), event.getPos(), s -> s.canPlaceBreak))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (isDisallowedHere(event.getPlayer(), event.getPos(), s -> s.canOpenContainers))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onEntityAttack(AttackEntityEvent event) {
        Entity target = event.getTarget();
        if (target instanceof HangingEntity && isDisallowedHere(event.getPlayer(), target.getPosition(), s -> s.canPlaceBreak))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMobGriefing(EntityMobGriefingEvent event) {
        Entity entity = event.getEntity();
        // endermen picking stuff up and zombies breaking down doors should be disallowed
        if (entity instanceof EndermanEntity || entity instanceof ZombieEntity || entity instanceof CreeperEntity && !CraftableDeeds.allowCreeperExplosions.get()) {
            if (isDisallowedHere(entity, entity.getPosition(), null))
                event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Start event) {
        Explosion explosion = event.getExplosion();
        Entity exploder = explosion.getExploder();
        if (exploder != null && isDisallowedHere(exploder, new BlockPos(explosion.getPosition()), null)) {
            // creepers are handled in onMobGriefing
            if (exploder instanceof CreeperEntity)
                return;
            if (exploder instanceof TNTEntity && CraftableDeeds.allowTntExplosions.get())
                return;
            if ((exploder instanceof WitherEntity || exploder instanceof WitherSkullEntity) && CraftableDeeds.allowWitherExplosions.get())
                return;

            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPiston(PistonEvent.Pre event) {
        if (event.getWorld() instanceof World) {
            DeedStorage storage = DeedStorage.get((World) event.getWorld());
            DeedStorage.Claim claim = storage.getClaim(event.getPos().getX(), event.getPos().getY(), event.getPos().getZ());
            if (claim != null && claim.isActive() && !claim.canPistonsPush)
                event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerStarting(RegisterCommandsEvent event) {
        DeedCommand.register(event.getDispatcher());
    }

    private static boolean isDisallowedHere(Entity entity, BlockPos pos, Function<DeedStorage.PlayerSettings, Boolean> relevantSetting) {
        // opped players should be ignored
        if (entity.hasPermissionLevel(CraftableDeeds.deedBypassPermissionLevel.get()))
            return false;
        DeedStorage storage = DeedStorage.get(entity.world);
        DeedStorage.Claim claim = storage.getClaim(pos.getX(), pos.getY(), pos.getZ());
        if (claim == null || !claim.isActive())
            return false;
        // the owner can do anything in their claim (obviously)
        if (claim.owner.equals(entity.getUniqueID()))
            return false;
        // allow players that are whitelisted in the pedestal settings
        if (relevantSetting != null && entity instanceof PlayerEntity) {
            if (!claim.playerSettings.containsKey(entity.getUniqueID())) {
                claim.playerSettings.put(entity.getUniqueID(), new DeedStorage.PlayerSettings((PlayerEntity) entity));
                storage.markDirtyAndSend();
            }
            DeedStorage.PlayerSettings settings = claim.playerSettings.get(entity.getUniqueID());
            if (settings != null && relevantSetting.apply(settings))
                return false;
        }
        // anything else should be disallowed
        return true;
    }

    private static boolean isExemptConfig(List<? extends String> config, Block block) {
        return isExemptConfig(config, block.getRegistryName().toString());
    }

    private static boolean isExemptConfig(List<? extends String> config, String search) {
        return config.stream().anyMatch(search::matches);
    }
}
