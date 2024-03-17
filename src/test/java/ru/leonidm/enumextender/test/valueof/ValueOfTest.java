package ru.leonidm.enumextender.test.valueof;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import org.junit.jupiter.api.Test;
import ru.leonidm.enumextender.EnumExtender;

import java.util.Map;

/**
 * @author LeonidM
 */
public class ValueOfTest {

    @Test
    public void valueOf() throws Exception {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            ValueOfEnum.valueOf("D");
        }, "No enum constant " + ValueOfEnum.class.getName() + ".D");

        ValueOfEnum d = EnumExtender.extend(ValueOfEnum.class, "D", Map.of());
        ValueOfEnum dValueOf = ValueOfEnum.valueOf("D");

        assertSame(d, dValueOf);
    }
}
