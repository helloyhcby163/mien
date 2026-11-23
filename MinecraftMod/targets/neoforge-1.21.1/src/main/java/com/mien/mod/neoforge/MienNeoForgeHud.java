package com.mien.mod.neoforge;

import com.mien.mod.core.AnimationStateMachine;
import com.mien.mod.core.MienConfig;
import com.mien.mod.core.MienPlatforms;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * HUD 预览入口：只计算位置并调用 MienPlatform.mouthRenderer()，不直接依赖具体渲染实现。
 */
public final class MienNeoForgeHud {
    private static final Identifier LAYER_ID = Identifier.of("mien", "mouth_preview");
    private static final int SCALE = 8;
    private static final int PREVIEW_MARGIN = 10;

    private MienNeoForgeHud() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(MienNeoForgeHud::onRegisterGuiLayers);
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, (context, tickCounter) -> render(context));
    }

    private static void render(DrawContext context) {
        MienConfig config = MienConfig.getInstance();
        if (config == null || config.face == null) {
            return;
        }

        int previewSize = config.face.size * SCALE;
        int previewX = context.getScaledWindowWidth() - previewSize - PREVIEW_MARGIN;
        int previewY = context.getScaledWindowHeight() - previewSize - PREVIEW_MARGIN;

        MienPlatforms.get().mouthRenderer().renderFacePreview(
                context, previewX, previewY, previewSize, AnimationStateMachine.currentState);
    }
}
