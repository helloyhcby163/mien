package com.mien.mod.fabric;

import com.mien.mod.core.AnimationStateMachine;
import com.mien.mod.core.MienPlatforms;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;

/**
 * 把嘴巴贴图贴在玩家头部模型的正前方。
 * 只负责注册、可见性判断与头部旋转；绘制入口统一走 MienPlatform.mouthRenderer()。
 */
public final class MienFabric3DRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

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

        matrices.push();
        // 进入头部部件局部空间，跟随头部俯仰与左右转动。
        this.getContextModel().head.rotate(matrices);
        MienPlatforms.get().mouthRenderer().renderHeadMouth(
                matrices, vertexConsumers, light, AnimationStateMachine.currentState);
        matrices.pop();
    }
}
