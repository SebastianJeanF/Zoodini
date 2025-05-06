package walknroll.zoodini.utils.animation;

public enum AnimationState {
    IDLE,
    IDLE_BLIND,
    WALK,
    WALK_BLIND,
    WALK_UP,
    WALK_UP_BLIND,
    WALK_DOWN,
    WALK_DOWN_BLIND,
    SUSPICION_METER,
    BLIND,
    EXPLODE;

    // Priority determines which animation plays when multiple states are active
    private final int priority;

    AnimationState() {
        // Default constructor assigns priorities in enum order
        this.priority = ordinal();
    }
}
