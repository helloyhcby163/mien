package com.mien.mod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

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

    public static class StateData {
        public float x, y, w, h, curve, open_h, thickness;
    }

    private static final Path CONFIG_FILE = Paths.get("config", "mien_config.json");

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
        if (loaded == null || !loaded.isValid()) {
            System.err.println("[Mien] WARNING: config/mien_config.json 缺失或格式损坏，已回退到内置默认配置。");
            loaded = readDefault();
        }
        if (loaded == null || !loaded.isValid()) {
            System.err.println("[Mien] WARNING: 内置默认配置不可用，使用最简内置参数继续运行。");
            loaded = createFallback();
        }
        INSTANCE = loaded;
    }

    private static MienConfig readFile() {
        if (!Files.isRegularFile(CONFIG_FILE)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
            return new Gson().fromJson(reader, MienConfig.class);
        } catch (Exception e) {
            System.err.println("[Mien] WARNING: 无法解析 " + CONFIG_FILE.toAbsolutePath() + " -> " + e);
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
        StateData flat = new StateData();
        flat.x = 10.9F;
        flat.y = 14.2F;
        flat.w = 2.05F;
        flat.h = 2.5F;
        config.states.put("flat", flat);
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
            Path parent = CONFIG_FILE.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(config, writer);
            }
            System.out.println("[Mien] config saved: " + CONFIG_FILE.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("[Mien] WARNING: 保存配置失败 -> " + e);
        }
    }
}
