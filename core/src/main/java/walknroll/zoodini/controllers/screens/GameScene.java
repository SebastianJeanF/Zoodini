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

import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import edu.cornell.gdiac.physics2.ObstacleData;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.util.PooledList;
import java.util.HashMap;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Timer;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.physics2.Obstacle;
import edu.cornell.gdiac.physics2.WheelObstacle;
import edu.cornell.gdiac.util.ScreenListener;
import walknroll.zoodini.GDXRoot;
import walknroll.zoodini.controllers.GuardAIController;
import walknroll.zoodini.controllers.InputController;
import walknroll.zoodini.controllers.SoundController;
import walknroll.zoodini.controllers.UIController;
import walknroll.zoodini.controllers.aitools.TileGraph;
import walknroll.zoodini.controllers.aitools.TileNode;
import walknroll.zoodini.models.GameLevel;
import walknroll.zoodini.models.entities.Avatar;
import walknroll.zoodini.models.entities.Cat;
import walknroll.zoodini.models.entities.Enemy;
import walknroll.zoodini.models.entities.Guard;
import walknroll.zoodini.models.entities.Octopus;
import walknroll.zoodini.models.entities.PlayableAvatar;
import walknroll.zoodini.models.entities.SecurityCamera;
import walknroll.zoodini.models.nonentities.Door;
import walknroll.zoodini.models.nonentities.Exit;
import walknroll.zoodini.models.nonentities.InkProjectile;
import walknroll.zoodini.models.nonentities.Key;
import walknroll.zoodini.models.nonentities.Vent;
import walknroll.zoodini.utils.Constants;
import walknroll.zoodini.utils.DebugPrinter;
import walknroll.zoodini.utils.VisionCone;
import walknroll.zoodini.utils.ZoodiniSprite;
import walknroll.zoodini.utils.enums.AvatarType;

/**
 * Gameplay controller for the game.
 *
 * This class does not have the Box2d world. That is stored inside of the
 * LevelModel object, as the world settings are determined by the JSON
 * file. However, the class does have all of the controller functionality,
 * including collision listeners for 2qthe active level.
 *
 * You will notice that asset loading is very different. It relies on the
 * singleton asset manager to manage the various assets.
 */
public class GameScene implements Screen, ContactListener, UIController.PauseMenuListener {
    /** How many frames after winning/losing do we continue? */
    public static final int EXIT_COUNT = 120;
    // ASSETS
    /** Need an ongoing reference to the asset directory */
    protected AssetDirectory directory;

    /** Value for current level */
    private int currentLevel;

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
    /** Tiled renderer */
    private TiledMapRenderer mapRenderer;

    // Win/lose related fields
    /** Whether or not this is an active controller */
    private boolean active;
    /** Whether we have completed this level */
    private boolean complete;
    /** Whether we have failed at this world (and need a reset) */
    private boolean failed;
    /** Countdown active for winning or losing */
    private int countdown;

    /** Constant scale used for player movement */
    private final float MOVEMENT_SCALE = 32f;

    // Camera movement fields
    private Vector2 cameraTargetPosition;
    private Vector2 cameraPreviousPosition;
    private float cameraTransitionTimer;
    private float cameraTransitionDuration;
    private boolean inCameraTransition;

    // Game Paused Menu
    private boolean gamePaused = false;

    /** Whether the game has been lost **/
    private boolean gameLost = false;

    private SoundController soundController;

    /** Caches */
    private Vector3 vec3tmp = new Vector3();
    private Vector2 vec2tmp = new Vector2();
    private Vector2 angleCache = new Vector2();
    private Vector2 vec2tmp2 = new Vector2();
    private Vector2 vec2tmp3 = new Vector2();

    /**
     * Sets whether the level is completed.
     *
     * If true, the level will advance after a countdown
     *
     * @param value whether the level is completed.
     */
    private boolean octopusArrived = false;

    private boolean catArrived = false;

    private boolean followModeActive = false;
    private final float FOLLOW_DISTANCE = 1f;

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
        map = new TmxMapLoader().load(directory.getEntry("levels", JsonValue.class).getString("" + this.currentLevel));
        level.populate(directory, map);
        level.getWorld().setContactListener(this);
        mapRenderer = new OrthogonalTiledMapRenderer(map);

