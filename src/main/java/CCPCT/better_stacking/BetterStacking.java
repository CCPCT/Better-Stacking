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
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;
import java.util.Map;

public class BetterStacking implements ClientModInitializer {

    int remaining = 1;
    private static int debugLogThrottle = 0;

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
            if (!ModConfig.get().modEnabled || !ModConfig.get().entityShowLabel || client.level == null) {
                return;
            }


            Map<BlockPos, List<EntityClusterManager.ClusterEntry>> clusters = EntityClusterManager.getActiveClusters();
            if (clusters.isEmpty()) return;

            Font font = client.font;
            MultiBufferSource.BufferSource bufferSource = context.bufferSource();
            PoseStack poseStack = context.poseStack();

            // FIX: Grab the current frame's interpolated active rendering lens directly from the engine
            Camera camera = client.gameRenderer.getMainCamera();
            Vec3 cameraPos = camera.position();

            for (Map.Entry<BlockPos, List<EntityClusterManager.ClusterEntry>> entry : clusters.entrySet()) {
                BlockPos blockPos = entry.getKey();
                for (var clusterEntry : entry.getValue()) {

                    final int count = clusterEntry.count();

                    final float relativeX = (float) ((blockPos.getX() + 0.5) - cameraPos.x);
                    final float relativeY = (float) ((blockPos.getY() + 0.2 + clusterEntry.entityHeight()) - cameraPos.y);
                    final float relativeZ = (float) ((blockPos.getZ() + 0.5) - cameraPos.z);

                    poseStack.pushPose();

                    poseStack.translate(relativeX, relativeY, relativeZ);

                    poseStack.mulPose(camera.rotation());
                    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

                    poseStack.scale(-0.025F, -0.025F, 0.025F);

                    Matrix4f matrix4f = poseStack.last().pose();

                    String text = "x" + count;

                    if (ModConfig.get().entityLabelShowName) {
                        text = clusterEntry.getDisplayName()+" "+text;
                    }

                    float textOffset = (float)(-font.width(text) / 2);

                    font.drawInBatch(
                            text, textOffset, -font.lineHeight, ModConfig.get().labelColour, false,
                            matrix4f, bufferSource, Font.DisplayMode.SEE_THROUGH,
                            ModConfig.get().labelBgColour,
                            15728880
                    );

                    poseStack.popPose();
                }

            }
        });


    }
}
