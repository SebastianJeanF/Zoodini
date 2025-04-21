package walknroll.zoodini.controllers.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.TextLayout;
import edu.cornell.gdiac.util.ScreenListener;
import walknroll.zoodini.GDXRoot;
import walknroll.zoodini.controllers.InputController;
import walknroll.zoodini.utils.Constants;
import walknroll.zoodini.utils.GameSettings;

public class SettingsScene implements Screen {
    private ScreenListener listener;

    private AssetDirectory assets;

    /** The drawing camera for this scene */
    private OrthographicCamera camera;
    /** Reference to sprite batch created by the root */
    private SpriteBatch batch;

    private int width;
    private int height;

    private TextLayout todoMessage;

    private Stage stage;
    private Table table;
    private TextureAtlas atlas;
    private Skin skin;

    private boolean waitingForAbilityKey;
    private boolean waitingForSwapKey;

    private int abilityKey;
    private int swapKey;

    public void create() {
        stage = new Stage(new ScreenViewport(camera));
        Gdx.input.setInputProcessor(stage);

        // table = new Table();
        // table.setFillParent(true);
        // stage.addActor(table);

        // tables.

        // atlas = new TextureAtlas(Gdx.files.internal("uiskin/uiskin.atlas"));
        skin = new Skin(Gdx.files.internal("uiskin/uiskin.json"));
        // Add widgets here

        Window window = new Window("Edit Keybinds", skin, "maroon");
        Container<Window> container = new Container<>(window);
        container.setFillParent(true);

        // window.setSize(this.width / 2, this.height / 2);
        window.setModal(true);
        window.setMovable(false);
        window.setVisible(false);

        window.defaults().spaceBottom(10f);

        TextButton closeWindow = new TextButton("Done", skin, "maroon");
        closeWindow.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                window.setVisible(false);
            }
        });
        window.getTitleTable().add(closeWindow).height(window.getPadTop());
        window.row().fill().expandX();
        // window;

        window.add(new Label("Use Ability", skin, "title")).width(Value.percentWidth(0.25f, container))
                .pad(Value.percentWidth(0.01f, container));
        TextButton setAbilityKey = new TextButton("Current: " + Input.Keys.toString(abilityKey), skin);
        setAbilityKey.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                SettingsScene.this.waitingForAbilityKey = true;
                SettingsScene.this.waitingForSwapKey = false;
            }
        });
        window.add(setAbilityKey).width(Value.percentWidth(0.2f, container))
                .height(Value.percentHeight(0.06f, container)).pad(Value.percentWidth(0.01f, container));

        window.row();
        window.add(new Label("Swap Character", skin, "title")).width(Value.percentWidth(0.25f, container))
                .pad(Value.percentWidth(0.01f, container));
        TextButton setSwapKey = new TextButton("Current: " + Input.Keys.toString(swapKey), skin);
        setSwapKey.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                SettingsScene.this.waitingForSwapKey = true;
                SettingsScene.this.waitingForAbilityKey = false;
            }
        });
        window.add(setSwapKey).width(Value.percentWidth(0.2f, container)).height(Value.percentHeight(0.06f, container))
                .pad(Value.percentWidth(0.01f, container));

        stage.addListener(new InputListener() {
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    return false;
                }
                if (SettingsScene.this.waitingForAbilityKey) {
                    setAbilityKey.setText("Current: " + Input.Keys.toString(keycode));
                    SettingsScene.this.abilityKey = keycode;
                    SettingsScene.this.waitingForAbilityKey = false;
                }
                if (SettingsScene.this.waitingForSwapKey) {
                    setSwapKey.setText("Current: " + Input.Keys.toString(keycode));
                    SettingsScene.this.swapKey = keycode;
                    SettingsScene.this.waitingForSwapKey = false;
                }
                return true;
            }
        });

        // window.pack();
        window.center();
        // window.setPosition(this.width / 2f - window.getWidth() / 2f,
        // this.height / 2f - window.getHeight() / 2f);

        Table table = new Table();
        // table.setSize(this.width, this.height);
        table.setFillParent(true);
        table.defaults().spaceBottom(10f);
        table.top().pad(Value.percentWidth(0.01f)).padTop(Value.percentHeight(0.3f));
        // table.setDebug(true); // This is optional, but enables debug lines for

        Value labelWidth = Value.percentWidth(0.25f, table);
        Value controlWidth = Value.percentWidth(0.5f, table);

        table.add(new Label("Volume", skin, "title")).left().width(labelWidth);
        Slider volumeSlider = new Slider(0f, 100f, 1f, false, skin);
        volumeSlider.setValue(100f);
        table.add(volumeSlider).left().width(controlWidth).expandX();

        table.row();
        table.add(new Label("Resolution", skin, "title")).left().width(labelWidth);
        SelectBox<String> resolutionSelect = new SelectBox<>(skin);
        resolutionSelect.setItems("1280x720", "1920x1080", "Fullscreen");
        table.add(resolutionSelect).left().width(controlWidth);

        table.row();
        table.add(new Label("Update Keybinds", skin, "title")).left().width(labelWidth);
        TextButton keybindsDialogOpen = new TextButton("Open Keybind Editor", skin);
        keybindsDialogOpen.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                window.setVisible(true);
            }
        });
        table.add(keybindsDialogOpen).left().width(controlWidth);

        table.row();
        TextButton menuReturn = new TextButton("Back to Menu", skin);
        menuReturn.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                listener.exitScreen(SettingsScene.this, GDXRoot.EXIT_MENU);
            }
        });
        table.add(menuReturn).left().width(labelWidth).expandY().bottom();

        stage.addActor(table);
        stage.addActor(container);
    }

    public GameSettings getSettings() {
        return new GameSettings(this.abilityKey, this.swapKey);
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
        // Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
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
    }

    public SettingsScene(SpriteBatch batch, AssetDirectory assets, GameSettings currentSettings) {
        this.batch = batch;
        this.assets = assets;
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        this.abilityKey = currentSettings.getAbilityKey();
        this.swapKey = currentSettings.getSwapKey();
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

    // @Override
    // public boolean keyDown(int keycode) {
    // return true;
    // })

    @Override
    public void show() {
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'show'");
    }

    // @Override
    // public void render(float delta) {
    // ScreenUtils.clear(0.702f, 0.1255f, 0.145f, 1.0f);
    // // TODO Auto-generated method stub
    // // throw new UnsupportedOperationException("Unimplemented method 'render'");
    // }

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
