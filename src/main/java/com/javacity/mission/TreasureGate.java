package com.javacity.mission;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

/**
 * Glowing entry arch on the main walkway (primitives only).
 * Walking up to it shows "PRESS E - Campus Treasure Hunt".
 */
public class TreasureGate {
    private final Node node = new Node("TreasureGate");
    private final Vector3f position;
    private float phase = 0f;
    private final Geometry topBar;

    public TreasureGate(AssetManager assetManager, Vector3f position) {
        this.position = position.clone();

        Material pillarMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        pillarMat.setColor("Color", new ColorRGBA(0.1f, 0.8f, 1.0f, 1f));
        Material topMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        topMat.setColor("Color", new ColorRGBA(1.0f, 0.8f, 0.1f, 1f));

        // Two pillars
        for (float sx : new float[]{-2.2f, 2.2f}) {
            Geometry pillar = new Geometry("GatePillar", new Box(0.4f, 3f, 0.4f));
            pillar.setMaterial(pillarMat);
            pillar.setLocalTranslation(sx, 3f, 0);
            node.attachChild(pillar);
        }
        // Top bar
        topBar = new Geometry("GateTop", new Box(2.6f, 0.4f, 0.4f));
        topBar.setMaterial(topMat);
        topBar.setLocalTranslation(0, 6.2f, 0);
        node.attachChild(topBar);

        // Floating gold ingot icon above the arch
        Geometry ingot = new Geometry("GateIngot", new Box(0.6f, 0.18f, 0.3f));
        Material goldMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        goldMat.setColor("Color", new ColorRGBA(1.0f, 0.78f, 0.12f, 1f));
        ingot.setMaterial(goldMat);
        ingot.setLocalTranslation(0, 7.2f, 0);
        ingot.setName("GateIngot");
        node.attachChild(ingot);

        node.setLocalTranslation(position);
    }

    public Node getNode() { return node; }
    public Vector3f getPosition() { return position; }

    public void update(float tpf) {
        phase += tpf * 2f;
        Geometry ingot = (Geometry) node.getChild("GateIngot");
        if (ingot != null) {
            ingot.setLocalTranslation(0, 7.2f + (float) Math.sin(phase) * 0.25f, 0);
            ingot.rotate(0, tpf * 1.5f, 0);
        }
    }
}
