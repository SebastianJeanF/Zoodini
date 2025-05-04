package walknroll.zoodini.controllers.aitools;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class TileNode {
    public static final int TILE_EMPTY = 0;
    public static final int TILE_FLOOR = 1;
    public static final int TILE_WALL = 2;

    public final int x;
    public final int y;
    private final int mapHeight;
    public boolean isWall;

    protected Array<Connection<TileNode>> connections;

    public TileNode(int x, int y, boolean isWall, Array<Connection<TileNode>> connections, int mapHeight) {
        this.x = x;
        this.y = y;
        this.connections = connections;
        this.mapHeight = mapHeight;
        this.isWall = isWall;
    }

    // TODO: is there a better way to let nodes know the height of map?
    public int getIndex() {
        return x * mapHeight + y;
    }

    public Array<Connection<TileNode>> getConnections() {
        return this.connections;
    }

    public Vector2 getCoords() {
        return new Vector2(x, y);
    }
}
