package dr.manhattan.external.api.npcs;

import com.google.inject.Singleton;
import dr.manhattan.external.api.M;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.NPCDefinition;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@Slf4j
public class MNpcCache {
    private static final Object BUS_SUB = new Object();
    static private final ConcurrentHashMap<Integer, NPC> npcs = new ConcurrentHashMap<>();


    static public List<NPC> getNpcs() {
        List<NPC> npcList = new ArrayList<>();
        for (NPC npc : npcs.values()) {
            npcList.add(npc);
        }
        return npcList;
    }
    static boolean refreshing = false;

    static public void refreshNpcs() {
        refreshing = true;
        if (M.client() == null) {
            return;
        }

        if (M.client().getGameState() != GameState.LOGGED_IN) {
            return;
        }
        npcs.clear();
        M.client().getNpcs().forEach(MNpcCache::addNpc);
        refreshing = false;
    }

    private static void addNpc(NPC npc) {
        if (npc == null) {
            return;
        }

        NPCDefinition def = MNpcDefinition.getDef(npc.getId());
        if (def == null) return;

        npcs.put(npc.getIndex(), npc);
    }


    public void start(EventBus eventBus) {
        eventBus.subscribe(GameTick.class, BUS_SUB, this::gameTick);
    }

    public void stop(EventBus eventBus) {
        eventBus.unregister(BUS_SUB);
    }


    private void gameTick(GameTick event) {
        if(!refreshing) refreshNpcs();
    }
}
