package edu.cornell.cis3152.lighting.utils;

import com.badlogic.gdx.ai.pfa.*;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.*;
import edu.cornell.gdiac.physics2.Obstacle;

import java.lang.StringBuilder;
import java.util.*;

public class GameGraph {
    /** The graph representing the game world */
    private Graph graph;
    /** The heuristic used by the A* algorithm */
    private DistanceHeuristic heuristic;
    /** The number of rows in the grid */
    private final int ROWS;
    /** The number of columns in the grid */
    private final int COLS;
    /** The A* pathfinder used to find paths in the graph */
    private IndexedAStarPathFinder<Node> aStarPathFinder;
    /** The x-coordinate of the bottom-left corner of the grid in world coordinates */
    private final float startX;
    /** The y-coordinate of the bottom-left corner of the grid in world coordinates */
    private final float startY;
    /** The size of each tile in the grid */
    private final float TERRAIN_TILE_SIZE = 1.0f;

    /**
     * Constructs a new GameGraph with the specified dimensions and obstacles.
     *
     * @param rows The number of rows in the grid
     * @param cols The number of columns in the grid
     * @param startX The x-coordinate of the bottom-left corner of the grid in world coordinates
     * @param startY The y-coordinate of the bottom-left corner of the grid in world coordinates
     * @param obstacles A list of obstacle objects that should be considered impassable
     */
    public GameGraph(int rows, int cols, float startX, float startY, List<Obstacle> obstacles) {
        this.ROWS = rows;
        this.COLS = cols;
        this.startX = startX;
        this.startY = startY;
        this.initializeGraph(obstacles);

        this.heuristic = new DistanceHeuristic();
    }

    /**
     * Prints a grid representation of the game graph to standard output.
     *
     * Each cell in the grid corresponds to a node in the game graph.
     * A cell is represented as "X" if the node is an obstacle, or "." if it is passable.
     * The grid is printed row by row, with the top row corresponding to the highest y-coordinate,
     * so that the output visually represents the game world's layout.
     *
     * Preconditions:
     * - The graph must be properly initialized and contain a list of nodes in row-major order.
     * - The number of nodes in the graph must equal ROWS * COLS.
     *
     * Postconditions:
     * - A textual grid is printed to the console that represents the navigability of each node.
     *
     * Invariants:
     * - Each node's obstacle status is not modified during the printing process.
     */
    public void printGrid() {
        // Get the list of nodes from the graph
        Array<Node> nodes = this.graph.getNodes();

        // Loop through rows in reverse order (so that the highest y values appear at the top)
        for (int row = ROWS - 1; row >= 0; row--) {
            StringBuilder line = new StringBuilder();
            // Loop through columns
            for (int col = 0; col < COLS; col++) {
                int index = row * COLS + col;
                Node node = nodes.get(index);
                // Use "X" for obstacles and "." for passable nodes
                line.append(node.isObstacle() ? "X " : ". ");
            }
            System.out.println(line);
        }
    }


    /**
     * Gets the node at the specified world position.
     * Converts the world position to grid coordinates and returns the corresponding node.
     *
     *
     * @INVARIANT this.graph must be initialized
     * @param pos The world position to get the node for
     * @return The node at the specified position, or null if no node exists there
     */
    private Node getNode(Vector2 pos) {
        Vector2 graphIndex = this.worldToGraphIndex(pos);
        int x = MathUtils.clamp(Math.round(graphIndex.x), 0, this.COLS - 1);
        int y = MathUtils.clamp(Math.round(graphIndex.y), 0, this.ROWS - 1);

        System.out.println("Graph Index: (" + x + ", " + y + ")");

        return x >= 0 && x < this.COLS && y >= 0 && y < this.ROWS
            ? this.graph.getNodes().get(y * this.COLS + x)
            : null;
    }

