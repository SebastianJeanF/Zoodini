package walknroll.zoodini.utils;

import java.util.Comparator;

import com.badlogic.gdx.math.Vector2;

import edu.cornell.gdiac.physics2.ObstacleSprite;

public class ZoodiniSprite extends ObstacleSprite implements Comparable<ZoodiniSprite> {
    private boolean drawingEnabled = true;

    public static Comparator<ZoodiniSprite> Comparison = new Comparator<ZoodiniSprite>() {
        @Override
        public int compare(ZoodiniSprite o1, ZoodiniSprite o2) {
            return o1.compareTo(o2);
        }
    };

    public boolean isDrawingEnabled() {
        return drawingEnabled;
    }

    public void setDrawingEnabled(boolean value) {
        this.drawingEnabled = value;
    }

    @Override
    public int compareTo(ZoodiniSprite o) {
        Vector2 position = this.obstacle.getPosition();
        Vector2 oPosition = o.getObstacle().getPosition();
        if (position.y > oPosition.y) {
            return -1;
        } else if (position.y < oPosition.y) {
            return 1;
        } else if (position.x > oPosition.x) {
            return -1;
        } else if (position.x < oPosition.x) {
            return 1;
        }
        return 0;
    }
}
