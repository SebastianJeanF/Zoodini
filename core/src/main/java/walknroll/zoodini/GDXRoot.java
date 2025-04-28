/*
 * GDXRoot.java
 *
 * This is the primary class file for running the game. It is the "static main"
 * of LibGDX. Its primary purpose is to swap between scenes.
 *
 * @author: Walker M. White
 * @version: 2/15/2025
 */
package walknroll.zoodini;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.SpriteSheet;
import edu.cornell.gdiac.util.ScreenListener;
import walknroll.zoodini.controllers.InputController;
import walknroll.zoodini.controllers.SoundController;
import walknroll.zoodini.controllers.screens.CreditsScene;
import walknroll.zoodini.controllers.screens.GameOverScene;
import walknroll.zoodini.controllers.screens.GameScene;
import walknroll.zoodini.controllers.screens.GameWinScene;
import walknroll.zoodini.controllers.screens.LevelSelectScene;
import walknroll.zoodini.controllers.screens.MenuScene;
import walknroll.zoodini.controllers.screens.SettingsScene;
import walknroll.zoodini.controllers.screens.StoryboardScene;
import walknroll.zoodini.models.entities.Guard;
import walknroll.zoodini.utils.GameSettings;
import walknroll.zoodini.utils.GameState;
import walknroll.zoodini.utils.LevelPortal;

/**
 * Root class for a LibGDX.
 *
 * This class is technically not the ROOT CLASS. Each platform has another class
 * above this (e.g. PC games use DesktopLauncher) which serves as the true root.
 * However, those classes are unique to each platform, while this class is the
 * same across all plaforms. In addition, this functions as the root class all
 * intents and purposes, and you would draw it as a root class in an
 * architecture
 * specification.
 */
public class GDXRoot extends Game implements ScreenListener {
	public static final int EXIT_QUIT = 0;
	public static final int EXIT_MENU = 1;
	public static final int EXIT_PLAY = 2;
	public static final int EXIT_SETTINGS = 3;
	public static final int EXIT_CREDITS = 4;
	public static final int EXIT_LEVEL_SELECT = 5;
	public static final int EXIT_LOSE = 6;
	public static final int EXIT_WIN = 7;
	public static final int EXIT_STORYBOARD = 8;

	private static final String SETTINGS_PREFERENCES_FILENAME = "zoodini-settings";
	private static final String STATE_PREFERENCES_FILENAME = "zoodini-state";

	/** AssetManager to load game assets (textures, data, etc.) */
	AssetDirectory directory;
	/** Drawing context to display graphics (VIEW CLASS) */
	private SpriteBatch batch;
	/** Player mode for the asset loading screen (CONTROLLER CLASS) */
	private MenuScene loading;
	/** Player mode for the the game proper (CONTROLLER CLASS) */
	private GameScene gameplay;
	private SettingsScene settings;
	private CreditsScene credits;
	private GameOverScene gameOver;
	private GameWinScene gameWin;
	private LevelSelectScene levelSelect;
	private StoryboardScene storyBoard;

	private Preferences settingsPrefs;
	private GameSettings gameSettings;

	private Preferences statePrefs;
	private GameState gameState;

	/**
	 * Creates a new game from the configuration settings.
	 */
	public GDXRoot() {
	}

	/**
	 * Called when the Application is first created.
	 *
	 * This is method immediately loads assets for the loading screen, and prepares
	 * the asynchronous loader for all other assets.
	 */
	public void create() {
		batch = new SpriteBatch();
		directory = new AssetDirectory("jsons/assets.json");
		settingsPrefs = Gdx.app.getPreferences(SETTINGS_PREFERENCES_FILENAME);
		gameSettings = new GameSettings(settingsPrefs);
		applyGameSettings();

		statePrefs = Gdx.app.getPreferences(STATE_PREFERENCES_FILENAME);
		gameState = new GameState(statePrefs);

		loading = new MenuScene(directory, batch, 1);

		loading.setScreenListener(this);
		setScreen(loading);
	}

	/**
	 * Called when the Application is destroyed.
	 *
	 * This is preceded by a call to pause().
	 */
	public void dispose() {
		// Call dispose on our children
		setScreen(null);
		if (loading != null) {
			loading.dispose();
		}
		if (gameplay != null) {
			gameplay.dispose();
		}
		if (settings != null) {
			settings.dispose();
		}
		if (credits != null) {
			credits.dispose();
		}
		if (levelSelect != null) {
			levelSelect.dispose();
		}
		if (gameOver != null) {
			gameOver.dispose();
		}
		if (gameWin != null) {
			gameWin.dispose();
		}
		if (storyBoard != null) {
			storyBoard.dispose();
		}

		batch.dispose();
		batch = null;

		// Unload all of the resources
		if (directory != null) {
			directory.unloadAssets();
			directory.dispose();
			directory = null;
		}
		super.dispose();
	}

