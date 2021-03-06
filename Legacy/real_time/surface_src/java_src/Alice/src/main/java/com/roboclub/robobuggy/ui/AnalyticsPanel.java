package com.roboclub.robobuggy.ui;

import java.awt.Color;

/**
 * {@link RobobuggyGUIContainer} used to display a {@link DataPanel} and a
 * {@link GraphPanel}
 */
public final class AnalyticsPanel extends RobobuggyGUIContainer {

    private static final long serialVersionUID = 7017667286491619492L;

    private DataPanel dataPanel;
    private static AnalyticsPanel instance;
    private Map map;

    /**
     * @return a reference to the analytics panel
     */
    public static synchronized AnalyticsPanel getInstance() {
        if (instance == null) {
            instance = new AnalyticsPanel();
        }
        return instance;

    }

    /**
     * Construct a new {@link AnalyticsPanel}
     */
    private AnalyticsPanel() {
        name = "analytics";
        dataPanel = new DataPanel();
        map = new Map();
        this.addComponent(map, 0, 0, 1, 0.75);
        this.addComponent(dataPanel, 0, 0, 1, 1);
        this.setBackground(Color.RED);

    }

    /**
     * Returns data values from the {@link DataPanel}
     *
     * @return data values from the {@link DataPanel}
     */
    public String valuesFromData() {
        return dataPanel.getValues();
    }


    /**
     * @return the data panel
     */
    public DataPanel getDataPanel() {
        return dataPanel;
    }
}
