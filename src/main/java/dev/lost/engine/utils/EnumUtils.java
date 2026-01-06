package dev.lost.engine.utils;

import org.jspecify.annotations.NonNull;

import java.util.Locale;
import java.util.Optional;

public class EnumUtils {
    public static <T extends Enum<T>> Optional<T> match(@NonNull String key, Class<T> enumClass) {
        try {
            return Optional.of(Enum.valueOf(enumClass, key.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
