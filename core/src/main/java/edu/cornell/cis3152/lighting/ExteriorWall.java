/*
 * WallModel.java
 *
 * This is a refactored version of the wall (screen boundary) from Lab 4. We
 * have made it a specialized class so that we can import its properties from
 * a JSON file.
 *
 * A further change from the JSON version is the notion of "padding". Padding
 * is the the amount that the visible sprite is larger than the physics object
 * itself (measured in physics coordinates, not Sprite coordinates). This keeps
 * the shadow from completely obstructing the wall sprite.
 *
 * Because this is an arbitrary polygon, it is difficult to compute proper
 * padding easily. Therefore, we avoid the problem by allowing the user to
 * specify a second polygon (still in physics coordinates) for the Sprite.
 * In theory, this means that physics body and sprite could be in completely
 * different locations. In practice, however, the padding polygon always
 * "contains" the physics polygon.
 *
 * @author: Walker M. White
 * @version: 2/15/2025
 */
package edu.cornell.cis3152.lighting;

import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.physics.box2d.*;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteMesh;
import edu.cornell.gdiac.math.Poly2;
import edu.cornell.gdiac.math.PolyTriangulator;
import edu.cornell.gdiac.physics2.*;


/**
 * A polygon shape representing the screen boundary
 *
 * This class is largely just a constructor. Everything else is provided by the
 * subclass.
 */
public class ExteriorWall extends ObstacleSprite {

	/**
	 * Creates a new exterior wall with the given settings
	 *
	 * @param directory The asset directory (for textures, etc)
	 * @param json      The JSON values defining this avatar
	 * @param units     The physics units for this avatar
	 */
	public ExteriorWall(AssetDirectory directory, JsonValue json, float units) {
		// Technically, we should do error checking here.
		// A JSON field might accidentally be missing
		float[] verts = json.get("boundary").asFloatArray();

		Poly2 poly = new Poly2();
		PolyTriangulator triangulator = new PolyTriangulator();
		triangulator.set(verts);
		triangulator.calculate();
		triangulator.getPolygon(poly);

		obstacle = new PolygonObstacle( poly );
		obstacle.setName( json.name() );

		obstacle.setBodyType(json.get("bodytype").asString().equals("static") ? BodyDef.BodyType.StaticBody : BodyDef.BodyType.DynamicBody);
		obstacle.setDensity(json.get("density").asFloat());
		obstacle.setFriction(json.get("friction").asFloat());
		obstacle.setRestitution(json.get("restitution").asFloat());
		obstacle.setPhysicsUnits( units );

		// Create the collision filter (used for light penetration)
		short collideBits = GameLevel.bitStringToShort(json.get("collide").asString());
		short excludeBits = GameLevel.bitStringToComplement(json.get("exclude").asString());
		Filter filter = new Filter();
		filter.categoryBits = collideBits;
		filter.maskBits = excludeBits;
		obstacle.setFilterData(filter);

		setDebugColor( ParserUtils.parseColor( json.get( "debug" ), Color.WHITE ) );

		// Now get the texture from the AssetManager singleton
		String key = json.get( "texture" ).asString();
		TextureRegion texture = new TextureRegion( directory.getEntry( key, Texture.class ) );
		setTextureRegion( texture );

		float[] pads  = json.get("padding").asFloatArray();
		poly.clear();
		triangulator.clear();
		triangulator.set(pads);
		triangulator.calculate();
		triangulator.getPolygon(poly);
		poly.scl( units );

		float tile = json.getFloat( "tile" );
		mesh = new SpriteMesh(poly, tile, tile);
	}
}
