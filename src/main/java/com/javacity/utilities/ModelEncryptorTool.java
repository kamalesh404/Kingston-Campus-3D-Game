package com.javacity.utilities;

import com.jme3.app.SimpleApplication;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;

import java.io.File;

/**
 * Headless utility to encrypt 3D assets to .pak and verify in-memory decryption.
 */
public class ModelEncryptorTool extends SimpleApplication {

    public static void main(String[] args) {
        ModelEncryptorTool app = new ModelEncryptorTool();
        AppSettings settings = new AppSettings(true);
        settings.setAudioRenderer(null);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start(com.jme3.system.JmeContext.Type.Headless);
    }

    @Override
    public void simpleInitApp() {
        System.out.println("Running Model Encryption Tool...");

        File sourceGlb = new File("assets/Scenes/City/newclg.glb");
        File targetPak = new File("assets/Scenes/City/newclg.pak");

        if (!sourceGlb.exists()) {
            System.err.println("Source GLB not found at: " + sourceGlb.getAbsolutePath());
            stop();
            return;
        }

        boolean success = AssetProtector.encryptFile(sourceGlb, targetPak);
        if (success) {
            System.out.println("Verifying encrypted model by loading and decrypting in memory...");
            Spatial testLoad = AssetProtector.loadEncryptedModel(assetManager, "Scenes/City/newclg.pak");
            if (testLoad != null) {
                System.out.println("VERIFICATION SUCCESS! Encrypted .pak loaded and parsed correctly in memory!");
                System.out.println("Model node name: " + testLoad.getName());
                System.out.println("Original GLB: " + sourceGlb.length() + " bytes");
                System.out.println("Encrypted PAK: " + targetPak.length() + " bytes");
            } else {
                System.err.println("VERIFICATION FAILED: Could not decrypt and parse model.");
            }
        }

        stop();
    }
}
