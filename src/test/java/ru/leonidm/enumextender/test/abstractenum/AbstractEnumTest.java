package ru.leonidm.enumextender.test.abstractenum;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import ru.leonidm.enumextender.EnumExtender;

import java.util.Map;

/**
 * @author LeonidM
 */
public class AbstractEnumTest {

    @Test
    @Disabled
    public void abstractEnum() {
        AbstractEnum d = EnumExtender.extend(AbstractEnum.class, "D", Map.of());
    }
}
