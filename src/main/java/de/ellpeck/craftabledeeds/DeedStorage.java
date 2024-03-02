package de.ellpeck.craftabledeeds;

import de.ellpeck.craftabledeeds.blocks.DeedPedestalTileEntity;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeedStorage extends SavedData {

    private static final String NAME = CraftableDeeds.ID + ":deed_storage";
    private static DeedStorage clientStorage;

    public Map<BlockPos, DeedPedestalTileEntity> pedestals = new HashMap<>();
    private final Level world;
    private final Map<Integer, Claim> claims = new HashMap<>();

    public DeedStorage(Level world) {
        this.world = world;
    }

    public void addClaim(int id, Player owner) {
        this.claims.put(id, new Claim(this.world, id, owner.getUUID()));
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
        if (this.world.isClientSide || this.world.getGameTime() % interval != 0)
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
                    if (stack.getItem() == CraftableDeeds.FILLED_DEED.get() && MapItem.getMapId(stack) == claim.mapId)
                        continue;
                }
                claim.pedestal = null;
                this.markDirtyAndSend();
            }

            // if the pedestal doesn't still contain our deed, check if there is any new pedestal
            AABB area = claim.getArea();
            for (DeedPedestalTileEntity tile : this.pedestals.values()) {
                BlockPos pos = tile.getPos();
                if (area.contains(pos.getX(), pos.getY(), pos.getZ())) {
                    ItemStack stack = tile.items.getStackInSlot(0);
                    if (stack.getItem() == CraftableDeeds.FILLED_DEED.get() && MapItem.getMapId(stack) == claim.mapId) {
                        claim.pedestal = pos;
                        this.markDirtyAndSend();
                        break;
                    }
                }
            }
        }
    }

    public void markDirtyAndSend() {
        if (!this.world.isClientSide) {
            PacketHandler.sendDeedsToEveryone(this.world);
            this.setDirty();
        }
    }

/*
    @Override
    public void read(CompoundTag nbt) {
        this.claims.clear();
        ListTag claims = nbt.getList("claims", SharedConstants.SNBT_NAG_VERSION);
        for (int i = 0; i < claims.size(); i++) {
            Claim claim = new Claim(this.world, claims.getCompound(i));
            this.claims.put(claim.mapId, claim);
        }
    }*/

    @Override
    public CompoundTag save(CompoundTag compound) {
        ListTag claims = new ListTag();
        for (Claim claim : this.claims.values())
            claims.add(claim.serializeNBT());
        compound.put("claims", claims);
        return compound;
    }

    public static DeedStorage get(Level world) {
        if (world.isClientSide) {
            if (clientStorage == null || clientStorage.world != world)
                clientStorage = new DeedStorage(world);
            return clientStorage;
        } else {
            return ((ServerLevel) world).getDataStorage().computeIfAbsent(DeedStorage::save, () -> new DeedStorage(world), NAME);
        }
    }

    public static class Claim implements INBTSerializable<CompoundTag> {

        public final Map<UUID, PlayerSettings> playerSettings = new HashMap<>();
        public int mapId;
        public UUID owner;
        public BlockPos pedestal;
        public int cooldown;
        public boolean canDispensersPlace = true;
        public boolean canPistonsPush = true;

        private final Level world;
        private int xCenter;
        private int zCenter;
        private int scale;

        public Claim(Level world, int mapId, UUID owner) {
            MapItemSavedData data = world.getMapData(MapItem.makeKey(mapId));
            this.world = world;
            this.mapId = mapId;
            this.owner = owner;
            this.xCenter = data.x;
            this.zCenter = data.z;
            this.scale = data.scale;
        }

        public Claim(Level world, CompoundTag nbt) {
            this.world = world;
            this.deserializeNBT(nbt);
        }

        public AABB getArea() {
            int i = 1 << this.scale;
            return new AABB(
                    // start at y 15
                    this.xCenter - 64 * i, 15, this.zCenter - 64 * i,
                    this.xCenter + 64 * i, this.world.getHeight(), this.zCenter + 64 * i);
        }

        public Object getOwnerName() {
            Player owner = this.world.getPlayerByUUID(this.owner);
            return owner != null ? owner.getDisplayName() : this.owner;
        }

        public boolean isActive() {
            if (this.cooldown > 0)
                return false;
            return !CraftableDeeds.requirePedestals.get() || this.pedestal != null;
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag nbt = new CompoundTag();
            nbt.putInt("id", this.mapId);
            nbt.putUUID("owner", this.owner);
            nbt.putInt("xCenter", this.xCenter);
            nbt.putInt("zCenter", this.zCenter);
            nbt.putInt("scale", this.scale);
            nbt.putInt("cooldown", this.cooldown);
            nbt.putBoolean("canDispensersPlace", this.canDispensersPlace);
            nbt.putBoolean("canPistonsPush", this.canPistonsPush);
            if (this.pedestal != null)
                nbt.putLong("pedestal", this.pedestal.asLong());
            ListTag playerSettings = new ListTag();
            for (PlayerSettings settings : this.playerSettings.values())
                playerSettings.add(settings.serializeNBT());
            nbt.put("playerSettings", playerSettings);
            return nbt;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            this.mapId = nbt.getInt("id");
            this.owner = nbt.getUUID("owner");
            this.xCenter = nbt.getInt("xCenter");
            this.zCenter = nbt.getInt("zCenter");
            this.scale = nbt.getInt("scale");
            this.cooldown = nbt.getInt("cooldown");
            this.canDispensersPlace = nbt.getBoolean("canDispensersPlace");
            this.canPistonsPush = nbt.getBoolean("canPistonsPush");
            this.pedestal = nbt.contains("pedestal") ? BlockPos.of(nbt.getLong("pedestal")) : null;
            this.playerSettings.clear();
            for (Tag inbt : nbt.getList("playerSettings", SharedConstants.SNBT_NAG_VERSION)) {
                PlayerSettings settings = new PlayerSettings((CompoundTag) inbt);
                this.playerSettings.put(settings.id, settings);
            }
        }
    }

    public static class PlayerSettings implements INBTSerializable<CompoundTag> {

        public UUID id;
        public String name;
        public boolean isFake;
        public boolean canPlaceBreak;
        public boolean loyalMobsAttack;
        public boolean canOpenContainers;

        public PlayerSettings(Player player) {
            this.id = player.getUUID();
            this.name = player.getDisplayName().getString();
            this.isFake = player instanceof FakePlayer;
        }

        public PlayerSettings(CompoundTag nbt) {
            this.deserializeNBT(nbt);
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag nbt = new CompoundTag();
            nbt.putUUID("id", this.id);
            nbt.putString("name", this.name);
            nbt.putBoolean("isFake", this.isFake);
            nbt.putBoolean("canPlaceBreak", this.canPlaceBreak);
            nbt.putBoolean("loyalMobsAttack", this.loyalMobsAttack);
            nbt.putBoolean("canOpenContainers", this.canOpenContainers);
            return nbt;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            this.id = nbt.getUUID("id");
            this.name = nbt.getString("name");
            this.isFake = nbt.getBoolean("isFake");
            this.canPlaceBreak = nbt.getBoolean("canPlaceBreak");
            this.loyalMobsAttack = nbt.getBoolean("loyalMobsAttack");
            this.canOpenContainers = nbt.getBoolean("canOpenContainers");
        }
    }
}
