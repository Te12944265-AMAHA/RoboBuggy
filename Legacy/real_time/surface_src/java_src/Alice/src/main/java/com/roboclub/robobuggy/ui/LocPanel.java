package com.roboclub.robobuggy.ui;

import com.roboclub.robobuggy.main.RobobuggyLogicNotification;
import com.roboclub.robobuggy.main.RobobuggyMessageLevel;
import com.roboclub.robobuggy.messages.GpsMeasurement;
import com.roboclub.robobuggy.nodes.localizers.LocTuple;
import com.roboclub.robobuggy.ros.Message;
import com.roboclub.robobuggy.ros.MessageListener;
import com.roboclub.robobuggy.ros.NodeChannel;
import com.roboclub.robobuggy.ros.Subscriber;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

/**
 * {@link JPanel} used to display location information
 */
public class LocPanel extends JPanel {

    private static final long serialVersionUID = 42L;
    private ArrayList<LocTuple> locs;
    private LocTuple imgNorthEast;
    private LocTuple imgSouthWest;
    private BufferedImage map;
    private boolean setup;
    private int frameWidth;
    private int frameHeight;
    private Subscriber gpsSub;

    /**
     * Construct a new {@link LocPanel}
     */
    public LocPanel() {
        locs = new ArrayList<LocTuple>();
        imgNorthEast = new LocTuple(-79.93596322545625, 40.443946388131266);
        imgSouthWest = new LocTuple(-79.95532877484377, 40.436597411027364);
        try {
            map = ImageIO.read(new File("images/lat_long_course_map.png"));
        } catch (Exception e) {
            new RobobuggyLogicNotification("Unable to read map image!", RobobuggyMessageLevel.WARNING);
        }
        setup = false;

        gpsSub = new Subscriber("uiLoc", NodeChannel.GPS.getMsgPath(), new MessageListener() {
            @Override
            public void actionPerformed(String topicName, Message m) {
                double latitude = ((GpsMeasurement) m).getLatitude();
                double longitude = ((GpsMeasurement) m).getLongitude();
                locs.add(new LocTuple(latitude, longitude));
                Gui.getInstance().fixPaint();
            }
        });

        locs.add(new LocTuple(-79.94596322545625, 40.440946388131266));
    }

    private void setup() {
        frameWidth = getWidth();
        frameHeight = getHeight();
    }

    private void drawTuple(Graphics2D g2d, LocTuple mTuple) {
        double dx = imgSouthWest.getLatitude() - imgNorthEast.getLatitude();
        double dy = imgSouthWest.getLongitude() - imgNorthEast.getLongitude();
        double x = (mTuple.getLatitude() - imgNorthEast.getLatitude()) / dx * frameWidth;
        double y = (mTuple.getLongitude() - imgSouthWest.getLongitude()) / dy * frameHeight;
        int cDiameter = 5;
        g2d.setColor(Color.RED);
        g2d.drawOval((int) x, -(int) y, cDiameter, cDiameter);
        g2d.fillOval((int) x, -(int) y, cDiameter, cDiameter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!setup) {
            setup();
            setup = true;
        }
        Graphics2D g2d = (Graphics2D) g.create();
        g.drawImage(map, 0, 0, frameWidth, frameHeight, Color.black, null);
        for (LocTuple mTuple : locs) {
            drawTuple(g2d, mTuple);
        }
        g2d.dispose();
    }
}