package com.mien.mod.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

import com.mien.mod.ExampleMod;
import com.mien.mod.neoforge.client.MienNeoForgeClient;

// 整个模组只存在于客户端：服务端不加载本类，也不会触碰任何客户端专属 API。
@Mod(value = ExampleMod.MOD_ID, dist = Dist.CLIENT)
public final class ExampleModNeoForge {
    public ExampleModNeoForge(IEventBus modEventBus) {
        // Run our common setup.
        ExampleMod.init();

        // 双保险：即使加载器在服务端构造了本类，也不加载任何客户端类。
        if (FMLEnvironment.dist == Dist.CLIENT) {
            MienNeoForgeClient.init(modEventBus);
        }
    }
}
