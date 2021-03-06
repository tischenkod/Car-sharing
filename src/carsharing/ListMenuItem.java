package carsharing;

import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class ListMenuItem extends MenuItem{
    GetCaptionEvent onGetCaption;

    List<MenuItem> items;

    public ListMenuItem(int key, String name) {
        super(key, name);
    }

    public ListMenuItem() {
        super(0, "");
    }

    public ListMenuItem setOnGetCaption(GetCaptionEvent onGetCaption) {
        this.onGetCaption = onGetCaption;
        return this;
    }

    @Override
    void init() {
        items = new LinkedList<>();
    }

    ListMenuItem add(MenuItem item) {
        items.add(item);
        return this;
    }

    @Override
    MenuResult enter() {
        if (items.size() == 0) {
            printCaption();
            return MenuResult.MR_NORMAL;
        }
        int subItemKey = -1;
        while (true) {
            printCaption();
            for (MenuItem item :
                    items) {
                System.out.println(item.toString());
            }

            Scanner scanner = new Scanner(System.in);
            try {
                subItemKey = scanner.nextInt();
            } catch (Exception ignored) {
            }

            MenuItem subItem = itemByKey(subItemKey);
            if (subItem != null) {
                MenuResult result = subItem.enter();
                if (result == MenuResult.MR_BACK) {
                    if (result.stepCount == 1) {
                        return MenuResult.MR_NORMAL;
                    } else {
                        return result.stepCount(result.stepCount - 1);
                    }
                }
            }
        }
    }

    private void printCaption() {
        if (onGetCaption != null) {
            String caption = onGetCaption.handle(this);
            if (caption != null && !"".equals(caption)){
                System.out.println(caption);
            }
        }
    }

    private MenuItem itemByKey(int key) {
        for (MenuItem item :
                items) {
            if (item.key == key) {
                return item;
            }
        }
        return null;
    }

    public int itemCount() {
        return items.size();
    }

    public void clear() {
        items.clear();
    }

    public int size() {
        return items.size();
    }
}
