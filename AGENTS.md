你当前正在协助开发 Minecraft 1.21.1 跨平台模组（基于 Architectury）。你必须绝对遵守以下铁律，在生成任何代码前，必须先在本地项目中搜索并验证相关 API 是否存在。
1. 项目速览与验证命令

    目标：在玩家原版皮肤上动态叠加和变形面部特征（嘴、眼）。

    核心验证：你每次修改 Java 代码后，必须主动运行 ./gradlew build 或触发 IDE 的 Gradle Sync。

    客户端启动：运行任务 :fabric:runClient 或 :neoforge:runClient。

2. 核心架构契约

    common 模块：只放纯逻辑、数据模型（如 Gson 配置解析）、接口定义。绝对禁止引入 net.minecraft.client 相关的渲染类。

    平台模块 (fabric / neoforge)：负责注册事件、渲染逻辑。渲染代码必须在对应平台模块中实现。

    单一事实源：所有配置参数由 common 模块的 MienConfig 类统一管理，其他模块只能通过 MienConfig.getInstance() 获取。

3. 绝对雷区 (DO NOT)

    禁止硬编码绝对路径：读取文件必须用 getResourceAsStream，绝对禁止使用 new File("E:/...")。

    禁止瞎猜渲染 API：1.21.1 的渲染 API 与旧版差异极大。如果不知道 DrawContext 或 VertexConsumer 怎么用，必须在本地项目中搜索现有的原版渲染代码（如按 Ctrl+Shift+F 搜索 drawTexture 或 GuiGraphics），照猫画虎。

    禁止跨模块死循环：common 不能依赖 fabric 或 neoforge。

4. 资源与配置路径规范

    JSON/贴图资源：必须放在 common/src/main/resources/assets/mien/ 目录下。

    本地代码加载：使用 MienConfig.class.getResourceAsStream("/assets/mien/default_config.json") 读取。

5. 排障导航

    Gradle 报错 Connection timed out：检查是否缺少阿里云镜像，提示用户配置网络或代理。

    HUD/3D 渲染不显示：1. 检查坐标换算；2. 检查是否传错了 Z 轴深度（Z-index）；3. 检查贴图路径是否全部小写。

    游戏崩溃：立刻查看 run/logs/latest.log，寻找 Caused by: java.lang.XXX，优先排查空指针（NullPointerException）或找不到类（NoClassDefFoundError）。