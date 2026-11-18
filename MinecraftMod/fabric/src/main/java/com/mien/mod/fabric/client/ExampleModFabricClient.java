package com.mien.mod.fabric.client;

import com.mien.mod.AnimationStateMachine;
import com.mien.mod.fabric.MienFabric3DRenderer;
import com.mien.mod.fabric.MienFabricHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class ExampleModFabricClient implements ClientModInitializer {
    private static final KeyBinding CYCLE_STATE_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.mien.cycle_state", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_M, "Mien")
    );

    private static final KeyBinding HOLD_STATE_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.mien.hold_state", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "Mien")
    );

    private static final KeyBinding TOGGLE_STATE_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.mien.toggle_state", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "Mien")
    );

    private static final KeyBinding OPEN_CONFIG_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.mien.open_config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "Mien")
    );

    private static boolean holdActive;

    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        MienFabricHud.register();
        MienFabric3DRenderer.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (CYCLE_STATE_KEY.wasPressed()) {
                AnimationStateMachine.cycle();
            }
            while (TOGGLE_STATE_KEY.wasPressed()) {
                AnimationStateMachine.toggle();
            }
            while (OPEN_CONFIG_KEY.wasPressed()) {
                client.setScreen(new MienConfigScreen());
            }

            // HOLD：按住 H 张嘴，松开恢复 flat。
            boolean holdDown = client.currentScreen == null && HOLD_STATE_KEY.isPressed();
            if (holdDown != holdActive) {
                holdActive = holdDown;
                if (holdDown) {
                    AnimationStateMachine.onHoldStart();
                } else {
                    AnimationStateMachine.onHoldEnd();
                }
            }
        });
    }
}
