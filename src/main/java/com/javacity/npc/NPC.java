package com.javacity.npc;

import com.javacity.utilities.AssetHelper;
import com.jme3.anim.AnimComposer;
import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.Geometry;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import java.util.Random;

public class NPC {
    public Node npcNode;
    public Spatial model;
    public BetterCharacterControl characterControl;
    public AnimComposer animComposer;
    public NPCState currentState = NPCState.IDLE;
    public Vector3f currentDestination = new Vector3f();
    // Start with 1.5s idle so physics can settle the capsule onto ground before first walk
    public float idleTimer = 1.5f;
    public float walkTimer = 0f;
    public String modelPath;
    public String npcName;
    public float talkTimer = 0f;
    public boolean canTalk = true;
    public float detectionRadius = 30f;
    public float walkSpeed = 6.0f + new Random().nextFloat() * 4.0f;
    public Vector3f threatPosition = new Vector3f();

    private static final String[] CYBERPUNK_NAMES = {
        "Raven", "Glitch", "Hex", "Byte", "Cipher", "Zero", "Phantom", "Neon", "Spark", "Flux"
    };
    private static final String[] CYBERPUNK_DIALOGUE = {
        "The grid never sleeps...",
        "Data flows like water through the neon veins.",
        "You can trust the code, but never the coder.",
        "The corps are watching. Always.",
        "Even zeros and ones have secrets.",
        "Down here, bandwidth is currency.",
        "The signal is strong, but the truth is static.",
        "Hack the planet, one byte at a time.",
        "Chrome hearts and silicon minds.",
        "The city pulses with electric dreams."
    };

    private AssetManager assetManager;
    private PhysicsSpace physicsSpace;

    public NPC(AssetManager assetManager, PhysicsSpace physicsSpace, String modelPath, Vector3f spawnPos) {
        this.assetManager = assetManager;
        this.physicsSpace = physicsSpace;
        this.modelPath = modelPath;
        this.npcName = CYBERPUNK_NAMES[new Random().nextInt(CYBERPUNK_NAMES.length)];

        npcNode = new Node("NPC_" + npcName + "_" + System.currentTimeMillis());
        
        try {
            model = assetManager.loadModel(modelPath);
        } catch (Exception e) {
            model = createPlaceholderNPC(assetManager);
        }
        
        model.setLocalScale(com.javacity.core.GameConfig.NPC_SCALE);
        npcNode.attachChild(model);
        
        float npcScale = com.javacity.core.GameConfig.NPC_SCALE;
        characterControl = new BetterCharacterControl(0.18f * npcScale, 0.70f * npcScale, 70f);
        npcNode.addControl(characterControl);
        physicsSpace.add(characterControl);
        
        characterControl.warp(spawnPos);
        
        animComposer = AssetHelper.findAnimComposer(model);
        if (animComposer != null) {
            
            com.jme3.anim.AnimClip walkClip = findClipByLength(animComposer, 0.5f, 3.0f);
            com.jme3.anim.AnimClip idleClip = findClipByLength(animComposer, 3.0f, 15.0f);
            
            if (walkClip == null) {
                walkClip = findMixamoClip(animComposer);
            }
            if (idleClip == null) {
                idleClip = walkClip;
            }
            
            try {
                if (walkClip != null) {
                    animComposer.addAction("Walk", animComposer.makeAction(walkClip.getName()));
                    animComposer.addAction("Run", animComposer.makeAction(walkClip.getName()));
                }
                if (idleClip != null) {
                    animComposer.addAction("Idle", animComposer.makeAction(idleClip.getName()));
                }
            } catch (Exception ex) {
                System.err.println("Error registering NPC actions: " + ex.getMessage());
            }
        }
    }
    
    public void update(float tpf) {
    }
    
    public void setDestination(Vector3f dest) {
        this.currentDestination.set(dest);
    }
    
    public Vector3f getPosition() {
        if (characterControl != null && characterControl.getRigidBody() != null) {
            return characterControl.getRigidBody().getPhysicsLocation();
        }
        return npcNode.getWorldTranslation();
    }
    
    public void destroy() {
        if (physicsSpace != null && characterControl != null) {
            physicsSpace.remove(characterControl);
        }
        npcNode.removeFromParent();
    }

    public String getDialogue() {
        return CYBERPUNK_DIALOGUE[new Random().nextInt(CYBERPUNK_DIALOGUE.length)];
    }

    private com.jme3.anim.AnimClip findClipByLength(AnimComposer composer, double minLen, double maxLen) {
        for (com.jme3.anim.AnimClip clip : composer.getAnimClips()) {
            double len = clip.getLength();
            if (len >= minLen && len <= maxLen) {
                return clip;
            }
        }
        return null;
    }

    private com.jme3.anim.AnimClip findMixamoClip(AnimComposer composer) {
        com.jme3.anim.AnimClip bestClip = null;
        double maxSuffix = -1.0;
        
        for (com.jme3.anim.AnimClip clip : composer.getAnimClips()) {
            String name = clip.getName();
            if (name.toLowerCase().contains("mixamo") || name.toLowerCase().contains("layer0")) {
                double suffix = 0.0;
                int lastDot = name.lastIndexOf('.');
                if (lastDot != -1) {
                    try {
                        suffix = Double.parseDouble(name.substring(lastDot));
                    } catch (NumberFormatException e) {
                        suffix = 0.0;
                    }
                } else if (name.endsWith("Layer0")) {
                    suffix = 0.0;
                }
                
                if (suffix > maxSuffix) {
                    maxSuffix = suffix;
                    bestClip = clip;
                }
            }
        }
        
        if (bestClip != null) {
            return bestClip;
        }
        if (!composer.getAnimClips().isEmpty()) {
            return composer.getAnimClips().iterator().next();
        }
        return null;
    }

    private Spatial createPlaceholderNPC(AssetManager assetManager) {
        Node npcGroup = new Node("PlaceholderNPC");
        
        ColorRGBA[] colors = {
            new ColorRGBA(0.85f, 0.25f, 0.25f, 1.0f),
            new ColorRGBA(0.25f, 0.75f, 0.35f, 1.0f),
            new ColorRGBA(0.95f, 0.65f, 0.15f, 1.0f),
            new ColorRGBA(0.65f, 0.25f, 0.85f, 1.0f),
            new ColorRGBA(0.25f, 0.55f, 0.95f, 1.0f)
        };
        ColorRGBA color = colors[new Random().nextInt(colors.length)];
        
        Box bodyBox = new Box(0.18f, 0.40f, 0.12f);
        Geometry bodyGeom = new Geometry("NPCBody", bodyBox);
        Material mat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        mat.setColor("BaseColor", color);
        mat.setFloat("Roughness", 0.5f);
        bodyGeom.setMaterial(mat);
        bodyGeom.setLocalTranslation(0, 0.40f, 0);
        npcGroup.attachChild(bodyGeom);
        
        com.jme3.scene.shape.Sphere headSphere = new com.jme3.scene.shape.Sphere(12, 12, 0.14f);
        Geometry headGeom = new Geometry("NPCHead", headSphere);
        Material headMat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        headMat.setColor("BaseColor", new ColorRGBA(0.95f, 0.85f, 0.75f, 1.0f));
        headGeom.setMaterial(headMat);
        headGeom.setLocalTranslation(0, 0.92f, 0);
        npcGroup.attachChild(headGeom);
        
        return npcGroup;
    }
}
