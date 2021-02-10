package dr.manhattan.external.api.pickups;

import dr.manhattan.external.api.M;
import dr.manhattan.external.api.items.MItemDefinition;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemQuantityChanged;
import net.runelite.api.events.ItemSpawned;
import net.runelite.client.eventbus.EventBus;

import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Singleton
@Slf4j
public class MPickupCache {
    static private final CopyOnWriteArrayList<TileItem> pickups = new CopyOnWriteArrayList<>();
    private static final Object BUS_SUB = new Object();

    static Instant lastRefresh = Instant.now().minus(Duration.ofHours(1));

    static public void refreshPickups() {
        if (M.client() == null) {
            return;
        }
        if (M.client().getGameState() != GameState.LOGGED_IN) {
            return;
        }
        log.info("Refreshing Pickups...");
        lastRefresh = Instant.now();
        Scene scene = M.client().getScene();
        Tile[][][] tiles = scene.getTiles();
        Collection<TileItem> pickupsCache = new ArrayList<>();
        int z = M.client().getPlane();
        for (int x = 0; x < Constants.SCENE_SIZE; ++x) {
            for (int y = 0; y < Constants.SCENE_SIZE; ++y) {
                Tile tile = tiles[z][x][y];
                if (tile == null) {
                    continue;
                }
                List<TileItem> pickupsOnTile = tile.getGroundItems();
                if (pickupsOnTile != null) {
                    pickupsCache.addAll(pickupsOnTile);
                }
            }
        }
        pickups.clear();
        log.info("Pickup cache: " + pickupsCache.size());
        for (TileItem ti : pickupsCache) {
            addPickup(ti);
        }
        log.info(pickups.size() + " Pickups refreshed!");
    }

    private static void addPickup(TileItem ti) {

        if (ti == null) {
            return;
        }
        if (lastRefresh.isBefore(Instant.now().minusSeconds(60))) {
            refreshPickups();
            return;
        }

        if (!M.client().isClientThread()) return;

        ItemDefinition def = MItemDefinition.getDef(ti.getId());

        if (def.getName().equalsIgnoreCase("null")) {
            return;
        }
        pickups.add(ti);
    }

    private static void removePickup(TileItem ti) {
        if (lastRefresh.isBefore(Instant.now().minusSeconds(60))) {
            refreshPickups();
            return;
        }
        pickups.remove(ti);
    }

    public void start(EventBus eventBus) {
        eventBus.subscribe(ItemSpawned.class, BUS_SUB, this::itemSpawned);
        eventBus.subscribe(ItemDespawned.class, BUS_SUB, this::itemDespawned);
        eventBus.subscribe(ItemQuantityChanged.class, BUS_SUB, this::itemChanged);
        eventBus.subscribe(GameStateChanged.class, BUS_SUB, this::gameStateChanged);

    }


    public void stop(EventBus eventBus) {
        eventBus.unregister(BUS_SUB);
    }


    private void gameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOGGED_IN) refreshPickups();
    }

    private void itemChanged(ItemQuantityChanged event) {
        removePickup(event.getItem());
        addPickup(event.getItem());
    }

    private void itemDespawned(ItemDespawned event) {
        removePickup(event.getItem());
    }

    private void itemSpawned(ItemSpawned event) {
        addPickup(event.getItem());
    }
}
