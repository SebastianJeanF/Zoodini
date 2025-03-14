/*
 * GameScene.java
 *
 * This is similar to PhysicsScene from Lab 4. However we have embedded the
 * world into the game level because of major problems with how box2d lights
 * works (we have been working to replace this awful library, but have not had
 * the time). This is particularly bad design. No model (GameLevel) should
 * ever depend on on a controller like a box2d physics world.
 *
 * @author: Walker M. White
 * @version: 2/15/2025
 */
package edu.cornell.cis3152.lighting.controllers.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import edu.cornell.cis3152.lighting.controllers.AIController;
import edu.cornell.cis3152.lighting.controllers.InputController;
import edu.cornell.cis3152.lighting.models.entities.Avatar;
import edu.cornell.cis3152.lighting.models.entities.Enemy;
import edu.cornell.cis3152.lighting.models.GameLevel;
import edu.cornell.cis3152.lighting.models.entities.Guard;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.TextAlign;
import edu.cornell.gdiac.graphics.TextLayout;
import edu.cornell.gdiac.util.*;

import edu.cornell.gdiac.physics2.*;

/**
 * Gameplay controller for the game.
 *
 * This class does not have the Box2d world. That is stored inside of the
 * LevelModel object, as the world settings are determined by the JSON
 * file. However, the class does have all of the controller functionality,
 * including collision listeners for the active level.
 *
 * You will notice that asset loading is very different. It relies on the
 * singleton asset manager to manage the various assets.
 */
public class GameScene implements Screen, ContactListener {
	// ASSETS
	/** Need an ongoing reference to the asset directory */
	protected AssetDirectory directory;
	/** The JSON defining the level model */
	private JsonValue levelFormat;
	/** The font for giving messages to the player */
	protected BitmapFont displayFont;
	/** The message to display */
	protected TextLayout message;

	/** Exit code for quitting the game */
	public static final int EXIT_QUIT = 0;
	/** How many frames after winning/losing do we continue? */
	public static final int EXIT_COUNT = 120;

	/** The orthographic camera */
	private OrthographicCamera camera;
	/** Reference to the game canvas */
	protected SpriteBatch batch;
	/** Listener that will update the player mode when we are done */
	private ScreenListener listener;

	/** Reference to the game level */
	protected GameLevel level;

	/** Whether or not this is an active controller */
	private boolean active;
	/** Whether we have completed this level */
	private boolean complete;
	/** Whether we have failed at this world (and need a reset) */
	private boolean failed;
	/** Countdown active for winning or losing */
	private int countdown;
    /** Controller for guards and security cameras */
    private AIController aiController;

	/** Mark set to handle more sophisticated collision callbacks */
	protected ObjectSet<Fixture> sensorFixtures;

    // Camera movement fields
    private Vector2 cameraTargetPosition;
    private Vector2 cameraPreviousPosition;
    private float cameraTransitionTimer;
    private float cameraTransitionDuration;
    private boolean inCameraTransition;

	/**
	 * Returns true if the level is completed.
	 *
	 * If true, the level will advance after a countdown
	 *
	 * @return true if the level is completed.
	 */
	public boolean isComplete() {
		return complete;
	}

	/**
	 * Sets whether the level is completed.
	 *
	 * If true, the level will advance after a countdown
	 *
	 * @param value whether the level is completed.
	 */
	public void setComplete(boolean value) {
		if (value) {
			BitmapFont font = directory.getEntry("display", BitmapFont.class);
			message = new TextLayout("Victory!", font);
			message.setAlignment(TextAlign.middleCenter);
			message.setColor(Color.YELLOW);
			message.layout();
			countdown = EXIT_COUNT;
		}
		complete = value;
	}

	/**
	 * Returns true if the level is failed.
	 *
	 * If true, the level will reset after a countdown
	 *
	 * @return true if the level is failed.
	 */
	public boolean isFailure() {
		return failed;
	}

	/**
	 * Sets whether the level is failed.
	 *
	 * If true, the level will reset after a countdown
	 *
	 * @param value whether the level is failed.
	 */
	public void setFailure(boolean value) {
		if (value) {
			message = new TextLayout("Failure", displayFont);
			message.setAlignment(TextAlign.middleCenter);
			message.layout();
			countdown = EXIT_COUNT;
		}
		failed = value;
	}

