package ru.leonidm.enumextender.test.fields;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import ru.leonidm.enumextender.EnumExtender;

import java.util.Map;

/**
 * @author LeonidM
 */
public class FieldEnumTest {

    @Test
    public void fieldEnum() {
        FieldEnum d = EnumExtender.extend(FieldEnum.class, "D", Map.of(
                "integer", -1,
                "bool", true,
                "string", "DD"
        ));

        assertEquals(-1, d.getInteger());
        assertEquals(true, d.isBool());
        assertEquals("DD", d.getString());
    }
}
