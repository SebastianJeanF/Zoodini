package walknroll.zoodini.utils;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import edu.cornell.gdiac.graphics.SpriteSheet;

public class InkMeterActor extends Actor {
    private final float capacity;
    private final float cost;
    private final float rechargeRate;
    private float currentCharge;
    private final SpriteSheet[] spriteSheet;
    private float timeElapsed = 0f;
    private int frameCount;
    private float chargePerFrame;

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
    }

    public void sync(float currentCharge){
        this.currentCharge = currentCharge;
    }

    @Override
    public void act(float dt){
        super.act(dt);

        int totalBars = spriteSheet.length;
        float chargePerBar = cost;
        float normalizedCharge = Math.min(currentCharge, capacity);

        for (int i = 0; i < totalBars; i++) {
            float barCharge = normalizedCharge - i * chargePerBar;

            if (barCharge >= chargePerBar) {
                spriteSheet[i].setFrame(frameCount - 1);
            } else if (barCharge > 0) {
                int frameIndex = Math.min((int)((barCharge / chargePerBar) * frameCount), frameCount - 1);
                spriteSheet[i].setFrame(frameIndex);
            } else {
                spriteSheet[i].setFrame(0);
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
