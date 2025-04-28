package walknroll.zoodini.controllers.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.util.ScreenListener;
import walknroll.zoodini.GDXRoot;
import walknroll.zoodini.utils.Constants;
import walknroll.zoodini.utils.LevelPortal;

public class LevelSelectScene implements Screen {
    private Array<Integer> availableLevels;

    private ScreenListener listener;

    private AssetDirectory assets;

    /** The drawing camera for this scene */
    private OrthographicCamera camera;
    /** Reference to sprite batch created by the root */
    private SpriteBatch batch;

    private int width;
    @SuppressWarnings("unused")
    private int height;

    private Stage stage;
    private Skin skin;

    private int selectedLevel;
    private int highestClearance;

    public LevelSelectScene(SpriteBatch batch, AssetDirectory assets, Array<Integer> availableLevels,
            int highestClearance) {
        this.batch = batch;
        this.assets = assets;
        this.availableLevels = availableLevels;
        this.highestClearance = highestClearance;
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void create() {
        stage = new Stage(new ScreenViewport(camera));
        Gdx.input.setInputProcessor(stage);

        skin = new Skin(Gdx.files.internal("uiskins/levelselect/uiskin.json"));

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
        Texture texture = assets.getEntry("splash", Texture.class);
        float ratio = (float) width / (float) texture.getWidth();
        batch.draw(texture, 0, 0, width, ratio * texture.getHeight());
        batch.end();

        stage.draw();
    }

    public void dispose() {
        skin.dispose();
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
        Table table = new Table();
        // table.setSize(this.width, this.height);
        table.setFillParent(true);
        table.defaults().spaceRight(10f).spaceBottom(10f);
        table.pad(Value.percentWidth(0.01f));
        // table.setDebug(true); // This is optional, but enables debug lines for

        for (int i = 0; i < this.availableLevels.size; i++) {
            int levelKey = this.availableLevels.get(i);
            Stack portalStack = new Stack();
            boolean levelOpen = Constants.DEBUG | highestClearance >= levelKey;
            boolean levelCompleted = Constants.DEBUG | levelKey < highestClearance;
            ImageButton levelButton = new ImageButton(new LevelPortal(levelOpen, false, levelCompleted),
                    new LevelPortal(levelOpen, true && levelOpen, levelCompleted));
            if (levelOpen) {
                levelButton.addListener(new ChangeListener() {
                    public void changed(ChangeEvent event, Actor actor) {
                        LevelSelectScene.this.selectedLevel = levelKey;
                        LevelSelectScene.this.listener.exitScreen(LevelSelectScene.this, GDXRoot.EXIT_STORYBOARD);
                    }
                });
            }
            portalStack.add(levelButton);
            Container<Label> labelContainer = new Container<>(new Label(String.valueOf(levelKey), skin));
            labelContainer.setFillParent(true);
            labelContainer.setTouchable(Touchable.disabled);
            portalStack.add(labelContainer);
            table.add(portalStack);

            if ((i + 1) % 5 == 0) {
                table.row();
            }
        }

        return table;
    }
}
