package com.roboclub.robobuggy.utilities;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * Class used to link the native library to the project
 */
public class JNISetup {

	/**
	 * Includes all of the jni libraries that we need to be able to use all of our libraries
	 * @return true iff the setup completes successfully
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static boolean setupJNI() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException{
	       final String PATH_TO_ADD =  "library";
	       final Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
	       usrPathsField.setAccessible(true);

	       //get array of paths
	       final String[] paths = (String[])usrPathsField.get(null);
	       //check if the path to add is already present
	       for(final String path : paths) {
	           if(path.equals(PATH_TO_ADD)) {
	               return true;
	           }
	       }

	       //add the new path
	       final String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
	       newPaths[newPaths.length-1] = PATH_TO_ADD;
	       usrPathsField.set(null, newPaths);
	       
	       return true;
	}
	
}
