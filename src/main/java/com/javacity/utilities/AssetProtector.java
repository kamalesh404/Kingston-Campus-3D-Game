package com.javacity.utilities;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.gltf.GlbLoader;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.security.Key;

/**
 * Utility to encrypt and decrypt 3D game models using AES-128.
 * Prevents unauthorized users from extracting .glb models and opening them in Blender / other game engines.
 */
public class AssetProtector {

    // 128-bit AES Key
    private static final byte[] SECRET_KEY = new byte[] {
        (byte) 0x4B, (byte) 0x65, (byte) 0x63, (byte) 0x33, // K e c 3
        (byte) 0x44, (byte) 0x5F, (byte) 0x4B, (byte) 0x69, // D _ K i
        (byte) 0x6E, (byte) 0x67, (byte) 0x73, (byte) 0x74, // n g s t
        (byte) 0x6F, (byte) 0x6E, (byte) 0x32, (byte) 0x31  // o n 2 1
    };

    private static final String ALGORITHM = "AES";

    public static byte[] encrypt(byte[] data) throws Exception {
        Key key = new SecretKeySpec(SECRET_KEY, ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    public static byte[] decrypt(byte[] encryptedData) throws Exception {
        Key key = new SecretKeySpec(SECRET_KEY, ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(encryptedData);
    }

    /**
     * Loads an AES-encrypted 3D model directly into memory and parses it via GltfLoader
     */
    public static Spatial loadEncryptedModel(AssetManager assetManager, String assetPath) {
        try {
            byte[] encryptedBytes = null;

            // 1. Try locating via AssetManager
            try {
                com.jme3.asset.AssetInfo info = assetManager.locateAsset(new ModelKey(assetPath));
                if (info != null) {
                    try (InputStream in = info.openStream()) {
                        encryptedBytes = in.readAllBytes();
                    }
                }
            } catch (Exception ignored) {}

            // 2. Try classpath resource
            if (encryptedBytes == null) {
                try (InputStream in = AssetProtector.class.getClassLoader().getResourceAsStream(assetPath)) {
                    if (in != null) {
                        encryptedBytes = in.readAllBytes();
                    }
                } catch (Exception ignored) {}
            }

            // 3. Try direct file path
            if (encryptedBytes == null) {
                File directFile = new File("assets/" + assetPath);
                if (directFile.exists()) {
                    encryptedBytes = Files.readAllBytes(directFile.toPath());
                }
            }

            if (encryptedBytes == null) {
                System.err.println("Encrypted model not found at: " + assetPath);
                return null;
            }

            // Decrypt the binary in memory
            final byte[] decrypted = decrypt(encryptedBytes);

            // Create in-memory AssetInfo for GltfLoader
            ModelKey virtualKey = new ModelKey(assetPath.replace(".pak", ".glb"));
            AssetInfo info = new AssetInfo(assetManager, virtualKey) {
                @Override
                public InputStream openStream() {
                    return new ByteArrayInputStream(decrypted);
                }
            };

            GlbLoader loader = new GlbLoader();
            return (com.jme3.scene.Spatial) loader.load(info);
        } catch (Exception e) {
            System.err.println("Failed to load encrypted model [" + assetPath + "]: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Encrypt a raw file (.glb) into a protected (.pak) file
     */
    public static boolean encryptFile(File sourceFile, File targetFile) {
        try {
            byte[] raw = Files.readAllBytes(sourceFile.toPath());
            byte[] encrypted = encrypt(raw);
            if (targetFile.getParentFile() != null) {
                targetFile.getParentFile().mkdirs();
            }
            Files.write(targetFile.toPath(), encrypted);
            System.out.println("Encrypted [" + sourceFile.getName() + "] (" + raw.length + " bytes) -> [" 
                + targetFile.getName() + "] (" + encrypted.length + " bytes)");
            return true;
        } catch (Exception e) {
            System.err.println("Failed to encrypt file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
