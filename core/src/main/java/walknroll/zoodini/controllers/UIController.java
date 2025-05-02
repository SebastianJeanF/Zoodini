package walknroll.zoodini.controllers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteSheet;
import walknroll.zoodini.models.GameLevel;
import walknroll.zoodini.models.entities.Avatar;

//Scene2d stuff
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;


import walknroll.zoodini.models.entities.Octopus;
import walknroll.zoodini.utils.CounterActor;
import walknroll.zoodini.utils.InkMeterActor;
import walknroll.zoodini.utils.MeowCooldownIndicator;
import walknroll.zoodini.utils.MinimapActor;
import walknroll.zoodini.utils.enums.AvatarType;

public class UIController {

    private final boolean debug = false;

    public interface PauseMenuListener {
        void onPauseStateChanged(boolean paused);

        void onRestart();

        void onReturnToMenu();
    }

    protected BitmapFont displayFont;

    private MinimapActor minimap;

    //Scene 2d components
    private Stage stage;
    private Skin skin;
    private ScreenViewport viewport;
    private Table rootTable;

    private Image catIconImage;
    private Image octopusIconImage;
    private InkMeterActor inkMeter;
    private Image smallCatIconImage;
    private Image smallOctopusIconImage;
    private Image dangerIconImage;
    private Image pauseIconImage;
    private Image restartButton;
    private Image homeButton;
    private Image pauseBannerImage;
    private Image resumeIconImage;
    private Image overlayBackground;
    private Table pauseMenuTable;
    private Image resumeButtonImage;
    private Image inkTextImage;
    private Image switch1;
    private Image switch2;

    private MeowCooldownIndicator meowCooldownIndicator;

    private boolean isPaused = false;
    private PauseMenuListener pauseListener;

    public UIController(AssetDirectory directory, GameLevel level) {
        viewport = new ScreenViewport();
        stage = new Stage(viewport);
        skin = new Skin(Gdx.files.internal("uiskins/default/uiskin.json")); //TODO: use AssetDirectory to load skins.
        initializeActors(directory, level);
        setupStageLayout();
    }


    /**
     * Initialize all the Actors (Images, Buttons, etc.)
     */
    private void initializeActors(AssetDirectory directory, GameLevel level){
        setFont(directory.getEntry("display", BitmapFont.class));

        setCatIconImage(new TextureRegion(directory.getEntry("cat-icon", Texture.class)));
        catIconImage.setVisible(false);

        setOctopusIconImage(new TextureRegion(directory.getEntry("octopus-icon", Texture.class)));
        octopusIconImage.setVisible(false);
        minimap = new MinimapActor(level);
        minimap.setPosition(20, Gdx.graphics.getHeight() - (minimap.getHeight() + 20));
        minimap.setCatTexture((directory.getEntry("cat-walk-transition", Texture.class)));
        minimap.setOctopusTexture((directory.getEntry("octopus", Texture.class)));

        if (level.isOctopusPresent()) {
            SpriteSheet inkSprites = directory.getEntry("ink-meter.animation", SpriteSheet.class);
            Octopus o = level.getOctopus();
            inkMeter = new InkMeterActor(inkSprites, o.getInkCapacity(), o.getInkCost(), o.getInkRegen());
        } else {
            inkMeter = null; // Make sure it's null when octopus is not present
        }

        inkTextImage = new Image(directory.getEntry("ink-text", Texture.class));

        smallCatIconImage = new Image(directory.getEntry("small-cat-icon", Texture.class));
        smallOctopusIconImage = new Image(directory.getEntry("small-octopus-icon", Texture.class));
        dangerIconImage = new Image(directory.getEntry("danger-icon", Texture.class));
        pauseIconImage = new Image(directory.getEntry("pause_icon", Texture.class));
        restartButton = new Image(directory.getEntry("restart_icon", Texture.class));
        homeButton = new Image(directory.getEntry("home-icon", Texture.class));
        pauseBannerImage = new Image(directory.getEntry("game_paused", Texture.class));
        resumeIconImage = new Image(directory.getEntry("resume_icon", Texture.class));
        resumeButtonImage = new Image(directory.getEntry("resume_button", Texture.class));
        switch1 = new Image(directory.getEntry("switch1", Texture.class));
        switch2 = new Image(directory.getEntry("switch2", Texture.class));

    }


