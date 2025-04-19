package walknroll.zoodini.controllers.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.ScreenUtils;

import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.TextLayout;
import edu.cornell.gdiac.util.ScreenListener;

public class SettingsScene implements Screen {
    private ScreenListener listener;
    private SpriteBatch batch;

    private TextLayout todoMessage;

    private Stage stage;
    private Table table;
    private TextureAtlas atlas;
    private Skin skin;

    public void create() {
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        table.setDebug(true); // This is optional, but enables debug lines for tables.

        // atlas = new TextureAtlas(Gdx.files.internal("uiskin/uiskin.atlas"));
        skin = new Skin(Gdx.files.internal("uiskin/uiskin.json"));
        // Add widgets here

        Label nameLabel = new Label("Name:", skin);
        TextField nameText = new TextField("", skin);
        Label addressLabel = new Label("Address:", skin);
        TextField addressText = new TextField("", skin);
        
        Table table = new Table();
        table.add(nameLabel);
        table.add(nameText).width(100);
        table.row();
        table.add(addressLabel);
        table.add(addressText).width(100);
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void render() {
        // Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    public void dispose() {
        stage.dispose();
    }

    public SettingsScene(SpriteBatch batch) {
        this.batch = batch;

        // todoMessage = new TextLayout("todo lol", );
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
    public void render(float delta) {
        ScreenUtils.clear(0.702f, 0.1255f, 0.145f, 1.0f);
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'render'");
    }

    // @Override
    // public void resize(int width, int height) {
    // // TODO Auto-generated method stub
    // // throw new UnsupportedOperationException("Unimplemented method 'resize'");
    // }

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

    // @Override
    // public void dispose() {
    // // TODO Auto-generated method stub
    // throw new UnsupportedOperationException("Unimplemented method 'dispose'");
    // }

}
