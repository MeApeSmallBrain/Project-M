package dr.manhattan.external.api.npcs;

import net.runelite.api.Client;
import net.runelite.api.LocatableQueryResults;
import net.runelite.api.NPC;
import net.runelite.api.NPCDefinition;
import net.runelite.api.queries.ActorQuery;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

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

    @Override
    public LocatableQueryResults<NPC> result(Client client)
    {
        return result();
    }
    public LocatableQueryResults<NPC> result()
    {
        return new LocatableQueryResults<>(MNpcCache.getNpcs().stream()
                .filter(predicate)
                .collect(Collectors.toList()));
    }
}
