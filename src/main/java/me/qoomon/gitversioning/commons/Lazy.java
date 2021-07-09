package me.qoomon.gitversioning.commons;


import static java.util.Objects.requireNonNull;

public final class Lazy<T> {

    private volatile Initializer<T> initializer;
    private T object;

    public Lazy(Initializer<T> initializer) {
        this.initializer = requireNonNull(initializer);
    }

    public T get() {
        if (initializer != null) {
            synchronized (this) {
                if (initializer != null) {
                    try {
                        object = initializer.get();
                        initializer = null;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return object;
    }

    public static <T> Lazy<T> of(T value) {
        return new Lazy<>(() -> value);
    }

    public static <T> Lazy<T> of(Initializer<T> initializer) {
        return new Lazy<>(initializer);
    }

    @FunctionalInterface
    public interface Initializer<T> {
        T get() throws Exception;
    }
}