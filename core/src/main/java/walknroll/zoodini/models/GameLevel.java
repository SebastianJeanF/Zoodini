/*
 * GameLevel.java
 *
 */

package walknroll.zoodini.models;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.EllipseMapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Ellipse;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

import box2dLight.PositionalLight;
import box2dLight.RayHandler;
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
import walknroll.zoodini.utils.Constants;
import walknroll.zoodini.utils.VisionCone;
import walknroll.zoodini.utils.ZoodiniSprite;
import walknroll.zoodini.utils.animation.AnimationState;
import walknroll.zoodini.utils.enums.AvatarType;

/**
 * Represents a single level in our game
 *
 * Note that the constructor does very little. The true initialization happens
 * by reading the JSON value. To reset a level, dispose it and reread the JSON.
 *
 * The level contains its own Box2d World, as the World settings are defined by
 * the JSON file. There is generally no controller code in this class, except
 * for the update method for moving ahead one timestep. All of the other methods
 * are getters and setters. The getters allow the GameScene class to modify the
 * level elements.
 */
public class GameLevel {
    /** Number of velocity iterations for the constrain solvers */
    public static final int WORLD_VELOC = 6;

    /** Number of position iterations for the constrain solvers */
    public static final int WORLD_POSIT = 2;

    /**
     * Returns a string equivalent to the sequence of bits in s
     *
     * This function assumes that s is a string of 0s and 1s of length < 16.
     * This function allows the JSON file to specify bit arrays in a readable
     * format.
     *
     * @param s the string representation of the bit array
     *
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
     *
     * This function assumes that s is a string of 0s and 1s of length < 16.
     * This function allows the JSON file to specify exclusion bit arrays (for
     * masking) in a readable format.
     *
     * @param s the string representation of the bit array
     *
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

    /** Whether or not the level is in debug more (showing off physics) */
    private boolean debug;
    // Physics objects for the game
    /** Reference to the cat avatar */
    private Cat avatarCat;

    /** Reference to the octopus avatar */
    private Octopus avatarOctopus;

    /**
     * Whether the currently active avatar is the cat. Otherwise, it's the octopus
     */
    private boolean catActive;

    /** Reference to the exit (for collision detection) */
    private Exit exit;
    private OrthographicCamera raycamera;
    private ObjectMap<Guard, PositionalLight> guardLights = new ObjectMap<>();
    private PositionalLight[] avatarLights = new PositionalLight[2]; // TODO: array or separate field for two avatars?
    private Array<Guard> guards = new Array<>();
    private Array<SecurityCamera> securityCameras = new Array<>();
    private ObjectMap<ZoodiniSprite, VisionCone> visions = new ObjectMap<>();
    private InkProjectile inkProjectile; // ink projectile (there should only ever be one!!!)

    private Array<Key> keys = new Array<>();

    private ObjectMap<Door, Key> doors = new ObjectMap<>();

    /** All the object sprites in the world. */
    protected PooledList<ZoodiniSprite> sprites = new PooledList<ZoodiniSprite>();
    /** All the objects in the world. */
    protected PooledList<Obstacle> objects = new PooledList<>();
    /** The Box2D world */
    protected World world;

    /** The boundary of the world */
    protected Rectangle bounds;

    /** Size of one tile. This serves as scaling factor for all drawings */
    private float units;
    RayHandler rayHandler;
    // TO FIX THE TIMESTEP
    /** The maximum frames per second setting for this level */
    protected int maxFPS;
    /** The minimum frames per second setting for this level */
    protected int minFPS;
    /** The amount of time in to cover a single animation frame */
    protected float timeStep;
    /** The maximum number of steps allowed before moving physics forward */
    protected float maxSteps;

    /** The maximum amount of time allowed in a frame */
    protected float maxTimePerFrame;

    /** The amount of time that has passed without updating the frame */
    protected float physicsTimeLeft;

    Affine2 affineCache = new Affine2();

    /**
     * Creates a new GameLevel
     *
     * The level is empty and there is no active physics world. You must read
     * the JSON file to initialize the level
     */
    public GameLevel() {
        world = null;
        bounds = new Rectangle(0, 0, 1, 1);
        debug = Constants.DEBUG;
        catActive = true;
    }

    public ObjectMap<ZoodiniSprite, VisionCone> getVisionConeMap() {
        return visions;
    }

