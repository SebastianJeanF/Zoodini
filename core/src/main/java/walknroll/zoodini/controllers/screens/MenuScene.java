/*
 * LoadingMode.java
 *
 * Asset loading is a really tricky problem. If you have a lot of sound or
 * images, it can take a long time to decompress them and load them into memory.
 * If you just have code at the start to load all your assets, your game will
 * look like it is hung at the start.
 *
 * The alternative is asynchronous asset loading. In asynchronous loading, you
 * load a little bit of the assets at a time, but still animate the game while
 * you are loading. This way the player knows the game is not hung, even though
 * he or she cannot do anything until loading is complete. You know those
 * loading screens with the inane tips that want to be helpful? That is
 * asynchronous loading.
 *
 * This player mode provides a basic loading screen. While you could adapt it
 * for between level loading, it is currently designed for loading all assets
 * at the start of the game.
 *
 * @author: Walker M. White
 *
 * @date: 11/21/2024
 */
package walknroll.zoodini.controllers.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.util.ScreenListener;
import walknroll.zoodini.GDXRoot;
import walknroll.zoodini.utils.FreeTypeSkin;

/**
 * Class that provides a loading screen for the state of the game.
 *
 * This is a fairly generic loading screen that shows the GDIAC logo and a
 * progress bar. Once all assets are loaded, the progress bar is replaced
 * by a play button. You are free to adopt this to your needs.
 */
public class MenuScene implements Screen {

	/** Default budget for asset loader (do nothing but load 60 fps) */
	private static int DEFAULT_BUDGET = 15;
	// There are TWO asset managers.
	// One to load the loading screen. The other to load the assets
	/** Internal assets for this loading screen */
	private AssetDirectory internal;

	/** The actual assets to be loaded */
	private AssetDirectory assets;
	/** Viewport */
	private Viewport viewport;
	/** Reference to sprite batch created by the root */
	private SpriteBatch batch;
	/** Listener that will update the player mode when we are done */
	private ScreenListener listener;
	/** The width of this scene */
	private int width;

	/** The height of this scene */
	private int height;

	/** The constants for arranging images on the screen */
	JsonValue constants;
	/** Current progress (0 to 1) of the asset manager */
	private float progress;
	/** The current state of the play button */
	private Integer pressState;

	/** Background image */
	private Texture background;
	/** logo */
	private Texture logo;

	/**
	 * The amount of time to devote to loading assets (as opposed to on screen
	 * hints, etc.)
	 */
	private int budget;

	/** Whether or not this player mode is still active */
	private boolean active;

	/** Scale factor for buttons/logo in screen. Equals 1 when resolution is 1280x720 */
	private float resScale;

	private Stage stage;
	private FreeTypeSkin skin;

	Affine2 cache = new Affine2();

	/**
	 * Creates a LoadingMode with the default budget, size and position.
	 *
	 * @param assets The asset directory to load from
	 * @param batch  The sprite batch to draw to
	 */
	public MenuScene(AssetDirectory assets, SpriteBatch batch) {
		this(assets, batch, DEFAULT_BUDGET);
	}

	/**
	 * Creates a LoadingMode with the default size and position.
	 *
	 * The budget is the number of milliseconds to spend loading assets each
	 * animation
	 * frame. This allows you to do something other than load assets. An animation
	 * frame is ~16 milliseconds. So if the budget is 10, you have 6 milliseconds to
	 * do something else. This is how game companies animate their loading screens.
	 *
	 * @param assets The asset directory to load from
	 * @param millis The loading budget in milliseconds
	 */
	public MenuScene(AssetDirectory assets, SpriteBatch batch, int millis) {
		this.batch = batch;
		budget = millis;

		// We need these files loaded immediately
		internal = new AssetDirectory("loading/boot.json");
		internal.loadAssets();
		internal.finishLoading();

		constants = internal.getEntry("constants", JsonValue.class);

		viewport = new FitViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		// No progress so far.
		progress = assets.getProgress();
		pressState = null;

		// Start loading the REAL assets
		this.assets = assets;
		this.assets.loadAssets();
		active = true;

		float resScaleX = Gdx.graphics.getWidth() / (float) constants.getFloat("screenWidth");
		float resScaleY = Gdx.graphics.getHeight() / (float) constants.getFloat("screenHeight");
		resScale = Math.min(resScaleX, resScaleY);

		background = internal.getEntry("splash", Texture.class);
		logo = internal.getEntry("logo", Texture.class);


	}

	public void create() {
		stage = new Stage(viewport);
		Gdx.input.setInputProcessor(stage);

		skin = new FreeTypeSkin(Gdx.files.internal("uiskins/zoodini/uiskin.json"));
        skin.resizeFont(resScale);

		Table table = makeSettingsTable();

		stage.addActor(table);
	}




