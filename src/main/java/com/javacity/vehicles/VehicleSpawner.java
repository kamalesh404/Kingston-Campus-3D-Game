package com.javacity.vehicles;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VehicleSpawner {
    private List<Vector3f> spawnPoints;
    private List<Vehicle> activeVehicles;
    private int maxActiveVehicles = com.javacity.core.GameConfig.VEHICLE_POPULATION_LIMIT;
    private final Random random = new Random();
    private AssetManager assetManager;
    private Node rootNode;
    private BulletAppState physicsSpace;
    private com.javacity.world.CityLoader cityLoader;

    public VehicleSpawner(AssetManager assetManager, Node rootNode, BulletAppState physicsSpace, com.javacity.world.CityLoader cityLoader) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;
        this.physicsSpace = physicsSpace;
        this.cityLoader = cityLoader;
        this.spawnPoints = new ArrayList<>();
        this.activeVehicles = new ArrayList<>();
    }

    public void addSpawnPoint(Vector3f position) {
        spawnPoints.add(position);
    }

    public Vehicle spawnVehicle(VehicleType type, Vector3f position, float yRotation) {
        if (activeVehicles.size() >= maxActiveVehicles) {
            return null; 
        }

        Vehicle vehicle = new Vehicle(type);
        vehicle.initialize(assetManager);
        
        float groundY = (cityLoader != null) ? cityLoader.getGroundHeight(position.x, position.z) : position.y;
        if (groundY <= 5.0f) groundY = position.y;
        if (groundY <= 5.0f) groundY = 17.7f;
        Vector3f roadPos = new Vector3f(position.x, groundY + 0.65f, position.z);
        Quaternion rot = new Quaternion();
        rot.fromAngles(0, yRotation, 0);
        vehicle.getVehicleNode().setLocalTranslation(roadPos);
        vehicle.getVehicleNode().setLocalRotation(rot);
        
        if (vehicle.getPhysicsControl() != null) {
            vehicle.getPhysicsControl().setPhysicsLocation(roadPos);
            vehicle.getPhysicsControl().setPhysicsRotation(rot);
        }

        rootNode.attachChild(vehicle.getVehicleNode());
        physicsSpace.getPhysicsSpace().add(vehicle.getPhysicsControl());

        activeVehicles.add(vehicle);
        return vehicle;
    }

    public void despawnVehicle(Vehicle vehicle) {
        rootNode.detachChild(vehicle.getVehicleNode());
        physicsSpace.getPhysicsSpace().remove(vehicle.getVehicleControl());
        activeVehicles.remove(vehicle);
    }

    public Vehicle findNearestVehicle(Vector3f playerPos, float maxDist) {
        Vehicle nearest = null;
        float minDistSq = maxDist * maxDist;

        for (Vehicle vehicle : activeVehicles) {
            float distSq = vehicle.getPosition().distanceSquared(playerPos);
            if (distSq < minDistSq) {
                minDistSq = distSq;
                nearest = vehicle;
            }
        }

        return nearest;
    }

    public void update(float tpf, Vector3f playerPos) {
        float maxDistanceSq = 350f * 350f; 
        
        List<Vehicle> toDespawn = new ArrayList<>();
        for (Vehicle v : activeVehicles) {
            Vector3f pos = v.getPosition();
            
            // Keep vehicles inside the map boundary
            if (!com.javacity.world.SpawnManager.isInsideBoundary(pos)) {
                Vector3f clamped = com.javacity.world.SpawnManager.clampToBoundary(pos);
                float gy = (cityLoader != null) ? cityLoader.getGroundHeight(clamped.x, clamped.z) : clamped.y;
                if (gy <= 5.0f) gy = 17.7f;
                clamped.y = gy + 0.65f;
                v.getVehicleControl().setPhysicsLocation(clamped);
                v.getVehicleControl().setLinearVelocity(Vector3f.ZERO);
                continue;
            }
            
            // Handle void falls for all vehicles (occupied or unoccupied)
            if (pos.y < 5.0f) {
                float gy = (cityLoader != null) ? cityLoader.getGroundHeight(pos.x, pos.z) : 17.7f;
                if (gy <= 5.0f) gy = 17.7f;
                Vector3f respawn = new Vector3f(pos.x, gy + 0.65f, pos.z);
                v.getVehicleControl().setPhysicsLocation(respawn);
                v.getVehicleControl().setLinearVelocity(Vector3f.ZERO);
                v.getVehicleControl().setAngularVelocity(Vector3f.ZERO);
                v.getVehicleControl().setPhysicsRotation(com.jme3.math.Matrix3f.IDENTITY);
                if (!v.isOccupied() && !spawnPoints.isEmpty()) {
                    Vector3f sp = spawnPoints.get(random.nextInt(spawnPoints.size()));
                    float sgy = (cityLoader != null) ? cityLoader.getGroundHeight(sp.x, sp.z) : sp.y;
                    if (sgy <= 5.0f) sgy = 17.7f;
                    v.getVehicleControl().setPhysicsLocation(new Vector3f(sp.x, sgy + 0.65f, sp.z));
                }
                continue;
            }
            
            if (pos.distanceSquared(playerPos) > maxDistanceSq && !v.isOccupied()) {
                toDespawn.add(v);
            }
        }
        
        for (Vehicle v : toDespawn) {
            despawnVehicle(v);
        }
        
        if (activeVehicles.size() < maxActiveVehicles && !spawnPoints.isEmpty()) {
            for (Vector3f sp : spawnPoints) {
                float distSq = sp.distanceSquared(playerPos);
                if (distSq > 30f * 30f && distSq < 200f * 200f) {
                    boolean occupied = false;
                    for (Vehicle existing : activeVehicles) {
                        if (existing.getPosition().distanceSquared(sp) < 15f * 15f) {
                            occupied = true;
                            break;
                        }
                    }
                    if (!occupied) {
                        spawnVehicle(VehicleType.CYBERPUNK, sp, random.nextFloat() * FastMath.TWO_PI);
                        break;
                    }
                }
            }
        }
    }

    public void spawnInitialVehicles() {
        java.util.Random rand = new java.util.Random();
        for (Vector3f sp : spawnPoints) {
            if (activeVehicles.size() >= maxActiveVehicles) break;
            boolean occupied = false;
            for (Vehicle existing : activeVehicles) {
                if (existing.getPosition().distanceSquared(sp) < 15f * 15f) {
                    occupied = true;
                    break;
                }
            }
            if (!occupied) {
                spawnVehicle(VehicleType.CYBERPUNK, sp, rand.nextFloat() * FastMath.TWO_PI);
            }
        }
    }

    public List<Vehicle> getActiveVehicles() {
        return activeVehicles;
    }
}