    /**
     * Lays out the game geography from the given JSON file
     *
     * @param directory the asset manager
     */
    public void populate(AssetDirectory directory, TiledMap map) {
        // Compute the FPS
        int[] fps = { 20, 60 };
        maxFPS = fps[1];
        minFPS = fps[0];
        timeStep = 1.0f / maxFPS;
        maxSteps = 1.0f + (float) maxFPS / minFPS;
        maxTimePerFrame = timeStep * maxSteps;

        world = new World(Vector2.Zero, false);

        MapProperties props = map.getProperties();
        int width = props.get("width", Integer.class);
        int height = props.get("height", Integer.class);
        units = props.get("tilewidth", Integer.class);
        bounds = new Rectangle(0, 0, width, height);

        MapLayer walls = map.getLayers().get("walls");
        createWallBodies(walls);

        MapLayer objectLayer = map.getLayers().get("objects");
        for (MapObject obj : objectLayer.getObjects()) {
            MapProperties properties = obj.getProperties();
            String type = properties.get("type", String.class);

            if ("Cat".equalsIgnoreCase(type)) {
                avatarCat = new Cat(properties, units);
                avatarCat.setAnimation(AnimationState.IDLE,
                        directory.getEntry("cat-idle.animation", SpriteSheet.class));
                avatarCat.setAnimation(AnimationState.WALK,
                        directory.getEntry("cat-walk.animation", SpriteSheet.class));
                avatarCat.setAnimation(AnimationState.WALK_DOWN,
                        directory.getEntry("cat-walk-down.animation", SpriteSheet.class));
                avatarCat.setAnimation(AnimationState.WALK_UP,
                        directory.getEntry("cat-walk-up.animation", SpriteSheet.class));
                activate(avatarCat);
            } else if ("Octopus".equalsIgnoreCase(type)) {
                avatarOctopus = new Octopus(properties, units);
                avatarOctopus.setAnimation(AnimationState.IDLE,
                        directory.getEntry("octopus-idle.animation", SpriteSheet.class));
                avatarOctopus.setAnimation(AnimationState.WALK,
                        directory.getEntry("octopus-walk.animation", SpriteSheet.class));
                avatarOctopus.setAnimation(AnimationState.WALK_DOWN,
                        directory.getEntry("octopus-walk-down.animation", SpriteSheet.class));
                avatarOctopus.setAnimation(AnimationState.WALK_UP,
                        directory.getEntry("octopus-walk-up.animation", SpriteSheet.class));
                activate(avatarOctopus);
            } else if ("Guard".equalsIgnoreCase(type)) {
                Guard g = new Guard(properties, units);
                g.setAnimation(AnimationState.IDLE, directory.getEntry("guard-idle.animation", SpriteSheet.class));
                g.setAnimation(AnimationState.WALK, directory.getEntry("guard-walk.animation", SpriteSheet.class));
                g.setAnimation(AnimationState.WALK_DOWN,
                        directory.getEntry("guard-walk-down.animation", SpriteSheet.class));
                g.setAnimation(AnimationState.WALK_UP,
                        directory.getEntry("guard-walk-up.animation", SpriteSheet.class));
                g.setSusMeter(directory.getEntry("suspicion-meter.animation", SpriteSheet.class)); // TODO: There must
                                                                                                   // be a better way to
                                                                                                   // do this
                guards.add(g);
                activate(g);
            } else if ("Camera".equalsIgnoreCase(type)) {
                SecurityCamera cam = new SecurityCamera(properties, units);
                cam.setAnimation(AnimationState.IDLE, directory.getEntry("camera-idle.animation", SpriteSheet.class));
                securityCameras.add(cam);
                activate(cam);
            } else if ("Door".equalsIgnoreCase(type)) {
                Door door = new Door(directory, obj, units);
                Key key = new Key(directory, obj.getProperties().get("key", MapObject.class), units);
                doors.put(door, key);
                keys.add(key);
                activate(door);
                activate(key);
            } else if ("Exit".equalsIgnoreCase(type)) {
                exit = new Exit(directory, properties, units);
                activate(exit);
            }

        }

        initializeVisionCones();

        // raycamera = new OrthographicCamera(gSize[0], gSize[1]);
        // raycamera.setToOrtho(false, gSize[0], gSize[1]);
        // rayHandler = new RayHandler(world, Gdx.graphics.getWidth(),
        // Gdx.graphics.getHeight());
        // RayHandler.useDiffuseLight(true);
        // RayHandler.setGammaCorrection(true);
        // rayHandler.setAmbientLight(0.5f,0.5f,0.5f,0.5f);

        // Initialize an ink projectile (but do not add it to the physics world, we only
        // do that on demand)
        JsonValue projectileData = directory.getEntry("constants", JsonValue.class).get("ink");
        inkProjectile = new InkProjectile(projectileData, units);
        inkProjectile.setAnimation(AnimationState.EXPLODE,
                directory.getEntry("ink-explosion.animation", SpriteSheet.class));
        inkProjectile.setAnimation(AnimationState.IDLE,
                directory.getEntry("ink-idle.animation", SpriteSheet.class));
        activate(inkProjectile);
        inkProjectile.setDrawingEnabled(false);
        inkProjectile.getObstacle().setActive(false);
    }

