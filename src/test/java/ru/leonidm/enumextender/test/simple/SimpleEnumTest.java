package ru.leonidm.enumextender.test.simple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import org.junit.jupiter.api.Test;
import ru.leonidm.enumextender.EnumExtender;

import java.util.Map;

/**
 * @author LeonidM
 */
public class SimpleEnumTest {

    @Test
    public void simpleEnum() {
        SimpleEnum[] values = SimpleEnum.values();

        assertThrowsExactly(IllegalStateException.class, () -> {
            EnumExtender.extend(SimpleEnum.class, "C", Map.of());
        }, "Enum with name '%s' is already defined in %s".formatted("C", SimpleEnum.class));

        SimpleEnum d = EnumExtender.extend(SimpleEnum.class, "D", Map.of());

        assertEquals(values.length, d.ordinal());
        assertEquals("D", d.name());

        assertEquals(values.length + 1, SimpleEnum.values().length);
    }
}
