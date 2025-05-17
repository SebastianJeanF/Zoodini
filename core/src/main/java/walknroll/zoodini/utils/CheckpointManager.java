package walknroll.zoodini.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.badlogic.gdx.math.Vector2;

import walknroll.zoodini.utils.Checkpoint.DoorState;
import walknroll.zoodini.utils.Checkpoint.KeyState;
import walknroll.zoodini.utils.enums.AvatarType;

public class CheckpointManager {

    /** Map of door IDs to associated checkpoints */
    private final HashMap<Integer, List<Checkpoint>> doorCheckpoints;

    /** Current Checkpoint for Gar */
    private Checkpoint currGarCheckpoint;

    /** Current Checkpoint for Otto */
    private Checkpoint currOttoCheckpoint;

    /** List of all checkpoints */
    private final List<Checkpoint> allCheckpoints;

    /** Stored game states for each door checkpoint; { doorId: CheckpointSaveState } */
    private final HashMap<Integer, CheckpointSaveState> checkpointStates;

    /** Snapshot of the game state that is saved when a checkpoint is unlocked */
    public static class CheckpointSaveState {

        /** Map of door IDs to their states (locked/unlocked) */
        public HashMap<Integer, DoorState> doorStates;
        /** Map of key IDs to their states (collected/uncollected) */
        public HashMap<Integer, KeyState> keyStates;
        /** Number of cat keys collected during snapshot */
        public int catKeyCount;
        /** Number of octopus keys collected during snapshot */
        public int octopusKeyCount;

        public CheckpointSaveState() {
            doorStates = new HashMap<>();
            keyStates = new HashMap<>();
            catKeyCount = 0;
            octopusKeyCount = 0;
        }

        public HashMap<Integer, DoorState> getDoorState() {
            return doorStates;
        }

        public HashMap<Integer, KeyState> getKeyState() {
            return keyStates;
        }

        public int getCatKeyCount() {
            return catKeyCount;
        }

        public int getOctopusKeyCount() {
            return octopusKeyCount;
        }

    }

