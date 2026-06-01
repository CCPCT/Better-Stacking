package CCPCT.better_stacking;

import CCPCT.better_stacking.modConfig.ModConfig;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityClusterManager {

    static final IntOpenHashSet entitiesToCull = new IntOpenHashSet();
    static final List<ClusterEntry> activeClusters = new ObjectArrayList<>();

    // Tracks the number of physical entities for culling logic thresholds
    static final Map<BlockPos, Object2IntOpenHashMap<String>> entityCountMap = new HashMap<>();
    // Tracks the consolidated display value (mobs = entity count, items = total item count, xp = total points)
    static final Map<BlockPos, Object2IntOpenHashMap<String>> displayValueMap = new HashMap<>();
    static final Map<BlockPos, Map<EntityTypeKey, Entity>> firstEntityMap = new HashMap<>();

    public static List<ClusterEntry> getActiveClusters() {
        return activeClusters;
    }

    public static boolean shouldSkipRender(Entity entity) {
        if (entity == Minecraft.getInstance().player || entity.getFirstPassenger() == Minecraft.getInstance().player) {
            return false;
        }
        return ModConfig.get().modEnabled && entitiesToCull.contains(entity.getId());
    }

    public static void updateClusterData(Minecraft client) {

        ClientLevel level = client.level;
        if (level == null) return;

        entitiesToCull.clear();
        entityCountMap.clear();
        displayValueMap.clear();
        activeClusters.clear();
        firstEntityMap.clear();

        if (!ModConfig.get().modEnabled) return;

        // PHASE 1: Gather data and increment value metrics
        for (Entity entity : level.entitiesForRendering()) {
            if (entity == client.player) continue;

            int valueIncrement = 1; // Default for mobs (1 entity = 1 count)
            switch (entity) {
                case ItemEntity item -> {
                    if (!ModConfig.get().itemGeneral) continue;
                    valueIncrement = item.getItem().getCount(); // Use item count instead of 1
                }
                case ExperienceOrb xp -> {
                    if (!ModConfig.get().xpGeneral) continue;
                    valueIncrement = xp.getValue(); // Use raw XP points instead of 1
                }
                case Mob _ -> {
                    if (!ModConfig.get().entityGeneral) continue;
                }
                default -> {
                    continue;
                }
            }

            BlockPos pos = new BlockPos(Mth.floor(entity.getX()), Mth.floor(entity.getY()), Mth.floor(entity.getZ()));
            EntityTypeKey key = EntityTypeKey.fromEntity(entity);
            String checkKey = key.check();

            Object2IntOpenHashMap<String> entityCounts = entityCountMap.get(pos);
            Object2IntOpenHashMap<String> displayValues = displayValueMap.get(pos);

            if (entityCounts == null) {
                entityCounts = new Object2IntOpenHashMap<>();
                displayValues = new Object2IntOpenHashMap<>();
                entityCountMap.put(pos, entityCounts);
                displayValueMap.put(pos, displayValues);
                firstEntityMap.put(pos, new HashMap<>());
            }

            // Track how many physical entity boxes are here for our culling limit check
            int rawCount = entityCounts.getInt(checkKey);
            entityCounts.put(checkKey, rawCount + 1);

            // Accumulate the smart text value
            int currentVal = displayValues.getInt(checkKey);
            displayValues.put(checkKey, currentVal + valueIncrement);

            if (rawCount == 0) {
                firstEntityMap.get(pos).put(key, entity);
            }
        }

        // PHASE 2: Evaluate rules against specific limits and package
        for (Entity entity : level.entitiesForRendering()) {
            if (entity == client.player) continue;

            int maxAllowed = 0;
            switch (entity) {
                case ItemEntity _ -> {
                    if (!ModConfig.get().itemGeneral) continue;
                }
                case ExperienceOrb _ -> { if (!ModConfig.get().xpGeneral) continue; }
                case Mob _ -> {
                    if (!ModConfig.get().entityGeneral) continue;
                    maxAllowed = ModConfig.get().entityCount;
                }
                default -> { continue; }
            }

            BlockPos pos = new BlockPos(Mth.floor(entity.getX()), Mth.floor(entity.getY()), Mth.floor(entity.getZ()));
            EntityTypeKey key = EntityTypeKey.fromEntity(entity);
            String checkKey = key.check();

            int totalEntities = entityCountMap.get(pos).getInt(checkKey);

            if (totalEntities > maxAllowed) {
                Entity representative = firstEntityMap.get(pos).get(key);
                if (entity != representative) {
                    entitiesToCull.add(entity.getId());
                } else {
                    // Extract the clean consolidated value
                    int dynamicCount = displayValueMap.get(pos).getInt(checkKey);

                    ClusterEntry entryPackage = new ClusterEntry(representative, key, dynamicCount);
                    activeClusters.add(entryPackage);
                }
            }
        }
    }

    public record ClusterEntry(Entity leader, EntityTypeKey type, int count) {}
}