import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.roboclub.robobuggy.messages.BatteryLevelMessage;
import com.roboclub.robobuggy.messages.GPSPoseMessage;
import com.roboclub.robobuggy.messages.GpsMeasurement;
import com.roboclub.robobuggy.nodes.planners.WayPointFollowerPlanner;
import com.roboclub.robobuggy.nodes.planners.WayPointUtil;


public class TestWayPointFollowerPlanner {

	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		GPSPoseMessage zeroPose = new GPSPoseMessage(new Date(), 0.0, 0.0, 0.0);
		GPSPoseMessage onePose = new GPSPoseMessage(new Date(), 0, 1, 0);
		GPSPoseMessage aPose = new GPSPoseMessage(new Date(), 1.5, 1, 0);
		GPSPoseMessage negPose = new GPSPoseMessage(new Date(), 0, -1, 0);



		if(GPSPoseMessage.getDistance(zeroPose, zeroPose) != 0){
			fail("distance function does not respect idenity");
		}
		
		if(GPSPoseMessage.getDistance(zeroPose, onePose) != 1){
			fail("distance function does not handle some input correctly ");
		}
		
		if(Math.abs(GPSPoseMessage.getDistance(aPose, zeroPose) -  1.802) > .001){
			fail("distance function does not handle some input correctly ");
		}
		
		if(GPSPoseMessage.getDistance(onePose, aPose) != 1.5){
			fail("distance function does not handle some input correctly ");
		}
		
		if(GPSPoseMessage.getDistance(onePose, negPose) != 2.0){
			fail("distance function does not handle some input correctly ");
		}
		
		
		
		}

	}
