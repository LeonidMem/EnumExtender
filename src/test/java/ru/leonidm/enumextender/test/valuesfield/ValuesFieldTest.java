package ru.leonidm.enumextender.test.valuesfield;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;
import ru.leonidm.enumextender.EnumExtender;

import java.util.Map;

/**
 * @author LeonidM
 */
public class ValuesFieldTest {

    @Test
    public void valuesField() {
        ValuesFieldEnum d = EnumExtender.extend(ValuesFieldEnum.class, "D", Map.of());

        ValuesFieldEnum[] expected = {ValuesFieldEnum.A, ValuesFieldEnum.B, ValuesFieldEnum.C};
        assertArrayEquals(expected, ValuesFieldEnum.$VALUES);
        assertArrayEquals(expected, ValuesFieldEnum.$VALUES$);

        expected = new ValuesFieldEnum[]{ValuesFieldEnum.A, ValuesFieldEnum.B, ValuesFieldEnum.C, d};
        assertArrayEquals(expected, ValuesFieldEnum.values());
    }
}
