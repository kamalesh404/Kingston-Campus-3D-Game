package com.javacity.camera;

import com.jme3.input.InputManager;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.*;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;

public class ThirdPersonCamera implements RawInputListener {
    private Camera cam;
    private Node sceneNode;
    private InputManager inputManager;
    
    private float yaw = 0;
    private float pitch = 15 * FastMath.DEG_TO_RAD;
    private float distance = 6.5f;
    
    private float minDistance = 2.5f;
    private float maxDistance = 15.0f;
    private float heightOffset = 2.8f;
    
    // Smoothed target position to eliminate physics jitter
    private Vector3f smoothedTarget = new Vector3f();
    private boolean firstFrame = true;
    
    private boolean inVehicle = false;
    private boolean enabled = true;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            firstFrame = true;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public ThirdPersonCamera(Camera cam, Node sceneNode, InputManager inputManager) {
        this.cam = cam;
        this.sceneNode = sceneNode;
        this.inputManager = inputManager;
        
        inputManager.setCursorVisible(false);
        inputManager.addRawInputListener(this);
    }

    public void update(float tpf, Vector3f targetPosition, boolean isInVehicle) {
        if (!enabled) return;
        if (this.inVehicle != isInVehicle) {
            this.inVehicle = isInVehicle;
            if (this.inVehicle) {
                distance = 10.0f;
                heightOffset = 3.5f;
            } else {
                distance = 6.5f;
                heightOffset = 2.8f;
            }
        }

        // Lock target position smoothly with high-precision lerp to eliminate micro-shaking
        if (firstFrame) {
            smoothedTarget.set(targetPosition);
            firstFrame = false;
        } else {
            // Smooth camera follow at a lag factor that eliminates physics jitter
            float smoothFactor = FastMath.clamp(12.0f * tpf, 0.02f, 0.85f);
            smoothedTarget.interpolateLocal(targetPosition, smoothFactor);
        }

        float currentScale = this.inVehicle ? 1.0f : com.javacity.core.GameConfig.PLAYER_SCALE;
        float actualDistance = distance * (this.inVehicle ? 1.2f : Math.max(1.0f, currentScale * 0.95f));
        float actualHeightOffset = heightOffset * (this.inVehicle ? 1.0f : Math.max(1.0f, currentScale * 0.80f));

        Quaternion rotation = new Quaternion().fromAngles(pitch, yaw, 0);
        Vector3f offset = rotation.mult(new Vector3f(0, 0, actualDistance));
        
        Vector3f lookAtPos = smoothedTarget.add(0, actualHeightOffset, 0);
        Vector3f finalPos = lookAtPos.add(offset);
        
        // Prevent camera from going below ground
        float minCamHeight = smoothedTarget.y + 0.3f;
        if (finalPos.y < minCamHeight) {
            finalPos.y = minCamHeight;
        }
        
        cam.setLocation(finalPos);
        cam.lookAt(lookAtPos, Vector3f.UNIT_Y);
    }

    @Override
    public void onMouseMotionEvent(MouseMotionEvent evt) {
        if (!enabled) return;
        inputManager.setCursorVisible(false);
        yaw -= evt.getDX() * 0.0022f;
        pitch -= evt.getDY() * 0.0022f;
        pitch = FastMath.clamp(pitch, -15 * FastMath.DEG_TO_RAD, 80 * FastMath.DEG_TO_RAD);
        
        if (evt.getDeltaWheel() != 0) {
            distance -= evt.getDeltaWheel() * 0.01f;
            distance = FastMath.clamp(distance, minDistance, maxDistance);
        }
    }

    @Override public void beginInput() {}
    @Override public void endInput() {}
    @Override public void onJoyAxisEvent(JoyAxisEvent evt) {}
    @Override public void onJoyButtonEvent(JoyButtonEvent evt) {}
    @Override public void onMouseButtonEvent(MouseButtonEvent evt) {}
    @Override public void onKeyEvent(KeyInputEvent evt) {}
    @Override public void onTouchEvent(TouchEvent evt) {}
}
