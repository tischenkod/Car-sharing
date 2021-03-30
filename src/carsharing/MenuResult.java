package carsharing;

public enum MenuResult{
    MR_BACK,
    MR_NORMAL;

    int stepCount = 1;

    MenuResult stepCount(int count) {
        stepCount = count;
        return this;
    }
}
