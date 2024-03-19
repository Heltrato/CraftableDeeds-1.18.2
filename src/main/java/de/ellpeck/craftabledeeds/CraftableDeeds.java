package de.ellpeck.craftabledeeds;

import de.ellpeck.craftabledeeds.blocks.DeedPedestalBlock;
import de.ellpeck.craftabledeeds.blocks.DeedPedestalRenderer;
import de.ellpeck.craftabledeeds.blocks.DeedPedestalTileEntity;
import de.ellpeck.craftabledeeds.items.EmptyDeedItem;
import de.ellpeck.craftabledeeds.items.FilledDeedItem;
import de.ellpeck.craftabledeeds.ui.DeedPedestalContainer;
import de.ellpeck.craftabledeeds.ui.DeedPedestalScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Mod(CraftableDeeds.ID)
public class CraftableDeeds {

    public static final String ID = "craftabledeeds";

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ID);
    public static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, ID);
    public static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, ID);

    public static final RegistryObject<Item> EMPTY_DEED = ITEMS.register("empty_deed", EmptyDeedItem::new);
    public static final RegistryObject<Item> FILLED_DEED = ITEMS.register("filled_deed", FilledDeedItem::new);

    public static final RegistryObject<Block> DEED_PEDESTAL_BLOCK = BLOCKS.register("deed_pedestal", DeedPedestalBlock::new);
    public static final RegistryObject<Item> DEED_PEDESTAL_ITEM = ITEMS.register("deed_pedestal", () -> new BlockItem(DEED_PEDESTAL_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS).fireResistant()));
    public static final RegistryObject<BlockEntityType<DeedPedestalTileEntity>> DEED_PEDESTAL_TILE = TILES.register("deed_pedestal", () -> BlockEntityType.Builder.of(DeedPedestalTileEntity::new, DEED_PEDESTAL_BLOCK.get()).build(null));
    public static final RegistryObject<MenuType<DeedPedestalContainer>> DEED_PEDESTAL_CONTAINER = CONTAINERS.register("deed_pedestal", () -> IForgeMenuType.create((id, inv, data) -> new DeedPedestalContainer(id, inv.player, data.readBlockPos())));

    public static ForgeConfigSpec.ConfigValue<Boolean> requirePedestals;
    public static ForgeConfigSpec.ConfigValue<Boolean> allowOpeningBlocks;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> allowedDimensions;
    public static ForgeConfigSpec.ConfigValue<Integer> claimCooldown;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> breakableBlocks;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> interactableBlocks;
    public static ForgeConfigSpec.ConfigValue<Boolean> allowTntExplosions;
    public static ForgeConfigSpec.ConfigValue<Boolean> allowCreeperExplosions;
    public static ForgeConfigSpec.ConfigValue<Boolean> allowWitherExplosions;
    public static ForgeConfigSpec.ConfigValue<Integer> deedBypassPermissionLevel;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> additionalLoyalMobs;

    public CraftableDeeds() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(PacketHandler::init);
        bus.addListener(Client::init);
        bus.addListener(Client::registerRenderers);
        MinecraftForge.EVENT_BUS.addListener(Events::onPlayerJoin);
        MinecraftForge.EVENT_BUS.addListener(Events::onWorldTick);
        MinecraftForge.EVENT_BUS.addListener(Events::onBlockBreak);
        MinecraftForge.EVENT_BUS.addListener(Events::onBlockPlace);
        MinecraftForge.EVENT_BUS.addListener(Events::onBlockInteract);
        MinecraftForge.EVENT_BUS.addListener(Events::onBlockClick);
        MinecraftForge.EVENT_BUS.addListener(Events::onEntityInteract);
        MinecraftForge.EVENT_BUS.addListener(Events::onEntityAttack);
        MinecraftForge.EVENT_BUS.addListener(Events::onExplosion);
        MinecraftForge.EVENT_BUS.addListener(Events::onPiston);
        MinecraftForge.EVENT_BUS.addListener(Events::onServerStarting);
        ITEMS.register(bus);
        BLOCKS.register(bus);
        TILES.register(bus);
        CONTAINERS.register(bus);

        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        requirePedestals = builder.comment("Whether the deed needs to be in a deed pedestal inside the claimed area for the claim to be valid").define("requirePedestals", true);
        allowOpeningBlocks = builder.comment("Whether opening blocks (like furnaces and chests) is allowed inside other players' claims").define("allowOpeningBlocks", false);
        allowedDimensions = builder.comment("The dimension ids of dimensions that using claims is allowed in. To allow all dimensions, add an entry \"*\"").defineList("allowedDimensions", Arrays.asList("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"), o -> true);
        claimCooldown = builder.comment("The amount of hours that have to pass before a destroyed claim's area can be claimed by anyone but the previous owner again").define("claimCooldown", 12);
        breakableBlocks = builder.comment("The list of blocks that can be broken in an area even if it is claimed, supports regex").defineList("breakableBlocks", Collections.emptyList(), o -> true);
        interactableBlocks = builder.comment("The list of blocks that can be interacted with in an area even if it is claimed, supports regex").defineList("interactableBlocks", Arrays.asList("minecraft:lever", ".*_door", ".*_fence_gate", ".*_button"), o -> true);
        deedBypassPermissionLevel = builder.comment("The permission level required to bypass deed restrictions").define("deedBypassPermissionLevel", 2);
        additionalLoyalMobs = builder.comment("The registry names of additional mobs that should be considered loyal and attack players in a claim. Note that, if a mob is tameable, it only attacks if tamed").defineList("additionalLoyalMobs", Collections.emptyList(), o -> true);
        builder.push("explosions");
        allowTntExplosions = builder.comment("Whether TNT explosions are allowed in claimed areas").define("tnt", true);
        allowCreeperExplosions = builder.comment("Whether creeper explosions are allowed in claimed areas").define("creepers", false);
        allowWitherExplosions = builder.comment("Whether explosions caused by withers are allowed in claimed areas").define("withers", true);
        builder.pop();
        ModLoadingContext.get().registerConfig(Type.COMMON, builder.build());
    }

    private static class Client {

        public static void init(FMLClientSetupEvent event) {
            ItemBlockRenderTypes.setRenderLayer(DEED_PEDESTAL_BLOCK.get(), RenderType.cutout());
            MenuScreens.register(DEED_PEDESTAL_CONTAINER.get(), DeedPedestalScreen::new);
        }

        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event){
            event.registerBlockEntityRenderer(DEED_PEDESTAL_TILE.get(), DeedPedestalRenderer::new);
        }



    }
}
