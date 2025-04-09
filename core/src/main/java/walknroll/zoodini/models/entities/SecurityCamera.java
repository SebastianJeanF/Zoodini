package walknroll.zoodini.models.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.utils.JsonValue;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.SpriteMesh;
import edu.cornell.gdiac.graphics.SpriteSheet;
import edu.cornell.gdiac.physics2.WheelObstacle;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.math.PathExtruder;
import edu.cornell.gdiac.math.PathFactory;
import walknroll.zoodini.models.GameLevel;
import walknroll.zoodini.utils.ZoodiniSprite;

public class SecurityCamera extends ZoodiniSprite {

    private int startFrame;
    private boolean isDisabled;
    private int disabledTime;
    private int disabledTimeRemaining;
    private float angle;

    // Ring effect properties
    private float currentRadius;
    private float maxRadius;
    private float expansionSpeed;
    private boolean isRingActive;
    private Color ringColor;
    private float ringThickness;
    private Affine2 affineCache;

    public boolean isDisabled() {
        return isDisabled;
    }

    public void setDisabled(boolean isDisabled) {
        this.isDisabled = isDisabled;
    }

    public float getAngle() {
        return angle;
    }

    public SecurityCamera(AssetDirectory directory, JsonValue json, JsonValue globals, float units) {
        float[] pos = json.get("pos").asFloatArray();
        float radius = globals.getFloat("radius");
        angle = json.get("angle").asFloat();
        obstacle = new WheelObstacle(pos[0], pos[1], radius);
        obstacle.setName(json.name());
        obstacle.setFixedRotation(false);

        obstacle.setBodyType(BodyDef.BodyType.StaticBody);
        obstacle.setDensity(globals.getFloat("density"));
        obstacle.setFriction(globals.getFloat("friction"));
        obstacle.setRestitution(globals.getFloat("restitution"));
        obstacle.setPhysicsUnits(units);

        short collideBits = GameLevel.bitStringToShort(globals.getString("collide"));
        short excludeBits = GameLevel.bitStringToComplement(globals.getString("exclude"));
        Filter filter = new Filter();
        filter.categoryBits = collideBits;
        filter.maskBits = excludeBits;
        obstacle.setFilterData(filter);

        setDebugColor(ParserUtils.parseColor(globals.get("debug"), Color.WHITE));

        String key = globals.getString("texture");
        startFrame = globals.getInt("startframe");
        sprite = directory.getEntry(key, SpriteSheet.class);
        sprite.setFrame(startFrame);

        float r = globals.getFloat("spriterad") * units;
        mesh = new SpriteMesh(-r, -r, 2 * r, 2 * r);

        disabledTime = disabledTimeRemaining = globals.getInt("disabledTime");
        isDisabled = false;

        // Initialize ring effect properties
        maxRadius = globals.getFloat("cameraRange", 4.0f);
        expansionSpeed = globals.getFloat("ringExpansionSpeed", 0.1f);
        ringThickness = globals.getFloat("ringThickness", 0.05f);
        ringColor = new Color(1, 0, 0, 0.5f); // Semi-transparent red
        currentRadius = 0f;
        isRingActive = false;
        affineCache = new Affine2();
    }

    /**
     * Activates the ring effect
     */
    public void activateRing() {
        if (!isRingActive) {
            isRingActive = true;
            currentRadius = 0f;
        }
    }

    @Override
    public void update(float dt) {
        super.update(dt);

        if (isDisabled()) {
            disabledTimeRemaining--;
        }

        if (disabledTimeRemaining < 0) {
            isDisabled = false;
            disabledTimeRemaining = disabledTime;
        }

        // Update ring animation
        if (isRingActive) {
            currentRadius += expansionSpeed;

            if (currentRadius >= maxRadius) {
                isRingActive = false;
            }
        }
    }

    @Override
    public void draw(SpriteBatch batch) {
        super.draw(batch);

        // Draw expanding ring if active
        if (isRingActive) {
            // Save original color
            Color originalColor = batch.getColor().cpy();

            // Set color for the ring
            batch.setColor(ringColor);

            // Draw ring using PathFactory and PathExtruder
            float x = getX();
            float y = getY();

            // Create n-gon path for the ring
            Path2 ringPath = new PathFactory().makeNgon(x, y, currentRadius, 64);

            // Create extruder for the ring outline
            PathExtruder ringExtruder = new PathExtruder(ringPath);
            ringExtruder.calculate(ringThickness);

            // Set up affine transformation
            affineCache.idt();
            affineCache.scale(obstacle.getPhysicsUnits(), obstacle.getPhysicsUnits());

            // Draw the ring
            batch.draw((TextureRegion) null, ringExtruder.getPolygon(), affineCache);

            // Restore original color
            batch.setColor(originalColor);
        }
    }

    public float getX() {
        return obstacle.getX();
    }

    public float getY() {
        return obstacle.getY();
    }
}
