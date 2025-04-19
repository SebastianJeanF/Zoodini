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
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.util.*;
import org.w3c.dom.Text;
import walknroll.zoodini.GDXRoot;
import walknroll.zoodini.controllers.GuardAIController;
import walknroll.zoodini.controllers.InputController;
import walknroll.zoodini.controllers.UIController;
import walknroll.zoodini.controllers.aitools.TileGraph;
import walknroll.zoodini.controllers.aitools.TileNode;
import walknroll.zoodini.models.GameLevel;
import walknroll.zoodini.models.entities.*;
import walknroll.zoodini.models.entities.Avatar.AvatarType;
import walknroll.zoodini.models.nonentities.Door;
import walknroll.zoodini.models.nonentities.InkProjectile;
import walknroll.zoodini.models.nonentities.Key;
import edu.cornell.gdiac.physics2.*;
import walknroll.zoodini.utils.VisionCone;
import walknroll.zoodini.utils.ZoodiniSprite;

import java.util.HashMap;
import java.util.Map;

import walknroll.zoodini.utils.VisionCone;
import walknroll.zoodini.utils.ZoodiniSprite;

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

    private boolean debug = true;

	// ASSETS
	/** Need an ongoing reference to the asset directory */
	protected AssetDirectory directory;
	/** The JSON defining the level model */
	private JsonValue levelFormat;
	/** The JSON defining the default entity configs */
	private JsonValue levelGlobals;
    /** Value for current level */
    private int currentLevel;


    /** How many frames after winning/losing do we continue? */
	public static final int EXIT_COUNT = 120;

	/** The orthographic camera */
	private OrthographicCamera camera;

	/** Reference to the game canvas */
	protected SpriteBatch batch;
	/** Listener that will update the player mode when we are done */
	private ScreenListener listener;
    /** All the UI elements belong to this */
    private UIController ui;
	/** Reference to the game level */
	protected GameLevel level;
	/** Mark set to handle more sophisticated collision callbacks */
	protected ObjectSet<Fixture> sensorFixtures;
    /** The current level */
    private final HashMap<Guard, GuardAIController> guardToAIController = new HashMap<>();

    /** TiledMap read from TMX */
    private TiledMap map;
    /** Graph representing the map */
    private TileGraph<TileNode> graph;


    // Win/lose related fields
    /** Whether or not this is an active controller */
    private boolean active;
    /** Whether we have completed this level */
    private boolean complete;
    /** Whether we have failed at this world (and need a reset) */
    private boolean failed;
    /** Countdown active for winning or losing */
    private int countdown;

	// Camera movement fields
	private Vector2 cameraTargetPosition;
	private Vector2 cameraPreviousPosition;
	private float cameraTransitionTimer;
	private float cameraTransitionDuration;
	private boolean inCameraTransition;

	// general-purpose cache vector
	private Vector2 cacheVec = new Vector2();


	/**
	 * Creates a new game world
	 *
	 * The physics bounds and drawing scale are now stored in the LevelModel and
	 * defined by the appropriate JSON file.
	 */
	public GameScene(AssetDirectory directory, SpriteBatch batch, int currentLevel) {
		this.directory = directory;
		this.batch = batch;
        this.currentLevel = currentLevel;
        level = new GameLevel();
        levelGlobals = directory.getEntry("globals", JsonValue.class);
        map = new TmxMapLoader().load(directory.getEntry("levels", JsonValue.class).getString(""+this.currentLevel));
		level.populate(directory, map, levelGlobals);
		level.getWorld().setContactListener(this);

		complete = false;
		failed = false;
		active = false;
		countdown = -1;

		camera = new OrthographicCamera();
        //30m, 20m is the map dimension. 1 tile = 1m
		camera.setToOrtho(false, level.getTileSize() * 15,  level.getTileSize() * 10);
        // Initialize camera tracking variables
		cameraTargetPosition = new Vector2();
		cameraPreviousPosition = new Vector2();
		cameraTransitionTimer = 0;
		cameraTransitionDuration = directory.getEntry("constants", JsonValue.class)
				.getFloat("CAMERA_INTERPOLATION_DURATION");
		inCameraTransition = false;


        //UI controller is not working as intended. Someone fix plz
        JsonValue avatarIcons = levelGlobals.get("avatarIcons");
        ui = new UIController();
        ui.setFont(directory.getEntry("display", BitmapFont.class));
        TextureRegion catIcon = new TextureRegion(directory.getEntry(avatarIcons.get("cat-texture").asString(), Texture.class));
        TextureRegion octopusIcon = new TextureRegion(directory.getEntry(avatarIcons.get("octopus-texture").asString(), Texture.class));
        ui.setCatIcon(catIcon);
        ui.setOctopusIcon(octopusIcon);
        ui.init();

        graph = new TileGraph<>(map, false);
        initializeAIControllers();

		setComplete(false);
		setFailure(false);
	}


    //-----------------Main logic--------------------------//

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

        ui.reset();

        setComplete(false);
        setFailure(false);
        countdown = -1;

        map = new TmxMapLoader().load(directory.getEntry("levels", JsonValue.class).getString(""+this.currentLevel));
        // Reload the json each time
        level.populate(directory, map, levelGlobals);
        level.getWorld().setContactListener(this);
        initializeAIControllers();
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
                draw();
            }
        }
    }

    /**
     * Returns whether to process the update loop
     *
     * At the start of the update loop, we check if it is time
     * to switch to a new game mode. If not, the update proceeds
     * normally.
     *
     * @param dt Number of seconds since last animation frame
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
            listener.exitScreen(this, GDXRoot.EXIT_MENU);
            return false;
        } else if (countdown > 0) {
            countdown--;
        } else if (countdown == 0) {
            reset();
        }

        return true;
    }

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
     * @param dt Number of seconds since last animation frame
     */
    public void update(float dt) {
        InputController input = InputController.getInstance();
        processPlayerAction(input, dt);
        level.update(dt); //collisions
        updateVisionCones(dt);
        updateGuardAI();
        processNPCAction(dt);
        updateCamera(dt);
    }


    private void thinkAll(float dt){
        InputController input = InputController.getInstance();
    }

    private void moveAll(float dt){

    }

    private void updateSecurityCameraVisionCones() {
        ObjectMap<ZoodiniSprite, VisionCone> visions = level.getVisionConeMap();
        for(ObjectMap.Entry<ZoodiniSprite, VisionCone> entry : visions.entries()){
            if (entry.key instanceof SecurityCamera && !((SecurityCamera) entry.key).isDisabled()) {
                Vector2 catPos = level.getCat().getPosition();
                Vector2 octPos = level.getOctopus().getPosition();

                if (entry.value.contains(catPos) || entry.value.contains(octPos)) {
                    ((SecurityCamera) entry.key).activateRing();
                    // Alert all guards

                    Avatar detectedPlayer = entry.value.contains(catPos) ? level.getCat() : level.getOctopus();

                    for (Guard guard : level.getGuards()) {
                        float guardToCameraDistance = guard.getPosition().dst(((SecurityCamera) entry.key).getPosition());
                        if (guardToCameraDistance <= ((SecurityCamera) entry.key).getAlarmDistance()) {
                            guard.setAggroTarget(detectedPlayer);
                            guard.setCameraAlerted(true);

                            // Optionally set target position directly if needed
                            guard.setTarget(detectedPlayer.getPosition());
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if the player is in the vision cones of any guards and updates their states accordingly.
     * This function specifically handles guard vision detection, separate from security cameras.
     */
    private void updateGuardVisionCones(float dt) {
        ObjectMap<ZoodiniSprite, VisionCone> visions = level.getVisionConeMap();

        for(ObjectMap.Entry<ZoodiniSprite, VisionCone> entry: visions.entries()) {
            // Skip if not a guard
            if (!(entry.key instanceof Guard)) {
                continue;
            }

            Guard guard = (Guard) entry.key;
            VisionCone visionCone = entry.value;

            Vector2 movementDir = guard.getMovementDirection();
            visionCone.updateFacingDirection(dt, movementDir);


            Vector2 catPos = level.getCat().getPosition();
            Vector2 octPos = level.getOctopus().getPosition();

            // Check if cat is detected
            if (visionCone.contains(catPos)) {
                guard.setAgroed(true);
                guard.setAggroTarget(level.getCat());
                guard.setTarget(level.getCat().getPosition());
//                System.out.println("Guard detected cat: " + guard.getAggroTarget());
            }

            // Check if octopus is detected
            else if (visionCone.contains(octPos)) {
                guard.setAgroed(true);
                guard.setAggroTarget(level.getOctopus());
                guard.setTarget(level.getOctopus().getPosition());
//                System.out.println("Guard detected octopus: " + guard.getAggroTarget());
            }
            // No player detected
            else {
                // Only set to false if the guard isn't being alerted by a camera
                if (!guard.isCameraAlerted()) {
                    guard.setAgroed(false);
                }
            }
        }
    }

    /**
     * Updates all vision cone detection in the game.
     * Delegates to specific functions for guards and security cameras.
     */
    public void updateVisionCones(float dt) {
        updateSecurityCameraVisionCones();
        updateGuardVisionCones(dt);
    }

    private Vector3 tmp = new Vector3();
    private Vector2 tmp2 = new Vector2();

    /**
     * Applies movement forces to the avatar and change firing states.
     */
    private void processPlayerAction(InputController input, float dt){
        tmp.setZero();
        tmp2.setZero();

        if(input.didSwap()){
            onSwap(input);
        }

        Avatar avatar = level.getAvatar();
        float vertical = input.getVertical();
        float horizontal = input.getHorizontal();
        moveAvatar(vertical, horizontal, avatar);
        level.getOctopus().regenerateInk(dt);

        if(avatar.getAvatarType() == AvatarType.OCTOPUS){
            Octopus octopus = (Octopus) avatar;
            tmp.set(input.getAiming(), 0);
            tmp = camera.unproject(tmp);
            tmp2.set(tmp.x, tmp.y)
                .scl(1.0f / level.getTileSize())
                .sub(octopus.getPosition())
                .clamp(0.0f, octopus.getAbilityRange()); //this decides the distance for projectile to travel
            octopus.setTarget(tmp2); //set a target vector relative to octopus's position as origin.

            if(input.didAbility() && octopus.canUseAbility()) { //check for ink resource here.
                octopus.setCurrentlyAiming(!octopus.isCurrentlyAiming()); //turn the reticle on and off
            }

            if(octopus.isCurrentlyAiming() && input.didLeftClick()){
                octopus.setDidFire(true);
                octopus.setCurrentlyAiming(false);
                octopus.consumeInk();
            } else {
                octopus.setDidFire(false);
            }

        } else if(avatar.getAvatarType() == AvatarType.CAT) {
            Cat cat = (Cat) avatar;
            cat.setMeowed(input.didAbility());
        }


        tmp.setZero();
        tmp2.setZero();
    }

    /**
     * Applies movement forces to NPCs.
     * Does NOT modify internal states of the NPCs. That is the
     * responsibility of ContactListener
     */
    private void processNPCAction(float dt){
        Octopus octopus = level.getOctopus();
        InkProjectile inkProjectile = level.getProjectile();
        ObjectMap<Door, Key> doors = level.getDoors();
        Array<Key> keys = level.getKeys();

        //Projectiles
        //TODO: not sure about the order of if statements here.
        if(inkProjectile.getShouldDestroy()){
            inkProjectile.destroy();
        }

        if(octopus.didFire()){
            activateInkProjectile(inkProjectile, octopus.getPosition(), octopus.getTarget());
        }

        if(inkProjectile.getPosition().dst(inkProjectile.getStartPosition()) > inkProjectile.getEndPosition().len()){
            inkProjectile.setShouldDestroy(true);
        }

        Array<Guard> guards = level.getGuards();
        updateGuards(guards);

        Array<SecurityCamera> cams = level.getSecurityCameras();


        // TODO: Might need to comment out again
        for (ObjectMap.Entry<Door, Key> entry : doors.entries()) {
            Door door = entry.key;
            Key key = entry.value;

            if(key.isCollected() && !key.isUsed() && door.isUnlocking()){

                float progress = 1.0f - (door.getRemainingTimeToUnlock() / door.getUnlockDuration());
                Vector2 doorPos = door.getObstacle().getPosition().cpy();
                float tileSize = level.getTileSize();
                door.showUnlockProgress(progress, doorPos, camera, tileSize);
            } else {
                door.resetTimer();
                door.hideUnlockProgress();
            }

            if(door.getRemainingTimeToUnlock() <= 0){
                key.setUsed(true);
                door.hideUnlockProgress();
            }
        }

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


        if(debug) {
            graph.clearTargetNodes();

            // For each guard, mark their target nodes for display
            guardToAIController.forEach((guard, controller) -> {
                Vector2 targetLocation = controller.getNextTargetLocation();
                if (targetLocation != null) {
                    graph.markPositionAsTarget(targetLocation);
                }
            });

            graph.draw(batch, camera, level.getTileSize());
            InputController ic = InputController.getInstance();
            if(ic.didLeftClick()) {
                TileNode t = graph.markNearestTile(camera, ic.getAiming(), level.getTileSize());
            }
        }

        // batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        // Final message

//        ui.draw(batch);
        ui.draw(batch, camera, level);
    }

    /**
     * Dispose of all (non-static) resources allocated to this mode.
     */
    public void dispose() {
        level.dispose();
        level = null;
    }


    //-----------------Helper Methods--------------------//

    public void initializeAIControllers() {
        guardToAIController.clear();

        graph = new TileGraph<>(map, true);

//        this.gameGraph = new GameGraph(12, 16, level.getBounds().x, level.getBounds().y, level.getSprites());
        Array<Guard> guards = level.getGuards();
        for (Guard g : guards) {
            GuardAIController aiController = new GuardAIController(g, level, graph);
            guardToAIController.put(g, aiController);
        }
    }

    private void updateGuardAI() {
        guardToAIController.forEach((guard, controller) -> {
            controller.update();
            guard.think(controller.getMovementDirection(), controller.getNextTargetLocation());
        });
    }


//    private void checkDeactivateKeyOnCollect() {
//        // Deactivate collected key's physics body if needed
//        if (keyCollected && level.getKey() != null && level.getKey().getObstacle().isActive()) {
//            // This is the safe time to modify physics bodies
//            level.getKey().getObstacle().setActive(false);
//        }
//    }

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

    private Vector2 angleCache = new Vector2();

    /**
     * @param verticalForce
     * @param horizontalForce
     * @param avatar
     */
    private void moveAvatar(float verticalForce, float horizontalForce, Avatar avatar) {
        // Rotate the avatar to face the direction of movement
        angleCache.set(horizontalForce, verticalForce);
        if (angleCache.len2() > 0.0f) {
            // Prevent faster movement when going diagonally
            if (angleCache.len() > 1.0f) {
                angleCache.nor();
            }

            float angle = angleCache.angleDeg();
            // Convert to radians with up as 0
            angle = (float) Math.PI * (angle - 90.0f) / 180.0f;
            avatar.getObstacle().setAngle(angle);
        }

        angleCache.scl(avatar.getForce()).scl(level.getTileSize());
        avatar.setMovement(angleCache.x, angleCache.y);
        avatar.applyForce();
    }

    private void activateInkProjectile(InkProjectile projectile, Vector2 origin, Vector2 target) {
        projectile.activate();
        projectile.setPosition(origin);
        projectile.setStartPosition(origin);
        projectile.setEndPosition(target);
        projectile.setMovement(target.nor());
        projectile.applyForce();
    }


	/**
	 * Updates the camera position with interpolation when transitioning
	 */
	private void updateCamera(float dt) {
        cameraTargetPosition.set(level.getAvatar().getPosition());

        // Get viewport dimensions in world units
        float viewWidth = camera.viewportWidth / level.getTileSize();
        float viewHeight = camera.viewportHeight / level.getTileSize();


        // Calculate soft boundaries that allow partial dead space
        float minX = level.getBounds().x + (viewWidth * 0.5f);
        float maxX = level.getBounds().x + (level.getBounds().width) - (viewWidth * 0.5f);
        float minY = level.getBounds().y + (viewHeight * 0.5f);
        float maxY = level.getBounds().y + (level.getBounds().height) - (viewHeight * 0.5f);

        // Clamp camera position with soft boundaries
        cameraTargetPosition.x = Math.max(minX, Math.min(cameraTargetPosition.x, maxX));
        cameraTargetPosition.y = Math.max(minY, Math.min(cameraTargetPosition.y, maxY));

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
		camera.position.x *= level.getTileSize();
		camera.position.y *= level.getTileSize();

		// Update the camera
		camera.update();
	}


	void updateGuards(Array<Guard> guards) {
        for (Guard guard : guards) {
            moveGuard(guard);
        }
	}


	void moveGuard(Guard guard) {
        Vector2 direction = guard.getMovementDirection();
        if(direction == null){ //ideally should never be null.
            System.out.println("Guard direction is null");
            return;
        }

		if (direction.len() > 0) {
			direction.nor().scl(guard.getForce());
			if (guard.isMeowed()) {
				direction.scl(4.25f);
			} else if (guard.isCameraAlerted()) {
                direction.scl(6.0f);
            }
            else if (guard.isAgroed()) {
				direction.scl(7f);
			} else if (guard.isSus()) {
                direction.scl(6f);
            } else {
                direction.scl(5f);
            }


			guard.setMovement(direction.x, direction.y);
		}

		// Update the guard's orientation to face the direction of movement.
		Vector2 movement = guard.getMovementDirection();
//		if (movement.len2() > 0.0001f) { // Only update if there is significant movement
//			guard.setAngle(movement.angleRad() - (float) Math.PI/2);
//		}
		guard.applyForce();
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
			Obstacle projectile = level.getProjectile().getObstacle();

            Array<Guard> guards = level.getGuards();

			if ((o1 == projectile || o2 == projectile)) {
				Array<SecurityCamera> secCameras = level.getSecurityCameras();
				for (int i = 0; i < secCameras.size; i++) {
					SecurityCamera cam = secCameras.get(i);
					Obstacle camObstacle = cam.getObstacle();
					if (o1 == camObstacle || o2 == camObstacle) {
						cam.disable();
                        contact.setEnabled(false);
                        level.getProjectile().setShouldDestroy(true);
						break;
					}
				}
			}


            for(Guard guard : guards){
                Obstacle enemy = guard.getObstacle();
                if((o1 == cat && o2 == enemy) || (o2 == cat && o1 == enemy) || (o1 == oct && o2 == enemy) || (o2 == oct && o1 == enemy)){
                    setFailure(true);
                }
            }

            // CAT
            for(Key key : level.getKeys()) {
                if (key.isCollected()) { continue;} //skip if the key is not collected

                Obstacle keyObs = key.getObstacle();
                if ((o1 == cat && o2 == keyObs) || (o2 == cat && o1 == keyObs)) {
                    key.setCollected(true);
                    key.setOwner(AvatarType.CAT);
                    System.out.println("COLLISION");
                    level.getCat().assignKey(key);
                }
            }

            // OCTOPUS
            for(Key key : level.getKeys()) {
                if (key.isCollected()) { continue;} //skip if the key is not collected

                Obstacle keyObs = key.getObstacle();
                if ((o1 == oct && o2 == keyObs) || (o2 == oct && o1 == keyObs)) {
                    key.setCollected(true);
                    key.setOwner(AvatarType.OCTOPUS);
                    level.getOctopus().assignKey(key);
                }
            }

            ObjectMap<Door, Key> doors = level.getDoors();
            for(Door door : doors.keys()) {

                doors.get(door);
                Obstacle doorObs = door.getObstacle();
                Key rightKey = doors.get(door);

                if (o1 == doorObs || o2 == doorObs) {
                    Obstacle other = (o1 == doorObs) ? o2 : o1;

                    boolean canUnlock =
                        (other == cat   && level.getCat().getKeys().contains(rightKey, true))
                            || (other == oct  && level.getOctopus().getKeys().contains(rightKey, true));

                    if (canUnlock && rightKey.isCollected() && !rightKey.isUsed() && door.isLocked()) {
                        door.setUnlocking(true);
                    }
                }

            }

            // Handle exit collision (only if door is unlocked)
            if((o1 == cat && o2 == exit) || (o2 == cat && o1 == exit)){
                catArrived = true;
            }

            if((o1 == oct && o2 == exit) || (o2 == oct && o1 == exit)){
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

            ObjectMap<Door,Key> doors = level.getDoors();

            for(Door door : doors.keys()) {

                doors.get(door);
                Obstacle doorObs = door.getObstacle();
                Key rightKey = doors.get(door);

                if (o1 == doorObs || o2 == doorObs) {
                    Obstacle other = (o1 == doorObs) ? o2 : o1;

                    boolean canUnlock =
                        (other == cat   && level.getCat().getKeys().contains(rightKey, true))
                            || (other == oct  && level.getOctopus().getKeys().contains(rightKey, true));

                    if (canUnlock && rightKey.isCollected() && !rightKey.isUsed() && door.isLocked()) {
                        door.setUnlocking(false);
                    }
                }

                if(!door.isLocked()){
                    Vector2 doorPos = door.getObstacle().getPosition();
                    graph.getNode((int)doorPos.x, (int)doorPos.y).isWall = false;
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
     * Sets the levelID
     * */
    public void setCurrentLevel(int v){
        currentLevel = v;
    }
}
