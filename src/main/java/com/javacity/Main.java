package com.javacity;

import com.javacity.GameApplication;
import com.javacity.core.GameConfig;
import com.jme3.system.AppSettings;

public class Main {
    public static void main(String[] args) {
        // Force Discrete NVIDIA / AMD High-Performance GPU Hardware Acceleration
        System.setProperty("org.lwjgl.opengl.Display.allowSoftwareOpenGL", "false");
        System.setProperty("sun.java2d.opengl", "true");

        // Suppress verbose GLTF extension warnings from console output
        java.util.logging.Logger.getLogger("com.jme3").setLevel(java.util.logging.Level.SEVERE);
        java.util.logging.Logger.getLogger("com.jme3.scene.plugins.gltf").setLevel(java.util.logging.Level.SEVERE);

        GameApplication app = new GameApplication();
        AppSettings settings = new AppSettings(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL32);
        settings.setTitle("KINGSTON ENGINEERING COLLEGE - CAMPUS CHRONICLES");
        settings.setResolution(GameConfig.DISPLAY_WIDTH, GameConfig.DISPLAY_HEIGHT);
        settings.setFullscreen(GameConfig.DISPLAY_FULLSCREEN);
        settings.setFullscreenMode(AppSettings.FULLSCREEN_MODE_BORDERLESS_WINDOW);
        settings.setResizable(false);
        settings.setCenterWindow(true);
        settings.setVSync(GameConfig.DISPLAY_VSYNC);
        settings.setFrameRate(GameConfig.DISPLAY_FPS);
        settings.setSamples(0); // MSAA off -> Low GPU TGP power consumption (~10-15W) & zero overheating
        settings.setBitsPerPixel(24);
        settings.setDepthBits(24);
        settings.setGammaCorrection(true);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }
}
