package com.mien.mod.fabric;

import com.mien.mod.MienRenderHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public final class MienFabricHud implements HudRenderCallback {
    private MienFabricHud() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register(new MienFabricHud());
    }

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        MienRenderHelper.renderMouthPreview(drawContext, MinecraftClient.getInstance());
    }
}
