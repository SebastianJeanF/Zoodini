package walknroll.zoodini.controllers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
// Scene2d stuff
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.SpriteSheet;
import walknroll.zoodini.controllers.screens.GameScene;
import walknroll.zoodini.models.GameLevel;
import walknroll.zoodini.models.entities.Octopus;
import walknroll.zoodini.models.entities.PlayableAvatar;
import walknroll.zoodini.utils.GameSettings;
import walknroll.zoodini.utils.InkMeterActor;
import walknroll.zoodini.utils.MeowCooldownIndicator;
import walknroll.zoodini.utils.MinimapActor;
import walknroll.zoodini.utils.enums.AvatarType;
import walknroll.zoodini.utils.enums.ExitAnimal;

public class UIController {

    public interface PauseMenuListener {

        void onPauseStateChanged(boolean paused);

        void onRestart();

        void onReturnToMenu();
    }

    private final boolean debug = false;

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
    private Image catFollowIconImage;
    private Image octopusFollowIconImage;
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
    private Image keyInventory;
    private Image catKeyInventory;
    private Label keyCount;
    private Label p2KeyCount;
    private Image screenDivider;

    private MeowCooldownIndicator meowCooldownIndicator;

    private boolean isPaused = false;

    private PauseMenuListener pauseListener;

