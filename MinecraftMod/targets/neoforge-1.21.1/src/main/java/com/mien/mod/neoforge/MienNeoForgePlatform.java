package com.mien.mod.neoforge;

import com.mien.mod.core.spi.MienPlatform;
import com.mien.mod.core.spi.MouthRenderer;
import com.mien.mod.neoforge.client.MienConfigScreen;
import net.minecraft.client.MinecraftClient;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

/**
 * NeoForge 平台实现，由 ServiceLoader 通过
 * META-INF/services/com.mien.mod.core.spi.MienPlatform 加载。
 */
public final class MienNeoForgePlatform implements MienPlatform {
    private final MouthRenderer mouthRenderer = new MienNeoForgeMouthRenderer();

    @Override
    public Path configDirectory() {
        // NeoForge 的 config 目录同样是游戏目录下的 config，与旧相对路径一致。
        return FMLPaths.CONFIGDIR.get();
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
