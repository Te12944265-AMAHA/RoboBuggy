package com.roboclub.robobuggy.ui;

/**
 * graphs for the pose
 */
public class PoseGraphsPanel extends RobobuggyGUIContainer{

	/**
	 * makes the pose graphs panels
	 */
	public PoseGraphsPanel(){
		this.addComponent(new PoseViewer(), 0.0, 0.0, .5, 1.0);
	//	this.addComponent(new EncoderGraph(), .75, 0.0, .25, .25);
		this.addComponent(new EncoderGraph(), .5, 0.0, 0.5, 0.5);
		this.addComponent(new SteeringGraph(), .5, 0.5, 0.5, 0.5);
	
		
	}
}
