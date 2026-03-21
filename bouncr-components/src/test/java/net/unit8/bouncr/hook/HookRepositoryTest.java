package net.unit8.bouncr.hook;

import net.unit8.bouncr.component.config.HookPoint;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HookRepositoryTest {
    private static class RecordingHook implements Hook<String> {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void run(String message) {
            messages.add(message);
        }
    }

    @Test
    void multipleHooksAtOnePoint() {
        final RecordingHook hook1 = new RecordingHook();
        final RecordingHook hook2 = new RecordingHook();
        final HookRepository repo = new HookRepository();
        repo.register(HookPoint.AFTER_SIGN_IN, hook1);
        repo.register(HookPoint.AFTER_SIGN_IN, hook2);

        repo.runHook(HookPoint.AFTER_SIGN_IN, "message");
        assertThat(hook1.messages).containsExactly("message");
        assertThat(hook2.messages).containsExactly("message");
    }
}
