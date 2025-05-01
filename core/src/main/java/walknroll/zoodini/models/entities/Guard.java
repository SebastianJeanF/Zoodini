package walknroll.zoodini.models.entities;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.PolylineMapObject;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import edu.cornell.gdiac.graphics.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.graphics.Pixmap;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.SpriteSheet;
import java.util.Arrays;
import walknroll.zoodini.models.entities.Enemy;
import walknroll.zoodini.utils.LevelPortal;
import walknroll.zoodini.utils.animation.Animation;
import walknroll.zoodini.utils.animation.AnimationController;
import walknroll.zoodini.utils.animation.AnimationState;

import static walknroll.zoodini.utils.animation.AnimationState.SUSPICION_METER;

public class Guard extends Enemy {
    public static final int MAX_CHASE_TIME = 60; // 1 second
    public static final float FOV_DISTANCE = 7.0f; // Maximum detection distance.
    public static final float FOV_ANGLE = 45.0f; // Total cone angle in degrees.

    private static final float PATROL_THRESHOLD = 0.5f; // Distance to switch patrol points
    private static Texture SUSPICION_METER_CURIOUS;
    private static final float CLOSE_DISTANCE_FACTOR = 0.4f; //
    private static final float MEDIUM_DISTANCE_FACTOR = 0.8f; //
    // Suspicion increase amounts for each zone
    private static final int CLOSE_ZONE_SUS_INCREASE = 5;
    private static final int MEDIUM_ZONE_SUS_INCREASE = 4;
    private static final int FAR_ZONE_SUS_INCREASE = 3;

    public static void setSuspicionMeterCuriousTexture(Texture suspicionMeterCurious) {
        Guard.SUSPICION_METER_CURIOUS = suspicionMeterCurious;
    }

    public static boolean isLoaded() {
        return Guard.SUSPICION_METER_CURIOUS != null;
    }

    private float fov;
    private float viewDistance;
    private boolean isChasing;

    private boolean meowed;
    private int chaseTimer;
    private boolean cameraAlerted;

    private Avatar aggroTarget;
    private boolean seesPlayer;
    private Avatar seenPlayer;

    /** The position that this guard should move to */
    Vector2 target = null;
    Vector2 movementDirection = null;
    Vector2 targetPosition = null;
    /** Direction guard is currently facing */
    private Vector2 currentDirection = new Vector2(0, 1); // Default facing up
    private Vector2 targetDirection = new Vector2(0, 1);

    private float turnSpeed = 5.0f;
    // --- Patrol Path Variables for Guard ---
    private Vector2[] patrolPoints;
    private int currentPatrolIndex = 0;

    private final AnimationController suspsicionMeter;
    private float susLevel;

    private final float susThreshold;
    private float maxSusLevel;
    private final float DEAGRRO_PERIOD = 60F;

    private final float ALERT_DEAGRRO_PERIOD = 300F;
    private float deAggroTimer;
    private boolean inkBlinded = false;
    private float inkBlindTimer = 0;
    private float tempViewDistance;
    private float tempFov;

    private float originalViewDistance;

    private float originalFov;

    /**
     * Creates a new dude with degenerate settings
     * <p>
     * The main purpose of this constructor is to set the initial capsule
     * orientation.
     */
    public Guard(MapProperties properties, float units) {
        super(properties, units);
        fov = properties.get("fov", Float.class);
        currentPatrolIndex = 0;
        cameraAlerted = false;
        isChasing = false;
        meowed = false;
        chaseTimer = 0;
        AnimationState state = AnimationState.SUSPICION_METER;
        suspsicionMeter = new AnimationController(state);
        viewDistance = properties.get("viewDistance", Float.class);
        susThreshold = 10F;
        maxSusLevel = 100F;
        seesPlayer = false;

        MapObject path = properties.get("path", MapObject.class);
        if (path instanceof PolylineMapObject line) {
            float[] vertices = line.getPolyline().getTransformedVertices();
            for (int i = 0; i < vertices.length; i++) {
                vertices[i] /= units;
            }
            setPatrolPoints(vertices);
        } else { setPatrolPoints(new Vector2[] {this.getPosition().cpy()});}

        originalViewDistance = viewDistance;
        originalFov = fov;
        tempViewDistance = viewDistance;
        tempFov = fov;

    }

    public void setSusMeter(SpriteSheet sheet) {
        final int START_FRAME = 0;
        final int FRAME_DELAY = 0;
        final boolean IS_LOOP = true;

        sheet.setFrame(START_FRAME);
        Animation anim = new Animation(
                sheet,
                START_FRAME,
                sheet.getSize() - 1,
                FRAME_DELAY,
                IS_LOOP);
        suspsicionMeter.addAnimation(SUSPICION_METER, anim);
    }

