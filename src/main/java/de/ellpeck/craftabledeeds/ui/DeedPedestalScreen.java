package de.ellpeck.craftabledeeds.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.ellpeck.craftabledeeds.CraftableDeeds;
import de.ellpeck.craftabledeeds.DeedStorage;
import de.ellpeck.craftabledeeds.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class DeedPedestalScreen extends AbstractContainerScreen<DeedPedestalContainer> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(CraftableDeeds.ID, "textures/ui/deed_pedestal.png");
    private static final int MAX_WIDGET_AMT_Y = 5;

    private Tab currentTab;
    private int scrollOffset;
    private int widgetAmountY;
    private boolean isScrolling;

    public DeedPedestalScreen(DeedPedestalContainer screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn);
        this.imageWidth = 240;
        this.imageHeight = 144;
    }

    @Override
    protected void init() {
        super.init();
        this.setTab(Tab.PLAYERS);
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
        for (var child : this.renderables) {
            if (child instanceof AbstractWidget widget) {
                if (widget.isHoveredOrFocused())
                    widget.renderToolTip(matrixStack, mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(PoseStack matrixStack, float partialTicks, int x, int y) {
        this.minecraft.textureManager.getTexture(TEXTURE);
        this.blit(matrixStack, this.getGuiLeft(), this.getGuiTop(), 0, 0, this.width, this.height);

        if (this.widgetAmountY > MAX_WIDGET_AMT_Y) {
            float percentage = this.scrollOffset / (float) (this.widgetAmountY - MAX_WIDGET_AMT_Y);
            this.blit(matrixStack, this.getGuiLeft() + 223, this.getGuiTop() + 19 + (int) (percentage * (120 - 15)), 232, 241, 12, 15);
        } else {
            this.blit(matrixStack, this.getGuiLeft() + 223, this.getGuiTop() + 19, 244, 241, 12, 15);
        }
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int x, int y) {
        this.font.draw(matrixStack, this.title, this.titleLabelX, this.titleLabelY, 4210752);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= this.getGuiLeft() + 223 && mouseY >= this.getGuiTop() + 19 && mouseX < this.getGuiLeft() + 223 + 12 && mouseY < this.getGuiTop() + 19 + 120) {
            this.isScrolling = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0)
            this.isScrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int i, double j, double k) {
        if (this.isScrolling && this.widgetAmountY > MAX_WIDGET_AMT_Y) {
            float percentage = Mth.clamp(((float) mouseY - (this.getGuiTop() + 19)) / (120 - 15), 0, 1);
            int offset = (int) (percentage * (float) (this.widgetAmountY - MAX_WIDGET_AMT_Y));
            if (offset != this.scrollOffset) {
                this.scrollOffset = offset;
                this.setTab(this.currentTab);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, i, j, k);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scroll) {
        if (this.widgetAmountY > MAX_WIDGET_AMT_Y) {
            int offset = Mth.clamp(this.scrollOffset - (int) Math.signum(scroll), 0, this.widgetAmountY - MAX_WIDGET_AMT_Y);
            if (offset != this.scrollOffset) {
                this.scrollOffset = offset;
                this.setTab(this.currentTab);
            }
        }
        return true;
    }

    private void setTab(Tab newTab) {
        this.renderables.clear();
        this.children().clear();

        // add tabs
        int yOffset = 16;
        int uOffset = 0;
        for (Tab tab : Tab.values()) {
            this.addRenderableOnly(new TabWidget(this.getGuiLeft() - 28, this.getGuiTop() + yOffset, 32, 28, 1 + uOffset, 167, tab));
            yOffset += 29;
            uOffset += 33;
        }

        // open tab
        this.currentTab = newTab;
        List<AbstractWidget> content = newTab.init.apply(this);
        this.widgetAmountY = content.size() / newTab.widgetAmountX;
        for (int y = 0; y < MAX_WIDGET_AMT_Y; y++) {
            if (y >= this.widgetAmountY)
                return;
            int xOffset = 4;
            for (int x = 0; x < newTab.widgetAmountX; x++) {
                AbstractWidget widget = this.addRenderableOnly(content.get((this.scrollOffset + y) * newTab.widgetAmountX + x));
                widget.x = this.getGuiLeft() + xOffset;
                widget.y = this.getGuiTop() + 18 + 25 * y;
                xOffset += widget.getWidth() + 1;
            }
        }
    }

    private class TabWidget extends AbstractButton {

        private final Tab tab;
        private final int u;
        private final int v;

        public TabWidget(int x, int y, int width, int height, int u, int v, Tab tab) {
            super(x, y, width, height, new TranslatableComponent("tab." + CraftableDeeds.ID + "." + tab.name().toLowerCase(Locale.ROOT)));
            this.u = u;
            this.v = v;
            this.tab = tab;
        }

        @Override
        public void onPress() {
            DeedPedestalScreen.this.scrollOffset = 0;
            DeedPedestalScreen.this.setTab(this.tab);
        }

        @Override
        public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            Minecraft.getInstance().getTextureManager().getTexture(TEXTURE);
            int v = this.v;
            if (DeedPedestalScreen.this.currentTab == this.tab)
                v += this.height;

            RenderSystem.enableDepthTest();
            blit(matrixStack, this.x, this.y, this.u, v, this.width, this.height, 256, 256);
        }

        @Override
        public void renderToolTip(PoseStack matrixStack, int mouseX, int mouseY) {
            DeedPedestalScreen.this.renderTooltip(matrixStack, this.getMessage(), mouseX, mouseY);
        }

        @Override
        public void updateNarration(NarrationElementOutput narrationElementOutput) {

        }
    }

    private enum Tab {
        PLAYERS(screen -> {
            List<AbstractWidget> ret = new ArrayList<>();
            List<ProperToggleWidget> allPlaceBreak = new ArrayList<>();
            List<ProperToggleWidget> allLoyalAttack = new ArrayList<>();
            List<ProperToggleWidget> allContainers = new ArrayList<>();
            DeedStorage.Claim claim = screen.getMenu().tile.getClaim();
            // add existing player settings
            claim.playerSettings.values().stream().filter(s -> !s.isFake).forEach(s -> ret.addAll(createPlayerRow(screen, s, allPlaceBreak, allLoyalAttack, allContainers)));
            for (Player player : screen.getMenu().tile.getLevel().players()) {
                // add player settings for new players
                if (!claim.playerSettings.containsKey(player.getUUID()) && !claim.owner.equals(player.getUUID()))
                    ret.addAll(createPlayerRow(screen, new DeedStorage.PlayerSettings(player), allPlaceBreak, allLoyalAttack, allContainers));
            }

            // "toggle all" entry
            if (ret.size() > 0) {
                ret.add(0, new TextWidget(0, 0, 148, 22, new TranslatableComponent("info." + CraftableDeeds.ID + ".set_all")));
                ret.add(1, new ProperToggleWidget(0, 0, 22, 22, 100, 167, 22, 0, TEXTURE, "info." + CraftableDeeds.ID + ".can_place_break",
                        allPlaceBreak.stream().allMatch(StateSwitchingButton::isStateTriggered),
                        v -> allPlaceBreak.forEach(w -> w.setStateTriggered(v))));
                ret.add(2, new ProperToggleWidget(0, 0, 22, 22, 100, 190, 22, 0, TEXTURE, "info." + CraftableDeeds.ID + ".loyal_mobs_attack",
                        allLoyalAttack.stream().allMatch(StateSwitchingButton::isStateTriggered),
                        v -> allLoyalAttack.forEach(w -> w.setStateTriggered(v))));
                ret.add(3, new ProperToggleWidget(0, 0, 22, 22, 100, 213, 22, 0, TEXTURE, "info." + CraftableDeeds.ID + ".can_open_containers",
                        allContainers.stream().allMatch(StateSwitchingButton::isStateTriggered),
                        v -> allContainers.forEach(w -> w.setStateTriggered(v))));
            }
            return ret;
        }, 4),
        BLOCKS(screen -> {
            List<AbstractWidget> ret = new ArrayList<>();
            List<ProperToggleWidget> allPlaceBreak = new ArrayList<>();
            DeedStorage.Claim claim = screen.getMenu().tile.getClaim();
            ret.add(new TextWidget(0, 0, 148, 22, Blocks.DISPENSER.getName()));
            ProperToggleWidget w;
            ret.add(w = new ProperToggleWidget(0, 0, 22, 22, 100, 167, 22, 0, TEXTURE, "info." + CraftableDeeds.ID + ".can_edit_world", claim.canDispensersPlace, v -> {
                claim.canDispensersPlace = v;
                PacketHandler.sendGeneralSettings(claim);
            }));
            allPlaceBreak.add(w);
            ret.add(new TextWidget(0, 0, 148, 22, Blocks.PISTON.getName()));
            ret.add(w = new ProperToggleWidget(0, 0, 22, 22, 100, 167, 22, 0, TEXTURE, "info." + CraftableDeeds.ID + ".can_edit_world", claim.canPistonsPush, v -> {
                claim.canPistonsPush = v;
                PacketHandler.sendGeneralSettings(claim);
            }));
            allPlaceBreak.add(w);
            claim.playerSettings.values().stream().filter(s -> s.isFake).forEach(s -> ret.addAll(createPlayerRow(screen, s, allPlaceBreak, null, null)));

            // "toggle all" entry
            ret.add(0, new TextWidget(0, 0, 148, 22, new TranslatableComponent("info." + CraftableDeeds.ID + ".set_all")));
            ret.add(1, new ProperToggleWidget(0, 0, 22, 22, 100, 167, 22, 0, TEXTURE, "info." + CraftableDeeds.ID + ".can_edit_world",
                    allPlaceBreak.stream().allMatch(StateSwitchingButton::isStateTriggered),
                    v -> allPlaceBreak.forEach(x -> x.setStateTriggered(v))));
            return ret;
        }, 2);

        public final Function<DeedPedestalScreen, List<AbstractWidget>> init;
        public final int widgetAmountX;

        Tab(Function<DeedPedestalScreen, List<AbstractWidget>> init, int widgetAmountX) {
            this.init = init;
            this.widgetAmountX = widgetAmountX;
        }

        private static List<AbstractWidget> createPlayerRow(DeedPedestalScreen screen, DeedStorage.PlayerSettings settings, List<ProperToggleWidget> allPlaceBreak, List<ProperToggleWidget> allLoyalAttack, List<ProperToggleWidget> allContainers) {
            DeedStorage.Claim claim = screen.getMenu().tile.getClaim();
            List<AbstractWidget> widgets = new ArrayList<>();
            widgets.add(new TextWidget(0, 0, 148, 22, new TextComponent(settings.name)));
            ProperToggleWidget w;
            widgets.add(w = new ProperToggleWidget(0, 0, 22, 22, 100, 167, 22, 0, TEXTURE, "info." + CraftableDeeds.ID + ".can_place_break", settings.canPlaceBreak, v -> {
                settings.canPlaceBreak = v;
                PacketHandler.sendPlayerSettings(settings, claim);
            }));
            allPlaceBreak.add(w);
            if (!settings.isFake) {
                widgets.add(w = new ProperToggleWidget(0, 0, 22, 22, 100, 190, 22, 0, TEXTURE, "info." + CraftableDeeds.ID + ".loyal_mobs_attack", settings.loyalMobsAttack, v -> {
                    settings.loyalMobsAttack = v;
                    PacketHandler.sendPlayerSettings(settings, claim);
                }));
                if (allLoyalAttack != null)
                    allLoyalAttack.add(w);
                widgets.add(w = new ProperToggleWidget(0, 0, 22, 22, 100, 213, 22, 0, TEXTURE, "info." + CraftableDeeds.ID + ".can_open_containers", settings.canOpenContainers, v -> {
                    settings.canOpenContainers = v;
                    PacketHandler.sendPlayerSettings(settings, claim);
                }));
                if (allContainers != null)
                    allContainers.add(w);
            }
            return widgets;
        }
    }

}
