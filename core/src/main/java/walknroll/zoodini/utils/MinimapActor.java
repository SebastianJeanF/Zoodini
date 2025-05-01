package walknroll.zoodini.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Disposable;
import edu.cornell.gdiac.physics2.BoxObstacle;
import walknroll.zoodini.models.GameLevel;
import walknroll.zoodini.models.entities.Avatar;
import walknroll.zoodini.models.entities.Guard;
import walknroll.zoodini.models.nonentities.Door;
import walknroll.zoodini.models.nonentities.Exit;
import walknroll.zoodini.models.nonentities.InteriorWall;
import walknroll.zoodini.utils.ZoodiniSprite;

public class MinimapActor extends Actor implements Disposable {
    // Minimap display parameters
    private final float MINIMAP_SIZE = 150f; // Size in pixels
    private final float BORDER_SIZE = 2f;    // Border thickness

    // Colors for different entities on the minimap
    private final Color BACKGROUND_COLOR = new Color(0.1f, 0.1f, 0.1f, 0.7f);
    private final Color BORDER_COLOR = new Color(0.5f, 0.5f, 0.5f, 1f);
    private final Color WALL_COLOR = new Color(0.3f, 0.3f, 0.3f, 1f);
    private final Color CAT_COLOR = new Color(0.9f, 0.8f, 0.2f, 1f);
    private final Color OCTOPUS_COLOR = new Color(0.2f, 0.5f, 0.9f, 1f);
    private final Color GUARD_COLOR = new Color(0.9f, 0.2f, 0.2f, 1f);
    private final Color DOOR_COLOR = new Color(0.6f, 0.4f, 0.2f, 1f);
    private final Color EXIT_COLOR = new Color(0.2f, 0.9f, 0.2f, 1f);
    private int updateCounter = 0;
    private static final int UPDATE_FREQUENCY = 10;


    // Reference to game level for accessing entities
    private GameLevel level;

    // Texture for the minimap
    private Texture minimapTexture;
    private Pixmap pixmap;

    // Flag to check if we need to redraw the map
    private boolean needsRedraw = true;

    // The visible player camera area on the minimap
    private float cameraViewportWidth;
    private float cameraViewportHeight;

    // The scale factor to convert from world units to minimap pixels
    private float scaleFactorX;
    private float scaleFactorY;

    public MinimapActor(GameLevel level) {
        this.level = level;
        setSize(MINIMAP_SIZE + 2 * BORDER_SIZE, MINIMAP_SIZE + 2 * BORDER_SIZE);

        // Initialize the pixmap and texture
        createMinimapTexture();
    }

    private void createMinimapTexture() {
        // Create a pixmap for the minimap
        pixmap = new Pixmap((int)(MINIMAP_SIZE + 2 * BORDER_SIZE),
            (int)(MINIMAP_SIZE + 2 * BORDER_SIZE),
            Pixmap.Format.RGBA8888);

        // Create a texture from the pixmap
        minimapTexture = new Texture(pixmap);
    }

    /**
     * Draws the minimap, including level bounds, walls, and entities.
     */
    private void drawMinimap() {
        if (pixmap == null) return;

        // Clear the pixmap
        pixmap.setColor(BORDER_COLOR);
        pixmap.fill();

        // Draw the background
        pixmap.setColor(BACKGROUND_COLOR);
        pixmap.fillRectangle((int)BORDER_SIZE, (int)BORDER_SIZE,
            (int)MINIMAP_SIZE, (int)MINIMAP_SIZE);

        // Calculate the scale factor based on the level bounds
        float levelWidth = level.getBounds().width;
        float levelHeight = level.getBounds().height;
        scaleFactorX = MINIMAP_SIZE / levelWidth;
        scaleFactorY = MINIMAP_SIZE / levelHeight;

        // We'll implement the actual drawing of level elements in the next step
        for (ZoodiniSprite sprite : level.getSprites()) {
            if (sprite instanceof InteriorWall) {
                drawWall((InteriorWall) sprite);
            } else if (sprite instanceof Door) {
                drawDoor((Door) sprite);
            } else if (sprite instanceof Exit) {
                drawExit((Exit) sprite);
            }
        }

        // Update the minimap texture
        minimapTexture.dispose();
        minimapTexture = new Texture(pixmap);

        needsRedraw = false;
    }

    /**
     * Converts a world position to minimap coordinates
     */
    private Vector2 worldToMinimap(float worldX, float worldY) {
        // Adjust for level bounds offset and scale to minimap size
        float minimapX = (worldX - level.getBounds().x) * scaleFactorX + BORDER_SIZE;
        float minimapY = (worldY - level.getBounds().y) * scaleFactorY + BORDER_SIZE;

        // Flip Y coordinate because pixmap coordinates are top-down
        minimapY = getHeight() - minimapY;

        return new Vector2(minimapX, minimapY);
    }

