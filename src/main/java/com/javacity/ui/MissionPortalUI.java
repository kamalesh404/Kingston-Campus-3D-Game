package com.javacity.ui;

import com.javacity.mission.Mission;
import com.javacity.mission.MissionManager;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.jme3.ui.Picture;
import java.util.List;

public class MissionPortalUI {
    private final Node guiNode, panel=new Node("MissionPortalUI"); private final List<Mission> missions; private final BitmapText details; private final Runnable start,close; private int selected; private boolean visible;
    public MissionPortalUI(Node guiNode,AssetManager assets,BitmapFont font,int width,int height,MissionManager manager,Runnable start,Runnable close){
        this.guiNode=guiNode;this.missions=manager.getAvailableMissions();this.start=start;this.close=close;
        Picture bg=new Picture("MissionPortalBackground");Material mat=new Material(assets,"Common/MatDefs/Gui/Gui.j3md");mat.setColor("Color",new ColorRGBA(0.01f,0.04f,0.10f,0.94f));bg.setMaterial(mat);bg.setWidth(width*.72f);bg.setHeight(height*.76f);bg.setLocalTranslation(width*.14f,height*.12f,0);panel.attachChild(bg);
        BitmapText title=text(font,"MISSION PORTAL",font.getCharSet().getRenderedSize()*2f,ColorRGBA.Cyan);title.setLocalTranslation(width*.20f,height*.82f,1);panel.attachChild(title);
        details=text(font,"",font.getCharSet().getRenderedSize()*1.05f,ColorRGBA.White);details.setLocalTranslation(width*.20f,height*.73f,1);panel.attachChild(details);
    }
    private BitmapText text(BitmapFont f,String s,float size,ColorRGBA c){BitmapText t=new BitmapText(f);t.setText(s);t.setSize(size);t.setColor(c);return t;}
    public void show(){visible=true;selected=0;refresh();if(panel.getParent()==null)guiNode.attachChild(panel);} public void hide(){visible=false;panel.removeFromParent();} public boolean isVisible(){return visible;}
    private void refresh(){StringBuilder s=new StringBuilder("[1-7] SELECT    [ENTER] START    [ESC] CLOSE\n\n");for(int i=0;i<missions.size();i++){Mission m=missions.get(i);s.append(i==selected?"> ":"  ").append(i+1).append(". ").append(m.name).append(" (Rs").append(m.reward).append(")\n");}if(!missions.isEmpty()){Mission m=missions.get(selected);s.append("\n").append(m.description).append("\nObjectives:\n");for(int i=0;i<m.objectives.size();i++)s.append(i+1).append(". ").append(m.objectives.get(i).description).append("\n");}details.setText(s.toString());}
    public void handleNumber(int number){if(visible&&number>=1&&number<=missions.size()){selected=number-1;refresh();}} public void startSelected(){if(visible&&!missions.isEmpty()){hide();start.run();}} public void close(){if(visible){hide();close.run();}} public Mission getSelectedMission(){return missions.isEmpty()?null:missions.get(selected);}
}
