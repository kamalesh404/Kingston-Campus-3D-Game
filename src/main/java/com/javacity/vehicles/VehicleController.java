package com.javacity.vehicles;

import com.javacity.world.CityLoader;
import com.javacity.world.SpawnManager;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public class VehicleController {

    private float currentSpeed = 0f;
    private float groundProbeTimer = 0f;
    private float lastGroundHeight = 17.7f;

    public void update(float tpf, Vehicle vehicle, boolean[] inputState, CityLoader cityLoader) {
        if (vehicle == null) return;
        com.jme3.bullet.control.RigidBodyControl body = vehicle.getPhysicsControl();
        if (body == null) return;

        body.activate();

        boolean wPressed = inputState[0];
        boolean aPressed = inputState[1];
        boolean sPressed = inputState[2];
        boolean dPressed = inputState[3];
        boolean spacePressed = (inputState.length > 5) ? inputState[5] : (inputState.length > 4 ? inputState[4] : false);

        Vector3f currentPos = body.getPhysicsLocation();

        // Steering Yaw calculation
        float[] angles = new float[3];
        body.getPhysicsRotation().toAngles(angles);
        float currentYaw = angles[1];

        float steerSpeed = 2.4f;
        if (aPressed) {
            currentYaw += steerSpeed * tpf;
        }
        if (dPressed) {
            currentYaw -= steerSpeed * tpf;
        }

        // Lock pitch and roll to 0 so car NEVER flips 90 degrees or tilts
        Quaternion uprightRot = new Quaternion().fromAngles(0, currentYaw, 0);

        // Target speed calculation
        float targetSpeed = 0f;
        float maxForwardSpeed = 16.0f; // Smooth crisp driving speed (~58 km/h)
        float maxReverseSpeed = -8.0f; // Reverse speed (~29 km/h)

        if (wPressed) {
            targetSpeed = maxForwardSpeed;
        } else if (sPressed) {
            targetSpeed = maxReverseSpeed;
        }

        float accelRate = (wPressed || sPressed) ? 12.0f : 8.0f;
        if (spacePressed) {
            targetSpeed = 0f;
            accelRate = 30.0f;
        }

        currentSpeed = FastMath.interpolateLinear(FastMath.clamp(tpf * accelRate, 0.05f, 1.0f), currentSpeed, targetSpeed);
        vehicle.setCurrentSpeed(currentSpeed);

        // Compute forward displacement along Z axis
        Vector3f fwd = uprightRot.getRotationColumn(2);
        float moveDist = currentSpeed * tpf;

        float nextX = currentPos.x + fwd.x * moveDist;
        float nextZ = currentPos.z + fwd.z * moveDist;

        // Ground height probing directly from map mesh surface
        // Mesh raycasts are expensive against the full campus model. Probe at
        // 12 Hz and reuse the last valid result between probes; this is below
        // the threshold at which vehicle suspension changes are noticeable.
        groundProbeTimer -= tpf;
        if (groundProbeTimer <= 0f) {
            groundProbeTimer = 1f / 12f;
            if (cityLoader != null) {
                float measured = cityLoader.getGroundHeight(nextX, nextZ);
                if (measured > 5.0f) lastGroundHeight = measured;
            }
        }
        float sampleY = lastGroundHeight;
        if (sampleY <= 5.0f) {
            sampleY = (currentPos.y > 10.0f) ? (currentPos.y - 0.50f) : 17.7f;
        }
        float targetY = sampleY + 0.65f;

        float nextY = FastMath.interpolateLinear(FastMath.clamp(tpf * 15.0f, 0.1f, 1.0f), currentPos.y, targetY);

        Vector3f nextPos = SpawnManager.clampToBoundary(new Vector3f(nextX, nextY, nextZ));

        body.setPhysicsLocation(nextPos);
        body.setPhysicsRotation(uprightRot);
        vehicle.getVehicleNode().setLocalTranslation(nextPos);
        vehicle.getVehicleNode().setLocalRotation(uprightRot);

        vehicle.updateVisualBodyDynamics(tpf, (wPressed ? 1.0f : (sPressed ? -0.5f : 0f)), 0f);
    }

    public void update(float tpf, Vehicle vehicle, boolean[] inputState) {
        update(tpf, vehicle, inputState, null);
    }
}
