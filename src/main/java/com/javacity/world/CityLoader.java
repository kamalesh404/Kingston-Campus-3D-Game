package com.javacity.world;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import java.util.ArrayList;
import java.util.List;

public class CityLoader {
    private AssetManager assetManager;
    private PhysicsSpace physicsSpace;
    private Spatial cityModel;

    public CityLoader(AssetManager assetManager, PhysicsSpace physicsSpace) {
        this.assetManager = assetManager;
        this.physicsSpace = physicsSpace;
    }

    public float getGroundHeight(float x, float z) {
        if (cityModel != null) {
            com.jme3.collision.CollisionResults results = new com.jme3.collision.CollisionResults();
            com.jme3.math.Ray ray = new com.jme3.math.Ray(new com.jme3.math.Vector3f(x, 1500f, z), new com.jme3.math.Vector3f(0, -1f, 0));
            cityModel.collideWith(ray, results);
            if (results.size() > 0) {
                for (int i = 0; i < results.size(); i++) {
                    float hitY = results.getCollision(i).getContactPoint().y;
                    if (hitY < 300f) {
                        return hitY;
                    }
                }
                return results.getClosestCollision().getContactPoint().y;
            }
        }
        return 0f;
    }

    public Node loadCity() {
        Node cityNode = new Node("City");
        
        cityModel = null;
        try {
            cityModel = assetManager.loadModel("Scenes/City/newclg.glb");
            System.out.println("Loaded Kingston Engineering College model: Scenes/City/newclg.glb");
        } catch (Exception e) {
            System.err.println("WARNING: Failed to load newclg.glb, attempting fallback: " + e.getMessage());
            try {
                cityModel = assetManager.loadModel("Scenes/City/newclg1.glb");
            } catch (Exception ex) {
                cityModel = createTempGround();
            }
        }

        // Remove floating roof archway/signboard artifacts hovering above the college
        removeFloatingArtifacts(cityModel);
        
        // Sanitize any negative scale transforms from imported 3D nodes before Bullet physics generation
        sanitizeNegativeScales(cityModel);

        // Gently ensure materials render naturally without over-reflection
        adjustMaterialsForNonIBL(cityModel);
        
        cityModel.updateModelBound();
        com.jme3.bounding.BoundingVolume bound = cityModel.getWorldBound();
        System.out.println("Kingston College raw bounds: " + bound);

        cityModel.setLocalScale(com.javacity.core.GameConfig.WORLD_SCALE);
        // Align model ground base (min Y = -0.0677) to y = 0.0f
        float yGroundOffset = 0.06771444f * com.javacity.core.GameConfig.WORLD_SCALE;
        cityModel.setLocalTranslation(0, yGroundOffset, 0);
        
        CollisionShape sceneShape = null;
        try {
            sceneShape = CollisionShapeFactory.createMeshShape(cityModel);
        } catch (Exception e) {
            System.err.println("Warning creating MeshShape for scene: " + e.getMessage() + ". Utilizing BoxShape fallback.");
            sceneShape = CollisionShapeFactory.createBoxShape(cityModel);
        }
        
        RigidBodyControl landscape = new RigidBodyControl(sceneShape, 0);
        cityModel.addControl(landscape);
        physicsSpace.add(landscape);

        // Solid floor under Kingston College campus ground
        float xMin = -260f, xMax = 260f;
        float zMin = -390f, zMax = 390f;
        float halfX = (xMax - xMin) / 2f;
        float halfZ = (zMax - zMin) / 2f;
        float midX = (xMin + xMax) / 2f;
        float midZ = (zMin + zMax) / 2f;

        com.jme3.bullet.collision.shapes.BoxCollisionShape groundShape = new com.jme3.bullet.collision.shapes.BoxCollisionShape(new com.jme3.math.Vector3f(halfX + 200f, 5.0f, halfZ + 200f));
        RigidBodyControl groundBody = new RigidBodyControl(groundShape, 0);
        groundBody.setPhysicsLocation(new com.jme3.math.Vector3f(midX, -5.0f, midZ));
        physicsSpace.add(groundBody);

        // Invisible World Perimeter Barriers around Kingston College
        float wallHeight = 500f;

        com.jme3.bullet.collision.shapes.BoxCollisionShape northWall = new com.jme3.bullet.collision.shapes.BoxCollisionShape(new Vector3f(halfX + 50f, wallHeight, 20f));
        RigidBodyControl northBody = new RigidBodyControl(northWall, 0);
        northBody.setPhysicsLocation(new Vector3f(midX, wallHeight, zMax + 10f));
        physicsSpace.add(northBody);

        com.jme3.bullet.collision.shapes.BoxCollisionShape southWall = new com.jme3.bullet.collision.shapes.BoxCollisionShape(new Vector3f(halfX + 50f, wallHeight, 20f));
        RigidBodyControl southBody = new RigidBodyControl(southWall, 0);
        southBody.setPhysicsLocation(new Vector3f(midX, wallHeight, zMin - 10f));
        physicsSpace.add(southBody);

        com.jme3.bullet.collision.shapes.BoxCollisionShape eastWall = new com.jme3.bullet.collision.shapes.BoxCollisionShape(new Vector3f(20f, wallHeight, halfZ + 50f));
        RigidBodyControl eastBody = new RigidBodyControl(eastWall, 0);
        eastBody.setPhysicsLocation(new Vector3f(xMax + 10f, wallHeight, midZ));
        physicsSpace.add(eastBody);

        com.jme3.bullet.collision.shapes.BoxCollisionShape westWall = new com.jme3.bullet.collision.shapes.BoxCollisionShape(new Vector3f(20f, wallHeight, halfZ + 50f));
        RigidBodyControl westBody = new RigidBodyControl(westWall, 0);
        westBody.setPhysicsLocation(new Vector3f(xMin - 10f, wallHeight, midZ));
        physicsSpace.add(westBody);

        cityNode.attachChild(createGroundPlane());
        cityNode.attachChild(cityModel);
        
        return cityNode;
    }

