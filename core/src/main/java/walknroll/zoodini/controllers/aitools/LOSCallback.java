package walknroll.zoodini.controllers.aitools;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import walknroll.zoodini.models.GameLevel;
import walknroll.zoodini.utils.DebugPrinter;

/**
 * Line of sight callback for raycasting
 */
public class LOSCallback implements RayCastCallback {
    private Array<Fixture> fixtures = new Array<>();
    private Array<Float> fractions = new Array<>();
    private Vector2 rayStart = new Vector2();
    private Vector2 rayEnd = new Vector2();
    private Vector2 nearestPoint = new Vector2();
    private boolean hitObstacle = false;
    private short obstacleCategory;

    public LOSCallback(short obstacleCategory) {
        this.obstacleCategory = obstacleCategory;
    }

    /**
     * Reset the callback for reuse
     */
    public void reset() {
        fixtures.clear();
        fractions.clear();
        hitObstacle = false;
    }

    /**
     * Set the ray positions for debugging and calculations
     */
    public void setRayPositions(Vector2 start, Vector2 end) {
        rayStart.set(start);
        rayEnd.set(end);
    }

    /**
     * Get the start position of the ray
     */
    public Vector2 getRayStart() {
        return rayStart;
    }

    /**
     * Get the end position of the ray
     */
    public Vector2 getRayEnd() {
        return rayEnd;
    }

    /**
     * Implementation of the Box2D raycasting callback
     */
    @Override
    public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
        // Skip sensor fixtures
        if (fixture.isSensor()) {
            return -1; // Continue checking other fixtures
        }

        // Store the fixture and fraction
        fixtures.add(fixture);
        fractions.add(fraction);

        // Check if this is an obstacle using filter bits (same way VisionCone does)
        if ((obstacleCategory & fixture.getFilterData().categoryBits) != 0) {
            DebugPrinter.println("Hit obstacle: " + fixture.getUserData());
            hitObstacle = true;
        }

        // Calculate the nearest point when we find one
        if (fixtures.size == 1 || fraction < fractions.get(0)) {
            Vector2 dir = new Vector2(rayEnd).sub(rayStart);
            nearestPoint.set(rayStart).add(dir.scl(fraction));
        }

        // Continue the raycast to find all intersections
        return -1;
    }

    /**
     * Get the closest fixture hit by the ray
     */
    public Fixture getClosestFixture() {
        if (fixtures.size == 0) {
            return null;
        }

        Fixture minFixture = fixtures.get(0);
        float minFraction = fractions.get(0);

        for (int i = 1; i < fixtures.size; i++) {
            if (fractions.get(i) < minFraction) {
                minFixture = fixtures.get(i);
                minFraction = fractions.get(i);
            }
        }

        // Calculate the nearest point
        Vector2 dir = new Vector2(rayEnd).sub(rayStart);
        nearestPoint.set(rayStart).add(dir.scl(minFraction));

        return minFixture;
    }

    /**
     * Get the nearest point where the ray hit something
     */
    public Vector2 getNearestPoint() {
        return nearestPoint;
    }

    /**
     * Check if the ray hit any obstacles
     */
    public boolean hitObstacle() {
        return hitObstacle;
    }
}
