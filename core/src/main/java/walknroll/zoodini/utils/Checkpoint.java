package walknroll.zoodini.utils;

import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.math.Vector2;

import walknroll.zoodini.utils.enums.AvatarType;

public class Checkpoint {
    /** The unique ID for this checkpoint */
    private final Integer id;

    /** The ID of the door that activates this checkpoint */
    private final Integer doorId;

    /** Which character this checkpoint is for (either "cat" or "octopus") */
    private final AvatarType forCharacter;

    /** Whether this checkpoint is currently active */
    private boolean isActive;

    /** The position of this checkpoint */
    private final Vector2 position;

    /** Inner class to store key state */
    public static class KeyState {
        public boolean collected;
        public AvatarType owner;

        public KeyState(boolean collected, AvatarType owner) {
            this.collected = collected;
            this.owner = owner;
        }

        public boolean getCollected() {
            return collected;
        }

        public AvatarType getOwner() {
            return owner;
        }
    }

    public static class DoorState {
        public boolean unlocked;
        public boolean isCheckpoint;

        public DoorState(boolean unlocked, boolean isCheckpoint) {
            this.unlocked = unlocked;
            this.isCheckpoint = isCheckpoint;
        }

        public boolean getUnlocked() {
            return unlocked;
        }

        public boolean getIsCheckpoint() {
            return isCheckpoint;
        }
    }

    /**
     * Creates a checkpoint with the given settings
     * @param properties The properties of the checkpoint
     * @param units     The physics units for this avatar
     */

    // Constructor
    public Checkpoint(MapProperties properties, float units) {
        this.id = properties.get("id", Integer.class);
        MapObject door = properties.get("door", MapObject.class);
        this.doorId = door.getProperties().get("id", Integer.class);
        this.forCharacter = (properties.get("forCat", Boolean.class)) ? AvatarType.CAT : AvatarType.OCTOPUS;
        this.position = new Vector2(properties.get("x", Float.class) / units, properties.get("y", Float.class) / units);
        this.isActive = false;
    }

    // Getters and setters
    public Integer getId() { return id; }

    public Integer getDoorId() { return doorId; }

    public AvatarType getForCharacter() { return forCharacter; }

    public boolean isActive() { return isActive; }

    public void setActive(boolean active) { this.isActive = active; }

    public Vector2 getPosition() { return position; }

    /** Returns whether this checkpoint applies to the given character */
    public boolean appliesTo(AvatarType character) {
        return forCharacter == character;
    }




}
