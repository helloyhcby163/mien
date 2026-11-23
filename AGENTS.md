你当前正在协助开发 Minecraft 1.21.1 跨平台模组 Mien（Architectury Loom + common/targets 分层 + ServiceLoader）。本文件是项目宪法：生成或修改任何代码前，必须先按下面的路径在本地项目中搜索并验证 API 与结构确实存在。
1. 项目速览与验证命令

    目标：在玩家原版皮肤上动态叠加和变形面部特征（嘴、眼）。

    代码根目录：MinecraftMod/。仓库根下的 generate.py / generate2.py / mouth_gui.py 是贴图与预设的辅助工具，不参与模组构建。

    构建（唯一可信验证）：
        cd MinecraftMod
        .\gradlew.bat clean build --console=plain
    必须 BUILD SUCCESSFUL，且 :common、:targets:fabric-1.21.1、:targets:neoforge-1.21.1 全部通过。

    客户端启动（旧任务名 :fabric:runClient / :neoforge:runClient 已不存在）：
        .\gradlew.bat :targets:fabric-1.21.1:runClient
        .\gradlew.bat :targets:neoforge-1.21.1:runClient

    运行日志：MinecraftMod/targets/<target>/run/logs/latest.log
    运行时配置：MinecraftMod/targets/<target>/run/config/mien_config.json

    当前唯一经过实际运行验收的是 Fabric 1.21.1；NeoForge 1.21.1 只保证编译与结构对齐。

2. 核心架构契约

    :common（MinecraftMod/common）
        纯 Java（java-library），包 com.mien.mod.core 与 com.mien.mod.core.spi。
        禁止出现任何 net.minecraft.*、net.fabricmc.*、net.neoforged.*，禁止应用 Loom 插件。
        唯一外部依赖：compileOnly com.google.code.gson:gson:2.10.1（Gson 由游戏运行时提供）。
        核心类：MienConfig、AnimationStateMachine、MouthGeometry、HeadQuad、PreviewBox、MienPlatforms、spi.MienPlatform、spi.MouthRenderer。

    targets（MinecraftMod/targets/fabric-1.21.1、MinecraftMod/targets/neoforge-1.21.1）
        只有这里可以应用 dev.architectury.loom / architectury-plugin，只有这里可以 import Minecraft 与加载器 API。
        事件注册、HUD、3D FeatureRenderer、MienConfigScreen、MienRenderHelper（具体绘制）、MienPlatform / MouthRenderer 实现都在这里。
        jar 名固定 mien-fabric / mien-neoforge：build.gradle 中用 archivesName 显式覆盖，不要依赖目录名。

    跨平台唯一通道：ServiceLoader
        common 只定义 spi.MienPlatform / spi.MouthRenderer。
        每个 target 必须提供 META-INF/services/com.mien.mod.core.spi.MienPlatform，内容为实现类全限定名。
        common 侧统一通过 MienPlatforms.find()（允许返回 null）或 MienPlatforms.get()（缺失即抛异常）获取。
        禁止反射、禁止自造平台接口、禁止 common 直接 new 平台类。

    UI 依赖方向（强约束）
        HUD、3D FeatureRenderer、MienConfigScreen 只能调用 MienPlatforms.get().mouthRenderer()。
        MienRenderHelper 只允许被 MienFabricMouthRenderer / MienNeoForgeMouthRenderer 引用。

    单一事实源
        配置：只通过 MienConfig.getInstance() 读取，target 不得另存配置副本。
        当前状态：AnimationStateMachine.currentState；GUI 切换状态时直接写它，HUD / 3D 每帧读取。
        几何换算：只允许用 MouthGeometry.headQuad / previewBox，target 不得重写公式。

    Gradle 结构
        根 settings.gradle 根据 gradle.properties 的 enabled_platforms + minecraft_version 动态 include targets/<loader>-<version>。
        根 build.gradle 的 subprojects 只做通用 Java 配置；Minecraft 依赖与 mappings 在各自 target 的 build.gradle。
        common 通过 implementation project(':common') + shadowBundle project(':common') 进入 target；生产 jar 内 asset 只能有一份。

3. 绝对雷区 (DO NOT)

    禁止硬编码绝对路径：Java 读资源用 getResourceAsStream 或 Identifier；配置文件用 MienPlatform.configDirectory()。仓库根 generate.py / generate2.py 仍写着 OUTPUT_DIR = r"E:\..."，修改这些脚本时必须改成基于 __file__ 的相对路径。

    禁止 common 依赖 Minecraft：新增 import 前确认不出现 net.minecraft.* / net.fabricmc.* / net.neoforged.*。

    禁止瞎猜 1.21.1 渲染 API：DrawContext / VertexConsumer / MatrixStack 的签名必须在本地依赖源码或现有类中查证后再写。贴图绘制参照现有 MienRenderHelper；3D 深度参照原版 CapeFeatureRenderer：脸部面在 z = -4 模型像素，最终平移 -(4/16F + 0.125F)，深度偏移不参与任何缩放。

    禁止把 head.rotate 移出 FeatureRenderer：旋转依赖 getContextModel().head，MienRenderHelper 拿不到模型。固定顺序：FeatureRenderer push → head.rotate → mouthRenderer().renderHeadMouth（内部 push / translate / 顶点 / pop）→ FeatureRenderer pop。

    禁止改变 StateData.x / y / w / h 语义：x / y 是 64x64 皮肤空间中嘴巴中心，w / h 是皮肤像素尺寸；256x256 贴图整张画布拉伸到 w×h 框内。改成左上角会让所有状态整体偏移 (w/2, h/2)。

    禁止 UI 直接调用 MienRenderHelper，禁止在 target 内复制几何或深度常量。

    禁止新增或改名状态却不配套资源：状态 key 必须同时存在于 default_config.json 的 states、AnimationStateMachine.CYCLE_ORDER（需要循环时）以及 assets/mien/textures/gui/mouth_<state>.png。缺贴图不会崩溃，只会打印 [Mien] missing texture 并整段跳过渲染。

    禁止删除或绕过配置容错：文件缺失 / JSON 解析失败必须回退内置 default_config.json，再失败必须 createFallback()，任何情况都不允许因配置问题让游戏崩溃。

    禁止无防护递归：任何目录递归（资源扫描、复制、打包）必须跳过符号链接、排除自身输出目录，并设置最大深度或已访问集合，避免死循环。

    禁止把 PyInstaller 运行期输出写进 sys._MEIPASS：该目录是只读临时解包目录，运行期输出必须落到用户选择的路径或用户主目录；只有读取打包资源时才使用 _MEIPASS。

    禁止 AI 执行任何 Git 命令：commit / amend / push / reset 一律由用户手动完成。

