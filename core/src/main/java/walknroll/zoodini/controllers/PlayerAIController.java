package walknroll.zoodini.controllers;

import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.List;
import walknroll.zoodini.controllers.aitools.TileGraph;
import walknroll.zoodini.controllers.aitools.TileNode;
import walknroll.zoodini.models.GameLevel;
import walknroll.zoodini.models.entities.PlayableAvatar;

public class PlayerAIController {
    /** The avatar being controlled by AI */
    private PlayableAvatar follower;

    /** The avatar being followed */
    private PlayableAvatar target;

    /** Whether the follow feature is enabled */
    private boolean followEnabled;

    private PlayerAIState currState;

    /** The grid for pathfinding */
    private TileGraph tileGraph;

    /** Level of the game */
    private GameLevel level;

    /** Counter to track time in current state */
    private long ticks = 0;

    /** Minimum time to stay in a state before changing */
    private static final int STATE_CHANGE_THRESHOLD = 30;

    /** Pathfinder for A* algorithm */
    private IndexedAStarPathFinder<TileNode> pathFinder;

    private Vector2 nextTargetLocation;

    /** The calculated movement direction */
    private final Vector2 movementDirection;

    /** How close the follower needs to be to the target to stop following */
    private static final float FOLLOW_DISTANCE = 0.5f;

    /** How close the follower needs to be to a waypoint to consider it reached */
    private static final float ARRIVAL_DISTANCE = 1f;

    /** Movement dampening factor to make following look more natural */
    private static final float FOLLOW_SPEED_FACTOR = 0.75f;

    float FOLLOW_BUFFER = 0.1f;


    public PlayerAIController(PlayableAvatar follower, PlayableAvatar target, GameLevel level, TileGraph tileGraph, boolean followEnabled) {
        this.follower = follower;
        this.target = target;
        this.followEnabled = followEnabled;
        this.tileGraph = tileGraph;
        this.currState = PlayerAIState.IDLE;
        this.movementDirection = new Vector2();
        this.pathFinder = new IndexedAStarPathFinder<>(tileGraph);
    }

    /**
     * Enable or disable the follow feature
     *
     * @param enabled Whether follow should be enabled
     */
    public void setFollowEnabled(boolean enabled) {
        this.followEnabled = enabled;
    }

    /**
     * Check if follow feature is enabled
     *
     * @return Whether follow is enabled
     */
    public boolean isFollowEnabled() {
        return followEnabled;
    }

    /**
     * Get the avatar being controlled
     *
     * @return The follower avatar
     */
    public PlayableAvatar getFollower() {
        return follower;
    }

    /**
     * Get the avatar being followed
     *
     * @return The target avatar
     */
    public PlayableAvatar getTarget() {
        return target;
    }

    /**
     * Swap the follower and target
     * Useful when switching control between avatars
     */
    public void swapAvatars() {
        PlayableAvatar temp = follower;
        follower = target;
        target = temp;
        this.currState = PlayerAIState.IDLE;

    }

    /**
     * Check if follower is within follow distance of target
     */
    private boolean withinFollowDistance() {
        Vector2 followerPos = follower.getPosition();
        Vector2 targetPos = target.getPosition();
        float distance = followerPos.dst(targetPos);
        return distance <= FOLLOW_DISTANCE;
    }

    private void initializeState() {
        if (followEnabled) {
            if (currState == PlayerAIState.IDLE) {
                currState = PlayerAIState.FOLLOWING;
            }
        } else {
            if (currState == PlayerAIState.FOLLOWING) {
                currState = PlayerAIState.IDLE;
            }
        }
    }

    /**
     * Update the AI state machine
     */
    private void updatePlayerAIState() {
        if (!followEnabled) {
            currState = PlayerAIState.IDLE;
            return;
        }
        PlayerAIState potentialState = currState;
        switch (currState) {
            case IDLE:
                // Player is outside of follow distance to target; IDLE -> FOLLOWING
                if (!withinFollowDistance()) {
                    potentialState = PlayerAIState.FOLLOWING;
                }
                break;
            case FOLLOWING:
                if (withinFollowDistance()) {
                    potentialState = PlayerAIState.IDLE;
                }
                break;
            default:
                // Do nothing (should not happen)
                break;
        }

        // Only change state if we've been in the current state long enough
        // or if we're forced to change by disabling/enabling follow
        if (potentialState != currState) {
            if (ticks >= STATE_CHANGE_THRESHOLD) {
                currState = potentialState;
                ticks = 0; // Reset counter on state change
            }
        }
    }

    public void update(float dt) {
        ticks++;
        updatePlayerAIState();
        System.out.println("Current State: " + currState);
        setNextTargetLocation();
        setMovementDirection();
    }


    /**
     * Helper function that determines the next waypoint location based on
     * pathfinding.
     * Uses the game graph to find a path from the guard's current position to the
     * target location.
     *
     * @param targetLocation The destination the guard is trying to reach
     * @return The next position the guard should move towards
     */
//    private Vector2 getNextWaypointLocation(Vector2 targetLocation) {
//        List<TileNode> path = tileGraph.getPath(follower.getPosition().cpy(), targetLocation.cpy(), pathFinder);
//        if (path.isEmpty()) {
//            if (currState == PlayerAIState.FOLLOWING) {
//                return target.getPosition().cpy();
//            }
//            return follower.getPosition().cpy();
//        }
//
//        int pathIdx = 0;
//        Vector2 nextStep = tileGraph.tileToWorld(path.get(pathIdx));
//        final float MIN_STEP_DISTANCE = 1F;
//
//        // Skip steps that are too close to the guard to prevent jittering
//        while (nextStep.dst(follower.getPosition().cpy()) < MIN_STEP_DISTANCE && pathIdx < path.size() - 1) {
//            pathIdx++;
//            nextStep = tileGraph.tileToWorld(path.get(pathIdx));
//        }
//        return nextStep;
//    }

