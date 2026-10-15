package com.javacity.world;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SpawnManager {
    // Map boundary (must match CityLoader). Everything must stay inside this box.
    // Kingston Engineering College Campus Map boundary (X: -260..260, Z: -390..390)
    public static final float BOUND_X_MIN = -260f;
    public static final float BOUND_X_MAX = 260f;
    public static final float BOUND_Z_MIN = -390f;
    public static final float BOUND_Z_MAX = 390f;
    public static final float BOUND_Y_MIN = -20f;
    public static final float BOUND_Y_MAX = 400f;

    private List<Vector3f> npcSpawnPoints = new ArrayList<>();
    private List<Vector3f> vehicleSpawnPoints = new ArrayList<>();
    private List<Float> vehicleSpawnRotations = new ArrayList<>();
    private Vector3f playerSpawnPoint;
    private Vector3f missionNPCSpawnPoint;
    
    private Random random = new Random();

    // Clamps a position to stay within the map boundary so nothing escapes
    // into the surrounding empty area.
    public static Vector3f clampToBoundary(Vector3f pos) {
        float x = FastMath.clamp(pos.x, BOUND_X_MIN, BOUND_X_MAX);
        float y = FastMath.clamp(pos.y, BOUND_Y_MIN, BOUND_Y_MAX);
        float z = FastMath.clamp(pos.z, BOUND_Z_MIN, BOUND_Z_MAX);
        return new Vector3f(x, y, z);
    }

    public static boolean isInsideBoundary(Vector3f pos) {
        return pos.x >= BOUND_X_MIN && pos.x <= BOUND_X_MAX
            && pos.z >= BOUND_Z_MIN && pos.z <= BOUND_Z_MAX;
    }

    public void setupSpawnPoints(CityLoader cityLoader) {
        float playerY = (cityLoader != null) ? cityLoader.getGroundHeight(-226.5f, 300.4f) + 2.5f : 17.7f;
        if (playerY <= 5.0f) playerY = 17.7f;
        playerSpawnPoint = new Vector3f(-226.5f, playerY, 300.4f);

        float missionY = (cityLoader != null) ? cityLoader.getGroundHeight(-215f, 300f) + 2.5f : 17.7f;
        missionNPCSpawnPoint = new Vector3f(-215f, missionY, 300f);

        System.out.println("Kingston College Campus custom Player Spawn Point: " + playerSpawnPoint);

        // Spawns keep a small margin inside the boundary
        float xMin = -220f, xMax = 220f;
        float zMin = -340f, zMax = 340f;

        // Main Walkway NPCs (well-spaced along X: -226.5 from Z: 320 down to Z: -340)
        float[] zWalkway = {
            320f, 280f, 240f, 200f, 160f, 120f, 80f, 40f, 0f, -40f, -80f, -120f, -160f, -200f, -240f, -280f, -320f
        };
        for (float zVal : zWalkway) {
            float xOffset = -226.5f + (random.nextFloat() - 0.5f) * 4.0f;
            float ny = (cityLoader != null) ? cityLoader.getGroundHeight(xOffset, zVal) + 2.5f : 17.7f;
            if (ny <= 5.0f) ny = 17.7f;
            Vector3f pt = new Vector3f(xOffset, ny, zVal);
            if (isFarFromExisting(pt, npcSpawnPoints, 14.0f)) {
                npcSpawnPoints.add(pt);
            }
        }

        // Students & Faculty NPCs spread overall across Kingston College campus quad & buildings
        for (int attempts = 0; attempts < 250 && npcSpawnPoints.size() < 60; attempts++) {
            float cx = (random.nextFloat() - 0.5f) * 440f;
            float cz = (random.nextFloat() - 0.5f) * 640f;
            float ny = (cityLoader != null) ? cityLoader.getGroundHeight(cx, cz) + 2.5f : 17.7f;
            if (ny <= 5.0f) ny = 17.7f;
            Vector3f pt = new Vector3f(cx, ny, cz);
            if (isFarFromExisting(pt, npcSpawnPoints, 14.0f)) {
                npcSpawnPoints.add(pt);
            }
        }

        // Vehicles spawned across college campus driveways and parking lots
        for (int i = 0; i < 25; i++) {
            float x = xMin + random.nextFloat() * (xMax - xMin);
            float z = zMin + random.nextFloat() * (zMax - zMin);
            float vy = (cityLoader != null) ? cityLoader.getGroundHeight(x, z) + 2.0f : 2.0f;
            // Ensure vehicles only spawn on ground level, never on building roofs (> 18.2f)
            if (vy > 18.2f) {
                vy = 17.7f;
            }
            vehicleSpawnPoints.add(new Vector3f(x, vy, z));
            vehicleSpawnRotations.add(random.nextFloat() * FastMath.TWO_PI);
        }

        float[][] fixedPoints = {
            {-210f, 300.4f}, {-180f, 305.0f}, {-138f, 319.1f}, {20f, 30f}, {-30f, 50f}, {80f, -60f}, {-100f, 80f},
            {120f, 150f}, {-150f, -120f}, {40f, -200f}, {-50f, 220f},
            {180f, -280f}, {-200f, 280f}, {0f, -150f}, {0f, 150f}
        };
        for (float[] pt : fixedPoints) {
            float vy = (cityLoader != null) ? cityLoader.getGroundHeight(pt[0], pt[1]) + 2.0f : 2.0f;
            if (vy > 18.2f) {
                vy = 17.7f;
            }
            vehicleSpawnPoints.add(new Vector3f(pt[0], vy, pt[1]));
            vehicleSpawnRotations.add(random.nextFloat() * FastMath.TWO_PI);
        }
    }
    
    public void setupSpawnPoints() {
        setupSpawnPoints(null);
    }
    
    public Vector3f getPlayerSpawnPoint() {
        return playerSpawnPoint;
    }
    
    public Vector3f getMissionNPCSpawnPoint() {
        return missionNPCSpawnPoint;
    }
    
    public Vector3f getRandomNPCSpawnPoint() {
        if (npcSpawnPoints.isEmpty()) return new Vector3f();
        return npcSpawnPoints.get(random.nextInt(npcSpawnPoints.size()));
    }
    
    public Vector3f getRandomVehicleSpawnPoint() {
        if (vehicleSpawnPoints.isEmpty()) return new Vector3f();
        return vehicleSpawnPoints.get(random.nextInt(vehicleSpawnPoints.size()));
    }
    
    public float getVehicleRotation(Vector3f spawnPoint) {
        int idx = vehicleSpawnPoints.indexOf(spawnPoint);
        if (idx >= 0 && idx < vehicleSpawnRotations.size()) {
            return vehicleSpawnRotations.get(idx);
        }
        return 0f;
    }

    private boolean isFarFromExisting(Vector3f candidate, List<Vector3f> existingList, float minDist) {
        float minDistSq = minDist * minDist;
        for (Vector3f existing : existingList) {
            if (existing.distanceSquared(candidate) < minDistSq) {
                return false;
            }
        }
        return true;
    }
}
