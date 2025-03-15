package edu.cornell.cis3152.lighting.models.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.cis3152.lighting.models.GameLevel;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteMesh;
import edu.cornell.gdiac.graphics.SpriteSheet;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.physics2.WheelObstacle;

public class SecurityCamera extends ObstacleSprite {

    private int startFrame;

    public SecurityCamera(AssetDirectory directory, JsonValue json, JsonValue globals, float units) {
        float[] pos = json.get("pos").asFloatArray();
        float radius = globals.getFloat("radius");
        obstacle = new WheelObstacle(pos[0], pos[1], radius);
        obstacle.setName(json.name());
        obstacle.setFixedRotation(false);

        obstacle.setBodyType(BodyDef.BodyType.StaticBody);
        obstacle.setDensity(globals.getFloat("density"));
        obstacle.setFriction(globals.getFloat("friction"));
        obstacle.setRestitution(globals.getFloat("restitution"));
        obstacle.setPhysicsUnits(units);

        short collideBits = GameLevel.bitStringToShort(globals.getString("collide"));
        short excludeBits = GameLevel.bitStringToComplement(globals.getString("exclude"));
        Filter filter = new Filter();
        filter.categoryBits = collideBits;
        filter.maskBits = excludeBits;
        obstacle.setFilterData(filter);

        setDebugColor(ParserUtils.parseColor(globals.get("debug"), Color.WHITE));

        String key = globals.getString("texture");
        startFrame = globals.getInt("startframe");
        sprite = directory.getEntry(key, SpriteSheet.class);
        sprite.setFrame(startFrame);

        float r = globals.getFloat("spriterad") * units;
        mesh = new SpriteMesh(-r, -r, 2 * r, 2 * r);

    }


}