        complete = false;
        failed = false;
        active = false;
        countdown = -1;

        camera = new OrthographicCamera();

        float NUM_TILES_WIDE = 18f;
        camera.setToOrtho(false, level.getTileSize() * NUM_TILES_WIDE,
                level.getTileSize() * NUM_TILES_WIDE * 720f / 1280f);

        // Initialize camera tracking variables
        cameraTargetPosition = new Vector2();
        cameraPreviousPosition = new Vector2();
        cameraTransitionTimer = 0;
        cameraTransitionDuration = directory.getEntry("constants", JsonValue.class)
                .getFloat("CAMERA_INTERPOLATION_DURATION");
        inCameraTransition = false;

        // UI Controller
        ui = new UIController(directory, level, batch);
        ui.setPauseMenuListener(this);

        graph = new TileGraph<>(map, false, 1);
        initializeAIControllers();

        setComplete(false);
        setFailure(false);

        soundController = SoundController.getInstance();
    }

    public int getCurrentLevel() {
        return currentLevel;
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
        followModeActive = false;

        setComplete(false);
        setFailure(false);
        countdown = -1;

        // map = new TmxMapLoader().load(directory.getEntry("levels",
        // JsonValue.class).getString("" + this.currentLevel));
        // Reload the json each time
        level.populate(directory, map);
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

    @Override
    public void onPauseStateChanged(boolean paused) {
        gamePaused = paused;
    }

    @Override
    public void onRestart() {
        reset();
    }

    @Override
    public void onReturnToMenu() {
        // Immediately stop music before transitioning to menu
        soundController.stopMusic();

        // Small delay before transition to ensure audio properly stops
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.exitScreen(GameScene.this, GDXRoot.EXIT_MENU);
                }
            }
        }, 0.1f);
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

        if (gameLost) {
            listener.exitScreen(this, GDXRoot.EXIT_LOSE);
            return false;
        }

        if (complete) {
            listener.exitScreen(this, GDXRoot.EXIT_WIN);
            return false;
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
        if (gamePaused) {
            return;
        }
        InputController input = InputController.getInstance();
        processPlayerAction(input, dt);
        level.update(dt); // collisions
        updateVisionCones(dt);
        updateGuardAI(dt);
        processNPCAction(dt);
        updateCamera(dt);

        ui.update(dt);
    }

    /**
     * Updates all vision cone detection in the game.
     * Delegates to specific functions for guards and security cameras.
     */
    public void updateVisionCones(float dt) {
        updateSecurityCameraVisionCones();
        updateGuardVisionCones(dt);
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

        mapRenderer.setView(camera);
        mapRenderer.render(); // divide this into layerwise rendering if you want

        level.draw(batch, camera);
        if (Constants.DEBUG) {
            graph.clearMarkedNodes();

            // For each guard, mark their target nodes for display
            guardToAIController.forEach((guard, controller) -> {
                graph.markWaypoints(guard.getPatrolPoints());
                Vector2 targetLocation = controller.getNextTargetLocation();
                if (targetLocation != null) {
                    graph.markPositionAsTarget(targetLocation);
                }
                Vector2 cameraTargetLocation = controller.getCameraAlertPosition();
                graph.markPositionAsTarget(cameraTargetLocation);
                Vector2 distractedTargetLocation = controller.getDistractPosition();
                graph.markPositionAsTarget(distractedTargetLocation);
            });

            graph.draw(batch, camera, level.getTileSize());
            InputController ic = InputController.getInstance();
            if (ic.didLeftClick()) {
                graph.markNearestTile(camera, ic.getAiming(), level.getTileSize());
            }
        }

        // Draw UI
        ui.draw(level);
    }

    /**
     * Dispose of all (non-static) resources allocated to this mode.
     */
    public void dispose() {
        level.dispose();
        level = null;
        ui.dispose();
    }

    public void initializeAIControllers() {
        Array<Guard> guards = level.getGuards();
        for (Guard g : guards) {
            GuardAIController aiController = new GuardAIController(g, level, graph);
            guardToAIController.put(g, aiController);
        }
    }

    // -----------------Helper Methods--------------------//

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
    }

    /**
     * Called when the Screen is resumed from a paused state.
     *
     * This is usually when it regains focus.
     */
    public void resume() {
    }

    /**
     * Called when this screen becomes the current screen for a Game.
     */
    public void show() {
        // Useless if called in outside animation loop
        active = true;
        // soundController.playMusic("game-music", true);
        soundController.stopMusic();

        // Then start the game music with a slight delay to prevent audio glitches
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                soundController.playMusic("game-music", true);
            }
        }, 0.1f);
    }

    /**
     * Called when this screen is no longer the current screen for a Game.
     */
    public void hide() {
        // Useless if called in outside animation loop
        active = false;
        soundController.stopMusic();
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

        boolean gameOver = countdown != -1;
        if (gameOver)
            return;

        try {
            Object o1 = body1.getUserData();
            Object o2 = body2.getUserData();

            // projectile-enemy collision
            if (o1 instanceof InkProjectile && o2 instanceof SecurityCamera cam) {
                cam.disable();
                contact.setEnabled(false);
            }

            if (o2 instanceof InkProjectile && o1 instanceof SecurityCamera cam) {
                cam.disable();
                contact.setEnabled(false);
            }

            if (o1 instanceof InkProjectile && o2 instanceof Guard g) {
                applyInkEffect(g);
            }

            if (o2 instanceof InkProjectile && o1 instanceof Guard g) {
                applyInkEffect(g);
            }

            if (o1 instanceof InkProjectile || o2 instanceof InkProjectile) {
                level.getProjectile().setShouldDestroy(true);
            }

            // player-guard collision
            if (((o1 instanceof Cat && o2 instanceof Guard) || (o2 instanceof Cat && o1 instanceof Guard))
                    && !level.getCat().isInvincible()) {
                setFailure(true);
                gameLost = true;
            }

            if (((o1 instanceof Octopus && o2 instanceof Guard) || (o2 instanceof Octopus && o1 instanceof Guard))
                    && !level.getOctopus().isInvincible()) {
                setFailure(true);
                gameLost = true;
            }

            // player-key collision
            if (o1 instanceof Key key && o2 instanceof Cat cat) {
                if (!key.isCollected()) {
                    key.setCollected(true);
                    key.setOwner(cat.getAvatarType());
                    cat.assignKey(key);
                    cat.increaseNumKeys();
                }
            }

            if (o2 instanceof Key key && o1 instanceof Cat cat) {
                if (!key.isCollected()) {
                    key.setCollected(true);
                    key.setOwner(cat.getAvatarType());
                    cat.assignKey(key);
                    cat.increaseNumKeys();
                }
            }

            if (o1 instanceof Key key && o2 instanceof Octopus oct) {
                if (!key.isCollected()) {
                    key.setCollected(true);
                    key.setOwner(oct.getAvatarType());
                    oct.assignKey(key);
                    oct.increaseNumKeys();
                }
            }

            if (o2 instanceof Key key && o1 instanceof Octopus oct) {
                if (!key.isCollected()) {
                    key.setCollected(true);
                    key.setOwner(oct.getAvatarType());
                    oct.assignKey(key);
                    oct.increaseNumKeys();
                }
            }

            // avatar-door collision
            if (o1 instanceof Door door && o2 instanceof Cat cat) {
                if (door.isLocked() && cat.getNumKeys() > 0) {
                    door.setUnlocking(true);
                    door.setUnlocker(cat);
                }
            }

            if (o2 instanceof Door door && o1 instanceof Cat cat) {
                if (door.isLocked() && cat.getNumKeys() > 0) {
                    door.setUnlocking(true);
                    door.setUnlocker(cat);
                }
            }

            if (o1 instanceof Door door && o2 instanceof Octopus oct) {
                if (door.isLocked() && oct.getNumKeys() > 0) {
                    door.setUnlocking(true);
                    door.setUnlocker(oct);
                }
            }

            if (o2 instanceof Door door && o1 instanceof Octopus oct) {
                if (door.isLocked() && oct.getNumKeys() > 0) {
                    door.setUnlocking(true);
                    door.setUnlocker(oct);
                }
            }

            // Avatar-vent collision
            if (o1 instanceof Vent vent && o2 instanceof Cat cat) {
                vent.setOpen(false);
                vent.setContainedEntities(vent.getContainedEntities() + 1);
                cat.setInvincible(true);
                cat.setDrawingEnabled(false);
            }

            if (o2 instanceof Vent vent && o1 instanceof Cat cat) {
                vent.setOpen(false);
                vent.setContainedEntities(vent.getContainedEntities() + 1);
                cat.setInvincible(true);
                cat.setDrawingEnabled(false);
            }

            if (o1 instanceof Vent vent && o2 instanceof Octopus oct) {
                vent.setOpen(false);
                vent.setContainedEntities(vent.getContainedEntities() + 1);
                oct.setInvincible(true);
                oct.setDrawingEnabled(false);
            }

            if (o2 instanceof Vent vent && o1 instanceof Octopus oct) {
                vent.setOpen(false);
                vent.setContainedEntities(vent.getContainedEntities() + 1);
                oct.setInvincible(true);
                oct.setDrawingEnabled(false);
            }

            // Avatar-exit collision
            if ((o1 instanceof Cat && o2 instanceof Exit) || (o2 instanceof Cat && o1 instanceof Exit)) {
                catArrived = true;
                checkWinCondition();
            }

            if ((o1 instanceof Octopus && o2 instanceof Exit) || (o2 instanceof Octopus && o1 instanceof Exit)) {
                octopusArrived = true;
                checkWinCondition();
            }

            if (catArrived && octopusArrived && !failed) {
                setComplete(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkWinCondition() {
        if (!failed) {
            boolean levelComplete = false;

            if (level.isCatPresent() && level.isOctopusPresent()) {
                // Both characters present - need both to reach exit
                levelComplete = catArrived && octopusArrived;
            } else if (level.isCatPresent()) {
                // Only cat present
                levelComplete = catArrived;
            } else if (level.isOctopusPresent()) {
                // Only octopus present
                levelComplete = octopusArrived;
            }

            if (levelComplete) {
                setComplete(true);
            }
        }
    }

    /** Unused ContactListener method */
    public void endContact(Contact contact) {
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        try {

            Object o1 = body1.getUserData();
            Object o2 = body2.getUserData();

            if (o1 instanceof Door door && o2 instanceof Cat cat) {
                if (door.isLocked() && cat.getNumKeys() > 0) {
                    door.setUnlocking(false);
                }
            }

            if (o2 instanceof Door door && o1 instanceof Cat cat) {
                if (door.isLocked() && cat.getNumKeys() > 0) {
                    door.setUnlocking(false);
                }
            }

            if (o1 instanceof Door door && o2 instanceof Octopus oct) {
                if (door.isLocked() && oct.getNumKeys() > 0) {
                    door.setUnlocking(false);
                }
            }

            if (o2 instanceof Door door && o1 instanceof Octopus oct) {
                if (door.isLocked() && oct.getNumKeys() > 0) {
                    door.setUnlocking(false);
                }
            }

            if (o1 instanceof Vent vent && o2 instanceof Cat cat) {
                vent.setContainedEntities(vent.getContainedEntities() - 1);
                cat.setInvincible(false);
                cat.setDrawingEnabled(true);
            }

            if (o2 instanceof Vent vent && o1 instanceof Cat cat) {
                vent.setContainedEntities(vent.getContainedEntities() - 1);
                cat.setInvincible(false);
                cat.setDrawingEnabled(true);
            }

            if (o1 instanceof Vent vent && o2 instanceof Octopus oct) {
                vent.setContainedEntities(vent.getContainedEntities() - 1);
                oct.setInvincible(false);
                oct.setDrawingEnabled(true);
            }

            if (o2 instanceof Vent vent && o1 instanceof Octopus oct) {
                vent.setContainedEntities(vent.getContainedEntities() - 1);
                oct.setInvincible(false);
                oct.setDrawingEnabled(true);
            }

            if (o1 instanceof Cat && o2 instanceof Exit) {
                catArrived = false;
            }

            if (o2 instanceof Cat && o1 instanceof Exit) {
                catArrived = false;
            }

            if (o1 instanceof Octopus && o2 instanceof Exit) {
                catArrived = false;
            }

            if (o2 instanceof Octopus && o1 instanceof Exit) {
                catArrived = false;
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
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        try {
            Object o1 = body1.getUserData();
            Object o2 = body2.getUserData();

            if (Constants.INVINCIBLE) {
                contact.setEnabled(false);
            }

            if (((o1 instanceof Cat && o2 instanceof Guard) || (o2 instanceof Cat && o1 instanceof Guard))
                    && level.getCat().isInvincible()) {
                contact.setEnabled(false);
            }

            if (((o1 instanceof Octopus && o2 instanceof Guard) || (o2 instanceof Octopus && o1 instanceof Guard))
                    && level.getOctopus().isInvincible()) {
                contact.setEnabled(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
     */
    public void setCurrentLevel(int v) {
        currentLevel = v;
    }

    void updateGuards(Array<Guard> guards) {
        for (Guard guard : guards) {
            moveGuard(guard);
        }
    }

    void moveGuard(Guard guard) {
        Vector2 direction = guard.getMovementDirection();
        if (direction == null) { // ideally should never be null.
            DebugPrinter.println("Guard direction is null");
            return;
        }

        if (direction.len() > 0) {
            // Use math formula cale the force based on the radius
            // This allows the radius to be changed, without affected
            float radius = ((WheelObstacle) guard.getObstacle()).getRadius();
            direction.nor().scl((float) (MOVEMENT_SCALE * Math.pow((radius / .5f), 2)));

            if (guard.isMeowed()) {
                direction.scl(guard.getDistractedForce());
            } else if (guard.isCameraAlerted()) {
                direction.scl(guard.getAlertedForce());
            } else if (guard.isAgroed()) {
                direction.scl(guard.getAgroedForce());
            } else if (guard.isSus()) {
                direction.scl(guard.getSusForce());
            } else {
                // if the guard is not in any special state, apply normal force
                direction.scl(guard.getForce());
            }

            // Regardless of any other guard states, lower speed
            // if the guard is inked
            if (guard.isInkBlinded()) {
                direction.scl(guard.getBlindedForceScale());
            }

            if (guard.isIdle()){
                direction.setZero();
            }

            guard.setMovement(direction.x, direction.y);
        }

        if (guard.isLookingAround()){
            // If the guard is looking around, set the movement direction to zero
            guard.setMovement(0, 0);
        }

        guard.applyForce();
    }

    private void updateSecurityCameraVisionCones() {
        vec2tmp2.set(0, 0);
        vec2tmp3.set(0, 0);
        ObjectMap<ZoodiniSprite, VisionCone> visions = level.getVisionConeMap();
        for (ObjectMap.Entry<ZoodiniSprite, VisionCone> entry : visions.entries()) {
            if (entry.key instanceof SecurityCamera && !((SecurityCamera) entry.key).isDisabled()) {
                Vector2 catPos = level.isCatPresent() ? level.getCat().getPosition() : vec2tmp2;
                Vector2 octPos = level.isOctopusPresent() ? level.getOctopus().getPosition() : vec2tmp3;
                if ((level.isCatPresent() && entry.value.contains(catPos))
                        || (level.isOctopusPresent() && entry.value.contains(octPos))) {

                    ((SecurityCamera) entry.key).activateRing();

                    PlayableAvatar detectedPlayer = entry.value.contains(catPos) ? level.getCat() : level.getOctopus();

                    detectedPlayer.setUnderCamera(true);

                    for (Guard guard : level.getGuards()) {
                        float guardToCameraDistance = guard.getPosition()
                                .dst(((SecurityCamera) entry.key).getPosition());
                        if (guardToCameraDistance <= ((SecurityCamera) entry.key).getAlarmDistance()) {
                            guard.setAggroTarget(detectedPlayer);
                            guard.setCameraAlerted(true);

                            // Optionally set target position directly if needed
                            guard.setTarget(detectedPlayer.getPosition());
                        }
                    }
                } else {
                    if (level.isCatPresent()) {
                        level.getCat().setUnderCamera(false);
                    }
                    if (level.isOctopusPresent()) {
                        level.getOctopus().setUnderCamera(false);
                    }
                }
            }
        }
    }

    /**
     * Checks if the player is in the vision cones of any guards and updates their
     * states accordingly.
     * This function specifically handles guard vision detection, separate from
     * security cameras.
     */
    private void updateGuardVisionCones(float dt) {
        vec2tmp2.set(0, 0);
        vec2tmp3.set(0, 0);
        ObjectMap<ZoodiniSprite, VisionCone> visions = level.getVisionConeMap();

        for (ObjectMap.Entry<ZoodiniSprite, VisionCone> entry : visions.entries()) {
            // Skip if not a guard
            if (!(entry.key instanceof Guard)) {
                continue;
            }

            Guard guard = (Guard) entry.key;
            VisionCone visionCone = entry.value;

            visionCone.setRadius(guard.getViewDistance());
            visionCone.setWideness(guard.getFov());

            Vector2 movementDir = guard.isIdle() ? new Vector2(0, -1) : guard.getMovementDirection();
            visionCone.updateFacingDirection(dt, movementDir);

            Vector2 catPos = level.isCatPresent() ? level.getCat().getPosition() : vec2tmp2;
            Vector2 octPos = level.isOctopusPresent() ? level.getOctopus().getPosition() : vec2tmp3;

            // Check if cat is detected
            if (level.isCatPresent() && visionCone.contains(catPos) && !level.getCat().isInvincible()) {
                guard.setAgroed(true);
                guard.setAggroTarget(level.getCat());
                guard.setTarget(level.getCat().getPosition());
                level.getCat().setUnderVisionCone(true);
                guard.setSeesPlayer(true);
                guard.setSeenPlayer(level.getCat());
                // DebugPrinter.println("Guard detected cat: " + guard.getAggroTarget());
            }

            // Check if octopus is detected
            else if (level.isOctopusPresent() && visionCone.contains(octPos) && !level.getOctopus().isInvincible()) {
                guard.setAgroed(true);
                guard.setAggroTarget(level.getOctopus());
                guard.setTarget(level.getOctopus().getPosition());
                level.getOctopus().setUnderVisionCone(true);
                guard.setSeesPlayer(true);
                guard.setSeenPlayer(level.getOctopus());
                // DebugPrinter.println("Guard detected octopus: " + guard.getAggroTarget());
            }
            // No player detected
            else {
                // Only set to false if the guard isn't being alerted by a camera
                if (level.isOctopusPresent()) {
                    level.getOctopus().setUnderVisionCone(false);
                }
                if (level.isCatPresent()) {
                    level.getCat().setUnderVisionCone(false);
                }
                guard.setSeesPlayer(false);
                guard.setSeenPlayer(null);
                if (!guard.isCameraAlerted()) {
                    guard.setAgroed(false);
                }
            }
        }
    }

    /**
     * Applies movement forces to the avatar and change firing states.
     */
    private void processPlayerAction(InputController input, float dt) {
        vec3tmp.setZero();
        vec2tmp.setZero();

        if (input.didSwap()) {
            onSwap(input);
        }

        Avatar avatar = level.getAvatar();
        float vertical = input.getVertical();
        float horizontal = input.getHorizontal();
        if (avatar != level.getInactiveAvatar()) {
            moveAvatar(vertical, horizontal, avatar);
        }
        handleFollowModeToggle(input);
        updateFollowMode();
        if (level.isOctopusPresent()) {
            level.getOctopus().regenerateInk(dt);
        }

        if (avatar.getAvatarType() == AvatarType.OCTOPUS) {
            Octopus octopus = (Octopus) avatar;
            vec3tmp.set(input.getAiming(), 0);
            vec3tmp = camera.unproject(vec3tmp);
            vec2tmp.set(vec3tmp.x, vec3tmp.y)
                    .scl(1.0f / level.getTileSize())
                    .sub(octopus.getPosition())
                    .clamp(0.0f, octopus.getAbilityRange()); // this decides the distance for projectile to travel
            octopus.setTarget(vec2tmp); // set a target vector relative to octopus's position as origin.

            if (input.didAbility() && octopus.canUseAbility()) { // check for ink resource here.
                octopus.setCurrentlyAiming(!octopus.isCurrentlyAiming()); // turn the reticle on and off
            }

            if (octopus.isCurrentlyAiming() && input.didLeftClick()) {
                octopus.setDidFire(true);
                octopus.setCurrentlyAiming(false);
                octopus.consumeInk();
            } else {
                octopus.setDidFire(false);
            }

        } else if (avatar.getAvatarType() == AvatarType.CAT) {
            Cat cat = (Cat) avatar;

            if (input.isAbilityHeld() && cat.canUseAbility()) {
                cat.setCurrentlyAiming(true);
            }

            if (cat.isCurrentlyAiming() && !input.isAbilityHeld()) {
                cat.setDidFire(true);
                cat.setCurrentlyAiming(false);
                soundController.playCatMeow();
            } else {
                cat.setDidFire(false);
            }
        }


        vec3tmp.setZero();
        vec2tmp.setZero();
    }

    /**
     * Applies movement forces to NPCs.
     * Does NOT modify internal states of the NPCs. That is the
     * responsibility of ContactListener
     */
    private void processNPCAction(float dt) {
        Octopus octopus = level.getOctopus();
        InkProjectile inkProjectile = level.getProjectile();
        PooledList<Door> doors = level.getDoors();

        // Projectiles
        // TODO: not sure about the order of if statements here.
        if (inkProjectile.getShouldDestroy()) {
            inkProjectile.destroy();
        }

        if (level.isOctopusPresent() && octopus.didFire()) {
            activateInkProjectile(inkProjectile, octopus.getPosition(), octopus.getTarget());
        }

        if (level.isOctopusPresent()
                && inkProjectile.getPosition().dst(inkProjectile.getStartPosition()) > octopus.getAbilityRange()) {
            inkProjectile.setShouldDestroy(true);
        }

        Array<Guard> guards = level.getGuards();
        updateGuards(guards);

        // TODO: Might need to comment out again
        for (Door door : level.getDoors()) {
            if (!door.isLocked()) {
                Vector2 doorPos = door.getObstacle().getPosition();
                graph.getNode((int) doorPos.x, (int) doorPos.y).isObstacle = false;
            }

            if (!door.isUnlocking()) {
                door.resetTimer();
            }
        }

        for (Vent vent : level.getVents()) {
            if (vent.getContainedEntities() <= 0) {
                vent.setOpen(true);
            } else {
                vent.setOpen(false);
            }
        }
    }

    private void updateGuardAI(float dt) {
        guardToAIController.forEach((guard, controller) -> {
            controller.update(dt);
            guard.think(controller.getMovementDirection(), controller.getNextTargetLocation());
        });
    }

    private void resetAvatarState(PlayableAvatar avatar) {
        avatar.setCurrentlyAiming(false);
        avatar.resetPhysics();
    }

    private void onSwap(InputController input) {
        if (input.didSwap() && !inCameraTransition && level.isCatPresent() && level.isOctopusPresent()) {
            resetAvatarState(level.getAvatar());
            resetAvatarState(level.getInactiveAvatar());
            // Save previous camera position before swapping
            cameraPreviousPosition.set(cameraTargetPosition);

            // swap the active character
            level.swapActiveAvatar();

            // Start camera transition
            cameraTransitionTimer = 0;
            inCameraTransition = true;
        }
    }

    /**
     * @param verticalForce
     * @param horizontalForce
     * @param avatar
     */
    private void moveAvatar(float verticalForce, float horizontalForce, Avatar avatar) {
        // Rotate the avatar to face the direction of movement
        angleCache.set(horizontalForce, verticalForce);
        if (angleCache.len2() > 0.0f) {
            // Prevent faster movement when going diagonallyd
            if (angleCache.len() > 1.0f) {
                angleCache.nor();
            }

            float angle = angleCache.angleDeg();
            // Convert to radians with up as 0
            angle = (float) Math.PI * (angle - 90.0f) / 180.0f;
            avatar.getObstacle().setAngle(angle);
        }
        float radius = ((WheelObstacle) avatar.getObstacle()).getRadius();

        angleCache.scl(avatar.getForce())
                .scl(MOVEMENT_SCALE)
                .scl((float) Math.pow((radius / .4f), 2)); // Scale the force based on the radius

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
        PlayableAvatar avatar = level.getAvatar();
        if (avatar.isCurrentlyAiming()) {
            camera.zoom = Math.min(1.2f, camera.zoom + 0.01f);
        } else {
            camera.zoom = Math.max(1.0f, camera.zoom - 0.005f);
        }

        cameraTargetPosition.set(avatar.getPosition());

        // Get viewport dimensions in world units
        float viewWidth = camera.viewportWidth / level.getTileSize();
        float viewHeight = camera.viewportHeight / level.getTileSize();

        // Calculate soft boundaries that allow partial dead space
        float minX = level.getBounds().x + (viewWidth * 0.5f * camera.zoom);
        float maxX = level.getBounds().x + (level.getBounds().width) - (viewWidth * 0.5f * camera.zoom);
        float minY = level.getBounds().y + (viewHeight * 0.5f * camera.zoom);
        float maxY = level.getBounds().y + (level.getBounds().height) - (viewHeight * 0.5f * camera.zoom);

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

    private void applyInkEffect(Guard guard) {
        DebugPrinter.println("Guard hit by ink!");
        // Set ink effect duration (in seconds)
        final float INK_EFFECT_DURATION = 5.0f;

        // Store original vision parameters and apply reduction
        guard.setInkBlinded(true);
        guard.setInkBlindTimer(INK_EFFECT_DURATION);

        final float MIN_VIEW_DISTANCE = 2f;
        final float MIN_FOV = 40f;

        // Reduce the view distance and FOV angle with minimum thresholds
        float reducedViewDistance = Math.max(guard.getViewDistance() * 0.6f, MIN_VIEW_DISTANCE);
        float reducedFov = Math.max(guard.getFov() * 0.6f, MIN_FOV);
        // Reduce the view distance and FOV angle
        guard.setTempViewDistance(reducedViewDistance); // 60% reduction
        guard.setTempFov(reducedFov); // 60% reduction
    }

    private void updateFollowMode() {
        if (followModeActive && level.getInactiveAvatar() != null && level.getAvatar() != null) {
            PlayableAvatar activeAvatar = level.getAvatar();
            PlayableAvatar inactiveAvatar = level.getInactiveAvatar();

            Vector2 activePos = activeAvatar.getPosition();
            Vector2 inactivePos = inactiveAvatar.getPosition();

            Vector2 direction = new Vector2(activePos).sub(inactivePos);
            float distance = direction.len();

            float FOLLOW_BUFFER = 0.1f;
            if (distance > FOLLOW_DISTANCE + FOLLOW_BUFFER) {
                direction.nor();
                moveAvatar(direction.y * 0.75f, direction.x * 0.75f, inactiveAvatar);
            }
            else if (distance > FOLLOW_DISTANCE - FOLLOW_BUFFER) {
                direction.nor();
                float speedFactor = (distance - (FOLLOW_DISTANCE - FOLLOW_BUFFER)) / (2 * FOLLOW_BUFFER);
                speedFactor = Math.max(0.1f, speedFactor) * 0.75f;
                moveAvatar(direction.y * speedFactor, direction.x * speedFactor, inactiveAvatar);
            }
            else {
                inactiveAvatar.setMovement(0, 0);
                inactiveAvatar.applyForce();
            }
        } else if (!followModeActive && level.getInactiveAvatar() != null) {
            PlayableAvatar inactiveAvatar = level.getInactiveAvatar();
            inactiveAvatar.setMovement(0, 0);
            inactiveAvatar.applyForce();
        }
    }

    private void handleFollowModeToggle(InputController input) {
        if (input.didPressFollowMode()) {
            followModeActive = !followModeActive;
        }
    }

}
