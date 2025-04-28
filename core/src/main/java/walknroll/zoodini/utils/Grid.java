package walknroll.zoodini.utils;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import edu.cornell.gdiac.physics2.*;
import edu.cornell.gdiac.util.PooledList;
import walknroll.zoodini.models.nonentities.ExteriorWall;
import walknroll.zoodini.models.nonentities.InteriorWall;

/**
 * A class representing the 2D grid of the game
 */
public class Grid {
    /**
     * The internal tile state.
     * Each tile on the board has a set of attributes associated with it.
     */
    private static class TileState {
        /** Whether the tile has been visited (for pathfinding) */
        public boolean visited = false;
        /** Whether the tile is a wall */
        public boolean wall = false;
        /** Whether the tile is a goal (for pathfinding) */
        public boolean goal = false;
    }

    /** The board width (in number of tiles) */
    private int width;
    /** The board height (in number of tiles) */
    private int height;
    /** The tile size (px)*/
    private float tileSize;
    /** The grid of booleans representing the map (True if wall, False otherwise) */
    private TileState[][] grid;
    /** The scaling factor between physics and screen coordinates */
    private Vector2 scale;

    private float boundsX;
    private float boundsY;

    public Grid(Rectangle bounds, PooledList<Obstacle> obstacles, PooledList<ObstacleSprite> obstacleSprites, float cellsPerUnit) {

        // Determine tile size in physics units
        this.tileSize = 1.0f / cellsPerUnit;

        // Calculate grid dimensions
        this.width = (int)(bounds.getWidth() * cellsPerUnit);
        this.height = (int)(bounds.getHeight() * cellsPerUnit);

        this.boundsX = bounds.getX();
        this.boundsY = bounds.getY();

        // Initialize grid
        this.grid = new TileState[width][height];

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                grid[i][j] = new TileState();
            }
        }

        initializeFromLevel(obstacles, obstacleSprites);
    }

    /**
     * Initialize the grid with walls from the level model
     * Precondition: The level model has been initialized
     * and obstacles[i] corresponds to obstacleSprites[i]
     *
     * @param obstacles The list of obstacles in the level
     * @param obstacleSprites The list of obstacle sprites in the level
     *
     */
    private void initializeFromLevel(PooledList<Obstacle> obstacles, PooledList<ObstacleSprite> obstacleSprites) {
        for (int i = 0; i < obstacles.size(); i ++) {
            Obstacle obstacle = obstacles.get(i);
            ObstacleSprite sprite = obstacleSprites.get(i);
            if (!(sprite instanceof InteriorWall) && !(sprite instanceof ExteriorWall)) {
                continue;
            }
            if (sprite instanceof InteriorWall) {
                Vector2 position = new Vector2(obstacle.getX(), obstacle.getY());
                if (!(obstacle instanceof BoxObstacle)) {
                    DebugPrinter.println("No box obstacle");
                    continue;
                }
                Vector2 dimension = new Vector2(((BoxObstacle) obstacle).getWidth(),
                    ((BoxObstacle) obstacle).getHeight());

                // Add a small epsilon to ensure boundary walls are detected
                float epsilon = 0.001f;

                // Convert to grid coordinates
                int startX = physicsToGridX(position.x - dimension.x / 2 - epsilon);
                int startY = physicsToGridY(position.y - dimension.y / 2 - epsilon);
                int endX = physicsToGridX(position.x + dimension.x / 2 - epsilon);
                int endY = physicsToGridY(position.y + dimension.y / 2 - epsilon);

                // Mark all covered cells as walls
                for (int x = startX; x <= endX; x++) {
                    for (int y = startY; y <= endY; y++) {
                        if (inBounds(x, y)) {
                            grid[x][y].wall = true;
                        }
                    }
                }
            }
        }
    }

    /**
     * Resets the values of the grid to false
     */
    public void resetGrid() {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                grid[i][j].visited = false;
                grid[i][j].goal = false;
            }
        }
    }

    /**
     * Gets the width of the grid in cells
     */
    public int getWidth() {
        return width;
    }

    /**
     * Gets the height of the grid in cells
     */
    public int getHeight() {
        return height;
    }

    /**
     * Gets the tile state at the specified grid coordinates
     *
     * @param x The x grid coordinate
     * @param y The y grid coordinate
     * @return The tile state or null if out of bounds
     */
    private TileState getTileState(int x, int y) {
        if (!inBounds(x, y)) {
            return null;
        }
        return grid[x][y];
    }

    /**
     * Checks if the specified grid cell is a wall
     *
     * @param x The x grid coordinate
     * @param y The y grid coordinate
     * @return True if the cell is a wall, false otherwise
     */
    public boolean isWall(int x, int y) {
        return grid[x][y].wall;
    }

    /**
     * Checks if the specified grid cell has been visited during pathfinding
     *
     * @param x The x grid coordinate
     * @param y The y grid coordinate
     * @return True if the cell has been visited, false otherwise
     */
    public boolean isVisited(int x, int y) {
        return grid[x][y].visited;
    }

    /**
     * Sets the specified grid cell to be visited during pathfinding
     *
     * @param x The x grid coordinate
     * @param y The y grid coordinate
     */
    public void setVisited(int x, int y) {
        grid[x][y].visited = true;
    }


    /**
     * Checks if the specified grid cell is a goal for pathfinding
     *
     * @param x The x grid coordinate
     * @param y The y grid coordinate
     * @return True if the cell is a goal, false otherwise
     */
    public boolean isGoal(int x, int y) {
        return grid[x][y].goal;
    }

    /**
     * Converts screen coordinates to grid coordinates
     *
     * @param screenX The x coordinate in screen space
     * @return The corresponding x coordinate in grid space
     */
    public int screenToGridX(float screenX) {
        float physicsX = screenX / scale.x;
        return physicsToGridX(physicsX);
    }

    /**
     * Converts screen coordinates to grid coordinates
     *
     * @param screenY The y coordinate in screen space
     * @return The corresponding y coordinate in grid space
     */
    public int screenToGridY(float screenY) {
        float physicsY = screenY / scale.y;
        return physicsToGridY(physicsY);
    }

    /**
     * Checks if grid coordinates are within bounds
     *
     * @param x The x grid coordinate
     * @param y The y grid coordinate
     * @return True if the coordinates are within the grid bounds
     */
    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    /**
     * Makrs the specified grid cell as a goal for pathfinding
     *
     * @param x The x grid coordinate
     * @param y The y grid coordinate
     */
    public void setGoal(int x, int y) {
        if (!inBounds(x, y)) {
            return;
        }
        grid[x][y].goal = true;
    }

    /**
     * Clears all visited marks and goals for pathfinding
     */
    public void clearMarks() {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                grid[i][j].visited = false;
                grid[i][j].goal = false;
            }
        }
    }

    /**
     * Converts physics coordinates to grid coordinates
     *
     * @param physicsX The x coordinate in physics space
     * @return The corresponding x coordinate in grid space
     */
    public int physicsToGridX(float physicsX) {
        return (int)((physicsX - boundsX) / tileSize);
    }

    /**
     * Converts physics coordinates to grid coordinates
     *
     * @param physicsY The y coordinate in physics space
     * @return The corresponding y coordinate in grid space
     */
    public int physicsToGridY(float physicsY) {
        return (int)((physicsY - boundsY) / tileSize);
    }

    public void printGoalCoords() {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (grid[i][j].goal) {
//                    DebugPrinter.println("Goal: " + i + " " + j);
                }
            }
        }
    }

    public void printGrid() {
        boolean gFlag = false;
        // DebugPrinter.println(width);
        // DebugPrinter.println(height);
        for (int j = height - 1; j >= 0; j--) {
            String[] row = new String[width];
            for (int i = 0; i < width; i++) {
                if (grid[i][j].goal) {
                    row[i] = "G";
                    gFlag = true;
                } else {
                    row[i] = grid[i][j].wall ? "X" : "O";
                }
            }
            // DebugPrinter.println(String.join(" ", row));
        }
        // DebugPrinter.println("Goal: " + gFlag);
    }

    public boolean isGoalSet() {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (grid[i][j].goal) {
                    return true;
                }
            }
        }
        return false;
    }

}

