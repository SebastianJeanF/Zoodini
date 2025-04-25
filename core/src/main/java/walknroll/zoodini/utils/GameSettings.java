package walknroll.zoodini.utils;

import com.badlogic.gdx.Input;

import walknroll.zoodini.utils.enums.AppResolution;

public class GameSettings {
    private int abilityKey;
    private int swapKey;

    private AppResolution resolution;
    private float musicVolume;

    private float soundVolume;

    public GameSettings() {
        this(Input.Keys.E, Input.Keys.SPACE, AppResolution.SMALL, 100f, 100f);
    }

    public GameSettings(int abilityKey, int swapKey, AppResolution resolution, float musicVolume, float soundVolume) {
        this.abilityKey = abilityKey;
        this.swapKey = swapKey;
        this.resolution = resolution;
        this.musicVolume = musicVolume;
        this.soundVolume = soundVolume;
    }

    public void setAbilityKey(int abilityKey) {
        this.abilityKey = abilityKey;
    }

    public void setSwapKey(int swapKey) {
        this.swapKey = swapKey;
    }

    public void setResolution(AppResolution resolution) {
        this.resolution = resolution;
    }

    public void setMusicVolume(float musicVolume) {
        this.musicVolume = musicVolume;
    }

    public void setSoundVolume(float soundVolume) {
        this.soundVolume = soundVolume;
    }

    public float getMusicVolume() {
        return musicVolume;
    }

    public float getSoundVolume() {
        return soundVolume;
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
