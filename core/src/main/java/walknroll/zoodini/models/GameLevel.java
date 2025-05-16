/*
 * GameLevel.java
 *
 */

package walknroll.zoodini.models;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.text.StringSubstitutor;

import com.badlogic.gdx.Input;
//import com.badlogic.gdx.maps.objects.TextMapObject;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.EllipseMapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.objects.TextureMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Ellipse;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.SpriteSheet;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.math.PathExtruder;
import edu.cornell.gdiac.math.PathFactory;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.Obstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.util.PooledList;
import walknroll.zoodini.controllers.InputController;
import walknroll.zoodini.controllers.UIController;
import walknroll.zoodini.models.entities.Avatar;
import walknroll.zoodini.models.entities.Cat;
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
import walknroll.zoodini.utils.animation.AnimationState;
import walknroll.zoodini.utils.enums.AvatarType;
import walknroll.zoodini.utils.enums.ExitAnimal;

/**
 * Represents a single level in our game
 * <p>
 * Note that the constructor does very little. The true initialization happens
 * by reading the JSON value. To reset a level, dispose it and reread the JSON.
 * <p>
 * The level contains its own Box2d World, as the World settings are defined by
 * the JSON file. There is generally no controller code in this class, except
 * for the update method for moving ahead one timestep. All of the other methods
 * are getters and setters. The getters allow the GameScene class to modify the
 * level elements.
 */
public class GameLevel {
    /**
     * Number of velocity iterations for the constrain solvers
     */
    public static final int WORLD_VELOC = 6;

    /**
     * Number of position iterations for the constrain solvers
     */
    public static final int WORLD_POSIT = 2;
    // private LOSController losController;

    /**
     * Returns a string equivalent to the sequence of bits in s
     * <p>
     * This function assumes that s is a string of 0s and 1s of length < 16.
     * This function allows the JSON file to specify bit arrays in a readable
     * format.
     *
     * @param s the string representation of the bit array
     * @return a string equivalent to the sequence of bits in s
     */
    public static short bitStringToShort(String s) {
        short value = 0;
        short pos = 1;
        for (int ii = s.length() - 1; ii >= 0; ii--) {
            if (s.charAt(ii) == '1') {
                value += pos;
            }
            pos *= 2;
        }
        return value;
    }

    /**
     * Returns a string equivalent to the COMPLEMENT of bits in s
     * <p>
     * This function assumes that s is a string of 0s and 1s of length < 16.
     * This function allows the JSON file to specify exclusion bit arrays (for
     * masking) in a readable format.
     *
     * @param s the string representation of the bit array
     * @return a string equivalent to the COMPLEMENT of bits in s
     */
    public static short bitStringToComplement(String s) {
        short value = 0;
        short pos = 1;
        for (int ii = s.length() - 1; ii >= 0; ii--) {
            if (s.charAt(ii) == '0') {
                value += pos;
            }
            pos *= 2;
        }
        return value;
    }

    /**
     * Whether or not the level is in debug more (showing off physics)
     */
    private boolean debug;
    // Physics objects for the game
    /**
     * Reference to the cat avatar
     */
    private Cat avatarCat;

    /**
     * Reference to the octopus avatar
     */
    private Octopus avatarOctopus;

    /**
     * Whether the currently active avatar is the cat. Otherwise, it's the octopus
     */
    private boolean catActive;

    /**
     * Reference to the exit (for collision detection)
     */
    private Exit exit;
    private Array<Guard> guards = new Array<>();
    private Array<SecurityCamera> securityCameras = new Array<>();
    private Array<Vent> vents = new Array<>();

    private ObjectMap<ZoodiniSprite, VisionCone> visions = new ObjectMap<>();
    private InkProjectile inkProjectile; // ink projectile (there should only ever be one!!!)

    private Array<Key> keys = new Array<>();

    private PooledList<Door> doors = new PooledList<>();

    /**
     * All the object sprites in the world.
     */
    protected PooledList<ZoodiniSprite> sprites = new PooledList<ZoodiniSprite>();
    /**
     * All the objects in the world.
     */
    protected PooledList<Obstacle> objects = new PooledList<>();
    /**
     * The Box2D world
     */
    protected World world;

    /**
     * The boundary of the world
     */
    protected Rectangle bounds;

    /**
     * Size of one tile. This serves as scaling factor for all drawings
     */
    private float units;
    // TO FIX THE TIMESTEP
    /**
     * The maximum frames per second setting for this level
     */
    protected int maxFPS;
    /**
     * The minimum frames per second setting for this level
     */
    protected int minFPS;
    /**
     * The amount of time in to cover a single animation frame
     */
    protected float timeStep;
    /**
     * The maximum number of steps allowed before moving physics forward
     */
    protected float maxSteps;

