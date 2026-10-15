package com.javacity.vehicles;

import com.jme3.math.ColorRGBA;

public enum VehicleType {
    CYBERPUNK("Models/Vehicles/golf.glb", 1000f, 6500f, 50f, 0.10f, 0.35f, new ColorRGBA(0.96f, 0.96f, 0.98f, 1.0f)), // Pure White
    SCI_FI("Models/Vehicles/golf.glb", 950f, 7000f, 55f, 0.10f, 0.35f, new ColorRGBA(0.96f, 0.96f, 0.98f, 1.0f)),    // Pure White
    SEDAN("Models/Vehicles/golf.glb", 1000f, 5500f, 45f, 0.10f, 0.35f, new ColorRGBA(0.96f, 0.96f, 0.98f, 1.0f)),     // Pure White
    SUV("Models/Vehicles/golf.glb", 1100f, 6000f, 45f, 0.10f, 0.35f, new ColorRGBA(0.96f, 0.96f, 0.98f, 1.0f)),       // Pure White
    TAXI("Models/Vehicles/golf.glb", 1000f, 5500f, 40f, 0.10f, 0.35f, new ColorRGBA(0.96f, 0.96f, 0.98f, 1.0f)),      // Pure White
    POLICE("Models/Vehicles/golf.glb", 1050f, 7500f, 55f, 0.10f, 0.35f, new ColorRGBA(0.96f, 0.96f, 0.98f, 1.0f));    // Pure White

    private final String modelPath;
    private final float mass;
    private final float accelerationForce;
    private final float maxSpeed;
    private final float metallic;
    private final float roughness;
    private final ColorRGBA bodyColor;

    VehicleType(String modelPath, float mass, float accelerationForce, float maxSpeed, 
                float metallic, float roughness, ColorRGBA bodyColor) {
        this.modelPath = modelPath;
        this.mass = mass;
        this.accelerationForce = accelerationForce;
        this.maxSpeed = maxSpeed;
        this.metallic = metallic;
        this.roughness = roughness;
        this.bodyColor = bodyColor;
    }

    public String getModelPath() { return modelPath; }
    public float getMass() { return mass; }
    public float getAccelerationForce() { return accelerationForce; }
    public float getMaxSpeed() { return maxSpeed; }
    public float getMetallic() { return metallic; }
    public float getRoughness() { return roughness; }
    public ColorRGBA getBodyColor() { return bodyColor; }
    public float getColorIntensity() { return 1.0f; }
}
