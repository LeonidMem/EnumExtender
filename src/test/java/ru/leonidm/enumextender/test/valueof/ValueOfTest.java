package ru.leonidm.enumextender.test.valueof;

import org.junit.jupiter.api.Test;
import ru.leonidm.enumextender.api.EnumExtender;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

/**
 * @author LeonidM
 */
public class ValueOfTest {

    private final EnumExtender<ValueOfEnum> enumExtender = EnumExtender.of(ValueOfEnum.class);

    @Test
    public void valueOf() throws Exception {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            ValueOfEnum.valueOf("D");
        }, "No enum constant " + ValueOfEnum.class.getName() + ".D");

        ValueOfEnum d = enumExtender.addEnum("D", Map.of()).getEnum();
        ValueOfEnum dValueOf = ValueOfEnum.valueOf("D");

        assertSame(d, dValueOf);
    }
}
