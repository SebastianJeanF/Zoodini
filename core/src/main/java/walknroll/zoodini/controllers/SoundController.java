package walknroll.zoodini.controllers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Controller for managing all game sounds and effects.
 * This is a singleton, meaning you call getInstance on anywhere in the project
 * and then call playSound(...) to play the sound. In Lab 4, these playSound calls are
 * made inside models, which have SoundEffect (from GDIAC extension) as their fields.
 * The current design where all sounds are loaded inside this class does not seem that bad though.
 *
 * TODO: replace access to Gdx internal with access using AssetDirectory for consistency, but not necessary right now.
 */
public class SoundController implements Disposable {
    private static SoundController instance;
    private final ObjectMap<String, Sound> soundEffects;
    private final ObjectMap<String, Music> music;
    private float soundVolume = 1.0f;
    private float musicVolume = 0.7f;
    private boolean soundEnabled = true;
    private boolean musicEnabled = true;
    private String currentMusic;

    /**
     * Private constructor for singleton pattern
     */
    private SoundController() {
        soundEffects = new ObjectMap<>();
        music = new ObjectMap<>();
        loadSounds();
        loadMusic();
    }

    /**
     * Get the singleton instance
     */
    public static SoundController getInstance() {
        if (instance == null) {
            instance = new SoundController();
        }
        return instance;
    }

    /**
     * Load all sound effects used in the game
     */
    private void loadSounds() {
        // Load cat sounds
        soundEffects.put("cat-meow", Gdx.audio.newSound(Gdx.files.internal("sounds/cat-meow.mp3")));
    }

    /**
     * Load all music tracks used in the game
     */
    private void loadMusic() {
        // Load background music
        music.put("game-music", Gdx.audio.newMusic(Gdx.files.internal("music/game-music.mp3")));
        soundEffects.put("ink-finish", Gdx.audio.newSound(Gdx.files.internal("sounds/explosion.mp3")));
    }

    /**
     * Play the meow sound when cat uses ability
     */
    public void playCatMeow() {
        playSound("cat-meow");
    }

    /**
     * Play the ink spray sound when octopus uses ability
     */
    public void playInkSpray() {
        playSound("ink-spray");
    }

    /**
     * Play the ink spray sound when ink lands on the target
     * */
    public void playInkFinish(){
        playSound("ink-finish");
    }

    /**
     * Play a sound by its key
     * @param soundKey The key of the sound to play
     */
    public void playSound(String soundKey) {
        if (soundEnabled && soundEffects.containsKey(soundKey)) {
            soundEffects.get(soundKey).play(soundVolume);
        }
    }

    /**
     * Play background music
     * @param musicKey The key of the music to play
     * @param looping Whether the music should loop
     */
    public void playMusic(String musicKey, boolean looping) {
        if (musicEnabled && music.containsKey(musicKey)) {
            // Stop current music if different
            if (currentMusic != null && !currentMusic.equals(musicKey)) {
                stopMusic();
            }

            Music track = music.get(musicKey);
            if (!track.isPlaying()) {
                track.setLooping(looping);
                track.setVolume(musicVolume);
                track.play();
                currentMusic = musicKey;
            }
        }
    }

    /**
     * Stop the currently playing music
     */
    public void stopMusic() {
        if (currentMusic != null && music.containsKey(currentMusic)) {
            music.get(currentMusic).stop();
            currentMusic = null;
        }
    }

    /**
     * Pause the currently playing music
     */
    public void pauseMusic() {
        if (currentMusic != null && music.containsKey(currentMusic)) {
            music.get(currentMusic).pause();
        }
    }

    /**
     * Resume the paused music
     */
    public void resumeMusic() {
        if (currentMusic != null && music.containsKey(currentMusic)) {
            music.get(currentMusic).play();
        }
    }

    /**
     * Set the volume for all sound effects
     * @param volume Volume level between 0 (silent) and 1 (max)
     */
    public void setSoundVolume(float volume) {
        this.soundVolume = Math.max(0, Math.min(1, volume));
    }

    /**
     * Set the volume for background music
     * @param volume Volume level between 0 (silent) and 1 (max)
     */
    public void setMusicVolume(float volume) {
        this.musicVolume = Math.max(0, Math.min(1, volume));
        if (currentMusic != null && music.containsKey(currentMusic)) {
            music.get(currentMusic).setVolume(musicVolume);
        }
    }

    /**
     * Enable or disable all sounds
     * @param enabled Whether sounds should be enabled
     */
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
    }

    /**
     * Enable or disable music
     * @param enabled Whether music should be enabled
     */
    public void setMusicEnabled(boolean enabled) {
        this.musicEnabled = enabled;
        if (!enabled) {
            stopMusic();
        }
    }

    /**
     * Clean up resources when they're no longer needed
     */
    @Override
    public void dispose() {
        for (Sound sound : soundEffects.values()) {
            sound.dispose();
        }
        soundEffects.clear();

        for (Music track : music.values()) {
            track.dispose();
        }
        music.clear();

        instance = null;
    }
}
