package carsharing;

import java.util.function.Function;
import java.util.function.Supplier;

public class ActionMenuItem extends MenuItem{
    Function<MenuItem, MenuResult> action;
    public ActionMenuItem(int key, String name, Function<MenuItem, MenuResult> action) {
        super(key, name);
        this.action = action;
    }

    @Override
    MenuResult enter() {
        return action.apply(this);
    }
}
