package walknroll.zoodini.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import walknroll.zoodini.models.entities.Cat;

public class MeowCooldownIndicator extends Actor {
    // UI Components
    private Image cooldownPentagon; // Dark red pentagon (on cooldown)
    private Image readyPentagon;    // Bright red pentagon (ready to use)
    private Label meowLabel;
    private GlyphLayout layout;

    // Configuration
    private static final float PENTAGON_SIZE = 150f;
    private final Color DARK_RED = new Color(0.6f, 0.1f, 0.1f, 0.9f);
    private final Color LIGHT_RED = new Color(0.9f, 0.2f, 0.2f, 0.9f);
    private final float FONT_SCALE = 0.4f; // Even smaller font

    /**
     * Creates a new meow cooldown indicator
     *
     * @param font The font to use for the cooldown text
     */
    public MeowCooldownIndicator(BitmapFont font) {
        // Create cooldown pentagon (dark red)
        Drawable cooldownDrawable = createPentagonDrawable(DARK_RED);
        cooldownPentagon = new Image(cooldownDrawable);
        cooldownPentagon.setSize(PENTAGON_SIZE, PENTAGON_SIZE);

        // Create ready pentagon (bright red)
        Drawable readyDrawable = createPentagonDrawable(LIGHT_RED);
        readyPentagon = new Image(readyDrawable);
        readyPentagon.setSize(PENTAGON_SIZE, PENTAGON_SIZE);

        // Create a copy of the font to avoid modifying the original
        BitmapFont labelFont = new BitmapFont(font.getData(), font.getRegion(), font.isFlipped());
        labelFont.getData().setScale(FONT_SCALE);
        LabelStyle meowStyle = new LabelStyle(labelFont, Color.WHITE);
        meowLabel = new Label("MEOW!", meowStyle);
        layout = new GlyphLayout();
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // Position pentagons
        cooldownPentagon.setPosition(getX(), getY());
        readyPentagon.setPosition(getX(), getY());

        // Draw appropriate pentagon
        if (cooldownPentagon.isVisible()) {
            cooldownPentagon.draw(batch, parentAlpha);
        } else {
            readyPentagon.draw(batch, parentAlpha);
        }

        // Center the text in the pentagon
        layout.setText(meowLabel.getStyle().font, meowLabel.getText());
        float textX = getX() + (PENTAGON_SIZE - layout.width) / 2;
        float textY = getY() + PENTAGON_SIZE/2 + layout.height/2 - 25;
        meowLabel.setPosition(textX, textY);
        meowLabel.draw(batch, parentAlpha);
    }

    /**
     * Updates the cooldown display based on the cat's state
     *
     * @param cat The cat whose cooldown to display
     */
    public void update(Cat cat) {
        if (cat == null) return;

        // Update which pentagon is visible based on cooldown state
        boolean isOnCooldown = !cat.canMeow();
        cooldownPentagon.setVisible(isOnCooldown);
        readyPentagon.setVisible(!isOnCooldown);

        // Update text based on cooldown state
        if (cat.canMeow()) {
            meowLabel.setText("MEOW!");
        } else {
            // Display remaining seconds rounded up
            int seconds = (int)Math.ceil(cat.getMeowCooldownRemaining());
            meowLabel.setText(String.valueOf(seconds));
        }
    }

    /**
     * Creates a pentagon shape drawable
     */
    private Drawable createPentagonDrawable(Color color) {
        int radius = 100;
        Pixmap pixmap = new Pixmap(radius*2, radius*2, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);

        // Draw a pentagon
        int vertices = 5;
        float[] xPoints = new float[vertices];
        float[] yPoints = new float[vertices];

        for (int i = 0; i < vertices; i++) {
            float angle = (float) (2 * Math.PI * i / vertices - Math.PI / 2);
            xPoints[i] = (float) (radius + 0.8f * radius * Math.cos(angle));
            yPoints[i] = (float) (radius + 0.8f * radius * Math.sin(angle));
        }

        // Fill the pentagon
        for (int i = 0; i < vertices-1; i++) {
            pixmap.fillTriangle(radius, radius,
                (int)xPoints[i], (int)yPoints[i],
                (int)xPoints[i+1], (int)yPoints[i+1]);
        }
        pixmap.fillTriangle(radius, radius,
            (int)xPoints[vertices-1], (int)yPoints[vertices-1],
            (int)xPoints[0], (int)yPoints[0]);

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    /**
     * Disposes of resources used by this indicator
     */
    public void dispose() {
        if (cooldownPentagon != null && cooldownPentagon.getDrawable() instanceof TextureRegionDrawable) {
            ((TextureRegionDrawable)cooldownPentagon.getDrawable()).getRegion().getTexture().dispose();
        }

        if (readyPentagon != null && readyPentagon.getDrawable() instanceof TextureRegionDrawable) {
            ((TextureRegionDrawable)readyPentagon.getDrawable()).getRegion().getTexture().dispose();
        }

        if (meowLabel != null && meowLabel.getStyle().font != null) {
            meowLabel.getStyle().font.dispose();
        }
    }
}
