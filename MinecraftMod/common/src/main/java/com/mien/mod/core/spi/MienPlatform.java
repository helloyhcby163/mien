package com.mien.mod.core.spi;

import java.nio.file.Path;

/**
 * 平台适配入口。由各 target 模块实现，并通过 ServiceLoader 注册：
 * META-INF/services/com.mien.mod.core.spi.MienPlatform。
 */
public interface MienPlatform {

    /**
     * 游戏目录下的 config 文件夹（通常为 run/config）。
     * 具体路径由平台决定，common 只用它拼接 mien_config.json。
     */
    Path configDirectory();

    /**
     * 当前平台的嘴巴渲染器。
     */
    MouthRenderer mouthRenderer();

    /**
     * 打开调参界面（由平台在客户端线程创建 Screen）。
     */
    void openConfigScreen();
}
