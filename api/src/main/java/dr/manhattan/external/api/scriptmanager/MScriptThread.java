package dr.manhattan.external.api.scriptmanager;

import dr.manhattan.external.api.M;
import dr.manhattan.external.api.MScript;
import net.runelite.api.GameState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MScriptThread implements Runnable {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private boolean runScript = true;

    public synchronized void kill() {
        this.runScript = false;
    }

    public void run() {
        while (runScript) {
            try {
                MScript activeScript = MScriptManager.getActiveScript();
                if (activeScript == null) Thread.sleep(1000);
                else {
                    if (M.client().getGameState() == GameState.LOGGED_IN)
                        Thread.sleep(activeScript.loop());
                    else {
                        Thread.sleep(1000);
                    }
                }
            } catch (InterruptedException e) {
                log.error("Script thread", e);
            }
        }
    }
}
