package walknroll.zoodini.utils;

import edu.cornell.gdiac.physics2.ObstacleSprite;

public class ZoodiniSprite extends ObstacleSprite {
    private boolean drawingEnabled = true;

    public boolean isDrawingEnabled() {
        return drawingEnabled;
    }

    public void setDrawingEnabled(boolean value) {
        this.drawingEnabled = value;
    }
}
