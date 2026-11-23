package com.mien.mod.fabric;

import com.mien.mod.core.AnimationStateMachine;
import com.mien.mod.core.MienConfig;
import com.mien.mod.core.MienPlatforms;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * HUD 预览入口：只计算位置并调用 MienPlatform.mouthRenderer()，不直接依赖具体渲染实现。
 */
public final class MienFabricHud implements HudRenderCallback {
    private static final int SCALE = 8;
    private static final int PREVIEW_MARGIN = 10;

    private MienFabricHud() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register(new MienFabricHud());
    }

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        MienConfig config = MienConfig.getInstance();
        if (config == null || config.face == null) {
            return;
        }

        int previewSize = config.face.size * SCALE;
        int previewX = drawContext.getScaledWindowWidth() - previewSize - PREVIEW_MARGIN;
        int previewY = drawContext.getScaledWindowHeight() - previewSize - PREVIEW_MARGIN;

        MienPlatforms.get().mouthRenderer().renderFacePreview(
                drawContext, previewX, previewY, previewSize, AnimationStateMachine.currentState);
    }
}
