package com.javacity.portal;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import java.util.ArrayList;
import java.util.List;

/** A separate lightweight game mode hosted inside the same application. */
public class PortalGame {
    private final Node root = new Node("PortalGameArena");
    private final AssetManager assets;
    private final List<Vector3f> goals = new ArrayList<>();
    private int nextGoal;
    private float phase;
    private boolean complete;
    private Vector3f origin;

    public PortalGame(AssetManager assets) { this.assets = assets; }
    public Node getNode() { return root; }
    public boolean isComplete() { return complete; }

    public void start(Vector3f origin) {
        this.origin = origin.clone(); nextGoal = 0; complete = false; phase = 0f;
        root.detachAllChildren(); goals.clear();
        Material floorMat = material("Common/MatDefs/Light/PBRLighting.j3md", new ColorRGBA(0.04f,0.08f,0.13f,1f));
        Geometry floor = new Geometry("PortalArenaFloor", new Box(32f,0.35f,32f)); floor.setMaterial(floorMat); floor.setLocalTranslation(origin.x,origin.y-0.8f,origin.z); root.attachChild(floor);
        Material goalMat = material("Common/MatDefs/Misc/Unshaded.j3md", new ColorRGBA(0.15f,0.85f,1f,1f));
        float[][] offsets={{-20f,0f,-18f},{18f,0f,-2f},{0f,0f,20f}};
        for(int i=0;i<offsets.length;i++){Vector3f p=new Vector3f(origin.x+offsets[i][0],origin.y+0.5f,origin.z+offsets[i][2]);goals.add(p);Geometry g=new Geometry("PortalGoal_"+i,new Cylinder(16,16,1.2f,3f));g.setMaterial(goalMat);g.setLocalTranslation(p);root.attachChild(g);}
        Material markerMat=material("Common/MatDefs/Misc/Unshaded.j3md",new ColorRGBA(0.95f,0.65f,0.12f,1f));
        Geometry beacon=new Geometry("PortalArenaBeacon",new Cylinder(12,12,0.25f,8f));beacon.setMaterial(markerMat);beacon.setLocalTranslation(origin.x,origin.y+4f,origin.z);root.attachChild(beacon);
    }
    private Material material(String definition, ColorRGBA color){Material m=new Material(assets,definition);m.setColor(definition.contains("PBR")?"BaseColor":"Color",color);return m;}
    public void update(float tpf, Vector3f playerPosition){if(complete)return;phase+=tpf;for(int i=0;i<root.getChildren().size();i++){if(root.getChild(i).getName().startsWith("PortalGoal_"))root.getChild(i).setLocalScale(1f+FastMath.sin(phase*3f+i)*0.08f);}if(nextGoal<goals.size()&&playerPosition.distanceSquared(goals.get(nextGoal))<16f){nextGoal++;if(nextGoal>=goals.size())complete=true;}}
    public String getStatus(){return complete?"CHALLENGE COMPLETE - PRESS [E] TO RETURN":"PORTAL CHALLENGE  |  REACH BEACON "+(nextGoal+1)+"/"+goals.size()+"  |  ESC TO RETURN";}
    public void stop(){root.detachAllChildren();goals.clear();}
}
