package walknroll.zoodini.utils.animation;

import edu.cornell.gdiac.graphics.SpriteSheet;

public class Animation {
    private final SpriteSheet spriteSheet;
    private final int startFrame;
    private final int endFrame;
    private final int frameDelay;
    private final boolean loop;

    private int currentFrame;
    private int cooldown;

    public Animation(SpriteSheet spriteSheet, int startFrame, int endFrame, int frameDelay, boolean loop) {
        this.spriteSheet = spriteSheet;
        this.startFrame = startFrame;
        this.endFrame = Math.min(endFrame, spriteSheet.getSize() - 1);
        this.frameDelay = frameDelay;
        this.loop = loop;
        this.currentFrame = startFrame;
        this.cooldown = 0;
    }

    public void update() {
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
