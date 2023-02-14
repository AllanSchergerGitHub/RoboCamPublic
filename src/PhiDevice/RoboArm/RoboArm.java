/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PhiDevice.RoboArm;
import com.phidget22.AttachEvent;
import com.phidget22.AttachListener;
import com.phidget22.DetachEvent;
import com.phidget22.DetachListener;
import com.phidget22.DeviceClass;
import com.phidget22.DigitalOutput;
import com.phidget22.DistanceSensor;
import com.phidget22.DistanceSensorDistanceChangeEvent;
import com.phidget22.DistanceSensorDistanceChangeListener;
import com.phidget22.DistanceSensorSonarReflectionsUpdateEvent;
import com.phidget22.DistanceSensorSonarReflectionsUpdateListener;
import com.phidget22.Encoder;
import com.phidget22.ErrorEvent;
import com.phidget22.ErrorListener;
import com.phidget22.LogLevel;
import com.phidget22.MotorPositionController;
import com.phidget22.MotorPositionControllerDutyCycleUpdateEvent;
import com.phidget22.MotorPositionControllerDutyCycleUpdateListener;
import com.phidget22.MotorPositionControllerPositionChangeListener;
import com.phidget22.PhidgetException;
import com.phidget22.RCServo;
import static java.lang.Thread.sleep;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pc
 */
public final class RoboArm {

    // Base joint
    // RoboArm0Updown <-arm attached to base (has ability to change it's length)
    // joint1 <-between Arm0 and weeder (produces rotation up or down of weeder head)
    
    DigitalOutput ch1 = null; // to run the weed clipper head
    DigitalOutput digitalOutput1 = null;
    
    int channelNumber = 0;
    DigitalOutput[] chUpDown0 = new DigitalOutput[4]; // to run the weed clipper head
    DigitalOutput[] digitalOutputUpDown0 = new DigitalOutput[4];

    MotorPositionController chBaseJoint = null;
    double priorMaxDuty = 0;
    
    RCServo chRCServo1 = null;
    RCServo phidRCServo = null;
    
    DistanceSensor chDistanceSensor1 = null;
    double distanceSensor1_value = 0;
            
    boolean errorSignal = false;
    
    public void RoboArm(){
        // do not delete this - it doesn't do anything?? but deleting it causes pain! (July 18 2018)
    }
    
    public void connectBaseJoint() throws PhidgetException, InterruptedException{
        chBaseJoint = new MotorPositionController();
                
        chBaseJoint.addAttachListener(new AttachListener() {
			public void onAttach(AttachEvent ae) {
				MotorPositionController phid = (MotorPositionController) ae.getSource();
				try {
					if(phid.getDeviceClass() != DeviceClass.VINT){
						System.out.println("channelx1 " + phid.getChannel() + " on device " + phid.getDeviceSerialNumber() + " attached");
					}
					else{
						System.out.println("channelx " + phid.getChannel() + " on device " + phid.getDeviceSerialNumber() + " hub port " + phid.getHubPort() + " attached");
					}
				} catch (PhidgetException ex) {
					System.out.println(ex.getDescription());
				}
			}
        });

        chBaseJoint.addDutyCycleUpdateListener(new MotorPositionControllerDutyCycleUpdateListener() {
        public void onDutyCycleUpdate(MotorPositionControllerDutyCycleUpdateEvent dc) {
				MotorPositionController phid2 = (MotorPositionController) dc.getSource();
				try {
			
                                    double currentDuty=phid2.getDutyCycle();
                                    if(currentDuty<priorMaxDuty){
                                        //System.out.println("new Max backdraft?: " + currentDuty+" current limit " +phid2.getCurrentLimit());
                                        priorMaxDuty=currentDuty;
                                    }
						
			
				} catch (PhidgetException ex) {
					System.out.println("duty change listener error "+ex.getDescription());
				}
			}
                });
        
        chBaseJoint.addErrorListener(new ErrorListener() {
			public void onError(ErrorEvent ee) {
				System.out.println("Error chBaseJoint: " + ee.getDescription());
			}
		});
        try {
                //System.out.println("chBaseJoint connect started  ");
                chBaseJoint.setDeviceSerialNumber(527307);              
                chBaseJoint.setHubPort(2);
                chBaseJoint.open(5000);
                chBaseJoint.setEngaged(true);
                chBaseJoint.setAcceleration(100);
                chBaseJoint.setVelocityLimit(800);
                chBaseJoint.setTargetPosition(18);

                sleep(200);
                //System.out.println("chBaseJoint.getPosition() "+chBaseJoint.getPosition());
                
                sleep(1);
                } catch (PhidgetException ex) {
                System.out.println("errorchBaseJoint "+ex.getDescription());
            }
    }
    
