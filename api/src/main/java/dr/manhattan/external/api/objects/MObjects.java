package dr.manhattan.external.api.objects;

import dr.manhattan.external.api.M;
import dr.manhattan.external.api.astar.AStar;
import dr.manhattan.external.api.astar.AStarPath;
import dr.manhattan.external.api.npcs.MNpcCache;
import dr.manhattan.external.api.npcs.MNpcs;
import dr.manhattan.external.api.player.MPlayer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.queries.TileObjectQuery;

import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class MObjects extends TileObjectQuery<GameObject, MObjects> {

    public MObjects hasName(Collection<String> names) {
        predicate = and(object ->
        {
            for (String name : names) {
                ObjectDefinition def = MObjectDefinition.getDef(object.getId());
                if (def == null) return false;
                if (def.getName().equals(name)) {
                    return true;
                }
            }
            return false;
        });
        return this;
    }

    public MObjects hasName(String... names) {
        return hasName(Arrays.asList(names));
    }

    public MObjects hasAction(String... actions) {
        predicate = and(object ->
        {
            for (String action : actions) {
                ObjectDefinition def = MObjectDefinition.getDef(object.getId());
                if (def == null) {
                    return false;
                }
                for (String a : def.getActions()) {
                    if (a == null) continue;
                    if (a.equals(action)) return true;
                }
            }
            return false;
        });
        return this;
    }
    public MObjects isReachable(){
        predicate = and(object -> {
            AStarPath path = new AStar().getPath(object.getLocalLocation());
            return (path.getDistanceToDestination() < Integer.MAX_VALUE);
        });
        return this;
    }

    public GameObject starNearest() {
        List<GameObject> objects = MObjectCache.getObjects().stream()
                .filter(Objects::nonNull)
                .filter(predicate)
                .distinct()
                .sorted(
                        Comparator.comparing(
                                (object -> MPlayer.get().getLocalLocation().distanceTo(object.getLocalLocation()))
                        )
                )
                .limit(20)
                .collect(Collectors.toList());


        objects.sort((GameObject go1, GameObject go2) -> {
            int cost1 = new AStar().getPath(go1).getCost();
            int cost2 = new AStar().getPath(go2).getCost();
            return cost1 - cost2;
        });

        if (objects.size() < 1) return null;
        else return objects.get(0);
    }

    public LocatableQueryResults<GameObject> result() {
        return result(M.client());
    }

    @Override
    public LocatableQueryResults<GameObject> result(Client client) {
        return new LocatableQueryResults<>(MObjectCache.getObjects().stream()
                .filter(Objects::nonNull)
                .filter(predicate)
                .distinct()
                .collect(Collectors.toList()));
    }
}
