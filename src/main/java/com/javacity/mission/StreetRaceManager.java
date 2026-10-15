package com.javacity.mission;

import com.javacity.audio.AudioManager;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Torus;
import java.util.ArrayList;
import java.util.List;

public class StreetRaceManager {
    private Node rootNode;
    private AssetManager assetManager;
    private AudioManager audioManager;

    private List<Vector3f> raceCheckpoints = new ArrayList<>();
    private List<Geometry> checkpointRings = new ArrayList<>();
    private int currentCheckpointIndex = 0;

    private boolean raceActive = false;
    private boolean raceCompleted = false;
    private float raceTimer = 0f;
    private float bestRaceTime = 999.9f;

    public StreetRaceManager(Node rootNode, AssetManager assetManager, AudioManager audioManager, com.javacity.world.CityLoader cityLoader) {
        this.rootNode = rootNode;
        this.assetManager = assetManager;
        this.audioManager = audioManager;

        setupRaceTrack(cityLoader);
    }

    public StreetRaceManager(Node rootNode, AssetManager assetManager, AudioManager audioManager) {
        this(rootNode, assetManager, audioManager, null);
    }

    private void setupRaceTrack(com.javacity.world.CityLoader cityLoader) {
        // 8 Spaced Street Checkpoints across Kingston Engineering College Campus
        float[][] coords = {
            {30f, 40f}, {100f, 120f}, {150f, -80f}, {80f, -220f},
            {-60f, -250f}, {-160f, -100f}, {-120f, 100f}, {-20f, 50f}
        };
        for (float[] pt : coords) {
            float y = cityLoader != null ? cityLoader.getGroundHeight(pt[0], pt[1]) + 3.0f : 3.0f;
            raceCheckpoints.add(new Vector3f(pt[0], y, pt[1]));
        }

        // Checkpoint positions recorded without attaching rings to the world by default
    }

    public void startRace() {
        raceActive = true;
        raceCompleted = false;
        currentCheckpointIndex = 0;
        raceTimer = 0f;
        spawnCheckpointRings();
        if (audioManager != null) audioManager.playMissionStart();
    }

    public void stopRace() {
        raceActive = false;
        clearCheckpointRings();
    }

    private void spawnCheckpointRings() {
        clearCheckpointRings();
        for (int i = 0; i < raceCheckpoints.size(); i++) {
            Vector3f pos = raceCheckpoints.get(i);
            Torus torus = new Torus(16, 32, 0.4f, 2.5f);
            Geometry ring = new Geometry("CheckpointRing_" + i, torus);
            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", i == 0 ? ColorRGBA.Yellow : new ColorRGBA(0.1f, 0.75f, 1.0f, 0.8f));
            ring.setMaterial(mat);
            ring.setLocalTranslation(pos);
            ring.setLocalRotation(new com.jme3.math.Quaternion().fromAngleAxis(com.jme3.math.FastMath.HALF_PI, com.jme3.math.Vector3f.UNIT_Y));
            rootNode.attachChild(ring);
            checkpointRings.add(ring);
        }
    }

    public void clearCheckpointRings() {
        for (Geometry ring : checkpointRings) {
            rootNode.detachChild(ring);
        }
        checkpointRings.clear();
    }

    public void update(float tpf, Vector3f vehiclePos) {
        if (!raceActive || raceCompleted) return;

        raceTimer += tpf;

        // Check collision with current active checkpoint ring
        if (currentCheckpointIndex < raceCheckpoints.size()) {
            Vector3f cpPos = raceCheckpoints.get(currentCheckpointIndex);
            if (vehiclePos.distanceSquared(cpPos) < 6.0f * 6.0f) {
                // Passed Checkpoint!
                checkpointRings.get(currentCheckpointIndex).getMaterial().setColor("Color", ColorRGBA.DarkGray);
                currentCheckpointIndex++;
                if (audioManager != null) audioManager.playMissionComplete();

                if (currentCheckpointIndex < raceCheckpoints.size()) {
                    checkpointRings.get(currentCheckpointIndex).getMaterial().setColor("Color", ColorRGBA.Yellow);
                } else {
                    // Race Finished!
                    raceCompleted = true;
                    raceActive = false;
                    clearCheckpointRings();
                    if (raceTimer < bestRaceTime) {
                        bestRaceTime = raceTimer;
                    }
                }
            }
        }
    }

    public boolean isRaceActive() { return raceActive; }
    public boolean isRaceCompleted() { return raceCompleted; }
    public int getCurrentCheckpointIndex() { return currentCheckpointIndex; }
    public int getTotalCheckpoints() { return raceCheckpoints.size(); }
    public float getRaceTimer() { return raceTimer; }
    public float getBestRaceTime() { return bestRaceTime; }
    public List<Vector3f> getRaceCheckpoints() { return raceCheckpoints; }
}
