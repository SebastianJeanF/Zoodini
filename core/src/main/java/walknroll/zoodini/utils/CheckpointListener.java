package walknroll.zoodini.utils;

import walknroll.zoodini.models.entities.PlayableAvatar;

/**
 * Interface for receiving checkpoint activation events
 */
public interface CheckpointListener {
    /**
     * Called when a door that has checkpoints is unlocked
     * @param doorId The ID of the door that was unlocked
     * @param unlocker The character that unlocked the door
     */
    void onCheckpointActivated(Integer doorId, PlayableAvatar unlocker);
}
