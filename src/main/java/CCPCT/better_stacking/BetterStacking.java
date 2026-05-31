package CCPCT.better_stacking;

import CCPCT.better_stacking.modConfig.ModConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

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

        LevelRenderEvents.END_MAIN.register(context -> {
            Minecraft client = Minecraft.getInstance();
            if (client.level == null) return;

            Font font = client.font;
            MultiBufferSource.BufferSource bufferSource = context.bufferSource();
            PoseStack poseStack = context.poseStack();

            // Grab the camera rotation quaternion
            net.minecraft.client.Camera camera = client.gameRenderer.getMainCamera();

            poseStack.pushPose();

            // 1. STAGE ONE: Push the matrix 3 blocks directly in front of the lens.
            // In Minecraft's camera coordinate system, looking straight ahead is along the negative Z-axis.
            // By keeping X and Y at 0, we remain perfectly centered horizontally and vertically.
            double renderX = 0.0;
            double renderY = 0.0;
            double renderZ = -3.0; // 3 blocks straight ahead
            poseStack.translate(renderX, renderY, renderZ);

            // 2. STAGE TWO: Apply the camera's rotation so the plate is billboarded to face you
            poseStack.mulPose(camera.rotation());

            // 3. STAGE THREE: Downscale the text matrix so it fits nicely on screen
            poseStack.scale(-0.025F, -0.025F, 0.025F);
            Matrix4f matrix4f = poseStack.last().pose();

            String testText = "RENDER TEST: SUCCESS";
            float textOffset = (float)(-font.width(testText) / 2);

            // Draw using standard normal mode first to rule out SEE_THROUGH layer bugs
            font.drawInBatch(
                    testText,
                    textOffset,
                    0,
                    0xFFFFFF, // Pure White
                    false,
                    matrix4f,
                    bufferSource,
                    Font.DisplayMode.NORMAL,
                    0x40000000, // Classic Translucent Dark Grey Background Box
                    15728880 // Full Bright (0xF000F0)
            );

            poseStack.popPose();

            // Flush the queue immediately
            bufferSource.endBatch();
        });

        LevelRenderEvents.END_MAIN.register(context -> {
            Minecraft client = Minecraft.getInstance();

            // Safety and config validation checks
            if (!ModConfig.get().modEnabled || !ModConfig.get().entityShowLabel || client.level == null) {
                return;
            }

            Map<BlockPos, Integer> clusters = EntityClusterManager.getActiveClusters();
            if (clusters.isEmpty()) return;

            Font font = client.font;
            MultiBufferSource.BufferSource bufferSource = context.bufferSource();
            PoseStack poseStack = context.poseStack();

            // FIX: Grab the current frame's interpolated active rendering lens directly from the engine
            Camera camera = client.gameRenderer.getMainCamera();
            Vec3 cameraPos = camera.position();

            for (Map.Entry<BlockPos, Integer> entry : clusters.entrySet()) {
                BlockPos blockPos = entry.getKey();
                int count = entry.getValue();

                int x = blockPos.getX();
                int y = blockPos.getY();
                int z = blockPos.getZ();

                // 1. Calculate Absolute World Coordinates of the label
                double worldLabelX = x + 0.5;
                double worldLabelY = y + 1.2;
                double worldLabelZ = z + 0.5;

                // 2. Calculate Camera-Relative Coordinates (what gets passed to poseStack)
                double relativeX = worldLabelX - cameraPos.x;
                double relativeY = worldLabelY - cameraPos.y;
                double relativeZ = worldLabelZ - cameraPos.z;

                // Throttle the output so it only prints roughly once every 2-3 seconds
                debugLogThrottle++;
                if (debugLogThrottle >= 100) {
                    debugLogThrottle = 0; // Reset counter

                    System.out.printf(
                            "[ModDebug] STACK FOUND (%s x%d)%n" +
                                    "  -> World Location:    X: %.2f, Y: %.2f, Z: %.2f%n" +
                                    "  -> Camera Position:   X: %.2f, Y: %.2f, Z: %.2f%n" +
                                    "  -> Offset To Camera:  X: %.2f, Y: %.2f, Z: %.2f%n%n",
                            client.level.getBlockState(blockPos).getBlock().getName().getString(),
                            count,
                            worldLabelX, worldLabelY, worldLabelZ,
                            cameraPos.x, cameraPos.y, cameraPos.z,
                            relativeX, relativeY, relativeZ
                    );
                }

                poseStack.pushPose();

                // Translate to world position relative to camera position
                poseStack.translate(new Vec3(x+0.5,y+3,z+0.5).add(cameraPos.scale(-1)));

                // FIX: Pull the valid 3D Quaternion directly from the engine's active camera
                poseStack.mulPose(camera.rotation());

                // Global layout plate sizing
                //poseStack.scale(-0.025F, -0.025F, 0.025F);
                Matrix4f matrix4f = poseStack.last().pose();

                String text = "x" + count;
                float textOffset = (float)(-font.width(text) / 2);

                // Safely render the text plate using modern display parameters

                font.drawInBatch(
                        text, textOffset, 0, ModConfig.get().labelColour, false,
                        matrix4f, bufferSource, Font.DisplayMode.SEE_THROUGH,
                        ModConfig.get().labelBgColour, 0xF000F0
                );

                poseStack.popPose();
            }

            // CRITICAL: Flush the buffer batch immediately as mandated by Fabric documentation
            bufferSource.endBatch();
        });;


    }
}
