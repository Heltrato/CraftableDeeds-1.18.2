package de.ellpeck.craftabledeeds.ui.blocks;

import de.ellpeck.craftabledeeds.CraftableDeeds;
import de.ellpeck.craftabledeeds.DeedStorage;
import de.ellpeck.craftabledeeds.PacketHandler;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ContainerBlock;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public class DeedPedestalBlock extends ContainerBlock {

    private static final VoxelShape SHAPE = makeCuboidShape(0, 0, 0, 16, 12, 16);

    public DeedPedestalBlock() {
        super(Properties.from(Blocks.STONE_BRICKS).hardnessAndResistance(5, 1200));
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        TileEntity tile = worldIn.getTileEntity(pos);
        if (!(tile instanceof DeedPedestalTileEntity))
            return ActionResultType.FAIL;
        DeedPedestalTileEntity pedestal = (DeedPedestalTileEntity) tile;
        ItemStackHandler items = pedestal.items;
        ItemStack contained = items.getStackInSlot(0);
        ItemStack hand = player.getHeldItem(handIn);
        if (contained.isEmpty()) {
            // putting a deed in
            if (hand.getItem() == CraftableDeeds.FILLED_DEED.get()) {
                if (!worldIn.isRemote) {
                    items.setStackInSlot(0, hand);
                    player.setHeldItem(handIn, ItemStack.EMPTY);
                    PacketHandler.sendTileEntityToClients(pedestal);
                }
                return ActionResultType.SUCCESS;
            }
        } else {
            DeedStorage.Claim claim = pedestal.getClaim();

            // opening the management ui
            if (claim.owner.equals(player.getUniqueID()) && !player.isSneaking()) {
                if (!worldIn.isRemote)
                    NetworkHooks.openGui((ServerPlayerEntity) player, pedestal, pos);
                return ActionResultType.SUCCESS;
            }

            // taking out the deed
            if (!worldIn.isRemote) {
                items.setStackInSlot(0, ItemStack.EMPTY);
                PacketHandler.sendTileEntityToClients(pedestal);

                if (claim.owner.equals(player.getUniqueID())) {
                    if (!player.addItemStackToInventory(contained))
                        worldIn.addEntity(new ItemEntity(worldIn, pos.getX() + 0.5F, pos.getY() + 1, pos.getZ() + 0.5F, contained));
                } else {
                    // if we're not the claim's owner, we want to put the claim in cooldown mode and explode
                    worldIn.createExplosion(null, pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F, 2, Explosion.Mode.NONE);
                    worldIn.addEntity(new ItemEntity(worldIn, pos.getX() + 0.5F, pos.getY() + 1.5F, pos.getZ() + 0.5F, new ItemStack(Items.NETHER_STAR)));
                    // cooldown is in hours, so ticks * seconds * minutes
                    claim.cooldown = CraftableDeeds.claimCooldown.get() * 20 * 60 * 60;
                }
            }
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.FAIL;
    }

    @Override
    public void onReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            TileEntity tile = worldIn.getTileEntity(pos);
            if (tile instanceof DeedPedestalTileEntity) {
                IItemHandler handler = ((DeedPedestalTileEntity) tile).items;
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (!stack.isEmpty())
                        worldIn.addEntity(new ItemEntity(worldIn, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack));
                }
            }
        }
        super.onReplaced(state, worldIn, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(IBlockReader worldIn) {
        return new DeedPedestalTileEntity();
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        return SHAPE;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
}
