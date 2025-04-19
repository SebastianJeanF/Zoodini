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
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
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
import com.badlogic.gdx.graphics.g2d.NinePatch;
public class UIController {

    private CircleTimer unlockTimer;
    private boolean showUnlockTimer = false;

    protected BitmapFont displayFont;

    private TextureRegion catIcon;
    private TextureRegion octopusIcon;

    //Scene 2d components
    private Stage stage;
    private Skin skin;
    private ScreenViewport viewport;

    private Image catIconImage;
    private Image octopusIconImage;
    private Image progressBg, progressFill;
    private static final float BAR_WIDTH  = 210f;
    private static final float BAR_HEIGHT = 25f;
    private static final float BAR_X      = 165f;
    private static final float BAR_Y      = 85f;
    private Table rootTable;

    public UIController() {
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
        //Initializing unlock timer
        float centerX = Gdx.graphics.getWidth()/2.0f;
        float centerY = Gdx.graphics.getHeight()/2.0f;
        unlockTimer = new CircleTimer(centerX, centerY, 30, Color.YELLOW);

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
        Drawable bgDrawable = skin.newDrawable("white", Color.DARK_GRAY);
        progressBg = new Image(bgDrawable);
        progressBg.setSize(BAR_WIDTH, BAR_HEIGHT);
        progressBg.setPosition(BAR_X, BAR_Y);
        progressBg.setVisible(false);
        stage.addActor(progressBg);

        Drawable fillDrawable = skin.newDrawable("white", new Color(0,0.8f,0,1));
        progressFill = new Image(fillDrawable);
        progressFill.setSize(0, BAR_HEIGHT);          // start at 0 width
        progressFill.setPosition(BAR_X, BAR_Y);
        progressFill.setVisible(false);
        stage.addActor(progressFill);
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

//        inkMeter = new ProgressBar(0, 100, 1, false, skin.get("default-horizontal", ProgressBar.ProgressBarStyle.class));
//        inkMeter.setWidth(210);
//        inkMeter.setHeight(35);
//        inkMeter.setPosition(165, 85); // Same position as the original
//        inkMeter.setAnimateDuration(0.1f); // Slight animation when value changes
//        inkMeter.setVisible(false); // Hide initially
//        inkMeter.setValue(100f);
//        stage.addActor(inkMeter);
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


    public void draw(GameLevel level) {
        Avatar avatar = level.getAvatar();
        boolean isOcto = avatar.getAvatarType() == AvatarType.OCTOPUS;

        // Icons
        if (catIconImage  != null) catIconImage.setVisible(!isOcto);
        if (octopusIconImage != null) octopusIconImage.setVisible(isOcto);

        // Progress bar background + fill
        progressBg.setVisible(isOcto);
        progressFill.setVisible(isOcto);

        if (isOcto) {
            Octopus octo = (Octopus)avatar;
            // compute 0→1 fill ratio
            float pct = octo.getInkRemaining() / octo.getInkCapacity();
            // resize the green fill bar
            progressFill.setWidth(BAR_WIDTH * pct);
            if (octo.canUseAbility()) {
                progressFill.setColor(Color.GREEN);  // green
            } else {
                progressFill.setColor(Color.BLACK);       // black
            }
        }

        // finally… draw the stage
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }
}
