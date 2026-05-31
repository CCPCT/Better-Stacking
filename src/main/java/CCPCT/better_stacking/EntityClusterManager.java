package CCPCT.better_stacking;

import CCPCT.better_stacking.modConfig.ModConfig;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityClusterManager {

    private static final IntOpenHashSet entitiesToCull = new IntOpenHashSet();
    // Tracks separate text labels per block position (e.g., BlockPos -> Map of Sub-types to Counts)
    private static final Map<BlockPos, java.util.List<ClusterEntry>> activeClusters = new HashMap<>();
    private static final Map<BlockPos, Object2IntOpenHashMap<EntityTypeKey>> clusterMap = new HashMap<>();
    private static final long UPDATE_INTERVAL_MS = 100;
    private static long lastUpdateTime = 0;

    public static Map<BlockPos, List<ClusterEntry>> getActiveClusters() {
        return Collections.unmodifiableMap(activeClusters);
    }

    public static boolean shouldSkipRender(Entity entity) {
        if (entity == Minecraft.getInstance().player || entity.getFirstPassenger() == Minecraft.getInstance().player) {
            return false;
        }
        return ModConfig.get().modEnabled && entitiesToCull.contains(entity.getId());
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

        Map<BlockPos, Map<EntityTypeKey, Entity>> firstEntityMap = new HashMap<>();

        // PHASE 1: Gather and separate counts based on EntityTypeKey (Type + Age)
        for (Entity entity : level.entitiesForRendering()) {
            if (entity == client.player) continue;

            switch (entity) {
                case ItemEntity _ -> {
                    if (!ModConfig.get().itemGeneral) continue;
                }
                case ExperienceOrb _ -> {
                    if (!ModConfig.get().xpGeneral) continue;
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

            Object2IntOpenHashMap<EntityTypeKey> typeCount = clusterMap.get(pos);
            if (typeCount == null) {
                typeCount = new Object2IntOpenHashMap<>();
                clusterMap.put(pos, typeCount);
                firstEntityMap.put(pos, new HashMap<>());
            }

            int count = typeCount.getInt(key);
            typeCount.put(key, count + 1);

            if (count == 0) {
                firstEntityMap.get(pos).put(key, entity);
            }
        }

        // PHASE 2: Evaluate rules against specific age cutoffs
        for (Entity entity : level.entitiesForRendering()) {
            if (entity == client.player) continue;

            int maxAllowed = 1;
            switch (entity) {
                case ItemEntity _ -> {
                    if (!ModConfig.get().itemGeneral) continue;
                }
                case ExperienceOrb _ -> {
                    if (!ModConfig.get().xpGeneral) continue;
                }
                case Mob _ -> {
                    if (!ModConfig.get().entityGeneral) continue;
                    maxAllowed = ModConfig.get().entityCount;
                }
                default -> {
                    continue;
                }
            }

            BlockPos pos = new BlockPos(Mth.floor(entity.getX()), Mth.floor(entity.getY()), Mth.floor(entity.getZ()));
            EntityTypeKey key = EntityTypeKey.fromEntity(entity);

            int totalCount = clusterMap.get(pos).getInt(key);

            if (totalCount > maxAllowed) {
                Entity representative = firstEntityMap.get(pos).get(key);
                if (entity != representative) {
                    entitiesToCull.add(entity.getId());
                } else {
                    // Create our flat entry package
                    ClusterEntry entryPackage = new ClusterEntry(
                            key.type(),
                            key.isBaby(),
                            totalCount,
                            entity.getBbHeight()
                    );

                    // Append cleanly into the block's flat list registry
                    activeClusters.computeIfAbsent(pos, _ -> new java.util.ArrayList<>()).add(entryPackage);
                }
            }
        }
    }

    public record EntityTypeKey(EntityType<?> type, boolean isBaby) {

        public static EntityTypeKey fromEntity(Entity entity) {
            boolean baby = false;
            if (entity instanceof AgeableMob ageable) {
                baby = ageable.isBaby();
            } else if (entity instanceof Zombie zombie) {
                baby = zombie.isBaby();
            }
            return new EntityTypeKey(entity.getType(), baby);
        }
    }

    public record ClusterEntry(
            EntityType<?> entityType,
            boolean isBaby,
            int count,
            float entityHeight
    ) {
        public String getDisplayName() {
            String name = entityType.getDescription().getString();
            return isBaby ? "Baby " + name : name;
        }
    }
}