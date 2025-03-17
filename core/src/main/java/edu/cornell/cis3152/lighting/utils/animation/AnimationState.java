package edu.cornell.cis3152.lighting.utils.animation;

public enum AnimationState {
    IDLE,
    WALK;

    // Priority determines which animation plays when multiple states are active
    private final int priority;

    AnimationState() {
        // Default constructor assigns priorities in enum order
        this.priority = ordinal();
    }
}
