package walknroll.zoodini.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TransformDrawable;

public class LevelPortal extends BaseDrawable implements TransformDrawable {
    private static TextureRegion BASE_PORTAL;
    private static TextureRegion PORTAL_CELL_BARS;

    public static void setTextures(Texture base, Texture bars) {
        LevelPortal.BASE_PORTAL = new TextureRegion(base);
        LevelPortal.PORTAL_CELL_BARS = new TextureRegion(bars);
    }

    public static boolean isLoaded() {
        return LevelPortal.BASE_PORTAL != null;
    }

    private boolean open;
    private boolean selected;

    public LevelPortal(boolean open, boolean selected) {
        if (LevelPortal.BASE_PORTAL == null) {
            throw new RuntimeException("Tried to create a LevelPortal before the assets have been loaded");
        }
        this.open = open;
        this.selected = selected;
        setMinWidth(LevelPortal.BASE_PORTAL.getRegionWidth());
        setMinHeight(LevelPortal.BASE_PORTAL.getRegionHeight());
    }

    public void draw(Batch batch, float x, float y, float originX, float originY, float width, float height,
            float scaleX,
            float scaleY, float rotation) {
        batch.setColor(this.selected ? Color.GRAY : Color.WHITE);
        batch.draw(LevelPortal.BASE_PORTAL, x, y, originX, originY, width, height, scaleX, scaleY, rotation);
        if (!open) {
            batch.draw(LevelPortal.PORTAL_CELL_BARS, x, y, originX, originY, width, height, scaleX, scaleY, rotation);
        }
    }

    public void draw(Batch batch, float x, float y, float width, float height) {
        batch.setColor(this.selected ? Color.GRAY : Color.WHITE);
        batch.draw(LevelPortal.BASE_PORTAL, x, y, width, height);
        if (!open) {
            batch.draw(LevelPortal.PORTAL_CELL_BARS, x, y, width, height);
        }
    }

}
