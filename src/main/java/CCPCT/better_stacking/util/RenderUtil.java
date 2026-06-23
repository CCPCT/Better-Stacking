package CCPCT.better_stacking.util;

import CCPCT.better_stacking.modConfig.ModConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.TextDisplayEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class RenderUtil {
    public static void renderLabel(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();

        if (!ModConfig.get().modEnabled || client.level == null) {
            return;
        }

        List<EntityClusterManager.ClusterEntry> clusters = EntityClusterManager.getActiveClusters();
        if (clusters.isEmpty()) return;

        PoseStack poseStack = context.poseStack();
        Camera camera = client.gameRenderer.mainCamera();
        Vec3 cameraPos = camera.position();

        SubmitNodeCollector nodeCollector = context.submitNodeCollector();
        CameraRenderState cameraRenderState = context.levelState().cameraRenderState;

        Font font = client.font;

        for (EntityClusterManager.ClusterEntry entry : clusters) {
            final int count = entry.count();
            final Entity leader = entry.leader();
            if (leader == null || !leader.isAlive()) continue;

            int suffixMode = switch (leader) {
                case ItemEntity _ -> ModConfig.get().itemSuffixMode;
                case ExperienceOrb _ -> ModConfig.get().xpSuffixMode;
                default -> ModConfig.get().entitySuffixMode;
            };

            String countText = switch (suffixMode) {
                case 0 -> String.valueOf(count);
                case 1 -> intToEng(count);
                case 2 -> intToMC(count);
                default -> "error";
            };

            String text = "x" + countText;
            final EntityTypeKey type = entry.type();

            switch (leader) {
                case ItemEntity _ -> {
                    if (!ModConfig.get().itemShowLabel) continue;
                    if (ModConfig.get().itemLabelShowName) text = type.display() + " " + text;
                }
                case ExperienceOrb _ -> { if (!ModConfig.get().xpShowLabel) continue; }
                case Mob _ -> {
                    if (!ModConfig.get().entityShowLabel) continue;
                    if (ModConfig.get().entityLabelShowName) text = type.display() + " " + text;
                }
                default -> { continue; }
            }

            // 2. Wrap your text in a Component, just like vanilla name tags expect
            Component labelComponent = Component.literal(text).withStyle(style -> style.withColor(ModConfig.get().labelColour));

            // 3. Interpolate the base location relative to the camera
            final Vec3 leaderPos = leader.position();
            Vec3 attachmentPosition = leaderPos.add(Vec3.Y_AXIS.scale(type.entity().getBbHeight())).subtract(cameraPos);

            poseStack.pushPose();

            // Translate the matrix to the entity's head height
            poseStack.translate(attachmentPosition.x, attachmentPosition.y, attachmentPosition.z);

            // 4. Submit directly to Mojang's native rendering batcher!
            // This handles billboarding, scaling, background rendering, and occlusion checks automatically.
            final int packedLight = 15728880;
            final int offset = 0; // Vertical pixel offset tweak if needed
            boolean isSeeThrough = ModConfig.get().renderThroughBlocks;

            nodeCollector.submitNameTag(
                    poseStack,
                    net.minecraft.world.phys.Vec3.ZERO, // Base offset relative to our translated matrix
                    offset,
                    labelComponent,
                    isSeeThrough,
                    packedLight,
                    cameraRenderState
            );

            poseStack.popPose();
        }
    }

    public static String intToEng(int value) {
        if (value < 1000) {
            return String.valueOf(value);
        }

        String[] suffixes = new String[]{"", "k", "M", "G", "T"};

        int exp = (int) (Math.log10(value) / 3);

        if (exp >= suffixes.length) {
            exp = suffixes.length - 1;
        }

        double scaledValue = value / Math.pow(1000, exp);

        return String.format("%.1f%s", scaledValue, suffixes[exp]);
    }

    public static String intToMC(int value) {
        double sbcLimit = 54.0;
        double sbLimit = 27.0;
        double sLimit = 64.0;

        double basePerSb = sLimit * sbLimit;
        double basePerSbc = basePerSb * sbcLimit;

        if (value >= basePerSbc) {
            double scaled = (double) value / basePerSbc;
            return String.format("%.1fsbc", scaled);
        }

        if (value >= basePerSb) {
            double scaled = (double) value / basePerSb;
            return String.format("%.1fsb", scaled);
        }

        if (value >= sLimit) {
            double scaled = (double) value / sLimit;
            return String.format("%.1fs", scaled);
        }

        return String.valueOf(value);
    }

}
