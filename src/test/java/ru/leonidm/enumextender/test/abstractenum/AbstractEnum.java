package ru.leonidm.enumextender.test.abstractenum;

/**
 * @author LeonidM
 */
public enum AbstractEnum {
    A {
        @Override
        public String method() {
            return "A";
        }
    },
    B {
        @Override
        public String method() {
            return "B";
        }
    },
    C {
        @Override
        public String method() {
            return "C";
        }
    };

    public abstract String method();
}
