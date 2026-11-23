package com.mien.mod.neoforge;

import com.mien.mod.core.AnimationStateMachine;
import com.mien.mod.core.MienPlatforms;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 把嘴巴贴图贴在玩家头部模型的正前方。
 * 只负责注册、可见性判断与头部旋转；绘制入口统一走 MienPlatform.mouthRenderer()。
 */
public final class MienNeoForge3DRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    private MienNeoForge3DRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(MienNeoForge3DRenderer::onAddLayers);
    }

    private static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (SkinTextures.Model model : event.getSkins()) {
            PlayerEntityRenderer renderer = event.getSkin(model);
            if (renderer != null) {
                renderer.addFeature(new MienNeoForge3DRenderer(renderer));
            }
        }
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
