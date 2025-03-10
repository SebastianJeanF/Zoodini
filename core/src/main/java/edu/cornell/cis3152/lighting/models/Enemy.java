package edu.cornell.cis3152.lighting.models;

import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.physics2.ObstacleSprite;

public class Enemy extends Avatar {
    public Enemy(AssetDirectory directory, JsonValue json, float units) {
        super(directory, json, units);
    }
}
