package walknroll.zoodini.controllers.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.util.ScreenListener;
import walknroll.zoodini.GDXRoot;
import walknroll.zoodini.utils.Constants;
import walknroll.zoodini.utils.FreeTypeSkin;
import walknroll.zoodini.utils.LevelPortal;

public class LevelSelectScene implements Screen {

    private Array<Integer> availableLevels;

    private ScreenListener listener;

    /** The drawing camera for this scene */
    private OrthographicCamera camera;
    /** Reference to sprite batch created by the root */
    private SpriteBatch batch;

    private int width;
    @SuppressWarnings("unused")
    private int height;

    private Stage stage;
    private Skin levelSelectSkin;
    private Skin normalButtonSkin;

    private int selectedLevel;
    private int highestClearance;

    /** Background image */
    private Texture background;
    Affine2 cache = new Affine2();

    private float resScale;
    private Texture logo;

    public LevelSelectScene(SpriteBatch batch, AssetDirectory assets,
            Array<Integer> availableLevels,
            int highestClearance) {
        this.batch = batch;
        this.availableLevels = availableLevels;
        this.highestClearance = highestClearance;
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        this.background = assets.getEntry("splash", Texture.class);

        this.logo = assets.getEntry("level-select-text", Texture.class);
        JsonValue constants = assets.getEntry("constants", JsonValue.class);
        float resScaleX = Gdx.graphics.getWidth() / (float) constants.getFloat("screenWidth");
        float resScaleY = Gdx.graphics.getHeight() / (float) constants.getFloat("screenHeight");
        this.resScale = Math.min(resScaleX, resScaleY);
    }

    public void create() {
        stage = new Stage(new ScreenViewport(camera));
        Gdx.input.setInputProcessor(stage);

        levelSelectSkin = new Skin(Gdx.files.internal("uiskins/levelselect/uiskin.json"));
        normalButtonSkin = new FreeTypeSkin(Gdx.files.internal("uiskins/zoodini/uiskin.json"));

        Table table = makeLevelSelectTable();
        stage.addActor(table);
    }

    public int getSelectedLevel() {
        return selectedLevel;
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        if (camera == null) {
            camera = new OrthographicCamera(width, height);
        } else {
            camera.setToOrtho(false, width, height);
        }
        if (stage != null) {
            stage.getViewport().update(width, height);
        }
    }

    public void render(float delta) {
        stage.act(delta);

        batch.begin(camera);
        batch.setColor(Color.WHITE);
        float scaleX = (float) width / (float) background.getWidth();
        float scaleY = (float) height / (float) background.getHeight();
        cache.idt();
        cache.scale(scaleX, scaleY);
        batch.draw(background, cache);

        batch.draw(logo,
                ((width / 2f) - (logo.getWidth() / 2f) * resScale),
                (height - (logo.getHeight() + 50) * resScale),
                logo.getWidth() * resScale,
                logo.getHeight() * resScale
        );
        batch.end();

        stage.draw();
    }

    public void dispose() {
        levelSelectSkin.dispose();
        normalButtonSkin.dispose();
        stage.dispose();
    }

    /**
     * Sets the ScreenListener for this mode
     *
     * The ScreenListener will respond to requests to quit.
     */
    public void setScreenListener(ScreenListener listener) {
        this.listener = listener;
    }

    @Override
    public void show() {
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'show'");
    }

    // @Override
    // public boolean keyDown(int keycode) {
    // return true;
    // })

    @Override
    public void pause() {
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'pause'");
    }

    @Override
    public void resume() {
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'resume'");
    }

    @Override
    public void hide() {
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'hide'");
    }

    private Table makeLevelSelectTable() {
        Table rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.pad(Value.percentWidth(0.02f)).padTop(Value.percentHeight(0.3f));

        Table table = new Table();
        table.setFillParent(false);
        table.defaults().spaceRight(10f).spaceBottom(10f);

        for (int i = 0; i < this.availableLevels.size; i++) {
            int levelKey = this.availableLevels.get(i);
            Stack portalStack = new Stack();
            boolean levelOpen = Constants.DEBUG | Constants.UNLOCK_ALL | highestClearance
                    >= levelKey;
            boolean levelCompleted = Constants.DEBUG | Constants.UNLOCK_ALL | levelKey
                    < highestClearance;
            ImageButton levelButton = new ImageButton(new LevelPortal(levelOpen, false,
                    levelCompleted),
                    new LevelPortal(levelOpen, true && levelOpen, levelCompleted));
            if (levelOpen) {
                levelButton.addListener(new ChangeListener() {

                    public void changed(ChangeEvent event, Actor actor) {
                        LevelSelectScene.this.selectedLevel = levelKey;
                        LevelSelectScene.this.listener.exitScreen(LevelSelectScene.this,
                                GDXRoot.EXIT_STORYBOARD);
                    }
                });
            }
            portalStack.add(levelButton);
            Container<Label> labelContainer = new Container<>(new Label(String.valueOf(levelKey),
                    levelSelectSkin));
            labelContainer.setFillParent(true);
            labelContainer.setTouchable(Touchable.disabled);
            portalStack.add(labelContainer);
            table.add(portalStack);

            if ((i + 1) % 6 == 0) {
                table.row();
            }
        }

        ScrollPane levelsScroller = new ScrollPane(table, normalButtonSkin);
        levelsScroller.setFadeScrollBars(false);
        rootTable.add(levelsScroller).height(Value.percentHeight(0.55f,
                rootTable)).width(Value.percentWidth(0.8f, rootTable)).top().expand();
        stage.setScrollFocus(levelsScroller);

        rootTable.row();
        TextButton menuReturn = new TextButton("Back to Menu", normalButtonSkin);
        menuReturn.addListener(new ChangeListener() {

            public void changed(ChangeEvent event, Actor actor) {
                listener.exitScreen(LevelSelectScene.this, GDXRoot.EXIT_MENU);
            }
        });
        rootTable.add(menuReturn).bottom().left();
        return rootTable;
    }
}
