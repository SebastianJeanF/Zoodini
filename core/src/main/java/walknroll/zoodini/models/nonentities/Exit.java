/*
 * Exit.java
 *
 * This is a refactored version of the exit door from Lab 4.  We have made it a
 * specialized class so that we can import its properties from a JSON file.
 *
 * @author: Walker M. White
 * @version: 2/15/2025
 */
package walknroll.zoodini.models.nonentities;

import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.physics.box2d.*;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteMesh;
import edu.cornell.gdiac.physics2.*;
import walknroll.zoodini.models.GameLevel;
import walknroll.zoodini.utils.ZoodiniSprite;

/**
 * A sensor obstacle representing the end of the level.
 *
 * This class is largely just a constructor. Everything else is provided by the
 * subclass.
 */
public class Exit extends ZoodiniSprite {
    /** Whether this door is locked */
    private boolean locked;
    /** The texture to use when the door is locked */
    private TextureRegion lockedTexture;
    /** The texture to use when the door is unlocked */
    private TextureRegion unlockedTexture;

    /**
     * Returns whether this door is locked.
     *
     * @return whether this door is locked
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * Sets whether this door is locked.
     * Updates the texture accordingly.
     *
     * @param value whether this door is locked
     */
    public void setLocked(boolean value) {
        locked = value;
        setTextureRegion(locked ? lockedTexture : unlockedTexture);
    }



    /**
     * Creates a door with the given settings
     *
     * @param directory The asset directory (for textures, etc)
     * @param units     The physics units for this avatar
     */
    public Exit(AssetDirectory directory, MapProperties properties, JsonValue globals, float units) {
        float[] pos = {properties.get("x", Float.class), properties.get("y", Float.class)};
        float size = properties.get("size", Float.class);

        obstacle = new BoxObstacle(pos[0], pos[1], size, size);
        obstacle.setName(properties.get("type", String.class));
        obstacle.setSensor(true);
        obstacle.setPhysicsUnits(units);

        float w = size * units;
        float h = size * units;
        mesh = new SpriteMesh(-w / 2, -h / 2, w, h);

        // Technically, we should do error checking here.
        // A JSON field might accidentally be missing
        obstacle.setBodyType(BodyType.StaticBody);
        obstacle.setDensity(0.0f);
        obstacle.setFriction(0.0f);
        obstacle.setRestitution(0.0f);

        // Create the collision filter (used for light penetration)
        short collideBits = GameLevel.bitStringToShort(properties.get("category", String.class));
        short excludeBits = GameLevel.bitStringToComplement(properties.get("exclude", String.class));
        Filter filter = new Filter();
        filter.categoryBits = collideBits;
        filter.maskBits = excludeBits;
        obstacle.setFilterData(filter);

        setDebugColor(ParserUtils.parseColor(globals.get("debug"), Color.WHITE));

        // Get textures for locked and unlocked states
        String lockedKey = globals.has("locked_texture") ?
            globals.get("locked_texture").asString() :
            globals.get("texture").asString();
        String unlockedKey = globals.get("texture").asString();


//        lockedTexture = new TextureRegion(directory.getEntry(lockedKey, Texture.class));
        unlockedTexture = new TextureRegion(directory.getEntry(unlockedKey, Texture.class));

        // Set initial state (locked by default)
        locked = true;
        setTextureRegion(unlockedTexture);
    }
}
