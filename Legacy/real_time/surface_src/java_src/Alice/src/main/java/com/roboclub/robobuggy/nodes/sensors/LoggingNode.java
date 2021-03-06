package com.roboclub.robobuggy.nodes.sensors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.roboclub.robobuggy.main.RobobuggyConfigFile;
import com.roboclub.robobuggy.main.RobobuggyLogicNotification;
import com.roboclub.robobuggy.main.RobobuggyMessageLevel;
import com.roboclub.robobuggy.messages.BrakeMessage;
import com.roboclub.robobuggy.messages.EncoderMeasurement;
import com.roboclub.robobuggy.messages.FingerPrintMessage;
import com.roboclub.robobuggy.messages.GPSPoseMessage;
import com.roboclub.robobuggy.messages.GpsMeasurement;
import com.roboclub.robobuggy.messages.GuiLoggingButtonMessage;
import com.roboclub.robobuggy.messages.ImageMessage;
import com.roboclub.robobuggy.messages.ImuMeasurement;
import com.roboclub.robobuggy.messages.NodeStatusMessage;
import com.roboclub.robobuggy.messages.ResetMessage;
import com.roboclub.robobuggy.messages.RobobuggyLogicNotificationMeasurement;
import com.roboclub.robobuggy.messages.StateMessage;
import com.roboclub.robobuggy.messages.SteeringMeasurement;
import com.roboclub.robobuggy.nodes.baseNodes.BuggyBaseNode;
import com.roboclub.robobuggy.nodes.baseNodes.BuggyDecoratorNode;
import com.roboclub.robobuggy.nodes.baseNodes.SerialNode;
import com.roboclub.robobuggy.ros.Message;
import com.roboclub.robobuggy.ros.MessageListener;
import com.roboclub.robobuggy.ros.NodeChannel;
import com.roboclub.robobuggy.ros.Publisher;
import com.roboclub.robobuggy.ros.Subscriber;
import com.roboclub.robobuggy.ui.Gui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * {@link SerialNode} for reading in logging commands from the GUI
 * When logging begins, a new folder is created, and then logging begins
 * to that folder
 */
public class LoggingNode extends BuggyDecoratorNode {

    private Publisher loggingButtonPub;
    private File outputFile;
    private File outputDirectory;
    private NodeChannel[] filters;
    private LinkedBlockingQueue<Message> messageQueue;
    private LogWriterThread loggingThread;
    private boolean keepLogging;

    private static final int MAX_QUEUE_SIZE = 10000;

    private Publisher statusPub;

    private static final String DATE_FILE_FORMAT = "yyyy-MM-dd-HH-mm-ss";


    /**
     * the statuses of the logging node
     */
    public enum LoggingNodeStatus implements INodeStatus {
        INITIALIZED,
        STARTED_LOGGING,
        STOPPED_LOGGING,
    }


    /**
     * Create a new {@link LoggingNode} decorator
     *
     * @param channel       the {@link NodeChannel} of the {@link LoggingNode}
     * @param outputDirPath The path to the output directory (not file)
     * @param filters       sensors to log. To log all sensors, just use NodeChannel.values()
     */
    public LoggingNode(NodeChannel channel, String outputDirPath, NodeChannel... filters) {
        super(new BuggyBaseNode(channel), "logging_node");

        this.filters = filters;
        messageQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
        keepLogging = true;
        outputDirectory = new File(outputDirPath);

        statusPub = new Publisher(NodeChannel.NODE_STATUS.getMsgPath());

        setupSubscriberList();

        if (!RobobuggyConfigFile.isDataPlayBack()) {
            setupLoggingTrigger();
        }

        statusPub.publish(new NodeStatusMessage(LoggingNode.class, LoggingNodeStatus.INITIALIZED, null));

    }