    /**
     * The maximum amount of time allowed in a frame
     */
    protected float maxTimePerFrame;

    /**
     * The amount of time that has passed without updating the frame
     */
    protected float physicsTimeLeft;

    Affine2 affineCache = new Affine2();

    private boolean catPresent;
    private boolean octopusPresent;

    // TODO: Write specification for these
    // TODO: Make a textController lol
    private Array<MapObject> textObjects = new Array<>();
    private BitmapFont textFont;
    private GlyphLayout layout = new GlyphLayout();
    private float textMaxYOffsetTile;
    private float textCurrYOffsetTile;
    private float textCycleTimeSec;
    private boolean textMovingUp;
    private float textPhase; // [0 .. halfCycle]
    private StringSubstitutor substitutor;

    /**
     * The map renderer for this level
     */
    private OrthogonalTiledMapRenderer mapRenderer;

    /** Array that contains image objects */
    private Array<TextureMapObject> imagesCache;

    /**
     * Creates a new GameLevel
     * <p>
     * The level is empty and there is no active physics world. You must read
     * the JSON file to initialize the level
     */
    public GameLevel() {
        world = null;
        bounds = new Rectangle(0, 0, 1, 1);
        debug = Constants.DEBUG;
        catActive = true;

        InputController ic = InputController.getInstance();
        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("swapKey", Input.Keys.toString(ic.getSwapKey()));
        valuesMap.put("abilityKey", Input.Keys.toString(ic.getAbilityKey()));
        valuesMap.put("followKey", Input.Keys.toString(ic.getFollowModeKey()));
        substitutor = new StringSubstitutor(valuesMap);
    }

    public ObjectMap<ZoodiniSprite, VisionCone> getVisionConeMap() {
        return visions;
    }

