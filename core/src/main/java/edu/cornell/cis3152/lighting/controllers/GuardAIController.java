package edu.cornell.cis3152.lighting.controllers;


import com.badlogic.gdx.math.Vector2;
import edu.cornell.cis3152.lighting.models.Avatar;
import edu.cornell.cis3152.lighting.models.Guard;
import edu.cornell.cis3152.lighting.utils.GameGraph;
import edu.cornell.cis3152.lighting.utils.GameGraph.Node;
import java.util.List;

public class GuardAIController {
    /** Guard identifier for this AI controller */
    private final Guard guard;
    /** Target of the guard (to chase) */
    private Avatar player;
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

    /** Min distance from target where the guard will de-agro */
    private final float PLAYER_DEAGRO_RADIUS = 10.0F;
    /** Min distance from waypoint where the guard will recalculate to next waypoint*/
    private final float WAYPOINT_RADIUS = 1.0F;






    public GuardAIController(Guard guard, Avatar player, GameGraph gameGraph, int susDelay) {
        this.guard = guard;
        this.player = player;
        this.currState = GuardState.PATROL;
        this.waypoints = guard.getPatrolPoints();
        this.currentWaypointIndex = 0;
        this.gameGraph = gameGraph;
        this.ticks = 0L;
        this.SUS_DELAY = susDelay;
        this.susTicks = 0;

    }



    private void changeStateIfApplicable() {
        ticks++;
        // Update suspicion level
        if (currState != GuardState.CHASE) { // Only update when not chasing
            if (this.guard.isAgroed()) { // In guard's line of sight
                susTicks = Math.max(susTicks + 1, SUS_DELAY); // Increase suspicion
            } else {
                susTicks = Math.min(susTicks - 1, 0); // Decrease suspicion
            }
        }

        setNextTargetLocation();

        switch(this.currState) {
            case PATROL:
                // If player is spotted, change state to chase
                if (checkPlayerIsSpotted()) {
                    currState = GuardState.CHASE;
                }
            case CHASE:
                // If guard reaches its target, change state to return
                if (checkDeAgroed()) {
                    currState = GuardState.RETURN;
                }
            case RETURN:
                // If guard spots player while returning, change state to chase
                if (checkPlayerIsSpotted()) {
                    currState = GuardState.CHASE;
                }
            case DISTRACTED:
                // TODO: Implement when meow is implemented
                break;
            default: // Should not happen
                break;
        }
    }

    private boolean checkPlayerIsSpotted() {
        // TODO: Replace with real guard.isAgroed() method
        return this.guard.isAgroed() && isMaxSuspicion();
    }

    private boolean isMaxSuspicion() {
        return this.susTicks >= SUS_DELAY;
    }

    private boolean checkDeAgroed() {
        return distanceFromGuard(player.getPosition()) <= PLAYER_DEAGRO_RADIUS;
    }

    private float distanceFromGuard(Vector2 target) {
        return this.guard.getPosition().dst(target);
    }

    public GuardState getGuardState() {
        return currState;
    }

    private Vector2 getNextWaypointLocation(Vector2 targetLocation) {
        List<Node> path = gameGraph.getPath(guard.getPosition().cpy(), targetLocation.cpy());
        if (path.isEmpty()) {
            if (currState == GuardState.CHASE) {
                return player.getPosition().cpy();
            }
            return guard.getPosition().cpy();
        } else {
            int pathIdx = 0;
            Vector2 nextStep = path.get(pathIdx).getWorldPosition().cpy();

            // Skip steps that are too close to the guard to prevent jittering
            while (nextStep.dst(guard.getPosition().cpy()) <= 5.0F && pathIdx < path.size()) {
                pathIdx++;
                nextStep = path.get(pathIdx).getWorldPosition().cpy();
            }
            return nextStep;
        }

    }

    private void setNextTargetLocation() {
        switch (currState) {
            case PATROL:
                if (isMaxSuspicion()) { // suspicion level above threshold
                    nextTargetLocation = player.getPosition();
                } else { // continue to patrol
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
                this.nextTargetLocation = getNextWaypointLocation(player.getPosition());
            case RETURN:
                break;
            case DISTRACTED:
                break;
            default:
                break;

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
