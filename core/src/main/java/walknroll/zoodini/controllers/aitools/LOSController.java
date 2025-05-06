package walknroll.zoodini.controllers.aitools;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import walknroll.zoodini.controllers.aitools.LOSCallback;

/**
 * Container for multiple raycasts to check line of sight in different directions
 */
public class LOSController {
    // Number of rays to cast
    private int NUM_RAYS;

    // Angle spread between rays (in degrees)
    private float ANGLE_SPREAD;

    // Maximum ray distance
    private float MAX_DISTANCE;

    // Array of ray callbacks
    private LOSCallback[] callbacks;

    // Reference to the Box2D world for raycasting
    private World world;

    /**
     * Create a line of sight controller with default settings
     *
     * @param world Box2D world for raycasting
     */
    public LOSController(World world, short obstacleCategory) {
        this.world = world;
        this.NUM_RAYS = 15;
        this.ANGLE_SPREAD = 0.4f;
        this.MAX_DISTANCE = 220F;
        // Initialize callbacks
        callbacks = new LOSCallback[NUM_RAYS];
        for (int i = 0; i < NUM_RAYS; i++) {
            callbacks[i] = new LOSCallback(obstacleCategory);
        }
    }


    /**
     * Reset all ray callbacks
     */
    public void reset() {
        for (int i = 0; i < NUM_RAYS; i++) {
            callbacks[i].reset();
        }
    }

    /**
     * Check if there's a clear line of sight between two points
     * Uses multiple rays to check around obstacles
     *
     * @param start Starting position
     * @param end Target position
     * @param safetyMargin Safety margin around obstacles
     * @return true if there's a clear line of sight
     */
    public boolean hasLineOfSight(Vector2 start, Vector2 end, float safetyMargin) {
        // Reset all callbacks
        reset();

        // Base direction vector
        Vector2 direction = new Vector2(end).sub(start).nor();
        float baseAngle = direction.angleDeg();
        float distance = start.dst(end);

        // Limit distance to max
        distance = Math.min(distance, MAX_DISTANCE);

        // Cast center ray
        Vector2 rayEnd = new Vector2(start).add(direction.cpy().scl(distance));
        callbacks[0].setRayPositions(start, rayEnd);
        world.rayCast(callbacks[0], start, rayEnd);

        // If center ray doesn't hit anything, we have direct line of sight
        if (!callbacks[0].hitObstacle()) {
            // Additional safety check for proximity to obstacles
            if (safetyMargin > 0) {
                // Cast rays perpendicular to the main ray to check for obstacles nearby
                Vector2 perpendicular = new Vector2(-direction.y, direction.x).nor().scl(safetyMargin);

                // Sample points along the path
                int samples = (int)(distance / safetyMargin) + 1;
                for (int i = 0; i < samples; i++) {
                    float t = (float)i / (samples - 1);
                    Vector2 pointOnRay = new Vector2(start).lerp(end, t);

                    // Check left side
                    Vector2 leftCheck = new Vector2(pointOnRay).add(perpendicular);
                    world.rayCast(callbacks[1], pointOnRay, leftCheck);

                    // Check right side
                    Vector2 rightCheck = new Vector2(pointOnRay).sub(perpendicular);
                    world.rayCast(callbacks[2], pointOnRay, rightCheck);

                    // If either side is too close to an obstacle, return false
                    if (callbacks[1].hitObstacle() || callbacks[2].hitObstacle()) {
                        return false;
                    }
                }
            }

            return true;
        }

        // Cast additional rays at various angles
        int raysFired = 1; // Center ray already fired
        for (int i = 1; i <= (NUM_RAYS - 1) / 2; i++) {
            if (raysFired >= NUM_RAYS) break;

            // Cast ray to the right
            float rightAngle = baseAngle + (i * ANGLE_SPREAD);
            Vector2 rightDir = new Vector2(1, 0).rotateDeg(rightAngle).nor();
            Vector2 rightEnd = new Vector2(start).add(rightDir.scl(distance));
            callbacks[raysFired].setRayPositions(start, rightEnd);
            world.rayCast(callbacks[raysFired], start, rightEnd);
            raysFired++;

            if (raysFired >= NUM_RAYS) break;

            // Cast ray to the left
            float leftAngle = baseAngle - (i * ANGLE_SPREAD);
            Vector2 leftDir = new Vector2(1, 0).rotateDeg(leftAngle).nor();
            Vector2 leftEnd = new Vector2(start).add(leftDir.scl(distance));
            callbacks[raysFired].setRayPositions(start, leftEnd);
            world.rayCast(callbacks[raysFired], start, leftEnd);
            raysFired++;
        }

        // Check if any ray has a clear path
        for (int i = 0; i < NUM_RAYS; i++) {
            if (!callbacks[i].hitObstacle()) {
                // Found a clear ray
                return true;
            }
        }

        // No clear path found
        return false;
    }

    /**
     * Simplified version that uses a default safety margin
     */
    public boolean hasLineOfSight(Vector2 start, Vector2 end) {
        return hasLineOfSight(start, end, 0.25f);
    }

    /**
     * Get the callback for a specific ray (for debugging)
     */
    public LOSCallback getCallback(int index) {
        if (index >= 0 && index < NUM_RAYS) {
            return callbacks[index];
        }
        return null;
    }
}
