package walknroll.zoodini.controllers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
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
import edu.cornell.gdiac.assets.AssetDirectory;
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
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;


public class UIController {

    public interface PauseMenuListener {
        void onPauseStateChanged(boolean paused);
        void onRestart();
        void onReturnToMenu();
    }

    protected BitmapFont displayFont;

    private TextureRegion catIcon;
    private TextureRegion octopusIcon;
    private TextureRegion pauseIcon;
    private TextureRegion restartIcon;
    private TextureRegion homeIcon;
    private TextureRegion pauseBanner;
    private TextureRegion resumeIcon;

    //Scene 2d components
    private Stage stage;
    private Skin skin;
    private ScreenViewport viewport;

    private Image catIconImage;
    private Image octopusIconImage;
    private Image pauseIconImage;
    private Image restartButton;
    private Image homeButton;
    private Image pauseBannerImage;
    private Image resumeIconImage;
    private Image overlayBackground;
    private Table pauseMenuTable;
    private Image progressBg, progressFill;
    private static final float BAR_WIDTH  = 210f;
    private static final float BAR_HEIGHT = 25f;
    private static final float BAR_X      = 165f;
    private static final float BAR_Y      = 85f;
    private Table rootTable;

    private boolean isPaused = false;
    private PauseMenuListener pauseListener;

    public UIController(AssetDirectory directory) {
        viewport = new ScreenViewport();
        stage = new Stage(viewport);
        skin = new Skin(Gdx.files.internal("skins/uiskin.json")); //TODO: use AssetDirectory to load skins.
        setFont(directory.getEntry("display", BitmapFont.class));
        setCatIcon(new TextureRegion(directory.getEntry("cat-icon", Texture.class)));
        setOctopusIcon(new TextureRegion(directory.getEntry("octopus-icon", Texture.class)));
        setPauseIcon(new TextureRegion(directory.getEntry("pause_icon", Texture.class)));
        setRestartIcon(new TextureRegion(directory.getEntry("restart_icon", Texture.class)));
        setHomeIcon(new TextureRegion(directory.getEntry("home_icon", Texture.class)));
        setPausedBanner(new TextureRegion(directory.getEntry("game_paused", Texture.class)));
        setResumeIcon(new TextureRegion(directory.getEntry("resume_icon", Texture.class)));
    }

    public void init() {
        if (displayFont == null) {
            return;
        }

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

        if (pauseIcon != null) {
            pauseIconImage = new Image(pauseIcon);
            float iconSize = 40f;
            pauseIconImage.setSize(iconSize, iconSize);
            float xPos = Gdx.graphics.getWidth() - iconSize - 20;
            float yPos = Gdx.graphics.getHeight() - iconSize - 20;
            pauseIconImage.setPosition(xPos, yPos);
            pauseIconImage.setVisible(true);
            pauseIconImage.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    togglePauseMenu(true);
                }
            });
            stage.addActor(pauseIconImage);
        }

        if (resumeIcon != null) {
            resumeIconImage = new Image(resumeIcon);
            float iconSize = 40f;
            resumeIconImage.setSize(iconSize, iconSize);
            float xPos = Gdx.graphics.getWidth() - iconSize - 20;
            float yPos = Gdx.graphics.getHeight() - iconSize - 20;
            resumeIconImage.setPosition(xPos, yPos);
            resumeIconImage.setVisible(false);
            resumeIconImage.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    togglePauseMenu(false);
                }
            });
            stage.addActor(resumeIconImage);
        }

        Drawable darkOverlay = skin.newDrawable("white", new Color(0, 0, 0, 0.7f));
        overlayBackground = new Image(darkOverlay);
        overlayBackground.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        overlayBackground.setVisible(false); // Hide initially
        stage.addActor(overlayBackground);

        pauseMenuTable = new Table();
        pauseMenuTable.setBackground(skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 0.9f)));
        pauseMenuTable.setSize(300, 200);
        pauseMenuTable.setPosition(
            (Gdx.graphics.getWidth() - 300) / 2,
            (Gdx.graphics.getHeight() - 200) / 2
        );
        pauseMenuTable.setVisible(false); // Hide initially

        Table buttonTable = new Table();

        if (restartIcon != null) {
            restartButton = new Image(restartIcon);
            restartButton.setSize(64, 64);
            buttonTable.add(restartButton).pad(10).size(64);
            restartButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    // We'll add the restart logic later
                    togglePauseMenu(false);
                    if (pauseListener != null) {
                        pauseListener.onRestart();
                    }
                }
            });
        }

        if (homeIcon != null) {
            homeButton = new Image(homeIcon);
            homeButton.setSize(64, 64);
            buttonTable.add(homeButton).pad(10).size(64);
            homeButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    // We'll add the home menu logic later
                    togglePauseMenu(false);
                    if (pauseListener != null) {
                        pauseListener.onReturnToMenu();
                    }
                }
            });
        }

        overlayBackground.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Only close if clicked on the background, not on menu
                if (!pauseMenuTable.isAscendantOf(event.getTarget())) {
                    togglePauseMenu(false);
                }
            }
        });

        if (pauseBanner != null) {
            pauseBannerImage = new Image(pauseBanner);
            float scale = 1.0f;
            pauseBannerImage.setSize(
                pauseBanner.getRegionWidth() * scale,
                pauseBanner.getRegionHeight() * scale
            );
            Stack bannerStack = new Stack();
            bannerStack.setSize(pauseBannerImage.getWidth(), pauseBannerImage.getHeight());

            Table overlayTable = new Table();
            overlayTable.setFillParent(true);
            overlayTable.center();

            overlayTable.add(buttonTable).padTop(40);

            bannerStack.addActor(pauseBannerImage);
            bannerStack.addActor(overlayTable);
            pauseMenuTable.add(bannerStack).padTop(20).row();
        }

        stage.addActor(pauseMenuTable);