    /**
     *
     * Generates edges between nodes in the pathfinding graph.
     * Connects nodes that are adjacent (orthogonally or diagonally) and not obstacles.
     * For diagonal connections, prevents corner-cutting through obstacles.
     *
     * @INVARIANT this.graph must be initialized
     * @param nodes The array of nodes to process and generate connections for
     */
    public void addEdges(Array<Node> nodes) {
        for (Node node : nodes) {
            // Don't add edge to obstacles
            if (node.isObstacle) {
                continue;
            }
            // Check potential connections to all other nodes
            for (int i = 0; i < nodes.size; i++) {
                Node targetNode = nodes.get(i);
                // Don't add edge to obstacles or self
                if (targetNode.isObstacle || node == targetNode) continue;
                // Check if the target is within connect distance (1.5 units)
                float distance = node.tileCoords.dst(targetNode.tileCoords);
                if (distance > 1.5f) {
                    continue;
                }
                // If it's a diagonal connection, perform corner-cutting check
                if (node.tileCoords.x != targetNode.tileCoords.x && node.tileCoords.y != targetNode.tileCoords.y) {
                    // Get the two corner nodes that would be cut through
                    Node cornerNode1 = getNode(
                        new Vector2(Math.round(node.tileCoords.x),
                        Math.round(targetNode.tileCoords.y))
                    );
                    Node cornerNode2 = getNode(
                        new Vector2(Math.round(targetNode.tileCoords.x),
                        Math.round(node.tileCoords.y))
                    );
                    // If either corner is an obstacle, don't allow diagonal movement
                    if (cornerNode1 != null && cornerNode2 != null && (cornerNode1.isObstacle || cornerNode2.isObstacle)) {
                        continue;
                    }
                }
                // Add a connection from the current node to the target node
                node.edges.add(new Edge(node, targetNode));
            }
        }
    }

