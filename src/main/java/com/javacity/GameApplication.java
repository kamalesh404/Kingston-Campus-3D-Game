package com.javacity;

import com.javacity.audio.AudioManager;
import com.javacity.camera.ThirdPersonCamera;
import com.javacity.core.GameConfig;
import com.javacity.core.GameConstants;
import com.javacity.core.GameStateManager;
import com.javacity.interaction.InteractionSystem;
import com.javacity.interaction.MissionPortal;
import com.javacity.mission.MissionManager;
import com.javacity.mission.TreasureGate;
import com.javacity.mission.TreasureHuntManager;
import com.javacity.npc.NPCSpawner;
import com.javacity.player.Player;
import com.javacity.player.PlayerState;
import com.javacity.police.PoliceManager;
import com.javacity.police.WantedSystem;
import com.javacity.save.SaveData;
import com.javacity.save.SaveManager;
import com.javacity.simulation.SustainabilityMetrics;
import com.javacity.ui.HUD;
import com.javacity.ui.PauseMenu;
import com.javacity.ui.MissionPortalUI;
import com.javacity.utilities.DebugUtils;
import com.javacity.vehicles.Vehicle;
import com.javacity.vehicles.VehicleController;
import com.javacity.vehicles.VehicleSpawner;
import com.javacity.world.DayNightSystem;
import com.javacity.world.WorldManager;
import java.util.ArrayList;
import java.util.List;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.system.AppSettings;
import com.jme3.scene.Node;

public class GameApplication extends mygame.GameApplication implements ActionListener, PauseMenu.PauseMenuCallback {

    private BulletAppState bulletAppState;
    private GameStateManager gameStateManager;
    private boolean debugEnabled = false;

    private WorldManager worldManager;
    private Player player;
    private ThirdPersonCamera thirdPersonCamera;
    private VehicleSpawner vehicleSpawner;
    private NPCSpawner npcSpawner;
    private PoliceManager policeManager;
    private MissionManager missionManager;
    private WantedSystem wantedSystem;
    private HUD hud;
    private com.javacity.ui.MiniMap miniMap;
    private com.javacity.mission.StreetRaceManager raceManager;
    private AudioManager audioManager;
    private DayNightSystem dayNightSystem;
    private SustainabilityMetrics sustainabilityMetrics;
    private SaveManager saveManager;
    private InteractionSystem interactionSystem;
    private DebugUtils debugUtils;
    private PauseMenu pauseMenu;
    private MissionPortal missionPortal;
    private MissionPortalUI missionPortalUI;
    private final List<com.javacity.interaction.MissionTerminal> missionEventTerminals = new ArrayList<>();
    private final List<String> completedMissionIds = new ArrayList<>();
    private final Node campusContainer = new Node("CampusContainer");
    private final Node campusGuiNode = new Node("CampusGuiNode");
    private com.javacity.ui.PortalConfirmationUI portalConfirmationUI;
    private com.javacity.archer.ArcherRealmManager archerRealmManager;
    private boolean inArcherRealm = false;

    private VehicleController vehicleController;
    private com.javacity.world.TrafficAI trafficAI;
    private DirectionalLight sun;
    private AmbientLight ambient;
    private DirectionalLight rim;
    private com.jme3.light.LightProbe lightProbe;

    // Mission 1: Campus Treasure Hunt (starts only at the walkway gate)
    private TreasureHuntManager treasureHunt;
    private TreasureGate treasureGate;
    private com.javacity.interaction.MissionTerminal huntTerminal;
    private boolean huntRewarded = false;
    private float huntCompleteBannerTimer = 0f;
    private float uiUpdateTimer = 0f;

    private boolean[] inputState = new boolean[6]; // forward, left, backward, right, sprint, jump
    private Vehicle currentVehicle;

    public static void main(String[] args) {
        GameApplication app = new GameApplication();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("KINGSTON ENGINEERING COLLEGE - VELLORE KATPADI (3D OPEN WORLD)");
        settings.setResolution(GameConfig.DISPLAY_WIDTH, GameConfig.DISPLAY_HEIGHT);
        settings.setFullscreen(GameConfig.DISPLAY_FULLSCREEN);
        settings.setFullscreenMode(AppSettings.FULLSCREEN_MODE_BORDERLESS_WINDOW);
        settings.setResizable(false);
        settings.setCenterWindow(true);
        settings.setVSync(GameConfig.DISPLAY_VSYNC);
        settings.setFrameRate(GameConfig.DISPLAY_FPS);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        setDisplayFps(false);
        setDisplayStatView(false);

        // Initialize physics
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        bulletAppState.setDebugEnabled(debugEnabled);
        // Lock physics to 60Hz fixed timestep. 4 substeps gives enough resolution for
        // character capsules to properly land on geometry without tunneling.
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.SEQUENTIAL);
        bulletAppState.getPhysicsSpace().setGravity(new Vector3f(0, GameConfig.PHYSICS_GRAVITY, 0));
        bulletAppState.getPhysicsSpace().setAccuracy(1f / 60f);
        bulletAppState.getPhysicsSpace().setMaxSubSteps(4);

