package walknroll.zoodini.models.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import edu.cornell.gdiac.graphics.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.graphics.Pixmap;

import edu.cornell.gdiac.assets.AssetDirectory;
import walknroll.zoodini.models.entities.Enemy;

public class Guard extends Enemy {
    public static final int MAX_CHASE_TIME = 60; // 1 second
    public static final float FOV_DISTANCE = 7.0f; // Maximum detection distance.
    public static final float FOV_ANGLE = 45.0f; // Total cone angle in degrees.

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

    private float susLevel;
    private float maxSusLevel;

    private final float DEAGRRO_PERIOD = 60F;
    private final float ALERT_DEAGRRO_PERIOD = 300F;
    private float deAggroTimer;

    private static final float BAR_WIDTH = 50.0f;
    private static final float BAR_HEIGHT = 5.0f;
    private static final float BAR_OFFSET_Y = 10.0f;
    private static final float AGGRO_BAR_OFFSET_Y = 20.0f;

    private Texture susBarTexture;
    private Texture deAggroBarTexture;


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
                    new Vector2(4f, 8),
                    new Vector2(14, 8)
            };
        }

        currentPatrolIndex = 0;
        cameraAlerted = false;
        isChasing = false;
        meowed = false;
        chaseTimer = 0;
        susLevel = 0F;
        maxSusLevel = 100F;
        deAggroTimer = 0F;

        // sus-bar texture
        Pixmap pixmap = new Pixmap((int) BAR_WIDTH, (int) BAR_HEIGHT, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.GREEN);
        pixmap.fill();
        susBarTexture = new Texture(pixmap);
        pixmap.dispose();

        // Aggro-bar texture
        pixmap = new Pixmap((int) BAR_WIDTH, (int) BAR_HEIGHT, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.RED);
        pixmap.fill();
        deAggroBarTexture = new Texture(pixmap);
        pixmap.dispose();
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
        this.susLevel = MathUtils.clamp(susLevel + delta, 0.0F, 100.0F);
    }

    public boolean isMaxSusLevel() {
        return susLevel == maxSusLevel;
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

    public void startDeAggroTimer() {
        this.deAggroTimer = isCameraAlerted() ? ALERT_DEAGRRO_PERIOD : DEAGRRO_PERIOD;
    }





    public void drawSusLevelBar(SpriteBatch batch) {
        if (susBarTexture == null) {
            System.err.println("Error: barTexture is null");
            return;
        }

        System.out.println("Drawing sus level bar");

        float susPercentage = susLevel / maxSusLevel;
        float barWidth = BAR_WIDTH * susPercentage;

        Vector2 position = this.getPosition();
        float barX = position.x;
        float barY = position.y;

        batch.draw(susBarTexture, barX * 62.5f, barY * 62.5f + BAR_OFFSET_Y, barWidth, BAR_HEIGHT);

        float deAggroPercentage = deAggroTimer / DEAGRRO_PERIOD;
        barWidth = BAR_WIDTH * deAggroPercentage;
        batch.draw(deAggroBarTexture, barX * 62.5f, barY * 62.5f + AGGRO_BAR_OFFSET_Y, barWidth, BAR_HEIGHT);
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
            // System.out.print("Guard is getting meow target");
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


    public void update(float dt) {
        applyForce();
    }

    @Override
    public void draw(SpriteBatch batch) {
        super.draw(batch);
        drawSusLevelBar(batch);
    }
}
