//package CCPCT.better_stacking.mixin;
//
//import CCPCT.better_stacking.EntityClusterManager;
//import net.minecraft.client.renderer.LevelRenderer;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//
//@Mixin(LevelRenderer.class)
//public class WorldRenderer {
//    @Inject(
//            method = "renderLevel",
//            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderResult(Lnet/minecraft/client/DeltaTracker;)V", shift = At.Shift.AFTER)
//    )
//    private void renderWorldSpaceLabels(DeltaTracker deltaTracker, boolean renderBlockOutline, RenderBuffers renderBuffers, CallbackInfo ci) {
//        Minecraft client = Minecraft.getInstance();
//        if (!ModConfig.get().modEnabled || !ModConfig.get().entityShowLabel || client.level == null) {
//            return;
//        }
//
//        Map<Long, Integer> clusters = EntityClusterManager.getActiveClusters();
//        if (clusters.isEmpty()) return;
//
//        Font font = client.font;
//        MultiBufferSource.BufferSource bufferSource = renderBuffers.bufferSource();
//
//        // Get camera position to translate from absolute world coords to relative camera space
//        double camX = client.gameRenderer.getMainCamera().getPosition().x;
//        double camY = client.gameRenderer.getMainCamera().getPosition().y;
//        double camZ = client.gameRenderer.getMainCamera().getPosition().z;
//
//        PoseStack poseStack = new PoseStack();
//
//        for (Map.Entry<Long, Integer> entry : clusters.entrySet()) {
//            long posLong = entry.getKey();
//            int count = entry.getValue();
//
//            // Unpack coordinate coordinates from your primitive long
//            int x = (int) (posLong >> 38);
//            int y = (int) (posLong << 26 >> 52); // Safe signed shift for Y
//            int z = (int) (posLong << 38 >> 38);
//
//            // Set up our label position above the block (e.g., center of the block, slightly elevated)
//            double renderX = (x + 0.5) - camX;
//            double renderY = (y + 1.2) - camY; // 1.2 blocks high ensures it floats perfectly above the entities
//            double renderZ = (z + 0.5) - camZ;
//
//            poseStack.pushPose();
//            poseStack.translate(renderX, renderY, renderZ);
//
//            // Make the label face the player camera continuously (Billboard Effect)
//            poseStack.mulPose(client.gameRenderer.getMainCamera().rotation());
//
//            // Scale the text down so it matches vanilla nametag sizes in 3D space
//            poseStack.scale(-0.025F, -0.025F, 0.025F);
//            Matrix4f matrix4f = poseStack.last().pose();
//
//            // Format your label: "x24"
//            String text = "x" + count;
//            float textOffset = (float) (-font.width(text) / 2);
//
//            // Draw the background translucent rectangle plate (Using your config background color)
//            font.drawInBatch(
//                    text, textOffset, 0, ModConfig.get().labelColour, false,
//                    matrix4f, bufferSource, Font.DisplayMode.SEE_THROUGH,
//                    ModConfig.get().labelBgColour, 15728880
//            );
//
//            poseStack.popPose();
//        }
//
//        bufferSource.endBatch(); // Flush the rendering queue immediately
//    }
//}
