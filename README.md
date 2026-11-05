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
    <version>1.1.0</version>
  </dependency>
</dependencies>
```

* Gradle:
```groovy
repositories {
  maven { url 'https://mvn.smashup.ru/releases' }
}

dependencies {
  implementation 'ru.leonidm:enum-extender:1.1.0'
}
```

# Usage

## JVM flags

Without instrumentation (see [below](#fix-of-broken-switch-case-branches)) the library reads loaded classes from
the private field of `ClassLoader`, so `java.lang` must be opened:

```sh
java --add-opens=java.base/java.lang=ALL-UNNAMED -jar my.jar
```

If `EnumInstrumentation` is installed before the first patch, this flag is not required.
However, it is still recommended to keep it for the cases when the agent could not be loaded.

To load the agent with `EnumInstrumentation.installAgent()`, the JDK is required. It is also recommended to enable flag:
```sh
java -Djdk.attach.allowAttachSelf=true -jar my.jar
```

Both flags are not needed if the library jar is passed as an agent: `-javaagent:enum-extender.jar`
(`EnumInstrumentation.installAgent()` must still be called, then it uses instrumentation of this agent).

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

**[!!!]** Without instrumentation this library can patch only loaded classes, and JIT-compiled code
that was compiled before the patch sometimes keeps old switch-case arrays as constants, so it can
throw `ArrayIndexOutOfBoundsException` for new enumerations. Install instrumentation before the first
patch to fix both problems:

```java
// Loads agent into the current JVM (JDK is required, see "JVM flags"),
// or use EnumInstrumentation.install(Instrumentation) if you already have Instrumentation
EnumInstrumentation.installAgent();
```

After installation:
* `patch(...)` retransforms classes that use patched arrays, so JIT-compiled code is deoptimized;
* switch-case classes loaded later are patched at the end of their static initializers by all added patchers.

The library jar itself is also an agent (`-javaagent:enum-extender.jar`), then `installAgent()` uses its
instrumentation. Bytecode is transformed with [ASM](https://asm.ow2.io/), which must be available at runtime.

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