        gameStateManager = new GameStateManager();
        gameStateManager.setState(GameStateManager.GameState.PLAYING);

        setupLighting();

        flyCam.setEnabled(false);
        inputManager.setCursorVisible(false);
        cam.setFrustumPerspective(45f, (float) cam.getWidth() / (float) cam.getHeight(), 0.1f, 4000f);

        rootNode.attachChild(campusContainer);
        guiNode.attachChild(campusGuiNode);

        worldManager = new WorldManager(assetManager, bulletAppState.getPhysicsSpace());
        worldManager.initialize(sun, ambient, viewPort);
        campusContainer.attachChild(worldManager.getRootNode());

        player = new Player(assetManager, bulletAppState.getPhysicsSpace());
        player.setPosition(worldManager.getSpawnManager().getPlayerSpawnPoint());
        campusContainer.attachChild(player.getNode());

        thirdPersonCamera = new ThirdPersonCamera(cam, worldManager.getRootNode(), inputManager);

        vehicleSpawner = new VehicleSpawner(assetManager, campusContainer, bulletAppState, worldManager.getCityLoader());
        for (int i = 0; i < 10; i++) {
            vehicleSpawner.addSpawnPoint(worldManager.getSpawnManager().getRandomVehicleSpawnPoint());
        }
        vehicleSpawner.spawnInitialVehicles();

        npcSpawner = new NPCSpawner(assetManager, bulletAppState.getPhysicsSpace(), campusContainer, worldManager.getSpawnManager());
        npcSpawner.spawnInitialNPCs();

        wantedSystem = new WantedSystem();

        policeManager = new PoliceManager(assetManager, bulletAppState.getPhysicsSpace(), campusContainer, worldManager.getSpawnManager());
        policeManager.setVehicleSpawner(vehicleSpawner);

        missionManager = new MissionManager();
        missionManager.setListener(new MissionManager.Listener() {
            public void onMissionStarted(com.javacity.mission.Mission mission) { resetMissionEventTerminals(); }
            public void onObjectiveComplete(com.javacity.mission.MissionObjective objective) { }
            public void onMissionComplete(com.javacity.mission.Mission mission) {
                if (!completedMissionIds.contains(mission.name)) completedMissionIds.add(mission.name);
                player.getStats().addMoney(mission.reward);
            }
            public void onMissionFailed(com.javacity.mission.Mission mission) { }
            public void onMissionAbandoned(com.javacity.mission.Mission mission) { }
        });

        interactionSystem = new InteractionSystem();

        float portalY = worldManager.getCityLoader().getGroundHeight(-210f, 300f);
        missionPortal = new MissionPortal(assetManager, new Vector3f(-210f, portalY, 300f), this::openPortalConfirmation);
        interactionSystem.register(missionPortal);
        campusContainer.attachChild(missionPortal.getNode());

        registerMissionEventTerminal("package", new Vector3f(80f, worldManager.getCityLoader().getGroundHeight(80f,120f)+1.5f,120f), "PRESS [E] COLLECT PACKAGE");
        registerMissionEventTerminal("witness", new Vector3f(-120f, worldManager.getCityLoader().getGroundHeight(-120f,100f)+1.5f,100f), "PRESS [E] TALK TO WITNESS");
        registerMissionEventTerminal("power_a", new Vector3f(140f, worldManager.getCityLoader().getGroundHeight(140f,80f)+1.5f,80f), "PRESS [E] RESTORE POWER A");
        registerMissionEventTerminal("power_b", new Vector3f(0f, worldManager.getCityLoader().getGroundHeight(0f,-40f)+1.5f,-40f), "PRESS [E] RESTORE POWER B");
        registerMissionEventTerminal("power_c", new Vector3f(-140f, worldManager.getCityLoader().getGroundHeight(-140f,-160f)+1.5f,-160f), "PRESS [E] RESTORE POWER C");
        registerMissionEventTerminal("rescue", new Vector3f(180f, worldManager.getCityLoader().getGroundHeight(180f,-180f)+1.5f,-180f), "PRESS [E] RESCUE STUDENTS");
        registerMissionEventTerminal("evidence", new Vector3f(40f, worldManager.getCityLoader().getGroundHeight(40f,-180f)+1.5f,-180f), "PRESS [E] COLLECT EVIDENCE");

        float getTermY = worldManager.getCityLoader().getGroundHeight(20f, 30f) + 1.5f;
        com.javacity.interaction.MissionTerminal getTerminal = new com.javacity.interaction.MissionTerminal(assetManager,
            new Vector3f(20, getTermY, 30), "PRESS [E] Start CAMPUS GETAWAY Mission",
            () -> {
                com.javacity.mission.Mission m = missionManager.getMissionByName("GETAWAY");
                if (m != null && m.state == com.javacity.mission.MissionState.AVAILABLE) {
                    missionManager.startMission(m);
                }
            }
        );
        interactionSystem.register(getTerminal);

