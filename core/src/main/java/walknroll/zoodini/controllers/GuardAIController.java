package walknroll.zoodini.controllers;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.ai.pfa.Heuristic;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;

import edu.cornell.gdiac.graphics.SpriteBatch;
import walknroll.zoodini.controllers.aitools.ManhattanHeuristic;
import walknroll.zoodini.controllers.aitools.TileGraph;
import walknroll.zoodini.controllers.aitools.TileNode;
import walknroll.zoodini.models.GameLevel;
import walknroll.zoodini.models.entities.Avatar;
import walknroll.zoodini.models.entities.Guard;
import walknroll.zoodini.models.entities.PlayableAvatar;
import walknroll.zoodini.utils.DebugPrinter;
import walknroll.zoodini.utils.enums.AvatarType;

/**
 * Controller class that manages the AI behavior for guard entities.
 * Handles guard patrolling, chasing, returning to patrol, and responding to
 * distractions.
 */
public class GuardAIController {
    /** Guard identifier for this AI controller */
    private final Guard guard;
    /** Target of the guard (to chase) */
    private PlayableAvatar targetPlayer;
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

    private long lastStateChangeTime = 0;

    private final long STATE_CHANGE_COOLDOWN = 10;

    private float CAT_MEOW_RADIUS;

    /** Graph representation of the game */
    private TileGraph tileGraph;

    private IndexedAStarPathFinder<TileNode> pathFinder;

    private Heuristic heuristic;

    private Vector2 nextTargetLocation;

    // Looking around behavior variables
    private float lookAroundDuration = 3.0f;  // How long guard looks around in seconds
    private float currentLookTime = 0.0f;     // Current time spent looking around
    private float lookDirection = 1.0f;       // 1.0 for right, -1.0 for left
    private float lookChangeTime = 1.0f;      // Time before changing look direction
    private float currentLookChangeTime = 0f; // Current time spent in current look direction


    /**
     * Constructs a new GuardAIController for a specific guard.
     *
     * @param guard     The guard entity that this controller will manage
     * @param level     The game level containing relevant game state information
     * @param tileGraph The graph representation of the level for pathfinding
     */
    public GuardAIController(Guard guard, GameLevel level, TileGraph<TileNode> tileGraph) {
        this.guard = guard;
        this.level = level;
        this.currState = GuardState.PATROL;
        this.waypoints = guard.getPatrolPoints();
        this.currentWaypointIndex = 0;
        this.tileGraph = tileGraph;
        this.ticks = 0L;
        this.distractPosition = new Vector2(0, 0);
        this.cameraAlertPosition = new Vector2(0, 0);
        this.pathFinder = new IndexedAStarPathFinder<>(tileGraph);
        this.heuristic = new ManhattanHeuristic<>();
        this.nextTargetLocation = new Vector2(0, 0);
        this.CAT_MEOW_RADIUS = level.isCatPresent() ? level.getCat().getAbilityRange() : 0;

        // Otherwise, stay in PATROL state
        if (waypoints.length <= 1) {
            guard.setIdle(true);
        }
    }

