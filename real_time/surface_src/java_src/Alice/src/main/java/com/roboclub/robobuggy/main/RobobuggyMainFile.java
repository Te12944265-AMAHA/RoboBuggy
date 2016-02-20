package com.roboclub.robobuggy.main;

import com.roboclub.robobuggy.simulation.SensorPlayer;
import com.roboclub.robobuggy.ui.Gui;
import com.roboclub.robobuggy.utilities.JNISetup;
import gnu.io.CommPortIdentifier;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;


/** This class is the driver starting up the robobuggy program, if you want the buggy to drive itself you should run this node */
public class RobobuggyMainFile {
    
    /**
	 * Run Alice
	 * @param args : None
	 */
    public static void main(String[] args) {

        try {
			JNISetup.setupJNI(); //must run for jni to install
			//note that errors are just printed to the console since the gui and logging system  has not been created yet
		} catch (NoSuchFieldException e1) {
			e1.printStackTrace();
		} catch (SecurityException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IllegalArgumentException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IllegalAccessException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
   

        List<String> ports = getAvailablePorts();
        System.out.println(ports);
        Robot.getInstance();
        Gui.getInstance();

     	
    	if (RobobuggyConfigFile.DATA_PLAY_BACK) {
    		//Play back mode enabled
    		new SensorPlayer("logs/2016-02-19-23-08-24/sensors_2016-02-19-23-08-24.txt", 1);
        }
        else {
        	//Play back disabled, create robot
        	Robot.getInstance().startNodes();
			new RobobuggyLogicNotification("Robobuggy Logic Notfication started", RobobuggyMessageLevel.NOTE);

            try {
                Thread.sleep(5000);

                new RobobuggyLogicNotification("Notification 1", RobobuggyMessageLevel.NOTE);
                new RobobuggyLogicNotification("Notification 2", RobobuggyMessageLevel.NOTE);

                Thread.sleep(500);

                new RobobuggyLogicNotification("Notif 3", RobobuggyMessageLevel.NOTE);
            }
            catch (InterruptedException e) {
                new RobobuggyLogicNotification("interreupted", RobobuggyMessageLevel.NOTE);
            }
		}


    }
    
    private static List<String> getAvailablePorts() {

        List<String> list = new ArrayList<String>();

        Enumeration<?> portList = CommPortIdentifier.getPortIdentifiers();

        while (portList.hasMoreElements()) {
            CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                list.add(portId.getName());
            }
        }

        return list;
    }

}