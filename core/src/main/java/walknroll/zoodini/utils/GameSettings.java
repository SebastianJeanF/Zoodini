package walknroll.zoodini.utils;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;

public class GameSettings {

    private static GameSettings instance;

    public static GameSettings getInstance() {
        if (instance == null) {
            instance = new GameSettings();
        }
        return instance;
    }

    public static void setInstance(GameSettings settings) {
        instance = settings;
    }

    private int abilityKey;
    private int swapKey;
    private int followKey;
    private int p2AbilityKey;

    private String resolution;

    private float musicVolume;

    private float soundVolume;
    private boolean coopEnabled;

    public GameSettings() {
        this(Input.Keys.E, Input.Keys.SPACE, Input.Keys.F, Input.Keys.SHIFT_RIGHT, "1280x720", 100f,
                100f, false);
    }

    public GameSettings(int abilityKey, int swapKey, int followKey, int p2AbilityKey,
            String resolution, float musicVolume, float soundVolume, boolean coopEnabled) {
        this.abilityKey = abilityKey;
        this.swapKey = swapKey;
        this.followKey = followKey;
        this.p2AbilityKey = p2AbilityKey;
        this.resolution = resolution;
        this.musicVolume = musicVolume;
        this.soundVolume = soundVolume;
        this.coopEnabled = coopEnabled;
    }

    public GameSettings(GameSettings other) {
        this(other.abilityKey, other.swapKey, other.followKey, other.p2AbilityKey, other.resolution,
                other.musicVolume, other.soundVolume, other.coopEnabled);
    }

    public GameSettings(Preferences prefs) {
        GameSettings defaults = new GameSettings();
        this.abilityKey = prefs.getInteger("abilityKey", defaults.getAbilityKey());
        this.swapKey = prefs.getInteger("swapKey", defaults.getSwapKey());
        this.followKey = prefs.getInteger("followKey", defaults.getFollowKey());
        this.p2AbilityKey = prefs.getInteger("p2AbilityKey", defaults.getP2AbilityKey());
        this.resolution = prefs.getString("resolution", defaults.getResolution());
        this.musicVolume = prefs.getFloat("musicVolume", defaults.getMusicVolume());
        this.soundVolume = prefs.getFloat("soundVolume", defaults.getSoundVolume());
        this.coopEnabled = prefs.getBoolean("coopEnabled", defaults.isCoopEnabled());
    }

    public int getP2AbilityKey() {
        return p2AbilityKey;
    }

    public void setP2AbilityKey(int p2AbilityKey) {
        this.p2AbilityKey = p2AbilityKey;
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
        prefs.putInteger("p2AbilityKey", this.p2AbilityKey);
        prefs.putString("resolution", this.resolution);
        prefs.putBoolean("coopEnabled", this.coopEnabled);
    }
}