	/**
	 * Returns true if this is the active screen
	 *
	 * @return true if this is the active screen
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * Creates a new game world
	 *
	 * The physics bounds and drawing scale are now stored in the LevelModel and
	 * defined by the appropriate JSON file.
	 */
	public GameScene(AssetDirectory directory, SpriteBatch batch) {
		this.directory = directory;
		this.batch = batch;

		level = new GameLevel();
		levelFormat = directory.getEntry("level1", JsonValue.class);
		level.populate(directory, levelFormat);
		level.getWorld().setContactListener(this);

		complete = false;
		failed = false;
		active = false;
		countdown = -1;

		camera = new OrthographicCamera();
        System.out.println("");
		camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        // Initialize camera tracking variables
        cameraTargetPosition = new Vector2();
        cameraPreviousPosition = new Vector2();
        cameraTransitionTimer = 0;
        cameraTransitionDuration = directory.getEntry("constants", JsonValue.class).getFloat("CAMERA_INTERPOLATION_DURATION");
        System.out.println(cameraTransitionDuration);
        inCameraTransition = false;


		setComplete(false);
		setFailure(false);
	}

	/**
	 * Dispose of all (non-static) resources allocated to this mode.
	 */
	public void dispose() {
		level.dispose();
		level = null;
	}

	/**
	 * Resets the status of the game so that we can play again.
	 *
	 * This method disposes of the level and creates a new one. It will
	 * reread from the JSON file, allowing us to make changes on the fly.
	 */
	public void reset() {
		level.dispose();

		setComplete(false);
		setFailure(false);
		countdown = -1;
		message = null;

		// Reload the json each time
		level.populate(directory, levelFormat);
		level.getWorld().setContactListener(this);
	}

	/**
	 * Returns whether to process the update loop
	 *
	 * At the start of the update loop, we check if it is time
	 * to switch to a new game mode. If not, the update proceeds
	 * normally.
	 *
	 * @param delta Number of seconds since last animation frame
	 *
	 * @return whether to process the update loop
	 */
	public boolean preUpdate(float dt) {
		InputController input = InputController.getInstance();
		input.sync();

		if (listener == null) {
			return true;
		}

		// Toggle debug
		if (input.didDebug()) {
			level.setDebug(!level.getDebug());
		}

		// Handle resets
		if (input.didReset()) {
			reset();
		}

		// Now it is time to maybe switch screens.
		if (input.didExit()) {
			listener.exitScreen(this, EXIT_QUIT);
			return false;
		} else if (countdown > 0) {
			countdown--;
		} else if (countdown == 0) {
			reset();
		}

		return true;
	}

	private Vector2 angleCache = new Vector2();

	/**
	 * The core gameplay loop of this world.
	 *
	 * This method contains the specific update code for this mini-game. It does
	 * not handle collisions, as those are managed by the parent class
	 * WorldController.
	 * This method is called after input is read, but before collisions are
	 * resolved.
	 * The very last thing that it should do is apply forces to the appropriate
	 * objects.
	 *
	 * @param delta Number of seconds since last animation frame
	 */
	public void update(float dt) {
		// Process actions in object model
		InputController input = InputController.getInstance();

        if(input.zoomOut()){
            camera.setToOrtho(false, camera.viewportWidth + 1, camera.viewportHeight + 1);
        }

		if (input.didSwap()) {
			// stop active character movement
			level.getAvatar().setMovement(0, 0);
			level.getAvatar().applyForce();
            // Save previous camera position before swapping
            cameraPreviousPosition.set(cameraTargetPosition);
			// swap the active character
			level.swapActiveAvatar();

            // Start camera transition
            cameraTransitionTimer = 0;
            inCameraTransition = true;
		}
		Avatar avatar = level.getAvatar();

        // Update camera target to active avatar's position
        cameraTargetPosition.set(avatar.getPosition());

        // Update camera position with interpolation
        updateCamera(dt);

		// Rotate the avatar to face the direction of movement
		angleCache.set(input.getHorizontal(), input.getVertical());
		if (angleCache.len2() > 0.0f) {
			float angle = angleCache.angleDeg();
			// Convert to radians with up as 0
			angle = (float) Math.PI * (angle - 90.0f) / 180.0f;
			avatar.getObstacle().setAngle(angle);
		}
		angleCache.scl(avatar.getForce());
		avatar.setMovement(angleCache.x, angleCache.y);
		avatar.applyForce();

        camera.translate(1f, 0, 0);

//        camera.lookAt(camera.viewportHeight,camera.viewportWidth,0);
//        camera.

        camera.update();

        // Update guards
        updateGuards();
		// Turn the physics engine crank.
		level.update(dt);
	}

	/**
	 * Draw the physics objects to the canvas
	 *
	 * For simple worlds, this method is enough by itself. It will need
	 * to be overriden if the world needs fancy backgrounds or the like.
	 *
	 * The method draws all objects in the order that they were added.
	 */
	public void draw() {
		ScreenUtils.clear(0.39f, 0.58f, 0.93f, 1.0f);

        // Set the camera's updated view
        batch.setProjectionMatrix(camera.combined);

		level.draw(batch, camera);

		// Final message
		if (message != null) {
			batch.begin(camera);
			batch.setBlur(0.5f);
			batch.drawText(message, Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);
			batch.setBlur(0.0f);
			batch.end();
		}
	}

