package ru.leonidm.enumextender.test.jit;

import org.jetbrains.annotations.NotNull;

/**
 * @author LeonidM
 */
final class LateSwitch {

    private LateSwitch() {

    }

    public static int branch(@NotNull JitEnum e) {
        switch (e) {
            case A:
                return 0;
            case B:
                return 1;
            case C:
                return 2;
            default:
                return -1;
        }
    }
}
