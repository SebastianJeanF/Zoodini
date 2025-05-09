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
 * This player mode provides a basic loading screen.  While you could adapt it
 * for between level loading, it is currently designed for loading all assets
 * at the start of the game.
 *
 * @author: Walker M. White
 * @date: 11/21/2024
 */
package walknroll.zoodini.controllers.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ScreenUtils;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.util.ScreenListener;
import walknroll.zoodini.GDXRoot;
import walknroll.zoodini.utils.MenuButton;

/**
 * Class that provides a loading screen for the state of the game.
 *
 * This is a fairly generic loading screen that shows the GDIAC logo and a
 * progress bar. Once all assets are loaded, the progress bar is replaced
 * by a play button. You are free to adopt this to your needs.
 */
public class GameWinScene implements Screen, InputProcessor {
    // There are TWO asset managers.
    // One to load the loading screen. The other to load the assets
    /** The actual assets to be loaded */
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

    /** The current state of the play button */
    private Integer pressState;

    /** Whether or not this player mode is still active */
    private boolean active;

    private Array<MenuButton> buttons;

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

        pressState = null;
        Gdx.input.setInputProcessor(this);

        float buttonY = constants.getFloat("button.y");
        buttons = Array.with(new MenuButton(constants.getFloat("button.menu.x"), buttonY,
                constants.getFloat("button.menu.width"), constants.getFloat("button.menu.height"), "home-icon",
                GDXRoot.EXIT_MENU));
        if (!this.onLastLevel) {
            buttons.add(new MenuButton(constants.getFloat("button.next.x"), buttonY,
                    constants.getFloat("button.next.width"), constants.getFloat("button.next.height"),
                    "game-win-next-level", GDXRoot.EXIT_PLAY));
        }
    }

    /**
     * Returns true if all assets are loaded and the player is ready to go.
     *
     * @return true if the player is ready to go
     */
    public boolean isReady() {
        return pressState != null;
    }

    @Override
    public void dispose() {
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

            // We are are ready, notify our listener
            if (isReady() && listener != null) {
                listener.exitScreen(this, pressState);
            }
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

    // PROCESSING PLAYER INPUT
    /**
     * Called when the screen was touched or a mouse button was pressed.
     *
     * This method checks to see if the play button is available and if the click
     * is in the bounds of the play button. If so, it signals the that the button
     * has been pressed and is currently down. Any mouse button is accepted.
     *
     * @param screenX the x-coordinate of the mouse on the screen
     * @param screenY the y-coordinate of the mouse on the screen
     * @param pointer the button or touch finger number
     * @return whether to hand the event to other listeners.
     */
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {

        // Flip to match graphics coordinates
        screenY = height - screenY;

        // Play button is a circle.
        for (MenuButton menuButton : buttons) {
            if (menuButton.isPressed()) {
                return true;
            }
            if (menuButton.contains(screenX, screenY)) {
                menuButton.press();
                break;
            }
        }
        // float buttonX = constants.getFloat("button.x");
        // float buttonWidth = constants.getFloat("button.width");
        // if (screenX >= buttonX && screenX <= buttonX + buttonWidth) {
        // pressState = 1;
        // }
        // float cx = width / 2;
        // float cy = (int) (constants.getFloat("bar.height") * height);
        // float s = constants.getFloat("button.scale") * scale;
        // float radius = s * internal.getEntry("play", Texture.class).getWidth() /
        // 2.0f;
        // float dist = (screenX - cx) * (screenX - cx) + (screenY - cy) * (screenY -
        // cy);
        // if (dist < radius * radius) {
        // pressState = 1;
        // }
        return false;
    }

    /**
     * Called when a finger was lifted or a mouse button was released.
     *
     * This method checks to see if the play button is currently pressed down. If
     * so,
     * it signals the that the player is ready to go.
     *
     * @param screenX the x-coordinate of the mouse on the screen
     * @param screenY the y-coordinate of the mouse on the screen
     * @param pointer the button or touch finger number
     * @return whether to hand the event to other listeners.
     */
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        for (MenuButton menuButton : buttons) {
            if (menuButton.isPressed()) {
                pressState = menuButton.getPressedState();
                return false;
            }
        }
        return true;
    }

    /**
     * Called when a key is pressed
     *
     * Used to process quitting the game with the ESC key
     *
     * @param keycode the key pressed
     * @return whether to hand the event to other listeners.
     */
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            pressState = GDXRoot.EXIT_QUIT;
        }
        return true;
    }

    /**
     * Called when a key is typed (UNSUPPORTED)
     *
     * @param keycode the key typed
     * @return whether to hand the event to other listeners.
     */
    public boolean keyTyped(char character) {
        return true;
    }

    /**
     * Called when a key is released (UNSUPPORTED)
     *
     * @param keycode the key released
     * @return whether to hand the event to other listeners.
     */
    public boolean keyUp(int keycode) {
        return true;
    }

    // UNSUPPORTED METHODS FROM InputProcessor

    /**
     * Called when the mouse was moved without any buttons being pressed.
     * (UNSUPPORTED)
     *
     * @param screenX the x-coordinate of the mouse on the screen
     * @param screenY the y-coordinate of the mouse on the screen
     * @return whether to hand the event to other listeners.
     */
    public boolean mouseMoved(int screenX, int screenY) {
        return true;
    }

    /**
     * Called when the mouse wheel was scrolled. (UNSUPPORTED)
     *
     * @param dx the amount of horizontal scroll
     * @param dy the amount of vertical scroll
     *
     * @return whether to hand the event to other listeners.
     */
    public boolean scrolled(float dx, float dy) {
        return true;
    }

    /**
     * Called when the touch gesture is cancelled (UNSUPPORTED)
     *
     * Reason may be from OS interruption to touch becoming a large surface such
     * as the user cheek. Relevant on Android and iOS only. The button parameter
     * will be Input.Buttons.LEFT on iOS.
     *
     * @param screenX the x-coordinate of the mouse on the screen
     * @param screenY the y-coordinate of the mouse on the screen
     * @param pointer the button or touch finger number
     * @param button  the button
     * @return whether to hand the event to other listeners.
     */
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return true;
    }

    /**
     * Called when the mouse or finger was dragged. (UNSUPPORTED)
     *
     * @param screenX the x-coordinate of the mouse on the screen
     * @param screenY the y-coordinate of the mouse on the screen
     * @param pointer the button or touch finger number
     * @return whether to hand the event to other listeners.
     */
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return true;
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
        // Cornell colors
        ScreenUtils.clear(0.702f, 0.1255f, 0.145f, 1.0f);

        batch.begin(camera);
        batch.setColor(Color.WHITE);

        // Height lock the logo
        Texture texture = this.onLastLevel ? assets.getEntry("game-win-final-splash", Texture.class)
                : assets.getEntry("game-win-splash", Texture.class);
        float ratio = (float) width / (float) texture.getWidth();
        batch.draw(texture, 0, height - (ratio * texture.getHeight()), width, ratio * texture.getHeight());

        for (MenuButton menuButton : buttons) {
            Color tint = menuButton.isPressed() ? Color.GRAY : Color.WHITE;
            texture = assets.getEntry(menuButton.getAssetName(), Texture.class);

            batch.setColor(tint);
            batch.draw(texture, menuButton.x, menuButton.y, menuButton.width, menuButton.height);
        }
        batch.end();
    }

}
