package walknroll.zoodini.models.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.math.PathExtruder;
import edu.cornell.gdiac.math.PathFactory;
import walknroll.zoodini.models.nonentities.Key;

import java.util.ArrayList;

public class Cat extends Avatar {
    /// Whether or not this Gar instance has triggered the meow action
    private boolean meowed;
    private final float abilityRange;

    // Ring effect properties
    private float currentRadius;
    private float maxRadius;
    private float expansionSpeed;
    private boolean isRingActive;
    private Color ringColor;
    private float ringThickness;
    private Affine2 affineCache;


    // Cooldown properties
    private float meowCooldown;          // Total cooldown duration
    private float meowCooldownRemaining; // Time remaining on cooldown
    private boolean onCooldown;          // Whether ability is on cooldown

    // Keys of doors
    private Array<Key> keys;

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
     * Gets the current value of <code>meowed</code>.
     *
     * @return Whether this Gar instance has meowed
     */
    public boolean getMeowed() {
        return meowed;
    }

    /**
     * Checks if the meow ability is available (not on cooldown)
     *
     * @return true if the ability can be used
     */
    public boolean canMeow() {
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

    /**
     * Update the value of <code>meowed</code>.
     *
     * @param value What to set the new value of <code>meowed</code> to
     */
    public void setMeowed(boolean value) {
        // Only allow meowing if not on cooldown
        if (value && onCooldown) {
            return; // Can't meow while on cooldown
        }

        meowed = value;

        // Start cooldown and activate ring when cat meows
        if (value) {
            activateRing();
            onCooldown = true;
            meowCooldownRemaining = meowCooldown;
        }
    }

    public Cat(AssetDirectory directory, MapProperties properties, JsonValue globals, float units) {
        super(AvatarType.CAT, directory, properties, globals, units);
        this.abilityRange = globals.getFloat("abilityRange", 5.0f);

        // Initialize ring effect properties
        maxRadius = abilityRange;
        expansionSpeed = properties.get("meowExpandSpeed", Float.class); //1 m/s
        ringThickness = globals.getFloat("ringThickness", 0.3f);
        ringColor = new Color(211f, 211f, 211f, 0.5f); // Semi-transparent green
        currentRadius = 0f;
        isRingActive = false;
        affineCache = new Affine2();

        // Initialize cooldown properties
        meowCooldown = globals.getFloat("meowCooldown", 10.0f); // 10 seconds default
        meowCooldownRemaining = 0;
        onCooldown = false;
        keys = new Array<Key>();
    }

    /**
     * Activates the ring effect
     */
    public void activateRing() {
        if (!isRingActive) {
            isRingActive = true;
            currentRadius = 0f;
        }
    }

    @Override
    public void update(float dt) {
        super.update(dt);

        // Update ring animation
        if (isRingActive) {
            currentRadius += expansionSpeed * dt;

            if (currentRadius >= maxRadius) {
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
    }


    PathFactory pf = new PathFactory();
    PathExtruder pe = new PathExtruder();

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

            // Draw ring using PathFactory and PathExtruder
            float x = getPosition().x;
            float y = getPosition().y;

            // Create n-gon path for the ring
            Path2 ringPath = pf.makeNgon(x, y, currentRadius, 64);

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
}
