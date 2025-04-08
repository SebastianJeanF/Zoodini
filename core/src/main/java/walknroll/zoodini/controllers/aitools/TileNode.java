package walknroll.zoodini.controllers.aitools;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.utils.Array;

public class TileNode{
    public static final int TILE_EMPTY = 0;
    public static final int TILE_FLOOR = 1;
    public static final int TILE_WALL = 2;

    public final int x;
    public final int y;
    public final Cell cell;
    private final int mapHeight;
    public boolean isWall;

    protected Array<Connection<TileNode>> connections;

    public TileNode (int x, int y, Cell cell, Array<Connection<TileNode>> connections, int mapHeight) {
        this.x = x;
        this.y = y;
        this.cell = cell;
        this.connections = connections;
        this.mapHeight = mapHeight;
//        if(cell != null) {
//            this.isWall = true;
//        } //TODO: set isWall based on the Cell
        if(cell != null) {
            this.isWall = cell.getTile().getObjects().getCount() == 1;
        }
    }

    //TODO: is there a better way to let nodes know the height of map?
    public int getIndex(){
        return x * mapHeight + y;
    }

    public Array<Connection<TileNode>> getConnections () {
        return this.connections;
    }
}
