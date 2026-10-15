package com.javacity.mission;

import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

interface OnMissionComplete { void onMissionComplete(Mission mission); }
interface OnObjectiveComplete { void onObjectiveComplete(MissionObjective objective); }

public class MissionManager {
    public interface Listener { void onMissionStarted(Mission mission); void onObjectiveComplete(MissionObjective objective); void onMissionComplete(Mission mission); void onMissionFailed(Mission mission); void onMissionAbandoned(Mission mission); }
    private final List<Mission> missions = new ArrayList<>();
    private Mission activeMission;
    private OnMissionComplete missionCompleteListener; private OnObjectiveComplete objectiveCompleteListener; private Listener listener;
    // Missions are currently hosted by the portal game. Keep this manager as a
    // compatibility shell so old save files and the treasure hunt remain safe.
    public MissionManager() { }
    public void setListeners(OnMissionComplete mcl, OnObjectiveComplete ocl) { missionCompleteListener=mcl; objectiveCompleteListener=ocl; }
    public void setListener(Listener l) { listener=l; }
    private void createGetawayMission() { List<MissionObjective> o=new ArrayList<>(); o.add(new MissionObjective("Talk to the contact",MissionObjective.ObjectiveType.TALK_TO_NPC,new Vector3f(20,1.5f,-300))); o.add(new MissionObjective("Get to the vehicle",MissionObjective.ObjectiveType.REACH_LOCATION,new Vector3f(60,1.5f,-380))); o.add(new MissionObjective("Enter the vehicle",MissionObjective.ObjectiveType.ENTER_VEHICLE,null)); o.add(new MissionObjective("Drive to the safehouse",MissionObjective.ObjectiveType.REACH_DESTINATION,new Vector3f(-40,1.5f,-220))); o.add(new MissionObjective("Lose the cops",MissionObjective.ObjectiveType.ESCAPE_POLICE,null)); Mission m=new Mission("GETAWAY","Meet the contact and make the delivery.",o); m.state=MissionState.AVAILABLE; missions.add(m); }
    private void createDataHeistMission() { List<MissionObjective> o=new ArrayList<>(); o.add(new MissionObjective("Locate the data terminal",MissionObjective.ObjectiveType.REACH_LOCATION,new Vector3f(150,1.5f,-200))); o.add(new MissionObjective("Hack the terminal",MissionObjective.ObjectiveType.HACK_TERMINAL,new Vector3f(150,1.5f,-200))); o.add(new MissionObjective("Reach the getaway vehicle",MissionObjective.ObjectiveType.REACH_LOCATION,new Vector3f(200,1.5f,-320))); o.add(new MissionObjective("Escape to the safehouse",MissionObjective.ObjectiveType.REACH_DESTINATION,new Vector3f(-50,1.5f,-520))); Mission m=new Mission("DATA HEIST","Infiltrate, steal the data, and escape.",o); m.state=MissionState.AVAILABLE; missions.add(m); }
    private void createCampusMissions() {
        addMission("CAMPUS DELIVERY","Deliver an engineering package across campus.",250,new MissionObjective("Reach the engineering office",MissionObjective.ObjectiveType.REACH_LOCATION,new Vector3f(80,1.5f,120)),new MissionObjective("Collect the package",MissionObjective.ObjectiveType.INTERACT_OBJECT,new Vector3f(80,1.5f,120),"package"),new MissionObjective("Deliver the package to the lab",MissionObjective.ObjectiveType.REACH_DESTINATION,new Vector3f(-80,1.5f,-80)));
        addMission("MISSING STUDENT","Investigate the last known locations of a missing student.",400,new MissionObjective("Search the library",MissionObjective.ObjectiveType.REACH_LOCATION,new Vector3f(-120,1.5f,100)),new MissionObjective("Talk to the witness",MissionObjective.ObjectiveType.TALK_TO_NPC,new Vector3f(-120,1.5f,100),"witness"),new MissionObjective("Return to the main walkway",MissionObjective.ObjectiveType.REACH_DESTINATION,new Vector3f(-226,1.5f,288)));
        addMission("POWER RESTORATION","Restore three disabled campus power points.",500,new MissionObjective("Restore power point A",MissionObjective.ObjectiveType.INTERACT_OBJECT,new Vector3f(140,1.5f,80),"power_a"),new MissionObjective("Restore power point B",MissionObjective.ObjectiveType.INTERACT_OBJECT,new Vector3f(0,1.5f,-40),"power_b"),new MissionObjective("Restore power point C",MissionObjective.ObjectiveType.INTERACT_OBJECT,new Vector3f(-140,1.5f,-160),"power_c"));
        addMission("EMERGENCY EVACUATION","Reach the affected building and escort everyone to safety.",650,new MissionObjective("Reach the emergency zone",MissionObjective.ObjectiveType.REACH_LOCATION,new Vector3f(180,1.5f,-180)),new MissionObjective("Rescue the students",MissionObjective.ObjectiveType.INTERACT_OBJECT,new Vector3f(180,1.5f,-180),"rescue"),new MissionObjective("Reach the safe zone",MissionObjective.ObjectiveType.REACH_DESTINATION,new Vector3f(-226,1.5f,288)));
        addMission("EVIDENCE RUN","Collect evidence and return it to the mission portal.",350,new MissionObjective("Find the evidence",MissionObjective.ObjectiveType.COLLECT_ITEM,new Vector3f(40,1.5f,-180),"evidence"),new MissionObjective("Return to the mission portal",MissionObjective.ObjectiveType.REACH_DESTINATION,new Vector3f(-226,1.5f,288)));
    }
    private void addMission(String name,String desc,int reward,MissionObjective... objectives){Mission m=new Mission(name,desc,new ArrayList<>(Arrays.asList(objectives))).withReward(reward);m.state=MissionState.AVAILABLE;missions.add(m);}
    public Mission getMissionByName(String name){for(Mission m:missions)if(m.name.equals(name))return m;return null;}
    public List<Mission> getAvailableMissions(){return missions;}
    public void startMission(Mission mission){if(mission==null||activeMission!=null)return;mission.resetForReplay();activeMission=mission;mission.state=MissionState.ACTIVE;if(listener!=null)listener.onMissionStarted(mission);}
    public Mission getActiveMission(){return activeMission;}
    public void restoreMission(String name, int objectiveIndex) {
        Mission mission = getMissionByName(name);
        if (mission == null) return;
        activeMission = mission;
        mission.state = MissionState.ACTIVE;
        mission.currentObjectiveIndex = Math.max(0, Math.min(objectiveIndex, mission.objectives.size() - 1));
    }
    public void notifyEvent(String eventId){if(activeMission==null)return;MissionObjective o=activeMission.getCurrentObjective();if(o!=null&&o.completeFromEvent(eventId))activeMission.advanceObjective();}
    public void abandonMission(){if(activeMission==null)return;Mission old=activeMission;activeMission=null;old.resetForReplay();if(listener!=null)listener.onMissionAbandoned(old);}
    public void update(float tpf,Vector3f playerPos,String playerState,int wantedLevel){if(activeMission==null)return;int old=activeMission.currentObjectiveIndex;activeMission.update(tpf,playerPos,playerState,wantedLevel);if(activeMission.state==MissionState.COMPLETE){if(missionCompleteListener!=null)missionCompleteListener.onMissionComplete(activeMission);if(listener!=null)listener.onMissionComplete(activeMission);activeMission=null;}else if(activeMission.state==MissionState.FAILED){if(listener!=null)listener.onMissionFailed(activeMission);activeMission.resetForReplay();activeMission=null;}else if(activeMission.currentObjectiveIndex>old){MissionObjective o=activeMission.objectives.get(old);if(objectiveCompleteListener!=null)objectiveCompleteListener.onObjectiveComplete(o);if(listener!=null)listener.onObjectiveComplete(o);}}
}
