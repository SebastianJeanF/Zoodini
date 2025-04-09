/*
 * GameLevel.java
 *
 * This stores all of the information to define a level for a top down game with
 * light and shadows. As with Lab 4, it has an avatar, some walls, and an exit.
 * This model supports JSON loading, and so the world is part of this object as
 * well. See the JSON demo for more information.
 *
 * There are two major differences from JSON Demo. First is the fixStep method.
 * This ensures that the physics engine is really moving at the same rate as the
 * visual framerate. You can usually survive without this addition. However,
 * when the physics adjusts shadows, it is very important. See this website for
 * more information about what is going on here.
 *
 * http://gafferongames.com/game-physics/fix-your-timestep/
 *
 * The second addition is the RayHandler. This is an attachment to the physics
 * world for drawing shadows. Technically, this is a view, and really should be
 * part of GameScene.  However, in true graphics programmer garbage design, this
 * is tightly coupled the the physics world and cannot be separated.  So we
 * store it here and make it part of the draw method. This is the best of many
 * bad options.
 *
 * TODO: Refactor this design to decouple the RayHandler as much as possible.
 * Next year, maybe.
 *
 * @author: Walker M. White
 * @version: 2/15/2025
 */
package walknroll.zoodini.models;

import box2dLight.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.Map;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.EllipseMapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.*;

