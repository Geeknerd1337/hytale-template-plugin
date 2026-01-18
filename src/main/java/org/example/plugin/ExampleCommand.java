package org.example.plugin;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.EventTitleUtil;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This is an example command that will simply print the name of the plugin in chat when used.
 * Also demonstrates showing a custom Experience Bar HUD.
 */
public class ExampleCommand extends CommandBase {

    private final String pluginName;
    private final String pluginVersion;
    
    // Store XP bars per player (in a real plugin, use a proper player data system)
    private final Map<UUID, ExperienceBarHud> playerXPBars = new HashMap<>();

    public ExampleCommand(String pluginName, String pluginVersion) {
        super("test", "Prints a test message from the " + pluginName + " plugin.");
        this.setPermissionGroup(GameMode.Adventure); // Allows the command to be used by anyone, not just OP
        this.pluginName = pluginName;
        this.pluginVersion = pluginVersion;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        // Send chat message
        ctx.sendMessage(Message.raw("Hello from the " + pluginName + " v" + pluginVersion + " plugin!"));

        // Show title and XP bar on screen (only works if sender is a player)
        if (ctx.isPlayer()) {
            Player player = ctx.senderAs(Player.class);
            PlayerRef playerRef = player.getPlayerRef();
            HudManager hudManager = player.getHudManager();
            
            // Show "Hello World" title
            Message title = Message.raw("Hello World!");
            Message subtitle = Message.raw("From " + pluginName);
            EventTitleUtil.showEventTitleToPlayer(playerRef, title, subtitle, true);
            
            // Get or create the XP bar for this player
            UUID playerId = playerRef.getUuid();
            ExperienceBarHud xpBar = playerXPBars.get(playerId);
            
            if (xpBar == null) {
                // First time - create and show the XP bar
                xpBar = new ExperienceBarHud(playerRef);
                playerXPBars.put(playerId, xpBar);
                hudManager.setCustomHud(playerRef, xpBar);
                ctx.sendMessage(Message.raw("XP Bar enabled! Run /test again to gain XP."));
            } else {
                // Already have an XP bar - add some XP!
                boolean leveledUp = xpBar.addExperience(25);
                
                if (leveledUp) {
                    Message levelUpTitle = Message.raw("LEVEL UP!");
                    Message levelUpSubtitle = Message.raw("You are now level " + xpBar.getLevel());
                    EventTitleUtil.showEventTitleToPlayer(playerRef, levelUpTitle, levelUpSubtitle, true);
                    ctx.sendMessage(Message.raw("Congratulations! You reached level " + xpBar.getLevel() + "!"));
                } else {
                    ctx.sendMessage(Message.raw("+25 XP! (" + xpBar.getCurrentXP() + "/" + xpBar.getXpToNextLevel() + ")"));
                }
            }
        }
    }
}