    private void removeFloatingArtifacts(Spatial spatial) {
        if (spatial instanceof Node) {
            Node node = (Node) spatial;
            List<Spatial> toRemove = new ArrayList<>();
            for (Spatial child : node.getChildren()) {
                String name = child.getName();
                if (name != null && isArtifactName(name)) {
                    toRemove.add(child);
                } else {
                    removeFloatingArtifacts(child);
                }
            }
            for (Spatial s : toRemove) {
                node.detachChild(s);
                System.out.println("Removed unwanted model from college top: " + s.getName());
            }
        }
    }

    private boolean isArtifactName(String name) {
        return name.startsWith("FlagPole")
                || name.startsWith("Kingston_Entrance")
                || name.startsWith("Kingston_Left_Pillar")
                || name.startsWith("Kingston_Right_Pillar")
                || name.startsWith("Kingston_Signboard");
    }

    // Creates a visible semi-transparent test wall so the map boundary can be
    // seen and calibrated. The geometric size (extents) is encoded in the name.
    private Spatial createVisibleWall(String name, Vector3f center, com.jme3.math.Vector3f extents, ColorRGBA color) {
        Box box = new Box(extents.x / 2f, extents.y / 2f, extents.z / 2f);
        Geometry wallGeo = new Geometry(name, box);
        Material mat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        mat.setColor("BaseColor", color);
        mat.setFloat("Metallic", 0.0f);
        mat.setFloat("Roughness", 0.6f);
        mat.getAdditionalRenderState().setBlendMode(com.jme3.material.RenderState.BlendMode.Alpha);
        mat.getAdditionalRenderState().setDepthWrite(false);
        wallGeo.setMaterial(mat);
        wallGeo.setLocalTranslation(center);
        wallGeo.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Transparent);
        return wallGeo;
    }

    // Renders a big readable wall name as an in-world 3D text so the boundary
    // label is visible from inside the map. `yaw` rotates it to face the interior.
    private Spatial createWallLabel(String text, Vector3f center, float yaw) {
        com.jme3.font.BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        com.jme3.font.BitmapText label = new com.jme3.font.BitmapText(font);
        label.setText(text);
        label.setSize(120f);          // font size
        label.setColor(ColorRGBA.Red);
        label.setLocalTranslation(center);
        label.setLocalScale(new com.jme3.math.Vector3f(6f, 4f, 1f)); // enlarge readable text
        label.rotate(0, yaw, 0);
        label.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Transparent);
        return label;
    }

    // Visible ground plane low under the city so it never shows through the
    // street surface as black patches. The city model base sits at y=-0.2.
    private Spatial createGroundPlane() {
        float width = 340f;    // covers X:-65..275
        float depth = 717f;    // covers Z:-738..-21
        Quad groundQuad = new Quad(width, depth);
        Geometry groundGeo = new Geometry("WorldGroundPlane", groundQuad);
        Material mat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        mat.setColor("BaseColor", new ColorRGBA(0.05f, 0.055f, 0.07f, 1.0f));
        mat.setFloat("Roughness", 0.9f);
        mat.setFloat("Metallic", 0.0f);
        groundGeo.setMaterial(mat);
        groundGeo.setLocalTranslation(105f, -0.5f, -379.5f);
        groundGeo.rotate(-FastMath.HALF_PI, 0, 0);
        return groundGeo;
    }

    private void sanitizeNegativeScales(Spatial spatial) {
        if (spatial == null) return;
        com.jme3.math.Vector3f scale = spatial.getLocalScale();
        if (scale.x < 0 || scale.y < 0 || scale.z < 0) {
            spatial.setLocalScale(Math.abs(scale.x), Math.abs(scale.y), Math.abs(scale.z));
        }
        if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                sanitizeNegativeScales(child);
            }
        }
    }

    private void adjustMaterialsForNonIBL(Spatial spatial) {
        if (spatial instanceof Geometry) {
            Geometry geo = (Geometry) spatial;
            Material mat = geo.getMaterial();
            String name = (geo.getName() != null ? geo.getName() : "").toLowerCase();
            String matName = (mat != null && mat.getName() != null ? mat.getName() : "").toLowerCase();

            if (mat != null && mat.getMaterialDef() != null && mat.getMaterialDef().getName().contains("PBR")) {
                try {
                    // Remove all dark specular/metallic reflections on non-metal surfaces
                    if (name.contains("metal") || name.contains("pole") || name.contains("pipe") || matName.contains("metal")) {
                        mat.setFloat("Roughness", 0.25f);
                        mat.setFloat("Metallic", 0.85f);
                    } else if (name.contains("glass") || name.contains("window") || matName.contains("glass") || matName.contains("window")) {
                        mat.setFloat("Roughness", 0.10f);
                        mat.setFloat("Metallic", 0.10f);
                    } else {
                        // All foliage, trees, terrain, campus buildings, columns & walls:
                        // Pure matte with ZERO metallic reflection so trees and walls never look like oily black rubber
                        mat.setFloat("Roughness", 0.95f);
                        mat.setFloat("Metallic", 0.0f);
                    }
                } catch (Exception e) {
                    // Ignore parameter mismatch
                }
            }
        }
        
        if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                adjustMaterialsForNonIBL(child);
            }
        }
    }

    private Spatial createTempGround() {
        Quad quad = new Quad(100f, 100f);
        Geometry geo = new Geometry("TempGround", quad);
        Material mat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        mat.setColor("BaseColor", ColorRGBA.Gray);
        mat.setFloat("Roughness", 0.8f);
        mat.setFloat("Metallic", 0.1f);
        geo.setMaterial(mat);
        geo.setLocalTranslation(-50f, 0, 50f);
        geo.rotate(-1.5708f, 0, 0);
        return geo;
    }
}
