/*
 * GDXRoot.java
 *
 * This is the primary class file for running the game. It is the "static main"
 * of LibGDX. Its primary purpose is to swap between scenes.
 *
 * @author: Walker M. White
 * @version: 2/15/2025
 */
package edu.cornell.cis3152.lighting;

import com.badlogic.gdx.*;
import edu.cornell.cis3152.lighting.controllers.screens.GameScene;
import edu.cornell.cis3152.lighting.controllers.screens.LoadingScene;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.util.*;

/**
 * Root class for a LibGDX.
 *
 * This class is technically not the ROOT CLASS. Each platform has another class
 * above this (e.g. PC games use DesktopLauncher) which serves as the true root.
 * However, those classes are unique to each platform, while this class is the
 * same across all plaforms. In addition, this functions as the root class all
 * intents and purposes, and you would draw it as a root class in an architecture
 * specification.
 */
public class GDXRoot extends Game implements ScreenListener {
	/** AssetManager to load game assets (textures, data, etc.) */
	AssetDirectory directory;
	/** Drawing context to display graphics (VIEW CLASS) */
	private SpriteBatch batch;
	/** Player mode for the asset loading screen (CONTROLLER CLASS) */
	private LoadingScene loading;
	/** Player mode for the the game proper (CONTROLLER CLASS) */
	private GameScene gameplay;

	/**
	 * Creates a new game from the configuration settings.
	 */
	public GDXRoot() {}

	/**
	 * Called when the Application is first created.
	 *
	 * This is method immediately loads assets for the loading screen, and prepares
	 * the asynchronous loader for all other assets.
	 */
	public void create() {
		batch  = new SpriteBatch();
		loading = new LoadingScene("jsons/assets.json",batch,1);

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
		if (screen == loading) {
			directory = loading.getAssets();
			gameplay = new GameScene(directory, batch);
			gameplay.setScreenListener(this);
			setScreen(gameplay);

			loading.dispose();
			loading = null;
		} else if (exitCode == GameScene.EXIT_QUIT) {
			// We quit the main application
			Gdx.app.exit();
		}
	}

}
