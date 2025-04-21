package walknroll.zoodini.utils;

import com.badlogic.gdx.Input;

import walknroll.zoodini.utils.enums.AppResolution;

public class GameSettings {
    private int abilityKey;
    private int swapKey;
    private AppResolution resolution;

    public GameSettings() {
        this(Input.Keys.E, Input.Keys.SPACE, AppResolution.SMALL);
    }

    public GameSettings(int abilityKey, int swapKey, AppResolution resolution) {
        this.abilityKey = abilityKey;
        this.swapKey = swapKey;
        this.resolution = resolution;
    }

    public int getAbilityKey() {
        return abilityKey;
    }

    public int getSwapKey() {
        return swapKey;
    }

    public AppResolution getResolution() {
        return resolution;
    }
}