    /**
     * Starts the logging process
     */
    private void setupLoggingTrigger() {
        new Subscriber("log", NodeChannel.GUI_LOGGING_BUTTON.getMsgPath(), new MessageListener() {
            @Override
            public void actionPerformed(String topicName, Message m) {

                GuiLoggingButtonMessage message = (GuiLoggingButtonMessage) m;
                if (message.getLoggingMessage().equals(GuiLoggingButtonMessage.LoggingMessage.START)) {

                    if (!createNewLogFile()) {

                        new RobobuggyLogicNotification("Error creating new log file!", RobobuggyMessageLevel.EXCEPTION);
                        return;

                    }

                    // we want to clear out old messages every time we start to log
                    messageQueue.clear();

                    keepLogging = true;
                    loggingThread = new LogWriterThread();
                    loggingThread.start();
                    new RobobuggyLogicNotification("Starting up logging thread!", RobobuggyMessageLevel.NOTE);
                    JsonObject params = new JsonObject();
                    params.addProperty("outputDir", outputDirectory.getPath());
                    statusPub.publish(new NodeStatusMessage(LoggingNode.class, LoggingNodeStatus.STARTED_LOGGING, params));

                } else if (message.getLoggingMessage().equals(GuiLoggingButtonMessage.LoggingMessage.STOP)) {

                    keepLogging = false;
                    new RobobuggyLogicNotification("Stopping logging thread!", RobobuggyMessageLevel.NOTE);
                    statusPub.publish(new NodeStatusMessage(LoggingNode.class, LoggingNodeStatus.STOPPED_LOGGING, null));
                    loggingThread.interrupt();

                } else {

                    new RobobuggyLogicNotification("Gui said something logger couldn't understand!", RobobuggyMessageLevel.EXCEPTION);

                }
            }
        });
    }

    /**
     * Sets up the subscriber list - Simply enumerates over our NodeChannel filters and adds
     * a subscriber for each one
     */
    private void setupSubscriberList() {
        for (NodeChannel filter : filters) {
            new Subscriber("log", filter.getMsgPath(), new MessageListener() {
                @Override
                public void actionPerformed(String topicName, Message m) {
                    while (!messageQueue.offer(m)) {
                        messageQueue.poll();
                    }
                }
            });
        }
    }

