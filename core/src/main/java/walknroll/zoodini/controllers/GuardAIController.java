package walknroll.zoodini.controllers;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;


import edu.cornell.gdiac.graphics.SpriteBatch;
import walknroll.zoodini.models.GameLevel;
import walknroll.zoodini.models.entities.Avatar;
import walknroll.zoodini.models.entities.Guard;
import walknroll.zoodini.utils.GameGraph;
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
    /** Target of the guard (to follow meow) */
    private Vector2 distractPosition;
    /** Current state of the finite state machine */
    private GuardState currState;
    /** Waypoints for the guard to patrol */
    private Vector2[] waypoints;
    /** Current waypoint index */
    private int currentWaypointIndex;

    private long ticks;
    /** Graph representation of the game */
    private GameGraph gameGraph;


    private Vector2 nextTargetLocation;


    /** Min distance from waypoint where the guard will recalculate to next waypoint*/
    private final float WAYPOINT_RADIUS = 0.25F;



    /**
     * Constructs a new GuardAIController for a specific guard.
     *
     * @param guard The guard entity that this controller will manage
     * @param level The game level containing relevant game state information
     * @param gameGraph The graph representation of the level for pathfinding
     */
    public GuardAIController(Guard guard, GameLevel level, GameGraph gameGraph) {
        this.guard = guard;
        this.level = level;
        this.currState = GuardState.PATROL;
        this.waypoints = guard.getPatrolPoints();
        this.currentWaypointIndex = 0;
        this.gameGraph = gameGraph;
        this.ticks = 0L;
        this.distractPosition = new Vector2(0, 0);
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
                // If player is spotted, change state to chase
//                if (checkPlayerIsSpotted()) {
                if (guard.isMaxSusLevel() || guard.isCameraAlerted()) {
                    currState = GuardState.CHASE;
                    guard.startDeAggroTimer();
                    if (guard.isCameraAlerted()) {
                        guard.setMaxSusLevel();
                    }

                }
                else if (tempDistract) {
                    currState = GuardState.DISTRACTED;
                    guard.setMeow(true);
                }
                break;
            case CHASE:
                // If guard reaches its target, change state to return
                if (guard.checkDeAggroed()) {
                    currState = GuardState.RETURN;
                    guard.setCameraAlerted(false);
                }
                break;
            case RETURN:
                // If guard reaches its target, change state to patrol
                if (hasReachedPatrolPath()) {
                    currState = GuardState.PATROL;
                } else if (guard.isMaxSusLevel() || guard.isCameraAlerted()) {
                    currState = GuardState.CHASE;
                    guard.startDeAggroTimer();
                }
                if (guard.isCameraAlerted()) {
                    guard.setMaxSusLevel();
                }
                break;
            case DISTRACTED:
                if (guard.isMaxSusLevel() || guard.isCameraAlerted()) {
                    currState = GuardState.CHASE;
                    guard.startDeAggroTimer();
                    guard.setMeow(false);
                }
                else if (guard.getPosition().dst(distractPosition) <= WAYPOINT_RADIUS) {
                    currState = GuardState.PATROL;
                    guard.setMeow(false);
                }

                break;
            default: // Should not happen
                break;
        }


    }

    private boolean tempDistract;

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
        List<Node> path = gameGraph.getPath(guard.getPosition().cpy(), targetLocation.cpy());

        if (path.isEmpty()) {
            if (currState == GuardState.CHASE) {
                return targetPlayer.getPosition().cpy();
            }
            return guard.getPosition().cpy();
        }

        int pathIdx = 0;
        Vector2 nextStep = path.get(pathIdx).getWorldPosition().cpy();
        final float MIN_STEP_DISTANCE = 0.5F;
        // System.out.println("First next step: " + nextStep.x + ", " + nextStep.y);

        // Skip steps that are too close to the guard to prevent jittering
        while (nextStep.dst(guard.getPosition().cpy()) < MIN_STEP_DISTANCE && pathIdx < path.size() - 1) {
            pathIdx++;
            nextStep = path.get(pathIdx).getWorldPosition().cpy();
        }
        // System.out.println("Next step: " + nextStep.x + ", " + nextStep.y);
        return nextStep;
    }


    /**
     * Helper function that updates the next target location based on the guard's current state.
     * Handles different targeting logic for patrol, chase, return, and distracted states.
     */
    private void setNextTargetLocation() {
        switch (currState) {
            case PATROL:
                tempDistract = didDistractionOccur();

                if (guard.isMaxSusLevel() || guard.isCameraAlerted()) { // suspicion level above threshold
                    targetPlayer = guard.getAggroTarget();
                    nextTargetLocation = getNextWaypointLocation(targetPlayer.getPosition());
                }
                else if (tempDistract) { // distraction occurred
                    distractPosition.set(getActivePlayer().getPosition());
                    // System.out.println("Distract position: " + distractPosition);
                    nextTargetLocation = getNextWaypointLocation(distractPosition);
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
//                    targetPlayer = getActivePlayer();
                    targetPlayer = guard.getAggroTarget();
                    nextTargetLocation = getNextWaypointLocation(targetPlayer.getPosition());
                    return;
                }

                // Find nearest waypoint to return to
                Vector2 nearestWaypoint = findNearestWaypoint();
                this.nextTargetLocation = getNextWaypointLocation(nearestWaypoint);
                break;
            case DISTRACTED:
//                // System.out.print("distraction position: " + distractPosition);
                Vector2 tmp = getNextWaypointLocation(distractPosition);
//                // System.out.print("Next waypoint: " + tmp);

                if (guard.isMaxSusLevel() || guard.isCameraAlerted()) { // suspicion level above threshold
//                    targetPlayer = getActivePlayer();
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
//        this.setNextTargetLocation();
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
        gameGraph.drawGraphDebug(batch, camera, nextTargetLocation, texture);
    }


    /**
     * Enum representing the possible states of the guard's AI state machine.
     */
    private static enum GuardState {
        /** Guard is patrolling without target*/
        PATROL,
        /** Guard is chasing target*/
        CHASE,
        /** Guard is returning to patrol (after being distracted or player
         * goes out of range)*/
        RETURN,
        /** Guard is distracted by meow*/
        DISTRACTED;

        private GuardState() {}
    }


}
