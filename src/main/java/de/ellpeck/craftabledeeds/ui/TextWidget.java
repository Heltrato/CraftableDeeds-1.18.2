package de.ellpeck.craftabledeeds.ui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;

public class TextWidget extends Widget {

    public TextWidget(int x, int y, int width, int height, ITextComponent title) {
        super(x, y, width, height, title);
    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        FontRenderer font = Minecraft.getInstance().fontRenderer;
        IReorderingProcessor processor = this.getMessage().func_241878_f();
        font.func_238422_b_(matrixStack, processor, this.x + this.width / 2F - font.func_243245_a(processor) / 2F, this.y + (this.height - 8) / 2F, 4210752);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }
}