    /**
     * Lays out the game geography from the given JSON file
     *
     * @param directory the asset manager
     */
    public void populate(AssetDirectory directory, TiledMap map, SpriteBatch batch) {
        // Compute the FPS
        int[] fps = { 20, 60 };
        maxFPS = fps[1];
        minFPS = fps[0];
        timeStep = 1.0f / maxFPS;
        maxSteps = 1.0f + (float) maxFPS / minFPS;
        maxTimePerFrame = timeStep * maxSteps;

        world = new World(Vector2.Zero, false);

        mapRenderer = new OrthogonalTiledMapRenderer(map, batch);
        MapLayer l =  map.getLayers().get("images");
        if(l != null){
            MapObjects objs =l.getObjects();
            imagesCache = new Array<>(objs.getCount());
            for(MapObject obj : objs){
                if(obj instanceof TextureMapObject t) {
                    imagesCache.add(t);
                }
            }
            imagesCache.sort((a,b) -> Float.compare(b.getY(), a.getY())); //descending order
        }

        MapProperties props = map.getProperties();
        int width = props.get("width", Integer.class);
        int height = props.get("height", Integer.class);
        units = props.get("tilewidth", Integer.class);
        bounds = new Rectangle(0, 0, width, height);

        MapLayer walls = map.getLayers().get("walls");
        JsonValue entityConstants = directory.getEntry("constants", JsonValue.class).get("entities");
        createWallBodies(walls, entityConstants.get("walls"));



        // Text
        textFont = directory.getEntry("game-text", BitmapFont.class);
        textPhase = 0f;
        JsonValue textConstants = directory.getEntry("constants", JsonValue.class).get("gameText");
        textCycleTimeSec = textConstants.getFloat("cycleTimeSec");
        textMaxYOffsetTile = textConstants.getFloat("maxYOffsetTile");
        textCurrYOffsetTile = 0f;
        textMovingUp = true;

        catPresent = false;
        octopusPresent = false;

        MapLayer objectLayer = map.getLayers().get("objects");
        for (MapObject obj : objectLayer.getObjects()) {
            MapProperties properties = obj.getProperties();
            String type = properties.get("type", String.class);

            if ("Cat".equalsIgnoreCase(type)) {
                DebugPrinter.println("Creating cat");
                avatarCat = new Cat(properties, entityConstants.get("cat"), units);
                avatarCat.setAnimation(AnimationState.IDLE,
                        directory.getEntry("cat-idle.animation", SpriteSheet.class), 15);
                avatarCat.setAnimation(AnimationState.WALK,
                        directory.getEntry("cat-walk.animation", SpriteSheet.class), 4);
                avatarCat.setAnimation(AnimationState.WALK_DOWN,
                        directory.getEntry("cat-walk-down.animation", SpriteSheet.class), 8);
                avatarCat.setAnimation(AnimationState.WALK_UP,
                        directory.getEntry("cat-walk-up.animation", SpriteSheet.class), 6);
                activate(avatarCat);
                catPresent = true;
            } else if ("Octopus".equalsIgnoreCase(type)) {
                DebugPrinter.println("Creating octopus");
                avatarOctopus = new Octopus(properties, entityConstants.get("octopus"), units);
                avatarOctopus.setAnimation(AnimationState.IDLE,
                        directory.getEntry("octopus-idle.animation", SpriteSheet.class), 7);
                avatarOctopus.setAnimation(AnimationState.WALK,
                        directory.getEntry("octopus-walk.animation", SpriteSheet.class), 6);
                avatarOctopus.setAnimation(AnimationState.WALK_DOWN,
                        directory.getEntry("octopus-walk-down.animation", SpriteSheet.class), 8);
                avatarOctopus.setAnimation(AnimationState.WALK_UP,
                        directory.getEntry("octopus-walk-up.animation", SpriteSheet.class), 6);
                activate(avatarOctopus);
                octopusPresent = true;
            } else if ("Guard".equalsIgnoreCase(type)) {
                DebugPrinter.println("Creating guard");
                Guard g = new Guard(properties, entityConstants.get("guard"), units);
                SpriteSheet idle = directory.getEntry("guard-idle-all.animation", SpriteSheet.class);
                idle = new SpriteSheet(idle);

                g.setAnimation(AnimationState.IDLE_NORTH, idle, 16 ,16 , 15);
                g.setAnimation(AnimationState.IDLE_LEFT, idle, 14 ,14 , 15);
                g.setAnimation(AnimationState.IDLE_SOUTH, idle, 9 ,13 , 20, true);
                g.setAnimation(AnimationState.IDLE_RIGHT, idle, 17, 17, 15);

                g.setAnimation(AnimationState.IDLE_NORTH_BLIND, idle, 6, 6, 15);
                g.setAnimation( AnimationState.IDLE_LEFT_BLIND, idle, 5, 5, 15);
                g.setAnimation(AnimationState.IDLE_SOUTH_BLIND, idle, 1, 3, 20, true);
                g.setAnimation(AnimationState.IDLE_RIGHT_BLIND, idle, 7, 8, 15);


                g.setAnimation(AnimationState.WALK,
                    new SpriteSheet(directory.getEntry("guard-walk.animation", SpriteSheet.class)), 8);
                g.setAnimation(AnimationState.WALK_DOWN,
                    new SpriteSheet(directory.getEntry("guard-walk-down.animation", SpriteSheet.class)), 8);
                g.setAnimation(AnimationState.WALK_UP,
                    new SpriteSheet(directory.getEntry("guard-walk-up.animation", SpriteSheet.class)), 8);
                g.setAnimation(AnimationState.WALK_DOWN_BLIND,
                    new SpriteSheet(directory.getEntry("guard-walk-down-inked.animation", SpriteSheet.class)), 8);
                g.setAnimation(AnimationState.WALK_BLIND,
                    new SpriteSheet(directory.getEntry("guard-walk-inked.animation", SpriteSheet.class)), 8);
                g.setAnimation(AnimationState.WALK_UP_BLIND,
                    new SpriteSheet(directory.getEntry("guard-walk-up-inked.animation", SpriteSheet.class)), 8);
                g.setSusMeter(new SpriteSheet(directory.getEntry("suspicion-meter.animation", SpriteSheet.class)));
                guards.add(g);
                activate(g);
            } else if ("Camera".equalsIgnoreCase(type)) {
                SecurityCamera cam = new SecurityCamera(properties, entityConstants.get("camera"), units);
                cam.setAnimation(AnimationState.IDLE, directory.getEntry("camera-idle.animation", SpriteSheet.class),
                        15);
                securityCameras.add(cam);
                activate(cam);
            } else if ("Door".equalsIgnoreCase(type)) {
                Door door = new Door(directory, properties, entityConstants.get("door"), units);
                doors.add(door);
                activate(door);
            } else if ("Key".equalsIgnoreCase(type)) {
                Key key = new Key(directory, properties, entityConstants.get("key"), units);
                keys.add(key);
                activate(key);
            } else if ("Exit".equalsIgnoreCase(type)) {
                ExitAnimal animalType = null;
                switch (properties.get("creature", "", String.class)) {
                    case "RABBIT" -> animalType = ExitAnimal.RABBIT;
                    case "PENGUIN" -> animalType = ExitAnimal.PENGUIN;
                    case "OCTOPUS" -> animalType = ExitAnimal.OCTOPUS;
                    default -> animalType = ExitAnimal.PANDA;
                }
                exit = new Exit(directory, properties, entityConstants.get("exit"), units, animalType);
                exit.create(directory);
                activate(exit);
            } else if ("Text".equalsIgnoreCase(type)) {
                textObjects.add(obj);
            } else if ("Vent".equalsIgnoreCase(type)) {
                Vent vent = new Vent(directory, properties, entityConstants.get("vent"), units);
                vents.add(vent);
                activate(vent);
            } else if ("Settings".equalsIgnoreCase(type)){
                Boolean disableMinimap = properties.get("disableMinimap", Boolean.class);
                if (disableMinimap != null && disableMinimap) {
                    UIController.disableMinimap(true);
                }
            }
        }

        if (catPresent) {
            catActive = true;
        } else if (octopusPresent) {
            catActive = false;
        }

        initializeVisionCones(entityConstants.get("visioncone"));
        // initializeLOSController(entityConstants.get("walls"));

        // Initialize an ink projectile (but do not add it to the physics world, we only
        // do that on demand)
        JsonValue projectileData = directory.getEntry("constants", JsonValue.class).get("entities").get("ink");
        inkProjectile = new InkProjectile(projectileData, units);
        SpriteSheet sheet1 = directory.getEntry("ink-explosion.animation", SpriteSheet.class);
        inkProjectile.setAnimation(AnimationState.EXPLODE, sheet1, sheet1.getSize() / 30);
        inkProjectile.setAnimation(AnimationState.IDLE,
                directory.getEntry("ink-projectile.animation", SpriteSheet.class), 5);
        activate(inkProjectile);
        inkProjectile.setDrawingEnabled(false);
        inkProjectile.getObstacle().setActive(false);
    }

