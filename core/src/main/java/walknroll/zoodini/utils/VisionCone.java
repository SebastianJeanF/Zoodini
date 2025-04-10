package walknroll.zoodini.utils;

import box2dLight.ConeLight;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.math.Poly2;
import edu.cornell.gdiac.math.PolyFactory;
import java.util.Arrays;
import walknroll.zoodini.models.GameLevel;

public class VisionCone implements RayCastCallback{

    private static final PolyFactory polyfactory = new PolyFactory();

    private int numRays;
    private Vector2 origin; //in world units
    private float radius;
    private float facingAngle;
    private float wideness;
    private Color c;
    private float units;
    private short maskbits = (short) 0xFFFF;
    private short categorybits = (short) 0x0001;

    private Poly2 cone;
    private Array<Vector2> rayEndPoints;
    private Body body; //body this cone is attached to

    private float currentFacingAngle = 0f; // Current angle in degrees
    private float targetFacingAngle = 0f;  // Target angle to rotate toward
    private float turnSpeed = 5.0f;        // Rotation speed in radians per second


    public Vector2 getPosition(){
        return this.origin;
    }
    public void setMaskbits(short m){
        maskbits = m;
    }
    public void setRadius(float r){
        radius = r;
    }
    public void setWideness(float w){
        wideness = w;
    }


    /**
     * Constructs a vision cone.
     * @param numRays number of rays: more rays means more fine but more expensive
     * @param origin the origin of the cone in meters
     * @param radius radius in meters
     * @param facing the degree angle of center of the cone
     * @param wideness the degree of the cone
     * @param c color
     * @param units pixels-per-meter ratio used for drawing only
     * @param maskbits bits that the rays collide with.
     */
    public VisionCone(int numRays, Vector2 origin, float radius, float facing, float wideness, Color c, float units, String categorybits, String maskbits) {
        this.numRays = numRays;
        this.radius = radius;
        this.facingAngle = facing;
        this.wideness = wideness;
        this.origin = origin;
        this.rayEndPoints = new Array<>();
        this.c = c;
        this.units = units;
        this.maskbits = GameLevel.bitStringToComplement(maskbits);
        this.categorybits = GameLevel.bitStringToShort(categorybits);

        cone = createPolygon(numRays, origin, radius, facing, wideness);
    }

    private Poly2 createPolygon(int numRays, Vector2 origin, float radius, float facing, float wideness) {
        Poly2 poly = new Poly2(new float[0]);
        int idx = poly.vertices.size;
        float coef = wideness / (float) numRays;
        poly.vertices.ensureCapacity(numRays * 2 + 2);

        int i;
        for(i = 0; i < numRays; ++i){
            float rads = (float) (Math.toRadians(facing + (i - (numRays - 1) / 2f) * coef));
            poly.vertices.items[idx++] = (float)((double)radius * Math.cos((double)rads) + (double)origin.x);
            poly.vertices.items[idx++] = (float)((double)radius * Math.sin((double)rads) + (double)origin.y);
        }

        poly.vertices.items[idx++] = origin.x;
        poly.vertices.items[idx++] = origin.y;

        FloatArray arr = poly.vertices;
        arr.size += numRays * 2 + 2;
        idx = poly.indices.size;
        poly.indices.ensureCapacity(3 * numRays);

        for(i = 0; i < numRays - 1; ++i) {
            poly.indices.items[idx++] = (short)(i);
            poly.indices.items[idx++] = (short)(i + 1);
            poly.indices.items[idx++] = (short)(numRays);
        }

        ShortArray var11 = poly.indices;
        var11.size += 3 * numRays;

        return poly;
    }

