package walknroll.zoodini.utils;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import com.badlogic.gdx.math.Vector2;

public class CheckpointManager {
    /** Map of door IDs to associated checkpoints */
    private HashMap<Integer, List<Checkpoint>> doorCheckpoints;

    /** Map of character names to their active checkpoints */
    private HashMap<String, Checkpoint> activeCheckpoints;

    /** List of all checkpoints */
    private List<Checkpoint> allCheckpoints;

    /** Constructor */
    public CheckpointManager() {
        doorCheckpoints = new HashMap<>();
        activeCheckpoints = new HashMap<>();
        allCheckpoints = new ArrayList<>();
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
}