    /** Constructor */
    public CheckpointManager() {
        doorCheckpoints = new HashMap<>();
        allCheckpoints = new ArrayList<>();
        checkpointStates = new HashMap<>();
        currGarCheckpoint = null;
        currOttoCheckpoint = null;
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
    //    public void activateDoorCheckpoints(Integer doorId) {
    //        if (!doorHasCheckpoints(doorId)) {
    //            return;
    //        }
    //        DebugPrinter.println("Activating checkpoints for door ID: " + doorId);
    //        resetCheckpoints(); // deactivate old checkpoints
    //        for (Checkpoint checkpoint : doorCheckpoints.get(doorId)) {
    //            checkpoint.setActive(true);
    //
    //            // Update active checkpoint for the character(s)
    //            String forCharacter = checkpoint.getForCharacter();
    //            if (forCharacter.equals("cat")) {
    //                currGarCheckpoint = checkpoint;
    //            }
    //            if (forCharacter.equals("octopus")) {
    //                currOttoCheckpoint = checkpoint;
    //            }
    //        }
    //        printActiveCheckpoints();
    //    }

    /**
     * Activate checkpoints associated with a door ID without deactivating existing checkpoints
     * for other characters
     */
    public void activateDoorCheckpoints(Integer doorId) {
        if (!doorCheckpoints.containsKey(doorId)) {
            return;
        }

        DebugPrinter.println("Activating checkpoints for door ID: " + doorId);

        // Instead of resetting all checkpoints, we'll process each character individually
        for (Checkpoint checkpoint : doorCheckpoints.get(doorId)) {
            checkpoint.setActive(true);

            // Update active checkpoint for the specific character only
            AvatarType forCharacter = checkpoint.getForCharacter();
            if (forCharacter == AvatarType.CAT) {
                // If there was a previous checkpoint for this character, deactivate it
                if (currGarCheckpoint != null) {
                    currGarCheckpoint.setActive(false);
                }
                currGarCheckpoint = checkpoint;
                DebugPrinter.println("Set Gar's checkpoint to door ID: " + doorId);
            } else if (forCharacter == AvatarType.OCTOPUS) {
                // If there was a previous checkpoint for this character, deactivate it
                if (currOttoCheckpoint != null) {
                    currOttoCheckpoint.setActive(false);
                }
                currOttoCheckpoint = checkpoint;
                DebugPrinter.println("Set Otto's checkpoint to door ID: " + doorId);
            }
        }

        printActiveCheckpoints();
    }

    /** Get the respawn position for a character */
    public Vector2 getRespawnPosition(String character) {
        if (character.equals("cat")) {
            return currGarCheckpoint.getPosition();
        } else if (character.equals("octopus")) {
            return currOttoCheckpoint.getPosition();
        }
        return null;
    }

    /** Reset all checkpoints (e.g., when restarting a level) */
    public void resetCheckpoints() {
        for (Checkpoint checkpoint : allCheckpoints) {
            checkpoint.setActive(false);
        }
    }

    public void clear() {
        doorCheckpoints.clear();
        allCheckpoints.clear();
        currGarCheckpoint = null;
        currOttoCheckpoint = null;
    }

    public boolean hasActiveCheckpoint(String character) {
        if (character.equals("cat") && currGarCheckpoint != null) {
            return true;
        } else
            return character.equals("octopus") && currOttoCheckpoint != null;
    }

    public void printActiveCheckpoints() {
        if (currGarCheckpoint == null) {
            DebugPrinter.println("No active checkpoint for Gar");
        } else {
            DebugPrinter.println("Active checkpoint for Gar: " + currGarCheckpoint.getId());
        }
        if (currOttoCheckpoint == null) {
            DebugPrinter.println("No active checkpoint for Otto");
        } else {
            DebugPrinter.println("Active checkpoint for Otto: " + currOttoCheckpoint.getId());
        }
    }

    // Store game state for a door
    public void storeGameState(Integer doorId, HashMap<Integer, DoorState> doorState,
            HashMap<Integer, KeyState> keyState,
            int catKeyCount, int octopusKeyCount) {
        CheckpointSaveState saveState = new CheckpointSaveState();
        saveState.doorStates.putAll(doorState);
        saveState.keyStates.putAll(keyState);
        saveState.catKeyCount = catKeyCount;
        saveState.octopusKeyCount = octopusKeyCount;

        checkpointStates.put(doorId, saveState);
    }

    public CheckpointSaveState getCheckpointSaveState(Integer doorId) {
        if (!checkpointStates.containsKey(doorId)) {
            return null;
        }
        return checkpointStates.get(doorId);
    }

    public Checkpoint getCurrGarCheckpoint() {
        return currGarCheckpoint;
    }

    public Checkpoint getCurrOttoCheckpoint() {
        return currOttoCheckpoint;
    }

    /**
     * Get the merged checkpoint state from both characters' checkpoints
     * This creates a combined state to ensure both characters' progress is preserved
     */
    public CheckpointSaveState getMergedCheckpointState() {
        // If neither character has a checkpoint, return null
        if (currGarCheckpoint == null && currOttoCheckpoint == null) {
            return null;
        }

        // Create a new merged state
        CheckpointSaveState mergedState = new CheckpointSaveState();

        // Add states from Gar's checkpoint if it exists
        if (currGarCheckpoint != null) {

            Integer garDoorId = currGarCheckpoint.getDoorId();
            CheckpointSaveState garState = checkpointStates.get(garDoorId);

            if (garState != null) {
                mergedState.doorStates.putAll(garState.doorStates);
                mergedState.keyStates.putAll(garState.keyStates);
                mergedState.catKeyCount = garState.catKeyCount;

                // Only use Otto's key count if Gar's checkpoint doesn't have it
                if (currOttoCheckpoint == null) {
                    mergedState.octopusKeyCount = garState.octopusKeyCount;
                }
            }
        }

        // Add states from Otto's checkpoint if it exists
        if (currOttoCheckpoint != null) {
            Integer ottoDoorId = currOttoCheckpoint.getDoorId();
            CheckpointSaveState ottoState = checkpointStates.get(ottoDoorId);

            if (ottoState != null) {
                // For doors, we want to merge (prefer unlocked state)
                for (Integer doorId : ottoState.doorStates.keySet()) {
                    // If the door is already in merged state but locked, and Otto's state has it unlocked,
                    // use Otto's unlocked state
                    if (!mergedState.doorStates.containsKey(doorId) ||
                            (mergedState.doorStates.get(doorId).getUnlocked()
                                    && !ottoState.doorStates.get(doorId).getUnlocked())) {
                        mergedState.doorStates.put(doorId, ottoState.doorStates.get(doorId));
                    }
                }

                // For keys, we want to merge (prefer collected state)
                for (Integer keyId : ottoState.keyStates.keySet()) {
                    KeyState ottoKeyState = ottoState.keyStates.get(keyId);

                    // If the key is not in merged state, or is uncollected but Otto collected it,
                    // use Otto's key state
                    if (!mergedState.keyStates.containsKey(keyId) ||
                            (!mergedState.keyStates.get(keyId).collected
                                    && ottoKeyState.collected)) {
                        mergedState.keyStates.put(keyId, ottoKeyState);
                    }
                }

                // Always use Otto's key count
                mergedState.octopusKeyCount = ottoState.octopusKeyCount;

                // Only use Gar's key count if Otto's checkpoint doesn't have it
                if (currGarCheckpoint == null) {
                    mergedState.catKeyCount = ottoState.catKeyCount;
                }
            }
        }

        return mergedState;
    }

    public boolean doorHasCheckpoint(Integer doorId) {
        return doorCheckpoints.containsKey(doorId);
    }
}
