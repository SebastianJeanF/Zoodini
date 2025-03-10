/*
 * Avatar.java
 *
 * This is a game Avatarthat allows us to read its properties from a JSON file.
 * As a result, it has a lot more getter and setter "hooks" than the Avatar in
 * the physics lab.
 *
 * While the avatar can support lights, these are completely decoupled from this
 * object. The avatar is not aware of any of the lights. These are attached to
 * the associated body and move with the body.
 *
 * @author: Walker M. White
 * @version: 2/15/2025
 */
package edu.cornell.cis3152.lighting.models;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteMesh;
import edu.cornell.gdiac.graphics.SpriteSheet;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.physics2.WheelObstacle;

/**
 * Player avatar for the lighting demo.
 */
public class Avatar extends ObstacleSprite {
	// Physics constants
	/** The factor to multiply by the input */
	private float force;
	/** The amount to slow the character down */
	private float damping;
	/** The maximum character speed */
	private float maxspeed;

	/** The current horizontal movement of the character */
	private Vector2 movement = new Vector2();
	/** Whether or not to animate the current frame */
	private boolean animate = false;

	/** How many frames until we can walk again */
	private int walkCool;
	/** The standard number of frames to wait until we can walk again */
	private int walkLimit;
	/** The initial starting frame of this sprite */
	private int startFrame;

	/** The rotational center of the filmstrip */
	private Vector2 center;

	/** Cache for internal force calculations */
	private Vector2 forceCache = new Vector2();

	public enum AvatarType {
		GAR,
		OTTO,
		ENEMY
	}

	private AvatarType avatarType;

	/**
	 * Returns the avatar type.
	 *
	 * Could be accomplished by runtime type checking but don't want to spend
	 * resources on that
	 *
	 * @return the type of this avatar
	 */
	public AvatarType getAvatarType() {
		return avatarType;
	}

    /**
     * Returns the position of this avatar.
     *
     * @return the position of this avatar.
     */
    public Vector2 getPosition() {
        return obstacle.getPosition();
    }

    public void setAngle(float angle) {
        obstacle.setAngle(angle);
    }

    public float getAngle() {
        return obstacle.getAngle();
    }

	/**
	 * Returns the directional movement of this character.
	 *
	 * This is the result of input times the avatar force.
	 *
	 * @return the directional movement of this character.
	 */
	public Vector2 getMovement() {
		return movement;
	}

	/**
	 * Sets the directional movement of this character.
	 *
	 * This is the result of input times the avatar force.
	 *
	 * @param value the directional movement of this character.
	 */
	public void setMovement(Vector2 value) {
		setMovement(value.x, value.y);
	}

	/**
	 * Sets the directional movement of this character.
	 *
	 * This is the result of input times the avatar force.
	 *
	 * @param dx the horizontal movement of this character.
	 * @param dy the horizontal movement of this character.
	 */
	public void setMovement(float dx, float dy) {
		movement.set(dx, dy);
	}

	/**
	 * Returns how much force to apply to get the avatar moving
	 *
	 * Multiply this by the input to get the movement value.
	 *
	 * @return how much force to apply to get the avatar moving
	 */
	public float getForce() {
		return force;
	}

	/**
	 * Sets how much force to apply to get the avatar moving
	 *
	 * Multiply this by the input to get the movement value.
	 *
	 * @param value how much force to apply to get the avatar moving
	 */
	public void setForce(float value) {
		force = value;
	}

	/**
	 * Returns how hard the brakes are applied to stop the avatar from moving
	 *
	 * @return how hard the brakes are applied to stop the avatar from moving
	 */
	public float getDamping() {
		return damping;
	}

	/**
	 * Sets how hard the brakes are applied to stop the avatar from moving
	 *
	 * @param value how hard the brakes are applied to stop the avatar
	 */
	public void setDamping(float value) {
		damping = value;
	}

	/**
	 * Returns the upper limit on avatar left-right movement.
	 *
	 * This does NOT apply to vertical movement.
	 *
	 * @return the upper limit on avatar left-right movement.
	 */
	public float getMaxSpeed() {
		return maxspeed;
	}

	/**
	 * Sets the upper limit on avatar left-right movement.
	 *
	 * This does NOT apply to vertical movement.
	 *
	 * @param value the upper limit on avatar left-right movement.
	 */
	public void setMaxSpeed(float value) {
		maxspeed = value;
	}

