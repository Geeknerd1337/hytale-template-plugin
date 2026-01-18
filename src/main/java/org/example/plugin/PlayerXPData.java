package org.example.plugin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;

/**
 * Stores XP data for a single player.
 * This class is serializable via the Hytale Codec system.
 */
public class PlayerXPData {

    @Nonnull
    public static final BuilderCodec<PlayerXPData> CODEC = BuilderCodec.builder(PlayerXPData.class, PlayerXPData::new)
            .addField(new KeyedCodec<>("Level", Codec.INTEGER), 
                (data, value) -> data.level = value, 
                data -> data.level)
            .addField(new KeyedCodec<>("CurrentXP", Codec.INTEGER), 
                (data, value) -> data.currentXP = value, 
                data -> data.currentXP)
            .addField(new KeyedCodec<>("XPToNextLevel", Codec.INTEGER), 
                (data, value) -> data.xpToNextLevel = value, 
                data -> data.xpToNextLevel)
            .build();

    private int level = 1;
    private int currentXP = 0;
    private int xpToNextLevel = 100;

    public PlayerXPData() {
        // Default constructor for codec
    }

    public PlayerXPData(int level, int currentXP, int xpToNextLevel) {
        this.level = level;
        this.currentXP = currentXP;
        this.xpToNextLevel = xpToNextLevel;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getCurrentXP() {
        return currentXP;
    }

    public void setCurrentXP(int currentXP) {
        this.currentXP = currentXP;
    }

    public int getXpToNextLevel() {
        return xpToNextLevel;
    }

    public void setXpToNextLevel(int xpToNextLevel) {
        this.xpToNextLevel = xpToNextLevel;
    }

    /**
     * Calculates the XP required for a given level.
     */
    public static int calculateXPForLevel(int level) {
        return 100 * level;
    }

    /**
     * Resets the XP data to defaults.
     */
    public void reset() {
        this.level = 1;
        this.currentXP = 0;
        this.xpToNextLevel = 100;
    }

    @Override
    public String toString() {
        return "PlayerXPData{level=" + level + ", currentXP=" + currentXP + ", xpToNextLevel=" + xpToNextLevel + "}";
    }
}