    public void setMaxSusLevel() {
        this.susLevel = this.maxSusLevel;
    }

    public void setMinSusLevel() {
        this.susLevel = 0.0F;
    }

    public float getSusLevel() {
        return susLevel;
    }

    public void deltaSusLevel(float delta) {
        this.susLevel = MathUtils.clamp(susLevel + delta, 0.0F, maxSusLevel);
    }

    public boolean isMaxSusLevel() {
        return susLevel == maxSusLevel;
    }

    public float getMaxSusLevel() {
        return maxSusLevel;
    }

    public float getSusThreshold() {
        return susThreshold;
    }

    /**
     * Check if the guard is "suspicious" of the player.
     * A guard is suspicious if their susLevel is greater than or equal to
     * the susThreshold.
     */
    public boolean isSus() {
        return susLevel >= susThreshold;
    }

    public boolean isMinSusLevel() {
        return susLevel == 0.0F;
    }

    public boolean checkDeAggroed() {
        return deAggroTimer <= 0;
    }

    public void deltaDeAggroTimer(float delta) {
        this.deAggroTimer = MathUtils.clamp(deAggroTimer + delta, 0.0F, DEAGRRO_PERIOD);
    }

    /**
     * Differentially start the deAggro timer. If the guard is alerted by a camera,
     * set the
     * timer to ALERT_DEAGRRO_PERIOD. Otherwise, set it to DEAGRRO_PERIOD.
     * ALERT_DEAGRRO_PERIOD is longer than DEAGRRO_PERIOD.
     */
    public void startDeAggroTimer() {
        this.deAggroTimer = isCameraAlerted() ? ALERT_DEAGRRO_PERIOD : DEAGRRO_PERIOD;
    }

    public Vector2[] getPatrolPoints() {
        return patrolPoints;
    }

    public void setPatrolPoints(Vector2[] v) {
        patrolPoints = v;
    }

    public void setPatrolPoints(float[] points) {
        assert points.length % 2 == 0;
        Vector2[] vec2s = new Vector2[points.length / 2];
        for (int i = 0; i < points.length; i += 2) {
            Vector2 v = new Vector2(points[i], points[i + 1]);
            vec2s[i / 2] = v;
        }
        setPatrolPoints(vec2s);
    }

    public boolean isCameraAlerted() {
        return cameraAlerted;
    }

    public void setCameraAlerted(boolean value) {
        cameraAlerted = value;
    }

    /** If a guard is "agroed", it is currently chasing a player */
    public boolean isAgroed() {
        return isChasing;
    }

    public void setSeesPlayer(boolean seesPlayer) {
        this.seesPlayer = seesPlayer;
    }

    public boolean isSeesPlayer() {
        return seesPlayer;
    }

    public void setSeenPlayer(Avatar seenPlayer) {
        this.seenPlayer = seenPlayer;
    }

    public Avatar getSeenPlayer() {
        return seenPlayer;
    }

    /**
     * Get current movement direction of guard.
     *
     * @INVARIANT: Must call guard.think() to get the most recent movement direction
     * @return The current movement direction of the guard
     */
    public Vector2 getMovementDirection() {
        return movementDirection;
    }

    public void setTarget(Vector2 target) {

        this.target = target;
    }

    public void think(Vector2 movementDirection, Vector2 targetPosition) {
        this.movementDirection = movementDirection;
        this.targetPosition = targetPosition;
    }

    public void setAgroed(boolean agroed) {
        isChasing = agroed;
    }

    public void setMeow(boolean meowed) {
        this.meowed = meowed;
    }

    public int calculateSusIncrease(Vector2 playerPosition) {
        // Calculate distance and angle to player
        Vector2 toPlayer = new Vector2(playerPosition).sub(getPosition());
        float distance = toPlayer.len();

        // Calculate angle between guard's facing direction and player
        float angleToPlayer = Math.abs(currentDirection.angleRad(toPlayer));

        // Convert FOV from degrees to radians for calculations
        float fovRadians = (float) Math.toRadians(fov);
        float halfFOV = fovRadians / 2;

        // Base suspicion increase based on distance
        int baseSuspicionIncrease;

        // Zone calculation - close, medium, or far
        if (distance <= viewDistance * CLOSE_DISTANCE_FACTOR) {
            baseSuspicionIncrease = CLOSE_ZONE_SUS_INCREASE;
        } else if (distance <= viewDistance * MEDIUM_DISTANCE_FACTOR) {
            baseSuspicionIncrease = MEDIUM_ZONE_SUS_INCREASE;
        } else {
            baseSuspicionIncrease = FAR_ZONE_SUS_INCREASE;
        }

        // DebugPrinter.println("sus increase is " + baseSuspicionIncrease);
        return baseSuspicionIncrease;
    }

