package com.mien.mod.fabric;

import com.mien.mod.core.HeadQuad;
import com.mien.mod.core.MienConfig;
import com.mien.mod.core.MouthGeometry;
import com.mien.mod.core.PreviewBox;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Fabric 客户端渲染逻辑（HUD 预览）。参数使用 Object 接收 DrawContext / MinecraftClient，
 * 避免在调用处暴露客户端类型。
 */
public final class MienRenderHelper {
    private static final int SKIN_TEXTURE_SIZE = 64;
    private static final int MOUTH_TEXTURE_SIZE = 256;

    private static final Map<Identifier, Boolean> TEXTURE_CACHE = new HashMap<>();

    // 头部方块在模型像素空间：脸部正前方为 z = -4 像素。
    private static final float FACE_Z = 4.0F;
    // 原版 ModelPart$Cuboid 构建顶点时统一除以 16。
    private static final float MODEL_SCALE = 1.0F / 16.0F;
    // 照抄原版 CapeFeatureRenderer 的深度外移量。
    private static final float DEPTH_OFFSET = 0.125F;

    private static String lastLogged = "";

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
        // 用 try/finally 保证任何异常都不会把启用状态的 scissor 带出渲染回调。
        drawContext.enableScissor(previewX, previewY, previewX + previewSize, previewY + previewSize);
        try {
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
                return;
            }

            // HUD 预览框的换算集中在 common 的 MouthGeometry。
            PreviewBox box = MouthGeometry.previewBox(stateData, config.face, previewX, previewY, previewSize);

            // 贴图 256x256 以嘴巴为中心，整张画布拉伸到 w x h 的框里（与 Python 生成时的比例一致）。
            drawContext.drawTexture(
                    getMouthTexture(state),
                    box.left(), box.top(),
                    box.width(), box.height(),
                    0.0F, 0.0F,
                    MOUTH_TEXTURE_SIZE, MOUTH_TEXTURE_SIZE,
                    MOUTH_TEXTURE_SIZE, MOUTH_TEXTURE_SIZE
            );
        } finally {
            drawContext.disableScissor();
        }
    }

    /**
     * 把指定状态的嘴巴渲染到玩家头部模型正前方。
     * 调用前矩阵应已 push 并完成 head.rotate；本方法内部自行 push / pop。
     */
    public static void renderHeadMouth(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                       int light, String state) {
        if (!hasMouthTexture(MinecraftClient.getInstance(), state)) {
            return;
        }

        MienConfig config = MienConfig.getInstance();
        MienConfig.StateData data = getStateData(state);
        if (config == null || config.face == null || data == null) {
            return;
        }

        HeadQuad quad = MouthGeometry.headQuad(data, config.face);
        float centerX = quad.centerX();
        float centerY = quad.centerY();

        Identifier texture = getMouthTexture(state);
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(texture));

        matrices.push();
        // 先平移到嘴巴中心，再单独沿 -Z 外移；深度不参与任何缩放，避免缩放后陷进头部导致消失。
        matrices.translate(centerX * MODEL_SCALE, centerY * MODEL_SCALE, -(FACE_Z * MODEL_SCALE + DEPTH_OFFSET));

        MatrixStack.Entry entry = matrices.peek();
        Matrix4f positionMatrix = entry.getPositionMatrix();
        float halfW = quad.halfWidth() * MODEL_SCALE;
        float halfH = quad.halfHeight() * MODEL_SCALE;

        String signature = state + data.x + "," + data.y + "," + data.w + "," + data.h;
        if (!lastLogged.equals(signature)) {
            lastLogged = signature;
            System.out.println("[Mien] 3D state=" + state
                    + " x=" + data.x + " y=" + data.y + " w=" + data.w + " h=" + data.h
                    + " -> center=(" + centerX + ", " + centerY + ")px"
                    + " half=(" + halfW + ", " + halfH + ")block"
                    + " z=" + -(FACE_Z * MODEL_SCALE + DEPTH_OFFSET)
                    + " matrix=" + positionMatrix);
        }

        // 顶点顺序与 ModelPart$Cuboid 的 -Z（脸部）面一致：左上 → 右上 → 右下 → 左下。
        vertex(buffer, entry, positionMatrix, -halfW, -halfH, 0.0F, 0.0F, 0.0F, light);
        vertex(buffer, entry, positionMatrix, halfW, -halfH, 0.0F, 1.0F, 0.0F, light);
        vertex(buffer, entry, positionMatrix, halfW, halfH, 0.0F, 1.0F, 1.0F, light);
        vertex(buffer, entry, positionMatrix, -halfW, halfH, 0.0F, 0.0F, 1.0F, light);

        matrices.pop();
    }

    private static void vertex(VertexConsumer buffer, MatrixStack.Entry entry, Matrix4f positionMatrix,
                               float x, float y, float z, float u, float v, int light) {
        buffer.vertex(positionMatrix, x, y, z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, 0.0F, 0.0F, -1.0F);
    }
}
