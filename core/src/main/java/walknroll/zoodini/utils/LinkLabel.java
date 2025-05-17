package walknroll.zoodini.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class LinkLabel extends Label {
    public LinkLabel(String text, String uri, Skin skin) {
        super(text, skin, "link");
        setTouchable(Touchable.enabled);
        addListener(new ClickListener() {

            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.net.openURI(uri);
            }
        });
    }
}
