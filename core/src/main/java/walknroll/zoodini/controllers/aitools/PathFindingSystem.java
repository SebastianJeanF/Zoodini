package walknroll.zoodini.controllers.aitools;


import walknroll.zoodini.controllers.aitools.*;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;

public class PathFindingSystem {
    private TileGraph tileGraph;
    private TileSmoothablePath path;
    private ManhattanHeuristic<TileNode> heuristic;
    private IndexedAStarPathFinder pathFinder;

    public PathFindingSystem(TileGraph tileGraph) {
        this.tileGraph = tileGraph;
        this.heuristic = new ManhattanHeuristic<TileNode>();
        this.path = null;
        this.pathFinder = new IndexedAStarPathFinder<TileNode>(tileGraph);

    }
}
