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
    // 头部方块在模型像素空间：x/z 为 -4..4，y 为 -8..0（y 轴向下），脸部正前方为 z = -4。
    private static final float FACE_CENTER_Y = -4.0F;
    private static final float FACE_HALF_SIZE = 4.0F;
    private static final float FACE_Z = -4.0F;
    // 原版 ModelPart$Cuboid 构建顶点时统一除以 16，这里用同样的比例把像素坐标换算为方块坐标。
    private static final float MODEL_SCALE = 1.0F / 16.0F;
    // 0.1 像素（约 0.00625 方块）的外移量，足以避免与脸部皮肤 Z-fighting。
    private static final float FACE_OFFSET = 0.1F;

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
        if (!MienRenderHelper.hasMouthTexture(MinecraftClient.getInstance(), MienRenderHelper.currentState)) {
            return;
        }

        Identifier texture = MienRenderHelper.getMouthTexture(MienRenderHelper.currentState);
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(texture));

        matrices.push();
        // 进入头部部件局部空间，跟随头部俯仰与左右转动。
        this.getContextModel().head.rotate(matrices);

        // 来自调参界面的参数：偏移单位为皮肤像素，缩放围绕脸部中心。
        MienConfig config = MienConfig.getInstance();
        float scale = config == null ? 1.0F : config.getMouthScale();
        float offsetX = config == null ? 0.0F : config.mouth_offset_x;
        float offsetY = config == null ? 0.0F : config.mouth_offset_y;

        matrices.translate(0.0F, FACE_CENTER_Y * MODEL_SCALE, 0.0F);
        matrices.translate(offsetX * MODEL_SCALE, offsetY * MODEL_SCALE, 0.0F);
        matrices.scale(scale, scale, scale);

        MatrixStack.Entry entry = matrices.peek();
        Matrix4f positionMatrix = entry.getPositionMatrix();
        // 脸部所在平面为 z = -4 像素，再向外偏移 0.1 像素避免 Z-fighting。
        float z = (FACE_Z - FACE_OFFSET) * MODEL_SCALE;
        float half = FACE_HALF_SIZE * MODEL_SCALE;

        // 顶点顺序与 ModelPart$Cuboid 的 -Z（脸部）面一致：左上 → 右上 → 右下 → 左下。
        vertex(buffer, entry, positionMatrix, -half, -half, z, 0.0F, 0.0F, light);
        vertex(buffer, entry, positionMatrix, half, -half, z, 1.0F, 0.0F, light);
        vertex(buffer, entry, positionMatrix, half, half, z, 1.0F, 1.0F, light);
        vertex(buffer, entry, positionMatrix, -half, half, z, 0.0F, 1.0F, light);

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
