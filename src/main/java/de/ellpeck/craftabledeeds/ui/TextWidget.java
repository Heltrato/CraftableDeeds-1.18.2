package de.ellpeck.craftabledeeds.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public class TextWidget extends AbstractWidget {

    public TextWidget(int x, int y, int width, int height, Component title) {
        super(x, y, width, height, title);
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        Font font = Minecraft.getInstance().font;
        FormattedCharSequence processor = this.getMessage().getVisualOrderText();
        font.draw(matrixStack, processor, this.x + this.width / 2F - font.width(processor) / 2F, this.y + (this.height - 8) / 2F, 4210752);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {

    }
}
