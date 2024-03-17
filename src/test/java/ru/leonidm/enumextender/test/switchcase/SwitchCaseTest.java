package ru.leonidm.enumextender.test.switchcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import ru.leonidm.enumextender.EnumExtender;
import ru.leonidm.enumextender.EnumSwitchCaseExtender;

import java.util.Map;

/**
 * @author LeonidM
 */
public class SwitchCaseTest {

    @Test
    public void switchCase() {
        SwitchCaseEnum[] values = SwitchCaseEnum.values();

        for (SwitchCaseEnum e : values) {
            int ordinal = e.ordinal();
            assertEquals(ordinal, switchWithDefault(e));
            assertEquals(ordinal, switchWithoutDefault(e));
            assertEquals(ordinal, returnEnhancedSwitchWithDefault(e));
            assertEquals(ordinal, returnEnhancedSwitchWithoutDefault(e));
            assertEquals(ordinal, returnEnhancedSwitchWithAllAndDefault(e));
        }

        SwitchCaseEnum d = EnumExtender.extend(SwitchCaseEnum.class, "D", Map.of());

        EnumSwitchCaseExtender.extend(SwitchCaseEnum.class, getClass().getClassLoader(), true);

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
        };

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
        };

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
