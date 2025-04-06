package walknroll.zoodini.controllers.aitools;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.SmoothableGraphPath;
import com.badlogic.gdx.math.Vector2;
import java.util.Spliterator;
import java.util.function.Consumer;

public class TileSmoothablePath <N extends TileNode> extends DefaultGraphPath<N> implements
    SmoothableGraphPath<N, Vector2> {

    private Vector2 cache = new Vector2();

    @Override
    public Vector2 getNodePosition(int index) {
        N node = nodes.get(index);
        return cache.set(node.x, node.y);
    }

    @Override
    public void swapNodes(int index1, int index2) {
        nodes.set(index1, nodes.get(index2));
    }

    @Override
    public void truncatePath(int newLength) {
        nodes.truncate(newLength);
    }
}
