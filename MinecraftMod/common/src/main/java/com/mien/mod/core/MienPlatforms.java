package com.mien.mod.core;

import com.mien.mod.core.spi.MienPlatform;

import java.util.ServiceLoader;

/**
 * 通过 ServiceLoader 解析平台实现，避免 common 依赖任何加载器 API。
 * <p>
 * 每个 target 在自己的 jar 内提供
 * META-INF/services/com.mien.mod.core.spi.MienPlatform。
 */
public final class MienPlatforms {
    private static boolean searched;
    private static MienPlatform platform;

    private MienPlatforms() {
    }

    /**
     * 查找平台实现；未注册时返回 null，调用方必须回退到内置逻辑。
     */
    public static synchronized MienPlatform find() {
        if (!searched) {
            searched = true;
            for (MienPlatform candidate : ServiceLoader.load(
                    MienPlatform.class, MienPlatform.class.getClassLoader())) {
                platform = candidate;
                System.out.println("[Mien] platform loaded: " + candidate.getClass().getName());
                break;
            }
            if (platform == null) {
                System.err.println("[Mien] WARNING: 未找到 MienPlatform 的 ServiceLoader 实现，使用回退逻辑。");
            }
        }
        return platform;
    }

    /**
     * 必须存在的平台实现；缺失时抛出异常，便于开发期尽早暴露注册遗漏。
     */
    public static MienPlatform get() {
        MienPlatform found = find();
        if (found == null) {
            throw new IllegalStateException(
                    "未找到 MienPlatform 实现，请检查 target 的 META-INF/services/com.mien.mod.core.spi.MienPlatform");
        }
        return found;
    }
}
