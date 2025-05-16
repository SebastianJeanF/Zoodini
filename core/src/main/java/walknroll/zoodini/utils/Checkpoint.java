package walknroll.zoodini.utils;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteMesh;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.util.PooledList;
import java.util.HashMap;
import java.util.Objects;
import walknroll.zoodini.models.GameLevel;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import walknroll.zoodini.models.entities.Cat;
import walknroll.zoodini.models.entities.Octopus;
import walknroll.zoodini.models.entities.PlayableAvatar;
import walknroll.zoodini.models.nonentities.Door;
import walknroll.zoodini.models.nonentities.Key;
import walknroll.zoodini.utils.enums.AvatarType;

public class Checkpoint {
    /** The unique ID for this checkpoint */
    private Integer id;

    /** The ID of the door that activates this checkpoint */
    private Integer doorId;

    /** Which character this checkpoint is for ("Gar", "Otto", or "Both") */
    private String forCharacter;

    /** Whether this checkpoint is currently active */
    private boolean isActive;

    /** The position of this checkpoint */
    private Vector2 position;

    /** Inner class to store key state */
    public static class KeyState {
        public boolean collected;
        public AvatarType owner;

        public KeyState(boolean collected, AvatarType owner) {
            this.collected = collected;
            this.owner = owner;
        }
    }

    /**
     * Creates a checkpoint with the given settings
     *
     * @param directory The asset directory (for textures, etc)
     * @param units     The physics units for this avatar
     */

    // Constructor
    public Checkpoint(AssetDirectory directory, MapProperties properties, JsonValue constants, float units) {
        this.id = properties.get("id", Integer.class);
        MapObject door = properties.get("door", MapObject.class);
        this.doorId = door.getProperties().get("id", Integer.class);
        this.forCharacter = (properties.get("forCat", Boolean.class)) ? "cat" : "octopus";
        this.position = new Vector2(properties.get("x", Float.class) / units, properties.get("y", Float.class) / units);
        this.isActive = properties.get("isActive", Boolean.class);
    }

    // Getters and setters
    public Integer getId() { return id; }

    public Integer getDoorId() { return doorId; }

    public String getForCharacter() { return forCharacter; }

    public boolean isActive() { return isActive; }

    public void setActive(boolean active) { this.isActive = active; }

    public Vector2 getPosition() { return position; }

    /** Returns whether this checkpoint applies to the given character */
    public boolean appliesTo(String character) {
        return forCharacter.equals(character);
    }




}
