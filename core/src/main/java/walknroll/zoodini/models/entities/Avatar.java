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
package walknroll.zoodini.models.entities;

import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.utils.JsonValue;

import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.SpriteMesh;
import edu.cornell.gdiac.graphics.SpriteSheet;
import edu.cornell.gdiac.physics2.WheelObstacle;
import walknroll.zoodini.models.GameLevel;
import walknroll.zoodini.utils.ZoodiniSprite;
import walknroll.zoodini.utils.animation.Animation;
import walknroll.zoodini.utils.animation.AnimationController;
import walknroll.zoodini.utils.animation.AnimationState;
import walknroll.zoodini.utils.enums.AvatarType;

/**
 * Player avatar for the lighting demo.
 */
public class Avatar extends ZoodiniSprite {
	// Physics constants
	/** The factor to multiply by the input */
	private float force;
	/** The amount to slow the character down */
	private float damping;
	/** The maximum character speed */
	private float maxspeed;

	/** The current horizontal movement of the character */
	private Vector2 movement = new Vector2();

	/** Cache for internal force calculations */
	private Vector2 forceCache = new Vector2();

	private boolean underCamera;

	private boolean underVisionCone;

	private final AvatarType avatarType;

	private boolean flipped = false;

	protected final AnimationController animationController;

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
	 * Returns whether the sprite is flipped horizontally
	 */
	public boolean isFlipped() {
		return flipped;
	}

	/**
	 * flips the sprite horizontally for when user moves left
	 */
	public void flipSprite() {
		flipped = !flipped;
	}

	/**
	 * Creates a new avatar with from the given settings
	 *
	 * @param avatarType The type of this avatar
	 * @param properties The properties of tiled map object
	 * @param units      The physics units for this avatar
	 */
	public Avatar(AvatarType avatarType, MapProperties properties, JsonValue constants, float units) {
		this.avatarType = avatarType;

		float[] pos = new float[2];
		pos[0] = properties.get("x", Float.class) / units;
		pos[1] = properties.get("y", Float.class) / units;
		float radius = constants.getFloat("obstacleRadius");

		obstacle = new WheelObstacle(pos[0], pos[1], radius);
		obstacle.setName(properties.get("type", String.class));
		obstacle.setFixedRotation(false);
		obstacle.setBodyType(BodyType.DynamicBody);
		obstacle.setDensity(1.0f);
		obstacle.setFriction(100.0f);
		obstacle.setRestitution(0.0f);
		obstacle.setPhysicsUnits(units);

		setForce(constants.getFloat("force"));
		setDamping(10.0f);
		setMaxSpeed(constants.getFloat("maxSpeed"));

		Filter filter = new Filter();
		filter.categoryBits = GameLevel.bitStringToShort(constants.getString("category"));
		filter.maskBits = GameLevel.bitStringToComplement(constants.getString("exclude"));
		obstacle.setFilterData(filter);

		float sr = constants.getFloat("spriteRadius") * units;
		mesh = new SpriteMesh(-sr, -sr/2 - 20f, 2 * sr, 2 * sr);

		underCamera = false;
		underVisionCone = false;
		animationController = new AnimationController(AnimationState.IDLE);
	}

