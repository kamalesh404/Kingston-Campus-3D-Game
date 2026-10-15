package com.javacity.player;

import com.jme3.anim.AnimComposer;
import com.jme3.anim.Armature;
import com.jme3.anim.SkinningControl;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class Player {
    private Node playerNode;
    private Spatial playerModel;
    private BetterCharacterControl characterControl;
    private PlayerStats stats;
    private PlayerState state;
    private PlayerController controller;
    private PlayerAnimationController animationController;
    private PhysicsSpace physicsSpace;

    public Player(AssetManager assetManager, PhysicsSpace physicsSpace) {
        this.physicsSpace = physicsSpace;
        this.stats = new PlayerStats();
        this.state = PlayerState.ON_FOOT;
        
        this.playerNode = new Node("PlayerNode");
        
        try {
            System.out.println("Loading main player model: " + com.javacity.core.GameConstants.PATH_PLAYER_MODEL);
            this.playerModel = assetManager.loadModel(com.javacity.core.GameConstants.PATH_PLAYER_MODEL);
        } catch (Exception e) {
            System.err.println("Failed to load primary player model, attempting fallback scan: " + e.getMessage());
            try {
                java.io.File customDir = new java.io.File("assets/Models/Player");
                java.io.File[] files = customDir.listFiles((dir, name) -> (name.endsWith(".glb") || name.endsWith(".g3db") || name.endsWith(".fbx")) && !name.equals("mainply.glb"));
                if (files != null && files.length > 0) {
                    String relativePath = "Models/Player/" + files[0].getName();
                    System.out.println("Found fallback player model: " + relativePath);
                    this.playerModel = assetManager.loadModel(relativePath);
                } else {
                    this.playerModel = createPlaceholderPlayer(assetManager);
                }
            } catch (Exception ex) {
                this.playerModel = createPlaceholderPlayer(assetManager);
            }
        }
        
        // Dynamic Animation Merging for Mixamo GLB models
        AnimComposer mainComposer = findAnimComposer(this.playerModel);
        SkinningControl mainSkinControl = findSkinningControl(this.playerModel);
        Armature targetArmature = (mainSkinControl != null) ? mainSkinControl.getArmature() : null;
        
        if (mainComposer != null) {
            System.out.println("Loading custom player model with armature. Joints count: " + (targetArmature != null ? targetArmature.getJointCount() : 0));
            System.out.println("--- MAIN PLAYER CLIPS ---");
            for (com.jme3.anim.AnimClip c : mainComposer.getAnimClips()) {
                System.out.println("Main Clip: name='" + c.getName() + "', length=" + c.getLength() + "s, tracks=" + (c.getTracks() != null ? c.getTracks().length : 0));
            }
            com.jme3.anim.AnimClip walkClip = findMixamoClip(mainComposer);
            if (walkClip != null) {
                com.jme3.anim.AnimClip walkClipCopy = cloneClip(walkClip, "Walk", targetArmature);
                mainComposer.addAnimClip(walkClipCopy);
                try { mainComposer.addAction("Walk", mainComposer.makeAction("Walk")); } catch(Exception e){}
                System.out.println("Mapped Walk clip to " + walkClip.getName());
            }
            
            // Load Idle animation
            try {
                Spatial idleSpatial = assetManager.loadModel("Models/Player/player_idle.glb");
                AnimComposer idleComposer = findAnimComposer(idleSpatial);
                if (idleComposer != null) {
                    System.out.println("--- IDLE CLIPS ---");
                    for (com.jme3.anim.AnimClip c : idleComposer.getAnimClips()) {
                        System.out.println("Idle Clip: name='" + c.getName() + "', length=" + c.getLength() + "s, tracks=" + (c.getTracks() != null ? c.getTracks().length : 0));
                    }
                    com.jme3.anim.AnimClip idleClip = findMixamoClip(idleComposer);
                    if (idleClip != null) {
                        com.jme3.anim.AnimClip idleClipCopy = cloneClip(idleClip, "Idle", targetArmature);
                        mainComposer.addAnimClip(idleClipCopy);
                        try { mainComposer.addAction("Idle", mainComposer.makeAction("Idle")); } catch(Exception e){}
                        System.out.println("Mapped Idle clip to " + idleClip.getName());
                    }
                }
            } catch (Exception ex) {
                System.err.println("Could not load idle animation: " + ex.getMessage());
            }
            
            // Load Run animation
            try {
                Spatial runSpatial = assetManager.loadModel("Models/Player/player_run.glb");
                AnimComposer runComposer = findAnimComposer(runSpatial);
                if (runComposer != null) {
                    System.out.println("--- RUN CLIPS ---");
                    for (com.jme3.anim.AnimClip c : runComposer.getAnimClips()) {
                        System.out.println("Run Clip: name='" + c.getName() + "', length=" + c.getLength() + "s, tracks=" + (c.getTracks() != null ? c.getTracks().length : 0));
                    }
                    com.jme3.anim.AnimClip runClip = findMixamoClip(runComposer);
                    if (runClip != null) {
                        com.jme3.anim.AnimClip runClipCopy = cloneClip(runClip, "Run", targetArmature);
                        com.jme3.anim.AnimClip sprintClip = cloneClip(runClip, "Sprint", targetArmature);
                        mainComposer.addAnimClip(runClipCopy);
                        mainComposer.addAnimClip(sprintClip);
                        try { mainComposer.addAction("Run", mainComposer.makeAction("Run")); } catch(Exception e){}
                        try { mainComposer.addAction("Sprint", mainComposer.makeAction("Sprint")); } catch(Exception e){}
                    }
                }
            } catch (Exception ex) {
                System.err.println("Could not load run animation: " + ex.getMessage());
            }
            
            // Load Jump animation
            try {
                Spatial jumpSpatial = assetManager.loadModel("Models/Player/player_jump.glb");
                AnimComposer jumpComposer = findAnimComposer(jumpSpatial);
                if (jumpComposer != null) {
                    System.out.println("--- JUMP CLIPS ---");
                    for (com.jme3.anim.AnimClip c : jumpComposer.getAnimClips()) {
                        System.out.println("Jump Clip: name='" + c.getName() + "', length=" + c.getLength() + "s, tracks=" + (c.getTracks() != null ? c.getTracks().length : 0));
                    }
                    com.jme3.anim.AnimClip jumpClip = findMixamoClip(jumpComposer);
                    if (jumpClip != null) {
                        com.jme3.anim.AnimClip jumpClipCopy = cloneClip(jumpClip, "Jump", targetArmature);
                        mainComposer.addAnimClip(jumpClipCopy);
                        try { mainComposer.addAction("Jump", mainComposer.makeAction("Jump")); } catch(Exception e){}
                    }
                }
            } catch (Exception ex) {
                System.err.println("Could not load jump animation: " + ex.getMessage());
            }

            // Load Punch animation
            try {
                Spatial punchSpatial = assetManager.loadModel("Models/Player/player_punch.glb");
                AnimComposer punchComposer = findAnimComposer(punchSpatial);
                if (punchComposer != null) {
                    System.out.println("--- PUNCH CLIPS ---");
                    for (com.jme3.anim.AnimClip c : punchComposer.getAnimClips()) {
                        System.out.println("Punch Clip: name='" + c.getName() + "', length=" + c.getLength() + "s, tracks=" + (c.getTracks() != null ? c.getTracks().length : 0));
                    }
                    com.jme3.anim.AnimClip punchClip = findMixamoClip(punchComposer);
                    if (punchClip != null) {
                        com.jme3.anim.AnimClip punchClipCopy = cloneClip(punchClip, "Punch", targetArmature);
                        mainComposer.addAnimClip(punchClipCopy);
                        try { mainComposer.addAction("Punch", mainComposer.makeAction("Punch")); } catch(Exception e){}
                        System.out.println("Mapped Punch clip to " + punchClip.getName());
                    }
                }
            } catch (Exception ex) {
                System.err.println("Could not load punch animation: " + ex.getMessage());
            }

            // Load Drive animation
            try {
                Spatial driveSpatial = assetManager.loadModel("Models/Player/player_drive.glb");
                AnimComposer driveComposer = findAnimComposer(driveSpatial);
                if (driveComposer != null) {
                    System.out.println("--- DRIVE CLIPS ---");
                    for (com.jme3.anim.AnimClip c : driveComposer.getAnimClips()) {
                        System.out.println("Drive Clip: name='" + c.getName() + "', length=" + c.getLength() + "s, tracks=" + (c.getTracks() != null ? c.getTracks().length : 0));
                    }
                    com.jme3.anim.AnimClip driveClip = findMixamoClip(driveComposer);
                    if (driveClip != null) {
                        com.jme3.anim.AnimClip driveClipCopy = cloneClip(driveClip, "Drive", targetArmature);
                        mainComposer.addAnimClip(driveClipCopy);
                        try { mainComposer.addAction("Drive", mainComposer.makeAction("Drive")); } catch(Exception e){}
                        System.out.println("Mapped Drive clip to " + driveClip.getName());
                    }
                }
            } catch (Exception ex) {
                System.err.println("Could not load drive animation: " + ex.getMessage());
            }
        }
        
        this.playerModel.setLocalScale(com.javacity.core.GameConfig.PLAYER_SCALE);
        this.playerNode.attachChild(this.playerModel);
        
        float pScale = com.javacity.core.GameConfig.PLAYER_SCALE;
        float capRadius = 0.40f * pScale;
        float capHeight = 1.60f * pScale;
        this.characterControl = new BetterCharacterControl(capRadius, capHeight, 90f);
        this.playerNode.addControl(this.characterControl);
        this.physicsSpace.add(this.characterControl);
        
        this.controller = new PlayerController(this.characterControl);
        this.animationController = new PlayerAnimationController(this.playerModel);
    }

    public void update(float tpf, com.jme3.renderer.Camera cam, boolean[] inputState) {
        if (state == PlayerState.ON_FOOT) {
            controller.update(tpf, cam, inputState);
            float speed = controller.getCurrentSpeed();
            boolean onGround = characterControl.isOnGround();
            animationController.update(tpf, speed, onGround, false);
        } else if (state == PlayerState.IN_VEHICLE) {
            animationController.update(tpf, 0f, true, true);
        } else if (state == PlayerState.DEAD) {
            animationController.update(tpf, 0f, true, false);
        }
    }

    public void punch() {
        if (state == PlayerState.ON_FOOT && animationController != null) {
            animationController.triggerPunch();
        }
    }

    public Vector3f getPosition() {
        return playerNode.getLocalTranslation();
    }

    public void setPosition(Vector3f position) {
        characterControl.warp(position);
    }

    public Node getNode() {
        return playerNode;
    }

    public BetterCharacterControl getCharacterControl() {
        return characterControl;
    }

    public PlayerStats getStats() {
        return stats;
    }

    public PlayerState getState() {
        return state;
    }

    public void setState(PlayerState state) {
        this.state = state;
    }
    
    public void enterVehicleState() {
        this.state = PlayerState.IN_VEHICLE;
        playerNode.setCullHint(Spatial.CullHint.Inherit);
        if (physicsSpace != null && characterControl != null) {
            physicsSpace.remove(characterControl);
        }
    }

    public void exitVehicleState(Vector3f exitPosition) {
        this.state = PlayerState.ON_FOOT;
        playerNode.setCullHint(Spatial.CullHint.Inherit);
        if (physicsSpace != null && characterControl != null) {
            if (!physicsSpace.getRigidBodyList().contains(characterControl.getRigidBody())) {
                physicsSpace.add(characterControl);
            }
            characterControl.warp(exitPosition);
        }
    }
    
    public void show() {
        playerNode.setCullHint(Spatial.CullHint.Inherit);
        if (physicsSpace != null && characterControl != null) {
            if (!physicsSpace.getRigidBodyList().contains(characterControl.getRigidBody())) {
                physicsSpace.add(characterControl);
            }
        }
    }
    
    public void hide() {
        playerNode.setCullHint(Spatial.CullHint.Always);
        if (physicsSpace != null && characterControl != null) {
            physicsSpace.remove(characterControl);
        }
    }

    private AnimComposer findAnimComposer(Spatial spatial) {
        if (spatial.getControl(AnimComposer.class) != null) {
            return spatial.getControl(AnimComposer.class);
        }
        if (spatial instanceof Node) {
            Node node = (Node) spatial;
            for (Spatial child : node.getChildren()) {
                AnimComposer composer = findAnimComposer(child);
                if (composer != null) return composer;
            }
        }
        return null;
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

    private com.jme3.anim.AnimClip findClipByLengthAndSuffix(AnimComposer composer, double minLen, double maxLen, String suffix) {
        com.jme3.anim.AnimClip bestMatch = null;
        for (com.jme3.anim.AnimClip clip : composer.getAnimClips()) {
            double len = clip.getLength();
            if (len >= minLen && len <= maxLen) {
                if (clip.getName().endsWith(suffix)) {
                    return clip;
                }
                if (bestMatch == null) {
                    bestMatch = clip;
                }
            }
        }
        return bestMatch;
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
            System.out.println("Selected best animation clip: " + bestClip.getName() + " (duration: " + bestClip.getLength() + "s)");
            return bestClip;
        }
        if (!composer.getAnimClips().isEmpty()) {
            com.jme3.anim.AnimClip first = composer.getAnimClips().iterator().next();
            System.out.println("No matching mixamo clip name. Defaulting to first clip: " + first.getName());
            return first;
        }
        return null;
    }

    private com.jme3.anim.SkinningControl findSkinningControl(Spatial spatial) {
        if (spatial.getControl(com.jme3.anim.SkinningControl.class) != null) {
            return spatial.getControl(com.jme3.anim.SkinningControl.class);
        }
        if (spatial instanceof Node) {
            Node node = (Node) spatial;
            for (Spatial child : node.getChildren()) {
                com.jme3.anim.SkinningControl control = findSkinningControl(child);
                if (control != null) return control;
            }
        }
        return null;
    }

    private com.jme3.anim.AnimClip cloneClip(com.jme3.anim.AnimClip original, String newName, Armature targetArmature) {
        com.jme3.anim.AnimClip copy = new com.jme3.anim.AnimClip(newName);
        if (targetArmature == null) {
            copy.setTracks(original.getTracks());
            return copy;
        }
        
        com.jme3.anim.AnimTrack[] originalTracks = original.getTracks();
        com.jme3.anim.AnimTrack[] clonedTracks = new com.jme3.anim.AnimTrack[originalTracks.length];
        
        for (int i = 0; i < originalTracks.length; i++) {
            com.jme3.anim.AnimTrack track = originalTracks[i];
            if (track instanceof com.jme3.anim.TransformTrack) {
                com.jme3.anim.TransformTrack tt = (com.jme3.anim.TransformTrack) track;
                com.jme3.anim.TransformTrack clonedTrack = new com.jme3.anim.TransformTrack(
                    null, // set below
                    tt.getTimes(),
                    tt.getTranslations(),
                    tt.getRotations(),
                    tt.getScales()
                );
                
                com.jme3.anim.util.HasLocalTransform oldTarget = tt.getTarget();
                if (oldTarget instanceof com.jme3.anim.Joint) {
                    com.jme3.anim.Joint oldJoint = (com.jme3.anim.Joint) oldTarget;
                    com.jme3.anim.Joint newJoint = findMatchingJoint(targetArmature, oldJoint.getName(), i);
                    if (newJoint != null) {
                        clonedTrack.setTarget(newJoint);
                    } else {
                        System.err.println("WARNING: Could not find match for joint " + oldJoint.getName());
                        clonedTrack.setTarget(oldTarget);
                    }
                } else {
                    clonedTrack.setTarget(oldTarget);
                }
                clonedTracks[i] = clonedTrack;
            } else {
                clonedTracks[i] = track;
            }
        }
        copy.setTracks(clonedTracks);
        return copy;
    }

    private com.jme3.anim.Joint findMatchingJoint(Armature armature, String oldName, int trackIndex) {
        if (armature == null || oldName == null) return null;
        com.jme3.anim.Joint joint = armature.getJoint(oldName);
        if (joint != null) return joint;
        
        String normalizedOld = cleanBoneName(oldName);
        for (int i = 0; i < armature.getJointCount(); i++) {
            com.jme3.anim.Joint j = armature.getJoint(i);
            if (cleanBoneName(j.getName()).equals(normalizedOld)) {
                return j;
            }
        }
        if (trackIndex >= 0 && trackIndex < armature.getJointCount()) {
            return armature.getJoint(trackIndex);
        }
        return null;
    }

    private String cleanBoneName(String name) {
        if (name == null) return "";
        String clean = name.toLowerCase();
        if (clean.contains(":")) {
            clean = clean.substring(clean.lastIndexOf(":") + 1);
        }
        clean = clean.replaceAll("[^a-zA-Z0-9]", "");
        return clean;
    }

    private Spatial createPlaceholderPlayer(AssetManager assetManager) {
        Node playerGroup = new Node("PlaceholderPlayer");
        
        com.jme3.scene.shape.Box bodyBox = new com.jme3.scene.shape.Box(0.35f, 0.9f, 0.25f);
        com.jme3.scene.Geometry bodyGeom = new com.jme3.scene.Geometry("PlayerBody", bodyBox);
        com.jme3.material.Material mat = new com.jme3.material.Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        mat.setColor("BaseColor", new com.jme3.math.ColorRGBA(0.1f, 0.6f, 1.0f, 1.0f));
        mat.setFloat("Metallic", 0.6f);
        mat.setFloat("Roughness", 0.2f);
        bodyGeom.setMaterial(mat);
        bodyGeom.setLocalTranslation(0, 0.9f, 0);
        playerGroup.attachChild(bodyGeom);
        
        com.jme3.scene.shape.Sphere headSphere = new com.jme3.scene.shape.Sphere(16, 16, 0.3f);
        com.jme3.scene.Geometry headGeom = new com.jme3.scene.Geometry("PlayerHead", headSphere);
        com.jme3.material.Material headMat = new com.jme3.material.Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        headMat.setColor("Color", new com.jme3.math.ColorRGBA(0.0f, 0.9f, 1.0f, 1.0f));
        headGeom.setMaterial(headMat);
        headGeom.setLocalTranslation(0, 2.1f, 0);
        playerGroup.attachChild(headGeom);
        
        return playerGroup;
    }
}
