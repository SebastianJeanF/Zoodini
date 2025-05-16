/*
 * InputController.java
 *
 * This class buffers in input from the devices and converts it into its
 * semantic meaning. If your game had an option that allows the player to
 * remap the control keys, you would store this information in this class.
 * That way, the main GameEngine does not have to keep track of the current
 * key mapping.
 *
 * @author: Walker M. White
 * 
 * @version: 2/15/2025
 */
package walknroll.zoodini.controllers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import edu.cornell.gdiac.util.Controllers;
import edu.cornell.gdiac.util.XBoxController;
import walknroll.zoodini.utils.Constants;
import walknroll.zoodini.utils.GameSettings;

/**
 * Class for reading player input.
 *
 * This supports both a keyboard and X-Box controller. In previous solutions, we
 * only detected the X-Box controller on start-up. This class allows us to
 * hot-swap in a controller via the new XBoxController class.
 */
public class InputController extends InputAdapter {

	/** The singleton instance of the input controller */
	private static InputController theController = null;

	/**
	 * Return the singleton instance of the input controller
	 *
	 * @return the singleton instance of the input controller
	 */
	public static InputController getInstance() {
		if (theController == null) {
			theController = new InputController();
		}
		return theController;
	}

	// Fields to manage buttons
	/** Whether the reset button was pressed. */
	private boolean resetPressed;
	private boolean resetPrevious;
	/** Whether the button to advanced worlds was pressed. */
	private boolean nextPressed;
	private boolean nextPrevious;
	/** Whether the button to step back worlds was pressed. */
	private boolean prevPressed;
	private boolean prevPrevious;
	/** Whether the debug toggle was pressed. */
	private boolean debugPressed;
	private boolean debugPrevious;
	/** Whether the exit button was pressed. */
	private boolean exitPressed;
	private boolean exitPrevious;
	/** Whether the swap button was pressed. */
	private boolean swapPressed;
	private boolean swapPrevious;

	/** Whether the ability button was pressed. */
	private boolean abilityPressed;
	private boolean abilityPrevious;
	private boolean p2AbilityPressed;
	private boolean p2AbilityPrevious;

	/** Whether the left click was pressed. */
	private boolean leftClicked;
	private boolean leftPrevious;

	/** How much did we move horizontally? */
	private float horizontal;
	private float p2Horizontal;
	/** How much did we move vertically? */
	private float vertical;
	private float p2Vertical;

	/** Where are we targeting? */
	private Vector2 aiming;

	private int abilityKey;
	private int p2AbilityKey;
	private int swapKey;

	private boolean followModePrevious;
	private boolean followModePressed;
	private int followModeKey;

	/** An X-Box controller (if it is connected) */
	XBoxController xbox;
	XBoxController p2xbox;

	/**
	 * Creates a new input controller
	 *
	 * The input controller attempts to connect to the X-Box controller at device 0,
	 * if it exists. Otherwise, it falls back to the keyboard control.
	 */
	public InputController() {
		// If we have a game-pad for id, then use it.
		Array<XBoxController> controllers = Controllers.get().getXBoxControllers();
		if (controllers.size > 0) {
			xbox = controllers.get(0);
		} else {
			xbox = null;
		}
		if (controllers.size > 1) {
			p2xbox = controllers.get(1);
		} else {
			p2xbox = null;
		}
		aiming = new Vector2();
		abilityKey = Input.Keys.E;
		p2AbilityKey = Input.Keys.SHIFT_RIGHT;
		swapKey = Input.Keys.SPACE;
		followModeKey = Input.Keys.F;
	}

	public void setFollowModeKey(int followModeKey) {
		this.followModeKey = followModeKey;
	}

	public boolean didPressFollowMode() {
		return followModePressed && !followModePrevious;
	}

	/**
	 * Returns the amount of sideways movement.
	 *
	 * -1 = left, 1 = right, 0 = still
	 *
	 * @return the amount of sideways movement.
	 */
	public float getHorizontal() {
		return horizontal;
	}

	public float getP2Horizontal() {
		return p2Horizontal;
	}

	/**
	 * Returns the amount of vertical movement.
	 *
	 * -1 = down, 1 = up, 0 = still
	 *
	 * @return the amount of vertical movement.
	 */
	public float getVertical() {
		return vertical;
	}

	public float getP2Vertical() {
		return p2Vertical;
	}

	/**
	 * Returns true if the reset button was pressed.
	 *
	 * @return true if the reset button was pressed.
	 */
	public boolean didReset() {
		return resetPressed && !resetPrevious;
	}

	/**
	 * Returns true if the player wants to go to the next level.
	 *
	 * @return true if the player wants to go to the next level.
	 */
	public boolean didForward() {
		return nextPressed && !nextPrevious;
	}

	/**
	 * Returns true if the player wants to go to the previous level.
	 *
	 * @return true if the player wants to go to the previous level.
	 */
	public boolean didBack() {
		return prevPressed && !prevPrevious;
	}

	/**
	 * Returns true if the player wants to go toggle the debug mode.
	 *
	 * @return true if the player wants to go toggle the debug mode.
	 */
	public boolean didDebug() {
		return debugPressed && !debugPrevious;
	}

	/**
	 * Returns true if the exit button was pressed.
	 *
	 * @return true if the exit button was pressed.
	 */
	public boolean didExit() {
		return exitPressed && !exitPrevious;
	}

	/**
	 * Returns true if the swap button was pressed.
	 *
	 * @return true if the swap button was pressed.
	 */
	public boolean didSwap() {
		return swapPressed && !swapPrevious;
	}

