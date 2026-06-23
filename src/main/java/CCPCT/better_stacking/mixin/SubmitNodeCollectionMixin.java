package CCPCT.better_stacking.mixin;

import CCPCT.better_stacking.ICustomNameTagSubmitter;
import com.mojang.blaze3d.vertex.PoseStack;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.TranslucentFeatureRenderPhase;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectionMixin implements ICustomNameTagSubmitter {
    @Final
    @Shadow
    public SimpleFeatureRenderPhase nameTags;

    @Final
    @Shadow
    public TranslucentFeatureRenderPhase seeThroughNameTags; // Use your exact decompiled field name here

    @Override
    public void betterStacking$submitCustomColorNameTag(
            PoseStack poseStack,
            @Nullable Vec3 nameTagAttachment,
            int offset,
            Component name,
            boolean seeThrough,
            int lightCoords,
            CameraRenderState camera,
            int textColor,
            int backgroundColor
    ) {
        if (nameTagAttachment != null) {
            Minecraft minecraft = Minecraft.getInstance();
            poseStack.pushPose();

            // Replicate vanilla position and billboard transformations
            poseStack.translate(nameTagAttachment.x, nameTagAttachment.y + (double)0.5F, nameTagAttachment.z);
            poseStack.mulPose(camera.orientation);
            poseStack.scale(0.025F, -0.025F, 0.025F);

            Matrix4f pose = new Matrix4f(poseStack.last().pose());
            float x = (float)(-minecraft.font.width(name)) / 2.0F;

            if (seeThrough) {
                this.nameTags.submit(new NameTagFeatureRenderer.Submit(
                        pose, x, (float)offset, name, lightCoords, textColor, backgroundColor, Font.DisplayMode.NORMAL
                ));
                this.seeThroughNameTags.submit(new NameTagFeatureRenderer.Submit(
                        pose, x, (float)offset, name, lightCoords, textColor, backgroundColor, Font.DisplayMode.SEE_THROUGH
                ));
            } else {
                this.nameTags.submit(new NameTagFeatureRenderer.Submit(
                        pose, x, (float)offset, name, lightCoords, textColor, backgroundColor, Font.DisplayMode.NORMAL
                ));
            }
            poseStack.popPose();
        }
    }
}
