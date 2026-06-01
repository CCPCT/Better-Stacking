package CCPCT.better_stacking;

import CCPCT.better_stacking.modConfig.ModConfig;
import CCPCT.better_stacking.util.EntityClusterManager;
import CCPCT.better_stacking.util.EntityTypeKey;
import CCPCT.better_stacking.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

public class BetterStacking implements ClientModInitializer {

    int remaining = 1;

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        System.out.println("better stacking initialised and loaded config :3");

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (remaining <= 0) {
                EntityClusterManager.updateClusterData(client);
                remaining = ModConfig.get().entityUpdateTimeInterval;
            }
            remaining -= 1;
        });

        LevelRenderEvents.BEFORE_GIZMOS.register(RenderUtil::renderLabel);
    }
}

