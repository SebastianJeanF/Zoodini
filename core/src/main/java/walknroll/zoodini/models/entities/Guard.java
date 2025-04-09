package walknroll.zoodini.models.entities;

import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonValue;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.SpriteSheet;
import walknroll.zoodini.models.entities.Enemy;
import walknroll.zoodini.utils.animation.Animation;
import walknroll.zoodini.utils.animation.AnimationController;
import walknroll.zoodini.utils.animation.AnimationState;

import static walknroll.zoodini.utils.animation.AnimationState.SUSPICION_METER;

public class Guard extends Enemy {
    public static final int MAX_CHASE_TIME = 60; // 1 second
    public static final float FOV_DISTANCE = 7.0f; // Maximum detection distance.
    public static final float FOV_ANGLE = 45.0f; // Total cone angle in degrees.

    private float fov;
    private float viewDistance;
    private boolean isChasing;
    private boolean meowed;
    private int chaseTimer;
    private boolean cameraAlerted;
    private Avatar aggroTarget;

    /** The position that this guard should move to */
    Vector2 target = null;
    Vector2 movementDirection = null;
    Vector2 targetPosition = null;

    // --- Patrol Path Variables for Guard ---
    private Vector2[] patrolPoints;
    private int currentPatrolIndex = 0;
    private static final float PATROL_THRESHOLD = 0.5f; // Distance to switch patrol points

    private final AnimationController suspsicionMeter;

    /**
     * Creates a new dude with degenerate settings
     * <p>
     * The main purpose of this constructor is to set the initial capsule
     * orientation.
     */
    public Guard(AssetDirectory directory, MapProperties properties, JsonValue globals, float units) {
        super(directory, properties, globals, units);
        fov = properties.get("fov", Float.class);
        currentPatrolIndex = 0;
        cameraAlerted = false;
        isChasing = false;
        meowed = false;
        chaseTimer = 0;
        AnimationState state = AnimationState.SUSPICION_METER;
        suspsicionMeter = new AnimationController(state);
        viewDistance = properties.get("viewDistance", Float.class);


        String animKey = globals.getString("suspicion");
        final int START_FRAME = 0;
        final int FRAME_DELAY = 0;
        final boolean IS_LOOP = true;

        if (animKey != null) {
            SpriteSheet animSheet = directory.getEntry(animKey, SpriteSheet.class);
            System.out.println("Number of frames: " + animSheet.getSize());
            animSheet.setFrame(START_FRAME);
            Animation anim = new Animation(
                animSheet,
                START_FRAME,
                animSheet.getSize() - 1,
                FRAME_DELAY,
                IS_LOOP
            );
            suspsicionMeter.addAnimation(state, anim);
        }
    }




    private void setupAnimations(AssetDirectory directory, JsonValue globals) {

    }

    public Vector2[] getPatrolPoints() {
        return patrolPoints;
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

    public void update(float dt) {
        applyForce();
    }

    /** Get current movement direction of guard.
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

//    public void updatePatrol() {
//        if (patrolPoints == null || patrolPoints.length <= 0 || isAgroed() || isMeowed()) {
//            return;
//        }
//
//        Vector2 patrolTarget = patrolPoints[currentPatrolIndex];
//        if (getPosition().dst(patrolTarget) < PATROL_THRESHOLD) {
//            currentPatrolIndex = (currentPatrolIndex + 1) % patrolPoints.length;
//            patrolTarget = patrolPoints[currentPatrolIndex];
//        }
//        setTarget(patrolTarget);
//    }

    /** If a guard is "meowed", it is currently patrolling to the spot of the meow,
     * but they are not chasing a player. When either alerted by a security camera,
     * or if they see a player, or if they reach the spot of the meow, the guard
     * leaves the "meowed" state
     * */
    public boolean isMeowed() {
        return meowed;
    }

    public Avatar getAggroTarget() {
        return aggroTarget;
    }

    public void setAggroTarget(Avatar target) {
        aggroTarget = target;
    }

    /** This timer is used to determine how long a guard should chase a player
     * before giving up and returning to their patrol route */
    public int getChaseTimer() {
        return chaseTimer;
    }

    public void setChaseTimer(int value) {
        chaseTimer = value;
    }

    public float getFov(){
        return fov;
    }

    public float getViewDistance(){
        return viewDistance;
    }





    /** The value of target is only valid if guard is agroed or is "meowed" */
    public Vector2 getTarget() {
        if (meowed == true) {
            // System.out.print("Guard is getting meow target");
        }
        return target;
    }

    int susTick = 0;
    int FRAMES_PER_CHANGE = 20;

    public void draw(SpriteBatch batch) {
        susTick++;
        super.draw(batch);
        if (susTick % FRAMES_PER_CHANGE == 0) {
            suspsicionMeter.update();
            suspsicionMeter.getCurrentSpriteSheet().setFrame(suspsicionMeter.getCurrentFrame());
        }

        if (suspsicionMeter != null) {
            float PIXEL_PER_WORLD_UNIT = getObstacle().getPhysicsUnits();
            float guardXPixel = getPosition().x * PIXEL_PER_WORLD_UNIT;
            float guardYPixel = getPosition().y * PIXEL_PER_WORLD_UNIT;
            float scale = 0.4f;

            float xPixelOffset = -64f * scale;
            float yPixelOffset = 20f;

            // Get the original width and height of the sprite sheet
            float originalWidth = suspsicionMeter.getCurrentSpriteSheet().getRegionWidth();
            float originalHeight = suspsicionMeter.getCurrentSpriteSheet().getRegionHeight();

            batch.draw(
                suspsicionMeter.getCurrentSpriteSheet(),
                guardXPixel + xPixelOffset,
                guardYPixel + yPixelOffset,
                originalWidth * scale,
                originalHeight * scale
            );
        }
    }
}
