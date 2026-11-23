package com.mien.mod.core.spi;

/**
 * 嘴巴渲染抽象。实现放在各 target 模块（fabric / neoforge），
 * common 只依赖本接口，绝不引用 net.minecraft.* 或加载器 API。
 * <p>
 * 平台类型（DrawContext / MatrixStack / VertexConsumerProvider）统一用 {@link Object} 传入，
 * 由实现类自行强转。
 */
public interface MouthRenderer {

    /**
     * 在 GUI / HUD 上绘制放大版脸部与指定状态的嘴巴。
     *
     * @param drawContext 平台绘制上下文（如 1.21.1 的 DrawContext）
     * @param previewX    预览区域左上角 X（屏幕坐标）
     * @param previewY    预览区域左上角 Y（屏幕坐标）
     * @param previewSize 预览区域边长（像素）
     * @param state       状态名，如 flat / smile / open
     */
    void renderFacePreview(Object drawContext, int previewX, int previewY, int previewSize, String state);

    /**
     * 把指定状态的嘴巴贴图渲染到玩家头部模型正前方。
     *
     * @param matrixStack     当前实体渲染矩阵栈（如 1.21.1 的 MatrixStack）
     * @param vertexConsumers 顶点消费者提供者（如 VertexConsumerProvider）
     * @param packedLight     打包光照值
     * @param state           状态名
     */
    void renderHeadMouth(Object matrixStack, Object vertexConsumers, int packedLight, String state);

    /**
     * 指定状态的嘴巴贴图是否真实存在（缺失时应静默跳过渲染）。
     */
    boolean isTextureAvailable(String state);
}
