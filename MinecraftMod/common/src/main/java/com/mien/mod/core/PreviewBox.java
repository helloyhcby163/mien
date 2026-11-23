package com.mien.mod.core;

/**
 * HUD / GUI 预览区域中嘴巴的屏幕矩形，单位为整数像素。
 * left / top 为左上角，width / height 为绘制尺寸。
 */
public record PreviewBox(int left, int top, int width, int height) {
}