    /**
     * Places each Actor at the right position.
     */
    private void setupStageLayout(){
        viewport = new ScreenViewport();
        stage = new Stage(viewport);
        rootTable = new Table();
        rootTable.setFillParent(true);

        stage.addActor(minimap);

        Table bottomLeftTable = new Table();
        bottomLeftTable.bottom().left();
        bottomLeftTable.setFillParent(true);
        bottomLeftTable.setDebug(debug);
        stage.addActor(bottomLeftTable);

        //Put icons at the top of each other.
        Stack stack = new Stack();
        stack.add(catIconImage);
        stack.add(octopusIconImage);
        bottomLeftTable.add(stack).pad(30);
        bottomLeftTable.add(inkMeter).align(Align.bottomLeft).padBottom(35);
        bottomLeftTable.add(inkTextImage);

        Table topRightTable = new Table();
        topRightTable.setFillParent(true);
        topRightTable.setDebug(debug);
        topRightTable.top().right();
        Stack s = new Stack();
        switch1.setVisible(false);
        switch2.setVisible(false);
        s.add(switch1);
        s.add(switch2);
        topRightTable.add(s).pad(10f);
        Stack pauseStack = new Stack();
        pauseStack.add(pauseIconImage);
        pauseStack.add(resumeIconImage);
        topRightTable.add(pauseStack).height(switch1.getHeight()).width(switch1.getHeight()).pad(10f);
        stage.addActor(topRightTable);


        //TODO: don't hardcode positions. Use tables.
        if (smallCatIconImage != null) {
            smallCatIconImage.setPosition(45, 600);
            smallCatIconImage.setVisible(false);
            stage.addActor(smallCatIconImage);
        }

        if (smallOctopusIconImage != null) {
            smallOctopusIconImage.setPosition(45, 600);
            smallOctopusIconImage.setVisible(false); // Hide initially, we'll toggle visibility
            stage.addActor(smallOctopusIconImage);
        }

        if (dangerIconImage != null) {
            dangerIconImage.setPosition(105, 615);
            dangerIconImage.setVisible(false);
            stage.addActor(dangerIconImage);
        }

        if (pauseIconImage != null) {
            pauseIconImage.setVisible(true);
            pauseIconImage.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    togglePauseMenu(true);
                }
            });
            //stage.addActor(pauseIconImage);
        }

        if (resumeIconImage != null) {
            resumeIconImage.setVisible(false);
            resumeIconImage.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    togglePauseMenu(false);
                }
            });
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

        if (restartButton != null) {
            restartButton.setSize(64, 64);
            buttonTable.add(restartButton).pad(10).size(48);
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

        if (homeButton != null) {
            homeButton.setSize(64, 64);
            buttonTable.add(homeButton).pad(10).size(48);
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
        if (resumeButtonImage != null) {
            resumeButtonImage.setSize(resumeButtonImage.getWidth() * 0.25f, resumeButtonImage.getHeight() * 0.25f);
            resumeButtonImage.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    togglePauseMenu(false);
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

        if (pauseBannerImage != null) {
            float scale = 1.0f;
            pauseBannerImage.setSize(
                pauseBannerImage.getWidth() * scale,
                pauseBannerImage.getHeight() * scale
            );
            Stack bannerStack = new Stack();
            bannerStack.setSize(pauseBannerImage.getWidth(), pauseBannerImage.getHeight());

            Table overlayTable = new Table();
            overlayTable.setFillParent(true);
            overlayTable.center();

            if (resumeButtonImage != null) {
                Table resumeRow = new Table();
                resumeRow.add(resumeButtonImage).padTop(67).padLeft(22).padRight(22);
                overlayTable.add(resumeRow).padTop(5).row();
            }
            overlayTable.add(buttonTable).padTop(0).row();

            bannerStack.addActor(pauseBannerImage);
            bannerStack.addActor(overlayTable);
            pauseMenuTable.add(bannerStack).padTop(20).row();
        }

        stage.addActor(pauseMenuTable);

        meowCooldownIndicator = new MeowCooldownIndicator(displayFont);
        meowCooldownIndicator.setPosition(viewport.getScreenWidth() - 200, 40);  // Position it where needed
        stage.addActor(meowCooldownIndicator);

    }

    public void setFont(BitmapFont f) {
        displayFont = f;
    }

    public void setCatIconImage(TextureRegion icon) {
        catIconImage = new Image(icon);
    }
    public void setOctopusIconImage(TextureRegion icon) {
        octopusIconImage = new Image(icon);
    }

    public void setSmallCatIcon(TextureRegion icon) {
        smallCatIconImage = new Image(icon);
    }

    public void setSmallOctopusIcon(TextureRegion icon) {
        smallOctopusIconImage = new Image(icon);
    }

    public void setDangerIcon(TextureRegion icon) {
        dangerIconImage = new Image(icon);
    }
    public void setPauseIcon (TextureRegion icon){
        pauseIconImage = new Image(icon);
    }

    public void setRestartIcon (TextureRegion icon){
        restartButton = new Image(icon);
    }

    public void setHomeIcon (TextureRegion icon){
        homeButton = new Image(icon);
    }

    public void setPausedBanner (TextureRegion icon){
        pauseBannerImage = new Image(icon);
    }

    public void setResumeIcon (TextureRegion icon){
        resumeIconImage = new Image(icon);
    }

    public void setResumeButton (TextureRegion icon){
        resumeButtonImage = new Image(icon);
    }

    public void update(float dt){
        stage.act(dt);
    }

    public void draw (GameLevel level){
        if (Gdx.input.getInputProcessor() != stage) {
            Gdx.input.setInputProcessor(stage);
        }

        Avatar avatar = level.getAvatar();
        boolean isOcto = avatar.getAvatarType() == AvatarType.OCTOPUS;

        // In draw method, add:
        if (avatar.getAvatarType() == AvatarType.CAT) {
            meowCooldownIndicator.setVisible(true);
            inkTextImage.setVisible(false);
            switch1.setVisible(true);
            switch2.setVisible(false);
            meowCooldownIndicator.update(level.getCat());
        } else {
            meowCooldownIndicator.setVisible(false);
            switch1.setVisible(false);
            switch2.setVisible(true);
            inkTextImage.setVisible(true);
        }

        // Icons
        if (catIconImage != null) catIconImage.setVisible(!isOcto);
        if (octopusIconImage != null) octopusIconImage.setVisible(isOcto);
        if (inkMeter != null && level.isOctopusPresent()) {
            inkMeter.setVisible(isOcto);
            inkMeter.sync(level.getOctopus().getInkRemaining());
        }

        if (dangerIconImage != null && smallCatIconImage != null && smallOctopusIconImage != null) {
            if (level.isInactiveAvatarInDanger()) {
                dangerIconImage.setVisible(true);
                if (isOcto) {
                    smallCatIconImage.setVisible(true);
                    smallOctopusIconImage.setVisible(false);
                } else {
                    smallOctopusIconImage.setVisible(true);
                    smallCatIconImage.setVisible(false);
                }
            } else {
                dangerIconImage.setVisible(false);
                smallCatIconImage.setVisible(false);
                smallOctopusIconImage.setVisible(false);
            }
        }

        // finally… draw the stage
        stage.draw();
    }

    public void togglePauseMenu(boolean paused) {
        isPaused = paused;
        overlayBackground.setVisible(paused);
        pauseMenuTable.setVisible(paused);

        if (pauseIconImage != null) {
            pauseIconImage.setVisible(!paused);
        }
        if (resumeIconImage != null) {
            resumeIconImage.setVisible(paused);
        }

        if (pauseListener != null) {
            pauseListener.onPauseStateChanged(paused);
        }
    }
    public void dispose(){
        stage.dispose();
        if (minimap != null) {
            minimap.dispose();
        }
    }

    public void setPauseMenuListener(PauseMenuListener l){
        pauseListener = l;
    }
}
