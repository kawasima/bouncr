package net.unit8.bouncr.hook;

import net.unit8.bouncr.component.config.HookPoint;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class HookRepositoryTest {
    @Test
    void multipleHooksAtOnePoint() {
        final Hook hook1 = mock(Hook.class);
        final Hook hook2 = mock(Hook.class);
        final HookRepository repo = new HookRepository();
        repo.register(HookPoint.AFTER_SIGN_IN, hook1);
        repo.register(HookPoint.AFTER_SIGN_IN, hook2);

        repo.runHook(HookPoint.AFTER_SIGN_IN, "message");
        verify(hook1).run(eq("message"));
        verify(hook2).run(eq("message"));
    }
}