package walknroll.zoodini.controllers.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
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
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.util.ScreenListener;
import walknroll.zoodini.GDXRoot;
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

    private Stage stage;
    private Skin skin;

    private boolean waitingForAbilityKey;
    private boolean waitingForSwapKey;
    private boolean waitingForFollowKey;

    private GameSettings settings;
    private boolean resetState;

    public SettingsScene(SpriteBatch batch, AssetDirectory assets, GameSettings currentSettings) {
        this.batch = batch;
        this.assets = assets;
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        this.resetState = false;
        this.settings = currentSettings;
    }

    public boolean shouldResetState() {
        return resetState;
    }

    public void create() {
        stage = new Stage(new ScreenViewport(camera));
        Gdx.input.setInputProcessor(stage);

        skin = new Skin(Gdx.files.internal("uiskins/orange/uiskin.json"));

        Window window = new Window("Edit Keybinds", skin, "maroon");
        Container<Window> windowContainer = makeKeybindsContainer(window);

        Table table = makeSettingsTable(window);

        stage.addActor(table);
        stage.addActor(windowContainer);
    }

    public GameSettings getSettings() {
        return this.settings;
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

    private Container<Window> makeKeybindsContainer(Window window) {
        Container<Window> container = new Container<>(window);
        container.setFillParent(true);

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

        window.add(new Label("Use Ability", skin, "title")).width(Value.percentWidth(0.25f, container))
                .pad(Value.percentWidth(0.01f, container));
        TextButton setAbilityKey = new TextButton("Current: " + Input.Keys.toString(this.settings.getAbilityKey()),
                skin);
        setAbilityKey.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                SettingsScene.this.waitingForAbilityKey = true;
                SettingsScene.this.waitingForSwapKey = false;
                SettingsScene.this.waitingForFollowKey = false;
            }
        });
        window.add(setAbilityKey).width(Value.percentWidth(0.2f, container))
                .height(Value.percentHeight(0.06f, container)).pad(Value.percentWidth(0.01f, container));

        window.row();
        window.add(new Label("Swap Character", skin, "title")).width(Value.percentWidth(0.25f, container))
                .pad(Value.percentWidth(0.01f, container));
        TextButton setSwapKey = new TextButton("Current: " + Input.Keys.toString(this.settings.getSwapKey()), skin);
        setSwapKey.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                SettingsScene.this.waitingForSwapKey = true;
                SettingsScene.this.waitingForAbilityKey = false;
                SettingsScene.this.waitingForFollowKey = false;
            }
        });
        window.add(setSwapKey).width(Value.percentWidth(0.2f, container)).height(Value.percentHeight(0.06f, container))
                .pad(Value.percentWidth(0.01f, container));

        window.row();
        window.add(new Label("Toggle Following", skin, "title")).width(Value.percentWidth(0.25f, container))
                .pad(Value.percentWidth(0.01f, container));
        TextButton setFollowButton = new TextButton("Current: " + Input.Keys.toString(this.settings.getFollowKey()), skin);
        setFollowButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                SettingsScene.this.waitingForFollowKey = true;
                SettingsScene.this.waitingForAbilityKey = false;
                SettingsScene.this.waitingForSwapKey = false;
            }
        });
        window.add(setFollowButton).width(Value.percentWidth(0.2f, container)).height(Value.percentHeight(0.06f, container))
                .pad(Value.percentWidth(0.01f, container));

        stage.addListener(new InputListener() {
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    return false;
                }
                if (SettingsScene.this.waitingForAbilityKey) {
                    setAbilityKey.setText("Current: " + Input.Keys.toString(keycode));
                    SettingsScene.this.settings.setAbilityKey(keycode);
                    SettingsScene.this.waitingForAbilityKey = false;
                }
                if (SettingsScene.this.waitingForSwapKey) {
                    setSwapKey.setText("Current: " + Input.Keys.toString(keycode));
                    SettingsScene.this.settings.setSwapKey(keycode);
                    SettingsScene.this.waitingForSwapKey = false;
                }
                if (SettingsScene.this.waitingForFollowKey) {
                    setFollowButton.setText("Current: " + Input.Keys.toString(keycode));
                    SettingsScene.this.settings.setFollowKey(keycode);
                    SettingsScene.this.waitingForFollowKey = false;
                }
                return true;
            }
        });

        window.row();
        window.add(new Label("Change a keybind by clicking its respective button, then typing the new key", skin))
                .colspan(2);

        window.center();
        return container;
    }

    private Table makeSettingsTable(Window window) {
        Table table = new Table();
        // table.setSize(this.width, this.height);
        table.setFillParent(true);
        table.defaults().spaceBottom(10f);
        table.top().pad(Value.percentWidth(0.01f)).padTop(Value.percentHeight(0.3f));

        Value labelWidth = Value.percentWidth(0.25f, table);
        Value controlWidth = Value.percentWidth(0.5f, table);

        table.add(new Label("Music Volume", skin, "title")).left().width(labelWidth);
        Slider musicVolumeSlider = new Slider(0f, 100f, 1f, false, skin);
        musicVolumeSlider.setValue(settings.getMusicVolume());
        musicVolumeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                SettingsScene.this.settings.setMusicVolume(musicVolumeSlider.getValue());
            }
        });
        table.add(musicVolumeSlider).left().width(controlWidth).expandX();

        table.row();
        table.add(new Label("Sound Effect Volume", skin, "title")).left().width(labelWidth);
        Slider soundVolumeSlider = new Slider(0f, 100f, 1f, false, skin);
        soundVolumeSlider.setValue(settings.getSoundVolume());
        soundVolumeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                SettingsScene.this.settings.setSoundVolume(soundVolumeSlider.getValue());
            }
        });
        table.add(soundVolumeSlider).left().width(controlWidth);

        table.row();
        table.add(new Label("Resolution", skin, "title")).left().width(labelWidth);
        SelectBox<String> resolutionSelect = new SelectBox<>(skin);
        resolutionSelect.setItems("1280x720", "1920x1080", "Fullscreen");
        resolutionSelect.setSelected(this.settings.getResolution());
        resolutionSelect.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                SettingsScene.this.settings.setResolution(resolutionSelect.getSelected());
            }
        });
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
        table.add(new Label("Reset Game State", skin, "title")).left().width(labelWidth);
        TextButton resetGameState = new TextButton("Reset", skin);
        resetGameState.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                SettingsScene.this.resetState = true;
            }
        });
        table.add(resetGameState).left().width(controlWidth);

        table.row();
        TextButton menuReturn = new TextButton("Back to Menu", skin);
        menuReturn.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                listener.exitScreen(SettingsScene.this, GDXRoot.EXIT_MENU);
            }
        });
        table.add(menuReturn).left().width(labelWidth).expandY().bottom();
        return table;
    }
}