    public void connectUpDownArm(int channelNumber)throws Exception {
        System.out.println("+channelNumber " +channelNumber);
        chUpDown0[channelNumber] = new DigitalOutput();
        chUpDown0[channelNumber].addAttachListener(new AttachListener() {
			public void onAttach(AttachEvent ae) {
				digitalOutputUpDown0[channelNumber] = (DigitalOutput) ae.getSource();
				try {
					if(digitalOutputUpDown0[channelNumber].getDeviceClass() != DeviceClass.VINT){
						System.out.println("connectUpDownArmchannel "+channelNumber+" " + digitalOutputUpDown0[channelNumber].getChannel() + " on device " + digitalOutputUpDown0[channelNumber].getDeviceSerialNumber() + " attached");
					}
					else{
						System.out.println("connectUpDownArmB_channel " +channelNumber+" " + digitalOutputUpDown0[channelNumber].getChannel() + " on device " + digitalOutputUpDown0[channelNumber].getDeviceSerialNumber() + " hub port " + digitalOutputUpDown0[channelNumber].getHubPort() + " attached");
					}
				} catch (PhidgetException ex) {
					System.out.println(ex.getDescription());
				}
			}
        });
        
        chUpDown0[channelNumber].addErrorListener(new ErrorListener() {
			public void onError(ErrorEvent ee) {
				System.out.println("connectUpDownArm Error for : " + ee.getDescription());
			}
        }
        );
        
        try {
                chUpDown0[channelNumber].setDeviceSerialNumber(527307);
                chUpDown0[channelNumber].setHubPort(5);
                chUpDown0[channelNumber].setChannel(channelNumber);
                chUpDown0[channelNumber].open(5000);
                System.out.println("connectUpDownArm getDutyCycle() channelNumber: "+channelNumber+" "+chUpDown0[channelNumber].getDutyCycle());
                sleep(1);
                chUpDown0[channelNumber].close();
                } catch (PhidgetException ex) {
                System.out.println(ex.getDescription());
            }
        }
    
    
    public void connectWeedClipperHead()throws Exception {
        ch1 = new DigitalOutput();
        ch1.addAttachListener(new AttachListener() {
			public void onAttach(AttachEvent ae) {
				digitalOutput1 = (DigitalOutput) ae.getSource();
				try {
					if(digitalOutput1.getDeviceClass() != DeviceClass.VINT){
						//System.out.println("channel " + digitalOutput1.getChannel() + " on device " + digitalOutput1.getDeviceSerialNumber() + " attached");
					}
					else{
						//System.out.println("channel " + digitalOutput1.getChannel() + " on device " + digitalOutput1.getDeviceSerialNumber() + " hub port " + digitalOutput1.getHubPort() + " attached");
					}
				} catch (PhidgetException ex) {
					System.out.println(ex.getDescription());
				}
			}
        });
        
        ch1.addErrorListener(new ErrorListener() {
			public void onError(ErrorEvent ee) {
				System.out.println("ErrorIn16xDevice for clipperhead: " + ee.getDescription());
			}
        }
        );
        
        try {             
                ch1.setDeviceSerialNumber(52730007);              
                ch1.setHubPort(3);
                ch1.open(5000);
                digitalOutput1.setDutyCycle(0);
                System.out.println("WeedClipperHead digitalOutput1.getDutyCycle() "+ch1.getDutyCycle());
                sleep(1);
                //ch1.close();
                } catch (PhidgetException ex) {
                System.out.println(ex.getDescription());
            }
        }
    
