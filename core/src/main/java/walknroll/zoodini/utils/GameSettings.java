package walknroll.zoodini.utils;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;

public class GameSettings {

    private int abilityKey;
    private int swapKey;
    private int followKey;
    private String resolution;
    private float musicVolume;
    private float soundVolume;
    private boolean coopEnabled;

    public GameSettings() {
        this(Input.Keys.E, Input.Keys.SPACE, Input.Keys.F, "1280x720", 100f, 100f, false);
    }

    public GameSettings(int abilityKey, int swapKey, int followKey, String resolution,
            float musicVolume,
            float soundVolume, boolean coopEnabled) {
        this.abilityKey = abilityKey;
        this.swapKey = swapKey;
        this.followKey = followKey;
        this.resolution = resolution;
        this.musicVolume = musicVolume;
        this.soundVolume = soundVolume;
        this.coopEnabled = coopEnabled;
    }

    public GameSettings(GameSettings other) {
        this(other.abilityKey, other.swapKey, other.followKey, other.resolution, other.musicVolume,
                other.soundVolume, other.coopEnabled);
    }

    public GameSettings(Preferences prefs) {
        GameSettings defaults = new GameSettings();
        this.abilityKey = prefs.getInteger("abilityKey", defaults.getAbilityKey());
        this.swapKey = prefs.getInteger("swapKey", defaults.getSwapKey());
        this.followKey = prefs.getInteger("followKey", defaults.getFollowKey());
        this.resolution = prefs.getString("resolution", defaults.getResolution());
        this.musicVolume = prefs.getFloat("musicVolume", defaults.getMusicVolume());
        this.soundVolume = prefs.getFloat("soundVolume", defaults.getSoundVolume());
        this.coopEnabled = prefs.getBoolean("coopEnabled", defaults.isCoopEnabled());
    }

    public boolean isCoopEnabled() {
        return coopEnabled;
    }

    public void setCoopEnabled(boolean coopEnabled) {
        this.coopEnabled = coopEnabled;
    }

    public int getFollowKey() {
        return followKey;
    }

    public void setFollowKey(int followKey) {
        this.followKey = followKey;
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
        prefs.putInteger("followKey", this.followKey);
        prefs.putString("resolution", this.resolution);
        prefs.putBoolean("coopEnabled", this.coopEnabled);
    }
}