	/**
	 * Returns the current animation frame of this avatar.
	 *
	 * @return the current animation frame of this avatar.
	 */
	public float getFrame() {
		return sprite.getFrame();
	}

	/**
	 * Sets the animation frame of this avatar.
	 *
	 * @param value animation frame of this avatar.
	 */
	public void setFrame(int value) {
		sprite.setFrame(value);
	}

	/**
	 * Returns the cooldown limit between walk animations
	 *
	 * @return the cooldown limit between walk animations
	 */
	public int getWalkLimit() {
		return walkLimit;
	}

	/**
	 * Sets the cooldown limit between walk animations
	 *
	 * @param value the cooldown limit between walk animations
	 */
	public void setWalkLimit(int value) {
		walkLimit = value;
	}

	/**
	 * Creates a new avatar with from the given settings
	 *
	 * @param avatarType The type of this avatar
	 * @param directory  The asset directory (for textures, etc)
	 * @param json       The JSON values defining this avatar
	 * @param units      The physics units for this avatar
	 */
	public Avatar(AvatarType avatarType, AssetDirectory directory, JsonValue json, float units) {
		this.avatarType = avatarType;

		float[] pos = json.get("pos").asFloatArray();
		float radius = json.getFloat("radius");
		obstacle = new WheelObstacle(pos[0], pos[1], radius);
		obstacle.setName(json.name());
		obstacle.setFixedRotation(false);

		// Technically, we should do error checking here.
		// A JSON field might accidentally be missing
		obstacle.setBodyType(json.getString("bodytype").equals("static") ? BodyDef.BodyType.StaticBody
				: BodyDef.BodyType.DynamicBody);
		obstacle.setDensity(json.getFloat("density"));
		obstacle.setFriction(json.getFloat("friction"));
		obstacle.setRestitution(json.getFloat("restitution"));
		obstacle.setPhysicsUnits(units);

		setForce(json.getFloat("force"));
		setDamping(json.getFloat("damping"));
		setMaxSpeed(json.getFloat("maxspeed"));
		setWalkLimit(json.getInt("walklimit"));

		// Create the collision filter (used for light penetration)
		short collideBits = GameLevel.bitStringToShort(json.getString("collide"));
		short excludeBits = GameLevel.bitStringToComplement(json.getString("exclude"));
		Filter filter = new Filter();
		filter.categoryBits = collideBits;
		filter.maskBits = excludeBits;
		obstacle.setFilterData(filter);

		setDebugColor(ParserUtils.parseColor(json.get("debug"), Color.WHITE));

		String key = json.getString("texture");
		startFrame = json.getInt("startframe");
		sprite = directory.getEntry(key, SpriteSheet.class);
		sprite.setFrame(startFrame);

		float r = json.getFloat("spriterad") * units;
		mesh = new SpriteMesh(-r, -r, 2 * r, 2 * r);
	}

	/**
	 * Applies the force to the body of this avatar
	 *
	 * This method should be called after the force attribute is set.
	 */
	public void applyForce() {
		if (!obstacle.isActive()) {
			return;
		}

		// Only walk or spin if we allow it
		obstacle.setLinearVelocity(Vector2.Zero);
		obstacle.setAngularVelocity(0.0f);

		// Apply force for movement
		if (getMovement().len2() > 0f) {
			forceCache.set(getMovement());
			obstacle.getBody().applyForce(forceCache, obstacle.getPosition(), true);
			animate = true;
		} else {
			animate = false;
		}
	}

	/**
	 * Updates the object's physics state (NOT GAME LOGIC).
	 *
	 * We use this method to reset cooldowns.
	 *
	 * @param dt number of seconds since last animation frame
	 */
	public void update(float dt) {
		// Animate if necessary
		if (animate && walkCool == 0) {
			if (sprite != null) {
				int next = (sprite.getFrame() + 1) % sprite.getSize();
				sprite.setFrame(next);
			}
			walkCool = walkLimit;
		} else if (walkCool > 0) {
			walkCool--;
		} else if (!animate) {
			if (sprite != null) {
				sprite.setFrame(startFrame);
			}
			walkCool = 0;
		}

		obstacle.update(dt);
	}
}
