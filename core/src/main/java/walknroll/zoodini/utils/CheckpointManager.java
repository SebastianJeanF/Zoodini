package walknroll.zoodini.utils;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import com.badlogic.gdx.math.Vector2;
import walknroll.zoodini.utils.Checkpoint.KeyState;

public class CheckpointManager {
    /** Map of door IDs to associated checkpoints */
    private HashMap<Integer, List<Checkpoint>> doorCheckpoints;

    /** Map of character names to their active checkpoints */
    private HashMap<String, Checkpoint> activeCheckpoints;

    /** List of all checkpoints */
    private List<Checkpoint> allCheckpoints;

    /** Stored door states for each door checkpoint */
    private HashMap<Integer, HashMap<Integer, Boolean>> doorStates;

    /** Stored key states for each door checkpoint */
    private HashMap<Integer, HashMap<Integer, KeyState>> keyStates;

    /** Stored cat key counts for each door checkpoint */
    private HashMap<Integer, Integer> catKeyCounts;

    /** Stored octopus key counts for each door checkpoint */
    private HashMap<Integer, Integer> octopusKeyCounts;

    /** Constructor */
    public CheckpointManager() {
        doorCheckpoints = new HashMap<>();
        activeCheckpoints = new HashMap<>();
        allCheckpoints = new ArrayList<>();
        doorStates = new HashMap<>();
        keyStates = new HashMap<>();
        catKeyCounts = new HashMap<>();
        octopusKeyCounts = new HashMap<>();
    }

    /** Add a checkpoint to the manager */
    public void addCheckpoint(Checkpoint checkpoint) {
        allCheckpoints.add(checkpoint);

        // Associate checkpoint with its door
        Integer doorId = checkpoint.getDoorId();
        if (!doorCheckpoints.containsKey(doorId)) {
            doorCheckpoints.put(doorId, new ArrayList<>());
        }
        doorCheckpoints.get(doorId).add(checkpoint);
    }

    public boolean doorHasCheckpoints(Integer doorId) {
        return doorCheckpoints.containsKey(doorId);
    }

    /** Activate checkpoints associated with a door ID */
    public void activateDoorCheckpoints(Integer doorId) {
        if (!doorHasCheckpoints(doorId)) {
            return;
        }
        System.out.println("Activating checkpoints for door ID: " + doorId);
        resetCheckpoints(); // deactivate old checkpoints
        for (Checkpoint checkpoint : doorCheckpoints.get(doorId)) {
            checkpoint.setActive(true);

            // Update active checkpoint for the character(s)
            String forCharacter = checkpoint.getForCharacter();
            if (forCharacter.equals("cat")) {
                activeCheckpoints.put("cat", checkpoint);
            }
            if (forCharacter.equals("octopus")) {
                activeCheckpoints.put("octopus", checkpoint);
            }
        }
        printActiveCheckpoints();
    }

    /** Get the respawn position for a character */
    public Vector2 getRespawnPosition(String character) {
        if (activeCheckpoints.containsKey(character)) {
            return activeCheckpoints.get(character).getPosition();
        }
        // Return null if no active checkpoint (game will use default spawn)
        return null;
    }

    /** Reset all checkpoints (e.g., when restarting a level) */
    public void resetCheckpoints() {
        for (Checkpoint checkpoint : allCheckpoints) {
            checkpoint.setActive(false);
        }
        activeCheckpoints.clear();
    }

    public void clear() {
        doorCheckpoints.clear();
        activeCheckpoints.clear();
        allCheckpoints.clear();
    }

    public boolean hasActiveCheckpoint(String character) {
        return activeCheckpoints.containsKey(character);
    }

    public HashMap<String, Checkpoint> getActiveCheckpoints() {
        return activeCheckpoints;
    }

    public void printActiveCheckpoints() {
        System.out.println("Active Checkpoints:");
        for (String character : activeCheckpoints.keySet()) {
            Checkpoint checkpoint = activeCheckpoints.get(character);
            System.out.println("Character: " + character + ", Checkpoint ID: " + checkpoint.getId());
        }
    }

    /**
     * Store game state for a specific door ID checkpoint
     * This is called by the GameScene when a door is unlocked and has associated checkpoints
     */
    public void storeGameState(Integer doorId, HashMap<Integer, Boolean> doorState,
        HashMap<Integer, KeyState> keyState,
        int catKeyCount, int octopusKeyCount) {
        doorStates.put(doorId, doorState);
        keyStates.put(doorId, keyState);
        catKeyCounts.put(doorId, catKeyCount);
        octopusKeyCounts.put(doorId, octopusKeyCount);

        System.out.println("Stored game state for door: " + doorId +
            " | Cat keys: " + catKeyCount +
            " | Octopus keys: " + octopusKeyCount);
    }

    /**
     * Get cat key count for a door ID
     */
    public int getCatKeyCount(Integer doorId) {
        return catKeyCounts.getOrDefault(doorId, 0);
    }

    /**
     * Get octopus key count for a door ID
     */
    public int getOctopusKeyCount(Integer doorId) {
        return octopusKeyCounts.getOrDefault(doorId, 0);
    }

    /**
     * Get door states for a door ID
     */
    public HashMap<Integer, Boolean> getDoorStates(Integer doorId) {
        return doorStates.get(doorId);
    }

    /**
     * Get key states for a door ID
     */
    public HashMap<Integer, KeyState> getKeyStates(Integer doorId) {
        return keyStates.get(doorId);
    }


}
