package ru.leonidm.enumextender.test.simple;

import org.junit.jupiter.api.Test;
import ru.leonidm.enumextender.api.EnumExtendException;
import ru.leonidm.enumextender.api.EnumExtender;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

/**
 * @author LeonidM
 */
public class SimpleEnumTest {

    private final EnumExtender<SimpleEnum> enumExtender = EnumExtender.of(SimpleEnum.class);

    @Test
    public void simpleEnum() {
        SimpleEnum[] values = SimpleEnum.values();

        assertThrowsExactly(EnumExtendException.class, () -> {
            enumExtender.addEnum("C", Map.of()).getEnum();
        }, "Enum with name '%s' is already defined in %s".formatted("C", SimpleEnum.class));

        SimpleEnum d = enumExtender.addEnum("D", Map.of())
                .getEnum();

        assertEquals(values.length, d.ordinal());
        assertEquals("D", d.name());

        assertEquals(values.length + 1, SimpleEnum.values().length);
    }

    @Test
    public void simpleEnumWithBuilder() {
        SimpleEnum[] values = SimpleEnum.values();

        assertThrowsExactly(EnumExtendException.class, () -> {
            enumExtender.enumBuilder("C").create().getEnum();
        }, "Enum with name '%s' is already defined in %s".formatted("C", SimpleEnum.class));

        SimpleEnum e = enumExtender.enumBuilder("E")
                .create()
                .getEnum();

        assertEquals(values.length, e.ordinal());
        assertEquals("E", e.name());

        assertEquals(values.length + 1, SimpleEnum.values().length);
    }
}
