package com.mien.mod.fabric;

import com.mien.mod.MienConfig;
import com.mien.mod.MienRenderHelper;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

/**
 * 把嘴巴贴图贴在玩家头部模型的正前方。
 * 作为玩家渲染器的 FeatureRenderer 注册，渲染时矩阵处于实体模型空间。
 */
public final class MienFabric3DRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    // 头部方块在模型像素空间：x/z 为 -4..4，y 为 -8..0（y 轴向下），脸部正前方为 z = -4 像素。
    private static final float FACE_Z = 4.0F;
    // 原版 ModelPart$Cuboid 构建顶点时统一除以 16，这里用同样的比例把像素坐标换算为方块坐标。
    private static final float MODEL_SCALE = 1.0F / 16.0F;
    // 照抄原版 CapeFeatureRenderer：matrices.translate(0.0F, 0.0F, 0.125F) 的深度外移量（方块单位）。
    private static final float DEPTH_OFFSET = 0.125F;

    private static String lastLogged = "";

    private MienFabric3DRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    public static void register() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityType == EntityType.PLAYER && entityRenderer instanceof PlayerEntityRenderer playerRenderer) {
                registrationHelper.register(new MienFabric3DRenderer(playerRenderer));
            }
        });
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity entity,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        String state = MienRenderHelper.currentState;
        if (!MienRenderHelper.hasMouthTexture(MinecraftClient.getInstance(), state)) {
            return;
        }

        MienConfig config = MienConfig.getInstance();
        MienConfig.StateData data = MienRenderHelper.getStateData(state);
        if (config == null || config.face == null || data == null) {
            return;
        }

        // 皮肤 64x64 空间的 x / y 映射到头部模型像素空间：
        // 脸部左/上边缘 face.x / face.y 对应模型 x = -size/2、y = -size，幅度不变。
        float centerX = (data.x - config.face.x) - config.face.size / 2.0F;
        float centerY = (data.y - config.face.y) - config.face.size;
        float halfWidth = data.w / 2.0F;
        float halfHeight = data.h / 2.0F;

        Identifier texture = MienRenderHelper.getMouthTexture(state);
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(texture));

        matrices.push();
        // 进入头部部件局部空间，跟随头部俯仰与左右转动。
        this.getContextModel().head.rotate(matrices);

        // 先平移到嘴巴中心，再单独沿 -Z 外移；深度不参与任何缩放，避免缩放后陷进头部导致消失。
        matrices.translate(centerX * MODEL_SCALE, centerY * MODEL_SCALE, -(FACE_Z * MODEL_SCALE + DEPTH_OFFSET));

        MatrixStack.Entry entry = matrices.peek();
        Matrix4f positionMatrix = entry.getPositionMatrix();
        float halfW = halfWidth * MODEL_SCALE;
        float halfH = halfHeight * MODEL_SCALE;

        if (!lastLogged.equals(state + data.x + "," + data.y + "," + data.w + "," + data.h)) {
            lastLogged = state + data.x + "," + data.y + "," + data.w + "," + data.h;
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
