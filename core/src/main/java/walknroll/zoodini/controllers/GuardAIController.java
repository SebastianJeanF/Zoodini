package walknroll.zoodini.controllers;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;


import edu.cornell.gdiac.graphics.SpriteBatch;
import java.util.ArrayList;
import walknroll.zoodini.models.GameLevel;
import walknroll.zoodini.models.entities.Avatar;
import walknroll.zoodini.models.entities.Guard;
//import walknroll.zoodini.utils.GameGraph;
import walknroll.zoodini.controllers.aitools.TileGraph;
import walknroll.zoodini.utils.GameGraph.Node;

import java.util.List;

/**
 * Controller class that manages the AI behavior for guard entities.
 * Handles guard patrolling, chasing, returning to patrol, and responding to distractions.
 */
public class GuardAIController {
    /** Guard identifier for this AI controller */
    private final Guard guard;
    /** Target of the guard (to chase) */
    private Avatar targetPlayer;
    /** Level of the game */
    private GameLevel level;
    /** Position of the last location of meow */
    private Vector2 distractPosition;
    /** Position of the last location of the player (after they are under camera) */
    private Vector2 cameraAlertPosition;
    /** Current state of the finite state machine */
    private GuardState currState;
    /** Waypoints for the guard to patrol */
    private Vector2[] waypoints;
    /** Current waypoint index */
    private int currentWaypointIndex;
    /** Temporary distraction flag */
    private boolean tempDistract;


    private long ticks;
    /** Graph representation of the game */
    private TileGraph tileGraph;


    private Vector2 nextTargetLocation;


    /** Min distance from waypoint where the guard will recalculate to next waypoint*/
    private final float WAYPOINT_RADIUS = 0.25F;



    /**
     * Constructs a new GuardAIController for a specific guard.
     *
     * @param guard The guard entity that this controller will manage
     * @param level The game level containing relevant game state information
     * @param tileGraph The graph representation of the level for pathfinding
     */
    public GuardAIController(Guard guard, GameLevel level, TileGraph tileGraph) {
        this.guard = guard;
        this.level = level;
        this.currState = GuardState.PATROL;
        this.waypoints = guard.getPatrolPoints();
        this.currentWaypointIndex = 0;
        this.tileGraph = tileGraph;
        this.ticks = 0L;
        this.distractPosition = new Vector2(0, 0);
        this.cameraAlertPosition = new Vector2(0, 0);
    }


    /**
     * Helper function to retrieve the currently active player avatar.
     *
     * @return The active player avatar from the game level
     */
    private Avatar getActivePlayer() {
        return level.getAvatar();
    }

    /**
     * Helper function that checks if a distraction from the cat's ability has occurred.
     *
     * @return true if the cat player has used its distraction ability, false otherwise
     */
    private boolean didDistractionOccur() {
        InputController input = InputController.getInstance();
        return input.didAbility() && getActivePlayer().getAvatarType() == Avatar.AvatarType.CAT;
    }

    /**
     * Helper function that checks if the guard has reached its patrol path.
     *
     * @return true if the guard is close enough to its next waypoint or if there are no waypoints, false otherwise
     */
    private boolean hasReachedPatrolPath() {
        if (waypoints.length == 0) {
            return true;
        }
        // Check if guard is close enough to the nearest waypoint
        return distanceFromGuard(nextTargetLocation) <= WAYPOINT_RADIUS;
    }

    /**
     * Helper function to find the nearest waypoint to the guard's current position.
     * Updates the currentWaypointIndex to match the nearest waypoint.
     * This is used in the RETURN state to find the nearest waypoint to return to.
     *
     * @return The Vector2 position of the nearest patrol waypoint
     */
    private Vector2 findNearestWaypoint() {
        if (waypoints.length == 0) {
            return guard.getPosition();
        }

        Vector2 nearest = waypoints[0];
        float minDistance = guard.getPosition().dst(nearest);

        for (int i = 1; i < waypoints.length; i++) {
            float distance = guard.getPosition().dst(waypoints[i]);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = waypoints[i];
                currentWaypointIndex = i; // Update the current waypoint index
            }
        }

