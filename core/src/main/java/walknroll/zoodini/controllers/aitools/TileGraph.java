package walknroll.zoodini.controllers.aitools;

import java.util.HashSet;
import java.util.Set;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.math.PathFactory;
import edu.cornell.gdiac.math.Poly2;
import edu.cornell.gdiac.math.PolyFactory;

public class TileGraph<N extends TileNode> implements IndexedGraph<TileNode> {

    public final int HEIGHT;
    public final int WIDTH;

    protected Array<TileNode> nodes;
    public TileNode startNode;
    private Set<TileNode> targetNodes = new HashSet<>();
    private Set<TileNode> waypoints = new HashSet<>();
    public int tileWidth;
    public int tileHeight;
    private int density;

    boolean diagonal;

    /**
     * Constructs a TileGraph from a TileMapTileLayer
     *
     * @param diagonal whether diagonal movement is allowed
     */
    public TileGraph(TiledMap map, boolean diagonal, int density) {
        this.density = density;
        MapProperties props = map.getProperties();
        WIDTH = props.get("width", Integer.class) * density;
        HEIGHT = props.get("height", Integer.class) * density;
        int tileWidth = map.getProperties().get("tilewidth", Integer.class) / density;
        int tileHeight = map.getProperties().get("tileheight", Integer.class) / density;

        this.nodes = new Array<TileNode>(WIDTH * HEIGHT);
        this.startNode = null;
        this.diagonal = diagonal;

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                nodes.add(new TileNode(x, y, false, new Array<>(4), HEIGHT));
            }
        }

        MapLayer wallLayer = map.getLayers().get("walls");
        for (MapObject obj : wallLayer.getObjects()) {
            if (!(obj instanceof RectangleMapObject))
                continue;

            Rectangle rect = ((RectangleMapObject) obj).getRectangle();

            int startX = (int) (rect.x / tileWidth);
            int startY = (int) (rect.y / tileHeight);
            int endX = (int) ((rect.x + rect.width) / tileWidth);
            int endY = (int) ((rect.y + rect.height) / tileHeight);

            for (int x = startX; x < endX; x++) {
                for (int y = startY; y < endY; y++) {
                    getNode(x, y).isWall = true;
                }
            }
        }

        MapLayer objectLayer = map.getLayers().get("objects");
        for (MapObject obj : objectLayer.getObjects()) {
            if (!(obj instanceof RectangleMapObject)
                    || (!"Camera".equalsIgnoreCase(obj.getProperties().get("type", String.class))
                            && !"Exit".equalsIgnoreCase(obj.getProperties().get("type", String.class))))
                continue;

            Rectangle rect = ((RectangleMapObject) obj).getRectangle();

            int startX = (int) (rect.x / tileWidth);
            int startY = (int) (rect.y / tileHeight);
            int endX = (int) ((rect.x + rect.width) / tileWidth);
            int endY = (int) ((rect.y + rect.height) / tileHeight);

            for (int x = startX; x <= endX; x++) {
                for (int y = startY; y <= endY; y++) {
                    getNode(x, y).isWall = true;
                }
            }
        }

        for (int x = 0; x < WIDTH; x++) {
            int idx = x * HEIGHT;
            for (int y = 0; y < HEIGHT; y++) {
                TileNode n = nodes.get(idx + y);
                if (x > 0)
                    addConnection(n, -1, 0);
                if (y > 0)
                    addConnection(n, 0, -1);
                if (x < WIDTH - 1)
                    addConnection(n, 1, 0);
                if (y < HEIGHT - 1)
                    addConnection(n, 0, 1);
            }
        }
    }

    /**
     * Helper method for adding edges to a node. Modifies TileNode n's connections.
     *
     * @param n       a node
     * @param xOffset x offset of neighbor node
     * @param yOffset y offset of neighbor node
     */
    private void addConnection(TileNode n, int xOffset, int yOffset) {
        TileNode t = getNode(n.x + xOffset, n.y + yOffset);
        if (!t.isWall)
            n.getConnections().add(new TileEdge(this, n, t));
    }

    public TileNode getNode(int x, int y) {
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

    Affine2 cache = new Affine2();
    PolyFactory pf = new PolyFactory();
    PathFactory pathFactory = new PathFactory();
    Color c = Color.WHITE;

    public void draw(SpriteBatch batch, Camera camera, float units) {
        batch.begin(camera);

        cache.idt();
        cache.scale(units / density, units / density);
        for (TileNode node : nodes) {
            // TODO: Some colors are not working idk why
            if (node.isWall) {
                c = Color.BLUE;
            } else {
                c = Color.GOLD;
            }

            if (waypoints.contains(node)) {
                c = Color.GREEN;
            }

            if (node.equals(selected)) {
                c = Color.MAGENTA;
            }

            if (targetNodes.contains(node)) {
                c = Color.RED;
            }

            Poly2 polygon = pf.makeNgon(node.x + 0.5f, node.y + 0.5f, 0.1f, 10);
            Path2 rect = pathFactory.makeRect(node.x, node.y, 0.95f, 0.95f);
            batch.setColor(c);
            batch.fill(polygon, cache);
            batch.outline(rect, cache);
        }
        batch.end();
        batch.setColor(Color.WHITE);
    }

    Vector3 vec3 = new Vector3();
    TileNode selected = null;

    /**
     * Returns the nearest tile that you click on the screen.
     * This method is used for debugging purpose.
     * Thus, the selected tile's origin is marked as a different color in debug
     * mode.
     */
    public TileNode markNearestTile(Camera cam, float screenX, float screenY, float units) {
        vec3.set(screenX, screenY, 0.0f);
        vec3 = cam.unproject(vec3).scl(1.0f / units * density);
        int x = MathUtils.floor(vec3.x);
        int y = MathUtils.floor(vec3.y);
        // DebugPrinter.println("You clicked:" + x + " " + y);
        try {
            selected = getNode(x, y);
        } catch (IndexOutOfBoundsException e) {

        }
        return selected;
    }

    /**
     * This is an overriden method.
     *
     * Returns the nearest tile that you click on the screen.
     * This method is used for debugging purpose.
     * Thus, the selected tile's origin is marked as a different color in debug
     * mode.
     */
    public TileNode markNearestTile(Camera cam, Vector2 screenCoord, float units) {
        return markNearestTile(cam, screenCoord.x, screenCoord.y, units);
    }

    /**
     * Marks a node as a target node that will be highlighted in red during debug
     * drawing.
     *
     * @param node The node to mark as a target
     */
    public void markAsTarget(TileNode node) {
        if (node != null) {
            targetNodes.add(node);
        }
    }

    public void markWaypoints(Vector2[] worldPos) {
        for (Vector2 pos : worldPos) {
            TileNode node = worldToTile(pos);
            if (node != null) {
                waypoints.add(node);
            }
        }
    }

    /**
     * Clears all marked target nodes.
     */
    public void clearMarkedNodes() {
        targetNodes.clear();
        waypoints.clear();
    }

    /**
     * Marks a world position as a target that will be highlighted in red during
     * debug drawing.
     *
     * @param worldPos The world position to mark
     */
    public void markPositionAsTarget(Vector2 worldPos) {
        TileNode node = worldToTile(worldPos);
        if (node != null) {
            markAsTarget(node);
        }
    }

    /**
     * Converts world coordinates to a TileNode.
     * World coordinates represent the bottom-left corner of each 1x1 meter tile.
     *
     * @param worldCoords the world coordinates to convert
     * @return the TileNode at the specified world coordinates, or null if out of
     *         bounds
     */
    public TileNode worldToTile(Vector2 worldCoords) {
        int tileX = MathUtils.floor(worldCoords.x) * density;
        int tileY = MathUtils.floor(worldCoords.y) * density;

        // Make sure coordinates are within bounds
        tileX = MathUtils.clamp(tileX, 0, WIDTH - 1);
        tileY = MathUtils.clamp(tileY, 0, HEIGHT - 1);

        if (tileX >= 0 && tileX < WIDTH && tileY >= 0 && tileY < HEIGHT) {
            return getNode(tileX, tileY);
        }
        return null;
    }

    /**
     * Converts tile grid coordinates to world center coordinates.
     * Returns the center point of the specified tile.
     *
     * @param tile the TileNode to convert
     * @return a Vector2 containing the world coordinates of the tile's center
     */
    public Vector2 tileToWorld(TileNode tile) {
        return new Vector2((tile.x + 0.5f) / density, (tile.y + 0.5f) / density);
    }

    public TileNode findNearestNonObstacleNode(Vector2 pos) {
        TileNode targetNode = this.worldToTile(pos);
        if (targetNode == null || !targetNode.isWall) {
            return targetNode;
        }

        float minDistance = Float.MAX_VALUE;
        TileNode nearestNode = null;

        for (TileNode node : nodes) {
            if (!node.isWall) {
                float distance = tileToWorld(node).dst(pos);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestNode = node;
                }
            }
        }

        return nearestNode;
    }

    /**
     * Returns whether the tile at the given world coordinates is a valid tile (not
     * a wall).
     *
     * @param targetLocation the target location to check (in World Coords)
     * @return true if the target location is a valid tile, false otherwise
     */
    public boolean isValidTile(Vector2 targetLocation) {
        return worldToTile(targetLocation).isWall;
    }

    /**
     * Returns the Tile of the nearest non-wall tile to the given target location
     * If the target location is not a wall, it returns the target location itself.
     * This function only looks 1 layer away from the target location (unlike
     * findNearestNonObstacleNode).
     *
     * @param targetLocation the target location to check (in World Coords)
     * @return the nearest valid tile
     */
    public TileNode getNearestValidTile(Vector2 targetLocation) {
        TileNode targetTile = worldToTile(targetLocation);
        int[][] horizontal = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
        // Check all 4 directions for valid tile
        for (int[] coord : horizontal) {
            int newX = targetTile.x + coord[0];
            int newY = targetTile.y + coord[1];
            if (!(getNode(newX, newY)).isWall) {
                return getNode(newX, newY);
            }
        }
        // Check all 4 corners if needed
        int[][] corners = { { 1, 1 }, { -1, 1 }, { -1, 1 }, { -1, 1 } };
        for (int[] coord : corners) {
            int newX = targetTile.x + coord[0];
            int newY = targetTile.y + coord[1];
            if (!(getNode(newX, newY)).isWall) {
                return getNode(newX, newY);
            }
        }
        // Nearest non-wall tile is not 1 layer away (shouldn't happen)
        return targetTile;
    }

    public int getTileWidth() {
        return tileWidth;
    }

}
