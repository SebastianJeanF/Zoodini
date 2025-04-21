package walknroll.zoodini.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;

public class CounterActor extends Actor {
    private final BitmapFont font;
    private Label label;
    private int max;
    private int current;
    private float elapsedTime;

    public CounterActor(BitmapFont font, int maxCount){
        this.font = font;
        this.max = maxCount;
        LabelStyle style = new LabelStyle(font, Color.WHITE);
        label = new Label(String.valueOf(current), style);
    }

    @Override
    public void act(float dt){
        super.act(dt);
        elapsedTime += dt;
        if(elapsedTime > max){
            elapsedTime = 0;
        }
        current = (int) elapsedTime;
        label.setText(current);
    }

    @Override
    public void draw (Batch batch, float parentAlpha) {
        label.setPosition(getX(), getY());
        label.draw(batch, parentAlpha);
    }
}
