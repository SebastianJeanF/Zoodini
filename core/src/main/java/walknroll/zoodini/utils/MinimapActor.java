package walknroll.zoodini.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Disposable;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.Obstacle;
import walknroll.zoodini.models.GameLevel;
import walknroll.zoodini.models.entities.Guard;
import walknroll.zoodini.models.entities.SecurityCamera;
import walknroll.zoodini.models.nonentities.Door;
import walknroll.zoodini.models.nonentities.Exit;
import walknroll.zoodini.models.nonentities.Key;
import walknroll.zoodini.models.nonentities.Vent;

public class MinimapActor extends Actor implements Disposable {
    // Minimap display parameters
    private final float MINIMAP_SIZE = 200f; // Size in pixels
    private final float BORDER_SIZE = 2f;    // Border thickness

    // Colors for different entities on the minimap
    private final Color BACKGROUND_COLOR = new Color(0.1f, 0.1f, 0.1f, 0.7f);
    private final Color BORDER_COLOR = new Color(0.5f, 0.5f, 0.5f, 1f);
    private final Color WALL_COLOR = new Color(0.3f, 0.3f, 0.3f, 1f);
    private final Color CAT_COLOR = new Color(1f, 0.65f, 0.0f, 1f);
    private final Color OCTOPUS_COLOR = new Color(0.7f, 0.2f, 0.7f, 1f);
    private final Color GUARD_COLOR = new Color(0.9f, 0.2f, 0.2f, 1f);
    private final Color CAMERA_COLOR = new Color(0.9f, 0.8f, 0.2f, 1f);
    private final Color DOOR_COLOR = new Color(0.6f, 0.4f, 0.2f, 1f);
    private final Color EXIT_COLOR = new Color(0.2f, 0.9f, 0.2f, 1f);
    private final Color KEY_COLOR = new Color(0.9f, 0.9f, 0.2f, 1f);
    private final Color VENT_COLOR = new Color(0.5f, 0.23f, 0.26f, 1f);

    private Texture octopusTexture;
    private Texture catTexture;
    private Texture guardTexture;
    private Texture cameraTexture;
    private Texture ventTexture;
    private Texture keyTexture;
    private Texture exitTexture;
    private Texture doorTexture;
    private boolean disabled;

    private int updateCounter = 0;
    private static final int UPDATE_FREQUENCY = 5;

    // Reference to game level for accessing entities
    private GameLevel level;

    // Texture for the minimap
    private Texture minimapTexture;
    private Pixmap pixmap;

    // Flag to check if we need to redraw the map
    private boolean needsRedraw = true;

    // The scale factor to convert from world units to minimap pixels
    private float scaleFactorX;
    private float scaleFactorY;

    // Dot texture for dynamic entities
    private Texture dotTexture;

    public MinimapActor(GameLevel level) {
        this.level = level;
        setSize(MINIMAP_SIZE + 2 * BORDER_SIZE, MINIMAP_SIZE + 2 * BORDER_SIZE);

        // Initialize the pixmap and texture
        createMinimapTexture();
        createDotTexture();
    }

    public void setOctopusTexture(Texture t){
        octopusTexture = t;
    }

    public void setCatTexture(Texture t){
        catTexture = t;
    }

    public void setGuardTexture(Texture t){
        guardTexture = t;
    }

    public void setVentTexture(Texture t){
        ventTexture = t;
    }

    public void setKeyTexture(Texture t) {
        keyTexture = t;
    }

    public void setDoorTexture(Texture t) {
        doorTexture = t;
    }

    public void setCameraTexture(Texture t) {
        cameraTexture = t;
    }

    public void setExitTexture(Texture t) {
        exitTexture = t;
    }




    private void createMinimapTexture() {
        // Create a pixmap for the minimap
        pixmap = new Pixmap((int)(MINIMAP_SIZE + 2 * BORDER_SIZE),
            (int)(MINIMAP_SIZE + 2 * BORDER_SIZE),
            Pixmap.Format.RGBA8888);

        // Create a texture from the pixmap
        minimapTexture = new Texture(new PixmapTextureData(pixmap, null, false, false));
    }

    private void createDotTexture() {
        // Create a small circle texture for dynamic entities
        Pixmap dotPixmap = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
        dotPixmap.setColor(Color.WHITE);
        dotPixmap.fillCircle(4, 4, 3);
        dotTexture = new Texture(dotPixmap);
        dotPixmap.dispose();
    }

