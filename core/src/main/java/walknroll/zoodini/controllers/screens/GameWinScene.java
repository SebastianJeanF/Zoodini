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
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

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
public class GameWinScene implements Screen {

    // There are TWO asset managers.
    // One to load the loading screen. The other to load the assets
    /** The actual assets to be loaded */
    @SuppressWarnings("unused")
    private AssetDirectory assets;
    /** The drawing camera for this scene */
    private OrthographicCamera camera;
    /** Reference to sprite batch created by the root */
    private SpriteBatch batch;

    /** Listener that will update the player mode when we are done */
    private ScreenListener listener;
    /** The width of this scene */
    private int width;

    /** The height of this scene */
    private int height;

    private int nextLevel;
    private boolean onLastLevel;

    /** The constants for arranging images on the screen */
    JsonValue constants;

    /** Whether or not this player mode is still active */
    private boolean active;

    Affine2 cache = new Affine2();

    /** Background image */
    private Texture background;
    private Texture backgroundFinal;

    private Stage stage;

    private Skin skin;

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
     * @param canvas The game canvas to draw to
     * @param millis The loading budget in milliseconds
     */
    public GameWinScene(AssetDirectory assets, SpriteBatch batch, int nextLevel, int maxLevel) {
        this.batch = batch;
        this.assets = assets;
        this.nextLevel = nextLevel;
        this.onLastLevel = nextLevel > maxLevel;

        constants = assets.getEntry("constants", JsonValue.class).get("gameWinScreen");
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        this.background = assets.getEntry("game-win-splash", Texture.class);
        this.backgroundFinal = assets.getEntry("game-win-final-splash", Texture.class);
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }

    public int getNextLevel() {
        return nextLevel;
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

    public void create() {
        stage = new Stage(new ScreenViewport(camera));
        Gdx.input.setInputProcessor(stage);

        skin = new FreeTypeSkin(Gdx.files.internal("uiskins/zoodini/uiskin.json"));

        Table table = makeGameWinTable();

        stage.addActor(table);
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
        this.width = width;
        this.height = height;
        if (camera == null) {
            camera = new OrthographicCamera(width, height);
        } else {
            camera.setToOrtho(false, width, height);
        }
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

    private Table makeGameWinTable() {
        Table table = new Table();
        // table.setSize(this.width, this.height);
        table.setFillParent(true);
        table.defaults().space(10f);
        table.bottom().pad(Value.percentWidth(0.02f));

        Value labelWidth = Value.percentWidth(0.25f, table);

        TextButton toMenu = new TextButton("Level Select", skin);
        toMenu.addListener(new ChangeListener() {

            public void changed(ChangeEvent event, Actor actor) {
                listener.exitScreen(GameWinScene.this, GDXRoot.EXIT_LEVEL_SELECT);
            }
        });
        table.add(toMenu).expandX().left().width(labelWidth).bottom();

        if (!this.onLastLevel) {
            TextButton toNextLevel = new TextButton("Next Level", skin);
            toNextLevel.addListener(new ChangeListener() {

                public void changed(ChangeEvent event, Actor actor) {
                    listener.exitScreen(GameWinScene.this, GDXRoot.EXIT_PLAY);
                }
            });
            table.add(toNextLevel).left().width(labelWidth).bottom();
        }

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
        stage.act();
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
        batch.begin(camera);
        batch.setColor(Color.WHITE);
        Texture bgImage = onLastLevel ? backgroundFinal : background;
        float scaleX = (float) width / (float) bgImage.getWidth();
        float scaleY = (float) height / (float) bgImage.getHeight();
        cache.idt();
        cache.scale(scaleX, scaleY);
        batch.draw(bgImage, cache);

        batch.end();
        stage.draw();
    }

}
