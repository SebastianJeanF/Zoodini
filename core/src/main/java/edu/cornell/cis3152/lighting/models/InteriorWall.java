/*
 * InteriorWall.java
 *
 * This is a refactored version of a platform from the JSON Demo. Now, instead
 * of platforms, they are interior walls. We want to specify them as a rectangle,
 * but uniformly tile them with a texture.
 *
 * A further change from the JSON version is the notion of "padding". Padding is
 * the the amount that the visible sprite is larger than the physics object
 * itself (measured in physics coordinates, not Sprite coordinates). This keeps
 * the shadow from completely obstructing the wall sprite.
 *
 * @author: Walker M. White
 * @version: 2/15/2025
 */
package edu.cornell.cis3152.lighting.models;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.physics.box2d.*;

import edu.cornell.cis3152.lighting.models.GameLevel;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteMesh;
import edu.cornell.gdiac.math.Poly2;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;

/**
 * A rectangle shape representing an interior wall.
 *
 * This class is largely just a constructor. Everything else is provided by the
 * subclass.
 */
public class InteriorWall extends ObstacleSprite {
	/** The texture anchor upon region initialization */
	protected Vector2 anchor;
	/** The padding (in physics units) to increase the sprite beyond the physics body */
	protected Vector2 padding;

	/**
	 * Create a new interior wall with the given settings
	 *
	 * @param directory The asset directory (for textures, etc)
	 * @param json      The JSON values defining this avatar
	 * @param units     The physics units for this avatar
	 */
	public InteriorWall(AssetDirectory directory, JsonValue json, float units) {

		float[] pos  = json.get("pos").asFloatArray();
		float[] size = json.get("size").asFloatArray();
		obstacle = new BoxObstacle(pos[0],pos[1],size[0],size[1]);
		obstacle.setName( json.name() );

		// Technically, we should do error checking here.
		// A JSON field might accidentally be missing
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

		float[] pad  = json.get("pad").asFloatArray();
		float w = (size[0]+2*pad[0])*units;
		float h = (size[1]+2*pad[0])*units;

		Poly2 poly = new Poly2(-w/2,-h/2,w,h);
		float tile = json.getFloat("tile");
		mesh = new SpriteMesh(poly, tile, tile);
	}

}