    /**
     * Sets the motor position for the weeder base unit - this is a DC motor.
     */
    public void SetBaseMotorPosition(double setPositionTo) {
                try {
                chBaseJoint.open(5000);
                chBaseJoint.setEngaged(true);
//                chBaseJoint.setAcceleration(100);
//                chBaseJoint.setVelocityLimit(800);
                chBaseJoint.setTargetPosition(setPositionTo);
                    try {
                        //System.out.println("chBaseJoint.getPosition: "+chBaseJoint.getPosition());
                        sleep(1);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(RoboArm.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } catch (PhidgetException ex) {
                System.out.println(ex.getDescription());
            }
                
    }
    
    public void setUpDownPosition(double setUpDownPositionTo, double waitTime, String UpDown) throws PhidgetException, InterruptedException{
        
        int[] channelNumberArray = {0,3}; // these two channels control the on/off power
        for(int channelNumber2 : channelNumberArray){
            //System.out.println("x "+channelNumber);
            channelNumber = channelNumber2;
            chUpDown0[channelNumber].open(5000);
            sleep(20);
            
                if(chUpDown0[channelNumber].getDutyCycle()>0){
                    System.err.println("CHANNEL WAS ON");
                }
            // turn off the power (it most likley will already be off but if we change directions before it reaches the end of a cycle it could still be on)
            
            chUpDown0[channelNumber].setDutyCycle(0);
            }
        
        // wait a brief period
        sleep(100);
        
        // set the switches so power flows the right direction
        int[] channelNumber2Array = {1,2}; // these two channels control the on/off power
        for(int channelNumber2 : channelNumber2Array){
            //System.out.println("x "+channelNumber);
            channelNumber = channelNumber2;
            chUpDown0[channelNumber].open(5000);
            sleep(20);
            
            // set the direction of the relays so power flows the right way
            // need some kind of if then here to show if we go to zero or one??
            if(UpDown.equals("Up")){
//                System.out.println("Going UP");
                if(channelNumber2==1){
                    chUpDown0[channelNumber].setDutyCycle(0);
                }
                else
                if(channelNumber2==2 && chUpDown0[1].getDutyCycle()==0){
                    chUpDown0[channelNumber].setDutyCycle(0);
                }
                else
                {
                    errorSignal=true;
                    System.err.println("some kind of problem --------------------------------------------- ");
                }
                }
            else
                if(UpDown.equals("Down")){
//                System.out.println("Going Down");
                if(channelNumber2==1){
                    chUpDown0[channelNumber].setDutyCycle(1);
                }
                else
                if(channelNumber2==2 && chUpDown0[1].getDutyCycle()==1){
                    chUpDown0[channelNumber].setDutyCycle(1);
                }
                else
                {
                    errorSignal=true;
                    System.err.println("some kind of problem --------------------------------------------- ");
                }
                }
            else
                {
                    errorSignal=true;
                    System.err.println("Error No Value Provided");
                    //chUpDown0[channelNumber].setDutyCycle(0);
                }
            }
        // wait a brief period
        sleep(100);
        
        if(waitTime >1500){
                    errorSignal=true;
                    System.err.println("some kind of problem (over 1500??) --------------------------------------------- ");            
        }
        
        // turn on the power 
        
        if (errorSignal) {
            System.err.println("not going to turn on the relays due to erro -----------------");
            errorSignal = false;
        }
        else
        {
        for(int channelNumber2 : channelNumberArray){
            //System.out.println("x "+channelNumber);
            channelNumber = channelNumber2;
            chUpDown0[channelNumber].open(5000);
            sleep(20);
            
                if(chUpDown0[channelNumber].getDutyCycle()>0){
                    System.err.println("CHANNEL WAS ON error 2");
                }
            // turn off the power (it most likley will already be off but if we change directions before it reaches the end of a cycle it could still be on)
            
            chUpDown0[channelNumber].setDutyCycle(1);
            }
        
        }

        // wait a brief period for the right amount of time so the arm moves the desired distance
        //System.out.println((long)waitTime);
        sleep((long)waitTime);
        
        // turn off the power
        for(int channelNumber2 : channelNumberArray){
            //System.out.println("x "+channelNumber);
            channelNumber = channelNumber2;
            chUpDown0[channelNumber].open(5000);
            sleep(20);
            
                if(chUpDown0[channelNumber].getDutyCycle()>0){
//                    System.err.println("CHANNEL WAS Working as designed");
                }
            // turn off the power (it most likley will already be off but if we change directions before it reaches the end of a cycle it could still be on)
            
            chUpDown0[channelNumber].setDutyCycle(0);
            }
        
        
           //System.out.println("value now "+setUpDownPositionTo+" on channelNumber: "+channelNumber);
    }

    public double distanceSensor1() throws PhidgetException{
        double distance = distanceSensorOne();
        return distance;
        }
    
    private double distanceSensorOne() throws PhidgetException{
        
        double distance = chDistanceSensor1.getDistance();
        return distance;
    }
    
    public void connectDistanceSensorOne() throws PhidgetException {
        
        chDistanceSensor1 = new DistanceSensor();

        chDistanceSensor1.addAttachListener(new AttachListener() {
			public void onAttach(AttachEvent ae) {
				DistanceSensor phid = (DistanceSensor) ae.getSource();
				try {
					if(phid.getDeviceClass() != DeviceClass.VINT){
						System.out.println("channel " + phid.getChannel() + " on device " + phid.getDeviceSerialNumber() + " attached");
					}
					else{
						System.out.println("channel " + phid.getChannel() + " on device " + phid.getDeviceSerialNumber() + " hub port " + phid.getHubPort() + " attached");
					}
				} catch (PhidgetException ex) {
					System.out.println(ex.getDescription());
				}
			}
        });
        
        chDistanceSensor1.addDetachListener(new DetachListener() {
			public void onDetach(DetachEvent de) {
				DistanceSensor phid = (DistanceSensor) de.getSource();
				try {
					if (phid.getDeviceClass() != DeviceClass.VINT) {
						System.out.println("channel " + phid.getChannel() + " on device " + phid.getDeviceSerialNumber() + " detached");
					} else {
						System.out.println("channel " + phid.getChannel() + " on device " + phid.getDeviceSerialNumber() + " hub port " + phid.getHubPort() + " detached");
					}
				} catch (PhidgetException ex) {
					System.out.println(ex.getDescription());
				}
			}
        });
                
        chDistanceSensor1.addErrorListener(new ErrorListener() {
			public void onError(ErrorEvent ee) {
				System.out.println("Error: " + ee.getDescription());
			}
        });

        chDistanceSensor1.addDistanceChangeListener(new DistanceSensorDistanceChangeListener() {
			public void onDistanceChange(DistanceSensorDistanceChangeEvent e) {
				distanceSensor1_value = e.getDistance();
                                //System.out.println("Distance Changed: " + distanceSensor1_value );
                                
			}
        });
        
        chDistanceSensor1.addSonarReflectionsUpdateListener(new DistanceSensorSonarReflectionsUpdateListener() {
			public void onSonarReflectionsUpdate(DistanceSensorSonarReflectionsUpdateEvent e) {
				System.out.println("Sonar Reflections Update");
				System.out.printf(" DST  |  AMPL \n");
				int[] distances = e.getDistances();
				int[] amplitudes = e.getAmplitudes();
				for(int i = 0; i < e.getCount(); i++){
					System.out.printf("%5d | %5d\n", distances[i], amplitudes[i]);
				}
				System.out.println("");
			}
        });
        try {
            chDistanceSensor1.setDeviceSerialNumber(527307);
            chDistanceSensor1.setHubPort(0);
            chDistanceSensor1.setChannel(0);
            chDistanceSensor1.open(5000);
            
        } catch (PhidgetException ex) {
            System.err.println("ERROR HERE??????? sonor distance connecting -------------------------------------");
            System.err.println(ex.getDescription());
        }
//        double x = distanceSensor1_value;
//        return x;
    }
    
    public void connectRCServo1() throws PhidgetException, InterruptedException {
        chRCServo1 = new RCServo();
        chRCServo1.addAttachListener(new AttachListener() {
			public void onAttach(AttachEvent ae) {
				phidRCServo = (RCServo) ae.getSource();
				try {
					if(phidRCServo.getDeviceClass() != DeviceClass.VINT){
						System.out.println("channel " + phidRCServo.getChannel() + " on device " + phidRCServo.getDeviceSerialNumber() + " attached");
					}
					else{
						System.out.println("channel " + phidRCServo.getChannel() + " on device " + phidRCServo.getDeviceSerialNumber() + " hub port " + phidRCServo.getHubPort() + " attached");
					}
				} catch (PhidgetException ex) {
					System.out.println(ex.getDescription());
				}
			}
        });

      
        chRCServo1.addErrorListener(new ErrorListener() {
			public void onError(ErrorEvent ee) {
				System.out.println("chRCServo1 Error: " + ee.getDescription());
			}
        });
        
        try {
        
            chRCServo1.setDeviceSerialNumber(527307);
//            System.out.println("1chRCServo1.getDeviceSerialNumber() : "+chRCServo1.getDeviceSerialNumber());
            chRCServo1.setHubPort(4);
//            System.out.println("2chRCServo1.getHubPort() : "+chRCServo1.getHubPort());
            chRCServo1.setChannel(0);
//            System.out.println("3chRCServo1.getChannel() : "+chRCServo1.getChannel());

            chRCServo1.open(5000);
            sleep(500);
//            System.out.println("3chRCServo1.getAttached() : "+chRCServo1.getAttached());
//            chRCServo1.
            chRCServo1.setTargetPosition(44.3);
//            System.out.println("4chRCServo1.");
            chRCServo1.setEngaged(true);
//            System.out.println("5chRCServo1.getEngaged() : "+chRCServo1.getEngaged());
            chRCServo1.setTargetPosition(44.3);
            Thread.sleep(200);
//            System.out.println("6chRCServo1.getPosition() : "+chRCServo1.getPosition());
            chRCServo1.setTargetPosition(48.6);
            Thread.sleep(600);
            chRCServo1.setTargetPosition(52);
            Thread.sleep(500);
           // chRCServo1.close();
            
        } catch (PhidgetException ex) {
            System.out.println(ex.getDescription());
        }

    }
    
    
    /**
     * Sets the position of the server on the cutter head 
     */
    public void RFESetServo1Position(double pos) throws InterruptedException, Exception{
                try {
                System.out.println("chRCServo1.getPosition: "+pos);
                    
                chRCServo1.open(5000);
                //chRCServo1.setEngaged(true);
                chRCServo1.setTargetPosition(pos);
                System.out.println("chRCServo1.getPosition: "+chRCServo1.getPosition());
                sleep(1);
                } catch (PhidgetException ex) {
                System.out.println("RFESetServo1Position error "+ex.getDescription());
            }
    }

    /**
     * Sets the Duty Cycle via the 16x controller (on/off switches).  Currently used primarily for turning the weeder on or off.
     */
    public void SetDutyCycleWeedClipperHead(double setDutyTo) throws InterruptedException, Exception{
                try {
                ch1.open(5000);
                ch1.setDutyCycle(setDutyTo);
                System.out.println("digitalOutput1.getDutyCycle() "+ch1.getDutyCycle());
                sleep(1);
                } catch (PhidgetException ex) {
                System.out.println(ex.getDescription());
            }
    }
    

    
}
