package walknroll.zoodini.models.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.SpriteSheet;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.math.PathExtruder;
import edu.cornell.gdiac.math.PathFactory;
import edu.cornell.gdiac.physics2.ObstacleData;
import walknroll.zoodini.models.nonentities.Key;
import walknroll.zoodini.utils.DebugPrinter;
import walknroll.zoodini.utils.animation.Animation;
import walknroll.zoodini.utils.animation.AnimationState;
import walknroll.zoodini.utils.enums.AvatarType;

public class Cat extends PlayableAvatar{
    /// Whether or not this Gar instance has triggered the meow action
    private boolean meowed;
    private boolean justMeowed;
    private final float abilityRange;
    private boolean currentlyAiming = false;

    // Ring effect properties
    private Vector2 centerPosition; // In Physics Units
    private float currentRadius;
    private float expansionSpeed;
    private boolean isRingActive;
    private Color ringColor;
    private float ringThickness;
    private Affine2 affineCache;

    // Cooldown properties
    private float meowCooldown; // Total cooldown duration
    private float meowCooldownRemaining; // Time remaining on cooldown
    private boolean onCooldown; // Whether ability is on cooldown

    // Keys of doors
    private Array<Key> keys;

    PathFactory pf = new PathFactory();
    PathExtruder pe = new PathExtruder();

    public Cat(MapProperties properties, JsonValue constants, float units) {
        super(AvatarType.CAT, properties, constants, units);
        this.abilityRange = constants.getFloat("abilityRange");

        // Initialize ring effect properties
        expansionSpeed = constants.getFloat("meowExpansionSpeed");
        ringThickness = 0.3f;
        ringColor = new Color(211f, 211f, 211f, 0.5f); // Semi-transparent green
        currentRadius = 0f;
        centerPosition = new Vector2(0, 0);
        isRingActive = false;
        affineCache = new Affine2();

        // Initialize cooldown properties
        meowCooldown = constants.getFloat("abilityCooldown"); // 10 seconds default
        meowCooldownRemaining = 0;
        onCooldown = false;
        keys = new Array<Key>();
    }

    public void assignKey(Key key) {
        keys.add(key);
    }

    public Array<Key> getKeys() {
        return keys;
    }

    public float getAbilityRange() {
        return abilityRange;
    }

    /**
     * Checks if the meow ability is available (not on cooldown)
     *
     * @return true if the ability can be used
     */
    public boolean canUseAbility() {
        return !onCooldown;
    }

    /**
     * Gets the remaining cooldown time as a percentage (0-1)
     *
     * @return cooldown progress where 0 means ready and 1 means full cooldown
     */
    public float getCooldownProgress() {
        return meowCooldownRemaining / meowCooldown;
    }

    public float getMeowCooldownRemaining() {
        return meowCooldownRemaining;
    }

    /**
     * Activates the ring effect
     */
    public void activateRing() {
        if (!isRingActive) {
            isRingActive = true;
            currentRadius = 0f;
            centerPosition.set(getPosition().x, getPosition().y);
        }
    }

    public void updateJustMeowed() {
        if (meowed && !justMeowed) {
            justMeowed = true;
        } else if (justMeowed) {
            justMeowed = false;
        }
    }

    public boolean didJustMeow() {
        return justMeowed;
    }

    /**
     * Adds spritesheet to animate for a given state.
     */
    @Override
    public void setAnimation(AnimationState state, SpriteSheet sheet) {
        switch (state) {
            // TODO: frame delays (number of frames elapsed before rendering the next
            // sprite) is set to 16 for all states. This needs to be adjusted.
            case IDLE -> animationController.addAnimation(AnimationState.IDLE,
                    new Animation(sheet, 0, sheet.getSize() - 1, 16, true));
            case WALK -> animationController.addAnimation(AnimationState.WALK,
                    new Animation(sheet, 0, sheet.getSize() - 1, 4, true));
            case WALK_DOWN -> animationController.addAnimation(AnimationState.WALK_DOWN,
                    new Animation(sheet, 0, sheet.getSize() - 1, 8, true));
            case WALK_UP -> animationController.addAnimation(AnimationState.WALK_UP,
                    new Animation(sheet, 0, sheet.getSize() - 1, 6, true));
        }
    }

    @Override
    public void update(float dt) {
        super.update(dt);

        // Update ring animation
        if (isRingActive) {
            currentRadius += expansionSpeed * dt;

            if (currentRadius >= this.abilityRange) {
                isRingActive = false;
            }
        }

        // Update cooldown timer
        if (onCooldown) {
            meowCooldownRemaining -= dt;

            // Reset cooldown when timer expires
            if (meowCooldownRemaining <= 0) {
                meowCooldownRemaining = 0;
                onCooldown = false;
            }
        }

        updateJustMeowed();
    }

    @Override
    public void draw(SpriteBatch batch) {
        setAngle(0);
        super.draw(batch);

        // Draw expanding ring if active
        if (isRingActive) {
            // Save original color
            Color originalColor = batch.getColor().cpy();

            // Set color for the ring
            batch.setColor(ringColor);

            // Create n-gon path for the ring
            Path2 ringPath = pf.makeNgon(centerPosition.x, centerPosition.y, currentRadius, 64);

            // Create extruder for the ring outline
            pe.set(ringPath);
            pe.calculate(ringThickness);

            // Set up affine transformation
            affineCache.idt();
            affineCache.scale(obstacle.getPhysicsUnits(), obstacle.getPhysicsUnits());

            // Draw the ring
            batch.draw((TextureRegion) null, pe.getPolygon(), affineCache);

            // Restore original color
            batch.setColor(originalColor);
        }
    }

    @Override
    public boolean isCurrentlyAiming() {
        return this.currentlyAiming;
    }

    @Override
    public void setCurrentlyAiming(boolean value) {
        this.currentlyAiming = value;
    }

    @Override
    public boolean didFire() {
        return meowed;
    }

    @Override
    public void setDidFire(boolean value) {
        // Only allow meowing if not on cooldown
        if (value && onCooldown) {
            return; // Can't meow while on cooldown
        }

        meowed = value;

        // Start cooldown and activate ring when cat meows
        if (value) {
            DebugPrinter.println("here");
            DebugPrinter.println("ring active: " + isRingActive);
            activateRing();
            onCooldown = true;
            meowCooldownRemaining = meowCooldown;
        }
    }
}
