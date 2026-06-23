package CCPCT.better_stacking.util;

import CCPCT.better_stacking.ICustomNameTagSubmitter;
import CCPCT.better_stacking.modConfig.ModConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

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

        CameraRenderState cameraRenderState = context.levelState().cameraRenderState;

        Font font = client.font;

        OrderedSubmitNodeCollector orderedCollector = context.submitNodeCollector().order(0);

        if (!(orderedCollector instanceof ICustomNameTagSubmitter customSubmitter)) {
            System.err.println("unable to wrap");
            return;
        }

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
                case ExperienceOrb _ -> {
                    if (!ModConfig.get().xpShowLabel) continue;
                }
                case Mob _ -> {
                    if (!ModConfig.get().entityShowLabel) continue;
                    if (ModConfig.get().entityLabelShowName) text = type.display() + " " + text;
                }
                default -> {
                    continue;
                }
            }

            // 1. Wrap your string into a plain literal Component
            Component labelComponent = Component.literal(text);

            final Vec3 leaderPos = leader.position();
            final Vec3 relativePos = leaderPos.subtract(cameraPos).add(Vec3.Y_AXIS.scale(type.entity().getBbHeight()));

            // 2. We use the custom interface method, passing world-space coordinates (Vec3)
            // because the method handles the translation internally.
            customSubmitter.betterStacking$submitCustomColorNameTag(
                    poseStack,
                    relativePos,
                    0,
                    labelComponent,
                    ModConfig.get().renderThroughBlocks,
                    15728880,
                    cameraRenderState,
                    ModConfig.get().labelColour,
                    ModConfig.get().labelBgColour
            );
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
