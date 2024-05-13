package com.skcraft.launcher.swing;

import lombok.extern.java.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author dags <dags@dags.me>
 */
@Log
public class RedditBackgroundPanel extends JPanel implements Runnable, ActionListener, Paintable {

    private static final ImageFader DEFAULT = defaultFader();

    private final AtomicReference<ImageFader> reference;
    private final AtomicBoolean showing;
    private final AtomicBoolean repaint;
    private final boolean random;
    private final Timer timer;
    private final long delay;
    private final long fade;

    public RedditBackgroundPanel(String subreddit, int postCount, boolean randomise, long interval, long fade) {
        this.reference = new AtomicReference<ImageFader>(DEFAULT);
        this.showing = new AtomicBoolean(true);
        this.repaint = new AtomicBoolean(true);
        this.timer = new Timer(200, this);
        this.random = randomise;
        this.delay = interval;
        this.fade = fade;
        this.timer.start();
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        ImageFader image = reference.get();
        image.paint(this, graphics, getWidth(), getHeight());
    }

    @Override
    public void paint(Graphics graphics) {
        super.paint(graphics);
        if (reference.get().isFading()) {
            repaint();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isShowing()) {
            if (repaint.get()) {
                repaint.set(false);
                repaint();
            }
        } else {
            timer.stop();
            showing.set(false);
        }
    }

    @Override
    public void run() {
        int index = 0;

        String directoryPath = "com/skcraft/launcher/media_embed/";  // Relative path to the directory

        List<String> targets = loadImagesFromDirectory(directoryPath);

        if (random) {
            Collections.shuffle(targets);
        }


        while (showing.get() && index < targets.size()) {
            try {
                String next = targets.get(index);
                ImageFader image = getImage(next);

                if (image != null) {
                    reference.set(image);
                    repaint.set(true);
                }

                if (++index >= targets.size()) {
                    index = 0;
                }

                Thread.sleep(delay);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private List<String> loadImagesFromDirectory(String directoryPath) {
        List<String> targets = new ArrayList<>();

        // Use the ClassLoader to load resources from the directory
        ClassLoader classLoader = RedditBackgroundPanel.class.getClassLoader();
        URL directoryURL = classLoader.getResource(directoryPath);

        if (directoryURL != null) {
            if (directoryURL.getProtocol().equals("jar")) {
                // If the directory is inside a JAR file
                try {
                    JarURLConnection jarConnection = (JarURLConnection) directoryURL.openConnection();
                    JarFile jarFile = jarConnection.getJarFile();
                    Enumeration<JarEntry> jarEntries = jarFile.entries();

                    while (jarEntries.hasMoreElements()) {
                        JarEntry jarEntry = jarEntries.nextElement();
                        String entryName = jarEntry.getName();

                        if (entryName.startsWith(directoryPath) && !entryName.equals(directoryPath + "/") &&
                                (entryName.toLowerCase().endsWith(".jpg") || entryName.toLowerCase().endsWith(".jpeg") || entryName.toLowerCase().endsWith(".png"))) {
                            targets.add(entryName);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return targets;
    }

    private ImageFader getImage(String address) {
        try {
            ImageFader current = reference.get();
            BufferedImage from = current != null ? current.getTo() : null;

            // Use the ClassLoader to load the image
            ClassLoader classLoader = RedditBackgroundPanel.class.getClassLoader();
            URL imageURL = classLoader.getResource(address);

            if (imageURL != null) {
                Image image = Toolkit.getDefaultToolkit().createImage(imageURL);
                MediaTracker mediaTracker = new MediaTracker(this);
                mediaTracker.addImage(image, 0);
                mediaTracker.waitForID(0);

                BufferedImage to = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = to.createGraphics();
                g.drawImage(image, 0, 0, null);
                g.dispose();

                return new ImageFader(from, to, fade);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static ImageFader defaultFader() {
        BufferedImage blank = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        return new ImageFader(blank, blank, 1000L);
    }
}
