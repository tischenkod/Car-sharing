package carsharing;

public class DynamicMenuItem extends ListMenuItem {
    EventHandler onEnter;


    public DynamicMenuItem(int key, String name, EventHandler onEnter) {
        super(key, name);
        this.onEnter = onEnter;
    }

    @Override
    MenuResult enter() {
        if (onEnter != null) {
            onEnter.handle(this);
        }
        return super.enter();
    }
}
