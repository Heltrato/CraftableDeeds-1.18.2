package de.ellpeck.craftabledeeds.items;

import de.ellpeck.craftabledeeds.CraftableDeeds;
import de.ellpeck.craftabledeeds.DeedStorage;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
    public InteractionResult useOn(UseOnContext p_42885_) {
        return super.useOn(p_42885_);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        // delet the deed when using a grindstone
        if (state.getBlock() == Blocks.GRINDSTONE) {
            if (!context.getLevel().isClientSide) {
                DeedStorage.get(context.getLevel()).removeClaim(MapItem.getMapId(context.getItem()));
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
