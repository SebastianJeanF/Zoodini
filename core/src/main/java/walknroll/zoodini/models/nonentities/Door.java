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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.physics.box2d.*;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.SpriteMesh;
import edu.cornell.gdiac.physics2.*;
import java.util.Iterator;
import walknroll.zoodini.models.GameLevel;
import walknroll.zoodini.models.entities.Avatar;
import walknroll.zoodini.models.entities.PlayableAvatar;
import walknroll.zoodini.utils.CircleTimer;
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
    /** Tiled MapObject for this door*/
    private MapObject mapObject;

    private CircleTimer unlockTimer;
    private boolean showUnlockTimer = false;

    private short collideBits;
    private short excludeBitsLocked;
    private short excludeBitsUnlocked;
    private float units;

    private PlayableAvatar unlocker;


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
        remainingTimeToUnlock = 3.0f; //TODO: set this using json somehow.
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
        Filter filter = new Filter();
        filter.categoryBits = this.collideBits;
        filter.maskBits = locked ? this.excludeBitsLocked : this.excludeBitsUnlocked;
        obstacle.setFilterData(filter);
        // obstacle.setSensor(!locked);
        setTextureRegion(locked ? lockedTexture : unlockedTexture);
    }

    /**
     * Sets who is the most recent character to
     * attempt to unlock this door.
     * @param avatar the character that is trying to unlock this door
     */
    public void setUnlocker(PlayableAvatar avatar){
        unlocker = avatar;
    }

	/**
	 * Creates a door with the given settings
	 *
	 * @param directory The asset directory (for textures, etc)
	 * @param units     The physics units for this avatar
	 */
	public Door(AssetDirectory directory, MapProperties properties, JsonValue constants, float units) {
		float[] pos = {properties.get("x",Float.class) / units, properties.get("y", Float.class) / units};
		float size = constants.getFloat("size");

		obstacle = new BoxObstacle(pos[0], pos[1], size, size);
		obstacle.setName(properties.get("type", String.class));
		obstacle.setSensor(false);
		obstacle.setPhysicsUnits(units);
        this.units = units;

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
		this.collideBits = GameLevel.bitStringToShort(constants.getString("category"));
		this.excludeBitsLocked = GameLevel.bitStringToComplement(constants.getString("exclude"));
		this.excludeBitsUnlocked = GameLevel.bitStringToComplement(constants.getString("exclude-unlocked"));
		Filter filter = new Filter();
		filter.categoryBits = this.collideBits;
		filter.maskBits = this.excludeBitsLocked;
		obstacle.setFilterData(filter);

        lockedTexture = new TextureRegion(directory.getEntry("locked_door", Texture.class));
        unlockedTexture = new TextureRegion(directory.getEntry("unlocked_door", Texture.class));
        unlockTimer = new CircleTimer(0.2f, Color.YELLOW, units);

        // Set initial state (locked by default)
        setLocked(true);
        setTextureRegion(lockedTexture);
        resetTimer(); //TODO: get this from json
        unlockTimer.setPosition(this.obstacle.getPosition());
    }

    public void update(float dt){
        super.update(dt);
        if(isLocked() && isUnlocking()){
            remainingTimeToUnlock -= dt;
        } else {
            resetTimer();
        }
        if(remainingTimeToUnlock <= 0.0f){
            setLocked(false);
            unlocker.decreaseNumKeys();
        }
        unlockTimer.setProgress(remainingTimeToUnlock / UNLOCK_DURATION);

    }


    @Override
    public void draw(SpriteBatch batch) {
        super.draw(batch);
        if(isLocked() && isUnlocking){
            unlockTimer.draw(batch);
        }
    }
}
