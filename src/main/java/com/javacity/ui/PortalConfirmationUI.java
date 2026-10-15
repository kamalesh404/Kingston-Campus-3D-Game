package com.javacity.ui;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.jme3.ui.Picture;

public class PortalConfirmationUI {
    private final Node guiNode;
    private final Node panel = new Node("PortalConfirmationUI");
    private final Runnable onConfirm;
    private final Runnable onCancel;
    private boolean visible = false;

    public PortalConfirmationUI(Node guiNode, AssetManager assets, BitmapFont font, int width, int height,
                                Runnable onConfirm, Runnable onCancel) {
        this.guiNode = guiNode;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;

        // Dark translucent backdrop
        Picture bg = new Picture("ConfirmBg");
        Material mat = new Material(assets, "Common/MatDefs/Gui/Gui.j3md");
        mat.setColor("Color", new ColorRGBA(0.02f, 0.05f, 0.12f, 0.95f));
        bg.setMaterial(mat);
        float pWidth = width * 0.58f;
        float pHeight = height * 0.56f;
        float pX = (width - pWidth) / 2f;
        float pY = (height - pHeight) / 2f;
        bg.setWidth(pWidth);
        bg.setHeight(pHeight);
        bg.setLocalTranslation(pX, pY, 0);
        panel.attachChild(bg);

        // Glowing border top line
        Picture borderTop = new Picture("BorderTop");
        Material bMat = new Material(assets, "Common/MatDefs/Gui/Gui.j3md");
        bMat.setColor("Color", new ColorRGBA(0.15f, 0.85f, 1.0f, 1.0f));
        borderTop.setMaterial(bMat);
        borderTop.setWidth(pWidth);
        borderTop.setHeight(3f);
        borderTop.setLocalTranslation(pX, pY + pHeight - 3f, 1);
        panel.attachChild(borderTop);

        // Title
        BitmapText title = new BitmapText(font);
        title.setText("PORTAL GATEWAY : THE ARCHER REALM");
        title.setSize(font.getCharSet().getRenderedSize() * 1.5f);
        title.setColor(new ColorRGBA(0.2f, 0.9f, 1.0f, 1.0f));
        title.setLocalTranslation(pX + 30f, pY + pHeight - 30f, 2);
        panel.attachChild(title);

        // Subtitle & Lore
        BitmapText sub = new BitmapText(font);
        sub.setText("Step through the ancient gateway into the forgotten proving grounds.\nEngage in third-person bow & arrow combat against mutant monster hordes!");
        sub.setSize(font.getCharSet().getRenderedSize() * 0.95f);
        sub.setColor(new ColorRGBA(0.8f, 0.85f, 0.95f, 1.0f));
        sub.setLocalTranslation(pX + 30f, pY + pHeight - 80f, 2);
        panel.attachChild(sub);

        // Controls box
        BitmapText controls = new BitmapText(font);
        controls.setText(
            "ARCHER REALM CONTROLS:\n" +
            "  - WASD        : Move & Walk\n" +
            "  - Left Shift  : Sprint\n" +
            "  - Mouse       : Camera Look / Aim\n" +
            "  - [E]         : Draw & Aim Bow (Over-the-shoulder Zoom)\n" +
            "  - [LMB]       : Fire Arrow\n" +
            "  - [R]         : Switch Arrow Type (Normal / Explosive / Acid)\n" +
            "  - [Q]         : RETURN TO KINGSTON CAMPUS AT ANY TIME"
        );
        controls.setSize(font.getCharSet().getRenderedSize() * 0.90f);
        controls.setColor(new ColorRGBA(0.95f, 0.80f, 0.30f, 1.0f));
        controls.setLocalTranslation(pX + 30f, pY + pHeight - 150f, 2);
        panel.attachChild(controls);

        // Prompt actions
        BitmapText actionPrompt = new BitmapText(font);
        actionPrompt.setText("[F] or [ENTER] : ENTER ARCHER REALM      |      [ESC] : CANCEL & STAY IN CAMPUS");
        actionPrompt.setSize(font.getCharSet().getRenderedSize() * 1.05f);
        actionPrompt.setColor(new ColorRGBA(0.2f, 1.0f, 0.6f, 1.0f));
        actionPrompt.setLocalTranslation(pX + 30f, pY + 45f, 2);
        panel.attachChild(actionPrompt);
    }

    public void show() {
        if (!visible) {
            visible = true;
            guiNode.attachChild(panel);
        }
    }

    public void hide() {
        if (visible) {
            visible = false;
            panel.removeFromParent();
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public void confirm() {
        if (visible) {
            hide();
            if (onConfirm != null) onConfirm.run();
        }
    }

    public void cancel() {
        if (visible) {
            hide();
            if (onCancel != null) onCancel.run();
        }
    }
}
