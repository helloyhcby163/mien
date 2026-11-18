package com.mien.mod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户端渲染逻辑。参数使用 Object 接收，由平台模块传入 DrawContext / MinecraftClient，
 * 避免在调用处暴露客户端类型。
 */
public final class MienRenderHelper {
    public static String currentState = "flat";

    private static final int SCALE = 8;
    private static final int PREVIEW_MARGIN = 10;
    private static final int SKIN_TEXTURE_SIZE = 64;
    private static final int MOUTH_TEXTURE_SIZE = 256;

    private static final Map<Identifier, Boolean> TEXTURE_CACHE = new HashMap<>();

    private MienRenderHelper() {
    }

    /**
     * 根据状态名拼接嘴巴贴图路径。
     */
    public static Identifier getMouthTexture(String state) {
        return Identifier.of("mien", "textures/gui/mouth_" + state + ".png");
    }

    /**
     * 取出某个状态在 JSON 中专属的 x / y / w / h 等参数，状态不存在时返回 null。
     */
    public static MienConfig.StateData getStateData(String state) {
        MienConfig config = MienConfig.getInstance();
        if (config == null || config.states == null) {
            return null;
        }
        return config.states.get(state);
    }

    /**
     * 检查贴图是否真实存在（结果缓存），缺失时打印一次实际拼接出的路径。
     */
    public static boolean hasMouthTexture(MinecraftClient minecraftClient, String state) {
        Identifier texture = getMouthTexture(state);
        Boolean exists = TEXTURE_CACHE.get(texture);
        if (exists == null) {
            exists = minecraftClient.getResourceManager().getResource(texture).isPresent();
            TEXTURE_CACHE.put(texture, exists);
            if (!exists) {
                System.out.println("[Mien] missing texture: " + texture);
            }
        }
        return exists;
    }

    public static void renderMouthPreview(Object context, Object client) {
        if (!(context instanceof DrawContext drawContext) || !(client instanceof MinecraftClient minecraftClient)) {
            return;
        }

        MienConfig config = MienConfig.getInstance();
        if (config == null || config.face == null) {
            return;
        }

        int previewSize = config.face.size * SCALE;
        int previewX = drawContext.getScaledWindowWidth() - previewSize - PREVIEW_MARGIN;
        int previewY = drawContext.getScaledWindowHeight() - previewSize - PREVIEW_MARGIN;

        renderFaceAndMouth(drawContext, minecraftClient, previewX, previewY, previewSize, currentState);
    }

    /**
     * 在指定位置绘制放大版脸部与当前状态的嘴巴。
     * 嘴巴使用 StateData 里的 x / y（64x64 皮肤空间中的中心）与 w / h（皮肤像素尺寸）定位，
     * 整张 256x256 嘴巴画布被拉伸到该框内，并裁剪在脸部预览区域内，避免溢出到黑边。
     */
    public static void renderFaceAndMouth(DrawContext drawContext, MinecraftClient minecraftClient,
                                          int previewX, int previewY, int previewSize, String state) {
        MienConfig config = MienConfig.getInstance();
        if (config == null || config.face == null || minecraftClient.player == null) {
            return;
        }

        SkinTextures skin = minecraftClient.getSkinProvider().getSkinTextures(minecraftClient.player.getGameProfile());
        if (skin == null) {
            return;
        }

        // 脸部预览区域就是裁剪区域，嘴巴不允许溢出到脸部之外。
        drawContext.enableScissor(previewX, previewY, previewX + previewSize, previewY + previewSize);

        // 脸部皮肤预览。
        drawContext.drawTexture(
                skin.texture(),
                previewX, previewY,
                previewSize, previewSize,
                (float) config.face.x, (float) config.face.y,
                config.face.size, config.face.size,
                SKIN_TEXTURE_SIZE, SKIN_TEXTURE_SIZE
        );

        MienConfig.StateData stateData = getStateData(state);
        if (stateData == null || !hasMouthTexture(minecraftClient, state)) {
            drawContext.disableScissor();
            return;
        }

        float pixelScale = previewSize / (float) config.face.size;
        int boxWidth = Math.max(1, Math.round(stateData.w * pixelScale));
        int boxHeight = Math.max(1, Math.round(stateData.h * pixelScale));
        int centerX = previewX + Math.round((stateData.x - config.face.x) * pixelScale);
        int centerY = previewY + Math.round((stateData.y - config.face.y) * pixelScale);

        // 贴图 256x256 以嘴巴为中心，整张画布拉伸到 w x h 的框里（与 Python 生成时的比例一致）。
        drawContext.drawTexture(
                getMouthTexture(state),
                centerX - boxWidth / 2, centerY - boxHeight / 2,
                boxWidth, boxHeight,
                0.0F, 0.0F,
                MOUTH_TEXTURE_SIZE, MOUTH_TEXTURE_SIZE,
                MOUTH_TEXTURE_SIZE, MOUTH_TEXTURE_SIZE
        );

        drawContext.disableScissor();
    }
}
