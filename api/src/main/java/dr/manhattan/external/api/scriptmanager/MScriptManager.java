package dr.manhattan.external.api.scriptmanager;

import dr.manhattan.external.api.M;
import dr.manhattan.external.api.MScript;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

import javax.inject.Singleton;

@Slf4j
@Singleton
public class MScriptManager {
    private static final String SCRIPT_MANAGER_THREAD_NAME = "ProjectM Script Manager";
    private static final Object BUS_SUB = new Object();
    static PluginManager pluginManager;
    private static MScript activeScript = null;

    public static void manageScripts() {
        new MScriptManager().runScriptManager();
    }

    ;

    public static synchronized MScript getActiveScript() {
        return activeScript;
    }

    public static synchronized void setActiveScript(MScript setScript) {
        activeScript = setScript;
    }

    private void runScriptManager() {
        if (pluginManager == null) {
            log.info("Plugin manager is null");
            pluginManager = M.pluginManager();
            return;
        }

        boolean switchedScript = false;
        log.info("Plugin manager: " + pluginManager);
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
    }

    public void start(EventBus eventBus, PluginManager pluginManager) {
        MScriptManager.pluginManager = pluginManager;
        eventBus.subscribe(PluginChanged.class, BUS_SUB, this::pluginChanged);
    }

    public void stop(EventBus eventBus) {
        eventBus.unregister(BUS_SUB);
    }

    private void pluginChanged(PluginChanged event) {
        manageScripts();
    }


}
