package com.javacity.npc;

import com.javacity.world.SpawnManager;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class NPCSpawner {
    private List<NPC> activeNPCs = new ArrayList<>();
    private int maxNPCs = com.javacity.core.GameConfig.NPC_POPULATION_LIMIT;
    private String[] civilianModels = {
        "Models/NPC/femalenpc.glb", 
        "Models/NPC/malenpc.glb",
        "Models/NPC/sir.glb"
    };
    
    private AssetManager assetManager;
    private PhysicsSpace physicsSpace;
    private Node rootNode;
    private SpawnManager spawnManager;
    private NPCController npcController;
    private Random random = new Random();

    public NPCSpawner(AssetManager assetManager, PhysicsSpace physicsSpace, Node rootNode, SpawnManager spawnManager) {
        this.assetManager = assetManager;
        this.physicsSpace = physicsSpace;
        this.rootNode = rootNode;
        this.spawnManager = spawnManager;
        this.npcController = new NPCController();
    }
    
    public void spawnInitialNPCs() {
        for (int i = 0; i < maxNPCs; i++) {
            Vector3f pos = spawnManager.getRandomNPCSpawnPoint();
            if (pos != null) {
                spawnNPC(pos);
            }
        }
    }
    
    public NPC spawnNPC(Vector3f pos) {
        if (activeNPCs.size() >= maxNPCs) return null;
        
        // Spacing check: ensure no active NPC is within 12m of spawn point
        for (NPC existing : activeNPCs) {
            if (existing.getPosition().distanceSquared(pos) < 12.0f * 12.0f) {
                return null;
            }
        }
        
        String model = civilianModels[random.nextInt(civilianModels.length)];
        NPC npc = new NPC(assetManager, physicsSpace, model, pos);
        rootNode.attachChild(npc.npcNode);
        activeNPCs.add(npc);
        return npc;
    }
    
    public void despawnNPC(NPC npc) {
        npc.destroy();
        activeNPCs.remove(npc);
    }
    
    public void update(float tpf, Vector3f playerPos) {
        Iterator<NPC> iter = activeNPCs.iterator();
        while (iter.hasNext()) {
            NPC npc = iter.next();
            Vector3f pos = npc.getPosition();
            
            // If NPC fell through geometry or void, warp them back to street level
            if (pos.y < -5.0f) {
                Vector3f respawnPos = spawnManager.getRandomNPCSpawnPoint();
                if (respawnPos != null) {
                    npc.characterControl.warp(respawnPos);
                } else {
                    npc.characterControl.warp(new Vector3f(pos.x, 10.0f, pos.z));
                }
                continue;
            }

            // Keep NPCs inside the map boundary - warp back if they wandered out
            if (!SpawnManager.isInsideBoundary(pos)) {
                Vector3f clamped = SpawnManager.clampToBoundary(pos);
                clamped.y = 10.0f;
                npc.characterControl.warp(clamped);
                continue;
            }
            
            // Despawn only if very far away (350+ units)
            if (pos.distanceSquared(playerPos) > 350f * 350f) {
                npc.destroy();
                iter.remove();
            } else {
                if (pos.distanceSquared(playerPos) <= com.javacity.core.GameConfig.NPC_SIMULATION_DISTANCE
                        * com.javacity.core.GameConfig.NPC_SIMULATION_DISTANCE) {
                    npcController.update(tpf, npc);
                    npcController.avoidNearbyNPCs(npc, activeNPCs);
                } else {
                    npc.characterControl.setWalkDirection(Vector3f.ZERO);
                }
            }
        }
        
        if (activeNPCs.size() < maxNPCs) {
            Vector3f pos = spawnManager.getRandomNPCSpawnPoint();
            if (pos != null && pos.distance(playerPos) < 200f && pos.distance(playerPos) > 15f) {
                spawnNPC(pos);
            }
        }
    }
    
    public NPC findNearestNPC(Vector3f playerPos, float maxDist) {
        NPC nearest = null;
        float minDistSq = maxDist * maxDist;
        for (NPC npc : activeNPCs) {
            float distSq = npc.getPosition().distanceSquared(playerPos);
            if (distSq < minDistSq) {
                minDistSq = distSq;
                nearest = npc;
            }
        }
        return nearest;
    }

    public List<NPC> getActiveNPCs() {
        return activeNPCs;
    }
}
