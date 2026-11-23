package com.mien.mod.core;

/**
 * 纯几何计算：把 64x64 皮肤空间的嘴巴参数换算为各渲染目标所需的坐标。
 * <p>
 * 语义（绝对保持不变）：
 * <ul>
 *   <li>{@code StateData.x / y}：嘴巴中心在 64x64 皮肤空间中的位置；</li>
 *   <li>{@code StateData.w / h}：嘴巴在皮肤上的像素尺寸；</li>
 *   <li>{@code FaceData.x / y / size}：脸部在皮肤上的左上角与边长。</li>
 * </ul>
 */
public final class MouthGeometry {

    private MouthGeometry() {
    }

    /**
     * 头部模型像素空间：脸部左/上边缘 face.x / face.y 对应模型 x = -size/2、y = -size，幅度不变。
     */
    public static HeadQuad headQuad(MienConfig.StateData state, MienConfig.FaceData face) {
        float centerX = (state.x - face.x) - face.size / 2.0F;
        float centerY = (state.y - face.y) - face.size;
        float halfWidth = state.w / 2.0F;
        float halfHeight = state.h / 2.0F;
        return new HeadQuad(centerX, centerY, halfWidth, halfHeight);
    }

    /**
     * HUD / GUI 预览屏幕坐标：以 model 中心为基准按像素缩放，宽高至少为 1 像素。
     */
    public static PreviewBox previewBox(MienConfig.StateData state, MienConfig.FaceData face,
                                        int previewX, int previewY, int previewSize) {
        float pixelScale = previewSize / (float) face.size;
        int width = Math.max(1, Math.round(state.w * pixelScale));
        int height = Math.max(1, Math.round(state.h * pixelScale));
        int centerX = previewX + Math.round((state.x - face.x) * pixelScale);
        int centerY = previewY + Math.round((state.y - face.y) * pixelScale);
        return new PreviewBox(centerX - width / 2, centerY - height / 2, width, height);
    }
}
