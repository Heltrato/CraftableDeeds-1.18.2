package de.ellpeck.craftabledeeds.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class DeedPedestalRenderer implements BlockEntityRenderer<DeedPedestalTileEntity> {
    private final BlockEntityRendererProvider.Context cont;

    public DeedPedestalRenderer(BlockEntityRendererProvider.Context context) {
        this.cont = context;
    }

    @Override
    public void render(DeedPedestalTileEntity tile, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        MapItemSavedData mapdata = tile.getMapData();
        if (mapdata != null) {
            matrixStackIn.translate(0.5F, 1.5F, 0.5F);
            matrixStackIn.translate(0, Math.sin((tile.getLevel().getGameTime() + partialTicks) / 15) * 0.05F, 0);
            matrixStackIn.mulPose(this.cont.getBlockEntityRenderDispatcher().camera.rotation());
            matrixStackIn.mulPose(Vector3f.ZP.rotationDegrees(180.0F));
            matrixStackIn.scale(0.0078125F, 0.0078125F, 0.0078125F);
            matrixStackIn.translate(-64.0D, -64.0D, 0.0D);
            int mapID = tile.getMapId();
            Minecraft.getInstance().gameRenderer.getMapRenderer().render(matrixStackIn, bufferIn, mapID, mapdata, true, combinedLightIn);
        }
    }
}
