package de.ellpeck.craftabledeeds.ui.items;

import de.ellpeck.craftabledeeds.CraftableDeeds;
import de.ellpeck.craftabledeeds.DeedStorage;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData;

import java.util.List;

public class EmptyDeedItem extends Item {

    public EmptyDeedItem() {
        super(new Properties().maxStackSize(1).group(ItemGroup.MISC).isImmuneToFire());
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack held = playerIn.getHeldItem(handIn);

        List<? extends String> dims = CraftableDeeds.allowedDimensions.get();
        if (!dims.contains("*") && !dims.contains(worldIn.getDimensionKey().getLocation().toString())) {
            playerIn.sendStatusMessage(new TranslationTextComponent("info." + CraftableDeeds.ID + ".disallowed_dimension"), true);
            return ActionResult.resultFail(held);
        }

        // if there is already a claim here, don't let us overwrite it
        DeedStorage storage = DeedStorage.get(worldIn);
        DeedStorage.Claim existing = storage.getClaim(playerIn.getPosX(), 64, playerIn.getPosZ());
        if (existing != null) {
            if (existing.cooldown <= 0 || !existing.owner.equals(playerIn.getUniqueID())) {
                if (!worldIn.isRemote)
                    playerIn.sendStatusMessage(new TranslationTextComponent("info." + CraftableDeeds.ID + ".already_claimed", existing.getOwnerName()), true);
                return ActionResult.resultFail(held);
            } else {
                // if we're in cooldown mode and we're the owner, remove the cooldown deed and create a new one like usual
                if (!worldIn.isRemote)
                    storage.removeClaim(existing.mapId);
            }
        }

        if (!playerIn.abilities.isCreativeMode)
            held.shrink(1);

        ItemStack filled = new ItemStack(CraftableDeeds.FILLED_DEED.get());
        createMapData(filled, playerIn, MathHelper.floor(playerIn.getPosX()), MathHelper.floor(playerIn.getPosZ()), 0, true, false);

        playerIn.addStat(Stats.ITEM_USED.get(this));
        playerIn.playSound(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1, 1);
        playerIn.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1, 1);

        if (held.isEmpty()) {
            return ActionResult.func_233538_a_(filled, worldIn.isRemote());
        } else {
            if (!playerIn.inventory.addItemStackToInventory(filled.copy()))
                playerIn.dropItem(filled, false);
            return ActionResult.func_233538_a_(held, worldIn.isRemote());
        }
    }

    public static MapData createMapData(ItemStack stack, PlayerEntity player, int x, int z, int scale, boolean trackingPosition, boolean unlimitedTracking) {
        int id = player.world.getNextMapId();
        MapData ret = new MapData(FilledMapItem.getMapName(id));
        ret.initData(x, z, scale, trackingPosition, unlimitedTracking, player.world.getDimensionKey());
        player.world.registerMapData(ret);
        if (!player.world.isRemote)
            DeedStorage.get(player.world).addClaim(id, player);
        if (!stack.isEmpty())
            stack.getOrCreateTag().putInt("map", id);
        return ret;
    }
}