    /**
     * Disposes of all resources for this model.
     * <p>
     * Because of all the heavy weight physics stuff, this method is absolutely
     * necessary whenever we reset a level.
     */
    public void dispose() {
        for (ObstacleSprite s : sprites) {
            s.getObstacle().deactivatePhysics(world);
        }

        visions.clear();
        guards.clear();
        securityCameras.clear();
        objects.clear();
        sprites.clear();
        doors.clear();
        textObjects.clear();
        if (imagesCache != null) {
            imagesCache.clear();
        }
        keys.clear();
        mapRenderer.dispose();
        vents.clear();
        if (world != null) {
            world.dispose();
            world = null;
        }
    }

    /**
     * Updates all of the models in the level.
     * <p>
     * This is borderline controller functionality. However, we have to do this
     * because
     * of how tightly coupled everything is.
     *
     * @param dt the time passed since the last frame
     */
    public void update(float dt) {
        if (fixedStep(dt)) {

            updateFlipSprite(getAvatar());
            PlayableAvatar inactiveAvatar = getInactiveAvatar();
            if (inactiveAvatar != null) {
                updateFlipSprite(inactiveAvatar);
            }

            if (avatarCat != null) {
                avatarCat.update(dt);
            }

            if (avatarOctopus != null) {
                avatarOctopus.update(dt);
            }

            if (inkProjectile != null) {
                inkProjectile.update(dt);
            }

            for (Guard g : guards) {
                g.update(dt);
                g.updateInkBlindTimer(dt);
                updateFlipGuardSprite(g);
            }

            for (SecurityCamera c : securityCameras) {
                c.update(dt);
            }

            for (VisionCone vc : visions.values()) {
                vc.update(world);
            }

            for (Door door : doors) {
                door.update(dt);
            }

            for (Key key : keys) {
                key.update(dt);
            }

            exit.update(dt);

            // checkPlayerInVisionCones();
            updateGameTextPosition(dt);
        }
    }

