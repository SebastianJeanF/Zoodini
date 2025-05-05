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

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.utils.JsonValue;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.SpriteMesh;
import edu.cornell.gdiac.graphics.SpriteSheet;
import edu.cornell.gdiac.physics2.BoxObstacle;
import walknroll.zoodini.models.GameLevel;
import walknroll.zoodini.utils.ZoodiniSprite;
import walknroll.zoodini.utils.enums.ExitAnimal;

/**
 * A sensor obstacle representing the end of the level.
 *
 * This class is largely just a constructor. Everything else is provided by the
 * subclass.
 */
public class Vent extends ZoodiniSprite {
    /** Whether this door is locked */
    private boolean open;
    private TextureRegion openTexture;
    private TextureRegion closedTexture;

    /**
     * Creates a door with the given settings
     *
     * @param directory The asset directory (for textures, etc)
     * @param units     The physics units for this avatar
     */
    public Vent(AssetDirectory directory, MapProperties properties, JsonValue constants, float units) {
        float[] pos = { properties.get("x", Float.class) / units, properties.get("y", Float.class) / units };
        float size = constants.getFloat("size");

        obstacle = new BoxObstacle(pos[0], pos[1], size, size);
        obstacle.setName(properties.get("type", String.class));
        obstacle.setSensor(true);
        obstacle.setPhysicsUnits(units);

        // Technically, we should do error checking here.
        // A JSON field might accidentally be missing
        obstacle.setBodyType(BodyType.StaticBody);
        obstacle.setDensity(0.0f);
        obstacle.setFriction(0.0f);
        obstacle.setRestitution(0.0f);

        // Create the collision filter (used for light penetration)
        short collideBits = GameLevel.bitStringToShort(constants.getString("category"));
        short excludeBits = GameLevel.bitStringToComplement(constants.getString("exclude"));
        Filter filter = new Filter();
        filter.categoryBits = collideBits;
        filter.maskBits = excludeBits;
        obstacle.setFilterData(filter);

        openTexture = new TextureRegion(directory.getEntry("vent-open", Texture.class));
        closedTexture = new TextureRegion(directory.getEntry("vent-closed", Texture.class));
        setTextureRegion(openTexture);

        float textureScale = constants.getFloat("spriteScale");
        float w = openTexture.getRegionWidth() * textureScale;
        float h = openTexture.getRegionHeight() * textureScale;
        mesh = new SpriteMesh(-w / 2, -h / 2, w, h);
    }

    public void setOpen(boolean value) {
        this.open = value;
        setTextureRegion(value ? openTexture : closedTexture);
    }

    public boolean isOpen() {
        return this.open;
    }
}