    /**
     * Disposes of all resources for this model.
     *
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

        sprites.clear();
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

            if (rayHandler != null) {
                rayHandler.setCombinedMatrix(raycamera);
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
                updateFlipSprite(g);
            }

            for (SecurityCamera c : securityCameras) {
                c.update(dt);
            }

            for (VisionCone vc : visions.values()) {
                vc.update(world);
            }

            for (Door door : doors.keys()) {
                door.update(dt);
            }

            for (Key key : keys) {
                key.update(dt);
            }

            // checkPlayerInVisionCones();
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
            if (v.contains(avatarCat.getPosition())) {
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
            } else if (v.contains(avatarOctopus.getPosition())) {
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
            } else if (!v.contains(avatarCat.getPosition())) {
                if (key instanceof Guard) {
                    ((Guard) key).setAgroed(false);
                }
            } else if (!v.contains(avatarOctopus.getPosition())) {
                if (key instanceof Guard) {
                    ((Guard) key).setAgroed(false);
                }
            }
        }
    }

    /**
     * Draws the level to the given game canvas
     *
     * If debug mode is true, it will outline all physics bodies as wireframes.
     * Otherwise it will only draw the sprite representations.
     *
     * @param batch  the sprite batch to draw to
     * @param camera the drawing camera
     */
    public void draw(SpriteBatch batch, Camera camera) {
        // Draw the sprites first (will be hidden by shadows)

        batch.begin(camera);
        sprites.sort(ZoodiniSprite.Comparison);
        for (ZoodiniSprite obj : sprites) {
            if (obj.isDrawingEnabled()) {
                batch.setColor(Color.WHITE);
                obj.draw(batch);
            }
        }

        Avatar avatar = getAvatar();
        if (avatar != null) {
            if (avatar.getAvatarType() == AvatarType.OCTOPUS) {
                Octopus octopus = (Octopus) avatar;
                if (octopus.isCurrentlyAiming() && octopus.canUseAbility()) {
                    drawOctopusReticle(batch, camera);
                    drawAbilityRange(batch, camera);
                }
            }
            if (avatar.getAvatarType() == AvatarType.CAT) {
                Cat cat = (Cat) avatar;
                if (cat.isCurrentlyAiming() && cat.canUseAbility()) {
                    drawAbilityRange(batch, camera);
                }
            }
        }

        for (ObjectMap.Entry<ZoodiniSprite, VisionCone> entry : visions.entries()) {
            if (entry.key instanceof SecurityCamera && ((SecurityCamera) entry.key).isDisabled()) {
                continue;
            }
            entry.value.draw(batch, camera);
        }

        // d debugging on top of everything.
        if (debug) {
            for (ObstacleSprite obj : sprites) {
                obj.drawDebug(batch);
            }
        }

        batch.end();
    }

    public boolean isInactiveAvatarInDanger() {
        if (catActive) {
            return isInDanger(avatarOctopus);
        } else {
            return isInDanger(avatarCat);
        }
    }

    // ------------------Helpers-----------------------//

