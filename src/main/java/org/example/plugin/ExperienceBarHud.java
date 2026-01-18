package org.example.plugin;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A custom HUD element that displays an experience bar on the player's screen.
 * The bar shows the current level and XP progress to the next level.
 * Features animated floating popup text when XP is gained!
 */
public class ExperienceBarHud extends CustomUIHud {

    private static final int BAR_WIDTH = 636; // Total fill width (640 - 4 for padding)
    private static final int POPUP_DISPLAY_MS = 1200; // How long popup stays visible
    
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
        // Load the UI template - try without prefix since it's in Common/UI/Custom/
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
     * Shows an animated floating popup for XP gain!
     * Uses the notification system for level ups!
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
        
        // TODO: Re-enable popup once base UI is confirmed working
        // showAnimatedPopup("+" + amount + " XP", "#4ade80");
        
        // Use notification system for level ups
        if (leveledUp) {
            NotificationUtil.sendNotification(
                getPlayerRef().getPacketHandler(),
                Message.raw("Level Up!"),
                Message.raw("You reached level " + level + "!"),
                NotificationStyle.Success
            );
        }

        return leveledUp;
    }
    
    /**
     * Shows a floating popup above the XP bar that disappears after a delay.
     * Positioned using HorizontalCenter in the UI markup for proper centering.
     */
    private void showAnimatedPopup(String text, String color) {
        int popupId = popupCounter.incrementAndGet();
        String popupSelector = "#Popup" + popupId;
        
        // Create popup with absolute positioning (centered above XP bar)
        // Using HorizontalCenter: 0 to center it on screen
        UICommandBuilder createBuilder = new UICommandBuilder();
        String popupUI = "Label " + popupSelector + " { " +
                "Anchor: (Bottom: 185, Height: 32, Width: 200, HorizontalCenter: 0); " +
                "Style: (FontSize: 20, HorizontalAlignment: Center, VerticalAlignment: Center, " +
                "TextColor: " + color + ", RenderBold: true); " +
                "Text: \"" + text + "\"; " +
                "}";
        
        // Append directly to root (selector works after document is loaded)
        createBuilder.appendInline("#Root", popupUI);
        this.update(false, createBuilder);
        
        // Schedule removal after delay
        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            UICommandBuilder removeBuilder = new UICommandBuilder();
            removeBuilder.remove(popupSelector);
            this.update(false, removeBuilder);
        }, POPUP_DISPLAY_MS, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Shows a custom animated popup with any message and color.
     */
    public void showCustomPopup(String text, String hexColor) {
        showAnimatedPopup(text, hexColor);
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

