package com.mien.mod.neoforge;

import com.mien.mod.MienRenderHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

public final class MienNeoForgeHud {
    private static final Identifier LAYER_ID = Identifier.of("mien", "mouth_preview");

    private MienNeoForgeHud() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(MienNeoForgeHud::onRegisterGuiLayers);
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, (context, tickCounter) ->
                MienRenderHelper.renderMouthPreview(context, MinecraftClient.getInstance()));
    }
}
