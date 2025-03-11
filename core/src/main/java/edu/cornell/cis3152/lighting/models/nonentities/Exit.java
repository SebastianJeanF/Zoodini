/*
 * Exit.java
 *
 * This is a refactored version of the exit door from Lab 4.  We have made it a
 * specialized class so that we can import its properties from a JSON file.
 *
 * @author: Walker M. White
 * @version: 2/15/2025
 */
package edu.cornell.cis3152.lighting.models.nonentities;

import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.physics.box2d.*;

import edu.cornell.cis3152.lighting.models.GameLevel;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteMesh;
import edu.cornell.gdiac.physics2.*;

/**
 * A sensor obstacle representing the end of the level.
 *
 * This class is largely just a constructor. Everything else is provided by the
 * subclass.
 */
public class Exit extends ObstacleSprite {

	/**
	 * Creates a exit with the given settings
	 *
	 * @param directory The asset directory (for textures, etc)
	 * @param json      The JSON values defining this avatar
	 * @param units     The physics units for this avatar
	 */
	public Exit(AssetDirectory directory, JsonValue json, float units) {
		float[] pos = json.get( "pos" ).asFloatArray();
		float[] size = json.get( "size" ).asFloatArray();

		obstacle = new BoxObstacle( pos[0], pos[1], size[0], size[1] );
		obstacle.setName( json.name() );
		obstacle.setSensor( true );
		obstacle.setPhysicsUnits( units );

		float w = size[0]*units;
		float h = size[1]*units;
		mesh = new SpriteMesh( -w/2,-h/2, w, h);

		// Technically, we should do error checking here.
		// A JSON field might accidentally be missing
		obstacle.setBodyType( json.get( "bodytype" ).asString().equals( "static" ) ? BodyDef.BodyType.StaticBody : BodyDef.BodyType.DynamicBody );
		obstacle.setDensity( json.get( "density" ).asFloat() );
		obstacle.setFriction( json.get( "friction" ).asFloat() );
		obstacle.setRestitution( json.get( "restitution" ).asFloat() );

		// Create the collision filter (used for light penetration)
		short collideBits = GameLevel.bitStringToShort( json.get( "collide" ).asString() );
		short excludeBits = GameLevel.bitStringToComplement( json.get( "exclude" ).asString() );
		Filter filter = new Filter();
		filter.categoryBits = collideBits;
		filter.maskBits = excludeBits;
		obstacle.setFilterData( filter );

		setDebugColor( ParserUtils.parseColor( json.get( "debug" ), Color.WHITE ) );

		// Now get the texture from the AssetManager singleton
		String key = json.get( "texture" ).asString();
		TextureRegion texture = new TextureRegion( directory.getEntry( key, Texture.class ) );
		setTextureRegion( texture );
	}
}
