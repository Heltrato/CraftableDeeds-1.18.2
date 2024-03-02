package de.ellpeck.craftabledeeds;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
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

    public static void sendDeeds(Player player) {
        PacketDeeds packet = new PacketDeeds(DeedStorage.get(player.level).save(new CompoundTag()));
        network.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), packet);
    }

    public static void sendDeedsToEveryone(Level world) {
        PacketDeeds packet = new PacketDeeds(DeedStorage.get(world).save(new CompoundTag()));
        network.send(PacketDistributor.DIMENSION.with(world::dimension), packet);
    }

    public static void sendPlayerSettings(DeedStorage.PlayerSettings settings, DeedStorage.Claim claim) {
        network.sendToServer(new PacketPlayerSettings(settings, claim.mapId));
    }

    public static void sendGeneralSettings(DeedStorage.Claim claim) {
        network.sendToServer(new PacketGeneralSettings(claim.canDispensersPlace, claim.canPistonsPush, claim.mapId));
    }

    public static void sendTileEntityToClients(BlockEntity tile) {
        ServerLevelAccessor world = (ServerLevelAccessor) tile.getLevel();
        // Stream<ServerPlayer> entities = world.getChunkSource().chunkMap.getTrackingPlayers(new ChunkPos(tile.getPos()), false);
        Stream<ServerPlayer> entities = (Stream<ServerPlayer>) world.getLevel().getChunkSource().chunkMap.getPlayers(new ChunkPos(tile.getBlockPos()), false);
        ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(tile);
        entities.forEach(e -> e.connection.send(packet));
    }

    private static class PacketDeeds {

        private final CompoundTag data;

        public PacketDeeds(CompoundTag data) {
            this.data = data;
        }

        public static PacketDeeds fromBytes(FriendlyByteBuf buf) {
            return new PacketDeeds(buf.readNbt());
        }

        public static void toBytes(PacketDeeds packet, FriendlyByteBuf buf) {
            buf.writeNbt(packet.data);
        }

        // lambda causes classloading issues on a server here
        @SuppressWarnings("Convert2Lambda")
        public static void onMessage(PacketDeeds packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(new Runnable() {
                @Override
                public void run() {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level != null)
                        DeedStorage.get(mc.level).save(packet.data);
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

        public static PacketPlayerSettings fromBytes(FriendlyByteBuf buf) {
            return new PacketPlayerSettings(new DeedStorage.PlayerSettings(buf.readNbt()), buf.readVarInt());
        }

        public static void toBytes(PacketPlayerSettings packet, FriendlyByteBuf buf) {
            buf.writeNbt(packet.settings.serializeNBT());
            buf.writeVarInt(packet.claimId);
        }

        public static void onMessage(PacketPlayerSettings packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                Player sender = ctx.get().getSender();
                DeedStorage storage = DeedStorage.get(sender.level);
                DeedStorage.Claim claim = storage.getClaim(packet.claimId);
                if (claim != null && claim.owner.equals(sender.getUUID())) {
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

        public static PacketGeneralSettings fromBytes(FriendlyByteBuf buf) {
            return new PacketGeneralSettings(buf.readBoolean(), buf.readBoolean(), buf.readVarInt());
        }

        public static void toBytes(PacketGeneralSettings packet, FriendlyByteBuf buf) {
            buf.writeBoolean(packet.canDispensersPlace);
            buf.writeBoolean(packet.canPistonsPush);
            buf.writeVarInt(packet.claimId);
        }

        public static void onMessage(PacketGeneralSettings packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                Player sender = ctx.get().getSender();
                DeedStorage storage = DeedStorage.get(sender.level);
                DeedStorage.Claim claim = storage.getClaim(packet.claimId);
                if (claim != null && claim.owner.equals(sender.getUUID())) {
                    claim.canDispensersPlace = packet.canDispensersPlace;
                    claim.canPistonsPush = packet.canPistonsPush;
                    storage.markDirtyAndSend();
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}