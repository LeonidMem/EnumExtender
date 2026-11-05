package ru.leonidm.enumextender.test.jit;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import ru.leonidm.enumextender.api.EnumExtender;
import ru.leonidm.enumextender.api.SwitchCasePatcher;
import ru.leonidm.enumextender.instrument.EnumInstrumentation;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author LeonidM
 */
public class JitSwitchCaseTest {

    @Test
    public void compiledAndLateSwitchCases() {
        int sum = 0;
        for (int i = 0; i < 200000; i++) {
            for (JitEnum e : JitEnum.values()) {
                sum += branch(e);
            }
        }

        assertEquals(600000, sum);

        EnumInstrumentation.installAgent();

        EnumExtender<JitEnum> enumExtender = EnumExtender.of(JitEnum.class);
        JitEnum x = enumExtender.enumBuilder("X").insertBefore(JitEnum.C).create().getEnum();
        JitEnum y = enumExtender.enumBuilder("Y").create().getEnum();

        enumExtender.switchCase()
                .addPatcher(SwitchCasePatcher.mappings(originalClass -> {
                    if (originalClass == JitSwitchCaseTest.class) {
                        return Map.of(y, JitEnum.A);
                    } else if (originalClass.getSimpleName().equals("LateSwitch")) {
                        return Map.of(y, JitEnum.B);
                    } else {
                        return Map.of();
                    }
                }))
                .patch(getClass().getClassLoader(), true);

        assertEquals(0, branch(JitEnum.A));
        assertEquals(1, branch(JitEnum.B));
        assertEquals(2, branch(JitEnum.C));
        assertEquals(-1, branch(x));
        assertEquals(0, branch(y));

        assertEquals(0, LateSwitch.branch(JitEnum.A));
        assertEquals(2, LateSwitch.branch(JitEnum.C));
        assertEquals(-1, LateSwitch.branch(x));
        assertEquals(1, LateSwitch.branch(y));
    }

    private static int branch(@NotNull JitEnum e) {
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
