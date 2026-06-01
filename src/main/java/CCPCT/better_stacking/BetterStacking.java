package CCPCT.better_stacking;

import CCPCT.better_stacking.modConfig.ModConfig;
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

        LevelRenderEvents.BEFORE_GIZMOS.register(context -> {
            Minecraft client = Minecraft.getInstance();

            // Safety and config validation checks
            if (!ModConfig.get().modEnabled || client.level == null) {
                return;
            }

            List<EntityClusterManager.ClusterEntry> clusters = EntityClusterManager.getActiveClusters();
            if (clusters.isEmpty()) return;

            Font font = client.font;
            MultiBufferSource.BufferSource bufferSource = context.bufferSource();
            PoseStack poseStack = context.poseStack();

            // FIX: Grab the current frame's interpolated active rendering lens directly from the engine
            Camera camera = client.gameRenderer.getMainCamera();
            Vec3 cameraPos = camera.position();

            for (EntityClusterManager.ClusterEntry entry : clusters) {
                final int count = entry.count();
                String text = "x" + count;
                final EntityTypeKey type = entry.type();
                var leader = entry.leader();

                if (leader instanceof ItemEntity) {
                    if (!ModConfig.get().itemShowLabel) continue;
                    if (ModConfig.get().itemLabelShowName) {
                        text = type.display() + " " + text;
                    }
                } else if (leader instanceof ExperienceOrb) {
                    if (!ModConfig.get().xpShowLabel) continue;
                } else if (leader instanceof Mob) {
                    if (!ModConfig.get().entityShowLabel) continue;
                    if (ModConfig.get().entityLabelShowName) {
                        text = type.display() + " " + text;
                    }
                }

                final Vec3 leaderPos = leader.position();

                poseStack.pushPose();

                poseStack.translate(leaderPos.add(Vec3.Y_AXIS.scale(type.entity().getBbHeight())).subtract(cameraPos));

                poseStack.mulPose(camera.rotation());
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

                poseStack.scale(-0.025F, -0.025F, 0.025F);

                Matrix4f matrix4f = poseStack.last().pose();

                float textOffset = (float) (-font.width(text) / 2);

                font.drawInBatch(
                        text, textOffset, -font.lineHeight - ModConfig.get().labelOffset, ModConfig.get().labelColour, false,
                        matrix4f, bufferSource, ModConfig.get().renderThroughBlocks ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.POLYGON_OFFSET,
                        ModConfig.get().labelBgColour,
                        15728880
                );

                poseStack.popPose();
            }
        });
    }
}