    /**
     * If a guard is "meowed", it is currently patrolling to the spot of the meow,
     * but they are not chasing a player. When either alerted by a security camera,
     * or if they see a player, or if they reach the spot of the meow, the guard
     * leaves the "meowed" state
     */
    public boolean isMeowed() {
        return meowed;
    }

    public Avatar getAggroTarget() {
        return aggroTarget;
    }

    public void setAggroTarget(Avatar target) {
        aggroTarget = target;
    }

    /**
     * This timer is used to determine how long a guard should chase a player
     * before giving up and returning to their patrol route
     */
    public int getChaseTimer() {
        return chaseTimer;
    }

    public void setChaseTimer(int value) {
        chaseTimer = value;
    }

    public float getFov() {
        return tempFov;
    }

    public float getViewDistance() {
        return tempViewDistance;
    }

    /**
     * Updates the guard's orientation to smoothly turn towards the target
     * direction.
     *
     * @param dt              The time delta since the last update
     * @param targetDirection The direction the guard should face
     */
    public void updateOrientation(float dt, Vector2 targetDirection) {
        if (targetDirection == null || targetDirection.len2() < 0.0001f) {
            return; // No valid direction to face
        }

        // Save the target direction
        this.targetDirection.set(targetDirection).nor();

        // Calculate the current angle in radians
        float currentAngle = currentDirection.angleRad();
        // Calculate the target angle in radians
        float targetAngle = this.targetDirection.angleRad();

        // Determine the shortest turning direction
        float angleDiff = targetAngle - currentAngle;
        // Normalize to [-PI, PI]
        while (angleDiff > Math.PI)
            angleDiff -= 2 * Math.PI;
        while (angleDiff < -Math.PI)
            angleDiff += 2 * Math.PI;

        // Calculate how much to turn this frame
        float turnAmount = Math.min(Math.abs(angleDiff), turnSpeed * dt);
        // Apply the turn in the correct direction
        float newAngle = currentAngle + Math.signum(angleDiff) * turnAmount;

        // Update the current direction
        currentDirection.set(MathUtils.cos(newAngle), MathUtils.sin(newAngle)).nor();

        // Set the guard's sprite angle (offset by -90 degrees since up is 0)
        setAngle(newAngle - (float) Math.PI / 2);
    }

    public void update(float dt) {
        // If we have a movement direction, update orientation
        if (movementDirection != null && movementDirection.len2() > 0.0001f) {
            updateOrientation(dt, movementDirection);
        }
        if (tempViewDistance < originalViewDistance && !inkBlinded) {
            tempViewDistance += dt;
        }
        if (tempFov < originalFov && !inkBlinded) {
            tempFov += dt;
        }
        applyForce();
        super.update(dt);
    }

    /** The value of target is only valid if guard is agroed or is "meowed" */
    public Vector2 getTarget() {
        if (meowed == true) {
            // System.out.print("Guard is getting meow target");
        }
        return target;
    }

    public void draw(SpriteBatch batch) {
        setAngle(0);
        super.draw(batch);
        drawSuspicionMeter(batch);
    }

//    // class‐scope constants
//    private static final float BASELINE_PX    = 32f;
//    private static final float SCALE_FACTOR   = 0.2f;
//    private static final Vector2  OFFSET_UNSCALED = new Vector2(-80f, 100f);
//
//    public void drawSuspicionMeter(SpriteBatch batch) {
//        // early-exit if we can’t draw
//        if (suspsicionMeter == null
//            || suspsicionMeter.getCurrentSpriteSheet() == null
//            || !Guard.isLoaded()) {
//            return;
//        }
//
//        // pick the right sprite
//        TextureRegion region = isMeowed()
//            ? Guard.SUSPICION_METER_CURIOUS
//            : suspsicionMeter.getCurrentSpriteSheet();
//
//        // only update animation when using the dynamic meter
//        if (!isMeowed()) {
//            updateSuspicionAnimation();
//        }
//
//        // compute scale & screen‐space position
//        float physToPx = getObstacle().getPhysicsUnits();
//        float scale    = SCALE_FACTOR * (physToPx / BASELINE_PX);
//        float xPx      = getPosition().x * physToPx + OFFSET_UNSCALED.x * scale;
//        float yPx      = getPosition().y * physToPx + OFFSET_UNSCALED.y * scale;
//
//        // single draw call
//        batch.draw(
//            region,
//            xPx, yPx,
//            region.getRegionWidth()  * scale,
//            region.getRegionHeight() * scale
//        );
//    }
//