    /**
     * Method to create a file name from a Date
     *
     * @param d date to create the file name from
     * @return the date as a filename-compatible string
     */
    private static String formatDateIntoFile(Date d) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FILE_FORMAT);
        return dateFormat.format(d);
    }

    /**
     * Creates the log file, and returns the status
     * Returns false if anything went wrong, but already throws the logic exception
     *
     * @return the status of the operation - true if it succeeded, false if it didn't
     */
    private boolean createNewLogFile() {
        if ((!outputDirectory.exists() && !outputDirectory.mkdirs()) || !outputDirectory.isDirectory()) {
            new RobobuggyLogicNotification("Output directory path isn't a folder!", RobobuggyMessageLevel.EXCEPTION);
            return false;
        }

        Date logCreationDate = new Date();


        outputDirectory = new File(RobobuggyConfigFile.LOG_FILE_LOCATION + "/" + formatDateIntoFile(logCreationDate));

        if (!outputDirectory.mkdirs()) {
            new RobobuggyLogicNotification("Couldn't create log folder!", RobobuggyMessageLevel.EXCEPTION);
            return false;
        }

        // each log file is called {filename}_{date}.txt
        outputFile = new File(outputDirectory.getPath() + "/" +
                RobobuggyConfigFile.LOG_FILE_NAME + "_" +
                formatDateIntoFile(logCreationDate) + ".txt")
        ;
        try {
            if (!outputFile.createNewFile()) {
                new RobobuggyLogicNotification("Couldn't create log file!", RobobuggyMessageLevel.EXCEPTION);
                return false;
            }
        } catch (IOException e) {
            new RobobuggyLogicNotification("Error reading the filesystem!", RobobuggyMessageLevel.EXCEPTION);
            return false;
        }

        //everything succeeded!
        return true;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean startDecoratorNode() {
        loggingButtonPub = new Publisher(NodeChannel.GUI_LOGGING_BUTTON.getMsgPath());

        new Subscriber("log", Gui.GuiPubSubTopics.GUI_LOG_BUTTON_UPDATED.toString(), new MessageListener() {
            @Override
            public void actionPerformed(String topicName, Message m) {
                loggingButtonPub.publish(m);
            }
        });

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean shutdownDecoratorNode() {
        return true;
    }


    /**
     * LogWriterThread - where we actually process each message and write it to the file
     */
    private class LogWriterThread extends Thread {

        private PrintStream fileWriteStream;
        private Gson messageTranslator;

        private int imuHits = 0;
        private int encoderHits = 0;
        private int gpsHits = 0;
        private int brakeHits = 0;
        private int fingerprintHits = 0;
        private int steeringHits = 0;
        private int logicNotificationHits = 0;
        private int logButtonHits = 0;
        private int imageHits = 0;
        private int poseMessageHits = 0;
        private int resetHits = 0;
        private int stateHits = 0;

        private String name = "\"name\": \"Robobuggy Data Logs\",";
        private String schemaVersion = "\"schema_version\": 1.1,";
        private String dateRecorded = "\"date_recorded\": \"" +
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()) + "\",";
        private String swVersion = "\"software_version\": \"" + RobobuggyConfigFile.ALICE_LIBRARY_VERSION + "\",";
        private String sensorDataHeader = "\"sensor_data\": [";
        private String footerDataEntry = "        {\"VERSION_ID\":\"STOP\"}";

        @Override
        public void interrupt() {
            //// TODO: 2/6/2016 add the stop message to the end of the queue, so that we parse all
            messageQueue.clear();
            printDataBreakdown();
        }

        /**
         * Instantiates a new LogWriterThread by clearing the message queue
         */
        LogWriterThread() {
        }

        @Override
        public synchronized void run() {
            try {
                fileWriteStream = new PrintStream(outputFile, "UTF-8");
                messageTranslator = new GsonBuilder()
                        .excludeFieldsWithModifiers(Modifier.TRANSIENT)
                        .serializeSpecialFloatingPointValues()
                        .create()
                ;
            } catch (FileNotFoundException | UnsupportedEncodingException e) {
                new RobobuggyLogicNotification("Error setting up the output file. Aborting logging!", RobobuggyMessageLevel.EXCEPTION);
                return;
            }

            fileWriteStream.println("{" + "\n    " + name + "\n    " + schemaVersion + "\n    " + dateRecorded
                    + "\n    " + swVersion + "\n    " + sensorDataHeader);

            //always want to log :)
            while (keepLogging) {

                //block until we have a message from the queue
                Message toSort;
                try {
                    toSort = messageQueue.take();

                    String msgAsJsonString = messageTranslator.toJson(toSort);

                    // and if you look on your right you'll see the almost-unnecessary
                    // giganti-frickin-ic telemetry block
                    if (toSort instanceof BrakeMessage) {
                        brakeHits++;
                    } else if (toSort instanceof EncoderMeasurement) {
                        encoderHits++;
                    } else if (toSort instanceof FingerPrintMessage) {
                        fingerprintHits++;
                    } else if (toSort instanceof GpsMeasurement) {
                        gpsHits++;
                    } else if (toSort instanceof GuiLoggingButtonMessage) {
                        logButtonHits++;
                    } else if (toSort instanceof ImuMeasurement) {
                        imuHits++;
                    } else if (toSort instanceof GPSPoseMessage) {
                        poseMessageHits++;
                    } else if (toSort instanceof ResetMessage) {
                        resetHits++;
                    } else if (toSort instanceof RobobuggyLogicNotificationMeasurement) {
                        logicNotificationHits++;
                    } else if (toSort instanceof StateMessage) {
                        stateHits++;
                    } else if (toSort instanceof SteeringMeasurement) {
                        steeringHits++;
                    } else if (toSort instanceof ImageMessage) {
                        imageHits++;
                    }

                    fileWriteStream.println("        " + msgAsJsonString + ",");
                    fileWriteStream.flush();

                } catch (InterruptedException e) {
                    //flush all the messages that came after the stop button
                    messageQueue.clear();
                    //note level since this is expected behavior
                    new RobobuggyLogicNotification("Logging was interrupted, exiting logging thread!", RobobuggyMessageLevel.NOTE);
                }
            }
        }

        private synchronized void printDataBreakdown() {
            //we've stopped logging
            JsonObject dataBreakdown = new JsonObject();
            dataBreakdown.addProperty(NodeChannel.GUI_LOGGING_BUTTON.getName(), logButtonHits);
            dataBreakdown.addProperty(NodeChannel.GPS.getName(), gpsHits);
            dataBreakdown.addProperty(NodeChannel.IMU.getName(), imuHits);
            dataBreakdown.addProperty(NodeChannel.ENCODER.getName(), encoderHits);
            dataBreakdown.addProperty(NodeChannel.BRAKE_STATE.getName(), brakeHits);
            dataBreakdown.addProperty(NodeChannel.STEERING.getName(), steeringHits);
            dataBreakdown.addProperty(NodeChannel.FP_HASH.getName(), fingerprintHits);
            dataBreakdown.addProperty(NodeChannel.LOGIC_NOTIFICATION.getName(), logicNotificationHits);
            dataBreakdown.addProperty(NodeChannel.PUSHBAR_CAMERA.getName(), imageHits);
            dataBreakdown.addProperty(NodeChannel.POSE.getName(), poseMessageHits);
            dataBreakdown.addProperty(NodeChannel.RESET.getName(), resetHits);
            dataBreakdown.addProperty(NodeChannel.STATE.getName(), stateHits);

            fileWriteStream.println(footerDataEntry);
            fileWriteStream.println("    ],\n    \"data_breakdown\" : " + dataBreakdown.toString() + "\n}");
            fileWriteStream.close();
            new RobobuggyLogicNotification("Finished writing to file", RobobuggyMessageLevel.NOTE);
        }
    }

}
