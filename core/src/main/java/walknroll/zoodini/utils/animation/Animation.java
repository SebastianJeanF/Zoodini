package walknroll.zoodini.utils.animation;

import edu.cornell.gdiac.graphics.SpriteSheet;
import walknroll.zoodini.utils.DebugPrinter;

public class Animation {
    private final SpriteSheet spriteSheet;
    private final int startFrame;
    private final int endFrame;
    private final int frameDelay;
    private final boolean loop;

    /** Should the animation cycle from beginning to end to beginning? */
    private boolean cycle;
    /** Is the animation currently playing forward or backward? This
     * is always true if "cycle" is false. */
    private boolean isForward;


    private int currentFrame;
    private int cooldown;

    public Animation(SpriteSheet spriteSheet, int startFrame, int endFrame, int frameDelay, boolean loop) {
        this(spriteSheet, startFrame, endFrame, frameDelay, loop, false);
    }

    public Animation(SpriteSheet spriteSheet, int startFrame, int endFrame, int frameDelay, boolean loop, boolean cycle) {
        this.spriteSheet = spriteSheet;
        this.startFrame = startFrame;
        this.endFrame = Math.min(endFrame, spriteSheet.getSize() - 1);
        this.frameDelay = frameDelay;
        this.loop = loop;
        this.currentFrame = startFrame;
        this.cooldown = 0;
        this.cycle = cycle;
    }

    public void update() {
        if (!cycle) {
            if (cooldown == 0) {
                currentFrame++;
                if (currentFrame > endFrame) {
                    currentFrame = loop ? startFrame : endFrame;
                }
                cooldown = frameDelay;
            } else {
                cooldown--;
            }
        }
        else {
            if (cooldown == 0) {
                if (isForward) {
                    currentFrame++;
                    if (currentFrame >= endFrame) {
                        currentFrame = endFrame;
                        isForward = false;
                    }
                } else {
                    currentFrame--;
                    if (currentFrame <= startFrame) {
                        currentFrame = startFrame;
                        isForward = true;
                    }
                }
                cooldown = frameDelay;
            } else {
                cooldown--;
            }
        }

    }

    public void reset() {
        currentFrame = startFrame;
        cooldown = 0;
    }

    public int getCurrentFrame() {
        return currentFrame;
    }

    public SpriteSheet getSpriteSheet() {
        return spriteSheet;
    }
}
