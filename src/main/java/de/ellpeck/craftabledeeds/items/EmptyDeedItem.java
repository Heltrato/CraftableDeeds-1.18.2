package de.ellpeck.craftabledeeds.items;

import de.ellpeck.craftabledeeds.CraftableDeeds;
import de.ellpeck.craftabledeeds.DeedStorage;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.List;

public class EmptyDeedItem extends Item {

    public EmptyDeedItem() {
        super(new Properties().stacksTo(1).tab(CreativeModeTab.TAB_MISC).fireResistant());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        ItemStack held = playerIn.getItemInHand(handIn);

        List<? extends String> dims = CraftableDeeds.allowedDimensions.get();
        if (!dims.contains("*") && !dims.contains(worldIn.dimension().location().toString())) {
            playerIn.displayClientMessage(new TranslatableComponent("info." + CraftableDeeds.ID + ".disallowed_dimension"), true);
            return InteractionResultHolder.fail(held);
        }

        // if there is already a claim here, don't let us overwrite it
        DeedStorage storage = DeedStorage.get(worldIn);
        DeedStorage.Claim existing = storage.getClaim(playerIn.getX(), 64, playerIn.getZ());
        if (existing != null) {
            if (existing.cooldown <= 0 || !existing.owner.equals(playerIn.getUUID())) {
                if (!worldIn.isClientSide)
                    playerIn.displayClientMessage(new TranslatableComponent("info." + CraftableDeeds.ID + ".already_claimed", existing.getOwnerName()), true);
                return InteractionResultHolder.fail(held);
            } else {
                // if we're in cooldown mode and we're the owner, remove the cooldown deed and create a new one like usual
                if (!worldIn.isClientSide)
                    storage.removeClaim(existing.mapId);
            }
        }

        if (!playerIn.getAbilities().instabuild)
            held.shrink(1);

        ItemStack filled = new ItemStack(CraftableDeeds.FILLED_DEED.get());
        createMapData(filled, playerIn, (double) Mth.floor(playerIn.getX()), (double) Mth.floor(playerIn.getZ()), (byte) 0, true, false);

        playerIn.awardStat(Stats.ITEM_USED.get(this));
        playerIn.playSound(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1, 1);
        playerIn.playSound(SoundEvents.PLAYER_LEVELUP, 1, 1);

        if (held.isEmpty()) {
            return InteractionResultHolder.sidedSuccess(filled, worldIn.isClientSide());
        } else {
            if (!playerIn.getInventory().add(filled.copy()))
                playerIn.drop(filled, false);
            return InteractionResultHolder.sidedSuccess(held, worldIn.isClientSide());
        }
    }

    public static MapItemSavedData createMapData(ItemStack stack, Player player, double x, double z, byte scale, boolean trackingPosition, boolean unlimitedTracking) {
        int id = player.level.getFreeMapId();
        MapItemSavedData ret = MapItemSavedData.createFresh(x, z, scale, trackingPosition, unlimitedTracking, player.level.dimension());
        // MapItemSavedData ret = new MapItemSavedData(MapItem.makeKey(id));
        //ret.setProperties(x, z, scale, trackingPosition, unlimitedTracking, player.world.getDimensionKey());
        player.level.setMapData(MapItem.makeKey(id), ret);
        if (!player.level.isClientSide)
            DeedStorage.get(player.level).addClaim(id, player);
        if (!stack.isEmpty())
            stack.getOrCreateTag().putInt("map", id);
        return ret;
    }
}
