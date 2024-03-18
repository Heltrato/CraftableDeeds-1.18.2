package de.ellpeck.craftabledeeds.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.function.Consumer;

import net.minecraft.client.gui.screens.Screen;

// the default toggle widget doesn't actually get toggled on click because why would it
// the recipe book just iterates all of its toggle widgets on click instead... why
public class ProperToggleWidget extends StateSwitchingButton {

    private final Consumer<Boolean> onToggled;
    private final String tooltipKey;

    public ProperToggleWidget(int xIn, int yIn, int widthIn, int heightIn, int xTexStartIn, int yTexStartIn, int xDiffTexIn, int yDiffTexIn, ResourceLocation resourceLocationIn, String tooltipKey, boolean triggered, Consumer<Boolean> onToggled) {
        super(xIn, yIn, widthIn, heightIn, triggered);
        this.initTextureValues(xTexStartIn, yTexStartIn, xDiffTexIn, yDiffTexIn, resourceLocationIn);
        this.tooltipKey = tooltipKey;
        this.onToggled = onToggled;
        this.setMessage(new TranslatableComponent(this.tooltipKey + "_" + this.isStateTriggered));
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.setStateTriggered(!this.isStateTriggered);
    }

    @Override
    public void setStateTriggered(boolean triggered) {
        super.setStateTriggered(triggered);
        this.onToggled.accept(this.isStateTriggered);
        this.setMessage(new TranslatableComponent(this.tooltipKey + "_" + this.isStateTriggered));
    }

    @Override
    public void renderToolTip(PoseStack matrixStack, int mouseX, int mouseY) {
        //DeedPedestalScreen.this.renderTooltip(matrixStack, Collections.singletonList(this.getMessage()), mouseX, mouseY, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Minecraft.getInstance().font);
    }
}
