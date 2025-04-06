package walknroll.zoodini.models.nonentities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.utils.JsonValue;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteMesh;
import edu.cornell.gdiac.graphics.SpriteSheet;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.physics2.WheelObstacle;
import walknroll.zoodini.models.GameLevel;
import walknroll.zoodini.utils.ZoodiniSprite;

public class InkProjectile extends ZoodiniSprite {

    /** Cache for internal force calculations */
    private Vector2 forceCache = new Vector2();

    /** The current horizontal movement of the character */
    private Vector2 movement = new Vector2();

    /** The position that this projectile was shot */
    private Vector2 startPosition = new Vector2();

    private int startFrame;

    private boolean shouldDestroy;


    public Vector2 getStartPosition(){
        return startPosition;
    }

    public void setStartPosition(Vector2 p){
        startPosition.set(p);
    }


    public boolean getShouldDestroy() {
        return shouldDestroy;
    }

    public void setShouldDestroy(boolean s) {
        this.shouldDestroy = s;
    }

    public void setPosition(Vector2 newPos) {
        obstacle.setPosition(newPos);
    }

    /**
     * Returns the position of this avatar.
     *
     * @return the position of this avatar.
     */
    public Vector2 getPosition() {
        return obstacle.getPosition();
    }

    public void setAngle(float angle) {
        obstacle.setAngle(angle);
    }

    public float getAngle() {
        return obstacle.getAngle();
    }

    /**
     * Returns the directional movement of this character.
     *
     * This is the result of input times the avatar force.
     *
     * @return the directional movement of this character.
     */
    public Vector2 getMovement() {
        return movement;
    }

    /**
     * Sets the directional movement of this character.
     *
     * This is the result of input times the avatar force.
     *
     * @param value the directional movement of this character.
     */
    public void setMovement(Vector2 value) {
        setMovement(value.x, value.y);
    }

    /**
     * Sets the directional movement of this character.
     *
     * This is the result of input times the avatar force.
     *
     * @param dx the horizontal movement of this character.
     * @param dy the horizontal movement of this character.
     */
    public void setMovement(float dx, float dy) {
        movement.set(dx, dy);
    }

    /**
     * Creates a exit with the given settings
     *
     * @param directory The asset directory (for textures, etc)
     * @param json      The JSON values defining this avatar
     * @param units     The physics units for this avatar
     */
    public InkProjectile(AssetDirectory directory, JsonValue json, float units) {
        float radius = json.getFloat("radius");
        obstacle = new WheelObstacle(0, 0, radius);
        obstacle.setName(json.name());
        obstacle.setFixedRotation(false);

        obstacle.setBodyType(BodyDef.BodyType.DynamicBody);
        obstacle.setDensity(json.getFloat("density"));
        obstacle.setFriction(json.getFloat("friction"));
        obstacle.setRestitution(json.getFloat("restitution"));
        obstacle.setPhysicsUnits(units);

        short collideBits = GameLevel.bitStringToShort(json.getString("collide"));
        short excludeBits = GameLevel.bitStringToComplement(json.getString("exclude"));
        Filter filter = new Filter();
        filter.categoryBits = collideBits;
        filter.maskBits = excludeBits;
        obstacle.setFilterData(filter);

        setDebugColor(ParserUtils.parseColor(json.get("debug"), Color.WHITE));

        String key = json.getString("texture");
        startFrame = json.getInt("startframe");
        sprite = directory.getEntry(key, SpriteSheet.class);
        sprite.setFrame(startFrame);

        float r = json.getFloat("spriterad") * units;
        mesh = new SpriteMesh(-r, -r, 2 * r, 2 * r);

        shouldDestroy = false;
    }

    /**
     * Applies the force to the body of this avatar
     *
     * This method should be called after the force attribute is set.
     */
    public void applyForce() {
        if(shouldDestroy){
            return;
        }

        if (!obstacle.isActive()) {
            return;
        }

        // Only walk or spin if we allow it
        obstacle.setLinearVelocity(Vector2.Zero);
        obstacle.setAngularVelocity(0.0f);

        // Apply force for movement
         if (getMovement().len2() > 0f) {
            forceCache.set(getMovement().nor().scl(50));
            obstacle.getBody().applyForce(forceCache, obstacle.getPosition(), true);
        }
    }

    @Override
    public void update(float dt) {
        obstacle.update(dt);
    }


    public void destroy(){
        if(!shouldDestroy){
            return;
        }
        getObstacle().setActive(false);
        setDrawingEnabled(false);
        setShouldDestroy(false);
    }

    public void activate(){
        setShouldDestroy(false);
        getObstacle().setActive(true);
        setDrawingEnabled(true);
    }
}
