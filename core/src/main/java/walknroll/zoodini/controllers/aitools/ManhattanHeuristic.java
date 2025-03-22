package walknroll.zoodini.controllers.aitools;

import com.badlogic.gdx.ai.pfa.Heuristic;

public class ManhattanHeuristic<N extends TileNode> implements Heuristic<N> {

    @Override
    public float estimate(N node, N endNode) {
        return Math.abs(endNode.x - node.x) + Math.abs(endNode.y - node.y);
    }
}
