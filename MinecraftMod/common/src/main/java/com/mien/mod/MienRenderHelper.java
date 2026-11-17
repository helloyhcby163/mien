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

        renderFaceAndMouth(drawContext, minecraftClient, previewX, previewY, previewSize, currentState,
                config.mouth_offset_x, config.mouth_offset_y, config.getMouthScale());
    }

    /**
     * 在指定位置绘制放大版脸部与当前嘴巴贴图。
     * 偏移量以皮肤脸部像素为单位，缩放围绕脸部中心。
     */
    public static void renderFaceAndMouth(DrawContext drawContext, MinecraftClient minecraftClient,
                                          int previewX, int previewY, int previewSize, String state,
                                          float offsetX, float offsetY, float scale) {
        MienConfig config = MienConfig.getInstance();
        if (config == null || config.face == null || minecraftClient.player == null) {
            return;
        }

        SkinTextures skin = minecraftClient.getSkinProvider().getSkinTextures(minecraftClient.player.getGameProfile());
        if (skin == null) {
            return;
        }

        // 脸部皮肤预览。
        drawContext.drawTexture(
                skin.texture(),
                previewX, previewY,
                previewSize, previewSize,
                (float) config.face.x, (float) config.face.y,
                config.face.size, config.face.size,
                SKIN_TEXTURE_SIZE, SKIN_TEXTURE_SIZE
        );

        // 叠加当前状态的嘴巴贴图（贴图为 256x256 的整脸画布，带透明背景）。
        if (!hasMouthTexture(minecraftClient, state)) {
            return;
        }

        float pixelScale = previewSize / (float) config.face.size;
        int mouthSize = Math.round(previewSize * scale);
        int centerX = previewX + previewSize / 2 + Math.round(offsetX * pixelScale);
        int centerY = previewY + previewSize / 2 + Math.round(offsetY * pixelScale);
        int mouthX = centerX - mouthSize / 2;
        int mouthY = centerY - mouthSize / 2;

        drawContext.drawTexture(
                getMouthTexture(state),
                mouthX, mouthY,
                mouthSize, mouthSize,
                0.0F, 0.0F,
                MOUTH_TEXTURE_SIZE, MOUTH_TEXTURE_SIZE,
                MOUTH_TEXTURE_SIZE, MOUTH_TEXTURE_SIZE
        );
    }
}
