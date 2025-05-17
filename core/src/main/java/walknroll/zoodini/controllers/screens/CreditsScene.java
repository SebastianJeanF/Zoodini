package walknroll.zoodini.controllers.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.util.ScreenListener;
import walknroll.zoodini.GDXRoot;
import walknroll.zoodini.utils.FreeTypeSkin;

public class CreditsScene implements Screen {

    private ScreenListener listener;

    private AssetDirectory assets;

    /** The drawing camera for this scene */
    private OrthographicCamera camera;
    /** Reference to sprite batch created by the root */
    private SpriteBatch batch;

    private int width;
    private int height;

    private Stage stage;
    private Skin skin;

    Affine2 cache = new Affine2();

    /** Background image */
    private Texture background;

    /** logo */
    private Texture logo;
    private float resScale;

    public CreditsScene(SpriteBatch batch, AssetDirectory assets) {
        this.batch = batch;
        this.assets = assets;
        this.background = assets.getEntry("splash", Texture.class);
        this.logo = assets.getEntry("logo", Texture.class);
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        JsonValue constants = assets.getEntry("constants", JsonValue.class);
        float resScaleX = Gdx.graphics.getWidth() / (float) constants.getFloat("screenWidth");
        float resScaleY = Gdx.graphics.getHeight() / (float) constants.getFloat("screenHeight");
        this.resScale = Math.min(resScaleX, resScaleY);
    }

    public void create() {
        stage = new Stage(new ScreenViewport(camera));
        Gdx.input.setInputProcessor(stage);

        skin = new FreeTypeSkin(Gdx.files.internal("uiskins/zoodini/uiskin.json"));

        Table table = makeCreditsTable();

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
        stage.dispose();
        skin.dispose();
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

    private Table makeCreditsTable() {
        Table table = new Table();
        // table.setSize(this.width, this.height);
        table.setFillParent(true);
        table.defaults().spaceBottom(10f).spaceRight(5f);
        table.top().pad(Value.percentWidth(0.02f)).padTop(Value.percentHeight(0.3f));

        Value labelWidth = Value.percentWidth(0.2f, table);
        Value controlWidth = Value.percentWidth(0.3f, table);

        VerticalGroup group = new VerticalGroup();

        group.addActor(new Label("Project Lead", skin, "gold"));
        group.addActor(new Label("Sebastian Jean-Francois", skin));
        group.addActor(new Label("Software Lead", skin, "gold"));
        group.addActor(new Label("Bill Park", skin));
        group.addActor(new Label("Design Lead", skin, "gold"));
        group.addActor(new Label("Lina Liu", skin));
        group.addActor(new Label("Designer & Programmer", skin, "gold"));
        group.addActor(new Label("Grace Jin", skin));
        group.addActor(new Label("Programmer", skin, "gold"));
        group.addActor(new Label("Nick Regennitter", skin));
        group.addActor(new Label("Programmer", skin, "gold"));
        group.addActor(new Label("Andrew Cheung", skin));
        group.addActor(new Label("Programmer", skin, "gold"));
        group.addActor(new Label("Abdul Raafai Asim", skin));
        group.addActor(new Label("Programmer", skin, "gold"));
        group.addActor(new Label("James Tu", skin));
        group.addActor(new Label("librayr", skin, "gold"));
        group.addActor(new Label("libgdx", skin));

        table.add(new ScrollPane(group, skin)).expandX().width(Value.percentWidth(0.8f, table));
        table.row();
        TextButton menuReturn = new TextButton("Back to Menu", skin);
        menuReturn.addListener(new ChangeListener() {

            public void changed(ChangeEvent event, Actor actor) {
                listener.exitScreen(CreditsScene.this, GDXRoot.EXIT_MENU);
            }
        });
        table.add(menuReturn).left().width(labelWidth).expandY().bottom();
        return table;
    }
}
