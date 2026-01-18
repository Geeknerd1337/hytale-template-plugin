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
 * Command to reset your XP to level 1.
 * Usage: /resetxp
 */
public class ResetXPCommand extends CommandBase {

    public ResetXPCommand() {
        super("resetxp", "Resets your XP to level 1.");
        this.setPermissionGroup(GameMode.Adventure); // Anyone can use it
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
            ctx.sendMessage(Message.raw("XP Bar not initialized. Please rejoin the server."));
            return;
        }

        int oldLevel = xpBar.getLevel();
        
        // Reset their XP
        xpManager.resetExperience(playerId);
        
        ctx.sendMessage(Message.raw("Your XP has been reset! (Was level " + oldLevel + ", now level 1)"));
        
        // Show a title notification
        Message title = Message.raw("XP Reset");
        Message subtitle = Message.raw("Back to level 1");
        EventTitleUtil.showEventTitleToPlayer(playerRef, title, subtitle, false);
    }
}