    public UIController(AssetDirectory directory, GameLevel level, SpriteBatch batch) {
        viewport = new ScreenViewport();
        stage = new Stage(viewport, batch);
        skin = new Skin(Gdx.files.internal("uiskins/default/uiskin.json")); //TODO: use AssetDirectory to load skins.
        initializeActors(directory, level);
        setupStageLayout(level);
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void update(float dt) {
        stage.act(dt);
    }

    public void draw(GameScene scene) {
        GameLevel level = scene.getLevel();
        if (Gdx.input.getInputProcessor() != stage) {
            Gdx.input.setInputProcessor(stage);
        }

        PlayableAvatar avatar = level.getAvatar();
        boolean isOcto = avatar.getAvatarType() == AvatarType.OCTOPUS;

        if (GameSettings.getInstance().isCoopEnabled() && level.isOctopusPresent() && level
                .isCatPresent()) {
            catIconImage.setVisible(true);
            octopusIconImage.setVisible(true);
            if (inkMeter != null && level.isOctopusPresent()) {
                inkMeter.setVisible(true);
                inkMeter.sync(level.getOctopus().getInkRemaining());
            }
            meowCooldownIndicator.setVisible(true);
            meowCooldownIndicator.update(level.getCat());
            if (level.isOctopusPresent()) {
                p2KeyCount.setText("x" + level.getOctopus().getNumKeys());
            }

        } else {
            if (avatar.getAvatarType() == AvatarType.CAT) {
                meowCooldownIndicator.setVisible(true);
                inkTextImage.setVisible(false);
                switch1.setVisible(true);
                switch2.setVisible(false);
                meowCooldownIndicator.update(level.getCat());
            } else {
                // Octopus
                meowCooldownIndicator.setVisible(false);
                switch1.setVisible(false);
                switch2.setVisible(true);
                inkTextImage.setVisible(true);
            }

            if (scene.getFollowModeActive()) {
                catFollowIconImage.setVisible(isOcto);
                octopusFollowIconImage.setVisible(!isOcto);
            } else {
                catFollowIconImage.setVisible(false);
                octopusFollowIconImage.setVisible(false);
            }
            // Icons
            if (catIconImage != null)
                catIconImage.setVisible(!isOcto);
            if (octopusIconImage != null)
                octopusIconImage.setVisible(isOcto);
            if (inkMeter != null && level.isOctopusPresent()) {
                inkMeter.setVisible(isOcto);
                inkMeter.sync(level.getOctopus().getInkRemaining());
            }
        }

        if (screenDivider != null) {
            screenDivider.setVisible(GameSettings.getInstance().isCoopEnabled() && level
                    .isOctopusPresent() && level.isCatPresent());
        }

        // Update the minimap
        keyCount.setText("x" + avatar.getNumKeys());

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

    public void dispose() {
        stage.dispose();
        if (minimap != null) {
            minimap.dispose();
        }
    }

    public void setPauseMenuListener(PauseMenuListener l) {
        pauseListener = l;
    }

    /**
     * Initialize all the Actors (Images, Buttons, etc.)
     */
    private void initializeActors(AssetDirectory directory, GameLevel level) {
        displayFont = directory.getEntry("display", BitmapFont.class);

        catIconImage = new Image(directory.getEntry("cat-icon", Texture.class));
        catIconImage.setVisible(false);

        octopusIconImage = new Image((directory.getEntry("octopus-icon", Texture.class)));
        octopusIconImage.setVisible(false);
        minimap = new MinimapActor(directory, level);
        minimap.setPosition(20, Gdx.graphics.getHeight() - (minimap.getHeight() + 20));
        minimap.setCatTexture(directory.getEntry("cat-walk-transition", Texture.class));
        minimap.setOctopusTexture(directory.getEntry("octopus", Texture.class));
        minimap.setGuardTexture(directory.getEntry("guard-single", Texture.class));
        minimap.setKeyTexture(directory.getEntry("key", Texture.class));
        minimap.setVentTexture(directory.getEntry("vent-closed", Texture.class));
        minimap.setCameraTexture(directory.getEntry("security-camera", Texture.class));
        ExitAnimal animalType = level.getExit().getCreature();
        TextureRegion t;
        switch (animalType) {
            case RABBIT -> t = directory.getEntry("rabbit-chained-idle.animation",
                    SpriteSheet.class);
            case PENGUIN -> t = directory.getEntry("penguin-chained-idle.animation",
                    SpriteSheet.class);
            case OCTOPUS -> t = directory.getEntry("octopus-chained-idle.animation",
                    SpriteSheet.class);
            default -> t = directory.getEntry("panda-chained-idle.animation", SpriteSheet.class);
        }
        minimap.setExitTexture(t);
        // minimap.setExitTexture(directory.getEntry("exit", Texture.class));

        if (level.isOctopusPresent()) {
            SpriteSheet inkSprites = directory.getEntry("ink-meter.animation", SpriteSheet.class);
            Octopus o = level.getOctopus();
            inkMeter = new InkMeterActor(inkSprites, o.getInkCapacity(), o.getInkCost(), o
                    .getInkRegen());
        } else {
            inkMeter = null; // Make sure it's null when octopus is not present
        }

        inkTextImage = new Image(directory.getEntry("ink-text", Texture.class));

        smallCatIconImage = new Image(directory.getEntry("small-cat-icon", Texture.class));
        smallOctopusIconImage = new Image(directory.getEntry("small-octopus-icon", Texture.class));
        catFollowIconImage = new Image(directory.getEntry("cat-follow-icon", Texture.class));
        octopusFollowIconImage = new Image(directory.getEntry("octopus-follow-icon",
                Texture.class));
        dangerIconImage = new Image(directory.getEntry("danger-icon", Texture.class));
        pauseIconImage = new Image(directory.getEntry("pause_icon", Texture.class));
        restartButton = new Image(directory.getEntry("restart_icon", Texture.class));
        homeButton = new Image(directory.getEntry("home-icon", Texture.class));
        pauseBannerImage = new Image(directory.getEntry("game_paused", Texture.class));
        resumeIconImage = new Image(directory.getEntry("resume_icon", Texture.class));
        resumeButtonImage = new Image(directory.getEntry("resume_button", Texture.class));
        switch1 = new Image(directory.getEntry("switch1", Texture.class));
        switch2 = new Image(directory.getEntry("switch2", Texture.class));
        keyInventory = new Image(directory.getEntry("key-inventory", Texture.class));
        catKeyInventory = new Image(directory.getEntry("key-inventory", Texture.class));
        LabelStyle style = new LabelStyle(displayFont, Color.BLACK);
        keyCount = new Label("x0", style);
        p2KeyCount = new Label("x0", style);

        final float followIconScale = 0.4f;
        catFollowIconImage.setSize(catFollowIconImage.getWidth() * followIconScale,
                catFollowIconImage.getHeight() * followIconScale);
        octopusFollowIconImage.setSize(octopusFollowIconImage.getWidth() * followIconScale,
                octopusFollowIconImage.getHeight() * followIconScale);

        final float inkTextScale = 0.4f;
        inkTextImage.setSize(inkTextImage.getWidth() * inkTextScale, inkTextImage.getHeight()
                * inkTextScale);

        Drawable dividerDrawable = skin.newDrawable("white", new Color(0.0f, 0.0f, 0.0f, 0.9f));
        screenDivider = new Image(dividerDrawable);
        screenDivider.setSize(3, Gdx.graphics.getHeight());
        screenDivider.setPosition(Gdx.graphics.getWidth() / 2f - 1.5f, 0);
        screenDivider.setVisible(GameSettings.getInstance().isCoopEnabled() && level
                .isOctopusPresent() && level.isCatPresent());

    }

    /**
     * Places each Actor at the right position.
     */
    private void setupStageLayout(GameLevel level) {
        viewport = new ScreenViewport();
        float graphicsWidth = Gdx.graphics.getWidth();
        stage = new Stage(viewport);
        rootTable = new Table();
        rootTable.setFillParent(true);

        stage.addActor(minimap);

        Table bottomLeftTable = new Table();
        bottomLeftTable.bottom().left();
        bottomLeftTable.setFillParent(true);
        bottomLeftTable.setDebug(debug);
        stage.addActor(bottomLeftTable);

        if (GameSettings.getInstance().isCoopEnabled() && level.isOctopusPresent() && level
                .isCatPresent()) {
            Table bottomRightTable = new Table();
            bottomRightTable.bottom().right();
            bottomRightTable.setFillParent(true);
            bottomRightTable.setDebug(debug);
            stage.addActor(bottomRightTable);

            Group octopusGroup = new Group();
            octopusGroup.addActor(octopusIconImage);
            if (inkMeter != null) {
                inkMeter.setPosition(catIconImage.getWidth() * 1.4f, 0);
                octopusGroup.addActor(inkMeter);
                inkTextImage.setPosition(catIconImage.getWidth() * 1.4f, catIconImage.getHeight()
                        * 0.35f);
                octopusGroup.addActor(inkTextImage);
            }
            bottomRightTable.add(octopusGroup).padBottom(30).padRight(graphicsWidth / 2f - 30);
        }

        Group avatarGroup = new Group();
        avatarGroup.addActor(catIconImage);

        if ((!GameSettings.getInstance().isCoopEnabled() && (level.isOctopusPresent() || level
                .isCatPresent()))
                || (GameSettings.getInstance().isCoopEnabled() && level.isOctopusPresent() && !level
                        .isCatPresent())
                || (GameSettings.getInstance().isCoopEnabled() && !level.isOctopusPresent() && level
                        .isCatPresent())) {
            avatarGroup.addActor(octopusIconImage);
            catFollowIconImage.setPosition(catIconImage.getWidth() * 0.85f, catIconImage.getHeight()
                    * 0.85f);
            octopusFollowIconImage.setPosition(catIconImage.getWidth() * 0.85f, catIconImage
                    .getHeight() * 0.85f);
            avatarGroup.addActor(catFollowIconImage);
            avatarGroup.addActor(octopusFollowIconImage);

            if (inkMeter != null) {
                inkMeter.setPosition(catIconImage.getWidth() * 1.4f, 0);
                avatarGroup.addActor(inkMeter);
                inkTextImage.setPosition(catIconImage.getWidth() * 1.4f, catIconImage.getHeight()
                        * 0.35f);
                avatarGroup.addActor(inkTextImage);
            }
        }

        bottomLeftTable.add(avatarGroup).pad(30);

        Table topRightTable = new Table();
        topRightTable.setFillParent(true);
        topRightTable.setDebug(debug);
        topRightTable.top().right();
        topRightTable.add(keyInventory);

        Stack inventory = new Stack();
        inventory.add(keyInventory);
        Table inventoryTable = new Table();
        inventoryTable.right();
        if (GameSettings.getInstance().isCoopEnabled() && level.isOctopusPresent() && level
                .isCatPresent()) {
            inventoryTable.add(p2KeyCount).padRight(15f);
        } else {
            inventoryTable.add(keyCount).padRight(15f);
        }
        inventory.add(inventoryTable);
        topRightTable.add(inventory);

        if (!GameSettings.getInstance().isCoopEnabled()) {
            Stack avatarSwitch = new Stack();
            switch1.setVisible(false);
            switch2.setVisible(false);
            avatarSwitch.add(switch1);
            avatarSwitch.add(switch2);
            topRightTable.add(avatarSwitch).pad(10f);
        }

        Stack pauseStack = new Stack();
        pauseStack.add(pauseIconImage);
        pauseStack.add(resumeIconImage);
        topRightTable.add(pauseStack).height(switch1.getHeight()).width(switch1.getHeight()).pad(
                10f);
        stage.addActor(topRightTable);

        Table leftTable = new Table();
        leftTable.setFillParent(true);
        leftTable.setDebug(debug);
        Group dangerIcons = new Group();
        dangerIcons.addActor(smallCatIconImage);
        dangerIcons.addActor(smallOctopusIconImage);
        dangerIconImage.moveBy(50, 30);
        dangerIcons.addActor(dangerIconImage);
        leftTable.left().add(dangerIcons).width(100).height(100);
        stage.addActor(leftTable);

        if (GameSettings.getInstance().isCoopEnabled() && level.isOctopusPresent() && level
                .isCatPresent()) {
            Table topLeftTable = new Table();
            topLeftTable.setFillParent(true);
            topLeftTable.setDebug(debug);
            topLeftTable.top().left();
            Stack inventoryCat = new Stack();
            inventoryCat.add(catKeyInventory);
            Table inventoryCatTable = new Table();
            inventoryCatTable.right();
            inventoryCatTable.add(keyCount).padRight(15f);
            inventoryCat.add(inventoryCatTable);
            topLeftTable.add(inventoryCat).padLeft(graphicsWidth / 2f - 130).padTop(30);
            stage.addActor(topLeftTable);
            stage.addActor(screenDivider);
        }
        setUpPauseMenu();

        meowCooldownIndicator = new MeowCooldownIndicator(displayFont);
        if (!GameSettings.getInstance().isCoopEnabled()) {
            Table bottomRightTable = new Table();
            bottomRightTable.setFillParent(true);
            bottomRightTable.setDebug(debug);
            bottomRightTable.bottom().right();
            bottomRightTable.add(meowCooldownIndicator).padBottom(40).padRight(200);
            stage.addActor(bottomRightTable);
        } else if (GameSettings.getInstance().isCoopEnabled() && level.isCatPresent()) {
            float meowCooldownIndicatorCOOPXPosition = level.isCatPresent() && !level
                    .isOctopusPresent() ? viewport.getScreenWidth() - 200 : graphicsWidth / 2f
                            - 200;
            meowCooldownIndicator.setPosition(meowCooldownIndicatorCOOPXPosition, 40);
            stage.addActor(meowCooldownIndicator);
        }
    }

    private void setUpPauseMenu() {
        if (pauseIconImage != null) {
            pauseIconImage.setVisible(true);
            pauseIconImage.addListener(new ClickListener() {

                @Override
                public void clicked(InputEvent event, float x, float y) {
                    togglePauseMenu(true);
                }
            });
            pauseIconImage.addListener(new InputListener() {

            });
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
        pauseMenuTable.setPosition((Gdx.graphics.getWidth() - 300) / 2, (Gdx.graphics.getHeight()
                - 200) / 2);
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
            resumeButtonImage.setSize(resumeButtonImage.getWidth() * 0.25f, resumeButtonImage
                    .getHeight() * 0.25f);
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
            pauseBannerImage.setSize(pauseBannerImage.getWidth() * scale, pauseBannerImage
                    .getHeight() * scale);
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
    }
}
