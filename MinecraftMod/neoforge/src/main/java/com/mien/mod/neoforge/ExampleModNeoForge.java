package com.mien.mod.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import com.mien.mod.ExampleMod;
import com.mien.mod.neoforge.client.MienNeoForgeClient;

@Mod(ExampleMod.MOD_ID)
public final class ExampleModNeoForge {
    public ExampleModNeoForge(IEventBus modEventBus) {
        // Run our common setup.
        ExampleMod.init();

        MienNeoForgeHud.register(modEventBus);
        MienNeoForge3DRenderer.register(modEventBus);
        MienNeoForgeClient.register(modEventBus);
    }
}
