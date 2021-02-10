package dr.manhattan.external.api.objects;

import dr.manhattan.external.api.M;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.LocatableQueryResults;
import net.runelite.api.ObjectDefinition;
import net.runelite.api.queries.TileObjectQuery;

import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
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
                if (def == null){
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
