package dr.manhattan.external.api.player;

import dr.manhattan.external.api.M;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;

public class MPlayer {

    public static boolean isMoving() {
        Player player = M.client().getLocalPlayer();
        if (player == null) {
            return false;
        }
        return player.getIdlePoseAnimation() != player.getPoseAnimation();
    }
    public static int getLevel(Skill skill){
       return M.client().getRealSkillLevel(skill);
    }

    public static boolean isAnimating() {
        return get().getAnimation() != -1;
    }

    public static Player get() {
        return M.client().getLocalPlayer();
    }

    public static WorldPoint location() {
        return get().getWorldLocation();
    }

    public static boolean isIdle() {
        return !isAnimating() && !isMoving();
    }
}
