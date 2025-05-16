package walknroll.zoodini.controllers.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Affine2;
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
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.util.ScreenListener;
import walknroll.zoodini.GDXRoot;
import walknroll.zoodini.utils.GameSettings;
import walknroll.zoodini.utils.FreeTypeSkin;

public class SettingsScene implements Screen {
    private ScreenListener listener;

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
    private GameSettings stagedSettings;
    private boolean resetState;

    /** Background image */
    private Texture background;
    /** logo */
    private Texture logo;

    Affine2 cache = new Affine2();

    public SettingsScene(SpriteBatch batch, AssetDirectory assets, GameSettings currentSettings) {
        this.batch = batch;
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        this.resetState = false;
        this.settings = currentSettings;
        this.stagedSettings = new GameSettings(currentSettings);

        this.background = assets.getEntry("splash", Texture.class);
        this.logo = assets.getEntry("logo", Texture.class);
    }

    public boolean shouldResetState() {
        return resetState;
    }

    public void create() {
        stage = new Stage(new ScreenViewport(camera));
        Gdx.input.setInputProcessor(stage);

        skin = new FreeTypeSkin(Gdx.files.internal("uiskins/zoodini/uiskin.json"));

        Window window = new Window("", skin);
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
        float scaleX = (float) width / (float) background.getWidth();
        float scaleY = (float) height / (float) background.getHeight();
        cache.idt();
        cache.scale(scaleX, scaleY);
        batch.draw(background, cache);

        batch.draw(logo, (width / 2f) - (logo.getWidth() / 2f), height - (logo.getHeight() + 50),
                logo.getWidth(),
                logo.getHeight());
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
        window.pad(Value.percentWidth(0.02f, container));

        // window.getTitleTable().add(closeWindow).height(window.getPadTop());
        window.row().fill().expandX();

        window.add(new Label("Use Ability", skin, "dark")).width(Value.percentWidth(0.25f, container))
                .pad(Value.percentWidth(0.01f, container));
        TextButton setAbilityKey = new TextButton(
                "Current: " + Input.Keys.toString(this.stagedSettings.getAbilityKey()),
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
        window.add(new Label("Swap Character", skin, "dark")).width(Value.percentWidth(0.25f, container))
                .pad(Value.percentWidth(0.01f, container));
        TextButton setSwapKey = new TextButton("Current: " + Input.Keys.toString(this.stagedSettings.getSwapKey()),
                skin);
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
        window.add(new Label("Toggle Following", skin, "dark")).width(Value.percentWidth(0.25f, container))
                .pad(Value.percentWidth(0.01f, container));
        TextButton setFollowButton = new TextButton(
                "Current: " + Input.Keys.toString(this.stagedSettings.getFollowKey()), skin);
        setFollowButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                SettingsScene.this.waitingForFollowKey = true;
                SettingsScene.this.waitingForAbilityKey = false;
                SettingsScene.this.waitingForSwapKey = false;
            }
        });
        window.add(setFollowButton).width(Value.percentWidth(0.2f, container))
                .height(Value.percentHeight(0.06f, container))
                .pad(Value.percentWidth(0.01f, container));

        stage.addListener(new InputListener() {
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    return false;
                }
                if (SettingsScene.this.waitingForAbilityKey) {
                    setAbilityKey.setText("Current: " + Input.Keys.toString(keycode));
                    SettingsScene.this.stagedSettings.setAbilityKey(keycode);
                    SettingsScene.this.waitingForAbilityKey = false;
                }
                if (SettingsScene.this.waitingForSwapKey) {
                    setSwapKey.setText("Current: " + Input.Keys.toString(keycode));
                    SettingsScene.this.stagedSettings.setSwapKey(keycode);
                    SettingsScene.this.waitingForSwapKey = false;
                }
                if (SettingsScene.this.waitingForFollowKey) {
                    setFollowButton.setText("Current: " + Input.Keys.toString(keycode));
                    SettingsScene.this.stagedSettings.setFollowKey(keycode);
                    SettingsScene.this.waitingForFollowKey = false;
                }
                return true;
            }
        });

        window.row();
        window.add(
                new Label("Change a keybind by clicking its button, then typing a new key", skin, "dark"))
                .colspan(2);

        window.row();
        TextButton closeWindow = new TextButton("Done", skin);
        closeWindow.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                window.setVisible(false);
            }
        });
        window.add(closeWindow).colspan(2); // .width(Value.percentWidth(0.8f, container));

        window.center();
        return container;
    }

    private Table makeSettingsTable(Window window) {
        Table table = new Table();
        // table.setSize(this.width, this.height);
        table.setFillParent(true);
        table.defaults().spaceBottom(10f);
        table.top().pad(Value.percentWidth(0.01f)).padTop(Value.percentHeight(0.3f));

        Value labelWidth = Value.percentWidth(0.33f, table);
        Value controlWidth = Value.percentWidth(0.5f, table);

        table.add(new Label("Music Volume", skin)).left().width(labelWidth);
        Slider musicVolumeSlider = new Slider(0f, 100f, 1f, false, skin);
        musicVolumeSlider.setValue(stagedSettings.getMusicVolume());
        musicVolumeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                SettingsScene.this.stagedSettings.setMusicVolume(musicVolumeSlider.getValue());
            }
        });
        table.add(musicVolumeSlider).left().width(controlWidth).expandX();

        table.row();
        table.add(new Label("Sound Effect Volume", skin)).left().width(labelWidth);
        Slider soundVolumeSlider = new Slider(0f, 100f, 1f, false, skin);
        soundVolumeSlider.setValue(stagedSettings.getSoundVolume());
        soundVolumeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                SettingsScene.this.stagedSettings.setSoundVolume(soundVolumeSlider.getValue());
            }
        });
        table.add(soundVolumeSlider).left().width(controlWidth);

        table.row();
        table.add(new Label("Resolution", skin)).left().width(labelWidth);
        SelectBox<String> resolutionSelect = new SelectBox<>(skin);
        resolutionSelect.setItems("1280x720", "1920x1080", "Fullscreen");
        resolutionSelect.setSelected(this.stagedSettings.getResolution());
        resolutionSelect.setAlignment(Align.center);
        resolutionSelect.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                SettingsScene.this.stagedSettings.setResolution(resolutionSelect.getSelected());
            }
        });
        table.add(resolutionSelect).left().width(controlWidth);

        table.row();
        table.add(new Label("Update Keybinds", skin)).left().width(labelWidth);
        TextButton keybindsDialogOpen = new TextButton("Open Keybind Editor", skin);
        keybindsDialogOpen.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                window.setVisible(true);
            }
        });
        table.add(keybindsDialogOpen).left().width(controlWidth);

        table.row();
        table.add(new Label("Reset Game State", skin)).left().width(labelWidth);
        TextButton resetGameState = new TextButton("Reset", skin);
        resetGameState.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                SettingsScene.this.resetState = true;
            }
        });
        table.add(resetGameState).left().width(controlWidth);

        table.row();
        TextButton menuSaveReturn = new TextButton("Save & Exit to Menu", skin);
        menuSaveReturn.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                SettingsScene.this.settings = SettingsScene.this.stagedSettings;
                listener.exitScreen(SettingsScene.this, GDXRoot.EXIT_MENU);
            }
        });
        table.add(menuSaveReturn).left().width(labelWidth).expandY().bottom().spaceRight(10f);

        TextButton menuReturn = new TextButton("Discard & Exit to Menu", skin);
        menuReturn.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                listener.exitScreen(SettingsScene.this, GDXRoot.EXIT_MENU);
            }
        });
        table.add(menuReturn).left().width(labelWidth).bottom();
        return table;
    }
}
