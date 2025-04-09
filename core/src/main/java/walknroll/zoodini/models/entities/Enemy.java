package walknroll.zoodini.models.entities;

import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;

public class Enemy extends Avatar {

    public Enemy(AssetDirectory directory, MapProperties properties, JsonValue globals, float units) {
        super(AvatarType.ENEMY, directory, properties, globals, units);
        if (sprite != null) {
            sprite = sprite.copy();
        }
    }

}