import com.badlogic.gdx.utils.ObjectMap.Entry;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.math.PathExtruder;
import edu.cornell.gdiac.math.PathFactory;
import edu.cornell.gdiac.util.*;
import java.util.Iterator;
import walknroll.zoodini.models.entities.Avatar;
import walknroll.zoodini.models.entities.Cat;
import walknroll.zoodini.models.entities.Guard;
import walknroll.zoodini.models.entities.Octopus;
import walknroll.zoodini.models.entities.SecurityCamera;
import walknroll.zoodini.models.entities.Avatar.AvatarType;
import walknroll.zoodini.models.nonentities.*;
import walknroll.zoodini.utils.VisionCone;
import walknroll.zoodini.utils.ZoodiniSprite;
import edu.cornell.gdiac.physics2.*;

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
    /** Whether or not the level is in debug more (showing off physics) */
    private boolean debug;

	/** Number of velocity iterations for the constrain solvers */
	public static final int WORLD_VELOC = 6;
	/** Number of position iterations for the constrain solvers */
	public static final int WORLD_POSIT = 2;

    /** Specialized renderer for rendering tiles */
    private OrthogonalTiledMapRenderer mapRenderer;

	// Physics objects for the game
	/** Reference to the cat avatar */
	private Cat avatarCat;
	/** Reference to the octopus avatar */
	private Octopus avatarOctopus;
	/** Whether the currently active avatar is the cat. Otherwise, it's the octopus */
	private boolean catActive;

	/** Reference to the goalDoor (for collision detection) */
	private Door goalDoor;
    /** Reference to the exit (for collision detection) */
    private Exit exit;
    /** Reference to the key (for pickup detection) */
    private Key key;

    private OrthographicCamera raycamera;

	private ObjectMap<Guard, PositionalLight> guardLights = new ObjectMap<>();
	private PositionalLight[] avatarLights = new PositionalLight[2]; // TODO: array or separate field for two avatars?
    private Array<Guard> guards = new Array<>();
    private Array<SecurityCamera> securityCameras = new Array<>();
    private ObjectMap<ZoodiniSprite, VisionCone> visions = new ObjectMap<>();
    private InkProjectile inkProjectile; // ink projectile (there should only ever be one!!!)

	/** All the object sprites in the world. */
	protected PooledList<ZoodiniSprite> sprites = new PooledList<ZoodiniSprite>();

	/** All the objects in the world. */
	protected PooledList<Obstacle> objects = new PooledList<>();

	/** The Box2D world */
	protected World world;
	/** The boundary of the world */
	protected Rectangle bounds;
    /** Map */
    private TiledMap tiledMap;
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



	/**
	 * Creates a new GameLevel
	 *
	 * The level is empty and there is no active physics world. You must read
	 * the JSON file to initialize the level
	 */
	public GameLevel() {
		world = null;
		bounds = new Rectangle(0, 0, 1, 1);
		debug = false;
		catActive = true;
	}

	/**
	 * Lays out the game geography from the given JSON file
	 *
	 * @param directory   the asset manager
	 * @param levelFormat the JSON file defining the level
	 * @param levelGlobals the JSON file defining configs global to every level
	 */
	public void populate(AssetDirectory directory, JsonValue levelFormat, JsonValue levelGlobals) {
        // Compute the FPS
        int[] fps = {20, 60};
        maxFPS = fps[1];
        minFPS = fps[0];
        timeStep = 1.0f / maxFPS;
        maxSteps = 1.0f + (float) maxFPS / minFPS;
        maxTimePerFrame = timeStep * maxSteps;


		world = new World(Vector2.Zero, false);
        tiledMap = new TmxMapLoader().load(levelFormat.get("map").getString("file"));
        mapRenderer = new OrthogonalTiledMapRenderer(tiledMap);


        TiledMapTileLayer ground = ((TiledMapTileLayer) tiledMap.getLayers().get("ground"));
        units = ground.getTileWidth();
        int width = ground.getWidth(); //30
        int height = ground.getHeight(); //20
        bounds = new Rectangle(0,0,width,height);

        MapLayer walls = tiledMap.getLayers().get("walls");
        createWallBodies(walls);


        MapLayer playerSpawn = tiledMap.getLayers().get("players");
        for(MapObject obj : playerSpawn.getObjects()){
            MapProperties properties = obj.getProperties();
            String type = properties.get("type", String.class);
            if("Cat".equalsIgnoreCase(type)){
                avatarCat = new Cat(directory, properties, levelGlobals.get("avatarCat"), units);
                activate(avatarCat);
            } else if("Octopus".equalsIgnoreCase(type)){
                avatarOctopus = new Octopus(directory, properties, levelGlobals.get("avatarOctopus"), units);
                activate(avatarOctopus);
            }
        }


        MapLayer guardSpawn = tiledMap.getLayers().get("guards");
        for(MapObject obj : guardSpawn.getObjects()){
            MapProperties properties = obj.getProperties();
            guards.add(new Guard(directory, properties, levelGlobals.get("guard"), units));
            activate(guards.peek());
        }


        MapLayer cameraSpawn = tiledMap.getLayers().get("cameras");
        for(MapObject obj : cameraSpawn.getObjects()){
            MapProperties properties = obj.getProperties();
            securityCameras.add(new SecurityCamera(directory, properties, levelGlobals.get("camera"), units));
            activate(securityCameras.peek());
        }


        // Walls
		goalDoor = new Door(directory, levelFormat.get("door"), levelGlobals.get("door"), units);
		activate(goalDoor);

        exit = new Exit(directory, levelFormat.get("exit"), levelGlobals.get("exit"), units);
        activate(exit);

        // Create the key
        if (levelFormat.has("key")) {
            JsonValue keyData = levelFormat.get("key");
            key = new Key(directory, keyData, levelGlobals.get("key"), units);
            activate(key);
        }

        // Security Cameras
//        JsonValue cameras = levelFormat.getChild("cameras");
//        while (cameras != null) {
//            SecurityCamera camera = new SecurityCamera(directory, cameras, levelGlobals.get("camera"), units);
//            activate(camera);
//            securityCameras.add(camera);
//            cameras = cameras.next();
//        }

		// Lights
        JsonValue visionJson = levelFormat.get("visions");
        initializeVisionCones(visionJson);

//        raycamera = new OrthographicCamera(gSize[0], gSize[1]);
//        raycamera.setToOrtho(false, gSize[0], gSize[1]);
//        rayHandler = new RayHandler(world, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
//        RayHandler.useDiffuseLight(true);
//        RayHandler.setGammaCorrection(true);
//        rayHandler.setAmbientLight(0.5f,0.5f,0.5f,0.5f);

		// Initialize an ink projectile (but do not add it to the physics world, we only
		// do that on demand)
		JsonValue projectileData = levelGlobals.get("ink");
		inkProjectile = new InkProjectile(directory, projectileData, units);
		activate(inkProjectile);
		inkProjectile.setDrawingEnabled(false);
		inkProjectile.getObstacle().setActive(false);
	}

    private void initializeVisionCones(JsonValue json) {
        int rayNum = json.getInt("rayNum");
        float radius = json.getFloat("radius");
        float[] color = json.get("color").asFloatArray();
        float wideness = json.getFloat("wideness");
        short mask = json.getShort("maskbit");
        short category = json.getShort("categorybit");
        Color c = new Color(color[0], color[1], color[2], color[3]);
        for(SecurityCamera cam : securityCameras){
            VisionCone vc = new VisionCone(rayNum, Vector2.Zero, radius, 0.0f, wideness, c, units, mask, category);
            float angle = cam.getAngle();
            vc.attachToBody(cam.getObstacle().getBody(), angle);
            visions.put(cam, vc);
        }

        for(Guard guard : guards){
            VisionCone vc = new VisionCone(rayNum, Vector2.Zero, radius, 0.0f, wideness, c, units, mask, category);
            vc.attachToBody(guard.getObstacle().getBody(), 90.0f);
            visions.put(guard, vc);
        }
    }


    /**
     * Create and register rectangle obstacles from a tile layer.
     * The layer must consist of tiles that has an object assigned to it.
     * */
    private void createWallBodies(MapLayer layer){
        float tileSize = getTileSize();
        for(MapObject wall : layer.getObjects()){
            if (wall instanceof RectangleMapObject rec)
            {
                Rectangle rectangle = rec.getRectangle(); //dimensions given in pixels
                Obstacle obstacle = new BoxObstacle(
                    (rectangle.x + rectangle.width / 2) / units,
                    (rectangle.y + rectangle.height / 2) / units,
                    rectangle.width / units,
                    rectangle.height / units
                );

                obstacle.setPhysicsUnits(units);
                obstacle.setBodyType(BodyType.StaticBody);

                Filter filter = new Filter();
                short collideBits = GameLevel.bitStringToShort("0100");
                short excludeBits = GameLevel.bitStringToComplement("0000");
                filter.categoryBits = collideBits;
                filter.maskBits = excludeBits;
                obstacle.setFilterData(filter);

                objects.add(obstacle);
                obstacle.activatePhysics(world);
            }
            else if (wall instanceof EllipseMapObject e)
            {
                Ellipse ellipse = e.getEllipse();
            }
            else if (wall instanceof PolygonMapObject poly)
            {

//                Polygon polygon = poly.getPolygon();
//                Obstacle obstacle = new PolygonObstacle(polygon.getVertices());
//                obstacle.setPhysicsUnits(units);
//                obstacle.setBodyType(BodyType.StaticBody);
//
//                Filter filter = new Filter();
//                short collideBits = GameLevel.bitStringToShort("0100");
//                short excludeBits = GameLevel.bitStringToComplement("0000");
//                filter.categoryBits = collideBits;
//                filter.maskBits = excludeBits;
//                obstacle.setFilterData(filter);
//
//                objects.add(obstacle);
//                obstacle.activatePhysics(world);
            }
        }

//        for (int x = 0; x < layer.getWidth(); x++) {
//            for (int y = 0; y < layer.getHeight(); y++) {
//                TiledMapTileLayer.Cell cell = layer.getCell(x, y);
//                if (cell == null)
//                    continue;
//
//                MapObjects cellObjects = cell.getTile().getObjects();
//                if (cellObjects.getCount() != 1)
//                    continue;
//
//                MapObject mapObject = cellObjects.get(0);
//                if (mapObject instanceof RectangleMapObject rectangleObject){
//                    Rectangle rectangle = rectangleObject.getRectangle(); //in pixels
//                    Obstacle obstacle =
//                    new BoxObstacle(
//                        x + 1/2f, y + 1/2f,
//                        rectangle.getWidth() / tileSize,
//                        rectangle.getHeight() / tileSize
//                    );
//
//                obstacle.setPhysicsUnits(units);
//                obstacle.setBodyType(BodyType.StaticBody);
//
//                Filter filter = new Filter();
//                short collideBits = GameLevel.bitStringToShort("0100");
//                short excludeBits = GameLevel.bitStringToComplement("0000");
//                filter.categoryBits = collideBits;
//                filter.maskBits = excludeBits;
//                obstacle.setFilterData(filter);
//
//                objects.add(obstacle);
//                obstacle.activatePhysics(world);
//                }
//            }
//        }
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

            //Update animation frames
            if(rayHandler != null) {
                rayHandler.setCombinedMatrix(raycamera);
            }

            if(avatarCat != null) {
                avatarCat.update(dt);
            }

            if(avatarOctopus != null) {
                avatarOctopus.update(dt);
            }

            if(inkProjectile != null) {
                inkProjectile.update(dt);
            }

            for (Guard g : guards) {
                g.update(dt);
            }

            for (SecurityCamera c : securityCameras) {
                c.update(dt);
            }

            for(VisionCone vc : visions.values()){
                vc.update(world);
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

        mapRenderer.setView((OrthographicCamera) camera);
        mapRenderer.render();

        for(ObjectMap.Entry<ZoodiniSprite, VisionCone> entry : visions.entries()){
			if (entry.key instanceof SecurityCamera && ((SecurityCamera) entry.key).isDisabled()) {
				continue;
			}
            entry.value.draw(batch,camera);
        }

        batch.begin(camera);
		for (ZoodiniSprite obj : sprites) {
			if (obj.isDrawingEnabled()) {
				obj.draw(batch);
			}
		}

		Avatar avatar = getAvatar();
        if(avatar != null) {
            if (avatar.getAvatarType() == AvatarType.OCTOPUS) {
                Octopus octopus = (Octopus) avatar;
                if (octopus.isCurrentlyAiming()) {
                    drawOctopusReticle(batch, camera);
                }
            }
        }

		batch.end();

        if(rayHandler != null) {
            rayHandler.render();
        }

		// Draw debugging on top of everything.
		if (debug) {
			batch.begin(camera);
			for (ObstacleSprite obj : sprites) {
				obj.drawDebug(batch);
			}
			batch.end();
		}
	}



    //------------------Helpers-----------------------//

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


    public void swapActiveAvatar() {
//        avatarLights[catActive ? 0 : 1].setActive(false);
        catActive = !catActive;
//        avatarLights[catActive ? 0 : 1].setActive(true);
    }

    Affine2 affineCache = new Affine2();
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
		Path2 reticlePath = new PathFactory().makeNgon(target.x + x, target.y + y, 0.25f, 64); //radius = 1.0m. 64 vertices
		PathExtruder reticleExtruder = new PathExtruder(reticlePath);
		reticleExtruder.calculate(0.1f); //line thickness = 0.1m
        affineCache.idt();
        affineCache.scale(getTileSize(), getTileSize());
		batch.draw((TextureRegion) null, reticleExtruder.getPolygon(), affineCache);

        Path2 rangePath = new PathFactory().makeNgon(x, y, octopus.getAbilityRange(), 64); //radius = 1.0m. 64 vertices
		PathExtruder rangeExtruder = new PathExtruder(rangePath);
		rangeExtruder.calculate(0.05f); //line thickness = 0.05m
        affineCache.idt();
        affineCache.scale(getTileSize(), getTileSize());
		batch.draw((TextureRegion) null, rangeExtruder.getPolygon(), affineCache);
		batch.setColor(Color.WHITE);
	}

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


    public PooledList<Obstacle> getObjects() {
        return objects;
    }

    public PooledList<ZoodiniSprite> getSprites() {
        return sprites;
    }

    /**
     * Returns a reference to the key
     *
     * @return a reference to the key
     */
    public Key getKey() {
        return key;
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
    public Avatar getAvatar() {
        return catActive ? avatarCat : avatarOctopus;
    }

    public Avatar getCat() {
        return avatarCat;
    }

    public Avatar getOctopus() {
        return avatarOctopus;
    }


    /**
     * Returns a reference to the door
     *
     * @return a reference to the door
     */
    public Door getDoor() {
        return goalDoor;
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

    public TiledMap getMap(){
        return tiledMap;
    }

    public float getTileSize(){
        return units;
    }
}
