package walknroll.zoodini.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.math.Poly2;
import edu.cornell.gdiac.math.PolyFactory;

public class CircleTimer {
    private float progress; // 0 to 1
    private float x, y, radius;
    private Color color;

    public CircleTimer(float x, float y, float radius, Color color) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
        this.progress = 0;
    }

    public void setProgress(float progress) {
        this.progress = MathUtils.clamp(progress, 0, 1);
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    PolyFactory polyFactory = new PolyFactory();
    public void draw(SpriteBatch batch) {

        // Enable blending for transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight()));
        batch.begin();

        // Draw background circle
        batch.setColor(0.2f, 0.2f, 0.2f, 0.7f);
        Poly2 circle = polyFactory.makeNgon(0,0,radius,12);
        batch.fill(circle, x, y);

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
            Poly2 triangle = polyFactory.makeTriangle(
                x, y,
                x + radius * MathUtils.cos(currentRad), y + radius * MathUtils.sin(currentRad),
                x + radius * MathUtils.cos(nextRad), y + radius * MathUtils.sin(nextRad)
            );
            batch.fill(
                triangle
            );

            currentAngle = nextAngle;
        }

        batch.end();
    }

    public void dispose() {

    }
}
