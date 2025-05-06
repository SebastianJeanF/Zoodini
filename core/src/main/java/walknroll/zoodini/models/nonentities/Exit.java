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
public class Exit extends ZoodiniSprite {
    private ExitAnimal creature;

    private Animation<TextureRegion> animation;
    private SpriteSheet spriteTextures;
    private float animationTime;

    private float textureScale;

    /**
     * Creates a door with the given settings
     *
     * @param directory The asset directory (for textures, etc)
     * @param units     The physics units for this avatar
     */
    public Exit(AssetDirectory directory, MapProperties properties, JsonValue constants, float units,
            ExitAnimal creature) {
        float[] pos = { properties.get("x", Float.class) / units, properties.get("y", Float.class) / units };
        float size = constants.getFloat("size");
        textureScale = constants.getFloat("spriteScale");
        this.creature = creature;

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

        obstacle.setUserData(this);
    }

    public void create(AssetDirectory directory) {
        switch (this.creature) {
            case PENGUIN -> spriteTextures = directory.getEntry("penguin-chained-idle.animation", SpriteSheet.class);
            case RABBIT -> spriteTextures = directory.getEntry("rabbit-chained-idle.animation", SpriteSheet.class);
            case OCTOPUS -> spriteTextures = directory.getEntry("octopus-chained-idle.animation", SpriteSheet.class);
            default -> spriteTextures = directory.getEntry("panda-chained-idle.animation", SpriteSheet.class);
        }

        TextureRegion[][] tmp = TextureRegion.split(spriteTextures.getTexture(), spriteTextures.getRegionWidth(),
                spriteTextures.getRegionHeight());

        float w = spriteTextures.getRegionWidth() * textureScale;
        float h = spriteTextures.getRegionHeight() * textureScale;
        mesh = new SpriteMesh(-w / 2, -h / 2, w, h);

        TextureRegion[] frames = new TextureRegion[spriteTextures.getSize()];
        if (spriteTextures.getSize() >= 0) System.arraycopy(tmp[0], 0, frames, 0, spriteTextures.getSize());

        animation = new Animation<>(0.5f, frames);
        animationTime = 0f;
    }

    @Override
    public void update(float dt) {
        animationTime += dt;
    }

    @Override
    public void draw(SpriteBatch batch) {
        if (this.obstacle != null && this.mesh != null) {
            float x = this.obstacle.getX();
            float y = this.obstacle.getY();
            float a = this.obstacle.getAngle();
            float u = this.obstacle.getPhysicsUnits();
            this.transform.idt();
            this.transform.preRotate((float) ((double) (a * 180.0F) / Math.PI));
            TextureRegion frame = animation.getKeyFrame(animationTime, true);
            this.transform.preTranslate(x * u, y * u);
            // this.transform.scale(textureScale, textureScale);

            batch.setTextureRegion(frame);
            batch.drawMesh(this.mesh, this.transform, false);
            // batch.draw(frame, this.transform);
            batch.setTexture((Texture) null);
        }
    }
}
