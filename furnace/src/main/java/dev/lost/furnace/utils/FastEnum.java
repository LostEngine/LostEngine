package dev.lost.furnace.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class FastEnum {

    private static final Map<Class<? extends Enum<?>>, Map<String, ? extends Enum<?>>> CACHE = new ConcurrentHashMap<>();

    /**
     * Modified version of {@link Class#enumConstantDirectory()}
     */
    @SuppressWarnings({"unchecked", "JavadocReference"})
    private static <T extends Enum<T>> Map<String, T> getEnumDirectory(Class<T> enumClass) {
        return (Map<String, T>) CACHE.computeIfAbsent(enumClass, clz -> {
            T[] universe = (T[]) clz.getEnumConstants();
            if (universe == null) return Map.of();
            Map<String, T> map = HashMap.newHashMap(universe.length);

            for (T constant : universe) {
                map.put(constant.name().toUpperCase(Locale.ROOT), constant);
            }
            return Map.copyOf(map);
        });
    }

    public static <T extends Enum<T>> @Nullable T get(@NotNull String key, @NotNull Class<T> enumClass) {
        return getEnumDirectory(enumClass).get(key.toUpperCase(Locale.ROOT));
    }

    public static <T extends Enum<T>> @NotNull T getOrElse(@NotNull String key, @NotNull Class<T> enumClass, @NotNull T defaultValue) {
        T value = get(key, enumClass);
        return value != null ? value : defaultValue;
    }

    public static <T extends Enum<T>> @NotNull T getOrElseGet(@NotNull String key, @NotNull Class<T> enumClass, @NotNull Supplier<T> supplier) {
        T value = get(key, enumClass);
        return value != null ? value : supplier.get();
    }

    public static <T extends Enum<T>> @NotNull Optional<T> getOptional(@NotNull String key, @NotNull Class<T> enumClass) {
        return Optional.ofNullable(get(key, enumClass));
    }

    public static <T extends Enum<T>> boolean contains(@NotNull String key, @NotNull Class<T> enumClass) {
        return getEnumDirectory(enumClass).containsKey(key.toUpperCase(Locale.ROOT));
    }

}
