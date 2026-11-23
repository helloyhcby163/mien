package com.mien.mod.neoforge;

import com.mien.mod.core.spi.MouthRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

/**
 * NeoForge 的嘴巴渲染实现。common 只看到 MouthRenderer 接口，
 * 平台类型在这里转换，绘制逻辑复用 MienRenderHelper。
 */
public final class MienNeoForgeMouthRenderer implements MouthRenderer {

    @Override
    public void renderFacePreview(Object drawContext, int previewX, int previewY, int previewSize, String state) {
        if (!(drawContext instanceof DrawContext context)) {
            return;
        }
        MienRenderHelper.renderFaceAndMouth(context, MinecraftClient.getInstance(),
                previewX, previewY, previewSize, state);
    }

    @Override
    public void renderHeadMouth(Object matrixStack, Object vertexConsumers, int packedLight, String state) {
        if (!(matrixStack instanceof MatrixStack matrices)
                || !(vertexConsumers instanceof VertexConsumerProvider providers)) {
            return;
        }
        MienRenderHelper.renderHeadMouth(matrices, providers, packedLight, state);
    }

    @Override
    public boolean isTextureAvailable(String state) {
        return MienRenderHelper.hasMouthTexture(MinecraftClient.getInstance(), state);
    }
}
