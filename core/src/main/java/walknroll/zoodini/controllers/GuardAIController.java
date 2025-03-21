package walknroll.zoodini.controllers;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;

import walknroll.zoodini.models.GameLevel;
import walknroll.zoodini.models.entities.Avatar;
import walknroll.zoodini.models.entities.Guard;
import walknroll.zoodini.utils.GameGraph;
import walknroll.zoodini.utils.GameGraph.Node;

import java.util.List;

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

    /** Delay in ticks for the guard to become suspicious */
    private final int SUS_DELAY;
    /** Cooldown in ticks for the guard to become neutral again */
    private int susTicks;

    /** Min distance from waypoint where the guard will recalculate to next waypoint*/
    private final float WAYPOINT_RADIUS = 1.0F;

    private final int DEAGRRO_PERIOD = 60;
    private final int ALERT_DEAGRRO_PERIOD = 300;
    private int deAggroTimer = DEAGRRO_PERIOD;


    // TODO: May or may not refactor to remove input controller as argument
    public GuardAIController(Guard guard, GameLevel level, GameGraph gameGraph, int susDelay) {
        this.guard = guard;
        this.level = level;
        this.currState = GuardState.PATROL;
        this.waypoints = guard.getPatrolPoints();
        this.currentWaypointIndex = 0;
        this.gameGraph = gameGraph;
        this.ticks = 0L;
        this.SUS_DELAY = susDelay;
        this.susTicks = 0;
        this.distractPosition = new Vector2(0, 0);
    }

    private Avatar getActivePlayer() {
        return level.getAvatar();
    }

    private boolean didDistractionOccur() {
        InputController input = InputController.getInstance();
        // // System.out.print(".");
        return input.didAbility() && getActivePlayer().getAvatarType() == Avatar.AvatarType.CAT;
    }


    private boolean hasReachedPatrolPath() {
        if (waypoints.length == 0) {
            return true;
        }
        // Check if guard is close enough to the nearest waypoint
        return distanceFromGuard(nextTargetLocation) <= WAYPOINT_RADIUS;
    }

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



    public void update() {
        ticks++;
        // Update suspicion level
        if (currState != GuardState.CHASE) { // Only update when not chasing
            if (this.guard.isAgroed()) { // In guard's line of sight
                susTicks = Math.max(susTicks + 3, SUS_DELAY); // Increase suspicion
            } else {
                susTicks = Math.max(susTicks - 1, 0); // Decrease suspicion
            }
        } else {
            if (!this.guard.isAgroed()) { // not in guard's line of sight
                deAggroTimer = Math.max(deAggroTimer - 1, 0); // decrease suspicion
            }
        }

        setNextTargetLocation();



        switch(this.currState) {
            case PATROL:
                // If player is spotted, change state to chase
//                if (checkPlayerIsSpotted()) {
                if (isMaxSuspicion() || guard.isCameraAlerted()) {
                    currState = GuardState.CHASE;
                    deAggroTimer = guard.isCameraAlerted()
                        ? ALERT_DEAGRRO_PERIOD
                        : DEAGRRO_PERIOD;

                }
                else if (tempDistract) {
                    currState = GuardState.DISTRACTED;
                    guard.setMeow(true);
                }
                break;
            case CHASE:
                // If guard reaches its target, change state to return
                if (checkDeAggroed()) {
                    currState = GuardState.RETURN;
                    guard.setCameraAlerted(false);
                }
                break;
            case RETURN:
                // If guard reaches its target, change state to patrol
                if (hasReachedPatrolPath()) {
                    currState = GuardState.PATROL;
                } else if (isMaxSuspicion() || guard.isCameraAlerted()) {
                    currState = GuardState.CHASE;
                    deAggroTimer = guard.isCameraAlerted()
                        ? ALERT_DEAGRRO_PERIOD
                        : DEAGRRO_PERIOD;
                }
                break;
            case DISTRACTED:
                if (isMaxSuspicion() || guard.isCameraAlerted()) {
                    currState = GuardState.CHASE;
                    deAggroTimer = guard.isCameraAlerted()
                        ? ALERT_DEAGRRO_PERIOD
                        : DEAGRRO_PERIOD;
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

    private boolean checkPlayerIsSpotted() {
        // TODO: Replace with real guard.isAgroed() method
        return this.guard.isAgroed() && isMaxSuspicion();
    }

    private boolean isMaxSuspicion() {

        return this.susTicks >= SUS_DELAY;
    }

    private boolean checkDeAggroed() {
        // System.out.println("Is Deagroed: " + distanceFromGuard(targetPlayer.getPosition()));
//        return distanceFromGuard(targetPlayer.getPosition()) >= PLAYER_DEAGRO_RADIUS;
        return deAggroTimer <= 0;
    }

    private float distanceFromGuard(Vector2 target) {
        return this.guard.getPosition().dst(target);
    }

    public GuardState getGuardState() {
        return currState;
    }

    private Vector2 getNextWaypointLocation(Vector2 targetLocation) {
        // System.out.println("Target Location: " + targetLocation.x + ", " + targetLocation.y);
//        // System.out.println("Target location")
        List<Node> path = gameGraph.getPath(guard.getPosition().cpy(), targetLocation.cpy());
//        // System.out.println("path " + path);

        if (path.isEmpty()) {
            if (currState == GuardState.CHASE) {
                return targetPlayer.getPosition().cpy();
            }
            return guard.getPosition().cpy();
        }

        int pathIdx = 0;
        Vector2 nextStep = path.get(pathIdx).getWorldPosition().cpy();
        final float MIN_STEP_DISTANCE = 1.0F;
        // System.out.println("First next step: " + nextStep.x + ", " + nextStep.y);

        // Skip steps that are too close to the guard to prevent jittering
        while (nextStep.dst(guard.getPosition().cpy()) < MIN_STEP_DISTANCE && pathIdx < path.size() - 1) {
            pathIdx++;
            nextStep = path.get(pathIdx).getWorldPosition().cpy();
        }
        // System.out.println("Next step: " + nextStep.x + ", " + nextStep.y);
        return nextStep;
    }


    private void setNextTargetLocation() {
        switch (currState) {
            case PATROL:
                tempDistract = didDistractionOccur();

                if (isMaxSuspicion() || guard.isCameraAlerted()) { // suspicion level above threshold
//                    targetPlayer = getActivePlayer();
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
                if (isMaxSuspicion() || guard.isCameraAlerted()) { // suspicion level above threshold
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

                if (isMaxSuspicion() || guard.isCameraAlerted()) { // suspicion level above threshold
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

    public Vector2 getNextTargetLocation() {
        return nextTargetLocation;
    }

    public Vector2 getMovementDirection() {
//        this.setNextTargetLocation();
        if (this.nextTargetLocation == null) {
            return Vector2.Zero;
        } else {
            return this.nextTargetLocation.cpy().sub(guard.getPosition()).nor();
        }
    }

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
