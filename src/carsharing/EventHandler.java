package carsharing;

@FunctionalInterface
public interface EventHandler {
    void handle(MenuItem sender);
}
