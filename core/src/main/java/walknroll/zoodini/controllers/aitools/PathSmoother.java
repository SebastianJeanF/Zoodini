package walknroll.zoodini.controllers.aitools;

import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements path smoothing for A* generated paths.
 * Uses line-of-sight checks to remove unnecessary waypoints.
 */
public class PathSmoother {

    private final TileGraph graph;

    /**
     * Creates a path smoother for the given tile graph
     *
     * @param graph The tile graph used for pathfinding
     */
    public PathSmoother(TileGraph graph) {
        this.graph = graph;
    }

    /**
     * Smooths a path by removing unnecessary waypoints.
     * Uses string-pulling (line-of-sight) algorithm to simplify paths.
     *
     * @param path The original path from A* pathfinding
     * @return A smoothed path with fewer waypoints
     */
    public List<Vector2> smoothPath(List<TileNode> path) {
        if (path == null || path.isEmpty()) {
            return new ArrayList<>();
        }

        List<Vector2> worldPath = new ArrayList<>();

        // Convert TileNodes to world coordinates
        for (TileNode node : path) {
            worldPath.add(graph.tileToWorld(node));
        }

        return stringPull(worldPath);
    }

    /**
     * String-pulling algorithm to reduce the number of waypoints.
     * Removes points that have a clear line of sight to a point further along the path.
     *
     * @param path The original path in world coordinates
     * @return A smoothed path with fewer waypoints
     */
    private List<Vector2> stringPull(List<Vector2> path) {
        if (path.size() <= 2) {
            return new ArrayList<>(path);
        }

        List<Vector2> smoothedPath = new ArrayList<>();

        // Always include the first point
        smoothedPath.add(path.get(0));

        int currentPoint = 0;

        while (currentPoint < path.size() - 1) {
            int furthestVisible = currentPoint + 1;

            // Look ahead as far as possible
            for (int i = furthestVisible + 1; i < path.size(); i++) {
                if (hasLineOfSight(path.get(currentPoint), path.get(i))) {
                    furthestVisible = i;
                }
            }

            // Add the furthest visible point to our path
            smoothedPath.add(path.get(furthestVisible));
            currentPoint = furthestVisible;
        }

        return smoothedPath;
    }

    /**
     * Checks if there's a clear line of sight between two points.
     * Uses Bresenham's line algorithm to check for obstacles.
     *
     * @param start The starting point
     * @param end The ending point
     * @return true if there's a clear line of sight, false otherwise
     */
    private boolean hasLineOfSight(Vector2 start, Vector2 end) {
        // We'll use Bresenham's line algorithm to check for obstacles
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
            TileNode node = graph.worldToTile(new Vector2(x0, y0));

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
}
