package com.javacity.vehicles;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

public class Vehicle {
    private Node vehicleNode;
    private RigidBodyControl rigidBodyControl;
    private String modelPath;
    private float maxSpeed;
    private float acceleration;
    private float brakeForce;
    private float steeringMax;
    private float health;
    private boolean isOccupied;
    private VehicleType type;
    private float currentSpeed = 0f;

    public Vehicle(VehicleType type) {
        this.type = type;
        this.modelPath = type.getModelPath();
        this.maxSpeed = type.getMaxSpeed();
        this.acceleration = type.getAccelerationForce();
        this.brakeForce = type.getMass() * 2f;
        this.steeringMax = FastMath.QUARTER_PI * 0.5f;
        this.health = 100f;
        this.isOccupied = false;
    }

    public void initialize(AssetManager assetManager) {
        Spatial model = null;
        try {
            model = assetManager.loadModel(modelPath);
        } catch (Exception e) {
            model = createPlaceholderVehicle(assetManager);
        }
        float scale = com.javacity.core.GameConfig.CAR_SCALE;
        model.setLocalScale(scale);
        model.setLocalTranslation(0, 0.20f, 0);
        orientModel(model);
        applyVehicleMaterials(assetManager, model);

        vehicleNode = new Node("VehicleNode");
        vehicleNode.attachChild(model);

        // Kinematic rigid body physics chassis that never clips, falls under map, or flips 90 degrees
        BoxCollisionShape boxShape = new BoxCollisionShape(new Vector3f(0.95f, 0.55f, 1.45f));
        rigidBodyControl = new RigidBodyControl(boxShape, type.getMass());
        rigidBodyControl.setKinematic(true);
        
        vehicleNode.addControl(rigidBodyControl);
    }

    private void orientModel(Spatial model) {
        if (model == null) return;
        model.updateModelBound();
        if (model.getWorldBound() instanceof com.jme3.bounding.BoundingBox) {
            com.jme3.bounding.BoundingBox bbox = (com.jme3.bounding.BoundingBox) model.getWorldBound();
            if (bbox.getXExtent() > bbox.getZExtent()) {
                model.setLocalRotation(new com.jme3.math.Quaternion().fromAngles(0, FastMath.HALF_PI, 0));
            } else {
                model.setLocalRotation(new com.jme3.math.Quaternion().fromAngles(0, 0, 0));
            }
        } else {
            model.setLocalRotation(new com.jme3.math.Quaternion().fromAngles(0, 0, 0));
        }
    }