    public boolean contains(float x, float y){
        return cone.contains(x,y);
//        float x_d = this.origin.x - x;
//        float y_d = this.origin.y - y;
//        float dst2 = x_d * x_d + y_d * y_d;
//        if (this.radius * this.radius <= dst2) {
//            return false;
//        } else {
//            boolean oddNodes = false;
//            float x2 = this.cone.vertices.items[this.numRays * 2] = this.origin.x;
//            float y2 = this.cone.vertices.items[this.numRays * 2 + 1] = this.origin.y;
//
//            for(int i = 0; i <= this.numRays; ++i) {
//                float x1 = this.cone.vertices.items[i];
//                float y1 = this.cone.vertices.items[i+1];
//                if ((y1 < y && y2 >= y || y1 >= y && y2 < y) && (y - y1) / (y2 - y1) * (x2 - x1) < x - x1) {
//                    oddNodes = !oddNodes;
//                }
//
//                x2 = x1;
//                y2 = y1;
//            }
//
//            return oddNodes;
//        }
    }

    public boolean contains(Vector2 position){
        return cone.contains(position.x, position.y);
    }

    /**
     * Angle is in degrees
     */
    public void attachToBody(Body body, float degree){
        this.body = body;
        this.facingAngle = degree;
    }



    /**
     * Smoothly updates the vision cone's facing direction to match the guard's orientation.
     * This method should be called during the update cycle, separate from the guard's update.
     *
     * @param dt The time delta since the last update
     * @param guardDirection The current direction vector that the guard is facing
     */
    public void updateFacingDirection(float dt, Vector2 guardDirection) {
        if (guardDirection == null || guardDirection.len2() < 0.0001f) {
            return; // No valid direction to face
        }

        // Calculate target angle from guard direction (in degrees)
        targetFacingAngle = MathUtils.radiansToDegrees * MathUtils.atan2(guardDirection.y, guardDirection.x);

        // Convert current and target angles to radians for calculations
        float currentAngleRad = MathUtils.degreesToRadians * currentFacingAngle;
        float targetAngleRad = MathUtils.degreesToRadians * targetFacingAngle;

        // Determine the shortest turning direction
        float angleDiff = targetAngleRad - currentAngleRad;

        // Normalize to [-PI, PI] for shortest rotation path
        while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
        while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI;

        // Calculate how much to turn this frame (limited by turn speed)
        float turnAmount = Math.min(Math.abs(angleDiff), turnSpeed * dt);

        // Apply the turn in the correct direction
        float newAngleRad = currentAngleRad + Math.signum(angleDiff) * turnAmount;

        // Convert back to degrees and update the current facing angle
        currentFacingAngle = MathUtils.radiansToDegrees * newAngleRad;

        // Apply the new angle to the vision cone's internal direction
//        setFacingDirection(currentFacingAngle);
        this.facingAngle = currentFacingAngle;
    }

    float closestFraction = 1.0f;
    @Override
    public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
        boolean collide = (this.maskbits & fixture.getFilterData().categoryBits) != 0;
        if(fraction < closestFraction
            && !body.getFixtureList().contains(fixture,true)
            && collide)
        {
            closestFraction = fraction;
            tmp1.set(point);
        }
        return 1;
    }


    Vector2 tmp1 = new Vector2();
    Vector2 tmp2 = new Vector2();
    Vector2 tmp3 = new Vector2();
    public void update(World world) {
        if(body == null){
            return;
        }

        float coef = wideness / (float) (numRays -1 );
        origin = body.getPosition();
        int k = 0;

        for (int i = 0; i < numRays; ++i) {
//            float degrees = MathUtils.radiansToDegrees * body.getAngle() + facingAngle;
            float degrees = facingAngle;
            degrees += (i - (numRays - 1) / 2f) * coef;
            float rads = MathUtils.degreesToRadians * degrees;

            tmp2.set(MathUtils.cos(rads), MathUtils.sin(rads));
            tmp3.set(origin).add(tmp2.scl(radius));

            closestFraction = 1.0f;
            tmp1.set(tmp3);

            world.rayCast(this, origin, tmp1);
            cone.vertices.items[k++] = tmp1.x;
            cone.vertices.items[k++] = tmp1.y;

        }
        cone.vertices.items[k++] = origin.x;
        cone.vertices.items[k++] = origin.y;
    }

    Affine2 cache = new Affine2();
    public void draw(SpriteBatch batch, Camera camera){
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.begin(camera);
        batch.setColor(c); //rgba
        cache.idt();
        cache.scale(units,units);
        batch.fill(cone, cache);
        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
}
