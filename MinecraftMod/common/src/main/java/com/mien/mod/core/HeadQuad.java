package com.mien.mod.core;

/**
 * 头部模型空间中的嘴巴矩形，单位为模型像素（1 像素 = 1/16 方块）。
 * centerX / centerY 为嘴巴中心，halfWidth / halfHeight 为半宽与半高。
 */
public record HeadQuad(float centerX, float centerY, float halfWidth, float halfHeight) {
}
