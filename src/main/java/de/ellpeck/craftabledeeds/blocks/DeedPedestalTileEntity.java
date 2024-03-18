package de.ellpeck.craftabledeeds.blocks;

import de.ellpeck.craftabledeeds.CraftableDeeds;
import de.ellpeck.craftabledeeds.DeedStorage;
import de.ellpeck.craftabledeeds.items.FilledDeedItem;
import de.ellpeck.craftabledeeds.ui.DeedPedestalContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public class DeedPedestalTileEntity extends BlockEntity implements MenuProvider, TickableBlockEntity {

    public final ItemStackHandler items = new ItemStackHandler(1);

    public DeedPedestalTileEntity(BlockPos pos, BlockState state) {
        super(CraftableDeeds.DEED_PEDESTAL_TILE.get(), pos, state);
    }



    public int getMapId() {
        ItemStack stack = this.items.getStackInSlot(0);
        if (stack.getItem() == CraftableDeeds.FILLED_DEED.get())
            return MapItem.getMapId(stack);
        return -1;
    }

    public MapItemSavedData getMapData() {
        int id = this.getMapId();
        if (id >= 0)
            return this.level.getMapData(FilledDeedItem.makeKey(id));
        return null;
    }

    public DeedStorage.Claim getClaim() {
        int mapId = this.getMapId();
        if (mapId >= 0)
            return DeedStorage.get(this.level).getClaim(mapId);
        return null;
    }

    @Override
    public void tick() {
        // send map data every 10 ticks similarly to how TrackedEntity does for item frames
        if (!this.level.isClientSide && this.level.getGameTime() % 10 == 0) {
            MapItemSavedData data = this.getMapData();
            if (data != null) {
                ItemStack stack = this.items.getStackInSlot(0);
                ((ServerChunkCache) this.level.getChunkSource()).chunkMap
                        .getPlayers(new ChunkPos(this.getBlockPos()), false)
                        .forEach(p -> {
                            Packet<?> ipacket = data.getHoldingPlayer(p).player.getAddEntityPacket();
                            if (ipacket != null)
                                p.connection.send(ipacket);
                        });
            }
        }
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        DeedStorage.get(this.level).pedestals.put(this.getBlockPos(), this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        DeedStorage.get(this.level).pedestals.remove(this.getBlockPos());
    }

    @Override
    public void saveAdditional(CompoundTag compound) {
        compound.put("items", this.items.serializeNBT());
        super.saveAdditional(compound);
    }

    @Override
    public void load(CompoundTag nbt) {
        this.items.deserializeNBT(nbt.getCompound("items"));
        super.load(nbt);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

/*    @Override
    public void handleUpdateTag(BlockState state, CompoundTag tag) {
        this.read(state, tag);
    }*/

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        this.load(pkt.getTag());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public AABB getRenderBoundingBox() {
        return new AABB(this.getBlockPos(), this.getBlockPos().offset(1, 2, 1));
    }

    @Override
    public Component getDisplayName() {
        return new TranslatableComponent("container." + CraftableDeeds.ID + ".deed_pedestal");
    }


    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new DeedPedestalContainer(id, player, this.getBlockPos());
    }
}
