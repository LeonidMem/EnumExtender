package ru.leonidm.enumextender.test.abstractenum;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import ru.leonidm.enumextender.api.EnumExtender;

import java.util.Map;

/**
 * @author LeonidM
 */
public class AbstractEnumTest {

    private final EnumExtender<AbstractEnum> enumExtender = EnumExtender.of(AbstractEnum.class);

    @Test
    @Disabled
    public void abstractEnum() {
        AbstractEnum d = enumExtender.addEnum("D", Map.of()).getEnum();
    }
}
