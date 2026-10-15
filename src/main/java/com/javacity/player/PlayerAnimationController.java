package com.javacity.player;

import com.jme3.anim.AnimComposer;
import com.jme3.math.FastMath;
import com.jme3.scene.Spatial;

public class PlayerAnimationController {
    private AnimComposer animComposer;
    private String currentAction = "";
    private float airborneTimer = 0f;
    private boolean isPunching = false;
    private float punchTimer = 0f;

    // Smoothed animation playback speed to prevent abrupt speed jumps
    private float smoothedAnimSpeed = 1.0f;

    public PlayerAnimationController(Spatial playerModel) {
        this.animComposer = com.javacity.utilities.AssetHelper.findAnimComposer(playerModel);
    }

    public void triggerPunch() {
        this.isPunching = true;
        this.punchTimer = 0.60f;
    }

    public void update(float tpf, float speed, boolean isOnGround, boolean isInVehicle) {
        if (animComposer == null) return;

        if (isPunching) {
            punchTimer -= tpf;
            if (punchTimer <= 0) {
                isPunching = false;
            }
        }

        if (!isOnGround) {
            airborneTimer += tpf;
        } else {
            airborneTimer = 0f;
        }

        String targetAction = currentAction.isEmpty() ? "Idle" : currentAction;

        if (isInVehicle) {
            targetAction = "Drive";
        } else if (isPunching) {
            targetAction = "Punch";
        } else if (!isOnGround && airborneTimer > 0.08f) {
            // Delay jump anim start slightly to avoid flicker when stepping off ledges
            targetAction = "Jump";
        } else if (isOnGround) {
            // Hysteresis thresholding: wide dead-bands prevent rapid Walk<->Run flickering
            if (currentAction.equals("Idle") || currentAction.equals("Punch") || currentAction.equals("Jump")) {
                if (speed > 1.0f) {
                    targetAction = speed > 14.0f ? "Run" : "Walk";
                } else {
                    targetAction = "Idle";
                }
            } else if (currentAction.equals("Walk")) {
                // Only switch out of Walk if clearly stopped or clearly running
                if (speed < 0.5f) {
                    targetAction = "Idle";
                } else if (speed > 15.0f) {
                    targetAction = "Run";
                } else {
                    targetAction = "Walk"; // Stay in Walk
                }
            } else if (currentAction.equals("Run")) {
                if (speed < 0.5f) {
                    targetAction = "Idle";
                } else if (speed < 8.0f) {
                    targetAction = "Walk";
                } else if (speed > 21.0f) {
                    targetAction = "Sprint";
                } else {
                    targetAction = "Run"; // Stay in Run
                }
            } else if (currentAction.equals("Sprint")) {
                if (speed < 0.5f) {
                    targetAction = "Idle";
                } else if (speed < 17.0f) {
                    targetAction = "Run";
                } else {
                    targetAction = "Sprint";
                }
            } else {
                targetAction = speed < 1.0f ? "Idle" : "Walk";
            }
        }

        if (!targetAction.equals(currentAction)) {
            if (animComposer.getAnimClip(targetAction) != null || animComposer.getAction(targetAction) != null) {
                try {
                    animComposer.setCurrentAction(targetAction);
                    currentAction = targetAction;
                    smoothedAnimSpeed = 1.0f; // Reset on transition
                } catch (Exception e) {
                    // Fallback on error
                }
            } else if (animComposer.getAnimClip("Idle") != null && !currentAction.equals("Idle")) {
                try {
                    animComposer.setCurrentAction("Idle");
                    currentAction = "Idle";
                    smoothedAnimSpeed = 1.0f;
                } catch (Exception e) {}
            }
        }

        // Compute target animation playback speed
        float targetAnimSpeed;
        if ("Walk".equals(currentAction)) {
            // Normalize: walk speed of 7.5 = 1.0x playback
            targetAnimSpeed = FastMath.clamp(speed / 7.5f, 0.7f, 1.2f);
        } else if ("Run".equals(currentAction)) {
            // Normalize: run speed of 15 = 1.0x playback
            targetAnimSpeed = FastMath.clamp(speed / 15.0f, 0.8f, 1.2f);
        } else if ("Sprint".equals(currentAction)) {
            targetAnimSpeed = FastMath.clamp(speed / 22.0f, 0.9f, 1.3f);
        } else if ("Punch".equals(currentAction)) {
            targetAnimSpeed = 2.0f;
        } else if ("Jump".equals(currentAction)) {
            targetAnimSpeed = 1.4f;
        } else {
            targetAnimSpeed = 1.0f;
        }

        // Smoothly interpolate animation speed to avoid sudden tempo jumps
        smoothedAnimSpeed = FastMath.interpolateLinear(tpf * 6.0f, smoothedAnimSpeed, targetAnimSpeed);
        animComposer.setGlobalSpeed(smoothedAnimSpeed);
    }
}
