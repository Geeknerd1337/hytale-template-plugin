package org.example.plugin;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.EventTitleUtil;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Command to give XP to yourself.
 * Usage: /givexp [amount]
 */
public class GiveXPCommand extends CommandBase {

    @Nonnull
    private final DefaultArg<Integer> amountArg = this.withDefaultArg(
            "amount", 
            "Amount of XP to give", 
            ArgTypes.INTEGER, 
            100,  // Default amount
            "Default: 100 XP"
    );

    public GiveXPCommand() {
        super("givexp", "Gives XP to yourself. Usage: /givexp [amount]");
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
        
        int amount = amountArg.get(ctx);
        
        if (amount <= 0) {
            ctx.sendMessage(Message.raw("Amount must be positive!"));
            return;
        }

        ExperienceManager xpManager = ExperienceManager.getInstance();
        ExperienceBarHud xpBar = xpManager.getXPBar(playerId);
        
        if (xpBar == null) {
            ctx.sendMessage(Message.raw("XP Bar not initialized. Please rejoin the server."));
            return;
        }

        int startLevel = xpBar.getLevel();
        int levelsGained = xpManager.giveExperience(playerId, amount);
        
        ctx.sendMessage(Message.raw("+" + amount + " XP! (" + xpBar.getCurrentXP() + "/" + xpBar.getXpToNextLevel() + ")"));
        
        if (levelsGained > 0) {
            Message title = Message.raw("LEVEL UP!");
            Message subtitle = Message.raw("You are now level " + xpBar.getLevel() + 
                    (levelsGained > 1 ? " (+" + levelsGained + " levels!)" : ""));
            EventTitleUtil.showEventTitleToPlayer(playerRef, title, subtitle, true);
            ctx.sendMessage(Message.raw("Congratulations! You gained " + levelsGained + " level(s)!"));
        }
    }
}

