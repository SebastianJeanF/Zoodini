package walknroll.zoodini.models.nonentities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.utils.JsonValue;

import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.SpriteMesh;
import edu.cornell.gdiac.graphics.SpriteSheet;
import edu.cornell.gdiac.physics2.WheelObstacle;
import walknroll.zoodini.controllers.SoundController;
import walknroll.zoodini.models.GameLevel;
import walknroll.zoodini.utils.ZoodiniSprite;
import walknroll.zoodini.utils.animation.Animation;
import walknroll.zoodini.utils.animation.AnimationController;
import walknroll.zoodini.utils.animation.AnimationState;

public class InkProjectile extends ZoodiniSprite {

    /** Cache for internal force calculations */
    private Vector2 forceCache = new Vector2();

    /** The current horizontal movement of the character */
    private Vector2 movement = new Vector2();

    /** The position that this projectile was shot */
    private Vector2 startPosition = new Vector2();
    private Vector2 endPosition = new Vector2();

    private int startFrame;

    private boolean shouldDestroy;

    private AnimationController animationController;


    public Vector2 getStartPosition(){
        return startPosition;
    }

    public void setStartPosition(Vector2 p){
        startPosition.set(p);
    }

    public Vector2 getEndPosition(){
        return endPosition;
    }

    public void setEndPosition(Vector2 p){
        endPosition.set(p);
    }


    public boolean getShouldDestroy() {
        return shouldDestroy;
    }

    public void setShouldDestroy(boolean s) {
        this.shouldDestroy = s;
    }

    public void setPosition(Vector2 newPos) {
        obstacle.setPosition(newPos);
    }

    /**
     * Returns the position of this avatar.
     *
     * @return the position of this avatar.
     */
    public Vector2 getPosition() {
        return obstacle.getPosition();
    }

    public void setAngle(float angle) {
        obstacle.setAngle(angle);
    }

    public float getAngle() {
        return obstacle.getAngle();
    }

    /**
     * Returns the directional movement of this character.
     *
     * This is the result of input times the avatar force.
     *
     * @return the directional movement of this character.
     */
    public Vector2 getMovement() {
        return movement;
    }

    /**
     * Sets the directional movement of this character.
     *
     * This is the result of input times the avatar force.
     *
     * @param value the directional movement of this character.
     */
    public void setMovement(Vector2 value) {
        setMovement(value.x, value.y);
    }

    /**
     * Sets the directional movement of this character.
     *
     * This is the result of input times the avatar force.
     *
     * @param dx the horizontal movement of this character.
     * @param dy the horizontal movement of this character.
     */
    public void setMovement(float dx, float dy) {
        movement.set(dx, dy);
    }

    /**
     * Creates a exit with the given settings
     *
     * @param json      The JSON values defining this avatar
     * @param units     The physics units for this avatar
     */
    public InkProjectile(JsonValue json, float units) {
        float radius = json.getFloat("radius");
        obstacle = new WheelObstacle(0, 0, radius);
        obstacle.setName(json.name());
        obstacle.setFixedRotation(false);

        obstacle.setBodyType(BodyDef.BodyType.DynamicBody);
        obstacle.setDensity(json.getFloat("density"));
        obstacle.setFriction(json.getFloat("friction"));
        obstacle.setRestitution(json.getFloat("restitution"));
        obstacle.setPhysicsUnits(units);

        short collideBits = GameLevel.bitStringToShort(json.getString("category"));
        short excludeBits = GameLevel.bitStringToComplement(json.getString("exclude"));
        Filter filter = new Filter();
        filter.categoryBits = collideBits;
        filter.maskBits = excludeBits;
        obstacle.setFilterData(filter);

        setDebugColor(ParserUtils.parseColor(json.get("debug"), Color.WHITE));

        float r = json.getFloat("spriterad") * units;
        mesh = new SpriteMesh(-r, -r, 2 * r, 2 * r);

        shouldDestroy = false;
        animationController = new AnimationController(AnimationState.IDLE);
    }

    public void setAnimation(AnimationState state, SpriteSheet sheet){
        switch(state){
            case IDLE -> animationController.addAnimation(AnimationState.IDLE, new Animation(sheet, 0, sheet.getSize()-1, 16, true));
            case EXPLODE -> animationController.addAnimation(AnimationState.EXPLODE, new Animation(sheet, 0, sheet.getSize()-1, 30 / sheet.getSize(), true));
        }
    }

    /**
     * Applies the force to the body of this avatar
     *
     * This method should be called after the force attribute is set.
     */
    public void applyForce() {
        if(getShouldDestroy()){
            return;
        }

        if (!obstacle.isActive()) {
            return;
        }

        // Only walk or spin if we allow it
        obstacle.setLinearVelocity(Vector2.Zero);
        obstacle.setAngularVelocity(0.0f);

        // Apply force for movement
         if (getMovement().len2() > 0f) {
            forceCache.set(getMovement().nor().scl(50));
            obstacle.getBody().applyForce(forceCache, obstacle.getPosition(), true);
        }
    }

    private boolean soundPlayed = false;
    @Override
    public void update(float dt) {
            if (this.getShouldDestroy()) {
                animationController.setState(AnimationState.EXPLODE);
                if (animationController.getCurrentFrame()
                    >= animationController.getCurrentSpriteSheet().getSize() - 1) {
                    setDrawingEnabled(false);
                    setShouldDestroy(false);
                }
                if (!soundPlayed) {
                    SoundController sc = SoundController.getInstance();
                    //sc.playInkFinish();
                    soundPlayed = true;
                }
            } else {
                animationController.setState(AnimationState.IDLE);
                soundPlayed = false;
            }

            animationController.update();


        // This is the key fix - update the sprite reference itself
        SpriteSheet currentSheet = animationController.getCurrentSpriteSheet();
        if (currentSheet != null) {
            sprite = currentSheet;  // Switch to the current animation's spritesheet
        }

        // Now setting the frame will work correctly
        if (sprite != null) {
            sprite.setFrame(animationController.getCurrentFrame());
        }

        obstacle.update(dt);
    }

    @Override
    public void draw(SpriteBatch batch) {
        if (this.obstacle != null && this.mesh != null) {
            float x = this.obstacle.getX();
            float y = this.obstacle.getY();
            float a = this.obstacle.getAngle();
            float u = this.obstacle.getPhysicsUnits();

            this.transform.idt();
            if(animationController.getCurrentState() == AnimationState.EXPLODE){
                transform.scale(5.0f,5.0f); //Adjust this to scale explosion sprites
            }
            this.transform.preRotate((float)((double)(a * 180.0F) / Math.PI));
            this.transform.preTranslate(x * u, y * u);
            batch.setTextureRegion(this.sprite);
            batch.drawMesh(this.mesh, this.transform, false);
            batch.setTexture(null);
        }
    }

    public void destroy(){
        if(!getShouldDestroy()){
            return;
        }
//        setDrawingEnabled(false);
//        setShouldDestroy(false);
        getObstacle().setActive(false);
    }

    public void activate(){
        setShouldDestroy(false);
        getObstacle().setActive(true);
        setDrawingEnabled(true);
    }
}
