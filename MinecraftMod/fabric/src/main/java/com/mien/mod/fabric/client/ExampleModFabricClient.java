package com.mien.mod.fabric.client;

import com.mien.mod.MienRenderHelper;
import com.mien.mod.fabric.MienFabric3DRenderer;
import com.mien.mod.fabric.MienFabricHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class ExampleModFabricClient implements ClientModInitializer {
    private static final List<String> STATE_ORDER = List.of(
            "flat", "smile", "sad", "open", "badsmile", "smile_fang", "open_fang");

    private static final KeyBinding CYCLE_STATE_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.mien.cycle_state", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_M, "Mien")
    );

    private static final KeyBinding OPEN_CONFIG_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.mien.open_config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "Mien")
    );

    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        MienFabricHud.register();
        MienFabric3DRenderer.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (CYCLE_STATE_KEY.wasPressed()) {
                int index = STATE_ORDER.indexOf(MienRenderHelper.currentState);
                MienRenderHelper.currentState = STATE_ORDER.get((index + 1) % STATE_ORDER.size());
                System.out.println("[Mien] current state: " + MienRenderHelper.currentState
                        + " -> " + MienRenderHelper.getMouthTexture(MienRenderHelper.currentState));
            }
            while (OPEN_CONFIG_KEY.wasPressed()) {
                client.setScreen(new MienConfigScreen());
            }
        });
    }
}