	/**
	 * Returns true if the ability button was pressed.
	 *
	 * @return true if the ability button was pressed
	 */
	public boolean didAbility() {
		return abilityPressed && !abilityPrevious;
	}

	public boolean didP2Ability() {
		return p2AbilityPressed && !p2AbilityPrevious;
	}

	/**
	 * Returns true if the ability button is currently held down.
	 *
	 * @return true if the ability button is currently hold
	 */
	public boolean isAbilityHeld() {
		return abilityPressed;
	}

	public boolean isP2AbilityHeld() {
		return p2AbilityPressed;
	}

	public Vector2 getAiming() {
		return aiming;
	}

	public boolean didLeftClick() {
		return leftClicked && !leftPrevious;
	}

	public void setAbilityKey(int keycode) {
		abilityKey = keycode;
	}

	public void setSwapKey(int keycode) {
		swapKey = keycode;
	}

	/**
	 * Synchronizes the game input with the current animation frame.
	 */
	public void sync() {
		// Copy state from last animation frame
		// Helps us ignore buttons that are held down
		resetPrevious = resetPressed;
		debugPrevious = debugPressed;
		exitPrevious = exitPressed;
		nextPrevious = nextPressed;
		prevPrevious = prevPressed;
		swapPrevious = swapPressed;
		abilityPrevious = abilityPressed;
		p2AbilityPrevious = p2AbilityPressed;
		leftPrevious = leftClicked;
		followModePrevious = followModePressed;

		// Check to see if a GamePad is connected
		if (xbox != null && xbox.isConnected()) {
			readGamepad(xbox, false);
			if (p2xbox != null && p2xbox.isConnected()) {
				readGamepad(p2xbox, true);
			}
			readKeyboard(true);// Read as a back-up
		} else {
			readKeyboard(false);
		}
	}

	/**
	 * Reads input from an X-Box controller connected to this computer.
	 *
	 * The method provides both the input bounds and the drawing scale. It needs
	 * the drawing scale to convert screen coordinates to world coordinates. The
	 * bounds are for the crosshair. They cannot go outside of this zone.
	 *
	 * @param bounds The input bounds for the crosshair.
	 * @param scale  The drawing scale
	 */
	private void readGamepad(XBoxController xbox, boolean p2) {
		resetPressed = xbox.getStart();
		exitPressed = xbox.getBack();
		nextPressed = xbox.getRBumper();
		prevPressed = xbox.getLBumper();
		debugPressed = xbox.getY();
		swapPressed = xbox.getA();
		followModePressed = xbox.getB();

		// Increase animation frame, but only if trying to move
		if (p2) {
			p2Horizontal = xbox.getLeftX();
			p2Vertical = xbox.getLeftY();
		} else {
			horizontal = xbox.getLeftX();
			vertical = xbox.getLeftY();
		}
	}

	/**
	 * Reads input from the keyboard.
	 *
	 * This controller reads from the keyboard regardless of whether or not an
	 * X-Box controller is connected. However, if a controller is connected,
	 * this method gives priority to the X-Box controller.
	 *
	 * @param secondary true if the keyboard should give priority to a gamepad
	 */
	private void readKeyboard(boolean secondary) {
		GameSettings settings = GameSettings.getInstance();

		// Give priority to gamepad results
		resetPressed = (secondary && resetPressed) || (Gdx.input.isKeyPressed(Input.Keys.R));
		debugPressed = (secondary && debugPressed) || (Gdx.input.isKeyPressed(Input.Keys.O));
		prevPressed = (secondary && prevPressed) || (Gdx.input.isKeyPressed(Input.Keys.P));
		nextPressed = (secondary && nextPressed) || (Gdx.input.isKeyPressed(Input.Keys.N));
		exitPressed = (secondary && exitPressed) || (Gdx.input.isKeyPressed(Input.Keys.ESCAPE));
		swapPressed = (secondary && swapPressed) || (Gdx.input.isKeyPressed(settings.getSwapKey()));
		abilityPressed = (secondary && abilityPressed) || (Gdx.input.isKeyPressed(settings
				.getAbilityKey()));
		leftClicked = (secondary && leftClicked) || (Gdx.input.isButtonPressed(Buttons.LEFT));
		followModePressed = (secondary && followModePressed) || (Gdx.input.isKeyPressed(settings
				.getFollowKey()));

		// Directional controls
		horizontal = (secondary ? horizontal : 0.0f);
		if (Gdx.input.isKeyPressed(Input.Keys.D)) {
			horizontal += 1.0f;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.A)) {
			horizontal -= 1.0f;
		}

		vertical = (secondary ? vertical : 0.0f);
		if (Gdx.input.isKeyPressed(Input.Keys.W)) {
			vertical += 1.0f;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.S)) {
			vertical -= 1.0f;
		}

		if (GameSettings.getInstance().isCoopEnabled()) {
			p2AbilityPressed = (secondary && p2AbilityPressed) || (Gdx.input.isKeyPressed(
					this.p2AbilityKey));
			p2Horizontal = (secondary ? p2Horizontal : 0.0f);
			if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
				p2Horizontal += 1.0f;
			}
			if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
				p2Horizontal -= 1.0f;
			}

			p2Vertical = (secondary ? p2Vertical : 0.0f);
			if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
				p2Vertical += 1.0f;
			}
			if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
				p2Vertical -= 1.0f;
			}
		}

		aiming.set(Gdx.input.getX(), Gdx.input.getY());
	}

}
