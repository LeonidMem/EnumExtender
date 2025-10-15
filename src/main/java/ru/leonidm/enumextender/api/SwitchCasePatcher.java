package ru.leonidm.enumextender.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

/**
 * Patcher of synthetic classes for switch-case branching
 *
 * @param <E> enum type
 * @author LeonidM
 */
public interface SwitchCasePatcher<E extends Enum<E>> {

    /**
     * Patches provided array, found in provided class. It is not necessary
     * to create new int array, you can safely modify argument and return itself.
     *
     * @param originalClass in mostly cases it must be classes where
     *                      synthetic switch-case class was created,
     *                      but, probably, they could not have been found
     *                      each time, so in this case synthetic class
     *                      is returned
     * @param ordinalArray  array to patch
     * @return new ordinal array
     */
    int @NotNull [] patch(@NotNull Class<?> originalClass, int @NotNull [] ordinalArray);

    /**
     * Wraps provided function as patcher
     *
     * @param mapper mapper function that returns mappings for original class
     * @param <E>    enum type
     * @return instance of {@link SwitchCasePatcher}
     */
    @NotNull
    static <E extends Enum<E>> SwitchCasePatcher<E> mappings(@NotNull Function<@NotNull Class<?>, @Nullable Map<E, E>> mapper) {
        return new SwitchCasePatcher<>() {
            @Override
            public int @NotNull [] patch(@NotNull Class<?> originalClass, int @NotNull [] ordinalArray) {
                Map<E, E> mappings = mapper.apply(originalClass);
                if (mappings != null) {
                    for (var entry : mappings.entrySet()) {
                        E key = entry.getKey();
                        E value = entry.getValue();

                        ordinalArray[key.ordinal()] = value != null ? value.ordinal() + 1 : 0;
                    }
                }
                return ordinalArray;
            }
        };
    }

    /**
     * Creates patcher that shift all values in array by 1 from provided index
     *
     * @param index          index from which shift must be applied
     * @param expectedLength expected length at which patcher must insert value
     * @param <E>            enum type
     * @return instance of {@link SwitchCasePatcher}
     */
    @NotNull
    static <E extends Enum<E>> SwitchCasePatcher<E> insert(int index, int expectedLength) {
        return new SwitchCasePatcher<>() {
            @Override
            public int @NotNull [] patch(@NotNull Class<?> originalClass, int @NotNull [] ordinalArray) {
                if (ordinalArray.length > expectedLength) {
                    return ordinalArray;
                } else if (ordinalArray.length < expectedLength) {
                    throw new IllegalStateException("Got array with length of " + ordinalArray.length
                                                    + ", but expected " + expectedLength);
                }

                int[] array = new int[ordinalArray.length + 1];
                System.arraycopy(ordinalArray, 0, array, 0, index);
                System.arraycopy(ordinalArray, index, array, index + 1, array.length - index - 1);
                return array;
            }
        };
    }

}
