package walknroll.zoodini.models.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.utils.JsonValue;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteMesh;
import edu.cornell.gdiac.graphics.SpriteSheet;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.physics2.WheelObstacle;
import walknroll.zoodini.models.GameLevel;
import walknroll.zoodini.utils.ZoodiniSprite;

public class SecurityCamera extends ZoodiniSprite {

    private int startFrame;

    private boolean isDisabled;
    private float disabledTime;
    private float disabledTimeRemaining;

    private float angle;

    public boolean isDisabled() {
        return isDisabled;
    }

    public void setDisabled(boolean isDisabled) {
        this.isDisabled = isDisabled;
    }

    public float getAngle() {
        return angle;
    }

    public SecurityCamera(AssetDirectory directory, MapProperties properties, JsonValue globals, float units) {
        float[] pos = new float[2];
        pos[0] = properties.get("x", Float.class) / units;
        pos[1] = properties.get("y", Float.class) / units;
        float radius = properties.get("radius", Float.class);
        angle = properties.get("angle", Float.class);
        obstacle = new WheelObstacle(pos[0], pos[1], radius);
        obstacle.setName(properties.get("type", String.class));
        obstacle.setFixedRotation(false);

        obstacle.setBodyType(BodyDef.BodyType.StaticBody);
        obstacle.setDensity(1.0f);
        obstacle.setFriction(0.0f);
        obstacle.setRestitution(0.0f);
        obstacle.setPhysicsUnits(units);

        short collideBits = GameLevel.bitStringToShort(properties.get("collide", String.class));
        short excludeBits = GameLevel.bitStringToComplement(properties.get("exclude", String.class));
        Filter filter = new Filter();
        filter.categoryBits = collideBits;
        filter.maskBits = excludeBits;
        obstacle.setFilterData(filter);

        setDebugColor(ParserUtils.parseColor(globals.get("debug"), Color.WHITE));

        String key = globals.getString("texture"); //TODO somehow pull texture from tiled?
        startFrame = globals.getInt("startframe");
        sprite = directory.getEntry(key, SpriteSheet.class);
        sprite.setFrame(startFrame);

        float r = properties.get("spriteRadius", Float.class) * units;
        mesh = new SpriteMesh(-r, -r, 2 * r, 2 * r);

        disabledTime = disabledTimeRemaining = properties.get("disabledTime", Float.class);

        isDisabled = false;
    }

    @Override
    public void update(float dt) {
        super.update(dt);

        if (isDisabled()) {
            disabledTimeRemaining--;
        }

        if (disabledTimeRemaining < 0) {
            isDisabled = false;
            disabledTimeRemaining = disabledTime;
        }
    }
}
