package carsharing;

@FunctionalInterface
public interface GetCaptionEvent {
    String handle(MenuItem sender);
}
