package walknroll.zoodini.utils;

import com.badlogic.gdx.Preferences;

public class GameState {
    private int highestClearance;
    private boolean storyboardSeen;

    public GameState(int highestLevelCleared, boolean storyboardSeen) {
        this.highestClearance = highestLevelCleared;
        this.storyboardSeen = storyboardSeen;
    }

    public GameState(Preferences prefs) {
        GameState defaults = new GameState();
        this.highestClearance = prefs.getInteger("highestLevelCleared", defaults.getHighestClearance());
        this.storyboardSeen = prefs.getBoolean("storyboardSeen", defaults.isStoryboardSeen());
    }

    public GameState() {
        this.highestClearance = 1;
        this.storyboardSeen = false;
    }

    @Override
    public String toString() {
        return "GameState [highestClearance=" + highestClearance + ", storyboardSeen=" + storyboardSeen + "]";
    }

    public int getHighestClearance() {
        return highestClearance;
    }

    public void setHighestClearance(int highestLevelCleared) {
        this.highestClearance = highestLevelCleared;
    }

    public boolean isStoryboardSeen() {
        return storyboardSeen;
    }

    public void setStoryboardSeen(boolean storyboardSeen) {
        this.storyboardSeen = storyboardSeen;
    }

    public void saveToPreferences(Preferences prefs) {
        prefs.putInteger("highestLevelCleared", this.highestClearance);
        prefs.putBoolean("storyboardSeen", this.storyboardSeen);
    }

}
