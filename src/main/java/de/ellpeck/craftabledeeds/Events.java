package de.ellpeck.craftabledeeds;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
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
        if (!entity.level.isClientSide) {
            if (entity instanceof Player) {
                PacketHandler.sendDeeds((Player) entity);
            } else if (entity instanceof IronGolem || entity instanceof SnowGolem || entity instanceof Wolf || CraftableDeeds.additionalLoyalMobs.get().contains(entity.getType().getRegistryName().toString())) {
                Mob mob = ((Mob) entity);
                mob.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(mob, Player.class, 10, false, false, p -> {
                    if (mob instanceof TamableAnimal && !((TamableAnimal) mob).isTame())
                        return false;
                    if (isDisallowedHere(p, p.getOnPos(), s -> !s.loyalMobsAttack)) {
                        // hack that allows iron golems to attack players, bleh
                        if (mob instanceof IronGolem)
                            ((IronGolem) mob).setPlayerCreated(false);
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
            if (isExemptConfig(CraftableDeeds.breakableBlocks.get(), String.valueOf(event.getState().getBlock())))
                return;
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player && isDisallowedHere(event.getEntity(), event.getPos(), s -> s.canPlaceBreak))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (isDisallowedHere(event.getPlayer(), event.getPos(), s -> s.canOpenContainers)) {
            BlockState state = event.getWorld().getBlockState(event.getPos());
            // always allow interacting with the pedestal!
            if (state.getBlock() == CraftableDeeds.DEED_PEDESTAL_BLOCK.get())
                return;
            if (isExemptConfig(CraftableDeeds.interactableBlocks.get(), String.valueOf(state.getBlock())))
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
        if (target instanceof HangingEntity && isDisallowedHere(event.getPlayer(), target.getOnPos(), s -> s.canPlaceBreak))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMobGriefing(EntityMobGriefingEvent event) {
        Entity entity = event.getEntity();
        // endermen picking stuff up and zombies breaking down doors should be disallowed
        if (entity instanceof EnderMan || entity instanceof Zombie || entity instanceof Creeper && !CraftableDeeds.allowCreeperExplosions.get()) {
            if (isDisallowedHere(entity, entity.getOnPos(), null))
                event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Start event) {
        Explosion explosion = event.getExplosion();
        Entity exploder = explosion.getExploder();
        if (exploder != null && isDisallowedHere(exploder, new BlockPos(explosion.getPosition()), null)) {
            // creepers are handled in onMobGriefing
            if (exploder instanceof Creeper)
                return;
            if (exploder instanceof PrimedTnt && CraftableDeeds.allowTntExplosions.get())
                return;
            if ((exploder instanceof WitherBoss || exploder instanceof WitherSkull) && CraftableDeeds.allowWitherExplosions.get())
                return;

            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPiston(PistonEvent.Pre event) {
        if (event.getWorld() instanceof Level) {
            DeedStorage storage = DeedStorage.get((Level) event.getWorld());
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
        if (entity.hasPermissions(CraftableDeeds.deedBypassPermissionLevel.get()))
            return false;
        DeedStorage storage = DeedStorage.get(entity.level);
        DeedStorage.Claim claim = storage.getClaim(pos.getX(), pos.getY(), pos.getZ());
        if (claim == null || !claim.isActive())
            return false;
        // the owner can do anything in their claim (obviously)
        if (claim.owner.equals(entity.getUUID()))
            return false;
        // allow players that are whitelisted in the pedestal settings
        if (relevantSetting != null && entity instanceof Player) {
            if (!claim.playerSettings.containsKey(entity.getUUID())) {
                claim.playerSettings.put(entity.getUUID(), new DeedStorage.PlayerSettings((Player) entity));
                storage.markDirtyAndSend();
            }
            DeedStorage.PlayerSettings settings = claim.playerSettings.get(entity.getUUID());
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
