package com.mien.mod.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mien.mod.core.spi.MienPlatform;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class MienConfig {
    public int skin_scale;
    public FaceData face;
    public int[] color;
    public Map<String, StateData> states;
    public float mouth_offset_x;
    public float mouth_offset_y;
    public float mouth_scale;

    public static class FaceData {
        public int x, y, size;
    }

    /**
     * 单个嘴部状态在 64x64 皮肤空间中的几何参数：
     * x / y 为嘴巴中心，w / h 为皮肤像素尺寸。语义不允许改变。
     */
    public static class StateData {
        public float x, y, w, h, curve, open_h, thickness;
    }

    private static final String FILE_NAME = "mien_config.json";

    /**
     * state key 会直接拼进贴图路径 textures/gui/mouth_&lt;state&gt;.png，
     * 只允许小写字母、数字、下划线与连字符，非法 key 一律丢弃。
     */
    private static final Pattern STATE_KEY_PATTERN = Pattern.compile("[a-z0-9_-]+");

    /**
     * 配置文件路径：优先使用平台注入的 config 目录（旧路径 config/mien_config.json 不变）；
     * 平台实现尚未注册时回退到相对路径，保持旧行为。
     */
    private static Path configFile() {
        MienPlatform platform = MienPlatforms.find();
        if (platform != null && platform.configDirectory() != null) {
            return platform.configDirectory().resolve(FILE_NAME);
        }
        return Paths.get("config", FILE_NAME);
    }

    /**
     * 缩放值缺失（0）时回退到 1.0，避免贴图被缩成不可见。
     */
    public float getMouthScale() {
        return mouth_scale <= 0.0F ? 1.0F : mouth_scale;
    }

    private static MienConfig INSTANCE;

    public static MienConfig getInstance() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    /**
     * 读取配置：本地文件优先，解析失败或内容不完整时回退到内置默认配置，保证游戏绝不崩溃。
     */
    public static void load() {
        MienConfig loaded = readFile();
        if (loaded != null) {
            loaded.repairStates();
        }
        if (loaded == null || !loaded.isValid()) {
            System.err.println("[Mien] WARNING: config/mien_config.json 缺失或格式损坏，已回退到内置默认配置。");
            loaded = readDefault();
            if (loaded != null) {
                loaded.repairStates();
            }
        }
        if (loaded == null || !loaded.isValid()) {
            System.err.println("[Mien] WARNING: 内置默认配置不可用，使用最简内置参数继续运行。");
            loaded = createFallback();
        }
        INSTANCE = loaded;
    }

    /**
     * 逐条修复 states：非法 key 丢弃，值为 null 的条目用内置默认值填充，其余条目原样保留，
     * 不再因为单个坏条目丢弃整份用户配置。
     */
    private void repairStates() {
        if (states == null) {
            return;
        }
        Map<String, StateData> repaired = new LinkedHashMap<>();
        List<String> droppedKeys = new ArrayList<>();
        List<String> resetKeys = new ArrayList<>();
        MienConfig defaults = null;

        for (Map.Entry<String, StateData> entry : states.entrySet()) {
            String key = entry.getKey();
            if (key == null || !STATE_KEY_PATTERN.matcher(key).matches()) {
                droppedKeys.add(key);
                continue;
            }
            StateData data = entry.getValue();
            if (data == null) {
                if (defaults == null) {
                    defaults = readDefault();
                }
                StateData fallback = defaults == null || defaults.states == null
                        ? null : defaults.states.get(key);
                data = copyState(fallback);
                if (data == null) {
                    data = createFlatState();
                }
                resetKeys.add(key);
            }
            repaired.put(key, data);
        }

        states = repaired;
        if (!droppedKeys.isEmpty()) {
            System.err.println("[Mien] WARNING: 丢弃非法 state key（只允许小写字母、数字、下划线与连字符）: "
                    + sanitizeForLog(droppedKeys));
        }
        if (!resetKeys.isEmpty()) {
            System.err.println("[Mien] WARNING: 以下 state 数据为空，已重置为默认值: " + sanitizeForLog(resetKeys));
        }
    }

    private static StateData copyState(StateData source) {
        if (source == null) {
            return null;
        }
        StateData copy = new StateData();
        copy.x = source.x;
        copy.y = source.y;
        copy.w = source.w;
        copy.h = source.h;
        copy.curve = source.curve;
        copy.open_h = source.open_h;
        copy.thickness = source.thickness;
        return copy;
    }

    /**
     * 兜底用的 flat 参数，避免修复后的 state 变成 null。
     */
    private static StateData createFlatState() {
        StateData flat = new StateData();
        flat.x = 10.9F;
        flat.y = 14.2F;
        flat.w = 2.05F;
        flat.h = 2.5F;
        return flat;
    }

    /**
     * 日志消毒：非可打印 ASCII 字符替换为 '?'，并截断超长 key，防止日志被污染。
     */
    private static String sanitizeForLog(List<String> keys) {
        StringBuilder result = new StringBuilder();
        for (String key : keys) {
            if (result.length() > 0) {
                result.append(", ");
            }
            if (key == null) {
                result.append("<null>");
                continue;
            }
            int limit = Math.min(key.length(), 64);
            for (int i = 0; i < limit; i++) {
                char c = key.charAt(i);
                result.append(c >= 0x20 && c <= 0x7E ? c : '?');
            }
            if (key.length() > limit) {
                result.append("...");
            }
        }
        return result.toString();
    }

    private static MienConfig readFile() {
        if (!Files.isRegularFile(configFile())) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(configFile(), StandardCharsets.UTF_8)) {
            return new Gson().fromJson(reader, MienConfig.class);
        } catch (Exception e) {
            System.err.println("[Mien] WARNING: 无法解析 " + configFile().toAbsolutePath() + " -> " + e);
            return null;
        }
    }

    private static MienConfig readDefault() {
        try {
            InputStream stream = MienConfig.class.getResourceAsStream("/assets/mien/default_config.json");
            if (stream != null) {
                try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    return new Gson().fromJson(reader, MienConfig.class);
                }
            }
        } catch (Exception e) {
            System.err.println("[Mien] WARNING: 读取内置默认配置失败 -> " + e);
        }
        return null;
    }

    private boolean isValid() {
        return face != null && face.size > 0
                && states != null && !states.isEmpty()
                && states.values().stream().allMatch(Objects::nonNull);
    }

    private static MienConfig createFallback() {
        MienConfig config = new MienConfig();
        config.skin_scale = 2;
        config.face = new FaceData();
        config.face.x = 8;
        config.face.y = 8;
        config.face.size = 8;
        config.color = new int[]{80, 40, 20};
        config.mouth_scale = 1.0F;
        config.states = new LinkedHashMap<>();
        config.states.put("flat", createFlatState());
        return config;
    }

    /**
     * 把当前内存中的参数写入游戏目录下的 config/mien_config.json。
     */
    public static void save() {
        MienConfig config = getInstance();
        if (config == null) {
            return;
        }
        try {
            Path parent = configFile().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(configFile(), StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(config, writer);
            }
            System.out.println("[Mien] config saved: " + configFile().toAbsolutePath());
        } catch (Exception e) {
            System.err.println("[Mien] WARNING: 保存配置失败 -> " + e);
        }
    }
}
