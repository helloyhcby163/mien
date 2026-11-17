package com.mien.mod.neoforge.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/**
 * NeoForge 客户端入口：注册 G 键并打开调参界面。
 */
public final class MienNeoForgeClient {
    private static final KeyBinding OPEN_CONFIG_KEY = new KeyBinding(
            "key.mien.open_config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "Mien"
    );

    private MienNeoForgeClient() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(MienNeoForgeClient::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(MienNeoForgeClient::onClientTick);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CONFIG_KEY);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        while (OPEN_CONFIG_KEY.wasPressed()) {
            MinecraftClient.getInstance().setScreen(new MienConfigScreen());
        }
    }
}
