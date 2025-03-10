package edu.cornell.cis3152.lighting.utils;

import com.badlogic.gdx.ai.pfa.*;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.*;
import edu.cornell.gdiac.physics2.Obstacle;
import java.util.*;


public class Pathfinder {
    private Graph graph;
    private DistanceHeuristic heuristic;
    private final int ROWS;
    private final int COLS;
    private IndexedAStarPathFinder<Node> aStarPathFinder;
    private final float startX;
    private final float startY;

    public Pathfinder(int rows, int cols, float startX, float startY, List<Obstacle> obstacles) {
        this.ROWS = rows;
        this.COLS = cols;
        this.startX = startX;
        this.startY = startY;
        this.initializeGraph(obstacles);
    }

    /**
     * Generates connections between nodes in the pathfinding graph.
     * Connects nodes that are adjacent (orthogonally or diagonally) and not obstacles.
     * For diagonal connections, prevents corner-cutting through obstacles.
     */
    public void addEdges(Array<Node> nodes) {
        // For each node in the provided array
        for (Node node : nodes) {
            // Skip obstacle nodes - they don't need connections
            if (node.isObstacle) continue;

            // Check potential connections to all other nodes
            for (int i = 0; i < nodes.size; i++) {
                Node targetNode = nodes.get(i);

                // Skip if the target is an obstacle or is the same node
                if (targetNode.isObstacle || node == targetNode) continue;

                // Check if the target is within connect distance (1.5 units)
                float distance = node.tilePos.dst(targetNode.tilePos);
                if (distance > 1.5f) continue;

                // If it's a diagonal connection, perform corner-cutting check
                if (node.tilePos.x != targetNode.tilePos.x && node.tilePos.y != targetNode.tilePos.y) {
                    // Get the two corner nodes that would be cut through
                    Node cornerNode1 = getNodeAt(
                        Math.round(node.tilePos.x),
                        Math.round(targetNode.tilePos.y)
                    );

                    Node cornerNode2 = getNode(
                        Math.round(targetNode.tilePos.x),
                        Math.round(node.tilePos.y)
                    );

                    // If either corner is an obstacle, don't allow diagonal movement
                    if (cornerNode1.isObstacle || cornerNode2.isObstacle) {
                        continue;
                    }
                }

                // Add a connection from the current node to the target node
                node.connections.add(new NodeConn(node, targetNode));
            }
        }
    }

    /**
     * Adds a node to the pathfinding graph at the specified position.
     * This method marks the node as not an obstacle and updates connections
     * for it and its neighbors.
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

        // Add all 8-way neighbors
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                // Skip the center node (original position)
                if (i == 0 && j == 0) continue;

                // Calculate neighbor position
                Vector2 neighborPos = new Vector2(
                    pos.x + i * Terrain.TILE_SIZE,
                    pos.y + j * Terrain.TILE_SIZE
                );

                // Find the node at this position
                Node neighbor = getNode(neighborPos);

                // Add to neighbors list if it exists
                if (neighbor != null) {
                    neighbors.add(neighbor);
                }
            }
        }

        // Regenerate connections for the node and its neighbors
        addEdges(neighbors);
    }



    /**
     * Initializes the graph representing the game world
     * Adds nodes and edges to the graph and initializes the A* path finder
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

        for (Obstacle obs: obstacles) {
            Vector2 pos = obs.getPosition();
            Node node = getNode(pos);
            if (node != null) {
                node.isObstacle = true;
            }
        }

        this.addEdges(nodes);
        this.graph = new Graph(nodes);

        // Add walls to graph by reading game objects

        addEdges(nodes);
        this.aStarPathFinder = new IndexedAStarPathFinder<>(graph);
    }


    /**
     * Converts a world position to the index of the node in the graph
     *
     * @param pos The world position to convert
     * @return The index of the node in the graph
     */

    public Vector2 worldToGraphIndex(Vector2 pos) {
        Vector2 roundedPos = pos.cpy().set(Math.round(pos.x), Math.round(pos.y));
        Vector2 integerPoint = roundedPos.sub(this.startX, this.startY).scl(1.0F / Terrain.TILE_SIZE);
        integerPoint.set(Math.round(integerPoint.x), Math.round(integerPoint.y));
        return integerPoint;
    }

    public List<Node> getPath(Vector2 currPos, Vector2 targetPos) {
        GraphPath<Node> graphPath = new DefaultGraphPath<>();
        Node start = getNode(currPos);
        Node end = getNode(targetPos);
        this.aStarPathFinder.searchNodePath(start, end, this.heuristic, graphPath);

        List<Node> path = new ArrayList<>();
        for (Node node : graphPath) {
            if (!node.equals(start)) {
                path.add(node);
            }
        }
        return path;
    }


    public class DistanceHeuristic implements Heuristic<Node> {
        public DistanceHeuristic() {}
        public float estimate(Node node, Node endNode) {
            return node.tileCoords.dst(endNode.tileCoords);
        }
    }

    private Node getNode(Vector2 pos) {
        Vector2 graphIndex = this.worldToGraphIndex(pos);
        int x = Math.clamp(Math.round(graphIndex.x), 0, this.COLS - 1);
        int y = Math.clamp(Math.round(graphIndex.y), 0, this.ROWS - 1);
        return x >= 0 && x < this.COLS && y >= 0 && y < this.ROWS? (Node)this.graph.getNodes().get(y * this.COLS + x) : null;
    }


    /**
     * A class representing the graph of the game world
     */
    private class Graph implements IndexedGraph<Node> {
        private final Array<Node> nodes;

        public Graph(Array<Node> nodes) {
            this.nodes = nodes;
        }

        public int getIndex(Node node) {
            return nodes.indexOf(node, true);
        }

        public int getNodeCount() {
            return nodes.size;
        }

        public Array<Connection<Node>> getConnections(Node node) {
            return node.edges;
        }

        public Array<Node> getNodes() {
            return this.nodes;
        }
    }


    public class Node {
        public Vector2 tileCoords;
        public boolean isObstacle;
        public Array<Connection<Node>> edges = new Array<>();

        public Node(Vector2 tileCoords, boolean isObstacle) {
            this.tileCoords = tileCoords;
            this.isObstacle = isObstacle;
        }

        public Vector2 getWorldPosition() {

        }
    }

    public class Edge implements Connection<Node> {
        private final Node fromNode;
        private final Node toNode;

        public Edge(Node fromNode, Node toNode) {
            this.fromNode = fromNode;
            this.toNode = toNode;
        }

        public float getCost() {

        }

        public Node getFromNode() {
            return this.fromNode;
        }

        public Node getToNode() {
            return this.toNode;
        }
    }

}

