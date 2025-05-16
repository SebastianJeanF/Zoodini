package walknroll.zoodini.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.Hinting;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

final public class FreeTypeSkin extends Skin {

    public FreeTypeSkin(FileHandle skinFile) {
        super(skinFile);
    }

    // Override json loader to process FreeType fonts from skin JSON
    @Override
    protected Json getJsonLoader(final FileHandle skinFile) {
        Json json = super.getJsonLoader(skinFile);
        final Skin skin = this;

        json.setSerializer(FreeTypeFontGenerator.class,
                new Json.ReadOnlySerializer<FreeTypeFontGenerator>() {

                    @Override
                    public FreeTypeFontGenerator read(Json json,
                            JsonValue jsonData, Class type) {
                        String path = json.readValue("font", String.class, jsonData);
                        jsonData.remove("font");

                        Hinting hinting = Hinting.valueOf(json.readValue("hinting",
                                String.class, "AutoMedium", jsonData));
                        jsonData.remove("hinting");

                        TextureFilter minFilter = TextureFilter.valueOf(
                                json.readValue("minFilter", String.class, "Nearest", jsonData));
                        jsonData.remove("minFilter");

                        TextureFilter magFilter = TextureFilter.valueOf(
                                json.readValue("magFilter", String.class, "Nearest", jsonData));
                        jsonData.remove("magFilter");

                        FreeTypeFontParameter parameter = json.readValue(
                                FreeTypeFontParameter.class, jsonData);
                        parameter.hinting = hinting;
                        parameter.minFilter = minFilter;
                        parameter.magFilter = magFilter;
                        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(skinFile
                                .parent().child(path));
                        BitmapFont font = generator.generateFont(parameter);
                        skin.add(jsonData.name, font);
                        if (parameter.incremental) {
                            generator.dispose();
                            return null;
                        } else {
                            return generator;
                        }
                    }
                });

        return json;
    }

    /**
     * Resizes the font in the skin to match the current resolution.
     */
    public void resizeFont(float resScale) {
        // TODO: Add BASE_FONT_SIZE to constants instead
        int BASE_FONT_SIZE = 35;

        FreeTypeFontGenerator gen =
            new FreeTypeFontGenerator(Gdx.files.internal("fonts/LuckiestGuy-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
        param.size = (int)(BASE_FONT_SIZE * resScale);
        param.incremental = true;

        // Generate scaled-up font
        BitmapFont scaledFont = gen.generateFont(param);
        this.add("default-font", scaledFont);
        this.get(TextButton.TextButtonStyle.class).font = scaledFont;

        // Clean up
        gen.dispose();
    }
}
