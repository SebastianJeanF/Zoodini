package walknroll.zoodini.utils.animation;

public enum AnimationState {
    IDLE_NORTH,
    IDLE_SOUTH,
    IDLE_LEFT,
    IDLE_RIGHT,
    IDLE_NORTH_BLIND,
    IDLE_SOUTH_BLIND,
    IDLE_LEFT_BLIND,
    IDLE_RIGHT_BLIND,
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
