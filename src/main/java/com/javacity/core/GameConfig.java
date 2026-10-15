package com.javacity.core;

public class GameConfig {
    // Balanced is the default demonstration preset. It keeps the original
    // models/materials while avoiding costly post-processing on laptop GPUs.
    private static final String QUALITY = System.getProperty("ecocity.quality", "balanced").toLowerCase();
    public static final boolean LOW_END = QUALITY.equals("low") ||
            (QUALITY.equals("auto") && (Runtime.getRuntime().availableProcessors() <= 4
                    || Runtime.getRuntime().maxMemory() <= 768L * 1024L * 1024L));
    public static final boolean POST_PROCESSING = QUALITY.equals("high") || (!LOW_END && !QUALITY.equals("balanced"));
    private static java.awt.DisplayMode getDesktopDisplayMode() {
        try {
            return java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDisplayMode();
        } catch (Throwable t) {
            return new java.awt.DisplayMode(1920, 1080, 24, 60);
        }
    }

    private static final java.awt.DisplayMode DESKTOP_MODE = getDesktopDisplayMode();

    // Display
    public static final int DISPLAY_WIDTH = Integer.getInteger("ecocity.width", DESKTOP_MODE.getWidth());
    public static final int DISPLAY_HEIGHT = Integer.getInteger("ecocity.height", DESKTOP_MODE.getHeight());
    // Fullscreen borderless window matching native monitor resolution
    public static final boolean DISPLAY_FULLSCREEN = Boolean.parseBoolean(System.getProperty("ecocity.fullscreen", "true"));
    public static final boolean DISPLAY_VSYNC = true;
    // Hard-locked to 60 FPS for stable, smooth gameplay. VSync enforces this at the GPU level.
    public static final int DISPLAY_FPS = Integer.getInteger("ecocity.fps", 60);

    // Player
    public static final float PLAYER_WALK_SPEED = 8.0f;
    public static final float PLAYER_RUN_SPEED = 16.0f;
    public static final float PLAYER_SPRINT_SPEED = 24.0f;
    public static final float PLAYER_JUMP_FORCE = 600f;

    // Camera
    public static final float CAMERA_DISTANCE = 10.0f;
    public static final float CAMERA_MIN_DISTANCE = 4.0f;
    public static final float CAMERA_MAX_DISTANCE = 28f;
    public static final float CAMERA_SENSITIVITY = 2f;

    // Vehicle
    public static final float VEHICLE_MAX_SPEED = 65f;
    public static final float VEHICLE_ACCELERATION = 950f;
    public static final float VEHICLE_BRAKE_FORCE = 130f;
    public static final float VEHICLE_STEERING_SPEED = 0.5f;

    // World
    public static final int NPC_POPULATION_LIMIT = Integer.getInteger("ecocity.npcs", LOW_END ? 18 : 32);
    public static final int VEHICLE_POPULATION_LIMIT = Integer.getInteger("ecocity.vehicles", LOW_END ? 12 : 24);
    public static final float ACTIVATION_DISTANCE = LOW_END ? 140f : 200f;
    public static final float NPC_SIMULATION_DISTANCE = LOW_END ? 120f : 175f;
    public static final int SHADOW_MAP_SIZE = Integer.getInteger("ecocity.shadowMap", LOW_END ? 512 : 1024);
    public static final float UI_UPDATE_INTERVAL = LOW_END ? 0.10f : 0.05f;

    // Physics
    public static final float PHYSICS_GRAVITY = -29.4f;

    // DayNight
    public static final float DAY_DURATION_SECONDS = 300f;

    // Scaling Configuration
    public static final float WORLD_SCALE = 800.0f;
    public static final float PLAYER_SCALE = 3.5f;
    public static final float CAR_SCALE = 1.40f;
    public static final float NPC_SCALE = 3.5f;
}
