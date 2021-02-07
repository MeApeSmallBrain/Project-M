package dr.manhattan.external.api.objects;

import dr.manhattan.external.api.M;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameObjectChanged;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.client.eventbus.EventBus;

import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Singleton
public class MObjectCache {


    private static final Object BUS_SUB = new Object();
    static private ConcurrentHashMap<Long, GameObject> objects = new ConcurrentHashMap<>();
    private static Instant lastRefresh = null;


    static public List<GameObject> getObjects() {
        List<GameObject> goList = new ArrayList<>();
        for (GameObject go : objects.values()) {
            goList.add(go);
        }
        return goList;
    }

    static public void refreshObjects() {
        if (M.client() == null) {
            return;
        }
        if (M.client().getLocalPlayer() == null) {
            return;
        }
        lastRefresh = Instant.now();
        Scene scene = M.client().getScene();
        Tile[][][] tiles = scene.getTiles();
        Collection<GameObject> objectsCache = new ArrayList<>();
        int z = M.client().getPlane();
        for (int x = 0; x < Constants.SCENE_SIZE; ++x) {
            for (int y = 0; y < Constants.SCENE_SIZE; ++y) {
                Tile tile = tiles[z][x][y];
                if (tile == null) {
                    continue;
                }
                GameObject[] gameObjects = tile.getGameObjects();
                if (gameObjects != null) {
                    objectsCache.addAll(Arrays.asList(gameObjects));
                }
            }
        }
        objects.clear();
        for (GameObject go : objectsCache) {
            addObject(go);
        }
    }

    private static void addObject(GameObject go) {


        if (lastRefresh == null || lastRefresh.isBefore(Instant.now().minus(Duration.ofMinutes(5)))) {
            refreshObjects();
            return;
        }
        if (go == null) {
            return;
        }

        if(!M.client().isClientThread()) return;

        ObjectDefinition def = M.client().getObjectDefinition(go.getId());
        MObjectDefinition.checkID(go.getId(), def);


        if (def.getName().equalsIgnoreCase("null")) {
            return;
        }
        objects.put(go.getHash(), go);
    }

    private static void removeObject(GameObject go) {
        objects.remove(go.getHash(), go);
    }

    public void start(EventBus eventBus) {
        eventBus.subscribe(GameObjectSpawned.class, BUS_SUB, this::objectSpawned);
        eventBus.subscribe(GameObjectDespawned.class, BUS_SUB, this::objectDepawned);
        eventBus.subscribe(GameObjectChanged.class, BUS_SUB, this::objectChanged);

    }


    public void stop(EventBus eventBus) {
        eventBus.unregister(BUS_SUB);
    }



    private void objectChanged(final GameObjectChanged event) {
        removeObject(event.getPrevious());
        addObject(event.getGameObject());
    }

    private void objectDepawned(final GameObjectDespawned event) {
        removeObject(event.getGameObject());
    }

    private void objectSpawned(final GameObjectSpawned event) { addObject(event.getGameObject()); }
}
