package ru.leonidm.enumextender.test.insert;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import ru.leonidm.enumextender.api.EnumExtender;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author LeonidM
 */
public class InsertEnumTest {

    private final EnumExtender<InsertEnum> enumExtender = EnumExtender.of(InsertEnum.class);

    @Test
    public void insertEnum() {
        InsertEnum[] values = InsertEnum.values();
        int ordinalC = InsertEnum.C.ordinal();
        int ordinalE = InsertEnum.E.ordinal();
        assertSame(InsertEnum.A, values[ordinalC - 1]);

        InsertEnum b = enumExtender.insertEnum("B", Map.of("original", false), InsertEnum.C).getEnum();
        enumExtender.switchCase().patch(getClass().getClassLoader(), true);

        InsertEnum[] newValues = InsertEnum.values();
        int newOrdinalC = InsertEnum.C.ordinal();
        int newOrdinalE = InsertEnum.E.ordinal();

        assertEquals(ordinalC + 1, newOrdinalC);
        assertEquals(ordinalE + 1, newOrdinalE);

        assertSame(b, newValues[newOrdinalC - 1]);

        for (InsertEnum e : newValues) {
            String expected = (e.original ? "" : "-") + e.name();
            assertEquals(expected, switchCase(e));
        }
    }

    @Test
    public void insertEnumWithBuilder() {
        InsertEnum[] values = InsertEnum.values();
        int ordinalE = InsertEnum.E.ordinal();
        assertSame(InsertEnum.C, values[ordinalE - 1]);

        InsertEnum d = enumExtender.insertEnum("D", Map.of("original", false), InsertEnum.E).getEnum();
        enumExtender.switchCase().patch(getClass().getClassLoader(), true);

        InsertEnum[] newValues = InsertEnum.values();
        int newOrdinalE = InsertEnum.E.ordinal();

        assertEquals(ordinalE + 1, newOrdinalE);

        assertSame(d, newValues[newOrdinalE - 1]);

        for (InsertEnum e : newValues) {
            String expected = (e.original ? "" : "-") + e.name();
            assertEquals(expected, switchCase(e));
        }
    }

    @NotNull
    private String switchCase(@NotNull InsertEnum e) {
        switch (e) {
            case A:
                return "A";
            case C:
                return "C";
            case E:
                return "E";
            default:
                return "-" + e.name();
        }
    }
}
