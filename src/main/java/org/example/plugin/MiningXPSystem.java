package org.example.plugin;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * ECS System that grants XP when a player breaks rock or dirt with a pickaxe.
 */
public class MiningXPSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    // XP amount per block mined
    private static final int XP_PER_BLOCK = 1;
    
    // Block types that give XP (partial match - block ID contains these strings)
    private static final Set<String> MINEABLE_BLOCKS = Set.of(
        "Stone",
        "Rock",
        "Dirt",
        "Cobblestone",
        "Granite",
        "Sandstone",
        "Ore"
    );
    
    // Item types that count as pickaxes (partial match)
    private static final Set<String> PICKAXE_ITEMS = Set.of(
        "Pickaxe",
        "Pick"
    );

    public MiningXPSystem() {
        super(BreakBlockEvent.class);
    }

    @Override
    @Nullable
    public Query<EntityStore> getQuery() {
        // Match all entities (no specific component requirements)
        return Archetype.empty();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, 
                       @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, 
                       @Nonnull BreakBlockEvent event) {
        
        // Get the entity that broke the block
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        
        // Check if it's a player
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return; // Not a player, ignore
        }
        
        // Get the PlayerRef for XP management
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        
        // Check if the block is a mineable type
        BlockType blockType = event.getBlockType();
        String blockId = blockType.getId();
        
        boolean isMineableBlock = MINEABLE_BLOCKS.stream()
            .anyMatch(type -> blockId.toLowerCase().contains(type.toLowerCase()));
        
        if (!isMineableBlock) {
            return; // Not a block we give XP for
        }
        
        // Check if the player is using a pickaxe
        ItemStack itemInHand = event.getItemInHand();
        if (itemInHand == null || itemInHand.isEmpty()) {
            return; // No tool in hand
        }
        
        String itemId = itemInHand.getItemId();
        boolean isPickaxe = PICKAXE_ITEMS.stream()
            .anyMatch(type -> itemId.toLowerCase().contains(type.toLowerCase()));
        
        if (!isPickaxe) {
            return; // Not using a pickaxe
        }
        
        // Give XP to the player!
        ExperienceManager.getInstance().giveExperience(playerRef.getUuid(), XP_PER_BLOCK);
        
        // Optional: Log for debugging
        // LOGGER.atFine().log("Player " + playerRef.getUsername() + " mined " + blockId + " with " + itemId + ", gained " + XP_PER_BLOCK + " XP");
    }
}