    /**
     * Checks if the player is in the vision cones of any guards or security cameras
     * and updates their states accordingly.
     */
    public void checkPlayerInVisionCones() {
        for (ObjectMap.Entry<ZoodiniSprite, VisionCone> entry : visions.entries()) {
            VisionCone v = entry.value;
            ZoodiniSprite key = entry.key;
            v.update(world);
            if (v.contains(avatarCat.getObstacle())) {
                if (key instanceof Guard) {
                    ((Guard) key).setTarget(
                            avatarCat.getPosition()); // TODO: this line might not be needed
                    ((Guard) key).setAgroed(true);
                    ((Guard) key).setAggroTarget(avatarCat);
                } else if (key instanceof SecurityCamera) {
                    SecurityCamera camera = (SecurityCamera) key;
                    if (!camera.isDisabled()) {
                        // Alert all guards when camera sees either player
                        for (Guard guard : getGuards()) {
                            if (guard != null) {
                                guard.setAggroTarget(avatarCat);
                                guard.setCameraAlerted(true);
                            }
                        }
                    }
                }
            } else if (v.contains(avatarOctopus.getObstacle())) {
                if (key instanceof Guard) {
                    ((Guard) key).setTarget(
                            avatarOctopus.getPosition()); // TODO: this line might not be needed
                    ((Guard) key).setAgroed(true);
                    ((Guard) key).setAggroTarget(avatarOctopus);
                    // DebugPrinter.println("In guard vision cone " + ((Guard)
                    // key).getAggroTarget());
                } else if (key instanceof SecurityCamera) {
                    SecurityCamera camera = (SecurityCamera) key;
                    if (!camera.isDisabled()) {
                        // Alert all guards when camera sees either player
                        for (Guard guard : guards) {
                            if (guard != null) {
                                guard.setTarget(avatarOctopus.getPosition());
                                guard.setCameraAlerted(true);
                            }
                        }
                    }
                }
            } else if (!v.contains(avatarCat.getObstacle())) {
                if (key instanceof Guard) {
                    ((Guard) key).setAgroed(false);
                }
            } else if (!v.contains(avatarOctopus.getObstacle())) {
                if (key instanceof Guard) {
                    ((Guard) key).setAgroed(false);
                }
            }
        }
    }

    /**
     * Draws the level to the given game canvas
     * <p>
     * If debug mode is true, it will outline all physics bodies as wireframes.
     * Otherwise it will only draw the sprite representations.
     *
     * @param batch  the sprite batch to draw to
     * @param camera the drawing camera
     */
    public void draw(SpriteBatch batch, Camera camera) {
        // Draw the sprites first (will be hidden by shadows)
        batch.begin(camera);
        batch.setColor(Color.WHITE);
        mapRenderer.setView((OrthographicCamera) camera);

        // Get ground layer and render it
        MapLayer groundLayer = mapRenderer.getMap().getLayers().get("ground");
        mapRenderer.renderTileLayer((TiledMapTileLayer) groundLayer);

        batch.setColor(Color.WHITE);
        MapLayer decorations = mapRenderer.getMap().getLayers().get("decorations");
        if(decorations == null){
            decorations = mapRenderer.getMap().getLayers().get("decoration");
        }
        if (decorations != null)
            mapRenderer.renderTileLayer((TiledMapTileLayer) decorations);

        sprites.sort(ZoodiniSprite.Comparison);
        for (ZoodiniSprite obj : sprites) {
            batch.setColor(Color.WHITE);
            if (obj.isDrawingEnabled()) {
                obj.draw(batch);
            }
            if (obj instanceof SecurityCamera cam) {
                if (!cam.isDisabled())
                    visions.get(obj).draw(batch, camera);
            }
        }

        batch.setColor(Color.WHITE);
        Avatar avatar = getAvatar();
        if (avatar != null) {
            if (avatar.getAvatarType() == AvatarType.OCTOPUS) {
                Octopus octopus = (Octopus) avatar;
                if (octopus.isCurrentlyAiming() && octopus.canUseAbility()) {
                    drawOctopusReticle(batch, octopus);
                    drawAbilityRange(batch, avatarOctopus);
                }
            }
            if (avatar.getAvatarType() == AvatarType.CAT) {
                Cat cat = (Cat) avatar;
                if (cat.isCurrentlyAiming() && cat.canUseAbility()) {
                    drawAbilityRange(batch, avatarCat);
                }
            }
        }
        if (Constants.CO_OP) {
            Avatar inactiveAvatar = getInactiveAvatar();
            if (inactiveAvatar != null && inactiveAvatar.getAvatarType() == AvatarType.OCTOPUS) {
                Octopus octopus = (Octopus) inactiveAvatar;
                if (octopus.isCurrentlyAiming() && octopus.canUseAbility()) {
                    drawOctopusReticle(batch, octopus);
                    drawAbilityRange(batch, avatarOctopus);
                }
            }
        }
        batch.setColor(Color.WHITE);
        for (ObjectMap.Entry<ZoodiniSprite, VisionCone> entry : visions.entries()) {
            if (entry.key instanceof Guard) {
                entry.value.draw(batch, camera);
            }
        }

        batch.setColor(Color.WHITE);
        // Get wall layer and render it
        MapLayer wallLayer = mapRenderer.getMap().getLayers().get("wall-tiles");
        if (wallLayer != null)
            mapRenderer.renderTileLayer((TiledMapTileLayer) wallLayer);

        batch.setColor(Color.WHITE);
        if(imagesCache != null) {
            for (TextureMapObject t : imagesCache) {
                batch.draw(t.getTextureRegion(), t.getX(), t.getY());
            }
        }


        // d debugging on top of everything.
        if (debug) {
            for (ObstacleSprite obj : sprites) {
                obj.drawDebug(batch);
            }
        }

        drawGameText(batch);

        batch.end();
    }

