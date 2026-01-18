package org.example.plugin;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

/**
 * A custom HUD element that displays an experience bar on the player's screen.
 * The bar shows the current level and XP progress to the next level.
 */
public class ExperienceBarHud extends CustomUIHud {

    private static final int BAR_WIDTH = 316; // Total fill width (320 - 4 for padding)
    
    private int level = 1;
    private int currentXP = 0;
    private int xpToNextLevel = 100;

    public ExperienceBarHud(@Nonnull PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder commandBuilder) {
        // Load the UI template (path relative to Common/UI/Custom/)
        commandBuilder.append("ExperienceBar.ui");
        
        // Set initial values
        updateDisplay(commandBuilder);
    }

    /**
     * Updates the visual display of the XP bar.
     */
    private void updateDisplay(@Nonnull UICommandBuilder commandBuilder) {
        // Calculate fill width based on XP percentage
        float percent = (float) currentXP / (float) xpToNextLevel;
        int fillWidth = (int) (percent * BAR_WIDTH);
        
        // Update the fill bar by setting the entire Anchor object
        Anchor fillAnchor = new Anchor();
        fillAnchor.setLeft(Value.of(2));
        fillAnchor.setTop(Value.of(2));
        fillAnchor.setBottom(Value.of(2));
        fillAnchor.setWidth(Value.of(fillWidth));
        commandBuilder.setObject("#Fill.Anchor", fillAnchor);
        
        // Update the level text (use TextSpans for dynamic updates)
        commandBuilder.set("#LevelText.TextSpans", Message.raw("Level " + level));
        
        // Update the XP text
        commandBuilder.set("#XPText.TextSpans", Message.raw(currentXP + "/" + xpToNextLevel + " XP"));
    }

    /**
     * Sets the experience values and updates the HUD.
     *
     * @param level The current level
     * @param currentXP The current XP amount
     * @param xpToNextLevel The XP required to reach the next level
     */
    public void setExperience(int level, int currentXP, int xpToNextLevel) {
        this.level = level;
        this.currentXP = currentXP;
        this.xpToNextLevel = xpToNextLevel;

        // Push the update to the client
        UICommandBuilder commandBuilder = new UICommandBuilder();
        updateDisplay(commandBuilder);
        this.update(false, commandBuilder);
    }

    /**
     * Adds XP and handles level ups.
     *
     * @param amount The amount of XP to add
     * @return true if the player leveled up
     */
    public boolean addExperience(int amount) {
        this.currentXP += amount;
        boolean leveledUp = false;

        // Handle level up(s)
        while (this.currentXP >= this.xpToNextLevel) {
            this.currentXP -= this.xpToNextLevel;
            this.level++;
            this.xpToNextLevel = calculateXPForLevel(this.level);
            leveledUp = true;
        }

        // Update the display
        UICommandBuilder commandBuilder = new UICommandBuilder();
        updateDisplay(commandBuilder);
        this.update(false, commandBuilder);

        return leveledUp;
    }

    /**
     * Calculates the XP required for a given level.
     * Uses a simple scaling formula: 100 * level
     */
    private int calculateXPForLevel(int level) {
        return 100 * level;
    }

    public int getLevel() {
        return level;
    }

    public int getCurrentXP() {
        return currentXP;
    }

    public int getXpToNextLevel() {
        return xpToNextLevel;
    }
}

