package com.javacity.archer;

import com.capdevon.input.GInputAppState;
import com.capdevon.physx.Physics;
import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.objects.PhysicsBody;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.ui.Picture;
import java.util.ArrayList;
import java.util.List;
import mygame.AnimDefs;
import mygame.GameApplication;
import mygame.audio.SoundManager;
import mygame.player.PlayerManager;
import mygame.states.CubeAppState;
import mygame.states.MonsterAppState;
import mygame.states.SceneAppState;

public class ArcherRealmManager extends BaseAppState implements ActionListener {

    private final Runnable onReturnToCampus;
    private GameApplication app;
    private BulletAppState archerPhysics;
    private GInputAppState gInput;
    private SceneAppState sceneAppState;
    private PlayerManager playerManager;
    private CubeAppState cubeAppState;
    private MonsterAppState monsterAppState;

    private Node bannerNode;
    private final List<SceneProcessor> initialProcessors = new ArrayList<>();
    private boolean isExiting = false;

    public ArcherRealmManager(Runnable onReturnToCampus) {
        this.onReturnToCampus = onReturnToCampus;
    }

    @Override
    protected void initialize(Application app) {
        this.app = (GameApplication) app;
        isExiting = false;

        // Remember existing viewPort processors to clean up newly added ones on exit
        initialProcessors.clear();
        initialProcessors.addAll(this.app.getViewPort().getProcessors());

        // Reset camera orientation and roll to ensure it is completely level
        Camera cam = this.app.getCamera();
        cam.setRotation(new com.jme3.math.Quaternion());
        cam.lookAtDirection(new Vector3f(0, 0, 1), Vector3f.UNIT_Y);

        // 1. Initialize Sound
        try {
            SoundManager.init(this.app.getAssetManager());
        } catch (Exception ex) {
            System.err.println("SoundManager init: " + ex.getMessage());
        }

        // 2. Preload Archer models
        try {
            this.app.getAssetManager().loadModel(AnimDefs.Archer.ASSET_PATH);
            this.app.getAssetManager().loadModel(AnimDefs.Monster.ASSET_PATH);
        } catch (Exception ex) {
            System.err.println("Archer model preloading: " + ex.getMessage());
        }

        // 3. Initialize Archer Bullet Physics
        PhysicsBody.setDeactivationEnabled(false);
        archerPhysics = new BulletAppState();
        getStateManager().attach(archerPhysics);
        PhysicsSpace space = archerPhysics.getPhysicsSpace();
        space.setGravity(Physics.DEFAULT_GRAVITY);
        space.getSolverInfo().setJointErp(1f);

        // 4. Attach Archer GInput
        gInput = new GInputAppState();
        getStateManager().attach(gInput);

        // 5. Attach Archer Gameplay AppStates
        sceneAppState = new SceneAppState();
        getStateManager().attach(sceneAppState);

        playerManager = new PlayerManager();
        getStateManager().attach(playerManager);

        cubeAppState = new CubeAppState();
        getStateManager().attach(cubeAppState);

        monsterAppState = new MonsterAppState();
        getStateManager().attach(monsterAppState);

        // 6. Setup top HUD banner: "[Q] RETURN TO KINGSTON CAMPUS"
        setupHUD();

        // 7. Register Q and ESC key mapping to exit back to Kingston Campus
        InputManager im = this.app.getInputManager();
        im.addMapping("ARCHER_RETURN_Q", new KeyTrigger(KeyInput.KEY_Q));
        im.addMapping("ARCHER_RETURN_ESC", new KeyTrigger(KeyInput.KEY_ESCAPE));
        im.addListener(this, "ARCHER_RETURN_Q", "ARCHER_RETURN_ESC");
    }

