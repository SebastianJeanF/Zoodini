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
public class Door extends ZoodiniSprite {
    /** Whether this door is locked */
    private boolean locked;
    /** The texture to use when the door is locked */
    private TextureRegion lockedTexture;
    /** The texture to use when the door is unlocked */
    private TextureRegion unlockedTexture;
    /** Time required to unlock this door (seconds) */
    private float remainingTimeToUnlock;
    /** Whether this door is being unlocked at current frame*/
    private boolean isUnlocking;
    private static final float UNLOCK_DURATION = 3.0f;


    public boolean isUnlocking() {
        return isUnlocking;
    }

    public float getUnlockDuration(){return UNLOCK_DURATION;}

    public void setUnlocking(boolean unlocking) {
        isUnlocking = unlocking;
    }

    public float getRemainingTimeToUnlock(){
        return remainingTimeToUnlock;
    }

    public void setRemainingTimeToUnlock(float t){
        remainingTimeToUnlock = t;
    }

    public void resetTimer(){
        remainingTimeToUnlock = 3; //TODO: set this using json somehow.
    }

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
        // When the door is locked, disable the sensor (door acts as a wall).
        // When unlocked, enable the sensor (door lets you pass through).
        obstacle.setSensor(!locked);
        setTextureRegion(locked ? lockedTexture : unlockedTexture);
    }



	/**
	 * Creates a door with the given settings
	 *
	 * @param directory The asset directory (for textures, etc)
	 * @param json      The JSON values defining this avatar
	 * @param units     The physics units for this avatar
	 */
	public Door(AssetDirectory directory, JsonValue json, JsonValue globals, float units) {
		float[] pos = json.get("pos").asFloatArray();
		float[] size = globals.get("size").asFloatArray();

		obstacle = new BoxObstacle(pos[0], pos[1], size[0], size[1]);
		obstacle.setName(json.name());
		obstacle.setSensor(false);
		obstacle.setPhysicsUnits(units);

		float w = size[0] * units;
		float h = size[1] * units;
		mesh = new SpriteMesh(-w / 2, -h / 2, w, h);

		// Technically, we should do error checking here.
		// A JSON field might accidentally be missing
		obstacle.setBodyType(globals.get("bodytype").asString().equals("static") ? BodyDef.BodyType.StaticBody
				: BodyDef.BodyType.DynamicBody);
		obstacle.setDensity(globals.get("density").asFloat());
		obstacle.setFriction(globals.get("friction").asFloat());
		obstacle.setRestitution(globals.get("restitution").asFloat());

		// Create the collision filter (used for light penetration)
		short collideBits = GameLevel.bitStringToShort(globals.get("collide").asString());
		short excludeBits = GameLevel.bitStringToComplement(globals.get("exclude").asString());
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


        lockedTexture = new TextureRegion(directory.getEntry(lockedKey, Texture.class));
        unlockedTexture = new TextureRegion(directory.getEntry(unlockedKey, Texture.class));

        // Set initial state (locked by default)
        setLocked(true);
        setTextureRegion(lockedTexture);
        resetTimer(); //TODO: get this from json
	}
}
