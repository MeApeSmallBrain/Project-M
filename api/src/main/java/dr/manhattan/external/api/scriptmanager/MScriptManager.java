package dr.manhattan.external.api.scriptmanager;

import dr.manhattan.external.api.M;
import dr.manhattan.external.api.MScript;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import org.apache.commons.lang3.ThreadUtils;

@Slf4j
public class MScriptManager {
    private static final String SCRIPT_MANAGER_THREAD_NAME = "ProjectM Script Manager";
    private static final Object BUS_SUB = new Object();
    private static MScript activeScript = null;
    private static PluginManager pluginManager = null;

    private static final Runnable runScriptManager = () -> {

        if (pluginManager == null) {
            pluginManager = M.getInstance().getPluginManager();
            return;
        }

        boolean switchedScript = false;
        for (Plugin p : pluginManager.getPlugins()) {
            if (p instanceof MScript) {
                if (pluginManager.isPluginEnabled(p)) {
                    if (p != getActiveScript()) {
                        if (!switchedScript) {
                            switchedScript = true;
                            if (getActiveScript() != null) pluginManager.setPluginEnabled(getActiveScript(), false);
                            setActiveScript((MScript) p);
                            log.info("Active script set to " + p.getName());
                            continue;
                        }
                        pluginManager.setPluginEnabled(p, false);
                        return;
                    }
                }
            }
        }

        if (getActiveScript() != null && !pluginManager.isPluginEnabled(getActiveScript())) {
            setActiveScript(null);
        }
    };


    public static void manageScripts() {
        if (ThreadUtils.findThreadsByName(SCRIPT_MANAGER_THREAD_NAME).size() > 0) {
            return;
        }
        new Thread(runScriptManager, SCRIPT_MANAGER_THREAD_NAME).start();
    }
    public void start(EventBus eventBus) {
        eventBus.subscribe(PluginChanged.class, BUS_SUB, this::pluginChanged);
    }

    public void stop(EventBus eventBus) {
        eventBus.unregister(BUS_SUB);
    }

    private void pluginChanged(PluginChanged event) {
        manageScripts();
    }
    public static synchronized MScript getActiveScript() {
        return activeScript;
    }

    public static synchronized void setActiveScript(MScript setScript) {
        activeScript = setScript;
    }


}
