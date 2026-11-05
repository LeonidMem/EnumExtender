package ru.leonidm.enumextender.instrument;

import org.jetbrains.annotations.NotNull;

import java.lang.instrument.Instrumentation;

/**
 * Java agent that only stores provided {@link Instrumentation}. It is loaded by the system class loader,
 * so it must not reference any other classes of the library.
 *
 * @author LeonidM
 */
public final class EnumExtenderAgent {

    public static volatile Instrumentation instrumentation;

    private EnumExtenderAgent() {

    }

    public static void premain(@NotNull String arguments, @NotNull Instrumentation instrumentation) {
        EnumExtenderAgent.instrumentation = instrumentation;
    }

    public static void agentmain(@NotNull String arguments, @NotNull Instrumentation instrumentation) {
        EnumExtenderAgent.instrumentation = instrumentation;
    }
}
