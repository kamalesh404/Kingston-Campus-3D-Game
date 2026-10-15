package com.javacity.interaction;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Torus;

public class MissionPortal implements Interactable {
    private final Node node = new Node("MissionPortal");
    private final Vector3f position;
    private final Runnable action;
    private Spatial doorModel;
    private Geometry portalEnergyQuad;
    private Geometry innerRuneRing;
    private PointLight portalLight;
    private float phase;

    public MissionPortal(AssetManager assets, Vector3f position, Runnable action) {
        this.position = position.clone();
        this.action = action;

        Node doorContainer = new Node("PortalDoorContainer");

        try {
            doorModel = assets.loadModel("Models/portal_door.glb");
            doorModel.setName("PortalDoorMesh");
            doorModel.setShadowMode(ShadowMode.CastAndReceive);

            doorModel.updateModelBound();
            BoundingBox bb = (BoundingBox) doorModel.getWorldBound();
            if (bb != null) {
                Vector3f center = bb.getCenter();
                float minY = bb.getMin(null).y;
                doorModel.setLocalTranslation(-center.x, -minY, -center.z);
            }
            doorContainer.attachChild(doorModel);
        } catch (Exception ex) {
            System.err.println("Warning: Failed to load Models/portal_door.glb, using arch fallback: " + ex.getMessage());
            Geometry arch = new Geometry("ArchFallback", new Torus(16, 32, 0.4f, 3.2f));
            Material archMat = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
            archMat.setColor("Color", new ColorRGBA(0.15f, 0.2f, 0.3f, 1f));
            arch.setMaterial(archMat);
            arch.setLocalTranslation(0, 3.2f, 0);
            doorContainer.attachChild(arch);
        }

        // Magical energy curtain inside the doorway
        Quad energyQuad = new Quad(3.2f, 4.8f);
        portalEnergyQuad = new Geometry("PortalVortex", energyQuad);
        Material vortexMat = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        vortexMat.setColor("Color", new ColorRGBA(0.1f, 0.85f, 1.0f, 0.82f));
        vortexMat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        portalEnergyQuad.setMaterial(vortexMat);
        portalEnergyQuad.setQueueBucket(Bucket.Transparent);
        portalEnergyQuad.setLocalTranslation(-1.6f, 0.2f, 0.05f);
        doorContainer.attachChild(portalEnergyQuad);

        // Mystical swirling rune ring
        Torus runeTorus = new Torus(16, 32, 0.12f, 1.5f);
        innerRuneRing = new Geometry("PortalRuneRing", runeTorus);
        Material runeMat = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        runeMat.setColor("Color", new ColorRGBA(0.6f, 0.3f, 1.0f, 0.9f));
        runeMat.getAdditionalRenderState().setBlendMode(BlendMode.Additive);
        innerRuneRing.setMaterial(runeMat);
        innerRuneRing.setQueueBucket(Bucket.Transparent);
        innerRuneRing.setLocalTranslation(0f, 2.5f, 0.08f);
        doorContainer.attachChild(innerRuneRing);

        // Ethereal point light in the doorway
        portalLight = new PointLight();
        portalLight.setColor(new ColorRGBA(0.2f, 0.85f, 1.0f, 1f));
        portalLight.setRadius(14f);
        portalLight.setPosition(new Vector3f(position.x, position.y + 2.5f, position.z));
        node.addLight(portalLight);

        node.attachChild(doorContainer);
        node.setLocalTranslation(this.position);
    }

    public void update(float tpf) {
        phase += tpf;

        if (portalEnergyQuad != null) {
            float pulse = 0.75f + FastMath.sin(phase * 3.5f) * 0.2f;
            ColorRGBA energyColor = new ColorRGBA(0.1f, 0.75f + FastMath.sin(phase * 2f) * 0.15f, 1.0f, pulse);
            portalEnergyQuad.getMaterial().setColor("Color", energyColor);
        }

        if (innerRuneRing != null) {
            innerRuneRing.rotate(0, 0, tpf * 1.5f);
            float ringScale = 1.0f + FastMath.cos(phase * 2.5f) * 0.08f;
            innerRuneRing.setLocalScale(ringScale, ringScale, 1.0f);
        }
    }

    public Node getNode() {
        return node;
    }

    @Override
    public String getInteractionPrompt() {
        return "PRESS [F] TO ENTER THE ARCHER REALM";
    }

    @Override
    public void interact(Object player) {
        if (action != null) {
            action.run();
        }
    }

    @Override
    public Vector3f getPosition() {
        return position;
    }

    @Override
    public float getInteractionRadius() {
        return 8.0f;
    }
}
