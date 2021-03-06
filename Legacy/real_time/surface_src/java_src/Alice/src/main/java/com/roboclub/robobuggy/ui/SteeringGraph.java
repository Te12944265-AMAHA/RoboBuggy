package com.roboclub.robobuggy.ui;

import com.roboclub.robobuggy.main.RobobuggyConfigFile;
import com.roboclub.robobuggy.messages.SteeringMeasurement;
import com.roboclub.robobuggy.ros.Message;
import com.roboclub.robobuggy.ros.MessageListener;
import com.roboclub.robobuggy.ros.NodeChannel;
import com.roboclub.robobuggy.ros.Subscriber;
import com.sun.javafx.geom.Vec2d;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.util.ArrayList;

/**
 * steering graph
 */
public class SteeringGraph extends RobobuggyGUIContainer {
    private ArrayList<Vec2d> list = new ArrayList();
    private ChartPanel chartPanel;
    private JFreeChart chart;


    /**
     * makes the new steering graph
     */
    public SteeringGraph() {
        XYSeries series1 = new XYSeries("Planned");

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series1);


        new Subscriber("uiSteer", NodeChannel.STEERING.getMsgPath(), new MessageListener() {

            @Override
            public void actionPerformed(String topicName, Message m) {
                SteeringMeasurement steerM = (SteeringMeasurement) m;
                while (series1.getItemCount() > RobobuggyConfigFile.GRAPH_LENGTH) {
                    series1.remove(0);
                }
                series1.add(steerM.getTimestamp().getTime(), steerM.getAngle());


                //		repaint();
            }
        });

        chart = ChartFactory.createXYLineChart("title", "xAxisLabel", "yAxisLabel", dataset,
                PlotOrientation.VERTICAL, true, true, true);
        chartPanel = new ChartPanel(chart);
        add(chartPanel);

        Thread thread = new Thread() {
            public void run() {
                while (true) {
                    //TODO get sizing to be better
                    JFreeChart chart = ChartFactory.createXYLineChart("Steering", "xAxisLabel", "yAxisLabel",
                            dataset, PlotOrientation.VERTICAL, true, true, true);
                /*        XYPlot xyPlot = (XYPlot) chart.getPlot();
				        NumberAxis domainAxis = (NumberAxis) xyPlot.getRangeAxis();
				        NumberAxis rangeAxis = (NumberAxis) xyPlot.getDomainAxis();
						domainAxis.setAutoRange(true);
						rangeAxis.setAutoRange(true);
						*/
                    chartPanel = new ChartPanel(chart);
                    try {
                        this.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        };

        thread.start();


    }


}
