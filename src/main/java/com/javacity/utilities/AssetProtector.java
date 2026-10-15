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

    // Dynamic runtime key derivation seed (XOR masked & entropy-shifted)
    private static final byte[] K_SEED = new byte[] {
        (byte) 227, (byte) 43, (byte) 68, (byte) 210,
        (byte) 200, (byte) 53, (byte) 64, (byte) 148,
        (byte) 142, (byte) 113, (byte) 28, (byte) 237,
        (byte) 171, (byte) 92, (byte) 97, (byte) 132
    };
    private static final byte[] K_MASK = new byte[] { (byte) 0xA5, (byte) 0x5A, (byte) 0x3C, (byte) 0xC3 };

    private static Key getSecureKey() {
        byte[] raw = new byte[16];
        for (int i = 0; i < 16; i++) {
            raw[i] = (byte) (K_SEED[i] ^ K_MASK[i % 4] ^ ((i * 7 + 13) & 0xFF));
        }
        return new SecretKeySpec(raw, "AES");
    }

    private static final String ALGORITHM = "AES";

    public static byte[] encrypt(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, getSecureKey());
        return cipher.doFinal(data);
    }

    public static byte[] decrypt(byte[] encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, getSecureKey());
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
