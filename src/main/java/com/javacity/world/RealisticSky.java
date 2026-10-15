package com.javacity.world;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.SkyFactory;

/**
 * Photorealistic 360-degree equirectangular sky & horizon atmosphere system.
 * Uses jMonkeyEngine's native SkyFactory equirectangular projection to render
 * realistic clouds, sky, and distant horizon scenery with ZERO stretching or distortion.
 */
public class RealisticSky {
    private Node skyNode;
    private Spatial skySpatial;
    private AssetManager assetManager;

    public RealisticSky(AssetManager assetManager) {
        this.assetManager = assetManager;
        this.skyNode = new Node("RealisticSkyNode");
        
        buildSkyDome();
    }

    private void buildSkyDome() {
        try {
            skySpatial = SkyFactory.createSky(
                assetManager,
                "Textures/Sky/realistic_sky_atmosphere.jpg",
                SkyFactory.EnvMapType.EquirectMap
            );
            skyNode.attachChild(skySpatial);
        } catch (Exception e) {
            System.err.println("Could not load SkyFactory equirectangular sky: " + e.getMessage());
        }
    }

    public void update(float tpf, Vector3f playerPos) {
        // SkyFactory automatically maintains infinite depth and centers on camera view
    }

    public Node getSkyNode() {
        return skyNode;
    }
}
