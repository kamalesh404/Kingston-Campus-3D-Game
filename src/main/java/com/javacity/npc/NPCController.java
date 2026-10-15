package com.javacity.npc;

import com.javacity.utilities.MathUtils;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.List;
import java.util.Random;

public class NPCController {
    private Random random = new Random();

    public void update(float tpf, NPC npc) {
        npc.update(tpf);

        switch (npc.currentState) {
            case IDLE:
                handleIdle(tpf, npc);
                break;
            case WALKING:
                handleWalking(tpf, npc);
                break;
            case JOGGING:
                handleJogging(tpf, npc);
                break;
            case LOOKING_AROUND:
                handleLookingAround(tpf, npc);
                break;
            case FLEEING:
                handleFleeing(tpf, npc);
                break;
            case TALKING:
                handleTalking(tpf, npc);
                break;
            case RUNNING:
                handleRunning(tpf, npc);
                break;
            default:
                break;
        }
    }

    private void handleIdle(float tpf, NPC npc) {
        npc.idleTimer -= tpf;
        if (npc.idleTimer <= 0) {
            float chance = random.nextFloat();
            if (chance < 0.20f) {
                // Look around action
                npc.currentState = NPCState.LOOKING_AROUND;
                npc.idleTimer = 3f + random.nextFloat() * 4f;
                playAnim(npc, "Idle");
                npc.characterControl.setWalkDirection(Vector3f.ZERO);
                return;
            } else if (chance < 0.35f) {
                // Jogging action
                Vector3f dest = pickSmartDestination(npc.getPosition());
                npc.setDestination(dest);
                npc.currentState = NPCState.JOGGING;
                playAnim(npc, "Run");
                npc.walkTimer = 10f + random.nextFloat() * 15f;
                return;
            }

            Vector3f dest = pickSmartDestination(npc.getPosition());
            npc.setDestination(dest);
            npc.currentState = NPCState.WALKING;
            playAnim(npc, "Walk");
            npc.walkTimer = 15f + random.nextFloat() * 25f;
        }
        npc.characterControl.setWalkDirection(Vector3f.ZERO);
    }

    private void handleWalking(float tpf, NPC npc) {
        npc.walkTimer -= tpf;
        Vector3f pos = npc.getPosition();
        Vector3f dest = npc.currentDestination;
        float dx = dest.x - pos.x;
        float dz = dest.z - pos.z;
        float distXZ = (float) Math.sqrt(dx * dx + dz * dz);

        if (distXZ < 3.5f || npc.walkTimer <= 0) {
            float nextStateRoll = random.nextFloat();
            if (nextStateRoll < 0.30f) {
                npc.currentState = NPCState.IDLE;
                npc.idleTimer = 2f + random.nextFloat() * 4f;
                playAnim(npc, "Idle");
                npc.characterControl.setWalkDirection(Vector3f.ZERO);
                return;
            } else if (nextStateRoll < 0.45f) {
                npc.currentState = NPCState.LOOKING_AROUND;
                npc.idleTimer = 4f + random.nextFloat() * 4f;
                playAnim(npc, "Idle");
                npc.characterControl.setWalkDirection(Vector3f.ZERO);
                return;
            }

            Vector3f newDest = pickSmartDestination(pos);
            npc.setDestination(newDest);
            npc.walkTimer = 15f + random.nextFloat() * 25f;
            playAnim(npc, "Walk");
            dx = newDest.x - pos.x;
            dz = newDest.z - pos.z;
            distXZ = (float) Math.sqrt(dx * dx + dz * dz);
        }

        if (distXZ > 0.1f) {
            float invDist = 1f / distXZ;
            Vector3f dir = new Vector3f(dx * invDist, 0, dz * invDist);
            npc.characterControl.setViewDirection(dir);
            // Scale walk speed to match NPC physical scale
            float scaledSpeed = npc.walkSpeed * com.javacity.core.GameConfig.NPC_SCALE * 0.35f;
            npc.characterControl.setWalkDirection(dir.mult(scaledSpeed));
        } else {
            npc.characterControl.setWalkDirection(Vector3f.ZERO);
        }
    }

    private void handleJogging(float tpf, NPC npc) {
        npc.walkTimer -= tpf;
        Vector3f pos = npc.getPosition();
        float dx = npc.currentDestination.x - pos.x;
        float dz = npc.currentDestination.z - pos.z;
        float distXZ = (float) Math.sqrt(dx * dx + dz * dz);

        if (distXZ < 3.5f || npc.walkTimer <= 0) {
            npc.currentState = NPCState.IDLE;
            npc.idleTimer = 2f + random.nextFloat() * 3f;
            playAnim(npc, "Idle");
            npc.characterControl.setWalkDirection(Vector3f.ZERO);
            return;
        }

        if (distXZ > 0.1f) {
            float invDist = 1f / distXZ;
            Vector3f dir = new Vector3f(dx * invDist, 0, dz * invDist);
            npc.characterControl.setViewDirection(dir);
            float scaledSpeed = npc.walkSpeed * com.javacity.core.GameConfig.NPC_SCALE * 0.50f;
            npc.characterControl.setWalkDirection(dir.mult(scaledSpeed));
        } else {
            npc.characterControl.setWalkDirection(Vector3f.ZERO);
        }
    }

