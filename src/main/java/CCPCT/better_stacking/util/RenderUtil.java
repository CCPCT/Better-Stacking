package CCPCT.better_stacking.util;

import CCPCT.better_stacking.modConfig.ModConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

public class RenderUtil {
    public static void renderLabel(LevelRenderContext context){
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
            final Entity leader = entry.leader();

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
                case ItemEntity itemEntity -> {
                    if (!ModConfig.get().itemShowLabel) continue;
                    if (ModConfig.get().itemLabelShowName) {
                        text = type.display() + " " + text;
                    }
                }
                case ExperienceOrb experienceOrb -> {
                    if (!ModConfig.get().xpShowLabel) continue;
                }
                case Mob mob -> {
                    if (!ModConfig.get().entityShowLabel) continue;
                    if (ModConfig.get().entityLabelShowName) {
                        text = type.display() + " " + text;
                    }
                }
                default -> {
                    continue;
                }
            }

            final Vec3 leaderPos = leader.position();

            poseStack.pushPose();

            poseStack.translate(leaderPos.add(Vec3.Y_AXIS.scale(type.entity().getBbHeight())).subtract(cameraPos));

            poseStack.mulPose(camera.rotation());
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

            float scale = ModConfig.get().labelSize/40;
            poseStack.scale(-scale, -scale, scale);

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
