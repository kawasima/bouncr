package net.unit8.bouncr.hook;

@FunctionalInterface
public interface Hook<T> {
    void run(T message);
}