    /**
     * Helper function that determines the next waypoint location based on
     * pathfinding with path smoothing to prevent zigzag movement.
     * Uses the game graph to find a path from the follower's current position to the
     * target location, then intelligently picks the furthest visible waypoint.
     *
     * @param targetLocation The destination the follower is trying to reach
     * @return The next position the follower should move towards
     */
    private Vector2 getNextWaypointLocation(Vector2 targetLocation) {
        // Get the raw path from A* pathfinding
        List<TileNode> path = tileGraph.getPath(follower.getPosition().cpy(), targetLocation.cpy(), pathFinder);

        // Handle empty path case
        if (path.isEmpty()) {
            if (currState == PlayerAIState.FOLLOWING) {
                return target.getPosition().cpy();
            }
            return follower.getPosition().cpy();
        }

        // Convert path to world coordinates
        List<Vector2> worldPath = new ArrayList<>();
        for (TileNode node : path) {
            worldPath.add(tileGraph.tileToWorld(node));
        }

        // Start with the first waypoint as our target
        int pathIndex = 0;
        Vector2 nextStep = worldPath.get(pathIndex);

        // Skip waypoints that are too close to prevent jittering
        final float MIN_STEP_DISTANCE = 1.0f;
        while (nextStep.dst(follower.getPosition()) < MIN_STEP_DISTANCE && pathIndex < worldPath.size() - 1) {
            pathIndex++;
            nextStep = worldPath.get(pathIndex);
        }

        // Now look ahead for the furthest visible waypoint
        // This is the key to reducing zigzag movement
        int furthestVisibleIndex = pathIndex;

        // Try to find a waypoint further along the path that we can move to directly
        for (int i = pathIndex + 1; i < worldPath.size(); i++) {
            // Check if we have a clear line of sight to this waypoint
            if (tileGraph.hasLineOfSight(follower.getPosition(), worldPath.get(i))) {
                furthestVisibleIndex = i;
            } else {
                // Stop at the first waypoint we can't see directly
                break;
            }
        }

        // Return the furthest waypoint that has a clear line of sight
        return worldPath.get(furthestVisibleIndex);
    }

    /**
     * Helper function that updates the next target location based on the guard's
     * current state.
     * Handles different targeting logic for patrol, chase, return, and distracted
     * states.
     */
    private void setNextTargetLocation() {
        Vector2 targetLocation = null;

        switch (currState) {
            case IDLE:
                // Set the target to the follower's current position
                targetLocation = follower.getPosition().cpy();
                break;
            case FOLLOWING:
                // Get the next waypoint location based on pathfinding
                targetLocation = getNextWaypointLocation(target.getPosition());
                break;
            default:
                // Do nothing (should not happen)
                break;
        }

        this.nextTargetLocation = targetLocation;
    }

    /**
     * Returns the guard's current target location for movement.
     *
     * @return The Vector2 position the guard is currently moving towards
     */
    public Vector2 getNextTargetLocation() {
        return nextTargetLocation;
    }

    /**
     * Calculate the direction for the follower to move
     */
    private void setMovementDirection() {
        // Don't move if in IDLE state or if there's no target
        if (currState == PlayerAIState.IDLE || nextTargetLocation == null) {
            movementDirection.setZero();
            return;
        }

        // Calculate direction to the next waypoint
        Vector2 followerPos = follower.getPosition();

        Vector2 direction = nextTargetLocation.cpy().sub(followerPos).nor();
        float distance = direction.len();


        if (distance > FOLLOW_DISTANCE + FOLLOW_BUFFER) {
            movementDirection.x = direction.x * 0.75f;
            movementDirection.y = direction.y * 0.75f;
        }
        else if (distance > FOLLOW_DISTANCE - FOLLOW_BUFFER) {
            float speedFactor = (distance - (FOLLOW_DISTANCE - FOLLOW_BUFFER)) / (2 * FOLLOW_BUFFER);
            speedFactor = Math.max(0.1f, speedFactor) * 0.75f;
            movementDirection.x = direction.x * speedFactor;
            movementDirection.y = direction.y * speedFactor;
        }
    }

    /**
     * Gets the calculated horizontal movement value.
     * This should be passed directly to moveAvatar.
     *
     * @return horizontal movement value (-1 to 1)
     */
    public float getHorizontalMovement() {
        return movementDirection.x;
    }

    /**
     * Gets the calculated vertical movement value.
     * This should be passed directly to moveAvatar.
     *
     * @return vertical movement value (-1 to 1)
     */
    public float getVerticalMovement() {
        return movementDirection.y;
    }

    /**
     * Returns whether the follower is currently following a path
     *
     * @return Whether the follower is actively following
     */
    public boolean isActivelyFollowing() {
        return followEnabled && currState == PlayerAIState.FOLLOWING;
    }

    /**
     * Enum representing the possible states of the Player Controller AI state machine.
     */
    private static enum PlayerAIState {
        /** Player is Idle */
        IDLE,
        /** Player is following the target */
        FOLLOWING;
        private PlayerAIState() {}
    }

}
