package walknroll.zoodini.controllers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.TextAlign;
import edu.cornell.gdiac.graphics.TextLayout;

public class UIController {

    public static final int VICTORY = 0;
    public static final int FAILURE = 1;
    public static final int KEY_TIMER = 2;
    public static final int UNLOCKED = 3;
    public static final int KEY_COLLECTED = 4;
    public static final int INTERRUPTED = 5;

    protected OrthographicCamera camera;
    /** The font for giving messages to the player */
    protected BitmapFont displayFont;

    //All texts are displayed by default. Call hideMessage to hide them.
    protected TextLayout victory;
    protected TextLayout failure;
    public TextLayout keyTimer;
    protected TextLayout unlocked;
    protected TextLayout collected;
    protected TextLayout interrupted;

    protected Array<TextLayout> messages;
    /** message location is pixel coordinate of the screen */
    protected ObjectMap<TextLayout, Vector2> messageLocations;

    public UIController(){
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void init(){
        if(displayFont == null){
            return;
        }
        victory = new TextLayout("Victory!", displayFont);
        victory.setAlignment(TextAlign.middleCenter);
        victory.setColor(Color.YELLOW);


        failure = new TextLayout("Failure!", displayFont);
        failure.setAlignment(TextAlign.middleCenter);
        failure.setColor(Color.RED);


        keyTimer = new TextLayout("Unlocking Door: " + 0 + "%", displayFont);
        keyTimer.setAlignment(TextAlign.middleCenter);
        keyTimer.setColor(Color.YELLOW);


        unlocked = new TextLayout("Door Unlocked!", displayFont);
        unlocked.setAlignment(TextAlign.middleCenter);
        unlocked.setColor(Color.GREEN);


        collected = new TextLayout("Key Collected!", displayFont);
        collected.setAlignment(TextAlign.middleCenter);
        collected.setColor(Color.YELLOW);


        interrupted = new TextLayout("Unlocking Interrupted!", displayFont);
        interrupted.setAlignment(TextAlign.middleCenter);
        interrupted.setColor(Color.YELLOW);



        messages = new Array<>();
        messages.add(victory);
        messages.add(failure);
        messages.add(keyTimer);
        messages.add(unlocked);
        messages.add(collected);
        messages.add(interrupted);


        messageLocations = new ObjectMap<>();
        messageLocations.put(victory, new Vector2(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2));
        messageLocations.put(failure, new Vector2(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2));
        messageLocations.put(keyTimer, new Vector2(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2));
        messageLocations.put(unlocked, new Vector2(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2));
        messageLocations.put(collected, new Vector2(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2));
        messageLocations.put(interrupted, new Vector2(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2));

        //hide everything at the start
        for(TextLayout text : messages){
            text.getColor().a = 0.0f;
        }
    }

    public void setCenterMessage(int code){
        for(TextLayout text : messageLocations.keys()){
            Vector2 location = messageLocations.get(text);
            if(location.x == Gdx.graphics.getWidth() / 2 && location.y == Gdx.graphics.getHeight() / 2){
                text.getColor().a = 0.0f;
            }
        }

        messages.get(code).getColor().a = 1.0f;
    }

    public void setFont(BitmapFont f){
        displayFont = f;
    }

    public void hideMessage(int code){
        messages.get(code).getColor().a = 0.0f;
    }

    public void showMessage(int code){
        messages.get(code).getColor().a = 1.0f;
    }

    public void reset(){
        for(TextLayout text : messages){
            text.getColor().a = 0.0f;
        }
    }


    public void update(){
        camera.update();
    }

    public void draw(SpriteBatch batch) {
        batch.setProjectionMatrix(camera.combined);
        batch.begin(camera);
        batch.flush();
        for (TextLayout message : messageLocations.keys()) {
            if (message != null && message.getColor().a == 1.0f) {
                batch.setColor(message.getColor());
                batch.drawText(message, messageLocations.get(message));
                batch.setColor(Color.WHITE);
            }
        }
        batch.flush();
        batch.end();
    }
}
