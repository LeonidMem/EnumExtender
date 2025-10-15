package ru.leonidm.enumextender.api;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * @param <E> enum type
 * @author LeonidM
 */
public sealed interface EnumResult<E extends Enum<E>> {

    /**
     * Returns instance of created enum or throws exception if it was not created
     *
     * @return instance of created enum
     * @throws EnumExtendException if it was not created
     */
    @NotNull
    E getEnum() throws EnumExtendException;

    /**
     * Returns exception if it was thrown or null
     *
     * @return exception or null
     */
    @Nullable
    EnumExtendException getException();

    /**
     * Returns created enum wrapped to optional or empty optional if it was not created
     *
     * @return enum optional
     */
    @NotNull
    Optional<E> asOptional();

    record Success<E extends Enum<E>>(@NotNull E getEnum) implements EnumResult<E> {

        @Override
        @NotNull
        public E getEnum() {
            return getEnum;
        }

        @Override
        @NotNull
        public Optional<E> asOptional() {
            return Optional.of(getEnum);
        }

        @Override
        @Nullable
        public EnumExtendException getException() {
            return null;
        }
    }

    record Error<E extends Enum<E>>(@NotNull EnumExtendException getException) implements EnumResult<E> {

        @Override
        @NotNull
        @Contract("-> fail")
        public E getEnum() throws EnumExtendException {
            throw getException;
        }

        @Override
        @NotNull
        public Optional<E> asOptional() {
            return Optional.empty();
        }

        @Override
        @NotNull
        public EnumExtendException getException() {
            return getException;
        }
    }

}
