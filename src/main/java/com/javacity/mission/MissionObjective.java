package com.javacity.mission;

import com.jme3.math.Vector3f;

public class MissionObjective {
    public enum ObjectiveType { REACH_LOCATION, ENTER_VEHICLE, TALK_TO_NPC, ESCAPE_POLICE, REACH_DESTINATION, HACK_TERMINAL, INTERACT_OBJECT, COLLECT_ITEM }
    public String description;
    public ObjectiveType type;
    public Vector3f targetLocation;
    public float completionRadius = 5f;
    public boolean isComplete = false;
    public String targetId = "";
    public boolean requiresEvent = false;

    public MissionObjective(String description, ObjectiveType type, Vector3f targetLocation) {
        this.description = description; this.type = type; this.targetLocation = targetLocation;
    }
    public MissionObjective(String description, ObjectiveType type, Vector3f targetLocation, String targetId) {
        this(description, type, targetLocation); this.targetId = targetId == null ? "" : targetId; requiresEvent = true;
    }
    public boolean checkCompletion(Vector3f playerPos, String playerState, int wantedLevel) {
        if (isComplete) return true;
        switch (type) {
            case REACH_LOCATION: case REACH_DESTINATION: case TALK_TO_NPC: case INTERACT_OBJECT: case COLLECT_ITEM:
                return targetLocation != null && playerPos != null && playerPos.distance(targetLocation) <= completionRadius && !requiresEvent;
            case ENTER_VEHICLE: return "IN_VEHICLE".equals(playerState);
            case HACK_TERMINAL: return targetLocation != null && playerPos != null && playerPos.distance(targetLocation) <= completionRadius && !requiresEvent;
            case ESCAPE_POLICE: return wantedLevel == 0;
            default: return false;
        }
    }
    public boolean completeFromEvent(String eventId) {
        if (isComplete || !requiresEvent) return false;
        if (targetId.isEmpty() || targetId.equals(eventId)) { isComplete = true; return true; }
        return false;
    }
}
