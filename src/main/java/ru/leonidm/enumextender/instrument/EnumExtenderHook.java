package ru.leonidm.enumextender.instrument;

import org.jetbrains.annotations.NotNull;
import ru.leonidm.enumextender.api.EnumExtender;
import ru.leonidm.enumextender.api.EnumSwitchCaseExtender;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author LeonidM
 */
public final class EnumExtenderHook {

    private EnumExtenderHook() {

    }

    /**
     * Patches switch/case synthetic class right at the end of its static initializer.
     * Called by the class transformer installed with {@link EnumInstrumentation}.
     *
     * @param syntheticClass switch/case synthetic class whose static initializer is running
     */
    public static void onSwitchMapInit(@NotNull Class<?> syntheticClass) {
        for (EnumExtender<?> enumExtender : EnumExtender.getExtenders()) {
            try {
                enumExtender.switchCase().patchInitializedClass(syntheticClass);
            } catch (Throwable t) {
                Logger.getLogger(EnumSwitchCaseExtender.class.getName())
                        .log(Level.SEVERE, t, () -> "Could not patch switch-case class '%s'".formatted(syntheticClass));
            }
        }
    }
}
