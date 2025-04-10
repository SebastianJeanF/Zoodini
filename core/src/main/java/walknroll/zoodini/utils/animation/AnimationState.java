package walknroll.zoodini.utils.animation;

public enum AnimationState {
    IDLE,
    WALK,
    WALK_UP,
    WALK_DOWN,
    SUSPICION_METER;


    // Priority determines which animation plays when multiple states are active
    private final int priority;

    AnimationState() {
        // Default constructor assigns priorities in enum order
        this.priority = ordinal();
    }
}
