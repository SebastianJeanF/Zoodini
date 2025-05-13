package walknroll.zoodini.controllers.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.util.ScreenListener;
import walknroll.zoodini.GDXRoot;
import walknroll.zoodini.utils.GameSettings;

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

    public CreditsScene(SpriteBatch batch, AssetDirectory assets) {
        this.batch = batch;
        this.assets = assets;
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void create() {
        stage = new Stage(new ScreenViewport(camera));
        Gdx.input.setInputProcessor(stage);

        skin = new Skin(Gdx.files.internal("uiskins/orange/uiskin.json"));

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
        Texture texture = assets.getEntry("splash", Texture.class);
        float ratio = (float) width / (float) texture.getWidth();
        batch.draw(texture, 0, 0, width, ratio * texture.getHeight());

        texture = assets.getEntry("logo", Texture.class);
        batch.draw(texture, (width / 2f) - (texture.getWidth() / 2f), height - (texture.getHeight() + 50),
                texture.getWidth(),
                texture.getHeight());
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
        table.top().pad(Value.percentWidth(0.01f)).padTop(Value.percentHeight(0.3f));

        Value labelWidth = Value.percentWidth(0.2f, table);
        Value controlWidth = Value.percentWidth(0.25f, table);

        table.add(new Label("Project Lead", skin, "title")).left().minWidth(labelWidth);
        table.add(new Label("Sebastian Jean-Francois", skin, "white")).left().width(controlWidth);

        table.add(new Label("librayr", skin, "title")).left().minWidth(labelWidth);
        table.add(new Label("libgdx", skin, "white")).left().width(controlWidth).expandX();

        table.row();
        table.add(new Label("Software Lead", skin, "title")).left().minWidth(labelWidth);
        table.add(new Label("Bill Park", skin, "white")).left().width(controlWidth);

        table.row();
        table.add(new Label("Design Lead", skin, "title")).left().minWidth(labelWidth);
        table.add(new Label("Lina Liu", skin, "white")).left().width(controlWidth);

        table.row();
        table.add(new Label("Designer & Programmer", skin, "title")).left().minWidth(labelWidth);
        table.add(new Label("Grace Jin", skin, "white")).left().width(controlWidth);

        table.row();
        table.add(new Label("Programmer", skin, "title")).left().minWidth(labelWidth);
        table.add(new Label("Nick Regennitter", skin, "white")).left().width(controlWidth);

        table.row();
        table.add(new Label("Programmer", skin, "title")).left().minWidth(labelWidth);
        table.add(new Label("Andrew Cheung", skin, "white")).left().width(controlWidth);

        table.row();
        table.add(new Label("Programmer", skin, "title")).left().minWidth(labelWidth);
        table.add(new Label("Abdul Raafai Asim", skin, "white")).left().width(controlWidth);

        table.row();
        table.add(new Label("Programmer", skin, "title")).left().minWidth(labelWidth);
        table.add(new Label("James Tu", skin, "white")).left().width(controlWidth);

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
