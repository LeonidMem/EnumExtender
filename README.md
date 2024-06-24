# EnumExtender

EnumExtender is a lightweight library that allows you to gracefully extend any enumeration with minimal compatibility
issues at the runtime. This library is useful when you are using bad-designed external code and, for example,
want to extend its functionality depending on some configurations.

# Importing

* Maven:
```xml
<repositories>
  <repository>
    <id>smashup-repository</id>
    <name>SmashUp Repository</name>
    <url>https://mvn.smashup.ru/releases</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>ru.leonidm</groupId>
    <artifactId>enum-extender</artifactId>
    <version>0.1.2</version>
  </dependency>
</dependencies>
```

* Gradle:
```groovy
repositories {
  maven { url 'https://mvn.smashup.ru/releases' }
}

dependencies {
  implementation 'ru.leonidm:enum-extender:0.1.2'
}
```

# Usage

## Simple extension of enumerations
```java
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SimpleEnum {
    A(3, true, "AA"),
    B(2, false, "BB"),
    C(1, true, "CC");

    private final int integer;
    private final boolean bool;
    private final String string;
}
```

```java
// Provided map contains names of the fields declared in the enumeration class
// and corresponding values. If some field is not specified in the map,
// it will contain default value (0 for primitives, null for objects)
FieldEnum d = EnumExtender.extend(FieldEnum.class, "D", Map.of(
        "integer", -1,
        "bool", true,
        "string", "DD"
));

assert d == FieldEnum.valueOf("D"); // true
assert d == FieldEnum.values()[d.ordinal()]; // true
assert d.integer == -1; // true
assert d.bool == true; // true
assert d.string.equals("DD"); // true
```

## Fix of broken switch/case branches

Switch/case branches can be broken if they were used before the extension. In such cases you must do the following:

```java
public enum SwitchCaseEnum {
    A,
    B,
    C
}
```

```java
SwitchCaseEnum d = EnumExtender.extend(SwitchCaseEnum.class, "D", Map.of());

// Mapper is used to select branches for new enumerations
EnumSwitchCaseExtender.Mapper mapper = (originalClass) -> {
    if (originalClass == SwitchCaseTest.class) {
        return Map.of(d, SwitchCaseEnum.A);
    } else {
        return Map.of();
    }
};

// Provided class loader is class loader whose switch/case synthetic classes must be extended
// Mapper argument can be null
// If last argument is true, also parents of the class loader will be extended
EnumSwitchCaseExtender.extend(SwitchCaseEnum.class, getClass().getClassLoader(), mapper, true);
```

### [!] Restriction

Right now all enumerations that were created by extension in switch/case without and that are not mapped will just
fall back to the default branch, but, unfortunately, all switch/case are working fine excluding this one:

```java
private int returnEnhancedSwitchWithoutDefault(SwitchCaseEnum e) {
    return switch (e) {
        case A -> 0;
        case B -> 1;
        case C -> 2;
    };
}
```

Somehow, JDK allows such switch/case to exist, which, to me, is a little bit strange, because all types, except
especially this one, are backwards compatible. In the bytecode `IncompatibleClassChangeError` is hardcoded on
the default branch for this, so there is only one solution — select branch from all other existing ones.

# Known issues:
* `IncompatibleClassChangeError` in enhanced switch/cases as described above
* Abstract enumeration cannot be initialized
* `EnumSets` and `EnumMaps` that were created before extension will be broken

# TODO:
* Add API to extend abstract enumerations
* Somehow create an approach to fix all `EnumSets` and `EnumMaps`