    private void setupHUD() {
        bannerNode = new Node("ArcherReturnBanner");
        int screenWidth = app.getContext().getSettings().getWidth();
        int screenHeight = app.getContext().getSettings().getHeight();

        BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        // Top banner background strip
        Picture bannerBg = new Picture("BannerBg");
        com.jme3.material.Material mat = new com.jme3.material.Material(app.getAssetManager(), "Common/MatDefs/Gui/Gui.j3md");
        mat.setColor("Color", new ColorRGBA(0.01f, 0.04f, 0.10f, 0.88f));
        bannerBg.setMaterial(mat);
        float bWidth = 630f;
        float bHeight = 38f;
        float bX = (screenWidth - bWidth) / 2f;
        float bY = screenHeight - bHeight - 12f;
        bannerBg.setWidth(bWidth);
        bannerBg.setHeight(bHeight);
        bannerBg.setLocalTranslation(bX, bY, 0);
        bannerNode.attachChild(bannerBg);

        BitmapText bannerText = new BitmapText(font);
        bannerText.setText("ARCHER REALM  |  PRESS [Q] OR [ESC] TO RETURN TO KINGSTON CAMPUS");
        bannerText.setSize(font.getCharSet().getRenderedSize() * 0.95f);
        bannerText.setColor(new ColorRGBA(0.2f, 0.95f, 1.0f, 1.0f));
        bannerText.setLocalTranslation(bX + 18f, bY + 26f, 1);
        bannerNode.attachChild(bannerText);

        app.getGuiNode().attachChild(bannerNode);
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (("ARCHER_RETURN_Q".equals(name) || "ARCHER_RETURN_ESC".equals(name)) && isPressed && !isExiting) {
            isExiting = true;
            exitToCampus();
        }
    }

    public void exitToCampus() {
        cleanupArcher();
        if (onReturnToCampus != null) {
            onReturnToCampus.run();
        }
    }

    private void cleanupArcher() {
        InputManager im = app.getInputManager();
        try {
            im.removeListener(this);
            im.deleteMapping("ARCHER_RETURN_Q");
            im.deleteMapping("ARCHER_RETURN_ESC");
        } catch (Exception ignored) {}

        if (bannerNode != null) {
            bannerNode.removeFromParent();
        }

        AppStateManager sm = getStateManager();
        if (monsterAppState != null && sm.hasState(monsterAppState)) sm.detach(monsterAppState);
        if (cubeAppState != null && sm.hasState(cubeAppState)) sm.detach(cubeAppState);
        if (playerManager != null && sm.hasState(playerManager)) sm.detach(playerManager);
        if (sceneAppState != null && sm.hasState(sceneAppState)) sm.detach(sceneAppState);
        if (gInput != null && sm.hasState(gInput)) sm.detach(gInput);
        if (archerPhysics != null && sm.hasState(archerPhysics)) sm.detach(archerPhysics);

        // Remove all remaining objects from rootNode that Archer attached
        Node rootNode = app.getRootNode();
        rootNode.detachAllChildren();
        rootNode.getLocalLightList().clear();

        // Clear newly added viewPort processors
        ViewPort vp = app.getViewPort();
        List<SceneProcessor> currentProcessors = new ArrayList<>(vp.getProcessors());
        for (SceneProcessor proc : currentProcessors) {
            if (!initialProcessors.contains(proc)) {
                vp.removeProcessor(proc);
            }
        }

        // Clean any leftover GUI from Archer (crosshairs, etc.)
        Node guiNode = app.getGuiNode();
        for (int i = guiNode.getChildren().size() - 1; i >= 0; i--) {
            Spatial s = guiNode.getChild(i);
            String sName = s.getName();
            if (sName == null || (!sName.equals("CampusGuiNode") && !sName.equals("HUD") && !sName.equals("MiniMap") && !sName.equals("MissionPortalUI") && !sName.equals("PortalConfirmationUI"))) {
                s.removeFromParent();
            }
        }
    }

    @Override
    protected void cleanup(Application app) {
        cleanupArcher();
    }

    @Override protected void onEnable() {}
    @Override protected void onDisable() {}
}
