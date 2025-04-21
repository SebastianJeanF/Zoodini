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

import com.badlogic.gdx.*;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.util.*;
import walknroll.zoodini.controllers.screens.CreditsScene;
import walknroll.zoodini.controllers.screens.GameScene;
import walknroll.zoodini.controllers.screens.MenuScene;
import walknroll.zoodini.controllers.screens.SettingsScene;

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
		loading = new MenuScene("jsons/assets.json", batch, 1);

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
		switch (exitCode) {
			case GDXRoot.EXIT_CREDITS:
				credits = new CreditsScene(batch);
				credits.setScreenListener(this);
				setScreen(credits);
				disposeExcept(credits);
				break;
			case GDXRoot.EXIT_MENU:
				loading = new MenuScene("jsons/assets.json", batch, 1);
				loading.setScreenListener(this);
				setScreen(loading);
				disposeExcept(loading);
				break;
			case GDXRoot.EXIT_PLAY:
				directory = loading.getAssets();
				gameplay = new GameScene(directory, batch, 6);
				gameplay.setScreenListener(this);
				setScreen(gameplay);
				disposeExcept(gameplay);
				break;
			case GDXRoot.EXIT_QUIT:
				Gdx.app.exit();
				break;
			case GDXRoot.EXIT_SETTINGS:
				settings = new SettingsScene(batch);
				settings.setScreenListener(this);
				setScreen(settings);
				disposeExcept(settings);
				break;
			default:
				break;
		}
		// if (screen == loading) {
		// directory = loading.getAssets();
		// gameplay = new GameScene(directory, batch);
		// gameplay.setScreenListener(this);
		// setScreen(gameplay);

		// loading.dispose();
		// loading = null;
		// } else if (exitCode == GameScene.EXIT_QUIT) {
		// // We quit the main application
		// Gdx.app.exit();
		// }
	}

}
