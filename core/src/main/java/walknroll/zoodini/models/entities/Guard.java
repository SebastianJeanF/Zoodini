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
import walknroll.zoodini.utils.DebugPrinter;
import walknroll.zoodini.utils.LevelPortal;
import walknroll.zoodini.utils.animation.Animation;
import walknroll.zoodini.utils.animation.AnimationController;
import walknroll.zoodini.utils.animation.AnimationState;

import static walknroll.zoodini.utils.animation.AnimationState.IDLE_NORTH;
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
    private static final int MEDIUM_ZONE_SUS_INCREASE = 5;
    private static final int FAR_ZONE_SUS_INCREASE = 2;


    private float fov;
    private float viewDistance;
    private boolean isChasing;
    private boolean isLookingAround;

    private boolean meowed;
    private int chaseTimer;
    private boolean cameraAlerted;

    private PlayableAvatar aggroTarget;
    private boolean seesPlayer;
    private PlayableAvatar seenPlayer;

    /** The position that this guard should move to */
    Vector2 target = null;
    Vector2 movementDirection = null;
    Vector2 targetPosition = null;
    /** Direction guard is currently facing */
    private Vector2 currentDirection = new Vector2(0, -1); // Default facing up
    private Vector2 targetDirection = new Vector2(0, -1);

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

    private final float agroedForce;
    private final float alertedForce;
    private final float susForce;
    private final float distractedForce;
    private final float blindedForceScale;

    private boolean isIdle = false;
    private float idleAngle;


    public float getSusForce() {
        return susForce;
    }

    public float getAgroedForce() {
        return agroedForce;
    }

    public float getBlindedForceScale() {
        return blindedForceScale;
    }

    public float getAlertedForce() {
        return alertedForce;
    }

    public float getDistractedForce() {
        return distractedForce;
    }

    public static void setSuspicionMeterCuriousTexture(Texture suspicionMeterCurious) {
        Guard.SUSPICION_METER_CURIOUS = suspicionMeterCurious;
    }

    public static boolean isLoaded() {
        return Guard.SUSPICION_METER_CURIOUS != null;
    }

    public float getIdleAngle(){
        return idleAngle;
    }

    /**
     * Creates a new dude with degenerate settings
     * <p>
     * The main purpose of this constructor is to set the initial capsule
     * orientation.
     */
    public Guard(MapProperties properties, JsonValue constants, float units) {
        super(properties, constants, units);
        fov = constants.getFloat("fov");
        idleAngle = properties.get("angle", Float.class);
        setAngle(MathUtils.degreesToRadians * idleAngle);
        animationController.setState(IDLE_NORTH);
        currentPatrolIndex = 0;
        cameraAlerted = false;
        isChasing = false;
        isLookingAround = false;
        meowed = false;
        chaseTimer = 0;
        AnimationState state = AnimationState.SUSPICION_METER;
        suspsicionMeter = new AnimationController(state);
        viewDistance = constants.getFloat("viewDistance");
        susThreshold = 5F;
        maxSusLevel = 100F;
        seesPlayer = false;

        agroedForce = constants.getFloat("agroedForce");
        alertedForce = constants.getFloat("alertedForce");
        susForce = constants.getFloat("susForce");
        distractedForce = constants.getFloat("distractedForce");

        blindedForceScale = constants.getFloat("blindedForceScale");


        MapObject path = properties.get("path", MapObject.class);
        if (path instanceof PolylineMapObject line) {
            float[] vertices = line.getPolyline().getTransformedVertices().clone();
            for (int i = 0; i < vertices.length; i++) {
                vertices[i] /= units;
            }
            setPatrolPoints(vertices);
        } else { setPatrolPoints(new Vector2[] {this.getPosition()});}

        originalViewDistance = viewDistance;
        originalFov = fov;
        tempViewDistance = viewDistance;
        tempFov = fov;

        obstacle.setUserData(this);
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

    public void setSusLevel(float susLevel) {
        this.susLevel = MathUtils.clamp(susLevel, 0.0F, maxSusLevel);
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

    public void setSeenPlayer(PlayableAvatar seenPlayer) {
        this.seenPlayer = seenPlayer;
    }

    public PlayableAvatar getSeenPlayer() {
        return seenPlayer;
    }

    public void setLookingAround(boolean lookingAround) {
        isLookingAround = lookingAround;
    }

    public boolean isLookingAround() {
        return isLookingAround;
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

    public void setIdle(boolean idle) {
        isIdle = idle;
    }

    public boolean isIdle() {
        return isIdle;
    }

    public PlayableAvatar getAggroTarget() {
        return aggroTarget;
    }

    public void setAggroTarget(PlayableAvatar target) {
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

    public void update(float dt) {
        super.update(dt);

        // If we have a movement direction, update orientation
        if(movementDirection == null || movementDirection.len() < 0.0001f){
            setAngle(MathUtils.degreesToRadians * idleAngle);
        }
        if (movementDirection != null && movementDirection.len2() > 0.0001f) {
            setAngle(movementDirection.angleRad());
        }
        if (tempViewDistance < originalViewDistance && !inkBlinded) {
            tempViewDistance += dt;
        }
        if (tempFov < originalFov && !inkBlinded) {
            tempFov += dt;
        }
        applyForce();

        if(isIdle) {
            float angle = idleAngle % 360;
            if (angle < 0) angle += 360;

            if (angle >= 315 || angle < 45) {
                if(inkBlinded){
                    animationController.setState(AnimationState.IDLE_RIGHT_BLIND);
                } else {
                    animationController.setState(AnimationState.IDLE_RIGHT);
                }
            } else if (angle >= 45 && angle < 135) {
                if(inkBlinded){
                    animationController.setState(AnimationState.IDLE_NORTH_BLIND);
                } else {
                    animationController.setState(AnimationState.IDLE_NORTH);
                }
            } else if (angle >= 135 && angle < 225) {
                if(inkBlinded){
                    animationController.setState(AnimationState.IDLE_LEFT_BLIND);
                } else {
                    animationController.setState(AnimationState.IDLE_LEFT);
                }
            } else { // 225 <= angle < 315
                if(inkBlinded){
                    animationController.setState(AnimationState.IDLE_SOUTH_BLIND);
                } else {
                    animationController.setState(AnimationState.IDLE_SOUTH);
                }
            }
        }
    }

    /** The value of target is only valid if guard is agroed or is "meowed" */
    public Vector2 getTarget() {
        if (meowed == true) {
            // System.out.print("Guard is getting meow target");
        }
        return target;
    }

    public void draw(SpriteBatch batch) {
        super.draw(batch);
        drawSuspicionMeter(batch);
    }


    public void drawSuspicionMeter(SpriteBatch batch) {
        float BASELINE_PX = 32;
        if (suspsicionMeter == null
            || suspsicionMeter.getCurrentSpriteSheet() == null
            || !Guard.isLoaded() || (susLevel == 0 && !isMeowed())) {
            return;
        }

        float PIXEL_PER_WORLD_UNIT = getObstacle().getPhysicsUnits();
        float guardXPixel = getPosition().x * PIXEL_PER_WORLD_UNIT;
        float guardYPixel = getPosition().y * PIXEL_PER_WORLD_UNIT;

        float SCALE = 0.2f * (PIXEL_PER_WORLD_UNIT / BASELINE_PX);
        float X_PIXEL_OFFSET = (-80f * SCALE);
        float Y_PIXEL_OFFSET = 140f * SCALE;



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

    public boolean isEven(int number) {
        switch (number) {
            case 1: return false;
            case 2: return true;
            case 3: return false;
            case 4: return true;
            case 5: return false;
            case 6: return true;
            case 7: return false;
            case 8: return true;
            default: throw new RuntimeException("passed illegal argument to isEven");
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
            else if (guardState == AnimationState.WALK_DOWN || guardState == AnimationState.WALK_DOWN_BLIND) {
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
