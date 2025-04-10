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
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.TextAlign;
import edu.cornell.gdiac.graphics.TextLayout;
import walknroll.zoodini.utils.CircleTimer;
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

    private CircleTimer unlockTimer;
    private boolean showUnlockTimer = false;

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

    private TextureRegion catIcon;
    private TextureRegion octopusIcon;

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
        //Initializing unlock timer
        float centerX = Gdx.graphics.getWidth()/2;
        float centerY = Gdx.graphics.getHeight()/2;
        unlockTimer = new CircleTimer(centerX, centerY, 30, Color.YELLOW);

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


    public void showUnlockProgress(float progress, Vector2 doorPosition, Camera gameCamera, float tileSize) {
        showUnlockTimer = true;

        Vector3 screenPos = new Vector3(doorPosition.x * tileSize, doorPosition.y * tileSize, 0);
        gameCamera.project(screenPos);
        unlockTimer.setPosition(screenPos.x, screenPos.y);
        unlockTimer.setProgress(progress);
    }
    public void hideUnlockProgress() {
        showUnlockTimer = false;
    }
    public void reset(){
    hideUnlockProgress();
    }

    public void setFont(BitmapFont f) {
        displayFont = f;
    }

    public void setCatIcon(TextureRegion icon) {
        catIcon = icon;
    }
    public void setOctopusIcon(TextureRegion icon) {
        octopusIcon = icon;
    }

    public void update() {
        camera.update();
    }


    public void draw(SpriteBatch batch) {
        // Save batch state
        boolean wasDrawing = batch.isDrawing();

        // Always end the batch to ensure the CircleTimer draws on top
        if (wasDrawing) {
            batch.end();
        }

        if (showUnlockTimer) {
            // Clear depth buffer to ensure timer appears on top
            Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
            unlockTimer.draw();
        }

        // Restore batch state
        if (wasDrawing) {
            batch.begin(camera);
        }
    }
    public void dispose() {
        if (unlockTimer != null) {
            unlockTimer.dispose();
        }
    }

    public void drawCatIcon(SpriteBatch batch){
        batch.setTextureRegion(catIcon);
        batch.setColor(Color.WHITE);
        batch.draw(catIcon, 45, 30, 0, 0, catIcon.getRegionWidth(), catIcon.getRegionHeight(), 1f, 1f, 0);
        batch.setColor(Color.WHITE);
    }

    public void drawOctopusIcon(SpriteBatch batch){
        batch.setTextureRegion(octopusIcon);
        batch.setColor(Color.WHITE);
        batch.draw(octopusIcon, 45, 30, 0, 0, octopusIcon.getRegionWidth(), octopusIcon.getRegionHeight(), 1f, 1f, 0);
        batch.setColor(Color.WHITE);
    }

    private void drawInkMeter(SpriteBatch batch, Octopus octopus) {
        batch.setTexture(null);
        batch.setColor(Color.BLACK);
        batch.outline(245, 115, 210, 35);
        batch.setColor(octopus.canUseAbility() ? Color.FOREST : Color.BLACK);
        batch.fill(250, 120, (octopus.getInkRemaining() / octopus.getInkCapacity()) * 200f, 25);
        batch.setColor(Color.WHITE);
    }

    public void draw(SpriteBatch batch, GameLevel level) {
        batch.begin(camera);
        Avatar avatar = level.getAvatar();

        if (avatar.getAvatarType() == AvatarType.OCTOPUS) {
            drawInkMeter(batch, (Octopus) avatar);
            drawOctopusIcon(batch);
        } else {
            drawCatIcon(batch);
        }

        batch.end();

    }
}
