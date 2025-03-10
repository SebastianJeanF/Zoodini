package edu.cornell.cis3152.lighting.models;

import com.badlogic.gdx.utils.JsonValue;

import edu.cornell.gdiac.assets.AssetDirectory;

/**
 * Player avatar for the plaform game.
 *
 * Note that the constructor does very little. The true initialization happens
 * by reading the JSON value.
 */
public class Octopus extends Avatar {
    /// Whether or not this Otto instance has triggered the blind action
    private boolean inked;
    private float flipScale = 1.0f;

    /**
     * Gets the current value of <code>inked</code>.
     *
     * @return Whether this Otto instance has inked
     */
    public boolean getInked() {
        return inked;
    }

    /**
     * Update the value of <code>inked</code>.
     *
     * @param value What to set the new value of <code>inked</code> to
     */
    public void setInked(boolean value) {
        inked = value;
    }

    public void setFlipScale(float scale) {
        flipScale = scale;
    }

    public Octopus(AssetDirectory directory, JsonValue json, float units) {
        super(AvatarType.OTTO, directory, json, units);
    }
}