    // TODO: Finish specification
    // INVARIANT: Batch must be currently drawing
    // INVARIANT: Caller is responsible for ending the batch
    public void drawGameText(SpriteBatch batch) {

        // TODO: Figure out root cause of drawing text
        // preventing debug graph tiles from being drawn
        if (textFont == null
                || textObjects.size == 0
                || Constants.DEBUG)
            return;

        for (MapObject textObj : textObjects) {
            MapProperties props = textObj.getProperties();
            String text = substitutor.replace(props.get("text", String.class));
            if (text == null)
                continue;

            float x = props.get("x", Float.class) / units;
            float y = props.get("y", Float.class) / units;

            // Get font scale if specified
            float scale = props.get("scale", 1.1f, Float.class);

            // Save the original font scale and color
            float originalScaleX = textFont.getData().scaleX;
            float originalScaleY = textFont.getData().scaleY;

            // // Scale font based on tile size and custom scale
            // float fontScale = (units / 32f) * scale; // Assuming 32 pixels is the base
            // tile size
            textFont.getData().setScale(1);

            // Calculate position based on alignment
            layout.setText(textFont, text);
            float textX = x * units - layout.width / 2; // Centered by default
            float textY = (y * units + layout.height) + (textCurrYOffsetTile * units); // Adjust for baseline

            // Draw text
            textFont.draw(batch, text, textX, textY);

            // Restore original scale and color
            textFont.getData().setScale(originalScaleX, originalScaleY);
        }

    }

    public boolean isInactiveAvatarInDanger() {
        if (catActive && octopusPresent) {
            return isInDanger(avatarOctopus);
        } else if (!catActive && catPresent) {
            return isInDanger(avatarCat);
        }
        return false;
    }

    // ------------------Helpers-----------------------//

    public boolean isCatPresent() {
        return catPresent;
    }

    public boolean isOctopusPresent() {
        return octopusPresent;
    }

    public void swapActiveAvatar() {
        if (catPresent && octopusPresent) {
            catActive = !catActive;
        }
    }

    public PooledList<Obstacle> getObjects() {
        return objects;
    }

    public PooledList<ZoodiniSprite> getSprites() {
        return sprites;
    }

    /**
     * Returns a reference to the keys
     *
     * @return a reference to the keys
     */
    public Array<Key> getKeys() {
        return keys;
    }

    public Array<Vent> getVents() {
        return vents;
    }

    /**
     * Returns the bounding rectangle for the physics world
     * <p>
     * The size of the rectangle is in physics, coordinates, not screen coordinates
     *
     * @return the bounding rectangle for the physics world
     */
    public Rectangle getBounds() {
        return bounds;
    }

    /**
     * Returns a reference to the Box2D World
     *
     * @return a reference to the Box2D World
     */
    public World getWorld() {
        return world;
    }

    /**
     * Returns a reference to the player avatar
     *
     * @return a reference to the player avatar
     */
    public PlayableAvatar getAvatar() {
        if (catActive && catPresent) {
            return avatarCat;
        } else if (!catActive && octopusPresent) {
            return avatarOctopus;
        }
        return catPresent ? avatarCat : avatarOctopus;
    }

    public Cat getCat() {
        return avatarCat;
    }

    public Octopus getOctopus() {
        return avatarOctopus;
    }

    public PlayableAvatar getInactiveAvatar() {
        if (!catActive && catPresent) {
            return avatarCat;
        } else if (catActive && octopusPresent) {
            return avatarOctopus;
        }
        return null;
    }

    /**
     * Returns a reference to the doors
     *
     * @return a reference to the doors
     */
    public PooledList<Door> getDoors() {
        return doors;
    }

    /**
     * Returns a reference to the exit area
     *
     * @return a reference to the exit area
     */
    public Exit getExit() {
        return exit;
    }

    public Array<SecurityCamera> getSecurityCameras() {
        return securityCameras;
    }

    /**
     * Returns a reference to the enemies
     *
     * @return a reference to the enemies
     */
    public Array<Guard> getGuards() {
        return guards;
    }

    public InkProjectile getProjectile() {
        return inkProjectile;
    }

    /**
     * Returns whether this level is currently in debug node
     * <p>
     * If the level is in debug mode, then the physics bodies will all be drawn
     * as wireframes onscreen
     *
     * @return whether this level is currently in debug node
     */
    public boolean getDebug() {
        return debug;
    }

    /**
     * Sets whether this level is currently in debug node
     * <p>
     * If the level is in debug mode, then the physics bodies will all be drawn
     * as wireframes onscreen
     *
     * @param value whether this level is currently in debug node
     */
    public void setDebug(boolean value) {
        debug = value;
    }

