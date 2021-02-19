package dr.manhattan.external.api.util;

import dr.manhattan.external.api.M;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuEntryAdded;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class MMenu {
    public static void addEntry(MenuEntryAdded event, String option) {
        List<MenuEntry> entries = new LinkedList<>(Arrays.asList(M.client().getMenuEntries()));

        if (entries.stream().anyMatch(e -> e.getOption().equals(option))) {
            return;
        }

        MenuEntry entry = new MenuEntry();
        entry.setOption(option);
        entry.setTarget(event.getTarget());
        entries.add(0, entry);

        M.client().setMenuEntries(entries.toArray(new MenuEntry[0]));
    }

}
