package ru.leonidm.enumextender.test.switchcase;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import ru.leonidm.enumextender.api.EnumExtender;
import ru.leonidm.enumextender.api.SwitchCasePatcher;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

/**
 * @author LeonidM
 */
public class SwitchCaseTest {

    private final EnumExtender<SwitchCaseEnum> enumExtender = EnumExtender.of(SwitchCaseEnum.class);

    @Test
    public void switchCase() {
        SwitchCaseEnum[] values = SwitchCaseEnum.values();

        for (SwitchCaseEnum e : values) {
            int ordinal = e.ordinal();
            assertEquals(ordinal, switchWithDefault(e));
            assertEquals(ordinal, switchWithoutDefault(e));
            assertEquals(ordinal, enhancedSwitchWithDefault(e));
            assertEquals(ordinal, enhancedSwitchWithoutDefault(e));
            assertEquals(ordinal, returnEnhancedSwitchWithDefault(e));
            assertEquals(ordinal, returnEnhancedSwitchWithoutDefault(e));
            assertEquals(ordinal, returnEnhancedSwitchWithAllAndDefault(e));
        }

        SwitchCaseEnum d = enumExtender.enumBuilder("D").create().getEnum();
        SwitchCaseEnum e = enumExtender.enumBuilder("E").create().getEnum();

        enumExtender.switchCase()
                .addPatcher(SwitchCasePatcher.mappings(
                        (originalClass) -> {
                            if (originalClass == SwitchCaseTest.class) {
                                return Map.of(e, SwitchCaseEnum.A);
                            } else {
                                return Map.of();
                            }
                        }
                ))
                .patch(getClass().getClassLoader(), true);

        assertEquals(2, switchWithDefault(d));
        assertEquals(-1, switchWithoutDefault(d));

        assertEquals(2, enhancedSwitchWithDefault(d));
        assertEquals(-1, enhancedSwitchWithoutDefault(d));
        assertEquals(3, returnEnhancedSwitchWithAllAndDefault(d));

        assertEquals(2, returnEnhancedSwitchWithDefault(d));
        assertThrowsExactly(IncompatibleClassChangeError.class, () -> {
            returnEnhancedSwitchWithoutDefault(d);
        });
        assertEquals(3, returnEnhancedSwitchWithAllAndDefault(d));


        assertEquals(0, switchWithDefault(e));
        assertEquals(0, switchWithoutDefault(e));
        assertEquals(0, enhancedSwitchWithDefault(e));
        assertEquals(0, enhancedSwitchWithoutDefault(e));
        assertEquals(0, returnEnhancedSwitchWithDefault(e));
        assertEquals(0, returnEnhancedSwitchWithoutDefault(e));
        assertEquals(0, returnEnhancedSwitchWithAllAndDefault(e));
    }

    private int switchWithDefault(@NotNull SwitchCaseEnum e) {
        int result;
        switch (e) {
            case A:
                result = 0;
                break;
            case B:
                result = 1;
                break;
            default:
                result = 2;
                break;
        }

        return result;
    }

    private int switchWithoutDefault(@NotNull SwitchCaseEnum e) {
        int result = -1;
        switch (e) {
            case A:
                result = 0;
                break;
            case B:
                result = 1;
                break;
            case C:
                result = 2;
                break;
        }

        return result;
    }

    private int enhancedSwitchWithDefault(@NotNull SwitchCaseEnum e) {
        int result;

        switch (e) {
            case A -> result = 0;
            case B -> result = 1;
            default -> result = 2;
        }

        return result;
    }

    private int enhancedSwitchWithoutDefault(@NotNull SwitchCaseEnum e) {
        int result = -1;

        switch (e) {
            case A -> result = 0;
            case B -> result = 1;
            case C -> result = 2;
        }

        return result;
    }

    private int enhancedSwitchWithAllAndDefault(@NotNull SwitchCaseEnum e) {
        int result = -1;

        switch (e) {
            case A -> result = 0;
            case B -> result = 1;
            case C -> result = 2;
            default -> result = 3;
        }

        return result;
    }

    private int returnEnhancedSwitchWithDefault(@NotNull SwitchCaseEnum e) {
        return switch (e) {
            case A -> 0;
            case B -> 1;
            default -> 2;
        };
    }

    private int returnEnhancedSwitchWithoutDefault(@NotNull SwitchCaseEnum e) {
        return switch (e) {
            case A -> 0;
            case B -> 1;
            case C -> 2;
        };
    }

    private int returnEnhancedSwitchWithAllAndDefault(@NotNull SwitchCaseEnum e) {
        return switch (e) {
            case A -> 0;
            case B -> 1;
            case C -> 2;
            default -> 3;
        };
    }
}
