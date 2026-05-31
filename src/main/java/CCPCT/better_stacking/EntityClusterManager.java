package CCPCT.better_stacking;

import CCPCT.better_stacking.modConfig.ModConfig;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EntityClusterManager {

    private static final IntOpenHashSet entitiesToCull = new IntOpenHashSet();

    // FIX: Map BlockPos directly to the inner type-count maps
    private static final Map<BlockPos, Object2IntOpenHashMap<EntityType<?>>> clusterMap = new HashMap<>();
    private static final Map<BlockPos, Integer> activeClusters = new HashMap<>();

    private static long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL_MS = 100;

    public static Map<BlockPos, Integer> getActiveClusters() {
        return Collections.unmodifiableMap(activeClusters);
    }

    public static boolean shouldSkipRender(Entity entity) {
        if (entity == Minecraft.getInstance().player || entity.getFirstPassenger() == Minecraft.getInstance().player) {
            return false;
        }
        if (!ModConfig.get().modEnabled) {
            return false;
        }
        return entitiesToCull.contains(entity.getId());
    }

    public static void updateClusterData(Minecraft client) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime < UPDATE_INTERVAL_MS) return;
        lastUpdateTime = currentTime;

        ClientLevel level = client.level;
        if (level == null) return;

        entitiesToCull.clear();
        clusterMap.clear();
        activeClusters.clear();

        Map<BlockPos, Map<EntityType<?>, Entity>> firstEntityMap = new HashMap<>();

        // PHASE 1: Count total matching entities per BlockPos
        for (Entity entity : level.entitiesForRendering()) {
            if (entity == client.player) continue;

            switch (entity) {
                case ItemEntity _ -> { if (!ModConfig.get().itemGeneral) continue; }
                case ExperienceOrb _ -> { if (!ModConfig.get().xpGeneral) continue; }
                case Mob _ -> { if (!ModConfig.get().entityGeneral) continue; }
                default -> { continue; }
            }

            // FIX: Create a standard immutable BlockPos object directly from entity coordinates
            BlockPos pos = new BlockPos(Mth.floor(entity.getX()), Mth.floor(entity.getY()), Mth.floor(entity.getZ()));
            EntityType<?> type = entity.getType();

            Object2IntOpenHashMap<EntityType<?>> typeCount = clusterMap.get(pos);
            if (typeCount == null) {
                typeCount = new Object2IntOpenHashMap<>();
                clusterMap.put(pos, typeCount);
                firstEntityMap.put(pos, new HashMap<>());
            }

            int count = typeCount.getInt(type);
            typeCount.put(type, count + 1);

            if (count == 0) {
                firstEntityMap.get(pos).put(type, entity);
            }
        }

        // PHASE 2: Apply the hard cutoff rules
        for (Entity entity : level.entitiesForRendering()) {
            if (entity == client.player) continue;

            int maxAllowed = 1;
            switch (entity) {
                case ItemEntity _ -> { if (!ModConfig.get().itemGeneral) continue; }
                case ExperienceOrb _ -> { if (!ModConfig.get().xpGeneral) continue; }
                case Mob _ -> {
                    if (!ModConfig.get().entityGeneral) continue;
                    maxAllowed = ModConfig.get().entityCount;
                }
                default -> { continue; }
            }

            BlockPos pos = new BlockPos(Mth.floor(entity.getX()), Mth.floor(entity.getY()), Mth.floor(entity.getZ()));
            EntityType<?> type = entity.getType();

            int totalCount = clusterMap.get(pos).getInt(type);

            if (totalCount > maxAllowed) {
                Entity representative = firstEntityMap.get(pos).get(type);
                if (entity != representative) {
                    entitiesToCull.add(entity.getId());
                } else {
                    activeClusters.put(pos, totalCount);
                }
            }
        }
    }
}