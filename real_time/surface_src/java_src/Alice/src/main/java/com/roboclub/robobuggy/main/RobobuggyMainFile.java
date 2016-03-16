package com.roboclub.robobuggy.main;

import com.roboclub.robobuggy.robots.AbstractRobot;
import com.roboclub.robobuggy.robots.SimRobot;
import com.roboclub.robobuggy.robots.TransistorDataCollection;
import com.roboclub.robobuggy.serial.RBSerialMessage;
import com.roboclub.robobuggy.simulation.SensorPlayer;
import com.roboclub.robobuggy.ui.Gui;
import com.roboclub.robobuggy.utilities.JNISetup;

import gnu.io.CommPortIdentifier;

import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import jdk.nashorn.internal.runtime.regexp.joni.Config;


/** This class is the driver starting up the robobuggy program, if you want the buggy to drive itself you should run this node */
public class RobobuggyMainFile {
    static public AbstractRobot robot;
	
    /*
    public static getRobot(){
    	
    }
    */
    
	
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
        
    	RobobuggyConfigFile.loadConfigFile(); //TODO make sure that logic Notification is setup before this point

   

		//Initialize message headers
		RBSerialMessage.initializeHeaders();


        
     	
    	if (RobobuggyConfigFile.DATA_PLAY_BACK) {
            robot = SimRobot.getInstance();//TransistorAuton;//SimRobot.getInstance();
    		//Play back mode enabled
    		new SensorPlayer(RobobuggyConfigFile.getPLAY_BACK_SOURCE_FILE(),1);
        }else{
        	robot = TransistorDataCollection.getInstance();

        }
    	
        Gui.getInstance();

        
        	//Play back disabled, create robot
        	robot.startNodes();
			new RobobuggyLogicNotification("Robobuggy Logic Notfication started", RobobuggyMessageLevel.NOTE);

    }
    
    
    /**
     * This method will reset and reload all parameters 
     * NOTE this method does not reset the ConfigurationPanel
     * @return 
     */
    public static void resetSystem(){
    	Robot.getInstance().shutDown();
 //   	Gui.close();
 //   	Gui.getInstance();
    	Robot.getInstance();
    	//TODO make this work for real 
    	
    }
    

}