package ru.leonidm.enumextender.test.fields;

import org.junit.jupiter.api.Test;
import ru.leonidm.enumextender.api.EnumExtender;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author LeonidM
 */
public class FieldEnumTest {

    private final EnumExtender<FieldEnum> enumExtender = EnumExtender.of(FieldEnum.class);

    @Test
    public void fieldEnum() {
        FieldEnum d = enumExtender.addEnum("D", Map.of(
                        "integer", -1,
                        "bool", true,
                        "string", "DD"
                ))
                .getEnum();

        assertEquals(-1, d.getInteger());
        assertEquals(true, d.isBool());
        assertEquals("DD", d.getString());
    }

    @Test
    public void fieldEnumWithBuilder() {
        FieldEnum e = enumExtender.enumBuilder("E")
                .setField("integer", -1)
                .setField("bool", true)
                .setField("string", "DD")
                .create()
                .getEnum();

        assertEquals(-1, e.getInteger());
        assertEquals(true, e.isBool());
        assertEquals("DD", e.getString());
    }
}
