package org.example.plugin;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A custom HUD element that displays an experience bar on the player's screen.
 * The bar shows the current level and XP progress to the next level.
 * Features floating popup text when XP is gained!
 */
public class ExperienceBarHud extends CustomUIHud {

    private static final int BAR_WIDTH = 636; // Total fill width (640 - 4 for padding)
    private static final int POPUP_DURATION_MS = 2000; // How long popups stay visible
    
    private int level = 1;
    private int currentXP = 0;
    private int xpToNextLevel = 100;
    
    // Counter for unique popup IDs
    private final AtomicInteger popupCounter = new AtomicInteger(0);

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
     * Use setInitialExperience() if called before the HUD is shown.
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
     * Sets initial experience values WITHOUT sending an update.
     * Use this when restoring data BEFORE showing the HUD.
     * The values will be applied when build() is called.
     */
    public void setInitialExperience(int level, int currentXP, int xpToNextLevel) {
        this.level = level;
        this.currentXP = currentXP;
        this.xpToNextLevel = xpToNextLevel;
        // Don't call update() - build() will use these values
    }

    /**
     * Adds XP and handles level ups.
     * Shows a floating popup with the XP gained!
     *
     * @param amount The amount of XP to add
     * @return true if the player leveled up
     */
    public boolean addExperience(int amount) {
        this.currentXP += amount;
        boolean leveledUp = false;
        int levelsGained = 0;

        // Handle level up(s)
        while (this.currentXP >= this.xpToNextLevel) {
            this.currentXP -= this.xpToNextLevel;
            this.level++;
            this.xpToNextLevel = calculateXPForLevel(this.level);
            leveledUp = true;
            levelsGained++;
        }

        // Update the display and show popup
        UICommandBuilder commandBuilder = new UICommandBuilder();
        updateDisplay(commandBuilder);
        
        // Show floating XP popup
        if (leveledUp) {
            showPopup(commandBuilder, "LEVEL UP! " + level, "#fbbf24"); // Gold color for level up
        } else {
            showPopup(commandBuilder, "+" + amount + " XP", "#4ade80"); // Green for normal XP
        }
        
        this.update(false, commandBuilder);

        return leveledUp;
    }
    
    /**
     * Shows a floating popup text above the XP bar.
     * The popup automatically disappears after POPUP_DURATION_MS.
     */
    private void showPopup(@Nonnull UICommandBuilder commandBuilder, String text, String color) {
        int popupId = popupCounter.incrementAndGet();
        String popupSelector = "#Popup" + popupId;
        
        // Create the popup inline (dynamically generated UI)
        String popupUI = "Label " + popupSelector + " { " +
                "Anchor: (Height: 28, Width: 200); " +
                "Style: (FontSize: 18, HorizontalAlignment: Center, VerticalAlignment: Center, " +
                "TextColor: " + color + ", RenderBold: true); " +
                "Text: \"" + text + "\"; " +
                "}";
        
        commandBuilder.appendInline("#PopupContainer", popupUI);
        
        // Schedule removal of the popup after delay
        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            UICommandBuilder removeBuilder = new UICommandBuilder();
            removeBuilder.remove(popupSelector);
            this.update(false, removeBuilder);
        }, POPUP_DURATION_MS, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Shows a custom popup with any message and color.
     */
    public void showCustomPopup(String text, String hexColor) {
        UICommandBuilder commandBuilder = new UICommandBuilder();
        showPopup(commandBuilder, text, hexColor);
        this.update(false, commandBuilder);
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

