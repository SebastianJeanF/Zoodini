package walknroll.zoodini.controllers.aitools;

import com.badlogic.gdx.ai.pfa.DefaultConnection;

public class TileEdge extends DefaultConnection<TileNode> {

    static final float NON_DIAGONAL_COST = (float) Math.sqrt(2);

    TileNode from;
    TileNode to;
    float cost;

    TileGraph graph;

    public TileEdge(TileGraph graph, TileNode from, TileNode to) {
        super(from, to);
        this.from = from;
        this.to = to;
        this.cost = cost;
        this.graph = graph;
    }

    @Override
    public float getCost() {
        int dx = Math.abs(to.x - from.x);
        int dy = Math.abs(to.y - from.y);
        return dx + dy;
        // if (graph.diagonal) return 1;
        // return getToNode().x != graph.startNode.x && getToNode().y !=
        // graph.startNode.y ? NON_DIAGONAL_COST : 1;
    }

    @Override
    public TileNode getFromNode() {
        return from;
    }

    @Override
    public TileNode getToNode() {
        return to;
    }
}