	/**
	 * Adds spritesheet to animate for a given state.
	 */
	public void setAnimation(AnimationState state, SpriteSheet sheet, int frameDelay) {
		switch (state) {
			// TODO: frame delays (number of frames elapsed before rendering the next
			// sprite) is set to 16 for all states. This needs to be adjusted.
			case IDLE -> animationController.addAnimation(AnimationState.IDLE,
					new Animation(sheet, 0, sheet.getSize() - 1, frameDelay, true));
			case WALK -> animationController.addAnimation(AnimationState.WALK,
					new Animation(sheet, 0, sheet.getSize() - 1, frameDelay, true));
			case WALK_BLIND -> animationController.addAnimation(AnimationState.WALK_BLIND,
					new Animation(sheet, 0, sheet.getSize() - 1, frameDelay, true));
			case WALK_DOWN -> animationController.addAnimation(AnimationState.WALK_DOWN,
					new Animation(sheet, 0, sheet.getSize() - 1, frameDelay, true));
			case WALK_DOWN_BLIND -> animationController.addAnimation(AnimationState.WALK_DOWN_BLIND,
					new Animation(sheet, 0, sheet.getSize() - 1, frameDelay, true));
			case WALK_UP -> animationController.addAnimation(AnimationState.WALK_UP,
					new Animation(sheet, 0, sheet.getSize() - 1, frameDelay, true));
			case WALK_UP_BLIND -> animationController.addAnimation(AnimationState.WALK_UP_BLIND,
					new Animation(sheet, 0, sheet.getSize() - 1, frameDelay, true));
			default -> {}
		}
	}

	public void resetPhysics() {
		forceCache.setZero();
		movement.setZero();
		applyForce();
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

			// Determine animation based on direction
			float dx = getMovement().x;
			float dy = getMovement().y;

			if (Math.abs(dy) > Math.abs(dx)) {
				// Vertical movement is dominant
				if (dy > 0) {
					if (getAvatarType() == AvatarType.ENEMY && ((Guard) this).isInkBlinded()) {
						animationController.setState(AnimationState.WALK_UP_BLIND);
					} else {
						animationController.setState(AnimationState.WALK_UP);
					}
				} else {
					if (getAvatarType() == AvatarType.ENEMY && ((Guard) this).isInkBlinded()) {
						animationController.setState(AnimationState.WALK_DOWN_BLIND);
					} else {
						animationController.setState(AnimationState.WALK_DOWN);
					}
				}
			} else {
				// Horizontal movement is dominant
				if (getAvatarType() == AvatarType.ENEMY && ((Guard) this).isInkBlinded()) {
					animationController.setState(AnimationState.WALK_BLIND);
				} else {
					animationController.setState(AnimationState.WALK);
				}
			}
		} else {
			animationController.setState(AnimationState.IDLE);
		}
	}

	// Method to manually set animation state (for attacks, jumps, etc.)
	public void setAnimationState(AnimationState state) {
		animationController.setState(state);
	}

	/**
	 * Updates the object's physics state (NOT GAME LOGIC).
	 *
	 * We use this method to reset cooldowns.
	 *
	 * @param dt number of seconds since last animation frame
	 */
	public void update(float dt) {
        super.update(dt);

        // Update animation controller
		animationController.update();

		// This is the key fix - update the sprite reference itself
		SpriteSheet currentSheet = animationController.getCurrentSpriteSheet();
		if (currentSheet != null) {
			sprite = currentSheet; // Switch to the current animation's spritesheet
		}

		// Now setting the frame will work correctly
		if (sprite != null) {
			sprite.setFrame(animationController.getCurrentFrame());
		}
	}

	@Override
	public void draw(SpriteBatch batch) {
		if (this.obstacle != null && this.mesh != null) {
			float x = this.obstacle.getX();
			float y = this.obstacle.getY();
			float a = this.obstacle.getAngle();
			float u = this.obstacle.getPhysicsUnits();

			this.transform.idt();
			this.transform.preTranslate(x * u, y * u);
			if (flipped) {
				this.transform.scale(-1.0F, 1.0F);
			}
			batch.setTextureRegion(this.sprite);
			batch.drawMesh(this.mesh, this.transform, false);
			batch.setTexture(null);
		}
	}

	public void setUnderCamera(boolean underCamera) {
		this.underCamera = underCamera;
	}

	public void setUnderVisionCone(boolean underVisionCone) {
		this.underVisionCone = underVisionCone;
	}

	public boolean isUnderCamera() {
		return underCamera;
	}

	public boolean isInGuardVisionCone() {
		return underVisionCone;
	}
}
