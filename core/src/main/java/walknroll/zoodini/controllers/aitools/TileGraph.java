package walknroll.zoodini.controllers.aitools;


import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.utils.Array;

public class TileGraph<N extends TileNode> implements IndexedGraph<TileNode> {

    public final int HEIGHT;
    public final int WIDTH;

    protected Array<TileNode> nodes;
    public TileNode startNode;

    boolean diagonal;

    /**
     * Constructs a TileGraph from a TileMapTileLayer
     * @param layer
     * @param diagonal whether diagonal movement is allowed
     */
    public TileGraph(TiledMapTileLayer layer, boolean diagonal){
        WIDTH = layer.getWidth();
        HEIGHT = layer.getHeight();
        this.nodes = new Array<TileNode>(WIDTH * HEIGHT);
        this.startNode = null;
        this.diagonal = diagonal;
        init(layer); //TODO: should init be called separately?
    }


    /**
     * Initializes the graph from a TiledMapTileLayer.
     * This method is different from the constructor: this method
     * is actually responsible for adding nodes and edges.
     * @param layer
     */
    public void init(TiledMapTileLayer layer){
        for(int x = 0; x < WIDTH; x++){
            for(int y = 0; y < HEIGHT; y++){
                nodes.add(new TileNode(x,y,layer.getCell(x,y), new Array<>(4), HEIGHT));
            }
        }

        for (int x = 0; x < WIDTH; x++) {
            int idx = x * HEIGHT;
            for (int y = 0; y < HEIGHT; y++) {
                TileNode n = nodes.get(idx + y);
                if (x > 0) addConnection(n, -1, 0);
                if (y > 0) addConnection(n, 0, -1);
                if (x < WIDTH - 1) addConnection(n, 1, 0);
                if (y < HEIGHT - 1) addConnection(n, 0, 1);
            }
        }
    }

    /**
     * Helper method for adding edges to a node.
     * Modifies TileNode n's connections.
     * @param n a node
     * @param xOffset x offset of neighbor node
     * @param yOffset y offset of neighbor node
     */
    private void addConnection(TileNode n, int xOffset, int yOffset){
        TileNode t = getNode(n.x + xOffset, n.y + yOffset);
        if (!t.isWall) n.getConnections().add(new TileEdge(this, n, t));
    }

    public TileNode getNode(int x, int y){
        return nodes.get(x * HEIGHT + y);
    }

    @Override
    public int getIndex(TileNode node) {
        return node.getIndex();
    }

    @Override
    public int getNodeCount() {
        return nodes.size;
    }

    @Override
    public Array<Connection<TileNode>> getConnections(TileNode fromNode) {
        return fromNode.getConnections();
    }
}
