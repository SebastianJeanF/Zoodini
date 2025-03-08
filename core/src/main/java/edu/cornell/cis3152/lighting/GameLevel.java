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
package edu.cornell.cis3152.lighting;

import box2dLight.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.physics2.ObstacleSprite;
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
	/** Reference to the character avatar */
	private Avatar avatar;
	/** Reference to the goalDoor (for collision detection) */
	private Exit goalDoor;

	/** Whether or not the level is in debug more (showing off physics) */
	private boolean debug;

	/** All the objects in the world. */
	protected PooledList<ObstacleSprite> sprites  = new PooledList<ObstacleSprite>();

	// LET THE TIGHT COUPLING BEGIN
	/** The Box2D world */
	protected World world;
	/** The boundary of the world */
	protected Rectangle bounds;

	/** The camera defining the RayHandler view; scale is in physics coordinates */
	protected OrthographicCamera raycamera;
	/** The rayhandler for storing lights, and drawing them (SIGH) */
	protected RayHandler rayhandler;
	/** All of the active lights that we loaded from the JSON file */
	private Array<PositionalLight> lights = new Array<PositionalLight>();
	/** The current light source being used.  If -1, there are no shadows */
	private int activeLight;

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
	 * Returns a reference to the lighting rayhandler
	 *
	 * @return a reference to the lighting rayhandler
	 */
	public RayHandler getRayHandler() {
		return rayhandler;
	}

	/**
	 * Returns a reference to the player avatar
	 *
	 * @return a reference to the player avatar
	 */
	public Avatar getAvatar() {
		return avatar;
	}

	/**
	 * Returns a reference to the exit door
	 *
	 * @return a reference to the exit door
	 */
	public Exit getExit() {
		return goalDoor;
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
	 * @param value	whether this level is currently in debug node
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
		world  = null;
		bounds = new Rectangle(0,0,1,1);
		debug  = false;
	}

	/**
	 * Lays out the game geography from the given JSON file
	 *
	 * @param directory 	the asset manager
	 * @param levelFormat	the JSON file defining the level
	 */
	public void populate(AssetDirectory directory, JsonValue levelFormat) {
		float[] pSize = levelFormat.get("world_size").asFloatArray();
		int[] gSize = levelFormat.get("screen_size").asIntArray();

		world = new World(Vector2.Zero,false);
		bounds = new Rectangle(0,0,pSize[0],pSize[1]);
		float units = gSize[1]/pSize[1];

		// Compute the FPS
		int[] fps = levelFormat.get("fps_range").asIntArray();
		maxFPS = fps[1]; minFPS = fps[0];
		timeStep = 1.0f/maxFPS;
		maxSteps = 1.0f + (float)maxFPS/minFPS;
		maxTimePerFrame = timeStep*maxSteps;

		// Create the lighting if appropriate
		if (levelFormat.has("lighting")) {
			initLighting(levelFormat.get("lighting"));
		}
		createPointLights(levelFormat.get("pointlights"));
		createConeLights(levelFormat.get("conelights"));

		// Get the units
		goalDoor = new Exit(directory, levelFormat.get("exit"), units);
		activate(goalDoor);

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

		// Create the dude and attach light sources
	    JsonValue avdata = levelFormat.get("avatar");
		avatar = new Avatar(directory, avdata, units);
		activate(avatar);
		attachLights(avatar);
	}

	/**
	 * Creates the ambient lighting for the level
	 *
	 * This is the amount of lighting that the level has without any light sources.
	 * However, if activeLight is -1, this will be ignored and the level will be
	 * completely visible.
	 *
	 * @param  light	the JSON tree defining the light
	 */
	private void initLighting(JsonValue light) {
		raycamera = new OrthographicCamera(bounds.width,bounds.height);
		raycamera.position.set(bounds.width/2.0f, bounds.height/2.0f, 0);
		raycamera.update();

		RayHandler.setGammaCorrection(light.getBoolean("gamma"));
		RayHandler.useDiffuseLight(light.getBoolean("diffuse"));
		rayhandler = new RayHandler(world, Gdx.graphics.getWidth(), Gdx.graphics.getWidth());
		rayhandler.setCombinedMatrix(raycamera);

		float[] color = light.get("color").asFloatArray();
		rayhandler.setAmbientLight(color[0], color[0], color[0], color[0]);
		int blur = light.getInt("blur");
		rayhandler.setBlur(blur > 0);
		rayhandler.setBlurNum(blur);
	}

	/**
	 * Creates the points lights for the level
	 *
	 * Point lights show light in all direction.  We treat them differently from
	 * cone lights because they have different defining attributes. However, all
	 * lights are added to the lights array. This allows us to cycle through both
	 * the point lights and the cone lights with activateNextLight().
	 *
	 * All lights are deactivated initially. We only want one active light at a
	 * time.
	 *
	 * @param  json	the JSON tree defining the list of point lights
	 */
	private void createPointLights(JsonValue json) {
		JsonValue light = json.child();
	    while (light != null) {
	    	float[] color = light.get("color").asFloatArray();
	    	float[] pos = light.get("pos").asFloatArray();
	    	float dist  = light.getFloat("distance");
	    	int rays = light.getInt("rays");

			PointLight point = new PointLight(rayhandler, rays, Color.WHITE, dist, pos[0], pos[1]);
			point.setColor(color[0],color[1],color[2],color[3]);
			point.setSoft(light.getBoolean("soft"));

			// Create a filter to exclude see through items
			Filter f = new Filter();
			f.maskBits = bitStringToComplement(light.getString("exclude"));
			point.setContactFilter(f);
			point.setActive(false); // TURN ON LATER
			lights.add(point);
	        light = light.next();
	    }
	}

	/**
	 * Creates the cone lights for the level
	 *
	 * Cone lights show light in a cone with a direction.  We treat them differently from
	 * point lights because they have different defining attributes.  However, all lights
	 * are added to the lights array.  This allows us to cycle through both the point
	 * lights and the cone lights with activateNextLight().
	 *
	 * All lights are deactivated initially.  We only want one active light at a time.
	 *
	 * @param  json	the JSON tree defining the list of point lights
	 */
	private void createConeLights(JsonValue json) {
		JsonValue light = json.child();
	    while (light != null) {
	    	float[] color = light.get("color").asFloatArray();
	    	float[] pos = light.get("pos").asFloatArray();
	    	float dist  = light.getFloat("distance");
	    	float face  = light.getFloat("facing");
	    	float angle = light.getFloat("angle");
	    	int rays = light.getInt("rays");

			ConeLight cone = new ConeLight(rayhandler, rays, Color.WHITE, dist, pos[0], pos[1], face, angle);
			cone.setColor(color[0],color[1],color[2],color[3]);
			cone.setSoft(light.getBoolean("soft"));

			// Create a filter to exclude see through items
			Filter f = new Filter();
			f.maskBits = bitStringToComplement(light.getString("exclude"));
			cone.setContactFilter(f);
			cone.setActive(false); // TURN ON LATER
			lights.add(cone);
	        light = light.next();
	    }
	}

	/**
	 * Attaches all lights to the avatar.
	 *
	 * Lights are offset form the center of the avatar according to the initial
	 * position. By default, a light ignores the body. This means that putting
	 * the light inside of these bodies fixtures will not block the light.
	 * However, if a light source is offset outside of the bodies fixtures,
	 * then they will cast a shadow.
	 *
	 * The activeLight is set to be the first element of lights, assuming it is
	 * not empty.
	 */
	public void attachLights(Avatar avatar) {
		for(PositionalLight light : lights) {
			light.attachToBody(avatar.getObstacle().getBody(), light.getX(), light.getY(), light.getDirection());
		}
		if (lights.size > 0) {
			activeLight = 0;
			lights.get(0).setActive(true);
		} else {
			activeLight = -1;
		}
	}

	/**
	 * Activates the next light in the light list.
	 *
	 * If activeLight is at the end of the list, it sets the value to -1,
	 * disabling all shadows. If activeLight is -1, it activates the first light
	 * in the list.
	 */
	public void activateNextLight() {
		if (activeLight != -1) {
			lights.get(activeLight).setActive(false);
		}
		activeLight++;
		if (activeLight >= lights.size) {
			activeLight = -1;
		} else {
			lights.get(activeLight).setActive(true);
		}
	}

	/**
	 * Activates the previous light in the light list.
	 *
	 * If activeLight is at the end of the list, it sets the value to -1,
	 * disabling all shadows. If activeLight is -1, it activates the first light
	 * in the list.
	 */
	public void activatePrevLight() {
		if (activeLight != -1) {
			lights.get(activeLight).setActive(false);
		}
		activeLight--;
		if (activeLight < -1) {
			activeLight = lights.size-1;
		} else if (activeLight > -1) {
			lights.get(activeLight).setActive(true);
		}
	}

	/**
	 * Disposes of all resources for this model.
	 *
	 * Because of all the heavy weight physics stuff, this method is absolutely
	 * necessary whenever we reset a level.
	 */
	public void dispose() {
		for(PositionalLight light : lights) {
			light.remove();
		}
		lights.clear();

		if (rayhandler != null) {
			rayhandler.dispose();
			rayhandler = null;
		}

		for(ObstacleSprite s : sprites) {
			s.getObstacle().deactivatePhysics(world);
		}
		sprites.clear();
		if (world != null) {
			world.dispose();
			world = null;
		}
	}

	/**
	 * Immediately adds the object to the physics world
	 *
	 * param obj The object to add
	 */
	protected void activate(ObstacleSprite sprite) {
		assert inBounds(sprite.getObstacle()) : "Object is not in bounds";
		sprites.add(sprite);
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
		boolean horiz = (bounds.x <= obj.getX() && obj.getX() <= bounds.x+bounds.width);
		boolean vert  = (bounds.y <= obj.getY() && obj.getY() <= bounds.y+bounds.height);
		return horiz && vert;
	}

	/**
	 * Updates all of the models in the level.
	 *
	 * This is borderline controller functionality. However, we have to do this because
	 * of how tightly coupled everything is.
	 *
	 * @param dt the time passed since the last frame
	 */
	public boolean update(float dt) {
		if (fixedStep(dt)) {
			if (rayhandler != null) {
				rayhandler.update();
			}
			avatar.update(dt);
			return true;
		}
		return false;
	}

	/**
	 * Fixes the physics frame rate to be in sync with the animation framerate
	 *
	 * http://gafferongames.com/game-physics/fix-your-timestep/
	 *
	 * @param dt the time passed since the last frame
	 */
	private boolean fixedStep(float dt) {
		if (world == null) return false;

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
	 * @param batch     the sprite batch to draw to
	 * @param camera    the drawing camera
	 */
	public void draw(SpriteBatch batch, Camera camera) {
		// Draw the sprites first (will be hidden by shadows)
		batch.begin(camera);
		for(ObstacleSprite obj : sprites) {
			obj.draw(batch);
		}
		batch.end();

		// Now draw the shadows
		if (rayhandler != null && activeLight != -1) {
			rayhandler.render();
		}

		// Draw debugging on top of everything.
		if (debug) {
			batch.begin(camera);
			for(ObstacleSprite obj : sprites) {
				obj.drawDebug(batch);
			}
			batch.end();
		}
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
		for(int ii = s.length()-1; ii >= 0; ii--) {
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
		for(int ii = s.length()-1; ii >= 0; ii--) {
			if (s.charAt(ii) == '0') {
				value += pos;
			}
			pos *= 2;
		}
		return value;
	}
}