        float dataTermY = worldManager.getCityLoader().getGroundHeight(-40f, -50f) + 1.5f;
        com.javacity.interaction.MissionTerminal dataTerminal = new com.javacity.interaction.MissionTerminal(assetManager,
            new Vector3f(-40, dataTermY, -50), "PRESS [E] Start ACADEMIC HEIST Mission",
            () -> {
                com.javacity.mission.Mission m = missionManager.getMissionByName("DATA HEIST");
                if (m != null && m.state == com.javacity.mission.MissionState.AVAILABLE) {
                    missionManager.startMission(m);
                }
            }
        );
        interactionSystem.register(dataTerminal);

        // Treasure Hunt entry on the main walkway, next to spawn
        float gateGy = worldManager.getCityLoader().getGroundHeight(-226.5f, 288f);
        treasureGate = new TreasureGate(assetManager, new Vector3f(-226.5f, gateGy, 288f));

        huntTerminal = new com.javacity.interaction.MissionTerminal(assetManager,
            new Vector3f(-226.5f, gateGy + 1.5f, 288f), "PRESS [E] Start TREASURE HUNT (6 gold bars)",
            () -> {
                if (treasureHunt != null && !treasureHunt.isActive()) {
                    huntRewarded = false;
                    treasureHunt.start(player.getPosition());
                }
            }
        );
        interactionSystem.register(huntTerminal);

        hud = new HUD(campusGuiNode, assetManager, guiFont, settings.getWidth(), settings.getHeight());

        audioManager = new AudioManager(assetManager, campusContainer);
        audioManager.init();
        audioManager.playAmbient();

        treasureHunt = new TreasureHuntManager(assetManager, campusContainer, worldManager.getCityLoader(), audioManager);

        miniMap = new com.javacity.ui.MiniMap(campusGuiNode, assetManager, settings.getWidth(), settings.getHeight());
        raceManager = new com.javacity.mission.StreetRaceManager(campusContainer, assetManager, audioManager, worldManager.getCityLoader());
        // No auto-start: the Treasure Hunt gate is the only mission entry.

        dayNightSystem = worldManager.getDayNightSystem();

        sustainabilityMetrics = new SustainabilityMetrics();

        saveManager = new SaveManager();

        debugUtils = new DebugUtils(campusGuiNode, guiFont);

        pauseMenu = new PauseMenu(campusGuiNode, assetManager, guiFont, settings.getWidth(), settings.getHeight(), this);
        missionPortalUI = new MissionPortalUI(campusGuiNode, assetManager, guiFont, settings.getWidth(), settings.getHeight(), missionManager,
            () -> missionManager.startMission(missionPortalUI.getSelectedMission()), this::closeMissionPortal);

        portalConfirmationUI = new com.javacity.ui.PortalConfirmationUI(
            campusGuiNode, assetManager, guiFont, settings.getWidth(), settings.getHeight(),
            this::enterArcherRealm, () -> portalConfirmationUI.hide()
        );

        vehicleController = new VehicleController();
        trafficAI = new com.javacity.world.TrafficAI();

