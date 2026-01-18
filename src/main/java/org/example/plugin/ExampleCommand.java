package org.example.plugin;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.EventTitleUtil;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * This is an example command that adds XP to the player.
 * The XP bar is automatically shown when the player joins (see ExamplePlugin).
 */
public class ExampleCommand extends CommandBase {

    private final String pluginName;
    private final String pluginVersion;

    public ExampleCommand(String pluginName, String pluginVersion) {
        super("test", "Adds 25 XP to your experience bar.");
        this.setPermissionGroup(GameMode.Adventure); // Allows the command to be used by anyone, not just OP
        this.pluginName = pluginName;
        this.pluginVersion = pluginVersion;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("This command can only be used by players."));
            return;
        }

        Player player = ctx.senderAs(Player.class);
        PlayerRef playerRef = player.getPlayerRef();
        UUID playerId = playerRef.getUuid();
        
        ExperienceManager xpManager = ExperienceManager.getInstance();
        ExperienceBarHud xpBar = xpManager.getXPBar(playerId);
        
        if (xpBar == null) {
            // XP bar not initialized (shouldn't happen if player join event fired)
            ctx.sendMessage(Message.raw("XP Bar not found. Initializing..."));
            xpManager.initializePlayer(player, playerRef);
            xpBar = xpManager.getXPBar(playerId);
        }
        
        // Add XP
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