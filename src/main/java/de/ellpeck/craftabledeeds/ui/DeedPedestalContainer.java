package de.ellpeck.craftabledeeds.ui;

import de.ellpeck.craftabledeeds.CraftableDeeds;
import de.ellpeck.craftabledeeds.DeedStorage;
import de.ellpeck.craftabledeeds.blocks.DeedPedestalTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class DeedPedestalContainer extends AbstractContainerMenu {

    public final DeedPedestalTileEntity tile;

    public DeedPedestalContainer(int id, Player player, BlockPos pos) {
        super(CraftableDeeds.DEED_PEDESTAL_CONTAINER.get(), id);
        this.tile = (DeedPedestalTileEntity) player.level.getBlockEntity(pos);
    }

    @Override
    public boolean stillValid(Player playerIn) {
        if (this.tile.isRemoved())
            return false;
        DeedStorage.Claim claim = this.tile.getClaim();
        return claim != null && claim.owner.equals(playerIn.getUUID());
    }


}