    public void drawSuspicionMeter(SpriteBatch batch) {
        float BASELINE_PX = 32;
        if (suspsicionMeter == null
            || suspsicionMeter.getCurrentSpriteSheet() == null
            || !Guard.isLoaded()) {
            return;
        }

        float PIXEL_PER_WORLD_UNIT = getObstacle().getPhysicsUnits();
        float guardXPixel = getPosition().x * PIXEL_PER_WORLD_UNIT;
        float guardYPixel = getPosition().y * PIXEL_PER_WORLD_UNIT;

        float SCALE = 0.2f * (PIXEL_PER_WORLD_UNIT / BASELINE_PX);
        float X_PIXEL_OFFSET = (-80f * SCALE);
        float Y_PIXEL_OFFSET = 100f * SCALE;



        if (isMeowed()) {
            batch.draw(
                Guard.SUSPICION_METER_CURIOUS,
                guardXPixel + getXPixelOffset(),
                guardYPixel + Y_PIXEL_OFFSET,
                Guard.SUSPICION_METER_CURIOUS.getWidth() * SCALE,
                Guard.SUSPICION_METER_CURIOUS.getHeight() * SCALE);
        } else {
            updateSuspicionAnimation();

            // Get the original width and height of the sprite sheet
            float originalWidthPx = suspsicionMeter.getCurrentSpriteSheet().getRegionWidth();
            float originalHeightPx = suspsicionMeter.getCurrentSpriteSheet().getRegionHeight();

            batch.draw(
                suspsicionMeter.getCurrentSpriteSheet(),
                guardXPixel + getXPixelOffset(),
                guardYPixel + Y_PIXEL_OFFSET,
                originalWidthPx * SCALE,
                originalHeightPx * SCALE);
        }

    }

    /**
     * Get the X pixel offset for the suspicion meter based on the guard's state
     * and movement direction.
     *
     * @return The X pixel offset for the suspicion meter.
     */
    private float getXPixelOffset() {
        float BASELINE_PX = 32;
        float PIXEL_PER_WORLD_UNIT = getObstacle().getPhysicsUnits();
        float SCALE = 0.2f * (PIXEL_PER_WORLD_UNIT / BASELINE_PX);

        AnimationState guardState = animationController.getCurrentState();

        if (isMeowed()) {
            if (guardState == AnimationState.WALK_UP) {
                return (-95f * SCALE);
            }
            else if (guardState == AnimationState.WALK_DOWN) {
                return (-90f * SCALE);
            }
            // Else if the guard is moving to the right
            else if (guardState == AnimationState.WALK  && movementDirection != null && movementDirection.x > 0) {
                return (-115f * SCALE);
            }
            // Else if the guard is moving to the left
            else if (guardState == AnimationState.WALK && movementDirection != null && movementDirection.x < 0) {
                return (-90f * SCALE);
            }
            return (-80f * SCALE);
        }
        else {
            if (guardState == AnimationState.WALK_UP) {
                return (-66f * SCALE);
            }
            else if (guardState == AnimationState.WALK_DOWN) {
                return (-66f * SCALE);
            }
            // Else if the guard is moving to the right
            else if (guardState == AnimationState.WALK  && movementDirection != null && movementDirection.x > 0) {
                return (-80f * SCALE);
            }
            // Else if the guard is moving to the left
            else if (guardState == AnimationState.WALK && movementDirection != null && movementDirection.x < 0) {
                return (-63f * SCALE);
            }
            return (-65f * SCALE);
        }

    }

    public boolean isInkBlinded() {
        return inkBlinded;
    }

    public void setInkBlinded(boolean blinded) {
        this.inkBlinded = blinded;
    }

    public void setInkBlindTimer(float duration) {
        this.inkBlindTimer = duration;
    }

    public void updateInkBlindTimer(float dt) {
        if (inkBlinded) {
            inkBlindTimer -= dt;
            if (inkBlindTimer <= 0) {
                inkBlinded = false;
            }
        }
    }

    public void setTempViewDistance(float distance) {
        this.tempViewDistance = distance;
    }

    public void setTempFov(float fov) {
        this.tempFov = fov;
    }

    /** Update the animation frame for the suspicion meter */
    private void updateSuspicionAnimation() {
        // Update the animation controller
        // Calculate which frame to display based on suspicion level
        int totalFrames = suspsicionMeter.getCurrentSpriteSheet().getSize() - 1;
        if (totalFrames <= 0) {
            return; // No valid frames
        }

        // Map suspicion level (0 to maxSusLevel) to frame index (0 to totalFrames)
        int frameIndex = Math.round((susLevel / maxSusLevel) * totalFrames);

        // Ensure frame index is within valid range
        frameIndex = MathUtils.clamp(frameIndex, 0, totalFrames);

        // Update the animation to show the correct frame
        suspsicionMeter.getCurrentSpriteSheet().setFrame(frameIndex);
    }

}
