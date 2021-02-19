package dr.manhattan.external.api.npcs;

import dr.manhattan.external.api.M;
import net.runelite.api.NPCDefinition;

import java.util.concurrent.ConcurrentHashMap;

public class MNpcDefinition {
    private static final ConcurrentHashMap<Integer, NPCDefinition> defCache = new ConcurrentHashMap<>();


    public static void checkID(int id) {
        if (defCache.containsKey(id)) return;
        NPCDefinition def = M.client().getNpcDefinition(id);
        defCache.put(id, def);
    }

    public static void checkID(int id, NPCDefinition def) {
        if (defCache.containsKey(id)) return;
        defCache.put(id, def);
    }

    public static NPCDefinition getDef(int id) {
        if (!defCache.containsKey(id)) {
            if (M.client().isClientThread())
                checkID(id);
            else return null;
        }
        return defCache.get(id);
    }
}
