package de.ellpeck.craftabledeeds.blocks;

import de.ellpeck.craftabledeeds.CraftableDeeds;
import de.ellpeck.craftabledeeds.DeedStorage;
import de.ellpeck.craftabledeeds.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

public class DeedPedestalBlock extends BaseEntityBlock {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);

    public DeedPedestalBlock() {
        super(Properties.copy(Blocks.STONE_BRICKS).strength(5, 1200));
    }

    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        BlockEntity tile = worldIn.getBlockEntity(pos);
        if (!(tile instanceof DeedPedestalTileEntity))
            return InteractionResult.FAIL;
        DeedPedestalTileEntity pedestal = (DeedPedestalTileEntity) tile;
        ItemStackHandler items = pedestal.items;
        ItemStack contained = items.getStackInSlot(0);
        ItemStack hand = player.getItemInHand(handIn);
        if (contained.isEmpty()) {
            // putting a deed in
            if (hand.getItem() == CraftableDeeds.FILLED_DEED.get()) {
                if (!worldIn.isClientSide) {
                    items.setStackInSlot(0, hand);
                    player.setItemInHand(handIn, ItemStack.EMPTY);
                    PacketHandler.sendTileEntityToClients(pedestal);
                }
                return InteractionResult.SUCCESS;
            }
        } else {
            DeedStorage.Claim claim = pedestal.getClaim();

            // opening the management ui
            if (claim.owner.equals(player.getUUID()) && !player.isCrouching()) {
                if (!worldIn.isClientSide)
                    NetworkHooks.openGui((ServerPlayer) player, pedestal, pos);
                return InteractionResult.SUCCESS;
            }

            // taking out the deed
            if (!worldIn.isClientSide) {
                items.setStackInSlot(0, ItemStack.EMPTY);
                PacketHandler.sendTileEntityToClients(pedestal);

                if (claim.owner.equals(player.getUUID())) {
                    if (!player.addItem(contained))
                        worldIn.addFreshEntity(new ItemEntity(worldIn, pos.getX() + 0.5F, pos.getY() + 1, pos.getZ() + 0.5F, contained));
                } else {
                    // if we're not the claim's owner, we want to put the claim in cooldown mode and explode
                    worldIn.explode(null, pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F, 2, Explosion.BlockInteraction.NONE);
                    worldIn.addFreshEntity(new ItemEntity(worldIn, pos.getX() + 0.5F, pos.getY() + 1.5F, pos.getZ() + 0.5F, new ItemStack(Items.NETHER_STAR)));
                    // cooldown is in hours, so ticks * seconds * minutes
                    claim.cooldown = CraftableDeeds.claimCooldown.get() * 20 * 60 * 60;
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    @Override
    public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity tile = worldIn.getBlockEntity(pos);
            if (tile instanceof DeedPedestalTileEntity) {
                IItemHandler handler = ((DeedPedestalTileEntity) tile).items;
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (!stack.isEmpty())
                        worldIn.addFreshEntity(new ItemEntity(worldIn, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack));
                }
            }
        }
        super.onRemove(state, worldIn, pos, newState, isMoving);
    }


    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DeedPedestalTileEntity();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
