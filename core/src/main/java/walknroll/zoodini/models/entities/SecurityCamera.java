package walknroll.zoodini.models.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.maps.MapProperties;
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

    public SecurityCamera(AssetDirectory directory, MapProperties properties, JsonValue globals, float units) {
        float[] pos = new float[2];
        pos[0] = properties.get("x", Float.class) / units;
        pos[1] = properties.get("y", Float.class) / units;
        float radius = properties.get("radius", Float.class);
        angle = properties.get("angle", Float.class);
        obstacle = new WheelObstacle(pos[0], pos[1], radius);
        obstacle.setName(properties.get("type", String.class));
        obstacle.setFixedRotation(false);

        obstacle.setBodyType(BodyDef.BodyType.StaticBody);
        obstacle.setDensity(1.0f);
        obstacle.setFriction(0.0f);
        obstacle.setRestitution(0.0f);
        obstacle.setPhysicsUnits(units);

        short collideBits = GameLevel.bitStringToShort(properties.get("collide", String.class));
        short excludeBits = GameLevel.bitStringToComplement(properties.get("exclude", String.class));
        Filter filter = new Filter();
        filter.categoryBits = collideBits;
        filter.maskBits = excludeBits;
        obstacle.setFilterData(filter);

        setDebugColor(ParserUtils.parseColor(globals.get("debug"), Color.WHITE));

        String key = globals.getString("texture"); //TODO somehow pull texture from tiled?
        startFrame = globals.getInt("startframe");
        sprite = directory.getEntry(key, SpriteSheet.class);
        sprite.setFrame(startFrame);

        float r = properties.get("spriteRadius", Float.class) * units;
        mesh = new SpriteMesh(-r, -r, 2 * r, 2 * r);

        disabledTime = disabledTimeRemaining = properties.get("disabledTime", Float.class);
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
