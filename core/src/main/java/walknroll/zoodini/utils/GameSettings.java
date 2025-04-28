package walknroll.zoodini.utils;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;

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

    public GameSettings(Preferences prefs) {
        GameSettings defaults = new GameSettings();
        this.abilityKey = prefs.getInteger("abilityKey", defaults.getAbilityKey());
        this.swapKey = prefs.getInteger("swapKey", defaults.getSwapKey());
        this.resolution = prefs.getString("resolution", defaults.getResolution());
        this.musicVolume = prefs.getFloat("musicVolume", defaults.getMusicVolume());
        this.soundVolume = prefs.getFloat("soundVolume", defaults.getSoundVolume());
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

    public void saveToPreferences(Preferences prefs) {
        prefs.putFloat("musicVolume", this.musicVolume);
        prefs.putFloat("soundVolume", this.soundVolume);
        prefs.putInteger("abilityKey", this.abilityKey);
        prefs.putInteger("swapKey", this.swapKey);
        prefs.putString("resolution", this.resolution);
    }
}
