package walknroll.zoodini.controllers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.utils.Align;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteSheet;
import walknroll.zoodini.models.GameLevel;
import walknroll.zoodini.models.entities.Avatar;
import walknroll.zoodini.models.entities.Avatar.AvatarType;
//Scene2d stuff
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import walknroll.zoodini.models.entities.Octopus;
import walknroll.zoodini.utils.CounterActor;
import walknroll.zoodini.utils.InkMeterActor;

public class UIController {

    private final static boolean debug = true;

    protected BitmapFont displayFont;

    private Stage stage;
    private Skin skin;
    private ScreenViewport viewport;
    private Table rootTable;

    private Image catIconImage;
    private Image octopusIconImage;
    private InkMeterActor inkMeter;
    private CounterActor counter;

    public UIController(AssetDirectory directory, GameLevel level) {
        skin = new Skin(Gdx.files.internal("skins/uiskin.json")); //TODO: use AssetDirectory to load skins.
        initializeActors(directory, level);
        setupStageLayout();
    }


    /**
     * Initialize all the Actors (Images, Buttons, etc.)
     */
    private void initializeActors(AssetDirectory directory, GameLevel level){
        setFont(directory.getEntry("display", BitmapFont.class));

        setCatIconImage(new TextureRegion(directory.getEntry("cat-icon", Texture.class)));
        catIconImage.setVisible(false);

        setOctopusIconImage(new TextureRegion(directory.getEntry("octopus-icon", Texture.class)));
        octopusIconImage.setVisible(false);

        SpriteSheet inkSprites = directory.getEntry("ink-meter.animation", SpriteSheet.class);
        Octopus o = level.getOctopus();
        inkMeter = new InkMeterActor(inkSprites, o.getInkCapacity(), o.getInkCost() ,o.getInkRegen());

        counter = new CounterActor(displayFont, 10);
    }


    /**
     * Places each Actor at the right position.
     */
    private void setupStageLayout(){
        viewport = new ScreenViewport();
        stage = new Stage(viewport);
        rootTable = new Table();
        rootTable.setFillParent(true);

        Table bottomLeftTable = new Table();
        bottomLeftTable.bottom().left();
        bottomLeftTable.setFillParent(true);
        bottomLeftTable.setDebug(debug);
        stage.addActor(bottomLeftTable);

        //Put icons at the top of each other.
        Stack stack = new Stack();
        stack.add(catIconImage);
        stack.add(octopusIconImage);
        bottomLeftTable.add(stack);
        bottomLeftTable.add(inkMeter).align(Align.bottom);
        bottomLeftTable.add(counter);
    }

    public void setFont(BitmapFont f) {
        displayFont = f;
    }

    public void setCatIconImage(TextureRegion icon) {
        catIconImage = new Image(icon);
    }
    public void setOctopusIconImage(TextureRegion icon) {
        octopusIconImage = new Image(icon);
    }

    public void update() {

    }

    public void resize(int width, int height){
        viewport.update(width, height, true);
    }

    public void dispose() {
        if (stage != null){
            stage.dispose();
        }
        if (skin != null){
            skin.dispose();
        }
    }

    public void update(float dt){
        stage.act(dt);
    }

    public void draw(GameLevel level) {
        Avatar avatar = level.getAvatar();
        boolean isOcto = avatar.getAvatarType() == AvatarType.OCTOPUS;

        // Icons
        if (catIconImage != null) catIconImage.setVisible(!isOcto);
        if (octopusIconImage != null) octopusIconImage.setVisible(isOcto);
        if (inkMeter != null) inkMeter.setVisible(isOcto);
        inkMeter.sync(level.getOctopus().getInkRemaining());


        // finally… draw the stage
        stage.draw();
    }
}
