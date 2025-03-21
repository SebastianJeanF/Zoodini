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
package walknroll.zoodini.controllers.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.TextAlign;
import edu.cornell.gdiac.graphics.TextLayout;
import edu.cornell.gdiac.util.*;
import walknroll.zoodini.controllers.GuardAIController;
import walknroll.zoodini.controllers.InputController;
import walknroll.zoodini.controllers.UIController;
import walknroll.zoodini.models.GameLevel;
import walknroll.zoodini.models.entities.Avatar;
import walknroll.zoodini.models.entities.Enemy;
import walknroll.zoodini.models.entities.Guard;
import walknroll.zoodini.models.entities.Octopus;
import walknroll.zoodini.models.entities.SecurityCamera;
import walknroll.zoodini.models.entities.Avatar.AvatarType;
import walknroll.zoodini.utils.GameGraph;
import edu.cornell.gdiac.physics2.*;
import java.util.HashMap;

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
	/** The JSON defining the default entity configs */
	private JsonValue levelGlobals;
	/** The font for giving messages to the player */
	protected BitmapFont displayFont;
	/** The message to display */
	protected TextLayout message;

	/** Exit code for quitting the game */
	public static final int EXIT_QUIT = 0;
	/** How many frames after winning/losing do we continue? */
	public static final int EXIT_COUNT = 120;

    /** Whether the player has collected the key */
    private boolean keyCollected = false;
    /** Timer for how long to display the key message */
    private int keyMessageTimer = 0;
    /** The character that collected the key */
    private Avatar keyCollector = null;
    /** Timer for how long the door has been unlocking */
    private int unlockingTimer = 0;
    /** Duration required to unlock the door (in frames) */
    private static final int UNLOCK_DURATION = 180;
    /** Whether the door is currently being unlocking */
    private boolean isUnlocking = false;

	/** The orthographic camera */
	private OrthographicCamera camera;
	/** Reference to the game canvas */
	protected SpriteBatch batch;
	/** Listener that will update the player mode when we are done */
	private ScreenListener listener;


    private UIController ui;

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

	/** Mark set to handle more sophisticated collision callbacks */
	protected ObjectSet<Fixture> sensorFixtures;

    /** The current level */
    private final HashMap<Guard, GuardAIController> guardToAIController = new HashMap<>();

    private GameGraph gameGraph;

    private Texture pixelTexture;



	// Camera movement fields
	private Vector2 cameraTargetPosition;
	private Vector2 cameraPreviousPosition;
	private float cameraTransitionTimer;
	private float cameraTransitionDuration;
	private boolean inCameraTransition;

	// general-purpose cache vector
	private Vector2 cacheVec;

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
    private boolean octopusArrived = false;
    private boolean catArrived = false;
    public void setComplete(boolean value) {
		if (value) {
			BitmapFont font = directory.getEntry("display", BitmapFont.class);
            TextLayout message = new TextLayout("Victory!", font);
			message.setAlignment(TextAlign.middleCenter);
			message.setColor(Color.YELLOW);
			message.layout();
            ui.setFont(font);
            ui.setMessage(message);
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
            BitmapFont font = directory.getEntry("display", BitmapFont.class);
            TextLayout message = new TextLayout("Failure!", font);
            message.setAlignment(TextAlign.middleCenter);
            message.setColor(Color.RED);
            message.layout();
            ui.setFont(font);
            ui.setMessage(message);
            countdown = EXIT_COUNT;
        }
        complete = value;
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
		levelGlobals = directory.getEntry("globals", JsonValue.class);
		level.populate(directory, levelFormat, levelGlobals);
		level.getWorld().setContactListener(this);

		complete = false;
		failed = false;
		active = false;
		countdown = -1;

		camera = new OrthographicCamera();
		// System.out.println("");
		// System.out.println("");
		camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		// Initialize camera tracking variables
		cameraTargetPosition = new Vector2();
		cameraPreviousPosition = new Vector2();
		cameraTransitionTimer = 0;
		cameraTransitionDuration = directory.getEntry("constants", JsonValue.class)
				.getFloat("CAMERA_INTERPOLATION_DURATION");
		inCameraTransition = false;

        ui = new UIController();
		cacheVec = new Vector2();

		setComplete(false);
		setFailure(false);
        initializeAIControllers();
        System.out.println("here");

        this.pixelTexture = createPixelTexture();
	}

	/**
	 * Dispose of all (non-static) resources allocated to this mode.
	 */
	public void dispose() {
		level.dispose();
		level = null;
        if (pixelTexture != null) {
            pixelTexture.dispose();
            pixelTexture = null;
        }
	}

    // Create a 1x1 white pixel texture to use for drawing shapes
    private Texture createPixelTexture() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture pixelTexture = new Texture(pixmap);
        pixmap.dispose();
        return pixelTexture;
    }

    public void initializeAIControllers() {
        // TODO: Make numRows and numCols dynamically generated based on level
        final float tileSizePhys = 0.5f; // Units are physics/world units
        final int numRows = 12 * MathUtils.ceil(1/ tileSizePhys);
        final int numCols = 16 * MathUtils.ceil(1/tileSizePhys);


        this.gameGraph = new GameGraph(tileSizePhys, numRows,  numCols, level.getBounds().x, level.getBounds().y, level.getSprites());

//        this.gameGraph = new GameGraph(tileSizePhys, numRows,  numCols, level.getBounds().x, level.getBounds().y, level.getSprites());
        Array<Enemy> enemies = level.getEnemies();
        for (Enemy enemy : enemies) {
            if (!(enemy instanceof Guard guard))
                continue;

            GuardAIController aiController = new GuardAIController(guard, level, this.gameGraph, 5);
            guardToAIController.put(guard, aiController);
        }

//        gameGraph.printGrid();
    }



	/**
	 * Resets the status of the game so that we can play again.
	 *
	 * This method disposes of the level and creates a new one. It will
	 * reread from the JSON file, allowing us to make changes on the fly.
	 */
	public void reset() {
		level.dispose();

        catArrived = false;
        octopusArrived = false;
        keyCollected = false;
        keyCollector = null;
        unlockingTimer = 0;
        isUnlocking = false;
        ui.reset();

		setComplete(false);
		setFailure(false);
		countdown = -1;
		message = null;

		// Reload the json each time
		level.populate(directory, levelFormat, levelGlobals);
		level.getWorld().setContactListener(this);
        initializeAIControllers();
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

        onSwap(input);

        Avatar avatar = level.getAvatar();

        checkFlipSprite(avatar, input);

        updateOctopusInkAim(avatar, input);

        rotateEntities(input, avatar);

        moveEntities(avatar);

        // Update camera target to active avatar's position
        cameraTargetPosition.set(avatar.getPosition());
        // Update camera position with interpolation
        updateCamera(dt);

        ui.update();

        // Update key message timer
        if(keyMessageTimer > 0) {
            keyMessageTimer--;
            if(keyMessageTimer == 0) {
                ui.setMessage(null); // Clear message when timer expires
            }
        }

        updateDoorUnlocking();

        checkDeactivateKeyOnCollect();

        // Set meow guard flag
        // TODO: Ideally, guards should only notice the meow the frame AFTER it happened,
        // but this is good enough for now
//        if (input.didAbility() && level.getAvatar() instanceof Cat ) {
//            for (Enemy t : level.getEnemies()) {
//                Guard guard = (Guard) t;
//                float DISTRACT_DISTANCE = 5.0f;
//                if (guard.getPosition().dst(avatar.getPosition()) < DISTRACT_DISTANCE) {
////                    guard.setMeow(true);
////                    guard.setTarget(avatar.getPosition());
//                }
//            }
//        }

		// Update guards
        updateGuardAI();
        updateGuards();

        updateInkProjectile();

        // Turn the physics engine crank.
		level.update(dt);
	}

    private void checkDeactivateKeyOnCollect() {
        // Deactivate collected key's physics body if needed
        if (keyCollected && level.getKey() != null && level.getKey().getObstacle().isActive()) {
            // This is the safe time to modify physics bodies
            level.getKey().getObstacle().setActive(false);
        }
    }

    private void onSwap(InputController input) {
        if (input.didSwap()) {
            // stop active character movement
            level.getAvatar().setMovement(0, 0);
            level.getAvatar().applyForce();
            // Save previous camera position before swapping
            cameraPreviousPosition.set(cameraTargetPosition);
            // Save previous camera position before swapping
            cameraPreviousPosition.set(cameraTargetPosition);
            // swap the active character
            level.swapActiveAvatar();

            // Start camera transition
            cameraTransitionTimer = 0;
            inCameraTransition = true;
        }
    }

    private void updateGuardAI() {
        guardToAIController.forEach((guard, controller) -> {
            controller.update();
            guard.think(controller.getMovementDirection(), controller.getNextTargetLocation());
        });
    }

    private void updateInkProjectile() {
        if (level.getProjectile().getToHide()) {
            level.hideInkProjectile();
            level.getProjectile().setToHide(false);
        }
    }

    private void updateDoorUnlocking() {
        // Update door unlocking progress
        if(isUnlocking) {
            unlockingTimer++;

            // Update unlocking message percentage
            if (unlockingTimer % 15 == 0) { // Update message every 1/4 second
                BitmapFont font = directory.getEntry("display", BitmapFont.class);
                TextLayout message = new TextLayout("Unlocking Door: " +
                    Math.round((float) unlockingTimer / UNLOCK_DURATION * 100) + "%",
                    font);
                message.setAlignment(TextAlign.middleCenter);
                message.setColor(Color.YELLOW);
                message.layout();
                ui.setFont(font);
                ui.setMessage(message);
            }
            // Check if door is fully unlocked
            if (unlockingTimer >= UNLOCK_DURATION) {
                level.getDoor().setLocked(false);
                isUnlocking = false;

                // Show door unlocked message
                BitmapFont font = directory.getEntry("display", BitmapFont.class);
                TextLayout message = new TextLayout("Door Unlocked!", font);
                message.setAlignment(TextAlign.middleCenter);
                message.setColor(Color.GREEN);
                message.layout();
                ui.setFont(font);
                ui.setMessage(message);
                keyMessageTimer = 120; // 2 seconds at 60 fps to show unlock message
            }
        }
    }

    private void moveEntities(Avatar avatar) {
        angleCache.scl(avatar.getForce());
        avatar.setMovement(angleCache.x, angleCache.y);
        avatar.applyForce();
    }

    private void rotateEntities(InputController input, Avatar avatar) {
        // Rotate the avatar to face the direction of movement
        angleCache.set(input.getHorizontal(), input.getVertical());
        if (angleCache.len2() > 0.0f) {
            float angle = angleCache.angleDeg();
            // Convert to radians with up as 0
            angle = (float) Math.PI * (angle - 90.0f) / 180.0f;
            avatar.getObstacle().setAngle(angle);
        }
    }

    private void updateOctopusInkAim(Avatar avatar, InputController input) {
        if (avatar.getAvatarType() == AvatarType.OCTOPUS) {
			Octopus octopus = (Octopus) avatar;

			if (input.isAbilityHeld()) {
				octopus.setCurrentlyAiming(true);
				Vector3 unprojected = camera.unproject(
						new Vector3(input.getAiming().x, input.getAiming().y, 0));
				cacheVec.set(unprojected.x / level.getLevelScaleX(),
						unprojected.y / level.getLevelScaleY());

				// TODO: max length should be a configurable value
				float scale = Math.min(cacheVec.dst(avatar.getPosition()) * level.getLevelScaleX(), 250);
				double dx = avatar.getPosition().x - cacheVec.x;
				double dy = avatar.getPosition().y - cacheVec.y;
				float angleRad = -((float) (Math.atan2(dx, dy) + Math.toRadians(90))); // scuffed math (TODO: fix?)
				cacheVec.set((float) Math.toDegrees(Math.cos(angleRad)), (float) Math.toDegrees(Math.sin(angleRad)))
						.nor().scl(scale);
				octopus.setTarget(cacheVec);
			}
			if (octopus.isCurrentlyAiming() && !input.isAbilityHeld()) {
				octopus.setCurrentlyAiming(false);
				octopus.setDidFire(true);
			}

			if (octopus.didFire()) {
				level.hideInkProjectile();
				level.createInkProjectile();
				octopus.setDidFire(false);
			}

			if ((level.getProjectile().getPosition().dst(avatar.getPosition()) * level.getLevelScaleX()) > (octopus
					.getTarget().len())) {
				level.getProjectile().setToHide(true);

			}
		}
    }

    private static void checkFlipSprite(Avatar avatar, InputController input) {
        // flips the sprite if the avatar is moving left
        if (!avatar.isFlipped() && input.getHorizontal() == -1f || avatar.isFlipped() && input.getHorizontal() == 1f) {
            avatar.flipSprite();
        }
    }
    // In your GameScene class (or a dedicated debug-drawing class)
