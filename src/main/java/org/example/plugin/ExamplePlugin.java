package org.example.plugin;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

/**
 * Example Plugin with XP System
 * 
 * Commands:
 * - /test - Adds 25 XP
 * - /givexp [amount] - Gives specified amount of XP (default 100)
 * - /resetxp - Resets XP to level 1
 * 
 * Features:
 * - XP bar shown automatically on join
 * - XP data persists between sessions
 */
public class ExamplePlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public ExamplePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Hello from " + this.getName() + " version " + this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up plugin " + this.getName());
        
        // Register commands
        this.getCommandRegistry().registerCommand(new ExampleCommand(this.getName(), this.getManifest().getVersion().toString()));
        this.getCommandRegistry().registerCommand(new GiveXPCommand());
        this.getCommandRegistry().registerCommand(new ResetXPCommand());
        
        // Register player events
        this.getEventRegistry().register(PlayerConnectEvent.class, this::onPlayerConnect);
        this.getEventRegistry().register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
        
        LOGGER.atInfo().log("Registered commands: /test, /givexp, /resetxp");
        LOGGER.atInfo().log("XP data will be saved to plugins/ExamplePlugin/xp_data.json");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down plugin - saving XP data...");
        ExperienceManager.getInstance().saveData();
        LOGGER.atInfo().log("XP data saved successfully!");
    }

    /**
     * Called when a player connects to the server.
     * Initializes and shows their XP bar, restoring saved data if available.
     */
    private void onPlayerConnect(PlayerConnectEvent event) {
        Player player = event.getPlayer();
        PlayerRef playerRef = event.getPlayerRef();
        
        if (player != null && playerRef != null) {
            LOGGER.atInfo().log("Player connected: " + playerRef.getUsername() + " - Initializing XP bar");
            ExperienceManager.getInstance().initializePlayer(player, playerRef);
        }
    }

    /**
     * Called when a player disconnects from the server.
     * Saves their XP data and removes the HUD reference.
     */
    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        
        if (playerRef != null) {
            LOGGER.atInfo().log("Player disconnected: " + playerRef.getUsername() + " - Saving XP data");
            ExperienceManager.getInstance().removePlayer(playerRef.getUuid());
            // Save after each disconnect to be safe
            ExperienceManager.getInstance().saveData();
        }
    }
}