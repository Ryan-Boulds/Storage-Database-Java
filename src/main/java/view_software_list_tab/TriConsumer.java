package view_software_list_tab;

@FunctionalInterface
public interface TriConsumer<T, U, V> {
    void accept(T t, U u, V v);
}