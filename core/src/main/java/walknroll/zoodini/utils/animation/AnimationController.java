package walknroll.zoodini.utils.animation;

import java.util.HashMap;
import java.util.Map;
import edu.cornell.gdiac.graphics.SpriteSheet;

public class AnimationController {
    private final Map<AnimationState, Animation> animations = new HashMap<>();
    private AnimationState currentState;

    public AnimationController(AnimationState defaultState) {
        this.currentState = defaultState;
    }

    public void addAnimation(AnimationState state, Animation animation) {
        animations.put(state, animation);
    }

    public void setState(AnimationState state) {
        // Only change state if the animation exists and it's different
        if (state != currentState && animations.containsKey(state)) {
            currentState = state;
            if (animations.get(state) != null) {
                animations.get(state).reset();
            }
        }
    }
    public AnimationState getCurrentState() {
        return currentState;
    }

    public void update() {
        Animation current = animations.get(currentState);
        if (current != null) {
            current.update();
        }
    }

    public SpriteSheet getCurrentSpriteSheet() {
        Animation current = animations.get(currentState);
        return current != null ? current.getSpriteSheet() : null;
    }

    public int getCurrentFrame() {
        Animation current = animations.get(currentState);
        return current != null ? current.getCurrentFrame() : 0;
    }
}