//        inkMeter = new ProgressBar(0, 100, 1, false, skin.get("default-horizontal", ProgressBar.ProgressBarStyle.class));
//        inkMeter.setWidth(210);
//        inkMeter.setHeight(35);
//        inkMeter.setPosition(165, 85); // Same position as the original
//        inkMeter.setAnimateDuration(0.1f); // Slight animation when value changes
//        inkMeter.setVisible(false); // Hide initially
//        inkMeter.setValue(100f);
//        stage.addActor(inkMeter);
    }
    public void togglePauseMenu(boolean paused) {
        isPaused = paused;
        overlayBackground.setVisible(paused);
        pauseMenuTable.setVisible(paused);

        if (pauseIconImage != null) {pauseIconImage.setVisible(!paused);}
        if (resumeIconImage != null) {resumeIconImage.setVisible(paused);}

        if (pauseListener != null) {
            pauseListener.onPauseStateChanged(paused);
        }
    }

    public void setPauseMenuListener(PauseMenuListener listener) {
        this.pauseListener = listener;
    }

    public boolean isPaused(){
        return isPaused;
    }

    public void reset(){
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
    public void setPauseIcon(TextureRegion icon) {pauseIcon = icon;}

    public void setRestartIcon(TextureRegion icon) {
        restartIcon = icon;
    }

    public void setHomeIcon(TextureRegion icon) {
        homeIcon = icon;
    }

    public void setPausedBanner(TextureRegion icon) {
        pauseBanner = icon;
    }

    public void setResumeIcon(TextureRegion icon) {
        resumeIcon = icon;
    }

    public void update() {

    }

    public void resize(int width, int height){
        viewport.update(width, height, true);

        if (pauseIconImage != null) {
            float iconSize = pauseIconImage.getWidth();
            pauseIconImage.setPosition(width - iconSize - 20, height -iconSize - 20);
        }

        if (resumeIconImage != null) {
            float iconSize = resumeIconImage.getWidth();
            resumeIconImage.setPosition(width - iconSize - 20, height - iconSize - 20);
        }
        if (overlayBackground != null) {
            overlayBackground.setSize(width, height);
        }
        if (pauseMenuTable != null) {
            float menuWidth = pauseMenuTable.getWidth();
            float menuHeight = pauseMenuTable.getHeight();
            pauseMenuTable.setPosition((width - menuWidth) / 2, (height - menuHeight) / 2);
        }
    }

    public void dispose() {
        if (stage != null){
            stage.dispose();
        }
        if (skin != null){
            skin.dispose();
        }
    }


    public void draw(GameLevel level) {
        if (Gdx.input.getInputProcessor() != stage) {
            Gdx.input.setInputProcessor(stage);
        }
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
