package walknroll.zoodini.models.entities;

import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.SpriteMesh;

/**
 * Player avatar for the plaform game.
 *
 * Note that the constructor does very little. The true initialization happens
 * by reading the JSON value.
 */
public class Octopus extends Avatar {
    /// Whether or not this Otto instance has triggered the blind action
    private boolean inked;
    private final float OCTOPUS_IMAGE_SCALE = 1.25f;
    private final float abilityRange;
    /// Whether this Octopus is currently aiming at a target
    private boolean currentlyAiming;

    /// The direction this Octopus is aiming in
    private Vector2 target;

    /// Whether this Octopus has fired an ink projectile
    private boolean didFire;

    /// The initial amount of ink
    private float inkCapacity;

    /// The amount of ink consumed per ability usage
    private float inkUsage;

    /// The amount of ink regenerated per second
    private float inkRegen;

    /// The amount of ink this octopus has
    private float inkRemaining;

    public float getAbilityRange() {
        return abilityRange;
    }

    public boolean didFire() {
        return didFire;
    }

    public void setDidFire(boolean didFire) {
        this.didFire = didFire;
    }

    public Vector2 getTarget() {
        return target.cpy();
    }

    public void setTarget(Vector2 aimVector) {
        this.target.set(aimVector);
    }

    /**
     * Gets the current value of <code>inked</code>.
     *
     * @return Whether this Otto instance has inked
     */
    public boolean getInked() {
        return inked;
    }

    /**
     * Gets the current value of <code>currentlyAiming</code>.
     *
     * @return Whether this Octopus instance is currently aiming
     */
    public boolean isCurrentlyAiming() {
        return currentlyAiming;
    }

    /**
     * Update the value of <code>currentlyAiming</code>.
     *
     * @param value Whether the Octopus is currently aiming
     */
    public void setCurrentlyAiming(boolean value) {
        this.currentlyAiming = value;
    }

    public float getInkRemaining() {
        return this.inkRemaining;
    }

    public float getInkCapacity() {
        return inkCapacity;
    }

    /**
     * Returns whether or not this Octopus has enough ink resource to use an ability
     *
     * @return
     */
    public boolean canUseAbility() {
        return inkRemaining >= inkUsage;
    }

    /**
     * Consume ink resource corresponding to one ability usage
     */
    public void consumeInk() {
        this.inkRemaining -= inkUsage;
    }

    /**
     * Regenerate one game tick's worth of ink points
     */
    public void regenerateInk(float dt) {
        this.inkRemaining = Math.min(inkCapacity, this.inkRemaining + dt * inkRegen);
    }

    public Octopus(AssetDirectory directory, MapProperties properties, JsonValue globals, float units) {
        super(AvatarType.OCTOPUS, directory, properties, globals, units);
        float r = properties.get("spriteRadius", Float.class) * OCTOPUS_IMAGE_SCALE * units;
        mesh = new SpriteMesh(-r, -r, 2 * r, 2 * r);
        target = new Vector2();
        this.abilityRange = properties.get("abilityRange", Float.class);
        this.inkRemaining = properties.get("inkCapacity", Float.class);
        this.inkRegen = properties.get("inkRegen", Float.class);
        this.inkUsage = properties.get("inkUsage", Float.class);
        this.inkCapacity = inkRemaining;
    }

    @Override
    public void update(float dt) {
        super.update(dt);
    }

    @Override
    public void draw(SpriteBatch batch) {
        setAngle(0);
        super.draw(batch);
    }
}
