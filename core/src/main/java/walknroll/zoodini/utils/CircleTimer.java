package walknroll.zoodini.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;

public class CircleTimer {
    private float progress; // 0 to 1
    private float x, y, radius;
    private Color color;
    private ShapeRenderer shapeRenderer;

    public CircleTimer(float x, float y, float radius, Color color) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
        this.progress = 0;
        this.shapeRenderer = new ShapeRenderer();
    }

    public void setProgress(float progress) {
        this.progress = MathUtils.clamp(progress, 0, 1);
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void draw() {

        // Enable blending for transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight()));
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        System.out.println("CircleTimer drawing at position: (" + x + ", " + y + ") with radius: " + radius);
        System.out.println("Screen dimensions: " + Gdx.graphics.getWidth() + "x" + Gdx.graphics.getHeight());

        // Draw background circle
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.7f);
        shapeRenderer.circle(x, y, radius);

        // Draw progress arc
        shapeRenderer.setColor(color);
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
            shapeRenderer.triangle(
                x, y,
                x + radius * MathUtils.cos(currentRad), y + radius * MathUtils.sin(currentRad),
                x + radius * MathUtils.cos(nextRad), y + radius * MathUtils.sin(nextRad)
            );

            currentAngle = nextAngle;
        }

        shapeRenderer.end();
    }

    public void dispose() {
        shapeRenderer.dispose();
    }
}
