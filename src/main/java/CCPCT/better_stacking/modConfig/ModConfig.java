package CCPCT.better_stacking.modConfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {

    public boolean modEnabled = true;
    public int entityUpdateTimeInterval = 20;
    public boolean renderThroughBlocks = false;
    public int labelColour = 0xA0FFFF00;
    public int labelBgColour = 0x67676767;
    public float labelSize = 1f;
    public float labelOffset = 10f;

    public boolean itemGeneral = false;
    public boolean itemShowLabel = true;
    public boolean itemLabelShowName = true;

    public boolean entityGeneral = false;
    public int entityCount = 5;
    public boolean entityShowLabel = true;
    public boolean entityLabelShowName = true;

    public boolean xpGeneral = false;
    public boolean xpShowLabel = true;


    public static ModConfig get() {
        if (INSTANCE==null)
            INSTANCE = new ModConfig();
        return INSTANCE;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ModConfig INSTANCE;


    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("easier-crafting.json");

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                INSTANCE = GSON.fromJson(Files.newBufferedReader(CONFIG_PATH), ModConfig.class);
            } else {
                INSTANCE = new ModConfig();
                save();
            }
        } catch (IOException e) {
            INSTANCE = new ModConfig();
        }
    }

    public static void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(get()));
        } catch (IOException e) {
            System.err.println("Unable to save EasierCrafting config!");
        }
    }
}