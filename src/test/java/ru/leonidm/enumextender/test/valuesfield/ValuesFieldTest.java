package ru.leonidm.enumextender.test.valuesfield;

import org.junit.jupiter.api.Test;
import ru.leonidm.enumextender.api.EnumExtender;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * @author LeonidM
 */
public class ValuesFieldTest {

    private final EnumExtender<ValuesFieldEnum> enumExtender = EnumExtender.of(ValuesFieldEnum.class);

    @Test
    public void valuesField() {
        ValuesFieldEnum d = enumExtender.addEnum("D", Map.of()).getEnum();

        ValuesFieldEnum[] expected = {ValuesFieldEnum.A, ValuesFieldEnum.B, ValuesFieldEnum.C};
        assertArrayEquals(expected, ValuesFieldEnum.$VALUES);
        assertArrayEquals(expected, ValuesFieldEnum.$VALUES$);

        expected = new ValuesFieldEnum[]{ValuesFieldEnum.A, ValuesFieldEnum.B, ValuesFieldEnum.C, d};
        assertArrayEquals(expected, ValuesFieldEnum.values());
    }
}