        return nearest;
    }



    /**
     * Updates the guard's AI state and behavior.
     * This is the main function that should be called each frame to progress the guard's AI.
     * Handles suspicion level changes, state transitions, and movement target updates.
     */
    public void update() {
        ticks++;
        // Update suspicion level
        if (currState != GuardState.CHASE) { // Only update when not chasing
            if (this.guard.isAgroed()) { // In guard's line of sight
                guard.deltaSusLevel(3); // Increase suspicion
            } else {
                guard.deltaSusLevel(-1); // Decrease suspicion
            }
        } else {
            // not in guard's line of sight and not under camera
            if (!this.guard.isAgroed() && !targetPlayer.isUnderCamera()) {
                guard.deltaDeAggroTimer(-1); // decrease deAggroTimer
            }
        }

        setNextTargetLocation();

        switch(this.currState) {
            case PATROL:
                // Guard is not max sus level but is suspicious; PATROL -> SUSPICIOUS
                if (guard.isSus()) {
                    currState = GuardState.SUSPICIOUS;
                }
                // Guard is not sus and is meowed; PATROL -> DISTRACTED
                // Due to ordering of checks, this will only happen if the guard is not suspicious
                // This makes sense since we don't want the guard to deagrro by being meowed
                else if (tempDistract) {
                    currState = GuardState.DISTRACTED;
                    guard.setMeow(true);
                }
                // Guard is not sus, not meowed, but player under camera; PATROL -> ALERTED
                // Due to ordering of checks, this will only happen if the guard is not suspicious
                // Guard shouldn't deaggro if other player touches camera
                else if (guard.isCameraAlerted()) {
                    currState = GuardState.AlERTED;
                    guard.startDeAggroTimer();
                    guard.setMaxSusLevel();
                }
                // Otherwise, stay in PATROL state
                break;
            case SUSPICIOUS:
                // Suspicion level is below threshold; SUSPICIOUS -> PATROL
                if (!guard.isSus()) {
                    currState = GuardState.PATROL;
                }
                // Max suspicion level reached; SUSPICIOUS -> CHASE
                else if (guard.isMaxSusLevel()) {
                    currState = GuardState.CHASE;
                    guard.startDeAggroTimer();
                }
                // Guard is not max sus level and is meowed; SUSPICIOUS -> DISTRACTED
                // Due to ordering of checks, this will only happen if the guard is suspicious
                // TODO: Ask if we need this transition
                else if (tempDistract) {
                    currState = GuardState.DISTRACTED;
                    guard.setMeow(true);
                }
                // Guard is not sus, not meowed, but player under camera; SUSPICIOUS -> ALERTED
                // Due to ordering of checks, this will only happen if the guard is suspicious
                // TODO: Ask if we need this transition
                else if (guard.isCameraAlerted()) {
                    currState = GuardState.AlERTED;
                    guard.startDeAggroTimer();
                    guard.setMaxSusLevel();
                }
                // Stay in SUSPICIOUS state -> Move towards player
                else {
                    // TODO: Implement logic to move towards player (Maybe reuse setNextTargetLocation?)
                }
                break;
            case CHASE:
                // If player deaggros the guard; CHASE -> PATROL
                // This happens if the guard is not in line of sight and the deAggroTimer is 0
                if (guard.checkDeAggroed()) {
                    currState = GuardState.PATROL;
                    // If guard was previously alerted by a camera
                    guard.setCameraAlerted(false);
                }
                break;
//            case RETURN:
//                // If guard reaches its target, change state to patrol
//                if (hasReachedPatrolPath()) {
//                    currState = GuardState.PATROL;
//                } else if (guard.isMaxSusLevel() || guard.isCameraAlerted()) {
//                    currState = GuardState.CHASE;
//                    guard.startDeAggroTimer();
//                }
//                if (guard.isCameraAlerted()) {
//                    guard.setMaxSusLevel();
//                }
//                break;
            case DISTRACTED:
                // If guard has reached meow location; DISTRACTED -> PATROL
                if (guard.getPosition().dst(distractPosition) <= WAYPOINT_RADIUS) {
                    currState = GuardState.PATROL;
                    guard.setMeow(false);
                }
                // Guard has not reached meow location, sus level is above threshold; DISTRACTED -> SUSPICIOUS
                else if (guard.isSus()) {
                    currState = GuardState.SUSPICIOUS;
                }
                break;
            case AlERTED:
                // If guard has reached camera location; ALERTED -> PATROL
                if (guard.getPosition().dst(cameraAlertPosition) <= WAYPOINT_RADIUS) {
                    currState = GuardState.PATROL;
                    guard.setMeow(false);
                }
                // Guard has not reached camera location, sus level is above threshold; ALERTED -> SUSPICIOUS
                else if (guard.isSus()) {
                    currState = GuardState.SUSPICIOUS;
                }
                break;
            default: // Should not happen
                break;
        }


    }

    /**
     * Helper function to check if the player has been spotted by the guard.
     *
     * @return true if the guard is aggravated and at maximum suspicion level, false otherwise
     */
    private boolean checkPlayerIsSpotted() {
        // TODO: Replace with real guard.isAgroed() method
        return this.guard.isAgroed() && guard.isMaxSusLevel();
    }

    /**
     * Helper function that calculates the distance between the guard and a target position.
     *
     * @param target The position to calculate distance to
     * @return The distance from the guard to the target position
     */
    private float distanceFromGuard(Vector2 target) {
        return this.guard.getPosition().dst(target);
    }

    /**
     * Returns the current state of the guard's AI state machine.
     *
     * @return The current GuardState (PATROL, CHASE, RETURN, or DISTRACTED)
     */
    public GuardState getGuardState() {
        return currState;
    }

    /**
     * Helper function that determines the next waypoint location based on pathfinding.
     * Uses the game graph to find a path from the guard's current position to the target location.
     *
     * @param targetLocation The destination the guard is trying to reach
     * @return The next position the guard should move towards
     */
    private Vector2 getNextWaypointLocation(Vector2 targetLocation) {
        // TODO: use the getPath method of the PathFinder
//        List<Node> path = tileGraph.getPath(guard.getPosition().cpy(), targetLocation.cpy());
        List<Node> path = new ArrayList<>();

        if (path.isEmpty()) {
            if (currState == GuardState.CHASE) {
                return targetPlayer.getPosition().cpy();
            }
            return guard.getPosition().cpy();
        }

        int pathIdx = 0;
        Vector2 nextStep = path.get(pathIdx).getWorldPosition().cpy();
        final float MIN_STEP_DISTANCE = 0.5F;

        // Skip steps that are too close to the guard to prevent jittering
        while (nextStep.dst(guard.getPosition().cpy()) < MIN_STEP_DISTANCE && pathIdx < path.size() - 1) {
            pathIdx++;
            nextStep = path.get(pathIdx).getWorldPosition().cpy();
        }
        return nextStep;
    }


    /**
     * Helper function that updates the next target location based on the guard's current state.
     * Handles different targeting logic for patrol, chase, return, and distracted states.
     */
    private void setNextTargetLocation() {
        switch (currState) {
            case PATROL:
                // Check whether the guard has been distracted by a meow
                tempDistract = didDistractionOccur();
                if (tempDistract) {
                    // Set the distract position to the current position of the player (Gar)
                    distractPosition.set(getActivePlayer().getPosition());
                    nextTargetLocation = getNextWaypointLocation(distractPosition);
                }
                else if (guard.isMaxSusLevel() || guard.isCameraAlerted()) { // suspicion level above threshold
                    targetPlayer = guard.getAggroTarget();
                    nextTargetLocation = getNextWaypointLocation(targetPlayer.getPosition());
                }
                else { // continue to patrol
                    if (waypoints.length == 0) {
                        return;
                    }
                    // Set next target location current waypoint index
                    nextTargetLocation = getNextWaypointLocation(waypoints[currentWaypointIndex]);
                    if (distanceFromGuard(nextTargetLocation) <= WAYPOINT_RADIUS) {
                        // If guard reaches waypoint, move to next waypoint
                        currentWaypointIndex = (currentWaypointIndex + 1) % waypoints.length;
                        nextTargetLocation = getNextWaypointLocation(waypoints[currentWaypointIndex]);
                    }
                }
                break;
            case CHASE:
                this.nextTargetLocation = getNextWaypointLocation(targetPlayer.getPosition());
                break;
            case RETURN:
                // TODO: Make guard be distract-able in this stage

                // Return to the nearest waypoint on the patrol path
                if (waypoints.length == 0) {
                    return;
                }
                if (guard.isMaxSusLevel() || guard.isCameraAlerted()) { // suspicion level above threshold
                    targetPlayer = guard.getAggroTarget();
                    nextTargetLocation = getNextWaypointLocation(targetPlayer.getPosition());
                    return;
                }

                // Find nearest waypoint to return to
                Vector2 nearestWaypoint = findNearestWaypoint();
                this.nextTargetLocation = getNextWaypointLocation(nearestWaypoint);
                break;
            case DISTRACTED:
                Vector2 tmp = getNextWaypointLocation(distractPosition);
                if (guard.isMaxSusLevel() || guard.isCameraAlerted()) { // suspicion level above threshold
                    targetPlayer = guard.getAggroTarget();
                    nextTargetLocation = getNextWaypointLocation(targetPlayer.getPosition());
                    return;
                }

                this.nextTargetLocation = tmp;
                break;
            default:
                break;
        }
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
     * Calculates the direction vector the guard should move in.
     *
     * @return A normalized Vector2 representing the movement direction
     */
    public Vector2 getMovementDirection() {
        if (this.nextTargetLocation == null) {
            return Vector2.Zero;
        } else {
            return this.nextTargetLocation.cpy().sub(guard.getPosition()).nor();
        }
    }

    /**
     * Draws debug visualization of the pathfinding graph.
     *
     * @param batch The SpriteBatch to draw with
     * @param camera The camera to use for coordinate transformations
     * @param texture The texture to use for drawing nodes
     */
    public void drawGraphDebug(SpriteBatch batch , OrthographicCamera camera, Texture texture) {
        tileGraph.draw(batch, camera, 1.0f);
//        gameGraph.drawGraphDebug(batch, camera, nextTargetLocation, texture);
    }


    /**
     * Enum representing the possible states of the guard's AI state machine.
     */
    private static enum GuardState {
        /** Guard is patrolling without target (0 <= susLevel < 50) */
        PATROL,
        /** Guard is suspicious of player (50 <= susLevel < 100) */
        SUSPICIOUS,
        /** Guard is chasing target (susLevel = 100 & agroLevel > 0) */
        CHASE,
        /** Guard is distracted by meow*/
        DISTRACTED,
        /** Guard is alerted by camera*/
        AlERTED;
        private GuardState() {}
    }


}