	/**
	 * Returns the budget for the asset loader.
	 *
	 * The budget is the number of milliseconds to spend loading assets each
	 * animation frame. This allows you to do something other than load assets.
	 * An animation frame is ~16 milliseconds. So if the budget is 10, you have
	 * 6 milliseconds to do something else. This is how game companies animate
	 * their loading screens.
	 *
	 * @return the budget in milliseconds
	 */
	public int getBudget() {
		return budget;
	}

	/**
	 * Sets the budget for the asset loader.
	 *
	 * The budget is the number of milliseconds to spend loading assets each
	 * animation frame. This allows you to do something other than load assets.
	 * An animation frame is ~16 milliseconds. So if the budget is 10, you have
	 * 6 milliseconds to do something else. This is how game companies animate
	 * their loading screens.
	 *
	 * @param millis the budget in milliseconds
	 */
	public void setBudget(int millis) {
		budget = millis;
	}

	/**
	 * Returns true if all assets are loaded and the player is ready to go.
	 *
	 * @return true if the player is ready to go
	 */
	public boolean isReady() {
		return pressState != null;
	}

	/**
	 * Returns the asset directory produced by this loading screen
	 *
	 * This asset loader is NOT owned by this loading scene, so it persists even
	 * after the scene is disposed. It is your responsbility to unload the
	 * assets in this directory.
	 *
	 * @return the asset directory produced by this loading screen
	 */
	public AssetDirectory getAssets() {
		return assets;
	}

	/**
	 * Called when this screen should release all resources.
	 */
	public void dispose() {
		internal.unloadAssets();
		internal.dispose();
		skin.dispose();
		stage.dispose();
	}

	// ADDITIONAL SCREEN METHODS
	/**
	 * Called when the Screen should render itself.
	 *
	 * We defer to the other methods update() and draw(). However, it is VERY
	 * important
	 * that we only quit AFTER a draw.
	 *
	 * @param delta Number of seconds since last animation frame
	 */
	public void render(float delta) {
		if (active) {
			update(delta);
			draw();
		}
	}

	/**
	 * Called when the Screen is resized.
	 *
	 * This can happen at any point during a non-paused state but will never happen
	 * before a call to show().
	 *
	 * @param width  The new width in pixels
	 * @param height The new height in pixels
	 */
	public void resize(int width, int height) {
		// Compute the drawing scale
		this.width = width;
		this.height = height;
		viewport.update(width, height, true);

	}

	/**
	 * Called when the Screen is paused.
	 *
	 * This is usually when it's not active or visible on screen. An Application is
	 * also paused before it is destroyed.
	 */
	public void pause() {
		// TODO Auto-generated method stub

	}

	/**
	 * Called when the Screen is resumed from a paused state.
	 *
	 * This is usually when it regains focus.
	 */
	public void resume() {
		// TODO Auto-generated method stub

	}

	/**
	 * Called when this screen becomes the current screen for a Game.
	 */
	public void show() {
		// Useless if called in outside animation loop
		active = true;
		viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
	}

	/**
	 * Called when this screen is no longer the current screen for a Game.
	 */
	public void hide() {
		// Useless if called in outside animation loop
		active = false;
	}

	/**
	 * Sets the ScreenListener for this mode
	 *
	 * The ScreenListener will respond to requests to quit.
	 */
	public void setScreenListener(ScreenListener listener) {
		this.listener = listener;
	}

	private Table makeSettingsTable() {
		Table table = new Table();
		 table.setSize(this.width, this.height);
		table.setFillParent(true);
		table.defaults().spaceBottom(10f);
		table.bottom().pad(Value.percentWidth(0.01f));

		Value labelWidth = Value.percentWidth(0.25f, table);
        Value labelHeight = Value.percentHeight(0.09f, table);

		TextButton menuStart = new TextButton("Start", skin);
		menuStart.addListener(new ChangeListener() {

			public void changed(ChangeEvent event, Actor actor) {
				listener.exitScreen(MenuScene.this, GDXRoot.EXIT_LEVEL_SELECT);
			}
		});
		table.add(menuStart)
            .expandX()
            .left()
            .width(labelWidth)
            .height(labelHeight)
            .bottom();

		table.row();
		TextButton menuSettings = new TextButton("Settings", skin);
		menuSettings.addListener(new ChangeListener() {

			public void changed(ChangeEvent event, Actor actor) {
				listener.exitScreen(MenuScene.this, GDXRoot.EXIT_SETTINGS);
			}
		});
		table.add(menuSettings)
            .left()
            .width(labelWidth)
            .height(labelHeight)
            .bottom();

		table.row();
		TextButton menuCredits = new TextButton("Credits", skin);
		menuCredits.addListener(new ChangeListener() {

			public void changed(ChangeEvent event, Actor actor) {
				listener.exitScreen(MenuScene.this, GDXRoot.EXIT_CREDITS);
			}
		});
		table.add(menuCredits)
            .left()
            .width(labelWidth)
            .height(labelHeight)
            .bottom();

		table.row();
		TextButton menuQuit = new TextButton("Quit", skin);
		menuQuit.addListener(new ChangeListener() {

			public void changed(ChangeEvent event, Actor actor) {
				listener.exitScreen(MenuScene.this, GDXRoot.EXIT_QUIT);
			}
		});
		table.add(menuQuit)
            .left()
            .width(labelWidth)
            .height(labelHeight)
            .bottom();

		return table;
	}

