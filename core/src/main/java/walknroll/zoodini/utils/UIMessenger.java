package walknroll.zoodini.utils;

import walknroll.zoodini.models.GameLevel;
import walknroll.zoodini.models.entities.Avatar;

/** A messenger interface that sends information to UI controller.*/
public interface UIMessenger {
    GameLevel getLevel();
    boolean getFollowModeActive();
}
