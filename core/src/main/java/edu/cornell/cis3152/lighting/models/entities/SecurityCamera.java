package edu.cornell.cis3152.lighting.models.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.cis3152.lighting.models.GameLevel;
import edu.cornell.cis3152.lighting.utils.ZoodiniSprite;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteMesh;
import edu.cornell.gdiac.graphics.SpriteSheet;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.physics2.WheelObstacle;

public class SecurityCamera extends ZoodiniSprite {

    private int startFrame;

    public SecurityCamera(AssetDirectory directory, JsonValue json, float units) {
        float[] pos = json.get("pos").asFloatArray();
        float radius = json.getFloat("radius");
        obstacle = new WheelObstacle(pos[0], pos[1], radius);
        obstacle.setName(json.name());
        obstacle.setFixedRotation(false);

        obstacle.setBodyType(BodyDef.BodyType.StaticBody);
        obstacle.setDensity(json.getFloat("density"));
        obstacle.setFriction(json.getFloat("friction"));
        obstacle.setRestitution(json.getFloat("restitution"));
        obstacle.setPhysicsUnits(units);

        short collideBits = GameLevel.bitStringToShort(json.getString("collide"));
        short excludeBits = GameLevel.bitStringToComplement(json.getString("exclude"));
        Filter filter = new Filter();
        filter.categoryBits = collideBits;
        filter.maskBits = excludeBits;
        obstacle.setFilterData(filter);

        setDebugColor(ParserUtils.parseColor(json.get("debug"), Color.WHITE));

        String key = json.getString("texture");
        startFrame = json.getInt("startframe");
        sprite = directory.getEntry(key, SpriteSheet.class);
        sprite.setFrame(startFrame);

        float r = json.getFloat("spriterad") * units;
        mesh = new SpriteMesh(-r, -r, 2 * r, 2 * r);

    }


}