    /**
     * Validates waypoints and updates any that are in wall tiles to be in valid
     * non-wall tiles.
     *
     * @param waypoints The array of waypoints to validate
     * @return A new array of valid waypoints in world coordinates
     */
    private Vector2[] getValidWaypoints(Vector2[] waypoints) {
        if (waypoints == null || waypoints.length == 0) {
            return new Vector2[0];
        }

        Vector2[] validWaypoints = new Vector2[waypoints.length];

        for (int i = 0; i < waypoints.length; i++) {
            Vector2 waypoint = waypoints[i];
            TileNode waypointTile = tileGraph.worldToTile(waypoint);

            // Check if waypoint is in a wall tile
            if (waypointTile == null || waypointTile.isObstacle) {
                // Find the nearest non-wall tile
                TileNode validTile = tileGraph.findNearestNonObstacleNode(waypoint);

                if (validTile != null) {
                    // Convert the valid tile to world coordinates (use the center of the tile)
                    validWaypoints[i] = tileGraph.tileToWorld(validTile);
//                    DebugPrinter.println("Updated waypoint " + i + " from " + waypoint +
//                            " to " + validWaypoints[i] + " (was in wall)");
                } else {
                    // This should not happen if your graph has at least one non-wall tile
//                    DebugPrinter.println("Warning: Could not find a valid non-wall tile for waypoint " + i);
                    validWaypoints[i] = waypoint; // Keep the original as fallback
                }
            } else {
                // Waypoint is already valid, so keep it
                validWaypoints[i] = waypoint;
            }
        }

        return validWaypoints;
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
     * Helper function that checks if a distraction from the cat's ability has
     * occurred.
     *
     * @return true if the cat player has used its distraction ability, false
     *         otherwise
     */
    private boolean didDistractionOccur() {
        float guardToPlayerDistance = guard.getPosition().dst(getActivePlayer().getPosition());
        return (getActivePlayer().getAvatarType() == AvatarType.CAT &&
                level.getCat().didJustMeow() &&
                guardToPlayerDistance <= CAT_MEOW_RADIUS);
    }

    public Vector2 getCameraAlertPosition() {
        return cameraAlertPosition;
    }

    public Vector2 getDistractPosition() {
        return distractPosition;
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

    /***
     * Helper function to check if the guard has reached a target location.
     * The guard "reaches" a target if it is within the radius of the arrival
     * distance in world coords.
     *
     * @param target The target position to check against
     * @return true if the guard has reached the target location, false otherwise
     */
    private boolean hasReachedTargetLocation(Vector2 target) {
        // Use world coordinates and a reasonable threshold
        float arrivalDistance = 1f;
        float distance = guard.getPosition().dst(target);

        // Optional debugging
        // if (distance < 1.0f) {
        // DebugPrinter.println("Distance to target: " + distance);
        // }

        return distance < arrivalDistance;
        // if (!tileGraph.isValidTile(target)) {
        // target = tileGraph.getNearestValidTile(target).getCoords();
        // }
        // Vector2 guardTile = tileGraph.worldToTile(guard.getPosition()).getCoords();
        // Vector2 targetTile = tileGraph.worldToTile(target).getCoords();
        // DebugPrinter.println("Current guard tile " + guardTile);
        // DebugPrinter.println("Current target tile " + targetTile);
        // DebugPrinter.println(tileGraph.worldToTile(guard.getPosition()).isWall);
        // DebugPrinter.println(tileGraph.worldToTile(target).isWall);
        // return guardTile.x == targetTile.x && guardTile.y == targetTile.y;
    }

    /**
     * Helper function to update the guard's suspicion level based on its current
     * state.
     * Increases suspicion if the guard is in line of sight of the player, otherwise
     * decreases it.
     * Also handles de-aggro timer when the guard is not in line of sight and not
     * under camera.
     */
    private void updateSusLevel() {
        if (currState != GuardState.CHASE) { // Only update when not chasing
            if (guard.isSeesPlayer() && guard.getSeenPlayer() != null) { // In guard's line of sight

                // If guard is alerted by a camera, increase suspicion to max
                // otherwise, calculate the increase based on distance to player
                int susIncrease = currState == GuardState.AlERTED
                        ? (int) guard.getMaxSusLevel()
                        : guard.calculateSusIncrease(guard.getSeenPlayer().getPosition());

                guard.deltaSusLevel(susIncrease); // Increase suspicion
            } else {
                // Only decrease suspicion if not in ALERTED state
                if (currState != GuardState.AlERTED) {
                    guard.deltaSusLevel(-0.75f); // Decrease suspicion
                }
            }
        } else { // Guard is chasing
            // When player is under camera and guard is in CHASE state
            if (targetPlayer != null && targetPlayer.isUnderCamera()) {
                // Don't change deaggroTimer if under camera
                guard.deltaDeAggroTimer(0);
            }
            // Not under camera and not deaggroed, normal de-aggro logic
            else if (!guard.isSeesPlayer()) {
                guard.deltaDeAggroTimer(-1); // Normal decrease
            } else {
                // Guard sees player directly, reset de-aggro timer
                guard.deltaDeAggroTimer(2);
            }
        }
    }

    public boolean canStateTransition() {
        return ticks - lastStateChangeTime > STATE_CHANGE_COOLDOWN;
    }

    /**
     * Updates the guard's state based on current conditions and state priority.
     */
    private void updateGuardState() {
        // First check for max suspicion level, which always leads to CHASE (highest
        // priority)
        if (guard.isMaxSusLevel() && currState != GuardState.CHASE) {
            currState = GuardState.CHASE;
            guard.startDeAggroTimer();
            return;
        }

        guard.setLookingAround(currState == GuardState.LOOKING_AROUND);

        switch (currState) {
            case CHASE:
                // If player deaggros the guard; CHASE -> PATROL
                // This happens if the guard is not in line of sight and the deAggroTimer is 0
                if (guard.checkDeAggroed() || targetPlayer.isInvincible()) {
                    currState = GuardState.SUSPICIOUS;
                    // If guard was previously alerted by a camera
                    guard.setCameraAlerted(false);
                    lastStateChangeTime = ticks;
                }
                // Stay in CHASE state -> Chase player (Handled in setNextTargetLocation)
                break;
            case SUSPICIOUS:
                // Suspicion level is below threshold; SUSPICIOUS -> PATROL
                // TODO: Experimenting guard leaving sus state only when sus level is 0
                if (!guard.isSus()) {
                    // if (guard.getSusLevel() == 0) {

                    currState = GuardState.PATROL;
                    lastStateChangeTime = ticks;
                }
                // Player under camera; SUSPICIOUS -> ALERTED
                else if (guard.isCameraAlerted()) {
                    currState = GuardState.AlERTED;
                    guard.startDeAggroTimer();
                    guard.setMaxSusLevel();
                    cameraAlertPosition.set(getActivePlayer().getPosition());
                    lastStateChangeTime = ticks;
                }
                break;
            case AlERTED:
                // If guard has reached camera location; ALERTED -> PATROL
                if (hasReachedTargetLocation(cameraAlertPosition)) {
                    currState = GuardState.PATROL;
                    guard.setCameraAlerted(false);
                    lastStateChangeTime = ticks;
                }
                // Guard has not reached camera location, sus level is above threshold; ALERTED
                // -> SUSPICIOUS
                else if (guard.isSus()) {
                    currState = GuardState.SUSPICIOUS;
                    guard.setCameraAlerted(true); // TODO: Make this false (if we want guard to lose momentum after
                                                  // spotting)
                    lastStateChangeTime = ticks;
                }
                break;
            case DISTRACTED:
                // If guard has reached meow location; DISTRACTED -> PATROL
//                if (hasReachedTargetLocation(distractPosition)) {
//                    currState = GuardState.PATROL;
//                    guard.setMeow(false);
//                    lastStateChangeTime = ticks;
//                }
                // If guard has reached meow location; DISTRACTED -> LOOKING_AROUND
                if (hasReachedTargetLocation(distractPosition)) {
                    currState = GuardState.LOOKING_AROUND;
                    // Initialize the looking around timer
                    currentLookTime = 0.0f;
                    currentLookChangeTime = 0f;
                    lookDirection = 1.0f;
                    guard.setMeow(false);
                    lastStateChangeTime = ticks;
                }
                // Guard has not reached meow location, sus level is above threshold; DISTRACTED
                // -> SUSPICIOUS
                else if (guard.isSus()) {
                    currState = GuardState.SUSPICIOUS;
                    guard.setMeow(false);
                    lastStateChangeTime = ticks;
                } else if (guard.isCameraAlerted()) {
                    currState = GuardState.AlERTED;
                    guard.setCameraAlerted(true);
                    guard.setMeow(false);
                    Vector2 playerPosition = getActivePlayer().getPosition();
                    cameraAlertPosition.set(getValidTileCoords(playerPosition));
                    lastStateChangeTime = ticks;
                }
                // Gar meows again -> should update distractPosition
                else if (didDistractionOccur()) {
                    DebugPrinter.println("here");
                    currState = GuardState.DISTRACTED;
                    guard.setMeow(true);
                    Vector2 playerPosition = getActivePlayer().getPosition();
                    distractPosition.set(getValidTileCoords(playerPosition));
                    lastStateChangeTime = ticks;
                }
                break;
            case LOOKING_AROUND:
                // Check for higher priority states first (same as in DISTRACTED)
                if (guard.isSus()) {
                    currState = GuardState.SUSPICIOUS;
                    guard.setMeow(false);
                    lastStateChangeTime = ticks;
                }
                else if (guard.isCameraAlerted()) {
                    currState = GuardState.AlERTED;
                    guard.setCameraAlerted(true);
                    guard.setMeow(false);
                    Vector2 playerPosition = getActivePlayer().getPosition();
                    cameraAlertPosition.set(getValidTileCoords(playerPosition));
                    lastStateChangeTime = ticks;
                }
                else if (didDistractionOccur()) {
                    // Another meow can interrupt looking around
                    currState = GuardState.DISTRACTED;
                    guard.setMeow(true);
                    Vector2 playerPosition = getActivePlayer().getPosition();
                    distractPosition.set(getValidTileCoords(playerPosition));
                    lastStateChangeTime = ticks;
                }
                // After looking around time is up, go back to PATROL state
                else if (currentLookTime >= lookAroundDuration) {
                    currState = GuardState.PATROL;
                    guard.setMeow(false);
                    lastStateChangeTime = ticks;
                }
                break;
            case PATROL:
                // Guard is not max sus level but is suspicious; PATROL -> SUSPICIOUS
                if (guard.isSus()) {
                    currState = GuardState.SUSPICIOUS;
                    lastStateChangeTime = ticks;
                }
                // Guard is not sus and is meowed; PATROL -> DISTRACTED
                // Due to ordering of checks, this will only happen if the guard is not
                // suspicious
                // This makes sense since we don't want the guard to deagrro by being meowed
                else if (didDistractionOccur()) {
                    currState = GuardState.DISTRACTED;
                    if (!guard.isMeowed()) {
                        guard.setSusLevel(guard.getSusThreshold() - 1);
                    }
                    guard.setMeow(true);
                    Vector2 playerPosition = getActivePlayer().getPosition();
                    distractPosition.set(getValidTileCoords(playerPosition));
                    lastStateChangeTime = ticks;
                }
                // Guard is not sus, not meowed, but player under camera; PATROL -> ALERTED
                // Due to ordering of checks, this will only happen if the guard is not
                // suspicious
                // Guard shouldn't deaggro if other player touches camera
                else if (guard.isCameraAlerted()) {
                    currState = GuardState.AlERTED;
                    guard.setCameraAlerted(true);
                    Vector2 playerPosition = getActivePlayer().getPosition();
                    cameraAlertPosition.set(getValidTileCoords(playerPosition));
                    lastStateChangeTime = ticks;
                }

                guard.setIdle(waypoints.length <= 1 && guard.getPosition().dst(waypoints[0]) <= 0.5f);

                break;
            default:
                // Should never happen, but reset to PATROL if we get an invalid state
                currState = GuardState.PATROL;
                break;
        }
        if (currState != GuardState.PATROL || guard.getPosition().dst(waypoints[0]) > 0.5f) {
            guard.setIdle(false);
        }

    }

    private void executeLookAround(float dt) {
        // Update looking around behavior if in LOOKING_AROUND state
        if (currState == GuardState.LOOKING_AROUND) {
            currentLookTime += dt;
            currentLookChangeTime += dt;
            // Change look direction periodically
            if (currentLookChangeTime >= lookChangeTime) {
                lookDirection *= -1; // Flip direction
                currentLookChangeTime = 0;

                // Update the guard's direction for looking left and right
                Vector2 lookDirectionVector = new Vector2(lookDirection, 0);
                guard.updateOrientation(dt, lookDirectionVector);
            }
        }
    }

    /**
     * Updates the guard's AI state and behavior.
     * This is the main function that should be called each frame to progress the
     * guard's AI.
     * Handles suspicion level changes, state transitions, and movement target
     * updates.
     */
    public void update(float dt) {
        ticks++;
        executeLookAround(dt);
        updateSusLevel();
        updateGuardState();
        setNextTargetLocation();

    }

    /**
     * Helper function to check if the player has been spotted by the guard.
     *
     * @return true if the guard is aggravated and at maximum suspicion level, false
     *         otherwise
     */
    private boolean checkPlayerIsSpotted() {
        // TODO: Replace with real guard.isAgroed() method
        return this.guard.isAgroed() && guard.isMaxSusLevel();
    }

    /**
     * Helper function that calculates the distance between the guard and a target
     * position.
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
     * Helper function that checks if the target position is not a wall.
     * If the target position is a wall, it returns the world coords of the nearest
     * non-wall tile.
     * If the target position is not a wall, it returns the original target
     * position.
     *
     * @param target The target position to check
     * @return A valid Vector2 position that is not a wall
     */
    public Vector2 getValidTileCoords(Vector2 target) {
        TileNode targetTile = tileGraph.worldToTile(target);
        if (!targetTile.isObstacle) {
            return target;
        } else {
//            DebugPrinter.println("Target tile is a wall: " + targetTile.getCoords());
            // If the target tile is a wall, find the nearest non-wall tile
            TileNode newTile = tileGraph.getNearestValidTile(target);
            return tileGraph.tileToWorld(newTile);
        }
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
    private Vector2 getNextWaypointLocation(Vector2 targetLocation) {
        List<TileNode> path = getPath(guard.getPosition().cpy(), targetLocation.cpy());
        if (path.isEmpty()) {
            if (currState == GuardState.CHASE) {
                return targetPlayer.getPosition().cpy();
            }
            return guard.getPosition().cpy();
        }

        int pathIdx = 0;
        Vector2 nextStep = tileGraph.tileToWorld(path.get(pathIdx));
        final float MIN_STEP_DISTANCE = 0.5F;

        // Skip steps that are too close to the guard to prevent jittering
        while (nextStep.dst(guard.getPosition().cpy()) < MIN_STEP_DISTANCE && pathIdx < path.size() - 1) {
            pathIdx++;
            nextStep = tileGraph.tileToWorld(path.get(pathIdx));
        }
        return nextStep;
    }

    /**
     * Helper function that updates the next target location based on the guard's
     * current state.
     * Handles different targeting logic for patrol, chase, return, and distracted
     * states.
     */
    private void setNextTargetLocation() {
        Vector2 newTarget = null;

        switch (currState) {
            case PATROL:
                if (waypoints.length == 0) {
                    return;
                }
                if (guard.isIdle()) {
                    return;
                }
                // If guard reaches waypoint, move to next waypoint
                // This target can be either the next waypoint on the patrol path or the nearest
                // waypoint to return to after distracted or alerted.
                if (hasReachedTargetLocation(waypoints[currentWaypointIndex])) {
                    currentWaypointIndex = (currentWaypointIndex + 1) % waypoints.length;
                }
                // Guard hasn't reached waypoint, so continue to current target
                newTarget = getNextWaypointLocation(waypoints[currentWaypointIndex]);
                break;
            case SUSPICIOUS:
                // If guard is sus but not max sus level, slowly move towards player
                // TODO: in order to move more slowly towards player update vector magnitude in
                // moveGuard function in GameScene
                targetPlayer = guard.getAggroTarget();
                if (targetPlayer != null) {
                    newTarget = getNextWaypointLocation(targetPlayer.getPosition());
                } else {
                    // Fall back to patrol behavior or some default position
                    newTarget = waypoints.length > 0 ? getNextWaypointLocation(waypoints[currentWaypointIndex])
                            : guard.getPosition();
                }
                break;
            case CHASE:
                targetPlayer = guard.getAggroTarget();
                if (targetPlayer != null) {
                    newTarget = getNextWaypointLocation(targetPlayer.getPosition());
                } else {
                    // If no target, maybe return to patrol
                    newTarget = waypoints.length > 0 ? getNextWaypointLocation(waypoints[currentWaypointIndex])
                            : guard.getPosition();
                }
                break;
            case DISTRACTED:
                if (distractPosition == null) { // should not happen if FSM is correct
                    return;
                }
                newTarget = getNextWaypointLocation(distractPosition);
                break;
            case AlERTED:
                if (cameraAlertPosition == null) { // should not happen if FSM is correct
                    return;
                }
                newTarget = getNextWaypointLocation(cameraAlertPosition);
                break;
            case LOOKING_AROUND:
                // Don't move, stay in place while looking around
                newTarget = distractPosition;
                break;

            default:
                break;
        }

        nextTargetLocation = getValidTileCoords(newTarget);
        // nextTargetLocation = newTarget;

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
        if (currState == GuardState.LOOKING_AROUND) {
            return new Vector2(lookDirection, 0);
        }
        else if (this.nextTargetLocation == null) {
            return Vector2.Zero;
        } else {
            return this.nextTargetLocation.cpy().sub(guard.getPosition()).nor();
        }
    }

    /**
     * Draws debug visualization of the pathfinding graph.
     *
     * @param batch   The SpriteBatch to draw with
     * @param camera  The camera to use for coordinate transformations
     * @param texture The texture to use for drawing nodes
     */
    public void drawGraphDebug(SpriteBatch batch, OrthographicCamera camera, Texture texture) {
        tileGraph.draw(batch, camera, 1.0f);
        // gameGraph.drawGraphDebug(batch, camera, nextTargetLocation, texture);
    }

    /**
     * Finds the shortest path between two positions in the world using A*.
     *
     * @INVARIANT this.heuristic must be initialized
     * @param currPosWorld   The starting position in world coordinates
     * @param targetPosWorld The target position in world coordinates
     * @return A list of nodes representing the path from start to target, excluding
     *         the start node
     */
    public List<TileNode> getPath(Vector2 currPosWorld, Vector2 targetPosWorld) {
        GraphPath<TileNode> graphPath = new DefaultGraphPath<>();
        TileNode start = tileGraph.worldToTile(currPosWorld);
        TileNode end = tileGraph.worldToTile(targetPosWorld);

        // DebugPrinter.println("Current guard Position: " + currPosWorld);
        // DebugPrinter.println("Graph's target: "+ end.getWorldPosition());
        // Check if start or end node is null
        if (start == null || end == null) {
            // System.err.println("Error: Start or end node is null.");
            return new ArrayList<>();
        }

        if (start.isObstacle) {
            start = tileGraph.findNearestNonObstacleNode(currPosWorld);
        }

        if (end.isObstacle) {
            end = tileGraph.findNearestNonObstacleNode(targetPosWorld);
        }

        pathFinder.searchNodePath(start, end, heuristic, graphPath);

        // Only add nodes to the path if they are not the start node
        List<TileNode> path = new ArrayList<>();
        for (TileNode node : graphPath) {
            if (!node.equals(start)) {
                path.add(node);
            }
        }
        return path;
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
        /** Guard is distracted by meow */
        DISTRACTED,
        /** Guard is alerted by camera*/
        AlERTED,
        /** Guard is looking around after reaching meow location */
        LOOKING_AROUND;
        private GuardState() {}
    }

}
