package CCPCT.better_stacking.modConfig;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;


public class ConfigScreen extends Screen {

    protected ConfigScreen() {
        super(Component.translatable("easiercrafting.config.title"));
    }

    public static Screen getConfigScreen(Screen parent) {
        ModConfig.load();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("easiercrafting.config.title"))
                .setSavingRunnable(ModConfig::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory generalTab = builder.getOrCreateCategory(text("general"));
        ConfigCategory itemTab = builder.getOrCreateCategory(text("item"));
        ConfigCategory entityTab = builder.getOrCreateCategory(text("entity"));
        ConfigCategory xpTab = builder.getOrCreateCategory(text("xp"));


        // === GENERAL TAB ===
        generalTab.addEntry(entryBuilder.startBooleanToggle(text("Enable Mod"), ModConfig.get().modEnabled)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> ModConfig.get().modEnabled = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startIntField(text("Entity update time interval"), ModConfig.get().entityUpdateTimeInterval)
                .setDefaultValue(20)
                .setTooltip(text("tick between entity updates. \nHigher = less frequent update"))
                .setMin(1).setMax(1200)
                .setSaveConsumer(newValue -> ModConfig.get().entityUpdateTimeInterval = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startAlphaColorField(text("Label Color"), ModConfig.get().labelColour)
                .setDefaultValue(0xA0FFFF00)
                .setAlphaMode(true)
                .setSaveConsumer(newValue -> ModConfig.get().labelColour = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startAlphaColorField(text("Label Background Color"), ModConfig.get().labelBgColour)
                .setDefaultValue(0x67676767)
                .setAlphaMode(true)
                .setSaveConsumer(newValue -> ModConfig.get().labelBgColour = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startFloatField(text("Label Size"), ModConfig.get().labelSize)
                .setDefaultValue(1.0f)
                .setSaveConsumer(newValue -> ModConfig.get().labelSize = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startFloatField(text("Label Offset"), ModConfig.get().labelOffset)
                .setDefaultValue(10.0f)
                .setSaveConsumer(newValue -> ModConfig.get().labelOffset = newValue)
                .build());


        // === ITEM TAB ===
        itemTab.addEntry(entryBuilder.startBooleanToggle(text("Item General"), ModConfig.get().itemGeneral)
                .setDefaultValue(false)
                .setSaveConsumer(newValue -> ModConfig.get().itemGeneral = newValue)
                .build());

        itemTab.addEntry(entryBuilder.startBooleanToggle(text("Show Item Label"), ModConfig.get().itemShowLabel)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> ModConfig.get().itemShowLabel = newValue)
                .build());

        itemTab.addEntry(entryBuilder.startBooleanToggle(text("Show Item Name in Label"), ModConfig.get().itemLabelShowName)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> ModConfig.get().itemLabelShowName = newValue)
                .build());


        // === ENTITY TAB ===
        entityTab.addEntry(entryBuilder.startBooleanToggle(text("Entity General"), ModConfig.get().entityGeneral)
                .setDefaultValue(false)
                .setSaveConsumer(newValue -> ModConfig.get().entityGeneral = newValue)
                .build());

        entityTab.addEntry(entryBuilder.startIntField(text("Minimum Entity"), ModConfig.get().entityCount)
                .setTooltip(text("Minimum entities require to stack"))
                .setDefaultValue(5)
                .setMin(1).setMax(100)
                .setSaveConsumer(newValue -> ModConfig.get().entityCount = newValue)
                .build());

        entityTab.addEntry(entryBuilder.startBooleanToggle(text("Show Entity Label"), ModConfig.get().entityShowLabel)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> ModConfig.get().entityShowLabel = newValue)
                .build());

        entityTab.addEntry(entryBuilder.startBooleanToggle(text("Show Entity Type in Label"), ModConfig.get().entityLabelShowName)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> ModConfig.get().entityLabelShowName = newValue)
                .build());


        // === XP TAB ===
        xpTab.addEntry(entryBuilder.startBooleanToggle(text("XP General"), ModConfig.get().xpGeneral)
                .setDefaultValue(false)
                .setSaveConsumer(newValue -> ModConfig.get().xpGeneral = newValue)
                .build());

        xpTab.addEntry(entryBuilder.startBooleanToggle(text("Show XP Label"), ModConfig.get().xpShowLabel)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> ModConfig.get().xpShowLabel = newValue)
                .build());

        return builder.build();
    }
    
    static Component text(String str) {
        return Component.literal(str);
    }
}