    /**
     * Draws the minimap, including level bounds, walls, and entities.
     */
    private void drawMinimap() {
        if (pixmap == null) return;
        // Clear the pixmap
        pixmap.setColor(BORDER_COLOR);
        pixmap.fill();
        pixmap.setColor(BACKGROUND_COLOR);
        pixmap.fillRectangle((int)BORDER_SIZE, (int)BORDER_SIZE,
            (int)MINIMAP_SIZE, (int)MINIMAP_SIZE);

        // Draw the background
        pixmap.setColor(BACKGROUND_COLOR);
        pixmap.fillRectangle((int)BORDER_SIZE, (int)BORDER_SIZE,
            (int)MINIMAP_SIZE, (int)MINIMAP_SIZE);

        // Calculate the scale factor based on the level bounds
        float levelWidth = level.getBounds().width;
        float levelHeight = level.getBounds().height;
        scaleFactorX = MINIMAP_SIZE / levelWidth;
        scaleFactorY = MINIMAP_SIZE / levelHeight;

        // Draw grid
        drawGrid();

        // Direct access to all objects in the level
        for (Obstacle obstacle : level.getObjects()) {
            // Categorize obstacles based on their properties
            boolean isWall = obstacle.getBodyType() == BodyDef.BodyType.StaticBody &&
                !obstacle.isSensor();

            if (isWall) {
                drawObstacle(obstacle);
            }
        }

        // Draw doors directly from doors collection
        drawAllDoors();

        // drawAllKeys()

        // drawExitDirect()

        // Update the minimap texture
        minimapTexture.draw(pixmap, 0, 0);
        needsRedraw = false;
    }

    Color c = new Color(0.3f, 0.3f, 0.3f, 0.5f);
    /**
     * Draws a simple grid on the minimap for reference
     */
    private void drawGrid() {
        pixmap.setColor(c);

        int gridSize = 5; // Grid cells in world units

        for (int x = 0; x <= level.getBounds().width; x += gridSize) {
            Vector2 start = worldToMinimap(x + level.getBounds().x, level.getBounds().y);
            Vector2 end = worldToMinimap(x + level.getBounds().x, level.getBounds().y + level.getBounds().height);

            // Draw vertical line
            pixmap.drawLine((int)start.x, (int)start.y, (int)end.x, (int)end.y);
        }

        for (int y = 0; y <= level.getBounds().height; y += gridSize) {
            Vector2 start = worldToMinimap(level.getBounds().x, y + level.getBounds().y);
            Vector2 end = worldToMinimap(level.getBounds().x + level.getBounds().width, y + level.getBounds().y);

            // Draw horizontal line
            pixmap.drawLine((int)start.x, (int)start.y, (int)end.x, (int)end.y);
        }
    }

    private void drawMapEntity(Vector2 position, float width, float height, Color color) {
        // Calculate the corners in world coordinates
        float worldLeft = position.x - width/2;
        float worldRight = position.x + width/2;
        float worldBottom = position.y - height/2;
        float worldTop = position.y + height/2;

        // Use the same inversion logic for all static entities
        float minimapLeftX = (worldLeft - level.getBounds().x) * scaleFactorX + BORDER_SIZE;
        float minimapRightX = (worldRight - level.getBounds().x) * scaleFactorX + BORDER_SIZE;
        float minimapTopY = (level.getBounds().y + level.getBounds().height - worldTop) * scaleFactorY + BORDER_SIZE;
        float minimapBottomY = (level.getBounds().y + level.getBounds().height - worldBottom) * scaleFactorY + BORDER_SIZE;

        // For rectangle drawing, calculate dimensions
        float rectX = Math.min(minimapLeftX, minimapRightX);
        float rectY = Math.min(minimapTopY, minimapBottomY);
        float rectWidth = Math.abs(minimapRightX - minimapLeftX);
        float rectHeight = Math.abs(minimapBottomY - minimapTopY);

        // Draw the entity
        pixmap.setColor(color);
        pixmap.fillRectangle(
            (int)rectX,
            (int)rectY,
            Math.max(1, (int)rectWidth),
            Math.max(1, (int)rectHeight)
        );
    }


    Vector2 vec2 = new Vector2();
    /**
     * Draw any obstacle on the minimap (walls, etc.)
     */
    private void drawObstacle(Obstacle obstacle) {
        // Get position and dimensions
        float x = obstacle.getX();
        float y = obstacle.getY();
        float width = 0;
        float height = 0;

        if (obstacle instanceof BoxObstacle) {
            BoxObstacle box = (BoxObstacle) obstacle;
            width = box.getWidth();
            height = box.getHeight();
        } else {
            // Default size for other obstacles
            width = 1.0f;
            height = 1.0f;
        }
        vec2.set(x,y);
       drawMapEntity(vec2, width, height, WALL_COLOR);
    }

    /**
     * Draws all doors directly from the door collection
     */
    private void drawAllDoors() {
        pixmap.setColor(DOOR_COLOR);

        for (Door door : level.getDoors()) {
            Vector2 position = door.getObstacle().getPosition();
            float size = 1.0f;  // Default size

            // Try to determine actual size
            if (door.getObstacle() instanceof BoxObstacle) {
                BoxObstacle box = (BoxObstacle) door.getObstacle();
                size = Math.max(box.getWidth(), box.getHeight());
            }

            drawMapEntity(position, size, size, DOOR_COLOR);
        }
    }

