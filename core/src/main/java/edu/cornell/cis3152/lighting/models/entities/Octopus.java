package edu.cornell.cis3152.lighting.models.entities;

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
    private boolean aiming;

    /**
     * Gets the current value of <code>aiming</code>.
     *
     * @return Whether this Octopus instance is aiming
     */
    public boolean isAiming() {
        return aiming;
    }

    /**
     * Update the value of <code>aiming</code>.
     *
     * @param value Whether the Octopus is currently aiming
     */
    public void setAiming(boolean value) {
        this.aiming = value;
    }

    public Octopus(AssetDirectory directory, JsonValue json, float units) {
        super(AvatarType.OCTOPUS, directory, json, units);
    }
}
