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
package edu.cornell.cis3152.lighting.models;

import box2dLight.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.*;
import edu.cornell.cis3152.lighting.models.nonentities.*;
import edu.cornell.cis3152.lighting.utils.ZoodiniSprite;
import edu.cornell.cis3152.lighting.models.entities.Avatar;
import edu.cornell.cis3152.lighting.models.entities.Cat;
import edu.cornell.cis3152.lighting.models.entities.Enemy;
import edu.cornell.cis3152.lighting.models.entities.Guard;
import edu.cornell.cis3152.lighting.models.entities.Octopus;
import edu.cornell.cis3152.lighting.models.entities.SecurityCamera;
import edu.cornell.cis3152.lighting.models.entities.Avatar.AvatarType;
import edu.cornell.cis3152.lighting.utils.VisionCone;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.math.PathExtruder;
import edu.cornell.gdiac.math.PathFactory;
import edu.cornell.gdiac.util.*;
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
	/** Number of velocity iterations for the constrain solvers */
	public static final int WORLD_VELOC = 6;
	/** Number of position iterations for the constrain solvers */
	public static final int WORLD_POSIT = 2;

	// Physics objects for the game
	/** Reference to the cat avatar */
	private Cat avatarCat;
	/** Reference to the octopus avatar */
	private Octopus avatarOctopus;
	/**
	 * Whether the currently active avatar is the cat. Otherwise, it's the octopus
	 */
	private boolean catActive;

	/** Reference to the goalDoor (for collision detection) */
	private Door goalDoor;

    /** Reference to the exit (for collision detection) */
    private Exit exit;

    private OrthographicCamera raycamera;

	private ObjectMap<Enemy, PositionalLight> enemyLights;
	private PositionalLight[] avatarLights; // TODO: array or separate field for two avatars?
	// ink projectile (there should only ever be one!!!)
	private InkProjectile inkProjectile;

	/** Whether or not the level is in debug more (showing off physics) */
	private boolean debug;

	/** All the object sprites in the world. */
	protected PooledList<ZoodiniSprite> sprites = new PooledList<ZoodiniSprite>();

	/** All the objects in the world. */
	protected PooledList<Obstacle> objects = new PooledList<>();

	// LET THE TIGHT COUPLING BEGIN
	/** The Box2D world */
	protected World world;
	/** The boundary of the world */
	protected Rectangle bounds;

    private float units;
    private Array<Enemy> enemies;
    private Array<SecurityCamera> securityCameras;
    private ObjectMap<ZoodiniSprite, VisionCone> visions;
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

    private float levelScaleX;
    private float levelScaleY;

    /** Reference to the key (for pickup detection) */
    private Key key;

	public float getLevelScaleX() {
		return levelScaleX;
	}

	public float getLevelScaleY() {
		return levelScaleY;
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
	public Array<Enemy> getEnemies() {
		return enemies;
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
		float[] pSize = levelFormat.get("world_size").asFloatArray();
		int[] gSize = levelGlobals.get("screen_size").asIntArray();

		world = new World(Vector2.Zero, false);
		bounds = new Rectangle(0, 0, pSize[0], pSize[1]);
		units = gSize[1] / pSize[1];

		levelScaleX = gSize[0] / pSize[0];
		levelScaleY = gSize[1] / pSize[1];

		// Compute the FPS
		int[] fps = levelGlobals.get("fps_range").asIntArray();
		maxFPS = fps[1];
		minFPS = fps[0];
		timeStep = 1.0f / maxFPS;
		maxSteps = 1.0f + (float) maxFPS / minFPS;
		maxTimePerFrame = timeStep * maxSteps;

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

		JsonValue bounds = levelFormat.getChild("exterior");
		while (bounds != null) {
			ExteriorWall obj = new ExteriorWall(directory, bounds, units);
			activate(obj);
			bounds = bounds.next();
		}

		JsonValue walls = levelFormat.getChild("interior");
		while (walls != null) {
			InteriorWall obj = new InteriorWall(directory, walls, units);
			activate(obj);
			walls = walls.next();
		}

		// Entities
		JsonValue catData = levelFormat.get("avatarCat");
		avatarCat = new Cat(directory, catData, levelGlobals.get("avatarCat"), units);
		activate(avatarCat);

		// Avatars
		JsonValue octopusData = levelFormat.get("avatarOctopus");
		avatarOctopus = new Octopus(directory, octopusData, levelGlobals.get("avatarOctopus"), units);
		activate(avatarOctopus);

		// Enemies
		this.enemies = new Array<>();
		JsonValue guards = levelFormat.getChild("guards");
		while (guards != null) {
			enemies.add(new Guard(directory, guards, levelGlobals.get("guard"), units));
			activate(enemies.peek());
			guards = guards.next();
		}

        // Security Cameras
        this.securityCameras = new Array<>();
        JsonValue cameras = levelFormat.getChild("cameras");
        while (cameras != null) {
            SecurityCamera camera = new SecurityCamera(directory, cameras, levelGlobals.get("camera"), units);
            activate(camera);
            securityCameras.add(camera);
            cameras = cameras.next();
        }

		// Lights
        this.visions = new ObjectMap<>();
        JsonValue visionJson = levelFormat.get("visions");
        initializeVisionCones(visionJson);

        raycamera = new OrthographicCamera(gSize[0], gSize[1]);
        raycamera.setToOrtho(false, gSize[0], gSize[1]);
        rayHandler = new RayHandler(world, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        RayHandler.useDiffuseLight(true);
        RayHandler.setGammaCorrection(true);
        rayHandler.setAmbientLight(0.5f,0.5f,0.5f,0.5f);

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

        for(Enemy guard : enemies){
            VisionCone vc = new VisionCone(rayNum, Vector2.Zero, radius, 0.0f, wideness, c, units, mask, category);
            vc.attachToBody(guard.getObstacle().getBody(), 90.0f);
            visions.put(guard, vc);
        }
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
	 * Immediately adds the object to the physics world
	 *
	 * @param obj The object to add
	 */
	protected void activate(ZoodiniSprite sprite) {
		assert inBounds(sprite.getObstacle()) : "Object is not in bounds";
		sprites.add(sprite);
		objects.add(sprite.getObstacle());
		sprite.getObstacle().activatePhysics(world);
	}

    public PooledList<Obstacle> getObjects() {
        return objects;
    }

    public PooledList<ZoodiniSprite> getSprites() {
        return sprites;
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

	public void createInkProjectile() {
		inkProjectile.setPosition(this.getAvatar().getPosition());
		inkProjectile.getObstacle().setActive(true);
		inkProjectile.setDrawingEnabled(true);
		Octopus o = (Octopus) getAvatar();
		Vector2 facing = o.getTarget().nor();
		inkProjectile.setMovement(facing.x, facing.y);
		inkProjectile.applyForce();
	}

	public void hideInkProjectile() {
		inkProjectile.getObstacle().setActive(false);
		inkProjectile.setDrawingEnabled(false);
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
            for(ObjectMap.Entry<ZoodiniSprite, VisionCone> entry: visions.entries()){
                VisionCone v = entry.value;
				ZoodiniSprite key = entry.key;
                v.update(world);
                if(v.contains(avatarCat.getPosition())){
                    if(key instanceof Guard){
                        ((Guard) key).setTarget(avatarCat.getPosition()); // TODO: this line might not be needed
                        ((Guard) key).setAgroed(true);
                        ((Guard) key).setAggroTarget(avatarCat);
                    } else if(key instanceof SecurityCamera){
                        SecurityCamera camera = (SecurityCamera)key;
                        if(!camera.isDisabled()) {
                            // Alert all guards when camera sees either player
                            for(Enemy e : enemies) {
                                if(e instanceof Guard) {
                                    Guard guard = (Guard)e;
                                    guard.setTarget(avatarCat.getPosition());
                                    guard.setAggroTarget(avatarCat);
                                    guard.setCameraAlerted(true);
                                }
                            }
                        }
                    }
                }
                else if (v.contains(avatarOctopus.getPosition())){
                    if(key instanceof Guard){
                        ((Guard) key).setTarget(avatarOctopus.getPosition()); // TODO: this line might not be needed
                        ((Guard) key).setAgroed(true);
                        ((Guard) key).setAggroTarget(avatarOctopus);
                    } else if(key instanceof SecurityCamera){
                        SecurityCamera camera = (SecurityCamera)key;
                        if(!camera.isDisabled()) {
                            // Alert all guards when camera sees either player
                            for(Enemy e : enemies) {
                                if(e instanceof Guard) {
                                    Guard guard = (Guard)e;
                                    guard.setTarget(avatarOctopus.getPosition());
                                    guard.setAggroTarget(avatarOctopus);
                                    guard.setCameraAlerted(true);
                                }
                            }
                        }
                    }
                } else if (!v.contains(avatarCat.getPosition())){
                    if(key instanceof Guard){
                        ((Guard) key).setAgroed(false);
                    }
                } else if (!v.contains(avatarOctopus.getPosition())){
                    if(key instanceof Guard){
                        ((Guard) key).setAgroed(false);
                    }
                }
            }

            rayHandler.setCombinedMatrix(raycamera);

			avatarCat.update(dt);
			avatarOctopus.update(dt);
            for (Enemy e : enemies) {
                e.update(dt);
            }
            for (SecurityCamera c : securityCameras) {
                c.update(dt);
            }
            inkProjectile.update(dt);
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

    public void swapActiveAvatar() {
//        avatarLights[catActive ? 0 : 1].setActive(false);
        catActive = !catActive;
//        avatarLights[catActive ? 0 : 1].setActive(true);
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
		if (avatar.getAvatarType() == AvatarType.OCTOPUS) {
			Octopus octopus = (Octopus) avatar;
			if (octopus.isCurrentlyAiming()) {
				drawOctopusReticle(batch, camera);
			}
		}

		batch.end();

        rayHandler.render();

		// Draw debugging on top of everything.
		if (debug) {
			batch.begin(camera);
			for (ObstacleSprite obj : sprites) {
				obj.drawDebug(batch);
			}
			batch.end();
		}
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
		float u = octopus.getObstacle().getPhysicsUnits();

		Vector2 target = octopus.getTarget();

		// TODO: a couple of magic numbers here need to be config values I think
		Path2 reticlePath = new PathFactory().makeCircle(target.x, target.y, 25);
		PathExtruder extruder = new PathExtruder(reticlePath);
		extruder.calculate(3);
		Affine2 transform = new Affine2();
		transform.preTranslate(x * u, y * u);
		batch.draw((TextureRegion) null, extruder.getPolygon(), transform);
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
}
