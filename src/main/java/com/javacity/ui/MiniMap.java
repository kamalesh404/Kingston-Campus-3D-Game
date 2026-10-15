package com.javacity.ui;

import com.jme3.asset.AssetManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.ui.Picture;
import java.util.ArrayList;
import java.util.List;

/**
 * Full-campus minimap using the rendered college top-view as background.
 * World bounds: X [-260, 260], Z [-390, 390] (matches SpawnManager).
 * Dots: player = green, vehicles = cyan, treasures = gold,
 * race checkpoints = yellow, police = red.
 */
public class MiniMap {
    // Full-map frame: game bounds + margin + dark surround (see render_campus_top.py).
    // 630px <-> X [-300, 300], 944px <-> Z [-450, 450].
    // Image top = -Z (far side), image bottom = +Z (walkway spawn side).
    private static final float X_MIN = -300f, X_MAX = 300f;
    private static final float Z_MIN = -450f, Z_MAX = 450f;

    private Node guiNode;
    private float mapW = 200f;
    private float mapH = 300f; // 2:3 like the campus image
    private float posX = 20f;
    private float posY = 20f;

    private Picture mapBg;
    private Picture playerDot;
    private List<Picture> markerPool = new ArrayList<>();

    public MiniMap(Node guiNode, AssetManager assetManager, int screenWidth, int screenHeight) {
        this.guiNode = guiNode;

        // Campus top-view background: generated render of OUR college model.
        // (realistic_map_view.jpg is a stock photo of a different campus, so it
        // must NOT be used - dots would plot on wrong buildings.)
        mapBg = new Picture("MiniMapBg");
        if (!tryClasspath(assetManager, "Textures/Minimap/campus_top.png")
                && !tryFile(assetManager, "assets/Textures/Minimap/campus_top.png")
                && !tryFile(assetManager, "build/resources/main/Textures/Minimap/campus_top.png")) {
            com.jme3.material.Material matBg = new com.jme3.material.Material(assetManager, "Common/MatDefs/Gui/Gui.j3md");
            matBg.setColor("Color", new ColorRGBA(0.05f, 0.08f, 0.12f, 0.75f));
            mapBg.setMaterial(matBg);
            System.out.println("MiniMap: WARNING background image not found, using dark panel");
        }
        mapBg.setWidth(mapW);
        mapBg.setHeight(mapH);
        mapBg.setLocalTranslation(posX, posY, 0);
        guiNode.attachChild(mapBg);

        // Player marker (green)
        playerDot = makeDot(assetManager, "MiniMapPlayer", ColorRGBA.Green, 9f);
        guiNode.attachChild(playerDot);

        // Pool for vehicles, treasures, checkpoints, police
        for (int i = 0; i < 30; i++) {
            Picture p = makeDot(assetManager, "MiniMapMarker_" + i, ColorRGBA.Yellow, 7f);
            p.setLocalTranslation(-100, -100, 1);
            guiNode.attachChild(p);
            markerPool.add(p);
        }
    }

    private boolean tryClasspath(AssetManager assetManager, String path) {
        try {
            mapBg.setImage(assetManager, path, true);
            System.out.println("MiniMap: background loaded from classpath: " + path);
            return true;
        } catch (Exception e) {
            System.out.println("MiniMap: classpath miss: " + path);
            return false;
        }
    }

    private boolean tryFile(AssetManager assetManager, String path) {
        try {
            java.awt.image.BufferedImage awt =
                javax.imageio.ImageIO.read(new java.io.File(path));
            if (awt == null) return false;
            com.jme3.texture.Image img =
                new com.jme3.texture.plugins.AWTLoader().load(awt, true);
            mapBg.setTexture(assetManager, new com.jme3.texture.Texture2D(img), true);
            System.out.println("MiniMap: background loaded from file: " + path);
            return true;
        } catch (Exception e) {
            System.out.println("MiniMap: file miss: " + path);
            return false;
        }
    }

    private Picture makeDot(AssetManager assetManager, String name, ColorRGBA color, float size) {
        Picture p = new Picture(name);
        com.jme3.material.Material mat = new com.jme3.material.Material(assetManager, "Common/MatDefs/Gui/Gui.j3md");
        mat.setColor("Color", color);
        p.setMaterial(mat);
        p.setWidth(size);
        p.setHeight(size);
        return p;
    }

    private float mapX(float worldX) {
        return posX + ((worldX - X_MIN) / (X_MAX - X_MIN)) * mapW;
    }

    private float mapY(float worldZ) {
        // Image top = -Z, gui +Y is up, so +Z (spawn side) sits at the bottom
        return posY + ((Z_MAX - worldZ) / (Z_MAX - Z_MIN)) * mapH;
    }

    private void placeDot(Picture p, Vector3f world, float zOrder) {
        float s = p.getWidth();
        p.setLocalTranslation(mapX(world.x) - s / 2f, mapY(world.z) - s / 2f, zOrder);
    }

    public void update(Vector3f playerPos, List<Vector3f> vehiclePositions,
                       List<Vector3f> checkpointPositions, List<Vector3f> policePositions) {
        update(playerPos, vehiclePositions, checkpointPositions, policePositions, null);
    }

    public void update(Vector3f playerPos, List<Vector3f> vehiclePositions,
                       List<Vector3f> checkpointPositions, List<Vector3f> policePositions,
                       List<Vector3f> treasurePositions) {
        if (playerPos != null) {
            placeDot(playerDot, playerPos, 3);
        }

        int markerIndex = 0;
        markerIndex = plotList(vehiclePositions, ColorRGBA.Cyan, markerIndex, 1);
        markerIndex = plotList(treasurePositions, ColorRGBA.Yellow, markerIndex, 2);
        markerIndex = plotList(checkpointPositions, new ColorRGBA(1f, 0.6f, 0.1f, 1f), markerIndex, 2);
        markerIndex = plotList(policePositions, ColorRGBA.Red, markerIndex, 2);

        for (int i = markerIndex; i < markerPool.size(); i++) {
            markerPool.get(i).setLocalTranslation(-100, -100, 1);
        }
    }

    private int plotList(List<Vector3f> positions, ColorRGBA color, int startIndex, float zOrder) {
        if (positions == null) return startIndex;
        int idx = startIndex;
        for (Vector3f w : positions) {
            if (idx >= markerPool.size() || w == null) break;
            if (w.x < X_MIN || w.x > X_MAX || w.z < Z_MIN || w.z > Z_MAX) continue;
            Picture p = markerPool.get(idx++);
            p.getMaterial().setColor("Color", color);
            placeDot(p, w, zOrder);
        }
        return idx;
    }
}
