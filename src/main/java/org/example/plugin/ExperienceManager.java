package org.example.plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages experience bars for all players on the server.
 * Handles persistence of XP data between server sessions.
 */
public class ExperienceManager {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DATA_DIR = Path.of("plugins/ExamplePlugin");
    private static final Path DATA_FILE = DATA_DIR.resolve("xp_data.json");

    private static ExperienceManager instance;

    // Active HUD references (only for online players)
    private final Map<UUID, ExperienceBarHud> playerXPBars = new ConcurrentHashMap<>();
    
    // Persistent XP data (for all players, saved to disk)
    private Map<String, PlayerXPData> xpData = new ConcurrentHashMap<>();

    private ExperienceManager() {
        loadData();
    }

    public static ExperienceManager getInstance() {
        if (instance == null) {
            instance = new ExperienceManager();
        }
        return instance;
    }

    /**
     * Loads XP data from disk.
     */
    public void loadData() {
        if (!Files.exists(DATA_FILE)) {
            LOGGER.atInfo().log("No XP data file found, starting fresh.");
            xpData = new ConcurrentHashMap<>();
            return;
        }

        try {
            String json = Files.readString(DATA_FILE);
            Type type = new TypeToken<Map<String, PlayerXPData>>(){}.getType();
            Map<String, PlayerXPData> loaded = GSON.fromJson(json, type);
            if (loaded != null) {
                xpData = new ConcurrentHashMap<>(loaded);
                LOGGER.atInfo().log("Loaded XP data for " + xpData.size() + " players.");
            }
        } catch (IOException e) {
            LOGGER.atWarning().log("Failed to load XP data: " + e.getMessage());
            xpData = new ConcurrentHashMap<>();
        }
    }

    /**
     * Saves all XP data to disk.
     */
    public void saveData() {
        try {
            // Create directory if it doesn't exist
            if (!Files.exists(DATA_DIR)) {
                Files.createDirectories(DATA_DIR);
            }

            // Update xpData from active HUDs before saving
            for (Map.Entry<UUID, ExperienceBarHud> entry : playerXPBars.entrySet()) {
                ExperienceBarHud hud = entry.getValue();
                PlayerXPData data = new PlayerXPData(hud.getLevel(), hud.getCurrentXP(), hud.getXpToNextLevel());
                xpData.put(entry.getKey().toString(), data);
            }

            String json = GSON.toJson(xpData);
            Files.writeString(DATA_FILE, json);
            LOGGER.atInfo().log("Saved XP data for " + xpData.size() + " players.");
        } catch (IOException e) {
            LOGGER.atWarning().log("Failed to save XP data: " + e.getMessage());
        }
    }

    /**
     * Initializes and shows the XP bar for a player.
     * Restores their previous XP if they have any saved.
     */
    public void initializePlayer(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        UUID playerId = playerRef.getUuid();
        
        // Create the XP bar HUD
        ExperienceBarHud xpBar = new ExperienceBarHud(playerRef);
        
        // Restore saved XP data BEFORE showing the HUD
        // Use setInitialExperience to avoid updating non-existent UI elements
        PlayerXPData savedData = xpData.get(playerId.toString());
        if (savedData != null) {
            xpBar.setInitialExperience(savedData.getLevel(), savedData.getCurrentXP(), savedData.getXpToNextLevel());
            LOGGER.atInfo().log("Restored XP for " + playerRef.getUsername() + ": Level " + savedData.getLevel());
        }
        
        playerXPBars.put(playerId, xpBar);
        
        // Show it to the player - this calls build() which will use the restored values
        HudManager hudManager = player.getHudManager();
        hudManager.setCustomHud(playerRef, xpBar);
    }

    /**
     * Removes the XP bar when a player leaves.
     * Saves their data before removing.
     */
    public void removePlayer(@Nonnull UUID playerId) {
        ExperienceBarHud hud = playerXPBars.remove(playerId);
        if (hud != null) {
            // Save their data before removing
            PlayerXPData data = new PlayerXPData(hud.getLevel(), hud.getCurrentXP(), hud.getXpToNextLevel());
            xpData.put(playerId.toString(), data);
        }
    }

    /**
     * Gets the XP bar for a player, or null if they don't have one.
     */
    @Nullable
    public ExperienceBarHud getXPBar(@Nonnull UUID playerId) {
        return playerXPBars.get(playerId);
    }

    /**
     * Checks if a player has an XP bar initialized.
     */
    public boolean hasXPBar(@Nonnull UUID playerId) {
        return playerXPBars.containsKey(playerId);
    }

    /**
     * Adds XP to a player and returns true if they leveled up.
     */
    public boolean addExperience(@Nonnull UUID playerId, int amount) {
        ExperienceBarHud xpBar = playerXPBars.get(playerId);
        if (xpBar != null) {
            return xpBar.addExperience(amount);
        }
        return false;
    }

    /**
     * Gives XP to a player (can be any amount).
     * Returns the number of levels gained.
     */
    public int giveExperience(@Nonnull UUID playerId, int amount) {
        ExperienceBarHud xpBar = playerXPBars.get(playerId);
        if (xpBar == null) return 0;
        
        int startLevel = xpBar.getLevel();
        xpBar.addExperience(amount);
        return xpBar.getLevel() - startLevel;
    }

    /**
     * Resets a player's XP to level 1 with 0 XP.
     */
    public void resetExperience(@Nonnull UUID playerId) {
        ExperienceBarHud xpBar = playerXPBars.get(playerId);
        if (xpBar != null) {
            xpBar.setExperience(1, 0, 100);
        }
        
        // Also reset saved data
        xpData.put(playerId.toString(), new PlayerXPData());
    }

    /**
     * Sets a player's level directly.
     */
    public void setLevel(@Nonnull UUID playerId, int level) {
        ExperienceBarHud xpBar = playerXPBars.get(playerId);
        if (xpBar != null) {
            int xpNeeded = PlayerXPData.calculateXPForLevel(level);
            xpBar.setExperience(level, 0, xpNeeded);
        }
    }

    /**
     * Gets a player's current level (0 if not found).
     */
    public int getLevel(@Nonnull UUID playerId) {
        ExperienceBarHud xpBar = playerXPBars.get(playerId);
        return xpBar != null ? xpBar.getLevel() : 0;
    }
}
