package walknroll.zoodini.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.math.Poly2;
import edu.cornell.gdiac.math.PolyFactory;

public class CircleTimer {
    private float progress; // 0 to 1
    private float x, y, radius;
    private Color color;
    private float units;
    private Poly2 circle;
    private Poly2 triangle;
    private float[] triangleVertices;

    public CircleTimer(float radius, Color color, float units) {
        this.radius = radius;
        this.color = color;
        this.progress = 0;
        this.units = units;
        circle = polyFactory.makeNgon(0,0,radius, 36);
        triangle = polyFactory.makeTriangle(0,0,0,0,0,0);
        triangleVertices = new float[6];
    }

    public void setProgress(float progress) {
        this.progress = MathUtils.clamp(progress, 0, 1);
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void setPosition(Vector2 v){
        this.x = v.x;
        this.y = v.y;
    }

    PolyFactory polyFactory = new PolyFactory();
    Affine2 affineCache = new Affine2();
    public void draw(SpriteBatch batch) {
        affineCache.idt();
        affineCache.scale(units, units);
        affineCache.translate(x,y);

        Color prevColor = batch.getColor();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Draw background circle
        batch.setColor(0.2f, 0.2f, 0.2f, 0.7f);
        batch.fill(circle, affineCache);

        // Draw progress arc
        batch.setColor(color);
        float angleStart = 90; // Start from top
        float angleEnd = angleStart - 360 * progress;

        // Draw arc segments
        int segments = 36;
        float angleIncrement = 360f / segments;
        float currentAngle = angleStart;

        while (currentAngle > angleEnd) {
            float nextAngle = Math.max(currentAngle - angleIncrement, angleEnd);
            float currentRad = currentAngle * MathUtils.degreesToRadians;
            float nextRad = nextAngle * MathUtils.degreesToRadians;

            // Draw triangle to approximate arc segment
//            Poly2 triangle = polyFactory.makeTriangle(
//                0, 0,
//                radius * MathUtils.cos(currentRad), radius * MathUtils.sin(currentRad),
//                radius * MathUtils.cos(nextRad), radius * MathUtils.sin(nextRad)
//            );
            int idx = 2;
            triangle.vertices.ensureCapacity(6);
            triangle.vertices.items[idx++] = radius * MathUtils.cos(currentRad);
            triangle.vertices.items[idx++] = radius * MathUtils.sin(currentRad);
            triangle.vertices.items[idx++] = radius * MathUtils.cos(nextRad);
            triangle.vertices.items[idx++] = radius * MathUtils.sin(nextRad);
            batch.fill(
                triangle, affineCache
            );

            currentAngle = nextAngle;
        }

        batch.setColor(prevColor);
    }

    public void dispose() {

    }
}
