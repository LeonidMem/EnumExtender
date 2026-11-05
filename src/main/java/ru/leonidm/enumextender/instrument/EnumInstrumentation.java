package ru.leonidm.enumextender.instrument;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.leonidm.enumextender.api.EnumExtendException;
import ru.leonidm.enumextender.api.EnumSwitchCaseExtender;

import java.lang.instrument.Instrumentation;

/**
 * Connects EnumExtender to the {@link Instrumentation}. After installation:
 * <ul>
 *     <li>{@link EnumSwitchCaseExtender#patch(ClassLoader, boolean)} retransforms classes that use patched
 *     switch/case arrays to prevent JIT optimizations;</li>
 *     <li>switch/case synthetic classes loaded later are patched at the end of their static initializers.</li>
 * </ul>
 *
 * @author LeonidM
 */
public final class EnumInstrumentation {

    private static volatile Instrumentation instrumentation;

    private EnumInstrumentation() {

    }

    /**
     * Returns installed {@link Instrumentation}
     *
     * @return installed instrumentation or null if it was not installed
     */
    @Nullable
    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }

    /**
     * Installs provided {@link Instrumentation}, for example, obtained in the agent of the application
     *
     * @param instrumentation instrumentation that supports retransformation of classes
     * @throws EnumExtendException if instrumentation does not support retransformation or if JVM is somehow broken
     */
    public static synchronized void install(@NotNull Instrumentation instrumentation) {
        if (EnumInstrumentation.instrumentation != null) {
            return;
        }

        if (!instrumentation.isRetransformClassesSupported()) {
            throw new EnumExtendException("Instrumentation does not support retransformation of classes");
        }

        try {
            SwitchMapHookClass.install(instrumentation);
        } catch (Exception e) {
            throw new EnumExtendException(e);
        }

        instrumentation.addTransformer(new SwitchMapTransformer(), false);
        EnumInstrumentation.instrumentation = instrumentation;
    }

    /**
     * Loads {@link EnumExtenderAgent} into the current JVM and installs its {@link Instrumentation}. If JVM forbids
     * attaching to itself, agent is loaded by the helper process, so JDK (not JRE) is required.
     *
     * @return installed instrumentation
     * @throws EnumExtendException if agent could not be loaded
     */
    @NotNull
    public static synchronized Instrumentation installAgent() {
        if (instrumentation != null) {
            return instrumentation;
        }

        Instrumentation agentInstrumentation = AgentLoader.load();
        install(agentInstrumentation);
        return agentInstrumentation;
    }
}
