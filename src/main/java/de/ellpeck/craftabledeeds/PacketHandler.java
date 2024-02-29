package de.ellpeck.craftabledeeds;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;
import java.util.stream.Stream;

public final class PacketHandler {
    private static SimpleChannel network;

    public static void init(FMLCommonSetupEvent event) {
        String version = "1";
        network = NetworkRegistry.newSimpleChannel(new ResourceLocation(CraftableDeeds.ID, "network"), () -> version, version::equals, version::equals);
        network.registerMessage(0, PacketDeeds.class, PacketDeeds::toBytes, PacketDeeds::fromBytes, PacketDeeds::onMessage);
        network.registerMessage(1, PacketPlayerSettings.class, PacketPlayerSettings::toBytes, PacketPlayerSettings::fromBytes, PacketPlayerSettings::onMessage);
        network.registerMessage(2, PacketGeneralSettings.class, PacketGeneralSettings::toBytes, PacketGeneralSettings::fromBytes, PacketGeneralSettings::onMessage);
    }

    public static void sendDeeds(PlayerEntity player) {
        PacketDeeds packet = new PacketDeeds(DeedStorage.get(player.world).write(new CompoundNBT()));
        network.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), packet);
    }

    public static void sendDeedsToEveryone(World world) {
        PacketDeeds packet = new PacketDeeds(DeedStorage.get(world).write(new CompoundNBT()));
        network.send(PacketDistributor.DIMENSION.with(world::getDimensionKey), packet);
    }

    public static void sendPlayerSettings(DeedStorage.PlayerSettings settings, DeedStorage.Claim claim) {
        network.sendToServer(new PacketPlayerSettings(settings, claim.mapId));
    }

    public static void sendGeneralSettings(DeedStorage.Claim claim) {
        network.sendToServer(new PacketGeneralSettings(claim.canDispensersPlace, claim.canPistonsPush, claim.mapId));
    }

    public static void sendTileEntityToClients(TileEntity tile) {
        ServerWorld world = (ServerWorld) tile.getWorld();
        Stream<ServerPlayerEntity> entities = world.getChunkProvider().chunkManager.getTrackingPlayers(new ChunkPos(tile.getPos()), false);
        SUpdateTileEntityPacket packet = new SUpdateTileEntityPacket(tile.getPos(), -1, tile.write(new CompoundNBT()));
        entities.forEach(e -> e.connection.sendPacket(packet));
    }

    private static class PacketDeeds {

        private final CompoundNBT data;

        public PacketDeeds(CompoundNBT data) {
            this.data = data;
        }

        public static PacketDeeds fromBytes(PacketBuffer buf) {
            return new PacketDeeds(buf.readCompoundTag());
        }

        public static void toBytes(PacketDeeds packet, PacketBuffer buf) {
            buf.writeCompoundTag(packet.data);
        }

        // lambda causes classloading issues on a server here
        @SuppressWarnings("Convert2Lambda")
        public static void onMessage(PacketDeeds packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(new Runnable() {
                @Override
                public void run() {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.world != null)
                        DeedStorage.get(mc.world).read(packet.data);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    private static class PacketPlayerSettings {

        private final DeedStorage.PlayerSettings settings;
        private final int claimId;

        public PacketPlayerSettings(DeedStorage.PlayerSettings settings, int claimId) {
            this.settings = settings;
            this.claimId = claimId;
        }

        public static PacketPlayerSettings fromBytes(PacketBuffer buf) {
            return new PacketPlayerSettings(new DeedStorage.PlayerSettings(buf.readCompoundTag()), buf.readVarInt());
        }

        public static void toBytes(PacketPlayerSettings packet, PacketBuffer buf) {
            buf.writeCompoundTag(packet.settings.serializeNBT());
            buf.writeVarInt(packet.claimId);
        }

        public static void onMessage(PacketPlayerSettings packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                PlayerEntity sender = ctx.get().getSender();
                DeedStorage storage = DeedStorage.get(sender.world);
                DeedStorage.Claim claim = storage.getClaim(packet.claimId);
                if (claim != null && claim.owner.equals(sender.getUniqueID())) {
                    claim.playerSettings.put(packet.settings.id, packet.settings);
                    storage.markDirtyAndSend();
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    private static class PacketGeneralSettings {

        private final int claimId;
        private final boolean canDispensersPlace;
        private final boolean canPistonsPush;

        public PacketGeneralSettings(boolean canDispensersPlace, boolean canPistonsPush, int claimId) {
            this.canDispensersPlace = canDispensersPlace;
            this.canPistonsPush = canPistonsPush;
            this.claimId = claimId;
        }

        public static PacketGeneralSettings fromBytes(PacketBuffer buf) {
            return new PacketGeneralSettings(buf.readBoolean(), buf.readBoolean(), buf.readVarInt());
        }

        public static void toBytes(PacketGeneralSettings packet, PacketBuffer buf) {
            buf.writeBoolean(packet.canDispensersPlace);
            buf.writeBoolean(packet.canPistonsPush);
            buf.writeVarInt(packet.claimId);
        }

        public static void onMessage(PacketGeneralSettings packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                PlayerEntity sender = ctx.get().getSender();
                DeedStorage storage = DeedStorage.get(sender.world);
                DeedStorage.Claim claim = storage.getClaim(packet.claimId);
                if (claim != null && claim.owner.equals(sender.getUniqueID())) {
                    claim.canDispensersPlace = packet.canDispensersPlace;
                    claim.canPistonsPush = packet.canPistonsPush;
                    storage.markDirtyAndSend();
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}