package edu.cornell.cis3152.lighting.models.entities;

import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;

public class Enemy extends Avatar {

    public Enemy(AssetDirectory directory, JsonValue json, JsonValue globals, float units) {
        super(AvatarType.ENEMY, directory, json, globals, units);
    }

}
