package edu.cornell.cis3152.lighting.models.entities;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.cis3152.lighting.models.entities.Enemy;
import edu.cornell.gdiac.assets.AssetDirectory;

public class Guard extends Enemy {
    public static final int MAX_CHASE_TIME = 60; // 1 second
    public static final float FOV_DISTANCE = 7.0f; // Maximum detection distance.
    public static final float FOV_ANGLE = 45.0f; // Total cone angle in degrees.

    private boolean isChasing;
    private boolean meowed;
    private int chaseTimer;
    private boolean cameraAlerted;

    /** The position that this guard should move to */
    Vector2 target = null;
    Vector2 movementDirection = null;
    Vector2 targetPosition = null;

    // --- Patrol Path Variables for Guard ---
    private Vector2[] patrolPoints;
    private int currentPatrolIndex = 0;
    private static final float PATROL_THRESHOLD = 0.5f; // Distance to switch patrol points



    /**
     * Creates a new dude with degenerate settings
     * <p>
     * The main purpose of this constructor is to set the initial capsule
     * orientation.
     */
    public Guard(AssetDirectory directory, JsonValue json, JsonValue globals, float units) {
        super(directory, json, globals, units);

        // Read patrol points from JSON if available.
        if (json.has("patrol")) {
            JsonValue patrolJson = json.get("patrol");
            patrolPoints = new Vector2[patrolJson.size];
            int index = 0;
            for (JsonValue point : patrolJson) {
                float x = point.getFloat(0);
                float y = point.getFloat(1);
                patrolPoints[index++] = new Vector2(x, y);
            }
        } else {
            // Fallback to default patrol points if none are provided in JSON.
            patrolPoints = new Vector2[] {
                    new Vector2(1, 8),
                    new Vector2(14, 8)
            };
        }

        currentPatrolIndex = 0;

        isChasing = false;
        meowed = false;
        chaseTimer = 0;
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

    /** The value of target is only valid if guard is agroed or is "meowed" */
    public Vector2 getTarget() {
        if (meowed == true) {
            System.out.print("Guard is getting meow target");
        }
        return target;
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

    public void updatePatrol() {
        if (patrolPoints == null || patrolPoints.length <= 0 || isAgroed() || isMeowed()) {
            return;
        }

        Vector2 patrolTarget = patrolPoints[currentPatrolIndex];
        if (getPosition().dst(patrolTarget) < PATROL_THRESHOLD) {
            currentPatrolIndex = (currentPatrolIndex + 1) % patrolPoints.length;
            patrolTarget = patrolPoints[currentPatrolIndex];
        }
        setTarget(patrolTarget);
    }

    /** If a guard is "meowed", it is currently patrolling to the spot of the meow,
     * but they are not chasing a player. When either alerted by a security camera,
     * or if they see a player, or if they reach the spot of the meow, the guard
     * leaves the "meowed" state
     * */
    public boolean isMeowed() {
        return meowed;
    }

    /** This timer is used to determine how long a guard should chase a player
     * before giving up and returning to their patrol route */
    public int getChaseTimer() {
        return chaseTimer;
    }

    public void setChaseTimer(int value) {
        chaseTimer = value;
    }
}
