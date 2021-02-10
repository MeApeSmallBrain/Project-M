package dr.manhattan.external.api.interact;

import dr.manhattan.external.api.mouse.MMouse;
import dr.manhattan.external.api.npcs.MNpcDefinition;
import dr.manhattan.external.api.objects.MObjectDefinition;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class MInteract {


    public static boolean gameObject(GameObject go, String... actions) {

        ObjectDefinition def = MObjectDefinition.getDef(go.getId());
        String[] goActions = def.getActions();
        int actionIndex = -1;
        String action = "";
        List<String> actionList = Arrays.asList(actions);
        for (int op = 0; op < goActions.length; op++) {
            if (actionList.contains(goActions[op])) {
                actionIndex = op;
                action = goActions[op];
                break;
            }
        }

        MenuOpcode actionOp = null;
        switch (actionIndex) {
            case 0:
                actionOp = MenuOpcode.GAME_OBJECT_FIRST_OPTION;
                break;
            case 1:
                actionOp = MenuOpcode.GAME_OBJECT_SECOND_OPTION;
                break;
            case 2:
                actionOp = MenuOpcode.GAME_OBJECT_THIRD_OPTION;
                break;
            case 3:
                actionOp = MenuOpcode.GAME_OBJECT_FOURTH_OPTION;
                break;
            case 4:
                actionOp = MenuOpcode.GAME_OBJECT_FIFTH_OPTION;
                break;
            default:
                return false;
        }

        MMenuEntryInterceptor.setMenuEntry(
                new MenuEntry(
                        "",
                        "",
                        go.getId(),
                        actionOp.getId(),
                        go.getSceneMinLocation().getX(),
                        go.getSceneMinLocation().getY(),
                        false
                )
        );

        MMouse.delayMouseClick(go.getConvexHull(), 0);
        return true;
    }

    public static boolean npc(NPC npc, String... actions) {

        NPCDefinition def = MNpcDefinition.getDef(npc.getId());
        String[] npcActions = def.getActions();
        int actionIndex = -1;
        String action = "";
        List<String> actionList = Arrays.asList(actions);
        for (int op = 0; op < npcActions.length; op++) {
            if (actionList.contains(npcActions[op])) {
                actionIndex = op;
                action = npcActions[op];
                break;
            }
        }

        MenuOpcode actionOp = null;
        switch (actionIndex) {
            case 0:
                actionOp = MenuOpcode.NPC_FIRST_OPTION;
                break;
            case 1:
                actionOp = MenuOpcode.NPC_SECOND_OPTION;
                break;
            case 2:
                actionOp = MenuOpcode.NPC_THIRD_OPTION;
                break;
            case 3:
                actionOp = MenuOpcode.NPC_FOURTH_OPTION;
                break;
            case 4:
                actionOp = MenuOpcode.NPC_FIFTH_OPTION;
                break;
            default:
                return false;
        }

        MMenuEntryInterceptor.setMenuEntry(
                new MenuEntry(
                        "",
                        "",
                        npc.getIndex(),
                        actionOp.getId(),
                        0,0,
                        false
                )
        );
        MMouse.delayMouseClick(npc.getConvexHull(), 0);
        return true;
    }
}

