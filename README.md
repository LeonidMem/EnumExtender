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
    <version>1.0.0</version>
  </dependency>
</dependencies>
```

* Gradle:
```groovy
repositories {
  maven { url 'https://mvn.smashup.ru/releases' }
}

dependencies {
  implementation 'ru.leonidm:enum-extender:1.0.0'
}
```

# Usage

## Add `-add-opens` flag
```sh
java --add-opens=java.base/java.lang=ALL-UNNAMED -jar my.jar```
```

## Simple extension of enumerations
```java
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SimpleEnum {
    A(4, true, "AA"),
    B(3, false, "BB"),
    C(2, true, "CC"),
    E(0, true, "EE");

    public final int integer;
    public final boolean bool;
    public final String string;
}
```

```java
EnumExtender<SimpleEnum> enumExtender = EnumExtender.of(SimpleEnum.class);
SimpleEnum f = enumExtender.enumBuilder("F")
        // If some field is not specified in the map, it will contain
        // default value (0 for primitives, null for objects)
        .setField("integer", -1)
        .setField("bool", true)
        .setField("string", "FF")
        .create()
        .getEnum();

assert f.integer == -1; // true
assert f.bool == true; // true
assert f.string.equals("FF"); // true
assert f.ordinal() == 4; // true
assert f == SimpleEnum.valueOf("F"); // true
assert f == SimpleEnum.values()[f.ordinal()]; // true

// Also, it is possible to insert enumeration at the middle of values
// After this operation, you MUST use patcher to fix broken switch-case branches
SimpleEnum d = enumExtender.enumBuilder("D")
        .insertBefore(SimpleEnum.E)
        .create()
        .getEnum();

assert d.ordinal() == 3; // true
assert d == SimpleEnum.values()[3]; // true

// Ordinal of all next enumerations after new one are also changed
assert SimpleEnum.E.ordinal() == 4; // true
assert f.ordinal() == 5; // true
```

## Fix of broken switch-case branches

Switch-case branches can be broken if they were used before the extension. In such cases you must do the following:

```java
public enum SwitchCaseEnum {
    A,
    B,
    C
}
```

```java
EnumExtender<SwitchCaseEnum> enumExtender = EnumExtender.of(SwitchCaseEnum.class);
SwitchCaseEnum d = enumExtender.enumBuilder("D")
        .create()
        .getEnum();

// SwitchCasePatcher is interface, so you can implement it by yourself,
// but patcher below is sufficient in many cases, because it is used
// to select branches for new enumerations
SwitchCasePatcher patcher = SwitchCasePatcher.mappings(
        (originalClass) -> { // Class where switch-case was found
            if (originalClass == SwitchCaseTest.class) {
                return Map.of(d, SwitchCaseEnum.A); // For enum D, follow A branch 
            } else {
                return Map.of();
            }
        }
);

enumExtender.switchCase()
        .addPatcher(patcher)
        // Provided class loader is class loader whose switch-case synthetic classes must be extended
        // If last argument is true, also parents of the class loader will be extended
        .patch(patcher.getClass().getClassLoader(), true);
```

**[!!!]** Right now, this library can patch only loaded classes. As temp solution, you can use
[ClassGraph](https://github.com/classgraph/classgraph) to find all switch-case classes and load them before patching.
This problem will be fixed later in this library by implementing Java agent that patches bytecode of such classes.

### [!] Restriction

Right now all enumerations that were created by extension in switch-case without and that are not mapped will just
fall back to the default branch, but, unfortunately, all switch-case are working fine excluding this one:

```java
private int returnEnhancedSwitchWithoutDefault(SwitchCaseEnum e) {
    return switch (e) {
        case A -> 0;
        case B -> 1;
        case C -> 2;
    };
}
```

Somehow, JDK allows such switch-case to exist, which, to me, is a little bit strange, because all types, except
especially this one, are backwards compatible. In the bytecode `IncompatibleClassChangeError` is hardcoded on
the default branch for this, so there is only one solution — select branch from all other existing ones.

# Known issues:
* `IncompatibleClassChangeError` in enhanced switch-cases as described above
* Abstract enumeration cannot be initialized
* `EnumSets` and `EnumMaps` that were created before extension will be broken

# TODO:
* Add API to extend abstract enumerations
* Somehow create an approach to fix all `EnumSets` and `EnumMaps`
