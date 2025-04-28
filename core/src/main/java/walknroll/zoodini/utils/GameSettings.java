package walknroll.zoodini.utils;

import com.badlogic.gdx.Input;

public class GameSettings {
    private int abilityKey;
    private int swapKey;

    private String resolution;
    private float musicVolume;

    private float soundVolume;

    public GameSettings() {
        this(Input.Keys.E, Input.Keys.SPACE, "1280x720", 100f, 100f);
    }

    public GameSettings(int abilityKey, int swapKey, String resolution, float musicVolume, float soundVolume) {
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

    public void setResolution(String resolution) {
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

    public String getResolution() {
        return resolution;
    }
}
