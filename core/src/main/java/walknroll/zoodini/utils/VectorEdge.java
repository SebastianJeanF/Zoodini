package walknroll.zoodini.utils;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.math.Vector2;

public class VectorEdge implements Connection<Vector2> {

    Vector2 from;
    Vector2 to;
    float cost;

    public VectorEdge(Vector2 from, Vector2 to){
        this.from = from;
        this.to = to;
        this.cost = from.dst(to);
    }

    @Override
    public float getCost() {
        return cost;
    }

    @Override
    public Vector2 getFromNode() {
        return from;
    }

    @Override
    public Vector2 getToNode() {
        return to;
    }
}