    private void applyVehicleMaterials(AssetManager assetManager, Spatial spatial) {
        if (spatial instanceof com.jme3.scene.Geometry) {
            com.jme3.scene.Geometry geo = (com.jme3.scene.Geometry) spatial;
            Material mat = geo.getMaterial();
            if (mat != null && mat.getMaterialDef().getName().contains("PBR")) {
                String geoName = (geo.getName() != null ? geo.getName() : "").toLowerCase();
                String matName = (mat.getName() != null ? mat.getName() : "").toLowerCase();
                
                if (matName.contains("glass") || matName.contains("window") || geoName.contains("glass") || geoName.contains("windshield")) {
                    mat.setFloat("Metallic", 0.10f);
                    mat.setFloat("Roughness", 0.05f);
                    mat.setColor("BaseColor", new ColorRGBA(0.35f, 0.65f, 0.95f, 0.6f));
                } else if (matName.contains("light") || matName.contains("led") || matName.contains("neon") || geoName.contains("light") || geoName.contains("headlight")) {
                    mat.setFloat("Metallic", 0.0f);
                    mat.setFloat("Roughness", 0.08f);
                    mat.setColor("BaseColor", new ColorRGBA(1.0f, 0.96f, 0.85f, 1.0f));
                    mat.getAdditionalRenderState().setBlendMode(com.jme3.material.RenderState.BlendMode.Additive);
                } else if (matName.contains("tire") || matName.contains("wheel") || matName.contains("rubber") || geoName.contains("tire") || geoName.contains("wheel")) {
                    mat.setFloat("Metallic", 0.0f);
                    mat.setFloat("Roughness", 0.85f);
                    mat.setColor("BaseColor", new ColorRGBA(0.08f, 0.08f, 0.08f, 1.0f));
                } else if (matName.contains("seat") || geoName.contains("seat") || matName.contains("interior") || matName.contains("cushion")) {
                    mat.setFloat("Metallic", 0.02f);
                    mat.setFloat("Roughness", 0.65f);
                    mat.setColor("BaseColor", new ColorRGBA(0.18f, 0.18f, 0.20f, 1.0f));
                } else if (matName.contains("chrome") || matName.contains("pole") || matName.contains("strut") || matName.contains("frame") || geoName.contains("pole") || geoName.contains("roof_support") || geoName.contains("frame")) {
                    mat.setFloat("Metallic", 0.95f);
                    mat.setFloat("Roughness", 0.10f);
                    mat.setColor("BaseColor", new ColorRGBA(0.90f, 0.92f, 0.96f, 1.0f));
                } else if (matName.contains("roof") || geoName.contains("canopy") || geoName.contains("roof")) {
                    mat.setFloat("Metallic", 0.15f);
                    mat.setFloat("Roughness", 0.35f);
                    mat.setColor("BaseColor", new ColorRGBA(0.92f, 0.92f, 0.94f, 1.0f));
                } else {
                    // Main vehicle painted body panels
                    if (mat.getParam("BaseColorMap") == null) {
                        mat.setColor("BaseColor", type.getBodyColor());
                        mat.setFloat("Metallic", type.getMetallic());
                        mat.setFloat("Roughness", type.getRoughness());
                    } else {
                        mat.setColor("BaseColor", ColorRGBA.White);
                    }
                }
            }
        }
        
        if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                applyVehicleMaterials(assetManager, child);
            }
        }
    }

    public void updateVisualBodyDynamics(float tpf, float throttle, float steering) {
        Spatial m = vehicleNode.getChild(0);
        if (m == null) return;
        m.setLocalRotation(new com.jme3.math.Quaternion().fromAngles(0, 0, 0));
    }

    public void setDrifting(boolean drifting) {}

    public void accelerate(float value) {}
    public void brake(float value) {}
    public void steer(float value) {}
    public void handbrake() {}

    public float getCurrentSpeed() {
        return currentSpeed;
    }

    public void setCurrentSpeed(float currentSpeed) {
        this.currentSpeed = currentSpeed;
    }

    public Vector3f getPosition() {
        if (rigidBodyControl != null) {
            return rigidBodyControl.getPhysicsLocation();
        }
        return vehicleNode.getWorldTranslation();
    }

    public void reset() {
        if (rigidBodyControl != null) {
            rigidBodyControl.clearForces();
            rigidBodyControl.setLinearVelocity(Vector3f.ZERO);
            rigidBodyControl.setAngularVelocity(Vector3f.ZERO);
        }
    }

    public Node getVehicleNode() {
        return vehicleNode;
    }

    public RigidBodyControl getRigidBodyControl() {
        return rigidBodyControl;
    }

    public RigidBodyControl getVehicleControl() {
        return rigidBodyControl;
    }

    public RigidBodyControl getPhysicsControl() {
        return rigidBodyControl;
    }

    public boolean isOccupied() {
        return isOccupied;
    }

    public void setOccupied(boolean occupied) {
        this.isOccupied = occupied;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public VehicleType getType() {
        return type;
    }

    public float getHealth() {
        return health;
    }

    private Spatial createPlaceholderVehicle(AssetManager assetManager) {
        Node carGroup = new Node("PlaceholderVehicle");
        
        Box bodyBox = new Box(0.9f, 0.45f, 1.8f);
        com.jme3.scene.Geometry bodyGeom = new com.jme3.scene.Geometry("CarBody", bodyBox);
        Material bodyMat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        bodyMat.setColor("BaseColor", new ColorRGBA(0.15f, 0.45f, 0.85f, 1.0f));
        bodyMat.setFloat("Metallic", 0.8f);
        bodyMat.setFloat("Roughness", 0.2f);
        bodyGeom.setMaterial(bodyMat);
        bodyGeom.setLocalTranslation(0, 0.5f, 0);
        carGroup.attachChild(bodyGeom);
        
        Box cabinBox = new Box(0.75f, 0.35f, 1.0f);
        com.jme3.scene.Geometry cabinGeom = new com.jme3.scene.Geometry("CarCabin", cabinBox);
        Material cabinMat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        cabinMat.setColor("BaseColor", new ColorRGBA(0.1f, 0.15f, 0.25f, 1.0f));
        cabinMat.setFloat("Metallic", 0.3f);
        cabinMat.setFloat("Roughness", 0.1f);
        cabinGeom.setMaterial(cabinMat);
        cabinGeom.setLocalTranslation(0, 1.1f, -0.2f);
        carGroup.attachChild(cabinGeom);
        
        Box lightBox = new Box(0.25f, 0.08f, 0.04f);
        com.jme3.scene.Geometry lightL = new com.jme3.scene.Geometry("HeadlightL", lightBox);
        com.jme3.scene.Geometry lightR = new com.jme3.scene.Geometry("HeadlightR", lightBox);
        Material lightMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        lightMat.setColor("Color", new ColorRGBA(1.0f, 0.95f, 0.7f, 1.0f));
        lightL.setMaterial(lightMat);
        lightR.setMaterial(lightMat);
        lightL.setLocalTranslation(-0.5f, 0.5f, 1.82f);
        lightR.setLocalTranslation(0.5f, 0.5f, 1.82f);
        carGroup.attachChild(lightL);
        carGroup.attachChild(lightR);
        
        return carGroup;
    }
}
