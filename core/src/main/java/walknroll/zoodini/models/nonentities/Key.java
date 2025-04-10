package walknroll.zoodini.models.nonentities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.Map;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.TextureMapObject;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.utils.JsonValue;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteMesh;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import walknroll.zoodini.models.GameLevel;
import walknroll.zoodini.models.entities.Avatar;
import walknroll.zoodini.models.entities.Avatar.AvatarType;
import walknroll.zoodini.utils.ZoodiniSprite;
import edu.cornell.gdiac.graphics.SpriteBatch;

/**
 * A sensor obstacle representing a key that can be picked up.
 */
public class Key extends ZoodiniSprite {

    /** Whether this key has been collected by the player */
    private boolean collected;
    /** The avatar that owns this key */
    private AvatarType owner;
    /** Whether this key has been used to unlock a door */
    private boolean used;
    /** Tiled MapObject for this key*/
    MapObject mapObject;

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    /**
     * Returns whether this key has been collected.
     *
     * @return whether this key has been collected
     */
    public boolean isCollected() {
        return collected;
    }

    /**
     * Sets whether this key has been collected
     *
     * @param value whether this key has been collected
     */
    public void setCollected(boolean value) {
        collected = value;
    }

    public void setOwner(AvatarType t){
        owner = t;
    }

    public AvatarType getOwnerType(){
        return owner;
    }

    /**
     * Creates a key with the given settings
     *
     * @param directory The asset directory (for textures, etc)
     * @param globals   The JSON values defining global key properties
     * @param units     The physics units for this key
     */
    public Key(AssetDirectory directory, MapObject obj, JsonValue globals, float units) {
        mapObject = obj;
        MapProperties properties = mapObject.getProperties();

        float[] pos = new float[2];
        pos[0] = properties.get("x", Float.class) / units;
        pos[1] = properties.get("y", Float.class) / units;
        float size = properties.get("size", Float.class);

        obstacle = new BoxObstacle(pos[0], pos[1], size, size);
        obstacle.setName(properties.get("type", String.class));
        obstacle.setSensor(true); // Keys should be sensors (no physical collision)
        obstacle.setPhysicsUnits(units);

        float w = size * units;
        float h = size * units;
        mesh = new SpriteMesh(-w / 2, -h / 2, w, h);

        // Set physics properties from JSON
        obstacle.setBodyType(BodyType.StaticBody);
        obstacle.setDensity(0.0f);
        obstacle.setFriction(0.0f);
        obstacle.setRestitution(0.0f);

        // Create the collision filter
        short collideBits = GameLevel.bitStringToShort(properties.get("category", String.class));
        short excludeBits = GameLevel.bitStringToComplement(properties.get("exclude", String.class));
        Filter filter = new Filter();
        filter.categoryBits = collideBits;
        filter.maskBits = excludeBits;
        obstacle.setFilterData(filter);

        setDebugColor(ParserUtils.parseColor(globals.get("debug"), Color.YELLOW));

        // Get texture
        TextureMapObject tObj = (TextureMapObject) obj;
        //String key = globals.get("texture").asString(); //TODO get texture from tiled.
        TextureRegion texture = tObj.getTextureRegion();
        //TextureRegion texture = new TextureRegion(directory.getEntry(key, Texture.class));
        setTextureRegion(texture);

        collected = false;
        used = false;
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

    public int getID(){
        return mapObject.getProperties().get("id", Integer.class);
    }

}
