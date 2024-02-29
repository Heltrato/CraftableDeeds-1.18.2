package de.ellpeck.craftabledeeds.ui;

import de.ellpeck.craftabledeeds.CraftableDeeds;
import de.ellpeck.craftabledeeds.DeedStorage;
import de.ellpeck.craftabledeeds.blocks.DeedPedestalTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.math.BlockPos;

public class DeedPedestalContainer extends Container {

    public final DeedPedestalTileEntity tile;

    public DeedPedestalContainer(int id, PlayerEntity player, BlockPos pos) {
        super(CraftableDeeds.DEED_PEDESTAL_CONTAINER.get(), id);
        this.tile = (DeedPedestalTileEntity) player.world.getTileEntity(pos);
    }

    @Override
    public boolean canInteractWith(PlayerEntity playerIn) {
        if (this.tile.isRemoved())
            return false;
        DeedStorage.Claim claim = this.tile.getClaim();
        return claim != null && claim.owner.equals(playerIn.getUniqueID());
    }
}