    public void swapActiveAvatar() {
        // avatarLights[catActive ? 0 : 1].setActive(false);
        catActive = !catActive;
        // avatarLights[catActive ? 0 : 1].setActive(true);
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

    /**
     * Returns the bounding rectangle for the physics world
     *
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
        return catActive ? avatarCat : avatarOctopus;
    }

    public Cat getCat() {
        return avatarCat;
    }

    public Octopus getOctopus() {
        return avatarOctopus;
    }

    public PlayableAvatar getInactiveAvatar() {
        return catActive ? avatarOctopus : avatarCat;
    }

    /**
     * Returns a reference to the doors
     *
     * @return a reference to the doors
     */
    public ObjectMap<Door, Key> getDoors() {
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
     *
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
     *
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
     *
     * This value is used by the rayhandler to fix the physics timestep.
     *
     * @return the maximum FPS supported by this level
     */
    public int getMaxFPS() {
        return maxFPS;
    }

    /**
     * Sets the maximum FPS supported by this level
     *
     * This value is used by the rayhandler to fix the physics timestep.
     *
     * @param value the maximum FPS supported by this level
     */
    public void setMaxFPS(int value) {
        maxFPS = value;
    }

    /**
     * Returns the minimum FPS supported by this level
     *
     * This value is used by the rayhandler to fix the physics timestep.
     *
     * @return the minimum FPS supported by this level
     */
    public int getMinFPS() {
        return minFPS;
    }

    /**
     * Sets the minimum FPS supported by this level
     *
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

    private void initializeVisionCones() {
        Color c = Color.WHITE.cpy().add(0, 0, 0, -0.5f);
        for (SecurityCamera cam : securityCameras) {
            float fov = cam.getFov();
            float dist = cam.getViewDistance();
            VisionCone vc = new VisionCone(60, Vector2.Zero, dist, 0.0f, fov, c, units, "000000", "111110");
            float angle = cam.getAngle();
            vc.attachToBody(cam.getObstacle().getBody(), angle);
            vc.setVisibility(true);
            visions.put(cam, vc);
        }

        for (Guard guard : guards) {
            float fov = guard.getFov();
            float dist = guard.getViewDistance();
            VisionCone vc = new VisionCone(60, Vector2.Zero, dist, 0.0f, fov, c, units, "000000", "111110");
            vc.attachToBody(guard.getObstacle().getBody(), 90.0f);
            vc.setVisibility(debug);
            visions.put(guard, vc);
        }
    }

    /**
     * Create and register rectangle obstacles from a tile layer.
     * The layer must consist of tiles that has an object assigned to it.
     */
    private void createWallBodies(MapLayer layer) {
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
                short collideBits = GameLevel.bitStringToShort("0001");
                short excludeBits = GameLevel.bitStringToComplement("0000");
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
     *
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

    private boolean isInDanger(Avatar avatar) {
        for (VisionCone cone : visions.values()) {
            if (cone.contains(avatar.getPosition())) {
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

    /**
     * Returns true if the object is in bounds.
     *
     * This assertion is useful for debugging the physics.
     *
     * @param obj The object to check.
     *
     * @return true if the object is in bounds.
     */
    private boolean inBounds(Obstacle obj) {
        boolean horiz = (bounds.x <= obj.getX() && obj.getX() <= bounds.x + bounds.width);
        boolean vert = (bounds.y <= obj.getY() && obj.getY() <= bounds.y + bounds.height);
        return horiz && vert;
    }

    private void drawAbilityRange(SpriteBatch batch, Camera camera) {
        PlayableAvatar avatar = getAvatar();
        batch.setTexture(null);
        batch.setColor(Color.BLACK);
        float x = avatar.getObstacle().getX();
        float y = avatar.getObstacle().getY();

        Path2 rangePath = new PathFactory().makeNgon(x, y, avatar.getAbilityRange(), 64); // radius = 1.0m. 64 vertices
        PathExtruder rangeExtruder = new PathExtruder(rangePath);
        rangeExtruder.calculate(0.05f); // line thickness = 0.05m
        affineCache.idt();
        affineCache.scale(getTileSize(), getTileSize());
        batch.draw((TextureRegion) null, rangeExtruder.getPolygon(), affineCache);
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
     * @param camera the camera used to unproject screen coordinates to world
     *               coordinates
     */
    private void drawOctopusReticle(SpriteBatch batch, Camera camera) {
        Octopus octopus = (Octopus) getAvatar();
        batch.setTexture(null);
        batch.setColor(Color.BLACK);
        float x = octopus.getObstacle().getX();
        float y = octopus.getObstacle().getY();

        Vector2 target = octopus.getTarget();

        // TODO: a couple of magic numbers here need to be config values I think
        Path2 reticlePath = new PathFactory().makeNgon(target.x + x, target.y + y, 0.25f, 64); // radius = 1.0m. 64
                                                                                               // vertices
        PathExtruder reticleExtruder = new PathExtruder(reticlePath);
        reticleExtruder.calculate(0.1f); // line thickness = 0.1m
        affineCache.idt();
        affineCache.scale(getTileSize(), getTileSize());
        batch.draw((TextureRegion) null, reticleExtruder.getPolygon(), affineCache);
    }
}
