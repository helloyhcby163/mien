package com.mien.mod.fabric;

import com.mien.mod.core.spi.MienPlatform;
import com.mien.mod.core.spi.MouthRenderer;
import com.mien.mod.fabric.client.MienConfigScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Path;

/**
 * Fabric 平台实现，由 ServiceLoader 通过
 * META-INF/services/com.mien.mod.core.spi.MienPlatform 加载。
 */
public final class MienFabricPlatform implements MienPlatform {
    private final MouthRenderer mouthRenderer = new MienFabricMouthRenderer();

    @Override
    public Path configDirectory() {
        // Fabric 的 config 目录就是游戏目录下的 config，与旧相对路径一致。
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public MouthRenderer mouthRenderer() {
        return mouthRenderer;
    }

    @Override
    public void openConfigScreen() {
        MinecraftClient.getInstance().setScreen(new MienConfigScreen());
    }
}
