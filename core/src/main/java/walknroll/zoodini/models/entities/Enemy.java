package walknroll.zoodini.models.entities;

import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;

public class Enemy extends Avatar {

    public Enemy(MapProperties properties, float units) {
        super(AvatarType.ENEMY, properties, units);
    }
}
