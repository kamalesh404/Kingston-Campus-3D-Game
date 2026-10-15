package com.javacity.world;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/**
 * Backdrop node container.
 * Infinite photorealistic 360-degree horizon landscape and sky atmosphere are
 * projected distortion-free by SkyFactory in RealisticSky.java.
 */
public class CityBackdrop {
    private Node backdropNode;

    public CityBackdrop(AssetManager assetManager) {
        this.backdropNode = new Node("CityBackdropNode");
    }

    public void update(Vector3f playerPos) {
    }

    public Node getBackdropNode() {
        return backdropNode;
    }
}
