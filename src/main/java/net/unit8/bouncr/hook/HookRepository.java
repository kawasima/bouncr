package net.unit8.bouncr.hook;

import enkan.collection.Multimap;
import net.unit8.bouncr.component.config.HookPoint;

public class HookRepository {
    private Multimap<HookPoint, Hook> hooks = Multimap.empty();

    public void register(HookPoint point, Hook hook) {
        synchronized (hooks) {
            hooks.put(point, hook);
        }
    }

    public void runHook(HookPoint point, Object message) {
        hooks.getAll(point).forEach((hook) -> hook.run(message));
    }
}
