package com.javacity.world;

import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

public class DayNightSystem {
    private float gameTime = 11.5f; // 11:30 AM Midday sunshine
    private float speedMultiplier = 0.4f; // Relaxed realistic day-night speed 
    
    private DirectionalLight sun;
    private AmbientLight ambient;
    private com.jme3.renderer.ViewPort viewPort;
    private RealisticSky realisticSky;

    public DayNightSystem(DirectionalLight sun, AmbientLight ambient, com.jme3.renderer.ViewPort viewPort, com.jme3.asset.AssetManager assetManager, Node rootNode) {
        this.sun = sun;
        this.ambient = ambient;
        this.viewPort = viewPort;
        this.realisticSky = new RealisticSky(assetManager);
        rootNode.attachChild(realisticSky.getSkyNode());
    }

    public void update(float tpf, Vector3f playerPos) {
        gameTime += (tpf * speedMultiplier) / 60.0f;
        if (gameTime >= 24.0f) {
            gameTime -= 24.0f;
        }

        // Realistic Outdoor Daylight Theme
        ColorRGBA skyColor = new ColorRGBA(0.48f, 0.68f, 0.90f, 1.0f); // Natural blue daylight sky
        if (sun != null) {
            // Natural ~50 degree sun angle illuminating campus front and trees with realistic depth
            sun.setDirection(new Vector3f(0.40f, -0.65f, -0.55f).normalizeLocal());
            sun.setColor(new ColorRGBA(1.0f, 0.96f, 0.88f, 1.0f).mult(1.15f));
        }
        if (ambient != null) {
            // Natural sky ambient fill balanced with LightProbe indirect environment lighting
            ambient.setColor(new ColorRGBA(0.60f, 0.72f, 0.90f, 1.0f).mult(0.35f));
        }
        if (viewPort != null) {
            viewPort.setBackgroundColor(skyColor);
        }
        if (realisticSky != null && playerPos != null) {
            realisticSky.update(tpf, playerPos);
        }
    }
    
    public void update(float tpf) {
        update(tpf, Vector3f.ZERO);
    }

    public boolean isNight() {
        return gameTime > 20.0f || gameTime < 6.0f;
    }

    public float getGameTime() {
        return gameTime;
    }
    
    public void setGameTime(float time) {
        this.gameTime = time;
    }

    public String getTimeString() {
        int hours = (int) gameTime;
        int minutes = (int) ((gameTime - hours) * 60);
        return String.format("%02d:%02d", hours, minutes);
    }
}
