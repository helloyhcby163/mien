package com.mien.mod.neoforge.client;

import com.mien.mod.core.AnimationStateMachine;
import com.mien.mod.core.MienPlatforms;
import com.mien.mod.neoforge.MienNeoForge3DRenderer;
import com.mien.mod.neoforge.MienNeoForgeHud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/**
 * NeoForge 客户端入口：注册状态按键与调参界面按键。
 */
public final class MienNeoForgeClient {
    private static final KeyBinding CYCLE_STATE_KEY = new KeyBinding(
            "key.mien.cycle_state", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_M, "Mien"
    );

    private static final KeyBinding HOLD_STATE_KEY = new KeyBinding(
            "key.mien.hold_state", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "Mien"
    );

    private static final KeyBinding TOGGLE_STATE_KEY = new KeyBinding(
            "key.mien.toggle_state", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "Mien"
    );

    private static final KeyBinding OPEN_CONFIG_KEY = new KeyBinding(
            "key.mien.open_config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "Mien"
    );

    private static boolean holdActive;

    private MienNeoForgeClient() {
    }

    /**
     * 客户端初始化唯一入口：只允许在 Dist.CLIENT 下调用，
     * HUD / 3D / 键位全部在这里注册，避免服务端加载任何客户端事件类。
     */
    public static void init(IEventBus modEventBus) {
        MienNeoForgeHud.register(modEventBus);
        MienNeoForge3DRenderer.register(modEventBus);

        modEventBus.addListener(MienNeoForgeClient::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(MienNeoForgeClient::onClientTick);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(CYCLE_STATE_KEY);
        event.register(HOLD_STATE_KEY);
        event.register(TOGGLE_STATE_KEY);
        event.register(OPEN_CONFIG_KEY);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        MinecraftClient client = MinecraftClient.getInstance();

        while (CYCLE_STATE_KEY.wasPressed()) {
            AnimationStateMachine.cycle();
        }
        while (TOGGLE_STATE_KEY.wasPressed()) {
            AnimationStateMachine.toggle();
        }
        while (OPEN_CONFIG_KEY.wasPressed()) {
            MienPlatforms.get().openConfigScreen();
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
    }
}