    /**
     * Draws all keys directly from the keys collection
     */
    private void drawAllKeys() {
        pixmap.setColor(KEY_COLOR);

        for (Key key : level.getKeys()) {
            // Only draw if not collected
            if (!key.isCollected()) {
                Vector2 position = key.getObstacle().getPosition();
                float size = 0.5f;  // Keys are small
                drawMapEntity(position, size, size, KEY_COLOR);
            }
        }
    }


    /**
     * Draws the exit directly from the exit reference
     */
    private void drawExitDirect() {
        Exit exit = level.getExit();
        if (exit != null) {
            Vector2 position = exit.getObstacle().getPosition();
            float size = 1.0f;  // Default size

            // Try to determine actual size
            if (exit.getObstacle() instanceof BoxObstacle) {
                BoxObstacle box = (BoxObstacle) exit.getObstacle();
                size = Math.max(box.getWidth(), box.getHeight());
            }

            drawMapEntity(position, size, size, EXIT_COLOR);

        }
    }

    /**
     * Modifies vec2 cache that contains map coordinate converted from world coordinate.
     */
    private Vector2 worldToMinimap(float worldX, float worldY) {
        // Adjust for level bounds offset and scale to minimap size
        float minimapX = (worldX - level.getBounds().x) * scaleFactorX + BORDER_SIZE;
        float minimapY = (worldY - level.getBounds().y) * scaleFactorY + BORDER_SIZE;
        vec2.set(minimapX, minimapY);
        return vec2;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (needsRedraw) {
            drawMinimap();
        }

        // Draw the minimap texture
        batch.draw(minimapTexture, getX(), getY(), getWidth(), getHeight());


        // Draw dynamic entities (players, guards, cameras)
        drawDynamicEntities(batch);
    }


    /**
     * Draws dynamic entities like players and guards that need to be updated every frame
     */
    private void drawDynamicEntities(Batch batch) {
        Color originalColor = batch.getColor().cpy();

        // Draw players
        if (level.getCat() != null) {
            Vector2 catPos = level.getCat().getPosition();
            Vector2 minimapCatPos = worldToMinimap(catPos.x, catPos.y);

            //batch.setColor(CAT_COLOR);
            batch.draw(catTexture,
                getX() + minimapCatPos.x - 4,
                getY() + minimapCatPos.y - 4,
                8, 8);
        }

        if (level.getOctopus() != null) {
            Vector2 octopusPos = level.getOctopus().getPosition();
            Vector2 minimapOctopusPos = worldToMinimap(octopusPos.x, octopusPos.y);

//            batch.setColor(OCTOPUS_COLOR);
            batch.draw(octopusTexture,
                getX() + minimapOctopusPos.x - 4,
                getY() + minimapOctopusPos.y - 4,
                8, 8);
        }

        // Draw guards
        // batch.setColor(GUARD_COLOR);
        for (Guard guard : level.getGuards()) {
            Vector2 guardPos = guard.getPosition();
            Vector2 minimapGuardPos = worldToMinimap(guardPos.x, guardPos.y);

            batch.draw(guardTexture,
                getX() + minimapGuardPos.x - 3,
                getY() + minimapGuardPos.y - 3,
                8, 10);
        }

        // Draw security cameras
        // batch.setColor(CAMERA_COLOR);
        for (SecurityCamera camera : level.getSecurityCameras()) {
            Vector2 cameraPos = camera.getPosition();
            Vector2 minimapCameraPos = worldToMinimap(cameraPos.x, cameraPos.y);

            batch.draw(cameraTexture,
                getX() + minimapCameraPos.x - 3,
                getY() + minimapCameraPos.y - 3,
                8, 6);
        }

        for (Vent vent : level.getVents()) {
            Vector2 position = vent.getObstacle().getPosition();
            Vector2 ventPos = worldToMinimap(position.x, position.y);
            batch.draw(ventTexture,
                getX() + ventPos.x - 4,
                getY() + ventPos.y - 4,
                12, 12);
        }

        for (Key key : level.getKeys()) {
            // Only draw if not collected
            Vector2 keyPos = key.getObstacle().getPosition();
            Vector2 minimapKeyPos = worldToMinimap(keyPos.x, keyPos.y);
            batch.draw(keyTexture,
                getX() + minimapKeyPos.x - 4,
                getY() + minimapKeyPos.y - 4,
                10, 10);
        }

        if (level.getExit() != null) {
            Vector2 exitPos = level.getExit().getObstacle().getPosition();
            Vector2 minimapExitPos = worldToMinimap(exitPos.x, exitPos.y);

            batch.draw(exitTexture,
                getX() + minimapExitPos.x - 4,
                getY() + minimapExitPos.y - 4,
                12, 12);
        }



        // Restore original color
        batch.setColor(originalColor);
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
        if (dotTexture != null) {
            dotTexture.dispose();
        }
    }

    public void setDisabled(boolean b){
        this.disabled = b;
    }
}
