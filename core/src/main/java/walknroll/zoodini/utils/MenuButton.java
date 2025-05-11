package walknroll.zoodini.utils;

/** Wrapper class for instantiating menu buttons */
public class MenuButton {
    public float x;
    public float y;
    public float width;
    public float height;

    private int pressedState;
    private String assetName;
    private boolean pressed;

    public MenuButton(float x, float y, float width, float height, String assetName, int pressedState) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.assetName = assetName;
        this.pressedState = pressedState;
        this.pressed = false;
    }

    public boolean contains(float x, float y) {
        return (x >= this.x) && (y >= this.y) && (x <= this.x + this.width) && (y <= this.y + this.height);
    }

    public String getAssetName() {
        return this.assetName;
    }

    public int getPressedState() {
        return this.pressedState;
    }

    public void press() {
        this.pressed = !this.pressed;
    }

    public boolean isPressed() {
        return this.pressed;
    }

    /** True when the mouse is currently over this button */
    private boolean hovered = false;

    public void setHovered(boolean isOver) {
        hovered = isOver;
    }

    public boolean isHovered() {
        return hovered;
    }

}