    /**
     * Returns the maximum FPS supported by this level
     * <p>
     * This value is used by the rayhandler to fix the physics timestep.
     *
     * @return the maximum FPS supported by this level
     */
    public int getMaxFPS() {
        return maxFPS;
    }

    /**
     * Sets the maximum FPS supported by this level
     * <p>
     * This value is used by the rayhandler to fix the physics timestep.
     *
     * @param value the maximum FPS supported by this level
     */
    public void setMaxFPS(int value) {
        maxFPS = value;
    }

    /**
     * Returns the minimum FPS supported by this level
     * <p>
     * This value is used by the rayhandler to fix the physics timestep.
     *
     * @return the minimum FPS supported by this level
     */
    public int getMinFPS() {
        return minFPS;
    }

    /**
     * Sets the minimum FPS supported by this level
     * <p>
     * This value is used by the rayhandler to fix the physics timestep.
     *
     * @param value the minimum FPS supported by this level
     */
    public void setMinFPS(int value) {
        minFPS = value;
    }

    public float getTileSize() {
        return units;
    }

    /**
     * Immediately adds the object to the physics world
     *
     * @param sprite The object to add
     */
    protected void activate(ZoodiniSprite sprite) {
        assert inBounds(sprite.getObstacle()) : "Object is not in bounds";
        sprites.add(sprite);
        objects.add(sprite.getObstacle());
        sprite.getObstacle().activatePhysics(world);
    }

    private void initializeVisionCones(JsonValue constants) {
        Color c = Color.WHITE.cpy().add(0, 0, 0, -0.5f);
        for (SecurityCamera cam : securityCameras) {
            float fov = cam.getFov();
            float dist = cam.getViewDistance();
            VisionCone vc = new VisionCone(60, Vector2.Zero, dist, 0.0f, fov, c, units, constants);
            vc.attachToBody(cam.getObstacle().getBody(), 0.0f);
            vc.setVisibility(true);
            visions.put(cam, vc);
        }

        for (Guard guard : guards) {
            float fov = guard.getFov();
            float dist = guard.getViewDistance();
            VisionCone vc = new VisionCone(60, Vector2.Zero, dist, 0.0f, fov, c, units, constants);
            vc.attachToBody(guard.getObstacle().getBody(), 0.0f);
            vc.setVisibility(debug);
            visions.put(guard, vc);
        }
    }

    // private void initializeLOSController(JsonValue constants) {
    // short exclude =
    // GameLevel.bitStringToComplement(constants.getString("exclude"));
    // this.losController = new LOSController(world, exclude);
    // }
    //
    // public LOSController getLOSController() {
    // return losController;
    // }

    /**
     * Create and register rectangle obstacles from a tile layer.
     * The layer must consist of tiles that has an object assigned to it.
     */
    private void createWallBodies(MapLayer layer, JsonValue constants) {
        for (MapObject wall : layer.getObjects()) {
            if (wall instanceof RectangleMapObject rec) {
                Rectangle rectangle = rec.getRectangle(); // dimensions given in pixels
                Obstacle obstacle = new BoxObstacle(
                        (rectangle.x + rectangle.width / 2) / units,
                        (rectangle.y + rectangle.height / 2) / units,
                        rectangle.width / units,
                        rectangle.height / units);

                obstacle.setPhysicsUnits(units);
                obstacle.setBodyType(BodyType.StaticBody);

                Filter filter = new Filter();
                short collideBits = GameLevel.bitStringToShort(constants.getString("category"));
                short excludeBits = GameLevel.bitStringToComplement(constants.getString("exclude"));
                filter.categoryBits = collideBits;
                filter.maskBits = excludeBits;
                obstacle.setFilterData(filter);

                objects.add(obstacle);
                obstacle.activatePhysics(world);
            } else if (wall instanceof EllipseMapObject e) {
                Ellipse ellipse = e.getEllipse();
            } else if (wall instanceof PolygonMapObject poly) {
                Polygon polygon = poly.getPolygon();
            }
        }

    }

    /**
     * Fixes the physics frame rate to be in sync with the animation framerate
     * <p>
     * http://gafferongames.com/game-physics/fix-your-timestep/
     *
     * @param dt the time passed since the last frame
     */
    private boolean fixedStep(float dt) {
        if (world == null)
            return false;

        physicsTimeLeft += dt;
        if (physicsTimeLeft > maxTimePerFrame) {
            physicsTimeLeft = maxTimePerFrame;
        }

        boolean stepped = false;
        while (physicsTimeLeft >= timeStep) {
            world.step(timeStep, WORLD_VELOC, WORLD_POSIT);
            physicsTimeLeft -= timeStep;
            stepped = true;
        }
        return stepped;
    }

