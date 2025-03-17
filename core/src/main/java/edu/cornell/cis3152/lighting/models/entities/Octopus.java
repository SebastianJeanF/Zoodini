package edu.cornell.cis3152.lighting.models.entities;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonValue;

import edu.cornell.cis3152.lighting.models.entities.Avatar;
import edu.cornell.gdiac.assets.AssetDirectory;

/**
 * Player avatar for the plaform game.
 *
 * Note that the constructor does very little. The true initialization happens
 * by reading the JSON value.
 */
public class Octopus extends Avatar {
    /// Whether this Octopus is currently aiming at a target
    private boolean currentlyAiming;

    /// The direction this Octopus is aiming in
    private Vector2 target;

    /// Whether this Octopus has fired an ink projectile
    private boolean didFire;

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

    public Octopus(AssetDirectory directory, JsonValue json, JsonValue globals, float units) {
        super(AvatarType.OCTOPUS, directory, json, globals, units);
        target = new Vector2();
    }
}