    private void handleLookingAround(float tpf, NPC npc) {
        npc.idleTimer -= tpf;
        npc.characterControl.setWalkDirection(Vector3f.ZERO);

        // Turn body slightly left and right to simulate looking around campus
        Vector3f curView = npc.characterControl.getViewDirection();
        float yaw = FastMath.atan2(curView.x, curView.z);
        yaw += FastMath.sin(npc.idleTimer * 2.0f) * 0.015f;
        Vector3f newView = new Vector3f(FastMath.sin(yaw), 0, FastMath.cos(yaw)).normalizeLocal();
        npc.characterControl.setViewDirection(newView);

        if (npc.idleTimer <= 0) {
            Vector3f newDest = pickSmartDestination(npc.getPosition());
            npc.setDestination(newDest);
            npc.currentState = NPCState.WALKING;
            playAnim(npc, "Walk");
            npc.walkTimer = 15f + random.nextFloat() * 20f;
        }
    }

    private void handleTalking(float tpf, NPC npc) {
        npc.talkTimer -= tpf;
        npc.characterControl.setWalkDirection(Vector3f.ZERO);
        playAnim(npc, "Idle");
        if (npc.talkTimer <= 0) {
            npc.currentState = NPCState.IDLE;
            npc.idleTimer = 1f + random.nextFloat() * 3f;
        }
    }

    private void handleRunning(float tpf, NPC npc) {
        Vector3f pos = npc.getPosition();
        Vector3f dir = pos.subtract(npc.threatPosition);
        dir.y = 0;

        if (dir.length() > 40f || npc.threatPosition.equals(Vector3f.ZERO)) {
            npc.currentState = NPCState.IDLE;
            npc.idleTimer = 2f + random.nextFloat() * 3f;
            playAnim(npc, "Idle");
            return;
        }

        if (dir.length() < 0.01f) {
            dir.set(1, 0, 0);
        }

        dir.normalizeLocal();
        npc.characterControl.setViewDirection(dir);
        // ~8.5 m/s * NPC_SCALE * 0.35 factor
        npc.characterControl.setWalkDirection(dir.mult(8.5f * com.javacity.core.GameConfig.NPC_SCALE * 0.35f));
    }

    private void handleFleeing(float tpf, NPC npc) {
        Vector3f pos = npc.getPosition();
        Vector3f fleeDir = pos.subtract(npc.currentDestination);
        fleeDir.y = 0;

        if (fleeDir.length() > 30f) {
            npc.currentState = NPCState.IDLE;
            npc.idleTimer = 0.5f;
            playAnim(npc, "Idle");
            return;
        }

        if (fleeDir.length() < 0.01f) {
            fleeDir.set(1, 0, 0);
        }

        fleeDir.normalizeLocal();
        npc.characterControl.setViewDirection(fleeDir);
        npc.characterControl.setWalkDirection(fleeDir.mult(10.0f * com.javacity.core.GameConfig.NPC_SCALE * 0.35f));
    }

    public void triggerFlee(NPC npc, Vector3f threatPos) {
        npc.currentState = NPCState.FLEEING;
        npc.setDestination(threatPos);
        playAnim(npc, "Run");
    }

    public void triggerRun(NPC npc, Vector3f threatPos) {
        npc.currentState = NPCState.RUNNING;
        npc.threatPosition.set(threatPos);
        playAnim(npc, "Run");
    }

    private Vector3f pickSmartDestination(Vector3f currentPos) {
        // 50% chance to pick a destination along the main campus walkway (X ~ -226.5)
        if (random.nextFloat() < 0.50f) {
            float targetZ = (random.nextFloat() - 0.5f) * 600f;
            float targetX = -226.5f + (random.nextFloat() - 0.5f) * 6.0f;
            return new Vector3f(targetX, currentPos.y, targetZ);
        } else {
            return MathUtils.randomPointInRadius(currentPos, 45f);
        }
    }

    public void avoidNearbyNPCs(NPC npc, List<NPC> nearbyNPCs) {
        if (npc.currentState != NPCState.WALKING && npc.currentState != NPCState.JOGGING) return;
        Vector3f pos = npc.getPosition();

        for (NPC other : nearbyNPCs) {
            if (other == npc) continue;
            Vector3f otherPos = other.getPosition();
            float dist = pos.distance(otherPos);

            // Interaction Trigger: When 2 walking/idle NPCs meet closely, face each other and talk!
            if (dist < 3.2f && dist > 0.1f) {
                if ((other.currentState == NPCState.WALKING || other.currentState == NPCState.IDLE) &&
                    (npc.currentState == NPCState.WALKING || npc.currentState == NPCState.IDLE)) {
                    if (random.nextFloat() < 0.35f) {
                        Vector3f dirToOther = otherPos.subtract(pos);
                        dirToOther.y = 0;
                        if (dirToOther.length() > 0.01f) dirToOther.normalizeLocal();

                        Vector3f dirToSelf = pos.subtract(otherPos);
                        dirToSelf.y = 0;
                        if (dirToSelf.length() > 0.01f) dirToSelf.normalizeLocal();

                        npc.currentState = NPCState.TALKING;
                        npc.talkTimer = 3f + random.nextFloat() * 4f;
                        npc.characterControl.setViewDirection(dirToOther);
                        npc.characterControl.setWalkDirection(Vector3f.ZERO);

                        other.currentState = NPCState.TALKING;
                        other.talkTimer = 3f + random.nextFloat() * 4f;
                        other.characterControl.setViewDirection(dirToSelf);
                        other.characterControl.setWalkDirection(Vector3f.ZERO);
                        return;
                    }
                }
            }
        }
    }

    private void playAnim(NPC npc, String animName) {
        if (npc.animComposer != null) {
            try {
                if (npc.animComposer.getAction(animName) != null) {
                    npc.animComposer.setCurrentAction(animName);
                } else if (npc.animComposer.getAnimClip(animName) != null) {
                    npc.animComposer.setCurrentAction(animName);
                } else if (npc.animComposer.getAction("Walk") != null) {
                    npc.animComposer.setCurrentAction("Walk");
                }
            } catch (Exception e) {}
        }
    }
}
