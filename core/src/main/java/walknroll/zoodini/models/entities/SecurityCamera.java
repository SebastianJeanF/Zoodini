package walknroll.zoodini.models.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.math.Vector2;
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
import walknroll.zoodini.utils.animation.Animation;
import walknroll.zoodini.utils.animation.AnimationController;
import walknroll.zoodini.utils.animation.AnimationState;

public class SecurityCamera extends ZoodiniSprite {

    private float fov;
    private float viewDistance;

    private boolean disabled;
    private float disabledTime; //in seconds
    private float disabledTimeRemaining;
    private float angle;

    // Ring effect properties
    private float currentRadius;
    private float alarmDistance;
    private float expansionSpeed;
    private boolean isRingActive;
    private Color ringColor;
    private float ringThickness;

    private final AnimationController animationController;

    Affine2 affineCache = new Affine2();
    PathFactory pf = new PathFactory();
    PathExtruder extruder = new PathExtruder();

    public SecurityCamera(MapProperties properties, float units) {
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

        short collideBits = GameLevel.bitStringToShort(properties.get("category", String.class));
        short excludeBits = GameLevel.bitStringToComplement(properties.get("exclude", String.class));
        Filter filter = new Filter();
        filter.categoryBits = collideBits;
        filter.maskBits = excludeBits;
        obstacle.setFilterData(filter);

        float r = properties.get("spriteRadius", Float.class) * units;
        mesh = new SpriteMesh(-r, -r, 2 * r, 2 * r);


        disabledTime = properties.get("disabledTime", Float.class);
        disabled = false;

        // Initialize ring effect properties
        alarmDistance = properties.get("alarmDistance", Float.class);
        expansionSpeed = 5.0f;
        ringThickness = 0.1f;
        ringColor = new Color(1, 0, 0, 0.5f); // Semi-transparent red
        currentRadius = 0f; //in meters
        isRingActive = false;
        fov = properties.get("fov",Float.class);
        viewDistance = properties.get("viewDistance", Float.class);

        animationController = new AnimationController(AnimationState.IDLE);
    }


    /**
     * Adds spritesheet to animate for a given state.
     * */
    public void setAnimation(AnimationState state, SpriteSheet sheet){
        switch(state){
            case IDLE -> animationController.addAnimation(AnimationState.IDLE, new Animation(sheet, 0, sheet.getSize()-1, 16, true));
            case WALK -> animationController.addAnimation(AnimationState.WALK, new Animation(sheet, 0, sheet.getSize()-1, 16, true));
            case WALK_DOWN -> animationController.addAnimation(AnimationState.WALK_DOWN, new Animation(sheet, 0, sheet.getSize()-1, 16, true));
            case WALK_UP -> animationController.addAnimation(AnimationState.WALK_UP, new Animation(sheet, 0, sheet.getSize()-1, 16, true));
        }
    }

    public float getAlarmDistance(){
        return alarmDistance;
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

        // Update animation controller
        animationController.update();

        // This is the key fix - update the sprite reference itself
        SpriteSheet currentSheet = animationController.getCurrentSpriteSheet();
        if (currentSheet != null) {
            sprite = currentSheet;  // Switch to the current animation's spritesheet
        }

        // Now setting the frame will work correctly
        if (sprite != null) {
            sprite.setFrame(animationController.getCurrentFrame());
        }

        super.update(dt);

        if (disabled == true) {
            disabledTimeRemaining -= dt;
        }

        if (disabledTimeRemaining <= 0) {
            disabled = false;
        }

        // Update ring animation
        if (isRingActive) {
            currentRadius += expansionSpeed * dt;

            if (currentRadius >= alarmDistance) {
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
            Path2 ringPath = pf.makeNgon(x, y, currentRadius, 64);

            // Create extruder for the ring outline
            extruder.set(ringPath);
            extruder.calculate(ringThickness);

            // Set up affine transformation
            affineCache.idt();
            affineCache.scale(obstacle.getPhysicsUnits(), obstacle.getPhysicsUnits());

            // Draw the ring
            batch.draw((TextureRegion) null, extruder.getPolygon(), affineCache);

            // Restore original color
            batch.setColor(originalColor);
        }
    }

    public Vector2 getPosition() {
        return obstacle.getPosition();
    }

    public float getX() {
        return obstacle.getX();
    }

    public float getY() {
        return obstacle.getY();
    }

    public float getFov(){
        return fov;
    }

    public float getViewDistance(){
        return viewDistance;
    }


    public boolean isDisabled() {
        return disabled;
    }

    public void disable() {
        this.disabled = true;
        disabledTimeRemaining = disabledTime;
    }

    public float getAngle() {
        return angle;
    }
}