4. 资源与配置路径规范

    资源必须放在 common/src/main/resources/assets/mien/：
        assets/mien/default_config.json
        assets/mien/textures/gui/mouth_<state>.png（路径与文件名全部小写）
    贴图路径由 MienRenderHelper.getMouthTexture 统一拼接：Identifier.of("mien", "textures/gui/mouth_" + state + ".png")。

    内置默认配置读取：MienConfig.class.getResourceAsStream("/assets/mien/default_config.json")。

    运行时配置写入：MienPlatform.configDirectory().resolve("mien_config.json")，文件名与旧版一致。
        Fabric：FabricLoader.getInstance().getConfigDir()
        NeoForge：FMLPaths.CONFIGDIR.get()
        只有完全找不到平台实现时才回退 Paths.get("config", "mien_config.json")。
        保存后日志必须出现 [Mien] config saved: <绝对路径>。

    dev 资源可见性（loom.mods，必须存在）
        common 的 assets 不会自动成为 mod 根路径。每个 target 的 build.gradle 必须包含：
            loom {
                mods {
                    mien {
                        sourceSet sourceSets.main
                        sourceSet 'main', project(':common')
                    }
                }
            }
        漏掉的症状：生产 jar 里有贴图，但 runClient 里 HUD / GUI / 3D 全部空白，日志出现 [Mien] missing texture。

    MienConfig 的 skin_scale、color、mouth_offset_x、mouth_offset_y、mouth_scale 是保留字段，当前渲染未使用；实际调参以 states 的 x / y / w / h 为准。

    Python 辅助工具
        generate.py / generate2.py 负责生成 mouth_<state>.png；mouth_gui.py 负责导出预设 JSON。
        mouth_gui.py 的 x / y 是嘴巴框左上角，而模组 x / y 是中心：导入其 JSON 前必须换算 x += w/2、y += h/2，否则渲染结果会向左上偏移 (w/2, h/2)。
        mouth_gui.py 导出的 states 不含 skin_scale / mouth_scale 等字段，导入后需要补齐默认值。

    当前资源缺口（待“嘴部收尾”任务处理）
        mouth_smile_fang.png 不存在；badsmile、open_fang 有贴图但 default_config.json 没有对应 state；sad_fang 有贴图但不在 CYCLE_ORDER。循环到这些状态时不会显示嘴巴。

5. 排障导航

    runClient 中全部贴图消失、但生产 jar 正常：先查 target 的 loom.mods（见第 4 节），再查 MinecraftMod/.gradle/loom-cache/projects/targets/<target>/launch.cfg 中的 fabric.classPathGroups 是否包含 common\build\resources\main。

    单个状态不显示：日志搜索 [Mien] missing texture:，对照 assets/mien/textures/gui/ 与 default_config.json 的 states。

    ServiceLoader 未生效：日志搜索 [Mien] platform loaded:；缺失时检查 META-INF/services 文件、实现类全限定名，并执行 clean build。

    HUD / 3D 不显示：1. 坐标是否经 MouthGeometry 换算；2. 3D z 是否为 -(4/16F + 0.125F) 且未参与缩放；3. 贴图路径是否全小写；4. HUD 的 enableScissor / disableScissor 是否成对。

    GUI 有预览但世界里没有：检查 AnimationStateMachine.currentState；按住 H 后松开必然回到 flat。

    配置没落盘：搜 [Mien] config saved:；确认平台已注册；确认写的是 targets/<target>/run/config，而不是仓库根目录。

    Gradle Connection timed out：仓库镜像见根 build.gradle；先排查网络 / 代理，不要盲目升降依赖版本。

    游戏崩溃：查 targets/<target>/run/logs/latest.log，先看 Caused by: 的 NullPointerException / NoClassDefFoundError / NoSuchMethodError。

6. 开发规范与验证清单

    改代码前：先在本地依赖与现有类中搜索 API，不要凭记忆写 1.21.1 签名。

    改 Java 后：.\gradlew.bat clean build --console=plain 必须通过。只改资源或 services 文件也必须 clean（processResources 有缓存）。

    改渲染或键位后：必须 runClient 实测 M（循环）/ H（长按）/ J（切换）/ G（配置屏），并确认 HUD、GUI 左侧预览、3D 嘴部三处同步且一致。

    改资源后：确认 mien-fabric jar 中 assets/mien 只有一份，且 META-INF/services 文件存在。

    common 纯净性自检：Grep 关键字 net.minecraft 在 common/src 下必须 0 结果。

    修改状态集合时：同时更新 CYCLE_ORDER、default_config.json、贴图文件，并逐个循环确认每个状态都有贴图。

    唯一构建入口是 MinecraftMod 目录下的 gradlew.bat；不要用 IDE 的 Gradle Sync 代替 build 结论。
