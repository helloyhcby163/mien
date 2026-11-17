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
import java.util.Map;

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

    public static void load() {
        // 优先读取游戏目录下的调参结果，其次读取 jar 内的默认配置。
        try {
            if (Files.isRegularFile(CONFIG_FILE)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
                    INSTANCE = new Gson().fromJson(reader, MienConfig.class);
                }
                if (INSTANCE != null) {
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            InputStream stream = MienConfig.class.getResourceAsStream("/assets/mien/default_config.json");
            if (stream != null) {
                try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    INSTANCE = new Gson().fromJson(reader, MienConfig.class);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            e.printStackTrace();
        }
    }
}
