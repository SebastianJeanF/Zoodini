package edu.cornell.cis3152.lighting.controllers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.TextLayout;

public class UIController {

    protected OrthographicCamera camera;
    /** The font for giving messages to the player */
    protected BitmapFont displayFont;
    /** The message to display */
    protected TextLayout message;

    public UIController(){
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void setFont(BitmapFont f){
        displayFont = f;
    }

    public void setMessage(TextLayout m){
        message = m;
    }

    public void reset(){
        message = null;
        displayFont = null;
    }

    public void update(){
        camera.update();
    }

    public void draw(SpriteBatch batch){
        batch.setProjectionMatrix(camera.combined);
        batch.begin(camera);

        if(message != null) {
            batch.setBlur(0.5f);
            batch.drawText(message, Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);
            batch.setBlur(0.0f);
        }

        batch.end();
    }
}
