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
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar.ProgressBarStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.TextAlign;
import edu.cornell.gdiac.graphics.TextLayout;
import walknroll.zoodini.models.nonentities.Door;
import walknroll.zoodini.models.nonentities.Key;
import walknroll.zoodini.utils.CircleTimer;
import walknroll.zoodini.models.GameLevel;
import walknroll.zoodini.models.entities.Avatar;
import walknroll.zoodini.models.entities.Avatar.AvatarType;
import walknroll.zoodini.models.entities.Octopus;
//Scene2d stuff
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
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

    //Scene 2d components
    private Stage stage;
    private Skin skin;
    private ScreenViewport viewport;

    private Image catIconImage;
    private Image octopusIconImage;
    private ProgressBar inkMeter;
    private Table rootTable;

    public UIController() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        //Scene2d
        viewport = new ScreenViewport();
        stage = new Stage(viewport);
        // Temporary Skin
        skin = new Skin(Gdx.files.internal("skins/uiskin.json"));
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
        // Temporarily commented to help with transition
        // Gdx.input.setInputProcessor(stage);
        skin.add("default", displayFont);
        // UI Element Style
        Label.LabelStyle labelStyle = new LabelStyle();
        labelStyle.font = displayFont;
        skin.add("default", labelStyle);
        //Create button style
        TextButton.TextButtonStyle textButtonStyle = new TextButtonStyle();
        textButtonStyle.up = skin.newDrawable("white", Color.DARK_GRAY);
        textButtonStyle.down = skin.newDrawable("white", Color.DARK_GRAY);
        textButtonStyle.checked = skin.newDrawable("white", Color.BLUE);
        textButtonStyle.over = skin.newDrawable("white", Color.LIGHT_GRAY);
        textButtonStyle.font = displayFont;
        skin.add("default", textButtonStyle);
        //Progress Bar
        ProgressBar.ProgressBarStyle progressBarStyle = new ProgressBarStyle();
        progressBarStyle.background = skin.newDrawable("white", Color.DARK_GRAY);
        progressBarStyle.knob = skin.newDrawable("white", new Color(0, 0.8f, 0, 1)); // Green knob
        progressBarStyle.knobBefore = skin.newDrawable("white", new Color(0, 0.8f, 0, 1)); // Green fill
        skin.add("default-horizontal", progressBarStyle);
        // Create root table for layout
        rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        if (catIcon != null) {
            catIconImage = new Image(catIcon);
            catIconImage.setSize(catIcon.getRegionWidth() * 0.7f, catIcon.getRegionHeight() * 0.7f);
            catIconImage.setPosition(45, 30);
            stage.addActor(catIconImage);
        }

        if (octopusIcon != null){
            octopusIconImage = new Image(octopusIcon);
            octopusIconImage.setSize(octopusIcon.getRegionWidth() * 0.7f, octopusIcon.getRegionHeight() * 0.7f);
            octopusIconImage.setPosition(45, 30);
            octopusIconImage.setVisible(false); // Hide initially, we'll toggle visibility
            stage.addActor(octopusIconImage);
        }
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

    public void resize(int width, int height){
        viewport.update(width, height, true);
    }

    public void dispose() {
        if (unlockTimer != null) {
            unlockTimer.dispose();
        }

        if (stage != null){
            stage.dispose();
        }
        if (skin != null){
            skin.dispose();
        }
    }

    public void drawCatIcon(SpriteBatch batch) {
        batch.setTextureRegion(catIcon);
        batch.setColor(Color.WHITE);
        // Using 0.7f scale to make the icon 70% of its original size
        batch.draw(catIcon, 45, 30, 0, 0, catIcon.getRegionWidth(), catIcon.getRegionHeight(), 0.7f, 0.7f, 0);
        batch.setColor(Color.WHITE);
    }

    public void drawOctopusIcon(SpriteBatch batch) {
        batch.setTextureRegion(octopusIcon);
        batch.setColor(Color.WHITE);
        // Using 0.7f scale to make the icon 70% of its original size
        batch.draw(octopusIcon, 45, 30, 0, 0, octopusIcon.getRegionWidth(), octopusIcon.getRegionHeight(), 0.7f, 0.7f, 0);
        batch.setColor(Color.WHITE);
    }

    private void drawInkMeter(SpriteBatch batch, Octopus octopus) {
        batch.setTexture(null);
        batch.setColor(Color.BLACK);
        batch.outline(165, 85, 210, 35);
        batch.setColor(octopus.canUseAbility() ? Color.FOREST : Color.BLACK);
        batch.fill(170, 90, (octopus.getInkRemaining() / octopus.getInkCapacity()) * 200f, 25);
        batch.setColor(Color.WHITE);
    }



    public void draw(SpriteBatch batch, OrthographicCamera gameCamera, GameLevel level) {
//        batch.begin(camera);
//        Avatar avatar = level.getAvatar();
//
//        if (avatar.getAvatarType() == AvatarType.OCTOPUS) {
//            drawInkMeter(batch, (Octopus) avatar);
//            drawOctopusIcon(batch);
//        } else {
//            drawCatIcon(batch);
//        }

//        ObjectMap<Door, Key>  doors =  level.getDoors();
//        for (ObjectMap.Entry<Door, Key> entry : doors.entries()) {
//            Door door = entry.key;
//            Key key = entry.value;
//            if (key.isUnlocking()) {
//                showUnlockProgress(key.getUnlockProgress(), door.getPosition(), camera, level.getTileSize());
//            }
//        }
//        drawDoorUnlocking(batch, );
        Avatar avatar = level.getAvatar();
        if (catIconImage != null) {
            catIconImage.setVisible(avatar.getAvatarType() == AvatarType.CAT);
        }
        if (octopusIconImage != null) {
            octopusIconImage.setVisible(avatar.getAvatarType() == AvatarType.OCTOPUS);
        }
        //ink meter still uses old system
        batch.begin(camera);
        if (avatar.getAvatarType() == AvatarType.OCTOPUS) {
            drawInkMeter(batch, (Octopus) avatar);
        }
        batch.end();
        // Stage Rendering
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();

    }
}