//    private ShapeRenderer shapeRenderer = new ShapeRenderer();
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
		// Set the camera's updated view
		batch.setProjectionMatrix(camera.combined);

		level.draw(batch, camera);

        // Final message
        ui.draw(batch);

        // Draw the graph of one of the guards
        guardToAIController.values().stream().findFirst().ifPresent(
            controller -> controller.drawGraphDebug(batch, camera, pixelTexture));

        for (Enemy enemy : level.getEnemies()) {
            if (enemy instanceof Guard guard) {
                guard.drawSusLevelBar(batch);
            }
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
//			if ((guard.isMeowed() && guard.getPosition().dst(guard.getTarget()) < 0.1f)) {
//				guard.setMeow(false);
//			}


			// Check Field-of-view (FOV), making guard agroed if they see a player

			moveGuard(guard);
			if (guard.isMeowed()) {

			}

			// guard.updatePatrol();
			// moveGuard(guard);
		}

	}


	void moveGuard(Guard guard) {


        Vector2 direction = guard.getMovementDirection();
        // System.out.print("Direction" + direction);



		if (direction.len() > 0) {
			direction.nor().scl(guard.getForce());
			if (guard.isMeowed()) {
				direction.scl(0.25f);
			} else if (guard.isCameraAlerted()) {
                direction.scl(2.0f);
            }
            else if (guard.isAgroed()) {
				direction.scl(1.25f);
			}

			guard.setMovement(direction.x, direction.y);
		}

		// Update the guard's orientation to face the direction of movement.
		Vector2 movement = guard.getMovementDirection();
		if (movement.len2() > 0.0001f) { // Only update if there is significant movement
			guard.setAngle(movement.angleRad() - (float) Math.PI/2);
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

        boolean gameOver = countdown != -1;
        if (gameOver) return;

        try {

            Obstacle o1 = (Obstacle) body1.getUserData();
            Obstacle o2 = (Obstacle) body2.getUserData();

            Obstacle cat = level.getCat().getObstacle();
            Obstacle oct = level.getOctopus().getObstacle();
            Obstacle exit = level.getExit().getObstacle();
            Obstacle door = level.getDoor().getObstacle();
			Obstacle projectile = level.getProjectile().getObstacle();
            Array<Enemy> guards = level.getEnemies();

			if ((o1 == projectile || o2 == projectile)) {
				Array<SecurityCamera> secCameras = level.getSecurityCameras();
				for (int i = 0; i < secCameras.size; i++) {
					SecurityCamera cam = secCameras.get(i);
					Obstacle camObstacle = secCameras.get(i).getObstacle();
					if (o1 == camObstacle || o2 == camObstacle) {
						cam.setDisabled(true);
						break;
					}
				}
				level.getProjectile().setToHide(true);
			}


            for(Enemy guard : guards){
                Obstacle enemy = guard.getObstacle();
                if((o1 == cat && o2 == enemy) || (o2 == cat && o1 == enemy) || (o1 == oct && o2 == enemy) || (o2 == oct && o1 == enemy)){
                    setFailure(true);
                }
            }

            // Handle key pickup
            if(!keyCollected && level.getKey() != null) {
                Obstacle keyObs = level.getKey().getObstacle();
                if(((o1 == cat || o1 == oct) && o2 == keyObs) ||
                    ((o2 == cat || o2 == oct) && o1 == keyObs)){
                    keyCollected = true;
                    level.getKey().setCollected(true);
                    if (o1 == cat || o2 == cat){keyCollector = level.getCat();}
                    else if (o1 == oct || o2 == oct){keyCollector = level.getOctopus();}

                    // Display a message that key was collected
                    BitmapFont font = directory.getEntry("display", BitmapFont.class);
                    TextLayout message = new TextLayout("Key Collected!", font);
                    message.setAlignment(TextAlign.middleCenter);
                    message.setColor(Color.YELLOW);
                    message.layout();
                    ui.setFont(font);
                    ui.setMessage(message);

                    // Make the message disappear after a few seconds
                    keyMessageTimer = 120; // 2 seconds at 60 fps
                }
            }

            // Handle door unlocking
            if(keyCollected && level.getDoor().isLocked() && keyCollector != null) {
                // Check if the key collector is standing on the door
                if((o1 == keyCollector.getObstacle() && o2 == door) ||
                    (o2 == keyCollector.getObstacle() && o1 == door)) {
                    isUnlocking = true;

                    // Display unlocking message
                    BitmapFont font = directory.getEntry("display", BitmapFont.class);
                    TextLayout message = new TextLayout("Unlocking Door: 0%", font);
                    message.setAlignment(TextAlign.middleCenter);
                    message.setColor(Color.YELLOW);
                    message.layout();
                    ui.setFont(font);
                    ui.setMessage(message);
                }
            }

            // Handle exit collision (only if door is unlocked)
            if((o1 == cat && o2 == exit) || (o2 == cat && o1 == exit)){
                System.out.println("Cat arrived");
                catArrived = true;
            }

            if((o1 == oct && o2 == exit) || (o2 == oct && o1 == exit)){
                System.out.println("Otto arrived");
                octopusArrived = true;
            }

            if(catArrived && octopusArrived && !failed){
                setComplete(true);
            }


		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** Unused ContactListener method */
	public void endContact(Contact contact) {
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        Object fd1 = fix1.getUserData();
        Object fd2 = fix2.getUserData();

        try {

            Obstacle o1 = (Obstacle) body1.getUserData();
            Obstacle o2 = (Obstacle) body2.getUserData();

            Obstacle cat = level.getCat().getObstacle();
            Obstacle oct = level.getOctopus().getObstacle();
            Obstacle exit = level.getExit().getObstacle();
            Obstacle door = level.getDoor().getObstacle();


            // Handle door unlocking cancellation
            if(isUnlocking && keyCollector != null) {
                // Check if the key collector left the door
                if((o1 == keyCollector.getObstacle() && o2 == door) ||
                    (o2 == keyCollector.getObstacle() && o1 == door)) {
                    isUnlocking = false;
                    unlockingTimer = 0;

                    // Display message that unlocking was interrupted
                    BitmapFont font = directory.getEntry("display", BitmapFont.class);
                    TextLayout message = new TextLayout("Unlocking Interrupted!", font);
                    message.setAlignment(TextAlign.middleCenter);
                    message.setColor(Color.RED);
                    message.layout();
                    ui.setFont(font);
                    ui.setMessage(message);
                    keyMessageTimer = 60; // 1 second at 60 fps
                }
            }


            if((o1 == cat && o2 == exit) || (o2 == cat && o1 == exit)){
                catArrived = false;
            }

            if((o1 == oct && o2 == exit) || (o2 == oct && o1 == exit)){
                octopusArrived = false;
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
	}

	/** Unused ContactListener method */
	public void postSolve(Contact contact, ContactImpulse impulse) {
	}

	/** Unused ContactListener method */
	public void preSolve(Contact contact, Manifold oldManifold) {
	}
}
