package ru.leonidm.enumextender.test.fields;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author LeonidM
 */
@Getter
@AllArgsConstructor
public enum FieldEnum {
    A(3, true, "AA"),
    B(2, false, "BB"),
    C(1, true, "CC");

    private final int integer;
    private final boolean bool;
    private final String string;
}
