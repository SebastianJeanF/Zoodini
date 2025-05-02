package walknroll.zoodini.controllers.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.SpriteSheet;
import edu.cornell.gdiac.util.ScreenListener;
import walknroll.zoodini.GDXRoot;

public class StoryboardScene implements Screen {
    private static SpriteSheet STORYBOARD;

    public static void setSpriteSheet(SpriteSheet spritesheet) {
        StoryboardScene.STORYBOARD = spritesheet;
    }

    public static boolean isLoaded() {
        return StoryboardScene.STORYBOARD != null;
    }

    private ScreenListener listener;
    @SuppressWarnings("unused")
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

    public StoryboardScene(SpriteBatch batch, AssetDirectory assets, int selectedLevel) {
        this.batch = batch;
        this.assets = assets;
        this.selectedLevel = selectedLevel;
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        if (!StoryboardScene.isLoaded()) {
            throw new RuntimeException(
                    "Tried to instantiate a StoryboardScene without loading the Storyboard spritesheet");
        }
        StoryboardScene.STORYBOARD.setFrame(0);
    }

    public void create() {
        stage = new Stage(new ScreenViewport(camera));
        Gdx.input.setInputProcessor(stage);

        skin = new Skin(Gdx.files.internal("uiskins/orange/uiskin.json"));

        Table table = makeStoryboardTable();

        stage.addActor(table);
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
        float ratio = (float) width / (float) STORYBOARD.getRegionWidth();
        batch.draw(STORYBOARD, 0, 0, width, ratio * STORYBOARD.getRegionHeight());
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

    public int getSelectedLevel() {
        return this.selectedLevel;
    }

    @Override
    public void show() {
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'show'");
    }

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

    private Table makeStoryboardTable() {
        Table table = new Table();
        table.setFillParent(true);
        table.defaults().spaceRight(10f);
        table.top().pad(Value.percentWidth(0.01f));

        Value controlWidth = Value.percentWidth(0.1f, table);

        TextButton nextButton = new TextButton("Next", skin);
        nextButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int nextFrame = StoryboardScene.STORYBOARD.getFrame() + 1;
                if (nextFrame >= StoryboardScene.STORYBOARD.getSize()) {
                    StoryboardScene.this.listener.exitScreen(StoryboardScene.this, GDXRoot.EXIT_PLAY);
                } else {
                    StoryboardScene.STORYBOARD.setFrame(StoryboardScene.STORYBOARD.getFrame() + 1);
                }
            }

        });
        table.add(nextButton).right().bottom().width(controlWidth).expand();

        // table.row();
        TextButton skipButton = new TextButton("Skip", skin);
        skipButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                StoryboardScene.this.listener.exitScreen(StoryboardScene.this, GDXRoot.EXIT_PLAY);
            }

        });
        table.add(skipButton).right().bottom().width(controlWidth);

        return table;
    }
}