	/**
	 * Update the status of this player mode.
	 *
	 * We prefer to separate update and draw from one another as separate methods,
	 * instead
	 * of using the single render() method that LibGDX does. We will talk about why
	 * we
	 * prefer this in lecture.
	 *
	 * @param delta Number of seconds since last animation frame
	 */
	private void update(float delta) {
		if (progress < 1.0f) {
			assets.update(budget);
			this.progress = assets.getProgress();
			if (progress >= 1.0f) {
				this.progress = 1.0f;
			}
		}
		stage.act(delta);
	}

	/**
	 * Draw the status of this player mode.
	 *
	 * We prefer to separate update and draw from one another as separate methods,
	 * instead
	 * of using the single render() method that LibGDX does. We will talk about why
	 * we
	 * prefer this in lecture.
	 */
	private void draw() {
		batch.begin(viewport.getCamera());
		batch.setColor(Color.WHITE);
		float scaleX = (float) width / (float) background.getWidth();
		float scaleY = (float) height / (float) background.getHeight();
		cache.idt();
		cache.scale(scaleX, scaleY);
		batch.draw(background, cache);

		batch.draw(logo,
				((width / 2f) - (logo.getWidth() / 2f) * resScale),
				(height - (logo.getHeight() + 50) * resScale),
				logo.getWidth() * resScale,
				logo.getHeight() * resScale
		);

		if (progress < 1.0f) {
			drawProgress();
			batch.end();
		} else {
			batch.end();
			stage.draw();
		}
	}

	/**
	 * Updates the progress bar according to loading progress
	 *
	 * The progress bar is composed of parts: two rounded caps on the end, and a
	 * rectangle in a middle. We adjust the
	 * size of the rectangle in the middle to represent the amount of progress.
	 */
	private void drawProgress() {
		float w = (int) (constants.getFloat("bar.width") * width);
		float cx = width / 2;
		float cy = (int) (constants.getFloat("bar.height") * height);
		TextureRegion region1, region2, region3;

		// "3-patch" the background
		batch.setColor(Color.WHITE);
		region1 = internal.getEntry("progress.backleft", TextureRegion.class);
		batch.draw(region1, cx - w / 2, cy, resScale * region1.getRegionWidth(), resScale * region1
				.getRegionHeight());

		region2 = internal.getEntry("progress.backright", TextureRegion.class);
		batch.draw(region2, cx + w / 2 - resScale * region2.getRegionWidth(), cy,
				resScale * region2.getRegionWidth(), resScale * region2.getRegionHeight());

		region3 = internal.getEntry("progress.background", TextureRegion.class);
		batch.draw(region3, cx - w / 2 + resScale * region1.getRegionWidth(), cy,
				w - resScale * (region2.getRegionWidth() + region1.getRegionWidth()),
				resScale * region3.getRegionHeight());

		// "3-patch" the foreground
		region1 = internal.getEntry("progress.foreleft", TextureRegion.class);
		batch.draw(region1, cx - w / 2, cy, resScale * region1.getRegionWidth(), resScale * region1
				.getRegionHeight());

		if (progress > 0) {
			region2 = internal.getEntry("progress.foreright", TextureRegion.class);
			float span = progress * (w - resScale * (region1.getRegionWidth() + region2
					.getRegionWidth()));

			batch.draw(region2, cx - w / 2 + resScale * region1.getRegionWidth() + span, cy,
					resScale * region2.getRegionWidth(), resScale * region2.getRegionHeight());

			region3 = internal.getEntry("progress.foreground", TextureRegion.class);
			batch.draw(region3, cx - w / 2 + resScale * region1.getRegionWidth(), cy,
					span, resScale * region3.getRegionHeight());
		} else {
			region2 = internal.getEntry("progress.foreright", TextureRegion.class);
			batch.draw(region2, cx - w / 2 + resScale * region1.getRegionWidth(), cy,
					resScale * region2.getRegionWidth(), resScale * region2.getRegionHeight());
		}

	}

}
