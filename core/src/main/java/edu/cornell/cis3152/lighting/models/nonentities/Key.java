package edu.cornell.cis3152.lighting.models.nonentities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.utils.JsonValue;

import edu.cornell.cis3152.lighting.models.GameLevel;
import edu.cornell.cis3152.lighting.utils.ZoodiniSprite;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteMesh;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.graphics.SpriteBatch;

/**
 * A sensor obstacle representing a key that can be picked up.
 */
public class Key extends ZoodiniSprite {

    /** Whether this key has been collected by the player */
    private boolean collected;

    /**
     * Returns whether this key has been collected.
     *
     * @return whether this key has been collected
     */
    public boolean isCollected() {
        return collected;
    }

    /**
     * Sets whether this key has been collected.
     *
     * @param value whether this key has been collected
     */
    public void setCollected(boolean value) {
        collected = value;

    }

    /**
     * Creates a key with the given settings
     *
     * @param directory The asset directory (for textures, etc)
     * @param json      The JSON values defining this key
     * @param globals   The JSON values defining global key properties
     * @param units     The physics units for this key
     */
    public Key(AssetDirectory directory, JsonValue json, JsonValue globals, float units) {
        float[] pos = json.get("pos").asFloatArray();
        float[] size = globals.get("size").asFloatArray();

        obstacle = new BoxObstacle(pos[0], pos[1], size[0], size[1]);
        obstacle.setName(json.name());
        obstacle.setSensor(true); // Keys should be sensors (no physical collision)
        obstacle.setPhysicsUnits(units);

        float w = size[0] * units;
        float h = size[1] * units;
        mesh = new SpriteMesh(-w / 2, -h / 2, w, h);

        // Set physics properties from JSON
        obstacle.setBodyType(globals.get("bodytype").asString().equals("static") ?
            BodyDef.BodyType.StaticBody : BodyDef.BodyType.DynamicBody);
        obstacle.setDensity(globals.get("density").asFloat());
        obstacle.setFriction(globals.get("friction").asFloat());
        obstacle.setRestitution(globals.get("restitution").asFloat());

        // Create the collision filter
        short collideBits = GameLevel.bitStringToShort(globals.get("collide").asString());
        short excludeBits = GameLevel.bitStringToComplement(globals.get("exclude").asString());
        Filter filter = new Filter();
        filter.categoryBits = collideBits;
        filter.maskBits = excludeBits;
        obstacle.setFilterData(filter);

        setDebugColor(ParserUtils.parseColor(globals.get("debug"), Color.YELLOW));

        // Get texture
        String key = globals.get("texture").asString();
        TextureRegion texture = new TextureRegion(directory.getEntry(key, Texture.class));
        setTextureRegion(texture);

        collected = false;
    }
    /**
     * Overrides the draw method to make the key invisible when collected
     */
    @Override
    public void draw(SpriteBatch batch) {
        if (!collected) {
            super.draw(batch);
        }
    }

}
