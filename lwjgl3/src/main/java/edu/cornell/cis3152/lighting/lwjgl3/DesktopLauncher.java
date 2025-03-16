/*
 * DesktopLauncher.java
 *
 * LibGDX is a cross-platform development library. You write all of your code in
 * the core project.  However, you still need some extra classes if you want to
 * deploy on a specific platform (e.g. PC, Android, Web).  That is the purpose
 * of this class.  It deploys your game on a PC/desktop computer.
 *
 * The only really important part of this file is the method configureApplication.
 * That is where you set your application specific settings.  Everything else
 * should be left alone (except the package, which must be tailored to your
 * application).
 *
 * @author: Walker M. White
 * @date: 1/10/25
 */
package edu.cornell.cis3152.lighting.lwjgl3;

import edu.cornell.cis3152.lighting.GDXRoot;
import edu.cornell.gdiac.backend.*;


/**
 * A class to launch the desktop (LWJGL3) application.
 *
 * This class sets the window size and launches the game. This is where you
 * define your initial application settings
 */
public class DesktopLauncher {

	/**
	 * Classic main method that all Java programmers know.
	 *
	 * This method simply exists to start a new GDXApp. For desktop games,
	 * LibGDX is built on top of LWJGL (this is not the case for Android).
	 *
	 * @param arg Command line arguments
	 */
    public static void main(String[] args) {
        // This handles macOS support and helps on Windows.
        if (StartupHelper.startNewJvmIfRequired()) {
            return;
        }
        new GDXApp(new GDXRoot(), configureApplication());
    }

    /**
     * Returns the application settings
     *
     * This method should be tailored to your application.
     *
     * @return the application settings
     */
    private static GDXAppSettings configureApplication() {
        GDXAppSettings config = new GDXAppSettings();
        config.title = "Box2d Lights";

        // Use windowed mode
        config.fullscreen = false;
        config.width  = 1280;
        config.height = 720;


        config.rawHDPI = false;

        // Use OpenGLES 2.0
        //config.useGL30 = false;

        // Window icons used by Windows (in Resources folder)
        config.iconList = "icons.txt";

        // Vsync limits the frames per second to what your hardware can display
        // and helps eliminate screen tearing.
        // This setting doesn't always work on Linux, so the next line is a safeguard.
        config.vSyncEnabled = true;
        // Limits FPS to the refresh rate of the monitor, plus 1 to try to match
        // fractional refresh rates. Necessary if Vsync fails
        config.foregroundFPS = 60;

        return config;
    }
}
