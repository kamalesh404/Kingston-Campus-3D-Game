package com.javacity.mission;

import com.javacity.audio.AudioManager;
import com.javacity.world.CityLoader;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mission 1: Campus Treasure Hunt.
 * Idle until the player enters the entry gate on the main walkway.
 * On start, GOLD_TOTAL gold bars spawn at random campus spots (primitives
 * only - no real models). Touch a bar to collect it. Collect all to win.
 */
public class TreasureHuntManager {

    public static final int GOLD_TOTAL = 6;
    public static final float COLLECT_RADIUS = 4.0f;
    public static final int REWARD_MONEY = 500;

    private final AssetManager assetManager;
    private final Node rootNode;
    private final CityLoader cityLoader;
    private final AudioManager audioManager;
    private final Random random = new Random();

    private boolean active = false;
    private boolean complete = false;
    private float huntTimer = 0f;
    private int collected = 0;

    private final List<Node> goldNodes = new ArrayList<>();
    private final List<Vector3f> goldPositions = new ArrayList<>();
    private float animPhase = 0f;

    public TreasureHuntManager(AssetManager assetManager, Node rootNode,
                               CityLoader cityLoader, AudioManager audioManager) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;
        this.cityLoader = cityLoader;
        this.audioManager = audioManager;
    }

    public boolean isActive() { return active; }
    public boolean isComplete() { return complete; }
    public int getCollected() { return collected; }
    public int getTotal() { return GOLD_TOTAL; }
    public float getHuntTimer() { return huntTimer; }
    public List<Vector3f> getGoldPositions() { return goldPositions; }

    /** Start the hunt: spawn gold bars at random campus spots. */
    public void start(Vector3f playerPos) {
        if (active) return;
        clearGold();
        collected = 0;
        complete = false;
        huntTimer = 0f;
        spawnGoldBars(playerPos);
        active = true;
        if (audioManager != null) audioManager.playMissionStart();
    }

    public void stop() {
        active = false;
        clearGold();
    }

    private void spawnGoldBars(Vector3f playerPos) {
        int tries = 0;
        while (goldPositions.size() < GOLD_TOTAL && tries < 400) {
            tries++;
            float x = -220f + random.nextFloat() * 440f;
            float z = -340f + random.nextFloat() * 680f;
            float gy = cityLoader != null ? cityLoader.getGroundHeight(x, z) : 15f;
            // Keep bars on walkable ground (not on tall roofs / voids)
            if (gy < 5f || gy > 30f) continue;
            Vector3f pt = new Vector3f(x, gy + 1.0f, z);
            if (playerPos != null && pt.distance(playerPos) < 30f) continue;
            boolean tooClose = false;
            for (Vector3f other : goldPositions) {
                if (pt.distance(other) < 45f) { tooClose = true; break; }
            }
            if (tooClose) continue;
            goldPositions.add(pt);
            goldNodes.add(makeGoldBar(pt, goldPositions.size() - 1));
        }
        for (Node n : goldNodes) {
            rootNode.attachChild(n);
        }
    }

    private Node makeGoldBar(Vector3f pos, int idx) {
        Node node = new Node("GoldBar_" + idx);

        // Gold ingot (primitive box, bright unshaded gold = visible from far)
        Box ingot = new Box(0.6f, 0.18f, 0.3f);
        Geometry gold = new Geometry("Gold", ingot);
        Material goldMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        goldMat.setColor("Color", new ColorRGBA(1.0f, 0.78f, 0.12f, 1f));
        gold.setMaterial(goldMat);
        node.attachChild(gold);

        // Light beam above the bar so players can spot it across campus
        Cylinder beam = new Cylinder(10, 10, 0.45f, 14f, true);
        Geometry beamGeo = new Geometry("Beam", beam);
        Material beamMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        beamMat.setColor("Color", new ColorRGBA(1.0f, 0.85f, 0.2f, 0.35f));
        beamMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        beamGeo.setMaterial(beamMat);
        beamGeo.setQueueBucket(RenderQueue.Bucket.Transparent);
        beamGeo.setLocalTranslation(0, 7f, 0);
        node.attachChild(beamGeo);

        node.setLocalTranslation(pos);
        return node;
    }

    public void update(float tpf, Vector3f playerPos) {
        if (!active || complete) return;
        huntTimer += tpf;
        animPhase += tpf * 2f;

        for (int i = goldNodes.size() - 1; i >= 0; i--) {
            Node n = goldNodes.get(i);
            // Bob + spin so bars are easy to spot
            Vector3f base = goldPositions.get(i);
            n.setLocalTranslation(base.x, base.y + (float) Math.sin(animPhase + i) * 0.3f, base.z);
            n.rotate(0, tpf * 1.5f, 0);

            if (playerPos != null && n.getLocalTranslation().distance(playerPos) <= COLLECT_RADIUS) {
                n.removeFromParent();
                goldNodes.remove(i);
                goldPositions.remove(i);
                collected++;
                if (audioManager != null) audioManager.playMissionComplete();
                if (collected >= GOLD_TOTAL) {
                    complete = true;
                    active = false;
                }
            }
        }
    }

    private void clearGold() {
        for (Node n : goldNodes) {
            n.removeFromParent();
        }
        goldNodes.clear();
        goldPositions.clear();
    }

    public String getTimeString() {
        int mins = (int) (huntTimer / 60f);
        float secs = huntTimer % 60f;
        return String.format("%02d:%04.1f", mins, secs);
    }
}
