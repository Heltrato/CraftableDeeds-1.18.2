package de.ellpeck.craftabledeeds;

import de.ellpeck.craftabledeeds.blocks.DeedPedestalTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.MapData;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeedStorage extends WorldSavedData {

    private static final String NAME = CraftableDeeds.ID + ":deed_storage";
    private static DeedStorage clientStorage;

    public Map<BlockPos, DeedPedestalTileEntity> pedestals = new HashMap<>();
    private final World world;
    private final Map<Integer, Claim> claims = new HashMap<>();

    public DeedStorage(World world) {
        super(NAME);
        this.world = world;
    }

    public void addClaim(int id, PlayerEntity owner) {
        this.claims.put(id, new Claim(this.world, id, owner.getUniqueID()));
        this.markDirtyAndSend();
    }

    public void removeClaim(int id) {
        if (this.claims.remove(id) != null)
            this.markDirtyAndSend();
    }

    public Claim getClaim(double x, double y, double z) {
        for (Claim claim : this.claims.values()) {
            if (claim.getArea().contains(x, y, z))
                return claim;
        }
        return null;
    }

    public Claim getClaim(int id) {
        return this.claims.get(id);
    }

    public void update() {
        int interval = 40;
        if (this.world.isRemote || this.world.getGameTime() % interval != 0)
            return;
        for (Claim claim : this.claims.values()) {
            // update claim cooldown
            if (claim.cooldown > 0) {
                claim.cooldown -= interval;
                if (claim.cooldown <= 0) {
                    this.removeClaim(claim.mapId);
                    continue;
                }
            }

            if (claim.pedestal != null) {
                // check if the existing pedestal still contains our deed and skip if it does
                DeedPedestalTileEntity existing = this.pedestals.get(claim.pedestal);
                if (existing != null) {
                    ItemStack stack = existing.items.getStackInSlot(0);
                    if (stack.getItem() == CraftableDeeds.FILLED_DEED.get() && FilledMapItem.getMapId(stack) == claim.mapId)
                        continue;
                }
                claim.pedestal = null;
                this.markDirtyAndSend();
            }

            // if the pedestal doesn't still contain our deed, check if there is any new pedestal
            AxisAlignedBB area = claim.getArea();
            for (DeedPedestalTileEntity tile : this.pedestals.values()) {
                BlockPos pos = tile.getPos();
                if (area.contains(pos.getX(), pos.getY(), pos.getZ())) {
                    ItemStack stack = tile.items.getStackInSlot(0);
                    if (stack.getItem() == CraftableDeeds.FILLED_DEED.get() && FilledMapItem.getMapId(stack) == claim.mapId) {
                        claim.pedestal = pos;
                        this.markDirtyAndSend();
                        break;
                    }
                }
            }
        }
    }

    public void markDirtyAndSend() {
        if (!this.world.isRemote) {
            PacketHandler.sendDeedsToEveryone(this.world);
            this.markDirty();
        }
    }

    @Override
    public void read(CompoundNBT nbt) {
        this.claims.clear();
        ListNBT claims = nbt.getList("claims", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < claims.size(); i++) {
            Claim claim = new Claim(this.world, claims.getCompound(i));
            this.claims.put(claim.mapId, claim);
        }
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        ListNBT claims = new ListNBT();
        for (Claim claim : this.claims.values())
            claims.add(claim.serializeNBT());
        compound.put("claims", claims);
        return compound;
    }

    public static DeedStorage get(World world) {
        if (world.isRemote) {
            if (clientStorage == null || clientStorage.world != world)
                clientStorage = new DeedStorage(world);
            return clientStorage;
        } else {
            return ((ServerWorld) world).getSavedData().getOrCreate(() -> new DeedStorage(world), NAME);
        }
    }

    public static class Claim implements INBTSerializable<CompoundNBT> {

        public final Map<UUID, PlayerSettings> playerSettings = new HashMap<>();
        public int mapId;
        public UUID owner;
        public BlockPos pedestal;
        public int cooldown;
        public boolean canDispensersPlace = true;
        public boolean canPistonsPush = true;

        private final World world;
        private int xCenter;
        private int zCenter;
        private int scale;

        public Claim(World world, int mapId, UUID owner) {
            MapData data = world.getMapData(FilledMapItem.getMapName(mapId));
            this.world = world;
            this.mapId = mapId;
            this.owner = owner;
            this.xCenter = data.xCenter;
            this.zCenter = data.zCenter;
            this.scale = data.scale;
        }

        public Claim(World world, CompoundNBT nbt) {
            this.world = world;
            this.deserializeNBT(nbt);
        }

        public AxisAlignedBB getArea() {
            int i = 1 << this.scale;
            return new AxisAlignedBB(
                    // start at y 15
                    this.xCenter - 64 * i, 15, this.zCenter - 64 * i,
                    this.xCenter + 64 * i, this.world.getHeight(), this.zCenter + 64 * i);
        }

        public Object getOwnerName() {
            PlayerEntity owner = this.world.getPlayerByUuid(this.owner);
            return owner != null ? owner.getDisplayName() : this.owner;
        }

        public boolean isActive() {
            if (this.cooldown > 0)
                return false;
            return !CraftableDeeds.requirePedestals.get() || this.pedestal != null;
        }

        @Override
        public CompoundNBT serializeNBT() {
            CompoundNBT nbt = new CompoundNBT();
            nbt.putInt("id", this.mapId);
            nbt.putUniqueId("owner", this.owner);
            nbt.putInt("xCenter", this.xCenter);
            nbt.putInt("zCenter", this.zCenter);
            nbt.putInt("scale", this.scale);
            nbt.putInt("cooldown", this.cooldown);
            nbt.putBoolean("canDispensersPlace", this.canDispensersPlace);
            nbt.putBoolean("canPistonsPush", this.canPistonsPush);
            if (this.pedestal != null)
                nbt.putLong("pedestal", this.pedestal.toLong());
            ListNBT playerSettings = new ListNBT();
            for (PlayerSettings settings : this.playerSettings.values())
                playerSettings.add(settings.serializeNBT());
            nbt.put("playerSettings", playerSettings);
            return nbt;
        }

        @Override
        public void deserializeNBT(CompoundNBT nbt) {
            this.mapId = nbt.getInt("id");
            this.owner = nbt.getUniqueId("owner");
            this.xCenter = nbt.getInt("xCenter");
            this.zCenter = nbt.getInt("zCenter");
            this.scale = nbt.getInt("scale");
            this.cooldown = nbt.getInt("cooldown");
            this.canDispensersPlace = nbt.getBoolean("canDispensersPlace");
            this.canPistonsPush = nbt.getBoolean("canPistonsPush");
            this.pedestal = nbt.contains("pedestal") ? BlockPos.fromLong(nbt.getLong("pedestal")) : null;
            this.playerSettings.clear();
            for (INBT inbt : nbt.getList("playerSettings", Constants.NBT.TAG_COMPOUND)) {
                PlayerSettings settings = new PlayerSettings((CompoundNBT) inbt);
                this.playerSettings.put(settings.id, settings);
            }
        }
    }

    public static class PlayerSettings implements INBTSerializable<CompoundNBT> {

        public UUID id;
        public String name;
        public boolean isFake;
        public boolean canPlaceBreak;
        public boolean loyalMobsAttack;
        public boolean canOpenContainers;

        public PlayerSettings(PlayerEntity player) {
            this.id = player.getUniqueID();
            this.name = player.getDisplayName().getString();
            this.isFake = player instanceof FakePlayer;
        }

        public PlayerSettings(CompoundNBT nbt) {
            this.deserializeNBT(nbt);
        }

        @Override
        public CompoundNBT serializeNBT() {
            CompoundNBT nbt = new CompoundNBT();
            nbt.putUniqueId("id", this.id);
            nbt.putString("name", this.name);
            nbt.putBoolean("isFake", this.isFake);
            nbt.putBoolean("canPlaceBreak", this.canPlaceBreak);
            nbt.putBoolean("loyalMobsAttack", this.loyalMobsAttack);
            nbt.putBoolean("canOpenContainers", this.canOpenContainers);
            return nbt;
        }

        @Override
        public void deserializeNBT(CompoundNBT nbt) {
            this.id = nbt.getUniqueId("id");
            this.name = nbt.getString("name");
            this.isFake = nbt.getBoolean("isFake");
            this.canPlaceBreak = nbt.getBoolean("canPlaceBreak");
            this.loyalMobsAttack = nbt.getBoolean("loyalMobsAttack");
            this.canOpenContainers = nbt.getBoolean("canOpenContainers");
        }
    }
}
