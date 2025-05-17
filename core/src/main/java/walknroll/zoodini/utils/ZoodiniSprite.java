package walknroll.zoodini.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.TextureMapObject;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import edu.cornell.gdiac.graphics.SpriteMesh;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.Obstacle;
import edu.cornell.gdiac.physics2.WheelObstacle;
import java.util.Comparator;

import com.badlogic.gdx.math.Vector2;

import edu.cornell.gdiac.physics2.ObstacleSprite;

public class ZoodiniSprite extends ObstacleSprite implements Comparable<ZoodiniSprite> {
    private boolean drawingEnabled = true;

    public ZoodiniSprite(){
        super();
    }


    /** Constructor for images */
    public ZoodiniSprite(TextureMapObject t, float units){
        super();
        MapProperties properties = t.getProperties();
        float[] pos = {properties.get("x", Float.class) / units, properties.get("y", Float.class)
            / units};
        float w = properties.get("width", Float.class) / units;
        float h = properties.get("height", Float.class) / units;
        obstacle = new BoxObstacle(pos[0] + w / 2, pos[1] + h / 2, w, h);
        obstacle.setSensor(true);
        obstacle.setPhysicsUnits(units);
        obstacle.setBodyType(BodyType.StaticBody);
        w = w * units;
        h = h * units;
        mesh = new SpriteMesh(-w / 2, -h / 2, w, h);
        setTextureRegion(t.getTextureRegion());
        setDebugColor(Color.GREEN);
    }

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
//        Vector2 position = this.obstacle.getPosition();
//        Vector2 oPosition = o.getObstacle().getPosition();
//        if (position.y > oPosition.y) {
//            return -1;
//        } else if (position.y < oPosition.y) {
//            return 1;
//        } else if (position.x > oPosition.x) {
//            return -1;
//        } else if (position.x < oPosition.x) {
//            return 1;
//        }

        float y = getBottomY();
        float y2 = o.getBottomY();
        return Float.compare(y2, y);
    }

    public float getBottomY(){
        float centerY = this.obstacle.getY();
        float height = 0;
        if(obstacle instanceof BoxObstacle b){
            height = b.getHeight();
        } else if (obstacle instanceof WheelObstacle w){
            height = w.getRadius() * 2;
        }
        return centerY - height/2.0f;
    }
}
