package com.javacity.mission;

import com.jme3.math.Vector3f;
import java.util.List;

public class Mission {
    public String name, description;
    public List<MissionObjective> objectives;
    public int currentObjectiveIndex = 0;
    public MissionState state = MissionState.NOT_STARTED;
    public int reward;
    public float elapsedTime, timeLimit;
    public Mission(String name, String description, List<MissionObjective> objectives) { this.name=name; this.description=description; this.objectives=objectives; }
    public Mission withReward(int reward) { this.reward=reward; return this; }
    public MissionObjective getCurrentObjective() { return currentObjectiveIndex >= 0 && currentObjectiveIndex < objectives.size() ? objectives.get(currentObjectiveIndex) : null; }
    public boolean advanceObjective() { MissionObjective current=getCurrentObjective(); if(current!=null) current.isComplete=true; currentObjectiveIndex++; if(currentObjectiveIndex>=objectives.size()){state=MissionState.COMPLETE;return true;} return false; }
    public void resetForReplay() { currentObjectiveIndex=0; elapsedTime=0f; state=MissionState.AVAILABLE; for(MissionObjective o:objectives)o.isComplete=false; }
    public void update(float tpf, Vector3f playerPos, String playerState, int wantedLevel) {
        if(state!=MissionState.ACTIVE && !state.name().startsWith("OBJECTIVE_")) return;
        elapsedTime += tpf;
        if(timeLimit > 0f && elapsedTime >= timeLimit) { state=MissionState.FAILED; return; }
        MissionObjective obj=getCurrentObjective(); if(obj!=null && obj.checkCompletion(playerPos,playerState,wantedLevel)) advanceObjective();
    }
}
