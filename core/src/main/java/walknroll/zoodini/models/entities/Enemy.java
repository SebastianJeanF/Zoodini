package walknroll.zoodini.models.entities;

import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import walknroll.zoodini.utils.enums.AvatarType;

public class Enemy extends Avatar {

    public Enemy(MapProperties properties, JsonValue constants, float units) {
        super(AvatarType.ENEMY, properties, constants, units);
    }
}
