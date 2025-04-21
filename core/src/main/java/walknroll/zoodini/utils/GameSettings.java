package walknroll.zoodini.utils;

import com.badlogic.gdx.Input;

public class GameSettings {
    private int abilityKey;
    private int swapKey;

    public GameSettings() {
        this(Input.Keys.E, Input.Keys.SPACE);
    }

    public GameSettings(int abilityKey, int swapKey) {
        this.abilityKey = abilityKey;
        this.swapKey = swapKey;
    }

    public int getAbilityKey() {
        return abilityKey;
    }

    public int getSwapKey() {
        return swapKey;
    }
}
