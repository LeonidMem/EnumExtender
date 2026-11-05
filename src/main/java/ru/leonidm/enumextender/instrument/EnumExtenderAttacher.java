package ru.leonidm.enumextender.instrument;

import com.sun.tools.attach.VirtualMachine;
import org.jetbrains.annotations.NotNull;

/**
 * Entry point of the helper process that loads {@link EnumExtenderAgent} into another JVM.
 *
 * @author LeonidM
 */
public final class EnumExtenderAttacher {

    private EnumExtenderAttacher() {

    }

    public static void main(@NotNull String @NotNull [] args) throws Exception {
        VirtualMachine virtualMachine = VirtualMachine.attach(args[0]);
        try {
            virtualMachine.loadAgent(args[1]);
        } finally {
            virtualMachine.detach();
        }
    }
}
