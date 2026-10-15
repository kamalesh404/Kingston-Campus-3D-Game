package com.javacity.player;

import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

public class PlayerController {
    private BetterCharacterControl characterControl;
    private float currentSpeed = 0f;
    private final float WALK_SPEED = 7.5f;
    private final float RUN_SPEED = 15.0f;
    private final float SPRINT_SPEED = 24.0f;
    private final float JUMP_FORCE = 25.0f;
    
    private Vector3f walkDirection = new Vector3f();
    private Vector3f viewDirection = new Vector3f(0, 0, 1);
    private Vector3f camDir = new Vector3f();
    private Vector3f camLeft = new Vector3f();

    public PlayerController(BetterCharacterControl characterControl) {
        this.characterControl = characterControl;
        this.characterControl.setJumpForce(new Vector3f(0, 600f, 0));
    }

    public void update(float tpf, Camera cam, boolean[] inputState) {
        boolean forward = inputState[0];
        boolean left = inputState[1];
        boolean backward = inputState[2];
        boolean right = inputState[3];
        boolean sprint = inputState[4];
        boolean jump = inputState[5];

        camDir.set(cam.getDirection()).multLocal(1, 0, 1).normalizeLocal();
        camLeft.set(cam.getLeft()).multLocal(1, 0, 1).normalizeLocal();

        walkDirection.set(0, 0, 0);

        if (forward) walkDirection.addLocal(camDir);
        if (backward) walkDirection.addLocal(camDir.negate());
        if (left) walkDirection.addLocal(camLeft);
        if (right) walkDirection.addLocal(camLeft.negate());

        if (walkDirection.lengthSquared() > 0) {
            walkDirection.normalizeLocal();
            viewDirection.set(walkDirection);
            
            // WALK by default (7.5m/s), SPRINT when LSHIFT is held (24.0m/s)
            float targetSpeed = sprint ? SPRINT_SPEED : WALK_SPEED;
            // Smooth acceleration: factor of 7 gives natural ~0.14s ramp-up at 60fps
            currentSpeed = FastMath.interpolateLinear(tpf * 7.0f, currentSpeed, targetSpeed);
            walkDirection.multLocal(currentSpeed);
        } else {
            // Smooth deceleration: factor of 9 gives natural ~0.11s stop
            currentSpeed = FastMath.interpolateLinear(tpf * 9.0f, currentSpeed, 0f);
            if (currentSpeed > 0.05f) {
                walkDirection.set(viewDirection).multLocal(currentSpeed);
            } else {
                currentSpeed = 0f;
            }
        }

        characterControl.setWalkDirection(walkDirection);
        characterControl.setViewDirection(viewDirection);

        if (jump && characterControl.isOnGround()) {
            characterControl.jump();
        }
    }

    public float getCurrentSpeed() {
        return currentSpeed;
    }
}