    /**
     * Draws a wall on the minimap
     */
    private void drawWall(InteriorWall wall) {
        Vector2 position = wall.getPosition();
        float width = wall.getWidth();
        float height = wall.getHeight();

        // Convert world coordinates to minimap coordinates
        Vector2 minimapPos = worldToMinimap(position.x - width/2, position.y - height/2);
        float minimapWidth = width * scaleFactorX;
        float minimapHeight = height * scaleFactorY;

        // Draw the wall
        pixmap.setColor(WALL_COLOR);
        pixmap.fillRectangle(
            (int)minimapPos.x,
            (int)(minimapPos.y - minimapHeight), // Adjust for top-down coordinates
            (int)minimapWidth,
            (int)minimapHeight
        );
    }

    /**
     * Draws a door on the minimap
     */
    private void drawDoor(Door door) {
        Vector2 position = door.getObstacle().getPosition();
        float size = 1.0f;  // Default size if we can't determine actual size

        if (door.getObstacle() instanceof BoxObstacle) {
            BoxObstacle boxObstacle = (BoxObstacle) door.getObstacle();
            size = Math.max(boxObstacle.getWidth(), boxObstacle.getHeight());
        }

        // Convert world coordinates to minimap coordinates
        Vector2 minimapPos = worldToMinimap(position.x, position.y);
        float minimapSize = size * scaleFactorX;

        // Draw the door
        pixmap.setColor(DOOR_COLOR);
        pixmap.fillRectangle(
            (int)(minimapPos.x - minimapSize/2),
            (int)(minimapPos.y - minimapSize/2),
            (int)minimapSize,
            (int)minimapSize
        );
    }

    /**
     * Draws an exit on the minimap
     */
    private void drawExit(Exit exit) {
        Vector2 position = exit.getObstacle().getPosition();
        float size = 1.0f; // Default size

        // Try to get size information from obstacle
        if (exit.getObstacle() instanceof BoxObstacle) {
            BoxObstacle boxObstacle = (BoxObstacle) exit.getObstacle();
            size = Math.max(boxObstacle.getWidth(), boxObstacle.getHeight());
        }

        // Convert world coordinates to minimap coordinates
        Vector2 minimapPos = worldToMinimap(position.x, position.y);
        float minimapSize = size * scaleFactorX;

        // Draw the exit
        pixmap.setColor(EXIT_COLOR);
        pixmap.fillRectangle(
            (int)(minimapPos.x - minimapSize/2),
            (int)(minimapPos.y - minimapSize/2),
            (int)minimapSize,
            (int)minimapSize
        );
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // Check if we need to redraw the minimap
        if (needsRedraw) {
            drawMinimap();
        }

        // Draw the minimap texture
        batch.draw(minimapTexture, getX(), getY(), getWidth(), getHeight());

        // Draw dynamic entities (players, guards)
        drawDynamicEntities(batch);
    }


    /**
     * Draws dynamic entities like players and guards that need to be updated every frame
     */
    private void drawDynamicEntities(Batch batch) {
        Color originalColor = batch.getColor().cpy();

        // Draw the cat
        Vector2 catPos = level.getCat().getPosition();
        Vector2 minimapCatPos = worldToMinimap(catPos.x, catPos.y);

        batch.setColor(CAT_COLOR);
        drawDot(batch, minimapCatPos.x, minimapCatPos.y, 3f);

        // Draw the octopus
        Vector2 octopusPos = level.getOctopus().getPosition();
        Vector2 minimapOctopusPos = worldToMinimap(octopusPos.x, octopusPos.y);

        batch.setColor(OCTOPUS_COLOR);
        drawDot(batch, minimapOctopusPos.x, minimapOctopusPos.y, 3f);

        // Draw guards
        batch.setColor(GUARD_COLOR);
        for (Guard guard : level.getGuards()) {
            Vector2 guardPos = guard.getPosition();
            Vector2 minimapGuardPos = worldToMinimap(guardPos.x, guardPos.y);
            drawDot(batch, minimapGuardPos.x, minimapGuardPos.y, 2.5f);
        }

        // Restore original color
        batch.setColor(originalColor);
    }

    /**
     * Helper method to draw a simple dot
     */
    private void drawDot(Batch batch, float x, float y, float size) {
        // Since we don't have a circle texture, we'll use a simple rectangle for now
        // In a real implementation, you might want to use a circle texture
        batch.draw(minimapTexture, x - size/2, y - size/2, size, size,
            0, 0, 1, 1); // Use a 1x1 pixel from the texture
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        updateCounter++;
        if (updateCounter >= UPDATE_FREQUENCY) {
            needsRedraw = true;
            updateCounter = 0;
        }
    }

    @Override
    public void dispose() {
        if (minimapTexture != null) {
            minimapTexture.dispose();
        }
        if (pixmap != null) {
            pixmap.dispose();
        }
    }
}