	/**
	 * The given screen has made a request to exit its player mode.
	 *
	 * The value exitCode can be used to implement menu options.
	 *
	 * @param screen   The screen requesting to exit
	 * @param exitCode The state of the screen upon exit
	 */
	public void exitScreen(Screen screen, int exitCode) {
		Integer selectedLevel = null;

		if (screen == gameplay) {
			selectedLevel = gameplay.getCurrentLevel();
		} else if (screen == loading) {
			// In this section, we can initialize all static textures in various drawable
			// classes now that we have a loaded assets directory

			if (!LevelPortal.isLoaded()) {
				LevelPortal.setTextures(directory.getEntry("level-bg", Texture.class),
						directory.getEntry("level-cell", Texture.class));
			}
			if (!Guard.isLoaded()) {
				Guard.setSuspicionMeterCuriousTexture(directory.getEntry("guard-suspicion-curious", Texture.class));
			}
			if (!StoryboardScene.isLoaded()) {
				StoryboardScene.setSpriteSheet(directory.getEntry("storyboard.animation", SpriteSheet.class));
			}
		} else if (screen == settings) {
			// extract settings info from settings screen here
			gameSettings = settings.getSettings();
			gameSettings.saveToPreferences(settingsPrefs);
			settingsPrefs.flush();
			applyGameSettings();

			if (settings.shouldResetState()) {
				gameState = new GameState(); // new prefs are saved and flushed at end of method
			}
		} else if (screen == credits) {
			// nothing to extract here
		} else if (screen == levelSelect) {
			selectedLevel = levelSelect.getSelectedLevel();
		} else if (screen == gameOver) {
			selectedLevel = gameOver.getLostLevel();
		} else if (screen == gameWin) {
			selectedLevel = gameWin.getNextLevel();
		} else if (screen == storyBoard) {
			selectedLevel = storyBoard.getSelectedLevel();
			gameState.setStoryboardSeen(true);
		}

		if (selectedLevel != null && selectedLevel == gameState.getHighestClearance() + 1) {
			gameState.setHighestClearance(gameState.getHighestClearance() + 1);
		}

		switch (exitCode) {
			case GDXRoot.EXIT_CREDITS:
				credits = new CreditsScene(batch);
				credits.setScreenListener(this);
				setScreen(credits);
				disposeExcept(credits);
				break;
			case GDXRoot.EXIT_MENU:
				loading = new MenuScene(directory, batch, 1);
				loading.setScreenListener(this);
				setScreen(loading);
				disposeExcept(loading);
				break;
			case GDXRoot.EXIT_LEVEL_SELECT:
				if (directory == null) {
					throw new RuntimeException("Asset directory was somehow not loaded after initial boot");
				}
				JsonValue levels = directory.getEntry("levels", JsonValue.class);
				Array<Integer> levelKeys = new Array<>();
				for (JsonValue value : levels) {
					levelKeys.add(Integer.parseInt(value.name()));
				}

				levelSelect = new LevelSelectScene(batch, directory, levelKeys, gameState.getHighestClearance());
				levelSelect.create();
				levelSelect.setScreenListener(this);
				setScreen(levelSelect);
				disposeExcept(levelSelect);
				break;
			case GDXRoot.EXIT_QUIT:
				Gdx.app.exit();
				break;
			case GDXRoot.EXIT_SETTINGS:
				settings = new SettingsScene(batch, directory, gameSettings);
				settings.create();
				settings.setScreenListener(this);
				setScreen(settings);
				disposeExcept(settings);
				break;
			case GDXRoot.EXIT_LOSE:
				gameOver = new GameOverScene(directory, batch, selectedLevel);
				gameOver.setScreenListener(this);
				setScreen(gameOver);
				disposeExcept(gameOver);
				break;
			case GDXRoot.EXIT_WIN:
				// TODO: in the future, each level will have a point to the next level
				gameWin = new GameWinScene(directory, batch, selectedLevel + 1);
				gameWin.setScreenListener(this);
				setScreen(gameWin);
				disposeExcept(gameWin);
				break;
			case GDXRoot.EXIT_STORYBOARD:
				if (gameState.isStoryboardSeen()) {
					startGameplay(selectedLevel);
				} else {
					storyBoard = new StoryboardScene(batch, directory, selectedLevel);
					storyBoard.create();
					storyBoard.setScreenListener(this);
					setScreen(storyBoard);
					disposeExcept(storyBoard);
					break;
				}
			case GDXRoot.EXIT_PLAY:
				startGameplay(selectedLevel);
				break;
			default:
				break;
		}

		gameState.saveToPreferences(statePrefs);
		statePrefs.flush();
	}

	private void applyGameSettings() {
		InputController.getInstance().setAbilityKey(this.gameSettings.getAbilityKey());
		InputController.getInstance().setSwapKey(this.gameSettings.getSwapKey());
		switch (this.gameSettings.getResolution().toLowerCase()) {
			case "1280x720" -> Gdx.graphics.setWindowedMode(1280, 720);
			case "1920x1080" -> Gdx.graphics.setWindowedMode(1920, 1080);
			case "fullscreen" -> Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());

		}
		SoundController.getInstance().setMusicVolume(this.gameSettings.getMusicVolume() / 100f);
		SoundController.getInstance().setSoundVolume(this.gameSettings.getSoundVolume() / 100f);
	}

	private void startGameplay(Integer selectedLevel) {
		if (directory == null) {
			throw new RuntimeException("Asset directory was somehow not loaded after initial boot");
		}
		if (selectedLevel == null) {
			throw new RuntimeException(
					"Tried to change to GameScene without properly setting the target level");
		}
		gameplay = new GameScene(directory, batch, selectedLevel);
		gameplay.setScreenListener(this);
		setScreen(gameplay);
		disposeExcept(gameplay);
	}

	private void disposeExcept(Screen screen) {
		if (gameplay != null && screen != gameplay) {
			gameplay.dispose();
			gameplay = null;
		}
		if (loading != null && screen != loading) {
			loading.dispose();
			loading = null;
		}
		if (settings != null && screen != settings) {
			settings.dispose();
			settings = null;
		}
		if (credits != null && screen != credits) {
			credits.dispose();
			credits = null;
		}
		if (levelSelect != null && screen != levelSelect) {
			levelSelect.dispose();
			levelSelect = null;
		}
		if (gameOver != null && screen != gameOver) {
			gameOver.dispose();
			gameOver = null;
		}
		if (gameWin != null && screen != gameWin) {
			gameWin.dispose();
			gameWin = null;
		}
		if (storyBoard != null && screen != storyBoard) {
			storyBoard.dispose();
			storyBoard = null;
		}
	}
}
