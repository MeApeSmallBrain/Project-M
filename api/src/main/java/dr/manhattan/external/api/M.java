package dr.manhattan.external.api;

import dr.manhattan.external.api.interact.MMenuEntryInterceptor;
import dr.manhattan.external.api.items.MInventory;
import dr.manhattan.external.api.npcs.MNpcCache;
import dr.manhattan.external.api.objects.MObjectCache;
import dr.manhattan.external.api.pickups.MPickupCache;
import dr.manhattan.external.api.scriptmanager.MScriptManager;
import dr.manhattan.external.api.scriptmanager.MScriptThread;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.PluginType;
import org.pf4j.Extension;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Slf4j
@Extension
@PluginDescriptor(
        name = "Project:M",
        description = "Automation Programming Interface",
        tags = {"OpenOSRS", "ProjectM", "Bot"},
        type = PluginType.SYSTEM
)
public class M extends Plugin {
    private static M m;
    @Inject
    private MScriptThread mScriptThread;
    @Inject
    private MScriptManager mScriptManager;
    @Inject
    private MObjectCache mObjectCache;
    @Inject
    private MPickupCache mPickupCache;
    @Inject
    private MInventory mInventory;
    @Inject
    private MMenuEntryInterceptor mMenuEntryInterceptor;
    @Inject
    private MNpcCache mNpcCache;
    @Inject
    private EventBus eventBus;
    @Inject
    private Client client;
    @Inject
    private PluginManager pluginManager;

    public static PluginManager getPluginManager() {
        return M.getInstance().pluginManager;
    }

    public static Client client() {
        return M.getInstance().client;
    }

    public static M getInstance() {
        if (m != null) return m;
        m = new M();
        return m;
    }

    @Override
    protected void startUp() {
        m = this;
        new Thread(mScriptThread, "ProjectM script thread").start();

        mObjectCache.start(eventBus);
        mNpcCache.start(eventBus);
        mInventory.start(eventBus);
        mPickupCache.start(eventBus);
        mMenuEntryInterceptor.start(eventBus);
        mScriptManager.start(eventBus, pluginManager);

        MScriptManager.manageScripts();
        MObjectCache.refreshObjects();
        MNpcCache.refreshNpcs();
    }

    @Override
    protected void shutDown() {
        mObjectCache.stop(eventBus);
        mNpcCache.stop(eventBus);
        mInventory.stop(eventBus);
        mPickupCache.stop(eventBus);
        mMenuEntryInterceptor.stop(eventBus);
        mScriptManager.stop(eventBus);
        mScriptThread.kill();
    }
}