package de.ellpeck.craftabledeeds.ui.items;

import de.ellpeck.craftabledeeds.CraftableDeeds;
import de.ellpeck.craftabledeeds.DeedStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class FilledDeedItem extends FilledMapItem {

    public FilledDeedItem() {
        super(new Properties().maxStackSize(1).isImmuneToFire());
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext context) {
        BlockState state = context.getWorld().getBlockState(context.getPos());
        // delet the deed when using a grindstone
        if (state.getBlock() == Blocks.GRINDSTONE) {
            if (!context.getWorld().isRemote) {
                DeedStorage.get(context.getWorld()).removeClaim(FilledMapItem.getMapId(context.getItem()));
                context.getPlayer().setHeldItem(context.getHand(), new ItemStack(CraftableDeeds.EMPTY_DEED.get()));
            }
            return ActionResultType.SUCCESS;
        }
        return super.onItemUse(context);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        DeedStorage.Claim claim = DeedStorage.get(worldIn).getClaim(getMapId(stack));
        if (claim == null)
            return;
        tooltip.add(new TranslationTextComponent("info." + CraftableDeeds.ID + ".owner", claim.getOwnerName()));
    }

    @Override
    public void onCreated(ItemStack stack, World worldIn, PlayerEntity playerIn) {
        // no-op since super does some nbt stuff we don't need
    }

    @Nullable
    @Override
    protected MapData getCustomMapData(ItemStack stack, World worldIn) {
        return getData(stack, worldIn);
    }
}
