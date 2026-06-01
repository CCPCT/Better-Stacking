package CCPCT.better_stacking;

import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;

public record EntityTypeKey(Entity entity, String check, String display, float bbHeight) {
    public static EntityTypeKey fromEntity(Entity entity) {
        // 1. Resolve age/baby properties
        boolean baby = switch (entity) {
            case AgeableMob ageable -> ageable.isBaby();
            case Zombie zombie -> zombie.isBaby();
            default -> false;
        };

        // 2. Resolve the structural display name component
        String checkName = switch (entity) {
            case ItemEntity item -> item.getItem().getItem().getDescriptionId();
            case ExperienceOrb _ -> "XP";
            default -> entity.getType().getDescriptionId();
        };

        String customName;
        if (entity instanceof ItemEntity item) {
            customName = item.getItem().getHoverName().getString();
        } else {
            customName = entity.getDisplayName().getString();
        }
        if (entity.hasCustomName()) {
            checkName = checkName + "/" + customName;
        }

        if (baby){
            customName = "Baby " + customName;
            checkName = "Baby " + checkName;
        }


        return new EntityTypeKey(entity, checkName, customName, entity.getBbHeight());
    }
}