    /**
     * Adds a node to the pathfinding graph at the specified position or marks an existing
     * node as passable. Updates connections for the node and its neighbors.
     *
     * @param pos The world position where to add or update a node
     */
    public void addNode(Vector2 pos) {
        // Find the node at the specified position
        Node node = getNode(pos);

        // Mark it as not an obstacle if it exists
        if (node != null) {
            node.isObstacle = false;
        } else {
            return; // Exit if no node found at position
        }

        // Find all neighboring nodes (8-way neighbors)
        Array<Node> neighbors = new Array<>();
        neighbors.add(node); // Include the node itself for connection generation

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) { // Skip the center node (original position)
                    continue;
                }
                Vector2 neighborPos = new Vector2(
                    pos.x + i * TERRAIN_TILE_SIZE,
                    pos.y + j * TERRAIN_TILE_SIZE
                );
                // Find the node at this position
                Node neighbor = getNode(neighborPos);
                if (neighbor != null) {
                    neighbors.add(neighbor);
                }
            }
        }
        // Generate connections for the node and its neighbors
        addEdges(neighbors);
    }


    /**
     * Initializes the graph representing the game world.
     * Creates nodes for each grid cell, marks obstacles, adds edges between nodes,
     * and initializes the A* path finder.
     *
     * @param obstacles A list of obstacle objects to mark as impassable
     */
    private void initializeGraph(List<Obstacle> obstacles) {
        Array<Node> nodes = new Array<>();
        int index = 0;

        for (int r = 0; r < ROWS; r++) {
            for(int c = 0; c < COLS; c++) {
                Node n = new Node(new Vector2(c, r), false);
                nodes.add(n);
                index++;
            }
        }

        this.graph = new Graph(nodes);
        this.addEdges(nodes);

        for (Obstacle obs: obstacles) {
            Vector2 pos = obs.getPosition();
            Node node = getNode(pos);
            if (node != null) {
                node.isObstacle = true;
            }
        }


        // TODO: Add walls to graph by reading game objects

//        addEdges(nodes);
        this.aStarPathFinder = new IndexedAStarPathFinder<>(graph);
    }


    /**
     * Converts a world position to a graph index position.
     * This translates the game world coordinates to the internal grid coordinates.
     *
     * @param pos The world position to convert
     * @return A Vector2 containing the corresponding grid coordinates
     */
    public Vector2 worldToGraphIndex(Vector2 pos) {
        Vector2 roundedPos = pos.cpy().set(Math.round(pos.x), Math.round(pos.y));
        Vector2 integerPoint = roundedPos.sub(this.startX, this.startY).scl(1.0F / TERRAIN_TILE_SIZE);
        integerPoint.set(Math.round(integerPoint.x), Math.round(integerPoint.y));
        return integerPoint;
    }

    /**
     * Finds the shortest path between two positions in the world using A*.
     *
     * @INVARIANT this.heuristic must be initialized
     * @param currPos The starting position in world coordinates
     * @param targetPos The target position in world coordinates
     * @return A list of nodes representing the path from start to target, excluding the start node
     */
    public List<Node> getPath(Vector2 currPos, Vector2 targetPos) {
//        System.out.println(currPos);
//        System.out.println(targetPos);
        GraphPath<Node> graphPath = new DefaultGraphPath<>();
        Node start = getNode(currPos);
        Node end = getNode(targetPos);

        System.out.println("Graph's target: "+ end.getWorldPosition());
        // Check if start or end node is null
        if (start == null || end == null) {
            System.err.println("Error: Start or end node is null.");
            return new ArrayList<>();
        }


        this.aStarPathFinder.searchNodePath(start, end, this.heuristic, graphPath);

        List<Node> path = new ArrayList<>();
        for (Node node : graphPath) {
            if (!node.equals(start)) {
                path.add(node);
            }
            System.out.print(node.getWorldPosition() + " ");
        }
        System.out.print("\n");
        return path;
    }

    /**
     * A heuristic for the A* algorithm that estimates the distance between nodes.
     * This implementation uses Euclidean distance between node grid coordinates.
     */
    public class DistanceHeuristic implements Heuristic<Node> {
        /**
         * Constructs a new DistanceHeuristic.
         */
        public DistanceHeuristic() {}

        /**
         * Estimates the distance from the current node to the goal node.
         * This is used by A* to prioritize which nodes to explore next.
         *
         * @param node The current node
         * @param endNode The goal node
         * @return The estimated distance as a float
         */
        public float estimate(Node node, Node endNode) {
            return node.tileCoords.dst(endNode.tileCoords);
        }
    }


    /**
     * An implementation of LibGDX's IndexedGraph interface that represents
     * the navigable space in the game world as a graph for pathfinding.
     */
    private class Graph implements IndexedGraph<Node> {
        private final Array<Node> nodes;

        /**
         * Constructs a new Graph with the specified nodes.
         *
         * @param nodes The array of nodes that make up the graph
         */
        public Graph(Array<Node> nodes) {
            this.nodes = nodes;
        }

        /**
         * Gets the index of a node in the graph.
         * This is required by the A* algorithm for efficient node lookups.
         *
         * @param node The node to get the index for
         * @return The index of the node
         */
        public int getIndex(Node node) {
            return nodes.indexOf(node, true);
        }

        /**
         * Gets the total number of nodes in the graph.
         *
         * @return The number of nodes
         */
        public int getNodeCount() {
            return nodes.size;
        }

        /**
         * Gets all connections (edges) leading from the specified node.
         *
         * @param node The node to get connections for
         * @return An array of connections from the node
         */
        public Array<Connection<Node>> getConnections(Node node) {
            return node.edges;
        }

        /**
         * Gets all nodes in the graph.
         *
         * @return An array containing all nodes
         */
        public Array<Node> getNodes() {
            return this.nodes;
        }
    }

    /**
     * Represents a single position in the game world that can be used for pathfinding.
     * Nodes form the vertices of the pathfinding graph.
     */
    public class Node {
        private Vector2 tileCoords;
        private boolean isObstacle;
        public Array<Connection<Node>> edges = new Array<>();

        /**
         * Constructs a new Node at the specified grid coordinates.
         *
         * @param tileCoords The coordinates of this node in the grid
         * @param isObstacle Whether this node represents an impassable obstacle
         */
        public Node(Vector2 tileCoords, boolean isObstacle) {
            this.tileCoords = tileCoords;
            this.isObstacle = isObstacle;
        }

        /**
         * Gets the grid coordinates of this node.
         *
         * @return The grid coordinates as a Vector2
         */
        public Vector2 getTileCoords() {
            return this.tileCoords;
        }

        /**
         * Checks if this node represents an obstacle.
         *
         * @return true if this node is an obstacle, false otherwise
         */
        public boolean isObstacle() {
            return this.isObstacle;
        }


        /**
         * Converts the node's grid coordinates to world coordinates.
         *
         * @return The position of this node in world coordinates
         */
        public Vector2 getWorldPosition() {
            return new Vector2(
                this.tileCoords.x * TERRAIN_TILE_SIZE + GameGraph.this.startX,
                this.tileCoords.y * TERRAIN_TILE_SIZE + GameGraph.this.startY);
        }
    }

    /**
     * Represents a connection between two nodes in the pathfinding graph.
     * Implements LibGDX's Connection interface to work with the A* algorithm.
     */
    public class Edge implements Connection<Node> {
        private final Node fromNode;
        private final Node toNode;

        /**
         * Constructs a new Edge between the specified nodes.
         *
         * @param fromNode The source node of the edge
         * @param toNode The destination node of the edge
         */
        public Edge(Node fromNode, Node toNode) {
            this.fromNode = fromNode;
            this.toNode = toNode;
        }

        /**
         * Gets the cost of traversing this edge.
         * This is typically the Euclidean distance between the nodes.
         *
         * @return The cost as a float
         */
        public float getCost() {
            return this.fromNode.getWorldPosition().cpy().dst(this.toNode.getWorldPosition());
        }

        /**
         * Gets the source node of this edge.
         *
         * @return The source node
         */
        public Node getFromNode() {
            return this.fromNode;
        }

        /**
         * Gets the destination node of this edge.
         *
         * @return The destination node
         */
        public Node getToNode() {
            return this.toNode;
        }
    }


}


