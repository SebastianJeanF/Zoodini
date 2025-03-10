package edu.cornell.cis3152.lighting.models;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;

public class Guard extends Enemy {
    public static final int MAX_CHASE_TIME = 60; // 1 second
    public static final float FOV_DISTANCE = 7.0f; // Maximum detection distance.
    public static final float FOV_ANGLE = 45.0f; // Total cone angle in degrees.

    private boolean isChasing;
    private boolean meowed;
    private int chaseTimer;
    private boolean cameraAlerted;


    /**
     * Creates a new dude with degenerate settings
     * <p>
     * The main purpose of this constructor is to set the initial capsule
     * orientation.
     */
    public Guard(AssetDirectory directory, JsonValue json, float units) {
        super(directory, json, units);

        isChasing = false;
        meowed = false;
        chaseTimer = 0;
    }

    public boolean isCameraAlerted() {
        return cameraAlerted;
    }

    public void setCameraAlerted(boolean value) {
        cameraAlerted = value;
    }

    public

    /** The position that this guard should move to */
    Vector2 target = null;

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

    public void setTarget(Vector2 target) {

        this.target = target;
    }

    public void setAgroed(boolean agroed) {
        isChasing = agroed;
    }

    public void setMeow(boolean meowed) {
        this.meowed = meowed;
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