    /**
     * Updates the camera position with interpolation when transitioning
     */
    private void updateCamera(float dt) {
        if (inCameraTransition) {
            // Update transition timer
            cameraTransitionTimer += dt;

            if (cameraTransitionTimer >= cameraTransitionDuration) {
                // Transition complete
                inCameraTransition = false;
                camera.position.set(cameraTargetPosition.x, cameraTargetPosition.y, 0);
            } else {
                // Calculate interpolated position
                float alpha = cameraTransitionTimer / cameraTransitionDuration;
                float x = Interpolation.smooth.apply(cameraPreviousPosition.x, cameraTargetPosition.x, alpha);
                float y = Interpolation.smooth.apply(cameraPreviousPosition.y, cameraTargetPosition.y, alpha);
                camera.position.set(x, y, 0);
            }
        } else {
            // Just follow the target directly
            camera.position.set(cameraTargetPosition.x, cameraTargetPosition.y, 0);
        }

        // Apply scaling to match world units
        camera.position.x *= level.getLevelScaleX();
        camera.position.y *= level.getLevelScaleY();

        // Update the camera
        camera.update();
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
		// IGNORE FOR NOW
	}

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
			if (preUpdate(delta)) {
				update(delta);
			}
			draw();
		}
	}
    void updateGuards() {
        Array<Enemy> enemies = level.getEnemies();
        for (Enemy enemy : enemies) {
            if (!(enemy instanceof Guard))
                continue;

            Guard guard = (Guard) enemy;
            // Check for meow alert (Gar) or inked alert (Otto)

            // Reset meow alert when the guard reaches its target
            if ((guard.isMeowed() && guard.getPosition().dst(guard.getTarget()) < 0.1f)
            ) {
                guard.setMeow(false);
            }

            // Check Field-of-view (FOV), making guard agroed if they see a player

            if (guard.isMeowed()) {

            }

            guard.updatePatrol();
            moveGuard(guard);
        }



    }




    void moveGuard(Guard guard) {

        Vector2 guardPos = guard.getPosition();

        Vector2 targetPos = level.getAvatar().getPosition();

        if (!guard.isAgroed() && !guard.isMeowed() && guard.getTarget() != null) {
            targetPos = guard.getTarget();
        }
        Vector2 direction = new Vector2(targetPos).sub(guardPos);


        if (direction.len() > 0) {
            direction.nor().scl(guard.getForce());
            if (guard.isMeowed()) {
                direction.scl(0.5f);
            }
            else if (guard.isAgroed()){
                direction.scl(1.1f);
            }
            else if (guard.isCameraAlerted()) {
                direction.scl(1.5f);
            }

            guard.setMovement(direction.x, direction.y);
            // Update guard orientation to face the target.
            guard.setAngle(direction.angleRad());
        }

        // Update the guard's orientation to face the direction of movement.
        Vector2 movement = guard.getMovement();
        if (movement.len2() > 0.0001f) {  // Only update if there is significant movement
            guard.setAngle(movement.angleRad() - (float)Math.PI/2);
        }
        guard.applyForce();

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

	/**
	 * Callback method for the start of a collision
	 *
	 * This method is called when we first get a collision between two objects. We
	 * use
	 * this method to test if it is the "right" kind of collision. In particular, we
	 * use it to test if we made it to the win door.
	 *
	 * @param contact The two bodies that collided
	 */
	public void beginContact(Contact contact) {
		Fixture fix1 = contact.getFixtureA();
		Fixture fix2 = contact.getFixtureB();

		Body body1 = fix1.getBody();
		Body body2 = fix2.getBody();

		Object fd1 = fix1.getUserData();
		Object fd2 = fix2.getUserData();

		try {
			Obstacle bd1 = (Obstacle) body1.getUserData();
			Obstacle bd2 = (Obstacle) body2.getUserData();

			Obstacle avatar = level.getAvatar().getObstacle();
			Obstacle door = level.getExit().getObstacle();

			// Check for win condition
			if ((bd1 == avatar && bd2 == door) ||
					(bd1 == door && bd2 == avatar)) {
				setComplete(true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/** Unused ContactListener method */
	public void endContact(Contact contact) {
	}

	/** Unused ContactListener method */
	public void postSolve(Contact contact, ContactImpulse impulse) {
	}

	/** Unused ContactListener method */
	public void preSolve(Contact contact, Manifold oldManifold) {
	}
}