    private boolean isInDanger(PlayableAvatar avatar) {
        if (avatar.isInvincible())
            return false;
        for (ObjectMap.Entry<ZoodiniSprite, VisionCone> entry : visions.entries()) {
            if (entry.key instanceof SecurityCamera && ((SecurityCamera) entry.key).isDisabled()) {
                continue;
            }
            if (entry.value.contains(avatar.getObstacle())) {
                return true;
            }
        }
        return false;
    }

    private void updateFlipSprite(Avatar avatar) {
        // flips the sprite if the avatar is moving left
        if (!avatar.isFlipped() && avatar.getMovement().x < 0.0f
                || avatar.isFlipped() && avatar.getMovement().x > 0.0f) {
            avatar.flipSprite();
        }
    }

    private void updateFlipGuardSprite(Guard guard){
        // flips the sprite if the guard is moving left
        if (!guard.isIdle() && (!guard.isFlipped() && guard.getMovement().x < 0.0f
                || guard.isFlipped() && guard.getMovement().x > 0.0f)) {
            guard.flipSprite();
        } else if (guard.isIdle() && guard.getMovement().x == 0.0f) {
            guard.setFlipped(false);
        }
    }

    private void updateGameTextPosition(float dt) {
        // in your update:
        float halfCycle = textCycleTimeSec / 2f;
        if (textMovingUp) {
            textPhase += dt;
            if (textPhase >= halfCycle) {
                textPhase = halfCycle;
                textMovingUp = false;
            }
        } else {
            textPhase -= dt;
            if (textPhase <= 0f) {
                textPhase = 0f;
                textMovingUp = true;
            }
        }

        // now alpha runs [0 .. 1] exactly
        float alpha = textPhase / halfCycle;

        // and if you still want that smooth “ease-in/out” curve:
        float smoothOffset = Interpolation.smooth.apply(
                -textMaxYOffsetTile, textMaxYOffsetTile, alpha);
        textCurrYOffsetTile = smoothOffset;
    }

    /**
     * Returns true if the object is in bounds.
     * <p>
     * This assertion is useful for debugging the physics.
     *
     * @param obj The object to check.
     * @return true if the object is in bounds.
     */
    private boolean inBounds(Obstacle obj) {
        boolean horiz = (bounds.x <= obj.getX() && obj.getX() <= bounds.x + bounds.width);
        boolean vert = (bounds.y <= obj.getY() && obj.getY() <= bounds.y + bounds.height);
        return horiz && vert;
    }

    PathFactory pathFactory = new PathFactory();
    PathExtruder pathExtruder = new PathExtruder();

    private void drawAbilityRange(SpriteBatch batch, PlayableAvatar avatar) {
        batch.setTexture(null);
        batch.setColor(Color.BLACK);
        float x = avatar.getObstacle().getX();
        float y = avatar.getObstacle().getY();

        Path2 rangePath = pathFactory.makeNgon(x, y, avatar.getAbilityRange(), 64); // radius = 1.0m. 64 vertices
        // TODO: ideally don't call makeNgon
        pathExtruder.set(rangePath);
        pathExtruder.calculate(0.05f); // line thickness = 0.05m
        affineCache.idt();
        affineCache.scale(getTileSize(), getTileSize());
        batch.fill(pathExtruder.getPolygon(), affineCache);
        batch.setColor(Color.WHITE);
    }

    /**
     * Draws a reticle on the screen to indicate aiming direction.
     *
     * <p>
     * This method retrieves the player's avatar and calculates the aiming position
     * relative to the game world using the camera's unprojection. It then
     * determines the direction from the avatar to the aiming position and draws a
     * purple reticle using a rotated rectangle.
     * </p>
     *
     * @param batch  the sprite batch used for rendering
     */
    private void drawOctopusReticle(SpriteBatch batch, Octopus octopus) {
        batch.setTexture(null);
        batch.setColor(Color.BLACK);
        float x = octopus.getObstacle().getX();
        float y = octopus.getObstacle().getY();

        Vector2 target = octopus.getTarget();

        // TODO: a couple of magic numbers here need to be config values I think
        Path2 reticlePath = pathFactory.makeNgon(target.x + x, target.y + y, 0.25f, 64);
        // TODO: ideally don't call makeNgon
        pathExtruder.set(reticlePath);
        pathExtruder.calculate(0.1f); // line thickness = 0.1m
        affineCache.idt();
        affineCache.scale(getTileSize(), getTileSize());
        batch.fill(pathExtruder.getPolygon(), affineCache);
        batch.setColor(Color.WHITE);
    }
}