        setupInputs();
    }

    private void registerMissionEventTerminal(String id, Vector3f position, String prompt) {
        com.javacity.interaction.MissionTerminal terminal = new com.javacity.interaction.MissionTerminal(assetManager, position, prompt, () -> {
            missionManager.notifyEvent(id);
            resetMissionEventTerminals();
        });
        missionEventTerminals.add(terminal);
        interactionSystem.register(terminal);
    }

    private void resetMissionEventTerminals() { for (com.javacity.interaction.MissionTerminal terminal : missionEventTerminals) terminal.reset(); }

    private void openPortalConfirmation() {
        if (inArcherRealm || (portalConfirmationUI != null && portalConfirmationUI.isVisible())) return;
        portalConfirmationUI.show();
        inputState = new boolean[6];
    }

    public void enterArcherRealm() {
        if (inArcherRealm) return;
        inArcherRealm = true;
        if (portalConfirmationUI != null) portalConfirmationUI.hide();
        if (missionPortalUI != null) missionPortalUI.close();

        // 1. Stop Kingston Campus audio loops
        if (audioManager != null) {
            audioManager.stopEngineLoop();
            audioManager.stopSiren();
            audioManager.stopAmbient();
        }

        // 2. Disable Kingston camera and FULLY DETACH Kingston physics from stateManager.
        // If we only disable it, getState(BulletAppState.class) still returns it first,
        // so archerPhysics would never be found — causing the player to float.
        if (thirdPersonCamera != null) thirdPersonCamera.setEnabled(false);
        if (bulletAppState != null) {
            bulletAppState.setEnabled(false);
            if (stateManager.hasState(bulletAppState)) {
                stateManager.detach(bulletAppState);
            }
        }

        // 3. Detach Kingston 2D GUI and 3D campus world
        campusGuiNode.removeFromParent();
        campusContainer.removeFromParent();
        rootNode.removeLight(sun);
        rootNode.removeLight(ambient);
        if (rim != null) rootNode.removeLight(rim);
        if (lightProbe != null) rootNode.removeLight(lightProbe);

        // 4. Attach and start Game 2: Archer Realm
        archerRealmManager = new com.javacity.archer.ArcherRealmManager(this::returnToKingstonCampus);
        stateManager.attach(archerRealmManager);
    }

    public void returnToKingstonCampus() {
        if (!inArcherRealm) return;

        // 1. Detach ArcherRealmManager (which also runs cleanupArcher inside its cleanup())
        if (archerRealmManager != null && stateManager.hasState(archerRealmManager)) {
            stateManager.detach(archerRealmManager);
            archerRealmManager = null;
        }

        // 2. Re-attach and re-enable Kingston physics BEFORE restoring campus geometry.
        //    It was fully detached when entering Archer Realm so we must re-attach it.
        if (bulletAppState != null) {
            if (!stateManager.hasState(bulletAppState)) {
                stateManager.attach(bulletAppState);
            }
            bulletAppState.setEnabled(true);
        }

        // 3. Restore Kingston Campus visual nodes and lights
        if (campusContainer.getParent() == null) {
            rootNode.attachChild(campusContainer);
        }
        // Guard against double-add (lights may already be present if cleanup left them)
        try { rootNode.removeLight(sun);       } catch (Exception ignored) {}
        try { rootNode.removeLight(ambient);   } catch (Exception ignored) {}
        try { if (rim != null) rootNode.removeLight(rim); } catch (Exception ignored) {}
        try { if (lightProbe != null) rootNode.removeLight(lightProbe); } catch (Exception ignored) {}
        rootNode.addLight(sun);
        rootNode.addLight(ambient);
        if (rim != null) rootNode.addLight(rim);
        if (lightProbe != null) rootNode.addLight(lightProbe);

        // 4. Restore camera frustum
        this.cam.setFrustumPerspective(45f, (float) this.cam.getWidth() / (float) this.cam.getHeight(), 0.1f, 4000f);

        // 5. Warp player safely back to the Kingston campus entrance walkway
        Vector3f returnPos;
        if (missionPortal != null) {
            float gy = worldManager.getCityLoader().getGroundHeight(missionPortal.getPosition().x, missionPortal.getPosition().z + 6.0f);
            if (gy <= 5.0f) gy = 17.7f;
            returnPos = new Vector3f(missionPortal.getPosition().x, gy + 2.5f, missionPortal.getPosition().z + 6.0f);
        } else {
            returnPos = worldManager.getSpawnManager().getPlayerSpawnPoint();
        }
        player.setPosition(returnPos);
        if (player.getCharacterControl() != null) {
            player.getCharacterControl().warp(returnPos);
            player.getCharacterControl().setWalkDirection(Vector3f.ZERO);
        }

        // 6. Re-enable camera and force instant position sync
        if (thirdPersonCamera != null) {
            thirdPersonCamera.setEnabled(true);
            thirdPersonCamera.update(0.016f, player.getPosition(), false);
        }

        // 7. Restore campus GUI
        if (campusGuiNode.getParent() == null) {
            guiNode.attachChild(campusGuiNode);
        }

        // 8. Resume ambient audio and reset all input state
        if (audioManager != null) {
            audioManager.playAmbient();
        }
        inputState = new boolean[6];
        inputManager.setCursorVisible(false);

        // Mark as returned — do this last so simpleUpdate resumes cleanly
        inArcherRealm = false;
    }

    private void openMissionPortal() {
        if (missionPortalUI == null || missionManager.getActiveMission() != null) return;
        missionPortalUI.show();
        gameStateManager.setState(GameStateManager.GameState.PAUSED);
        bulletAppState.setEnabled(false);
        inputManager.setCursorVisible(true);
    }

    private void closeMissionPortal() {
        gameStateManager.setState(GameStateManager.GameState.PLAYING);
        bulletAppState.setEnabled(true);
        inputManager.setCursorVisible(false);
    }


    private void setupLighting() {
        // PBR Environment LightProbe - essential for jME PBR materials so ambient light illuminates all sides
        try {
            com.jme3.scene.Spatial probeModel = assetManager.loadModel("Scenes/defaultProbe.j3o");
            if (probeModel != null && probeModel.getLocalLightList().size() > 0) {
                lightProbe = (com.jme3.light.LightProbe) probeModel.getLocalLightList().get(0);
                lightProbe.getArea().setCenter(Vector3f.ZERO.clone());
                lightProbe.getArea().setRadius(10000f);
                rootNode.addLight(lightProbe);
            }
        } catch (Exception e) {
            System.err.println("Note: defaultProbe.j3o: " + e.getMessage());
        }

        // Natural Direct Sun Light - warm realistic sunlight
        sun = new DirectionalLight();
        sun.setDirection(new Vector3f(0.40f, -0.65f, -0.55f).normalizeLocal());
        sun.setColor(new ColorRGBA(1.0f, 0.96f, 0.88f, 1.0f).mult(1.15f));
        rootNode.addLight(sun);

        // Natural Sky Ambient Fill - balanced with LightProbe IBL
        ambient = new AmbientLight();
        ambient.setColor(new ColorRGBA(0.60f, 0.72f, 0.90f, 1.0f).mult(0.35f));
        rootNode.addLight(ambient);

        // Soft Secondary Fill Light - subtle backfill
        rim = new DirectionalLight();
        rim.setDirection(new Vector3f(-0.40f, -0.50f, 0.55f).normalizeLocal());
        rim.setColor(new ColorRGBA(0.70f, 0.75f, 0.85f, 1.0f).mult(0.20f));
        rootNode.addLight(rim);

        // Realistic Soft Directional Shadows - grounded & natural without pitch-black darkening
        DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, GameConfig.SHADOW_MAP_SIZE, 2);
        dlsr.setLight(sun);
        dlsr.setShadowIntensity(0.30f);
        dlsr.setEdgeFilteringMode(com.jme3.shadow.EdgeFilteringMode.Bilinear);
        viewPort.addProcessor(dlsr);

        // Balanced Post-Processing: Filmic Tone Mapping, Anti-Aliasing, and subtle Bloom
        if (GameConfig.POST_PROCESSING) try {
            com.jme3.post.FilterPostProcessor fpp = new com.jme3.post.FilterPostProcessor(assetManager);
            
            com.jme3.post.filters.BloomFilter bloom = new com.jme3.post.filters.BloomFilter(com.jme3.post.filters.BloomFilter.GlowMode.SceneAndObjects);
            bloom.setExposureCutOff(0.75f);
            bloom.setBloomIntensity(0.25f);
            bloom.setBlurScale(0.8f);
            fpp.addFilter(bloom);

            com.jme3.post.filters.ToneMapFilter toneMap = new com.jme3.post.filters.ToneMapFilter();
            toneMap.setWhitePoint(new Vector3f(1.6f, 1.6f, 1.6f));
            fpp.addFilter(toneMap);

            com.jme3.post.filters.FXAAFilter fxaa = new com.jme3.post.filters.FXAAFilter();
            fpp.addFilter(fxaa);

            com.jme3.post.filters.FogFilter fog = new com.jme3.post.filters.FogFilter();
            fog.setFogColor(new ColorRGBA(0.55f, 0.70f, 0.90f, 1.0f));
            fog.setFogDistance(5000f);
            fog.setFogDensity(0.000012f);
            fpp.addFilter(fog);

            viewPort.addProcessor(fpp);
        } catch (Exception ex) {
            System.err.println("PostProcessing notification: " + ex.getMessage());
        }

        // Natural Horizon Sky Color
        viewPort.setBackgroundColor(new ColorRGBA(0.48f, 0.68f, 0.90f, 1.0f));
    }

    private void setupInputs() {
        if (inputManager.hasMapping(SimpleApplication.INPUT_MAPPING_EXIT)) {
            inputManager.deleteMapping(SimpleApplication.INPUT_MAPPING_EXIT);
        }

        inputManager.addMapping(GameConstants.INPUT_FORWARD, new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping(GameConstants.INPUT_BACKWARD, new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping(GameConstants.INPUT_LEFT, new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping(GameConstants.INPUT_RIGHT, new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping(GameConstants.INPUT_SPRINT, new KeyTrigger(KeyInput.KEY_LSHIFT));
        inputManager.addMapping(GameConstants.INPUT_JUMP, new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping(GameConstants.INPUT_INTERACT, new KeyTrigger(KeyInput.KEY_E));
        inputManager.addMapping(GameConstants.INPUT_ENTER_VEHICLE, new KeyTrigger(KeyInput.KEY_F), new KeyTrigger(KeyInput.KEY_E));
        inputManager.addMapping(GameConstants.INPUT_PUNCH, new com.jme3.input.controls.MouseButtonTrigger(com.jme3.input.MouseInput.BUTTON_LEFT), new KeyTrigger(KeyInput.KEY_Q));
        inputManager.addMapping(GameConstants.INPUT_TOGGLE_DEBUG, new KeyTrigger(KeyInput.KEY_F3));
        inputManager.addMapping(GameConstants.INPUT_PAUSE, new KeyTrigger(KeyInput.KEY_ESCAPE));
        inputManager.addMapping("MISSION_SELECT_1", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("MISSION_SELECT_2", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("MISSION_SELECT_3", new KeyTrigger(KeyInput.KEY_3));
        inputManager.addMapping("MISSION_SELECT_4", new KeyTrigger(KeyInput.KEY_4));
        inputManager.addMapping("MISSION_SELECT_5", new KeyTrigger(KeyInput.KEY_5));
        inputManager.addMapping("MISSION_SELECT_6", new KeyTrigger(KeyInput.KEY_6));
        inputManager.addMapping("MISSION_SELECT_7", new KeyTrigger(KeyInput.KEY_7));
        inputManager.addMapping("MISSION_CONFIRM", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addMapping("PORTAL_CONFIRM", new KeyTrigger(KeyInput.KEY_RETURN), new KeyTrigger(KeyInput.KEY_F));

        inputManager.addListener(this, GameConstants.INPUT_FORWARD, GameConstants.INPUT_BACKWARD,
                GameConstants.INPUT_LEFT, GameConstants.INPUT_RIGHT, GameConstants.INPUT_SPRINT,
                GameConstants.INPUT_JUMP, GameConstants.INPUT_INTERACT, GameConstants.INPUT_ENTER_VEHICLE,
                GameConstants.INPUT_PUNCH, GameConstants.INPUT_TOGGLE_DEBUG, GameConstants.INPUT_PAUSE,
                "MISSION_SELECT_1", "MISSION_SELECT_2", "MISSION_SELECT_3", "MISSION_SELECT_4",
                "MISSION_SELECT_5", "MISSION_SELECT_6", "MISSION_SELECT_7", "MISSION_CONFIRM", "PORTAL_CONFIRM");
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (inArcherRealm) {
            return;
        }

        if (isPressed && pauseMenu != null && pauseMenu.isVisible()) {
            if (name.equals(GameConstants.INPUT_PAUSE)) {
                onResume();
                return;
            }
            if (name.equals("MISSION_SELECT_1")) { pauseMenu.handleInput(1); return; }
            if (name.equals("MISSION_SELECT_2")) { pauseMenu.handleInput(2); return; }
            if (name.equals("MISSION_SELECT_3")) { pauseMenu.handleInput(3); return; }
            if (name.equals("MISSION_SELECT_4")) { pauseMenu.handleInput(4); return; }
            return;
        }

        if (isPressed && portalConfirmationUI != null && portalConfirmationUI.isVisible()) {
            if (name.equals(GameConstants.INPUT_PAUSE)) {
                portalConfirmationUI.cancel();
                return;
            }
            if (name.equals("PORTAL_CONFIRM") || name.equals(GameConstants.INPUT_ENTER_VEHICLE) || name.equals("MISSION_CONFIRM")) {
                portalConfirmationUI.confirm();
                return;
            }
            return;
        }

        if (isPressed && missionPortalUI != null && missionPortalUI.isVisible()) {
            if (name.equals(GameConstants.INPUT_PAUSE)) { missionPortalUI.close(); return; }
            if (name.equals("MISSION_CONFIRM")) { missionPortalUI.startSelected(); return; }
            if (name.startsWith("MISSION_SELECT_")) {
                missionPortalUI.handleNumber(Integer.parseInt(name.substring(name.length() - 1)));
                return;
            }
            return;
        }
        if (name.equals(GameConstants.INPUT_FORWARD)) inputState[0] = isPressed;
        if (name.equals(GameConstants.INPUT_LEFT)) inputState[1] = isPressed;
        if (name.equals(GameConstants.INPUT_BACKWARD)) inputState[2] = isPressed;
        if (name.equals(GameConstants.INPUT_RIGHT)) inputState[3] = isPressed;
        if (name.equals(GameConstants.INPUT_SPRINT)) inputState[4] = isPressed;
        if (name.equals(GameConstants.INPUT_JUMP)) inputState[5] = isPressed;

        if (isPressed) {
            if (name.equals(GameConstants.INPUT_TOGGLE_DEBUG)) {
                debugUtils.toggle();
                bulletAppState.setDebugEnabled(!bulletAppState.isDebugEnabled());
            } else if (name.equals(GameConstants.INPUT_PAUSE)) {
                if (gameStateManager.getCurrentState() == GameStateManager.GameState.PLAYING) {
                    gameStateManager.setState(GameStateManager.GameState.PAUSED);
                    pauseMenu.show();
                    bulletAppState.setEnabled(false);
                    if (thirdPersonCamera != null) {
                        thirdPersonCamera.setEnabled(false);
                    }
                    inputManager.setCursorVisible(true);
                    java.util.Arrays.fill(inputState, false);
                } else if (gameStateManager.getCurrentState() == GameStateManager.GameState.PAUSED) {
                    onResume();
                }
            } else if (name.equals(GameConstants.INPUT_PUNCH)) {
                if (player.getState() == PlayerState.ON_FOOT) {
                    player.punch();
                }
            } else if (name.equals(GameConstants.INPUT_INTERACT) || name.equals(GameConstants.INPUT_ENTER_VEHICLE)) {
                if (player.getState() == PlayerState.ON_FOOT) {
                    if (missionPortal != null && player.getPosition().distance(missionPortal.getPosition()) <= missionPortal.getInteractionRadius()) {
                        openPortalConfirmation();
                        return;
                    }
                    Vehicle nearest = vehicleSpawner.findNearestVehicle(player.getPosition(), 4.0f);
                    if (nearest != null && !nearest.isOccupied()) {
                        enterVehicle(nearest);
                    } else {
                        interactionSystem.tryInteract(player);
                    }
                } else if (player.getState() == PlayerState.IN_VEHICLE) {
                    exitVehicle();
                }
            }
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (inArcherRealm) {
            return;
        }
        if (gameStateManager.getCurrentState() != GameStateManager.GameState.PLAYING) {
            return;
        }

        if (missionPortal != null) missionPortal.update(tpf);
        if (player.getState() == PlayerState.ON_FOOT) {
            player.update(tpf, cam, inputState);
            thirdPersonCamera.update(tpf, player.getPosition(), false);
            interactionSystem.update(tpf, player.getPosition());
            audioManager.stopEngineLoop();
            
            if (inputState[0] || inputState[1] || inputState[2] || inputState[3]) {
                audioManager.playFootstep();
            }
        } else if (player.getState() == PlayerState.IN_VEHICLE && currentVehicle != null) {
            vehicleController.update(tpf, currentVehicle, inputState, worldManager.getCityLoader());
            
            player.hide();
            
            thirdPersonCamera.update(tpf, currentVehicle.getPosition(), true);
            audioManager.playEngineLoop();
        }

        Vector3f activePos = player.getState() == PlayerState.IN_VEHICLE && currentVehicle != null ? currentVehicle.getPosition() : player.getPosition();
        worldManager.update(tpf, activePos);
        trafficAI.update(tpf, vehicleSpawner.getActiveVehicles(), player.getPosition());
        npcSpawner.update(tpf, player.getPosition());
        sustainabilityMetrics.update(tpf, currentVehicle != null ? Math.abs(currentVehicle.getCurrentSpeed()) : 0f, player.getState() == PlayerState.IN_VEHICLE, wantedSystem.getWantedLevel());
        wantedSystem.update(tpf, true);
        policeManager.update(tpf, wantedSystem.getWantedLevel(), player.getPosition());
        
        if (wantedSystem.getWantedLevel() > 0) {
            audioManager.playSiren();
        } else {
            audioManager.stopSiren();
        }

        raceManager.update(tpf, activePos);

        // Treasure Hunt: gate idle animation + gold bar collection (touch to collect)
        if (treasureGate != null) treasureGate.update(tpf);
        if (treasureHunt != null) treasureHunt.update(tpf, activePos);

        String pState = player.getState() == PlayerState.IN_VEHICLE ? "IN_VEHICLE" : "ON_FOOT";
        missionManager.update(tpf, player.getPosition(), pState, wantedSystem.getWantedLevel());

        // Treasure Hunt reward: once per completion
        if (treasureHunt != null && treasureHunt.isComplete() && !huntRewarded) {
            huntRewarded = true;
            huntCompleteBannerTimer = 6f;
            player.getStats().addMoney(TreasureHuntManager.REWARD_MONEY);
            if (huntTerminal != null) huntTerminal.reset();
            audioManager.playMissionComplete();
        }
        if (huntCompleteBannerTimer > 0f) huntCompleteBannerTimer -= tpf;

        String missionText = "";
        if (treasureHunt != null && treasureHunt.isActive()) {
            missionText = String.format("TREASURE HUNT GOLD: %d/%d | TIME: %s",
                treasureHunt.getCollected(), treasureHunt.getTotal(), treasureHunt.getTimeString());
        } else if (huntCompleteBannerTimer > 0f) {
            missionText = String.format("TREASURE COMPLETE! +Rs%d", TreasureHuntManager.REWARD_MONEY);
        } else if (raceManager.isRaceActive()) {
            int mins = (int)(raceManager.getRaceTimer() / 60f);
            float secs = raceManager.getRaceTimer() % 60f;
            missionText = String.format("STREET RACE CP: %d/%d | TIME: %02d:%04.1f", raceManager.getCurrentCheckpointIndex() + 1, raceManager.getTotalCheckpoints(), mins, secs);
        } else if (raceManager.isRaceCompleted()) {
            missionText = String.format("VICTORY! BEST TIME: %.1fs", raceManager.getBestRaceTime());
        } else if (missionManager.getActiveMission() != null && missionManager.getActiveMission().getCurrentObjective() != null) {
            missionText = missionManager.getActiveMission().getCurrentObjective().description;
        }

        uiUpdateTimer -= tpf;
        if (uiUpdateTimer <= 0f) {
            uiUpdateTimer = GameConfig.UI_UPDATE_INTERVAL;
            List<Vector3f> vehPositions = new ArrayList<>(vehicleSpawner.getActiveVehicles().size());
            for (Vehicle v : vehicleSpawner.getActiveVehicles()) vehPositions.add(v.getPosition());
            List<Vector3f> policePositions = new ArrayList<>(policeManager.getPoliceVehicles().size());
            for (Vehicle pv : policeManager.getPoliceVehicles()) policePositions.add(pv.getPosition());
            java.util.List<Vector3f> treasurePositions = (treasureHunt != null && treasureHunt.isActive()) ? treasureHunt.getGoldPositions() : null;
            miniMap.update(player.getPosition(), vehPositions,
                raceManager.isRaceActive() ? raceManager.getRaceCheckpoints() : null,
                policePositions, treasurePositions);

            float speed = currentVehicle != null && player.getState() == PlayerState.IN_VEHICLE ? currentVehicle.getCurrentSpeed() : -1f;
            Vehicle nearestVehicle = vehicleSpawner.findNearestVehicle(player.getPosition(), 500.0f);
            Vector3f nearestCarPos = nearestVehicle != null ? nearestVehicle.getPosition() : null;
            com.javacity.npc.NPC nearestNpc = npcSpawner.findNearestNPC(player.getPosition(), 500.0f);
            Vector3f nearestNpcPos = nearestNpc != null ? nearestNpc.getPosition() : null;
            String npcName = nearestNpc != null ? nearestNpc.npcName : "Civilian";
            hud.update(tpf, player.getStats().getHealth(), wantedSystem.getWantedLevel(), missionText, speed, dayNightSystem.getTimeString(), sustainabilityMetrics, player.getPosition(), nearestCarPos, "GolfCart", nearestNpcPos, npcName);
        }

        debugUtils.update(tpf, player.getPosition(), wantedSystem.getWantedLevel(), missionManager.getActiveMission() != null ? missionManager.getActiveMission().state.name() : "NONE", npcSpawner.getActiveNPCs().size(), vehicleSpawner.getActiveVehicles().size(), timer.getFrameRate());
    }

    private void enterVehicle(Vehicle v) {
        currentVehicle = v;
        currentVehicle.setOccupied(true);
        player.enterVehicleState();
        audioManager.playEngineLoop();
    }

    private void exitVehicle() {
        if (currentVehicle != null) {
            currentVehicle.setOccupied(false);
            Vector3f carPos = currentVehicle.getPosition();
            com.jme3.math.Quaternion carRot = currentVehicle.getPhysicsControl().getPhysicsRotation();
            Vector3f leftSide = carRot.mult(new Vector3f(-2.2f, 0f, 0f));
            float exitY = (worldManager != null && worldManager.getCityLoader() != null) ? worldManager.getCityLoader().getGroundHeight(carPos.x + leftSide.x, carPos.z + leftSide.z) + 1.5f : carPos.y;
            Vector3f exitPos = new Vector3f(carPos.x + leftSide.x, Math.max(exitY, 17.7f), carPos.z + leftSide.z);
            player.exitVehicleState(exitPos);
            currentVehicle = null;
        }
        audioManager.stopEngineLoop();
    }

    @Override
    public void onSave() {
        saveGame();
    }

    private void saveGame() {
        SaveData data = new SaveData();
        Vector3f pos = player.getPosition();
        data.playerPosition = new float[]{pos.x, pos.y, pos.z};
        data.playerHealth = player.getStats().getHealth();
        data.money = player.getStats().getMoney();
        data.wantedLevel = wantedSystem.getWantedLevel();
        if (missionManager.getActiveMission() != null) {
            data.currentMission = missionManager.getActiveMission().name;
            data.missionState = missionManager.getActiveMission().state.name();
            data.missionObjective = missionManager.getActiveMission().currentObjectiveIndex;
        }
        data.completedMissionIds.addAll(completedMissionIds);
        data.completedMissions = completedMissionIds.size();
        data.timeOfDay = dayNightSystem.getGameTime();
        
        saveManager.save(data, "savegame");
        onResume();
    }

    @Override
    public void onLoad() {
        loadGame();
    }

    private void loadGame() {
        SaveData data = saveManager.load("savegame");
        if (data != null) {
            player.setPosition(new Vector3f(data.playerPosition[0], data.playerPosition[1], data.playerPosition[2]));
            float diff = data.playerHealth - player.getStats().getHealth();
            if (diff > 0) player.getStats().heal(diff);
            else if (diff < 0) player.getStats().damage(-diff);
            
            wantedSystem.setWantedLevel(data.wantedLevel);
            dayNightSystem.setGameTime(data.timeOfDay);
            completedMissionIds.clear();
            completedMissionIds.addAll(data.completedMissionIds);
            if ("ACTIVE".equals(data.missionState) && !data.currentMission.isEmpty()) {
                missionManager.restoreMission(data.currentMission, data.missionObjective);
            }
        }
        onResume();
    }

    @Override
    public void onResume() {
        gameStateManager.setState(GameStateManager.GameState.PLAYING);
        pauseMenu.hide();
        bulletAppState.setEnabled(true);
        if (thirdPersonCamera != null) {
            thirdPersonCamera.setEnabled(true);
        }
        inputManager.setCursorVisible(false);
        java.util.Arrays.fill(inputState, false);
    }

    @Override
    public void onQuit() {
        stop();
    }
}
