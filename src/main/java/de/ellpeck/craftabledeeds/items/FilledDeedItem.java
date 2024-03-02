package de.ellpeck.craftabledeeds.items;

import de.ellpeck.craftabledeeds.CraftableDeeds;
import de.ellpeck.craftabledeeds.DeedStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class FilledDeedItem extends MapItem {

    public FilledDeedItem() {
        super(new Properties().stacksTo(1).fireResistant());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }


    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        // delet the deed when using a grindstone
        if (state.getBlock() == Blocks.GRINDSTONE) {
            if (!context.getLevel().isClientSide) {
                DeedStorage.get(context.getLevel()).removeClaim(MapItem.getMapId(context.getItemInHand()));
                context.getPlayer().setItemInHand(context.getHand(), new ItemStack(CraftableDeeds.EMPTY_DEED.get()));
            }
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }


    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        DeedStorage.Claim claim = DeedStorage.get(worldIn).getClaim(getMapId(stack));
        if (claim == null)
            return;
        tooltip.add(new TranslatableComponent("info." + CraftableDeeds.ID + ".owner", claim.getOwnerName()));
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level worldIn, Player playerIn) {
        // no-op since super does some nbt stuff we don't need
    }


    @Nullable
    @Override
    protected MapItemSavedData getCustomMapData(ItemStack stack, Level worldIn) {
        return getSavedData(stack, worldIn);
    }
}
