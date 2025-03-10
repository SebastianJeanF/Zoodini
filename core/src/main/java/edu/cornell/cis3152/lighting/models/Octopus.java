package edu.cornell.cis3152.lighting.models;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonValue;

import edu.cornell.cis3152.lighting.controllers.InputController;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.math.Path2;

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

    @Override
    public void draw(SpriteBatch batch) {
        batch.setTexture(null);
        batch.setColor(Color.PURPLE);
        float x = this.obstacle.getX();
        float y = this.obstacle.getY();
        float u = this.obstacle.getPhysicsUnits();
        Rectangle rect = new Rectangle(x, y, x + 100f, y + 100f);
        this.transform.idt();
        var input = InputController.getInstance();
        float a = getPosition().angleRad(input.getAiming());
        this.transform.preRotate((float)((double)(a * 180.0F) / Math.PI));
        this.transform.preTranslate(x * u, y * u);
        batch.fill(rect, transform);
        batch.setColor(Color.WHITE);
        System.out.println("tried to draw the funny line");
        super.draw(batch);
    }
}
