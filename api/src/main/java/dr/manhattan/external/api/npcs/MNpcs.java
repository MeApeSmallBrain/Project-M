package dr.manhattan.external.api.npcs;

import dr.manhattan.external.api.astar.AStar;
import dr.manhattan.external.api.astar.AStarPath;
import dr.manhattan.external.api.player.MPlayer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.LocatableQueryResults;
import net.runelite.api.NPC;
import net.runelite.api.NPCDefinition;
import net.runelite.api.queries.ActorQuery;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class MNpcs extends ActorQuery<NPC, MNpcs> {
    public MNpcs isID(Integer... ids){
        return isID(Arrays.asList(ids));
    }
    public MNpcs isID(Collection<Integer> ids)
    {
        predicate = and(actor -> ids.contains(actor.getId()));
        return this;
    }

    public MNpcs hasAction(String... actions) {
        predicate = and(object ->
        {
            for (String action : actions) {
                NPCDefinition def = MNpcDefinition.getDef(object.getId());
                if (def == null) return false;
                for (String a : def.getActions()) {
                    if (a == null) continue;
                    if (a.equals(action)) return true;
                }
            }
            return false;
        });
        return this;
    }

    public MNpcs isReachable(){
        predicate = and(object -> {
            AStarPath path = new AStar().getPath(object.getLocalLocation());
            log.info("Distance to " + object.getName() + " is " + path.getDistanceToDestination());
            return (path.getDistanceToDestination() < Integer.MAX_VALUE);
        });
        return this;
    }
    public MNpcs isInteracting(){
        predicate = and(object -> {
            return object.getInteracting() != null;
        });
        return this;
    }

    public MNpcs notInteracting(){
        predicate = and(object -> {
            return object.getInteracting() == null;
        });
        return this;
    }
    public MNpcs notDead(){
        predicate = and(object -> {
            return !object.isDead();
        });
        return this;
    }
    public NPC starNearest() {
        return starNearest(5);
    }
    public NPC starNearest(int limit){
        List<NPC> npcs = MNpcCache.getNpcs().stream()
                .filter(predicate)
                .sorted(
                        Comparator.comparing(
                                (object -> MPlayer.get().getLocalLocation().distanceTo(object.getLocalLocation()))
                        )
                )
                .limit(limit)
                .collect(Collectors.toList());

        npcs.sort((NPC n1, NPC n2) -> {
            int cost1 = new AStar().getPath(n1).getCost();
            int cost2 = new AStar().getPath(n2).getCost();
            return cost1 - cost2;
        });
        if (npcs.size() < 1) return null;
        else return npcs.get(0);
    }

    @Override
    public LocatableQueryResults<NPC> result(Client client)
    {
        return result();
    }
    public LocatableQueryResults<NPC> result()
    {
        return new LocatableQueryResults(MNpcCache.getNpcs().stream()
                .filter(predicate)
                .collect(Collectors.toList()));
    }
}
