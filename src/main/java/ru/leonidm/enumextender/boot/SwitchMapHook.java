package ru.leonidm.enumextender.boot;

import java.util.function.Consumer;

/**
 * Hook that is called at the end of static initializers of switch/case synthetic classes.
 * It should not reference anything except {@code java.base} and must never be referenced
 * by the library directly, only by name.
 *
 * @author LeonidM
 */
public final class SwitchMapHook {

    public static volatile Consumer<Class<?>> hook;

    private SwitchMapHook() {

    }

    public static void onInit(Class<?> syntheticClass) {
        Consumer<Class<?>> consumer = hook;
        if (consumer != null) {
            consumer.accept(syntheticClass);
        }
    }
}
