package walknroll.zoodini.utils;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import edu.cornell.gdiac.graphics.SpriteSheet;

public class InkMeterActor extends Actor {
    private final float capacity;
    private final float cost;
    private final float rechargeRate;
    private final SpriteSheet[] spriteSheet;
    private int filledBars;
    private float timeElapsed = 0f;
    private float frameDelay;
    private int frameCount;

    public InkMeterActor(SpriteSheet sheet, float cap, float cost, float rate){
        this.capacity = cap;
        this.cost = cost;
        this.rechargeRate = rate;

        int barCount = (int)(cap / cost);
        this.spriteSheet = new SpriteSheet[barCount];
        for (int i = 0; i < barCount; i++) {
            this.spriteSheet[i] = sheet.copy();
            this.spriteSheet[i].setFrame(0);
        }

        this.frameCount = sheet.getSize();
        this.frameDelay = cap / ((frameCount - 1) * rate * cost);
    }

    public void sync(float currentCharge){
        filledBars = (int)(currentCharge / cost);
    }

    @Override
    public void act(float dt){
        super.act(dt);
        timeElapsed += dt;

        if (timeElapsed >= frameDelay) {
            timeElapsed = 0f;
            for (int i = 0; i < spriteSheet.length; i++) {
                if (i < filledBars) {
                    int currentFrame = spriteSheet[i].getFrame();
                    if (currentFrame < frameCount - 1) {
                        spriteSheet[i].setFrame(currentFrame + 1);
                    }
                } else {
                    spriteSheet[i].setFrame(0);
                }
            }
        }
    }
    @Override
    public void draw(Batch batch, float parentAlpha){
        float barWidth = spriteSheet[0].getRegionWidth();
        float barHeight = spriteSheet[0].getRegionHeight();
        for (int i = 0; i < spriteSheet.length; i++) {
            batch.draw(spriteSheet[i], getX() + i * (barWidth + 2), getY(), barWidth, barHeight);
        }
    }
}
