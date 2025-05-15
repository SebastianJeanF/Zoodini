package walknroll.zoodini.controllers.aitools;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.ai.pfa.Heuristic;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    private Heuristic heuristic = new ManhattanHeuristic<>();

    boolean diagonal;

    /** Temporary vectors for calculations */
    private final Vector2 tmpVec1 = new Vector2();
    private final Vector2 tmpVec2 = new Vector2();
    private final Vector2 tmpVec3 = new Vector2();

    private static final float SAFETY_MARGIN = 0.25f;

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
                    getNode(x, y).isObstacle = true;
                }
            }
        }

        MapLayer objectLayer = map.getLayers().get("objects");
        for (MapObject obj : objectLayer.getObjects()) {
            if(!(obj instanceof RectangleMapObject)){
                continue;
            }

            String type = obj.getProperties().get("type", String.class);
            if (type == null) {
                continue;
            }

            if(!(type.equalsIgnoreCase("Camera") || type.equalsIgnoreCase("Door") || type.equalsIgnoreCase("Exit"))){
                continue;
            }

            Rectangle rect = ((RectangleMapObject) obj).getRectangle();

            if(type.equalsIgnoreCase("Door")){
                int startX = (int) (rect.x / tileWidth);
                int startY = (int) (rect.y / tileHeight);
                int endX = (int) ((rect.x + rect.width) / tileWidth);
                int endY = (int) ((rect.y + rect.height) / tileHeight);

                for (int x = startX; x < endX; x++) {
                    for (int y = startY; y < endY; y++) {
                        getNode(x, y).isObstacle = true;
                    }
                }
            } else {

                int startX = (int) (rect.x / tileWidth);
                int startY = (int) (rect.y / tileHeight);
                int endX = (int) ((rect.x + rect.width) / tileWidth);
                int endY = (int) ((rect.y + rect.height) / tileHeight);

                for (int x = startX; x <= endX; x++) {
                    for (int y = startY; y <= endY; y++) {
                        getNode(x, y).isObstacle = true;
                    }
                }
            }
        }
        addConnections();
    }

    public void addConnections(){
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
     * Creates connections between nodes in the graph
     * Handles both cardinal and diagonal directions based on the diagonal flag
     */
    private void createConnections() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                TileNode n = getNode(x, y);

                // Cardinal connections (up, down, left, right)
                if (x > 0)
                    addConnection(n, -1, 0);  // Left
                if (y > 0)
                    addConnection(n, 0, -1);  // Down
                if (x < WIDTH - 1)
                    addConnection(n, 1, 0);   // Right
                if (y < HEIGHT - 1)
                    addConnection(n, 0, 1);   // Up

                // Add diagonal connections if enabled
                if (diagonal) {
                    if (x > 0 && y > 0)
                        addDiagonalConnection(n, -1, -1);  // Bottom-left
                    if (x < WIDTH - 1 && y > 0)
                        addDiagonalConnection(n, 1, -1);   // Bottom-right
                    if (x > 0 && y < HEIGHT - 1)
                        addDiagonalConnection(n, -1, 1);   // Top-left
                    if (x < WIDTH - 1 && y < HEIGHT - 1)
                        addDiagonalConnection(n, 1, 1);    // Top-right
                }
            }
        }
    }

    /**
     * Helper method for adding diagonal edges to a node.
     * Ensures that diagonal movement is only allowed if there are no obstacles
     * in the adjacent cardinal directions to prevent corner-cutting.
     *
     * @param n       a node
     * @param xOffset x offset of neighbor node
     * @param yOffset y offset of neighbor node
     */
    private void addDiagonalConnection(TileNode n, int xOffset, int yOffset) {
        TileNode targetNode = getNode(n.x + xOffset, n.y + yOffset);

        // Don't connect to obstacle nodes
        if (targetNode.isObstacle) {
            return;
        }

        // Check if the path is blocked by adjacent obstacles (prevents corner-cutting)
        TileNode horizNeighbor = getNode(n.x + xOffset, n.y);
        TileNode vertNeighbor = getNode(n.x, n.y + yOffset);

        // We can only move diagonally if at least one of the adjacent cardinal nodes is not an obstacle
        if (!horizNeighbor.isObstacle || !vertNeighbor.isObstacle) {
            n.getConnections().add(new TileEdge(this, n, targetNode));
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
        if (!t.isObstacle)
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
            if (node.isObstacle) {
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
        if (targetNode == null || !targetNode.isObstacle) {
            return targetNode;
        }

        float minDistance = Float.MAX_VALUE;
        TileNode nearestNode = null;

        for (TileNode node : nodes) {
            if (!node.isObstacle) {
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
        return worldToTile(targetLocation).isObstacle;
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
            if (!(getNode(newX, newY)).isObstacle) {
                return getNode(newX, newY);
            }
        }
        // Check all 4 corners if needed
        int[][] corners = { { 1, 1 }, { -1, 1 }, { -1, 1 }, { -1, 1 } };
        for (int[] coord : corners) {
            int newX = targetTile.x + coord[0];
            int newY = targetTile.y + coord[1];
            if (!(getNode(newX, newY)).isObstacle) {
                return getNode(newX, newY);
            }
        }
        // Nearest non-wall tile is not 1 layer away (shouldn't happen)
        return targetTile;
    }

    /**
     * Finds the shortest path between two positions in the world using A*.
     *
     * @INVARIANT this.heuristic must be initialized
     * @param currPosWorld   The starting position in world coordinates
     * @param targetPosWorld The target position in world coordinates
     * @return A list of nodes representing the path from start to target, excluding
     *         the start node
     */
    public List<TileNode> getPath(Vector2 currPosWorld, Vector2 targetPosWorld, IndexedAStarPathFinder pathFinder) {
        GraphPath<TileNode> graphPath = new DefaultGraphPath<>();
        TileNode start = worldToTile(currPosWorld);
        TileNode end = worldToTile(targetPosWorld);

        // DebugPrinter.println("Current guard Position: " + currPosWorld);
        // DebugPrinter.println("Graph's target: "+ end.getWorldPosition());
        // Check if start or end node is null
        if (start == null || end == null) {
            // System.err.println("Error: Start or end node is null.");
            return new ArrayList<>();
        }

        if (start.isObstacle) {
            start = findNearestNonObstacleNode(currPosWorld);
        }

        if (end.isObstacle) {
            end = findNearestNonObstacleNode(targetPosWorld);
        }

        pathFinder.searchNodePath(start, end, heuristic, graphPath);

        // Only add nodes to the path if they are not the start node
        List<TileNode> path = new ArrayList<>();
        for (TileNode node : graphPath) {
            if (!node.equals(start)) {
                path.add(node);
            }
        }
        return path;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    /**
     * Determines if three points form a corner by measuring the change in direction.
     *
     * @param prev The previous point
     * @param current The current point
     * @param next The next point
     * @return true if the points form a significant corner
     */
    private boolean isCorner(Vector2 prev, Vector2 current, Vector2 next) {
        // Calculate directions
        Vector2 dir1 = new Vector2(current).sub(prev).nor();
        Vector2 dir2 = new Vector2(next).sub(current).nor();

        // Calculate dot product to measure angle change
        float dotProduct = dir1.dot(dir2);

        // Consider it a corner if the direction changes significantly
        // (dot product < 0.7 is approximately > 45 degrees change)
        return dotProduct < 0.7f;
    }

    /**
     * Helper function that checks if the target position is not a wall.
     * If the target position is a wall, it returns the world coords of the nearest
     * non-wall tile.
     * If the target position is not a wall, it returns the original target
     * position.
     *
     * @param target The target position to check
     * @return A valid Vector2 position that is not a wall
     */
    public Vector2 getValidTileCoords(Vector2 target) {
        TileNode targetTile = worldToTile(target);
        if (!targetTile.isObstacle) {
            return target;
        } else {
            // If the target tile is a wall, find the nearest non-wall tile
            TileNode newTile = getNearestValidTile(target);
            return tileToWorld(newTile);
        }
    }

    /**
     * Checks if there's a clear line of sight between two points.
     * Uses Bresenham's line algorithm to check for obstacles.
     *
     * @param start The starting point
     * @param end The ending point
     * @return true if there's a clear line of sight, false otherwise
     */
    public boolean hasLineOfSight(Vector2 start, Vector2 end) {

        int x0 = (int)start.x;
        int y0 = (int)start.y;
        int x1 = (int)end.x;
        int y1 = (int)end.y;

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (x0 != x1 || y0 != y1) {
            TileNode node = worldToTile(new Vector2(x0, y0));

            // Check if this tile is an obstacle
            if (node != null && node.isObstacle) {
                return false;
            }

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }

        return true;
    }

    /**
     * Enhanced line of sight check that detects corners and obstacles
     */
    public boolean hasEnhancedLineOfSight(Vector2 start, Vector2 end) {
        // Basic line of sight check using TileGraph
        if (!hasLineOfSight(start, end)) {
            return false;
        }

        // Additional check: Ensure the path doesn't go too close to obstacles
        Vector2 dir = tmpVec1.set(end).sub(start).nor();
        Vector2 perpendicular = tmpVec2.set(-dir.y, dir.x).nor().scl(SAFETY_MARGIN);

        float distance = start.dst(end);
        float step = 0.5f; // Check every half unit

        // Sample points along the path
        for (float dist = 0; dist < distance; dist += step) {
            // Position on the direct line
            Vector2 pos = tmpVec3.set(start).add(dir.x * dist, dir.y * dist);

            // Check points to the left and right of the line
            Vector2 leftPos = new Vector2(pos).add(perpendicular);
            Vector2 rightPos = new Vector2(pos).sub(perpendicular);

            // Check if any of these positions would hit an obstacle
            TileNode leftNode = worldToTile(leftPos);
            TileNode rightNode = worldToTile(rightPos);

            if ((leftNode != null && leftNode.isObstacle) ||
                (rightNode != null && rightNode.isObstacle)) {
                return false; // Too close to an obstacle
            }
        }

        return true;
    }

    public void dispose(){
        for(TileNode n : nodes){
            n.dispose();
        }
        nodes.clear();
        nodes = null;
        targetNodes.clear();
        targetNodes = null;
        waypoints.clear();;
        waypoints = null;
        heuristic = null;
    }
}
