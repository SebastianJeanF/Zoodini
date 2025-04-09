package walknroll.zoodini.controllers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.TextAlign;
import edu.cornell.gdiac.graphics.TextLayout;
import walknroll.zoodini.models.GameLevel;
import walknroll.zoodini.models.entities.Avatar;
import walknroll.zoodini.models.entities.Avatar.AvatarType;
import walknroll.zoodini.models.entities.Octopus;

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

    // All texts are displayed by default. Call hideMessage to hide them.
    protected TextLayout victory;
    protected TextLayout failure;
    public TextLayout keyTimer;
    protected TextLayout unlocked;
    protected TextLayout collected;
    protected TextLayout interrupted;

    protected Array<TextLayout> messages;
    /** message location is pixel coordinate of the screen */
    protected ObjectMap<TextLayout, Vector2> messageLocations;

    public UIController() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void init() {
        if (displayFont == null) {
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
    }

    public void reset() {

    }

    public void setFont(BitmapFont f) {
        displayFont = f;
    }

    public void update() {
        camera.update();
    }

    private void drawInkMeter(SpriteBatch batch, Octopus octopus) {
        batch.setTexture(null);
        batch.setColor(Color.BLACK);
        batch.outline(45, 45, 210, 35);
        batch.fill(50, 50, (octopus.getInkRemaining() / octopus.getInkCapacity()) * 200f, 25);
        batch.setColor(Color.WHITE);
    }

    public void draw(SpriteBatch batch, GameLevel level) {
        batch.begin(camera);
        Avatar avatar = level.getAvatar();

        if (avatar.getAvatarType() == AvatarType.OCTOPUS) {
            drawInkMeter(batch, (Octopus) avatar);
        }

        batch.end();
    }
}
