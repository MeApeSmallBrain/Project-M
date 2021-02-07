package dr.manhattan.external.api.player;

import dr.manhattan.external.api.M;
import dr.manhattan.external.api.calc.MCalc;
import dr.manhattan.external.api.interact.MMenuEntryInterceptor;
import dr.manhattan.external.api.items.MItemDefinition;
import dr.manhattan.external.api.mouse.MMouse;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.eventbus.EventBus;

import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Singleton
public class MInventory {
    private static final ConcurrentHashMap<Integer, WidgetItem> inventory = new ConcurrentHashMap<>();

    private final Object BUS_SUB = new Object();

    public static int getEmptySlots() {
        return 28 - inventory.size();
    }

    public static void openInventory() {
        Client client = M.getInstance().getClient();

        if (client == null || client.getGameState() != GameState.LOGGED_IN) {
            return;
        }
        client.runScript(915, 3); //open inventory
    }

    public static boolean dropAllExcept(String... items) {
        List<String> dontDrop = Arrays.asList(items);
        inventory.forEach((slot, widget) -> {
            ItemDefinition def = MItemDefinition.getDef(widget.getId());
            if (def == null) return;
            if (dontDrop.contains(def.getName())) return;
            dropAction(widget);
        });
        return true;
    }

    public static boolean drop(String... items) {
        List<String> dropList = Arrays.asList(items);
        Optional<Map.Entry<Integer, WidgetItem>> entry =  inventory.entrySet().stream().filter(e -> {
            ItemDefinition def = MItemDefinition.getDef(e.getValue().getId());
            return dropList.contains(def.getName());
        }).findFirst();

        if(entry.isEmpty()) return false;
        dropAction(entry.get().getValue());
        return true;
    }

    public static boolean dropAll(String... items) {
        List<String> dropList = Arrays.asList(items);
        inventory.forEach((slot, widget) -> {
            ItemDefinition def = MItemDefinition.getDef(widget.getId());
            if (def == null) return;
            if (!dropList.contains(def.getName())) return;
            dropAction(widget);
        });
        return true;
    }

    private static void dropAction(WidgetItem widget) {
        MMenuEntryInterceptor.setMenuEntry(
                new MenuEntry(
                        "",
                        "",
                        widget.getId(),
                        MenuOpcode.ITEM_DROP.getId(),
                        widget.getIndex(),
                        9764864,
                        false)
        );

        MMouse.delayMouseClick(widget.getCanvasBounds(), 0);
        try {
            Thread.sleep(MCalc.nextInt(300, 1000));
        } catch (InterruptedException e) {
            log.error("Drop all", e);
        }
    }

    public static boolean isFull() {
        return getEmptySlots() <= 0;
    }

    public static boolean isEmpty() {
        return getEmptySlots() >= 28;
    }

    public static boolean isOpen() {
        Client client = M.getInstance().getClient();

        if (client.getWidget(WidgetInfo.INVENTORY) == null) {
            return false;
        }
        return !client.getWidget(WidgetInfo.INVENTORY).isHidden();
    }

    public static void refreshInventory() {
        Collection<WidgetItem> items = getInvWidgets();
        inventory.clear();
        for (WidgetItem item : items) {
            inventory.put(item.getIndex(), item);
            MItemDefinition.checkID(item.getId());
        }
    }


    private static Collection<WidgetItem> getInvWidgets() {
        Widget inv = getInvWidget();
        if (inv == null) return null;
        return inv.getWidgetItems();
    }

    private static Widget getInvWidget() {
        Client client = M.getInstance().getClient();
        if (client == null) return null;
        return client.getWidget(WidgetInfo.INVENTORY);
    }

    public void start(EventBus eventBus) {
        eventBus.subscribe(ItemContainerChanged.class, BUS_SUB, this::itemContainerChanged);
    }

    public void stop(EventBus eventBus) {
        eventBus.unregister(BUS_SUB);
    }

    private void itemContainerChanged(ItemContainerChanged event) {
        if (event.getContainerId() == InventoryID.INVENTORY.getId()) {
            refreshInventory();
        }
    }
}
