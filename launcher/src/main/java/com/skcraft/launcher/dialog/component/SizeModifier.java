package com.skcraft.launcher.dialog.component;

import java.awt.*;

public class SizeModifier {

    public double sizeModifier;

    public void calculateSizeModifier() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width;
        int screenHeight = screenSize.height;

        sizeModifier = Math.min(screenWidth / 1920.0, screenHeight / 1080.0);
    }
}
