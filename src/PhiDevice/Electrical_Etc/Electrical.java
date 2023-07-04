/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PhiDevice.Electrical_Etc;

import com.phidget22.*;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author pc
 */
public class Electrical {

    CurrentInput chCurrentSensor1 = null;
    double currentSensor1_value = 0;
    double currentOverageCounter = 0; // how many times was the current over a threashold?
    double maxOverageCurrent = 0; // what was the maximum current measurement over a threashold?
//    public void Electrical(){
//        // do not delete this - it doesn't do anything?? but deleting it causes pain! (July 18 2018)
//    }

    public double currentSensor1() {
        double current = currentSensorOne();
        return current;
    }

    private double currentSensorOne() {
        try {
            currentSensor1_value = chCurrentSensor1.getCurrent();
            if (Math.abs(currentSensor1_value) > 4.9) { // 2.9 was set so only very large readings are reported - this may need to be adjusted up or down
                maxOverageCurrent = Math.max(maxOverageCurrent, Math.abs(currentSensor1_value));
                currentOverageCounter = currentOverageCounter + 1;
                System.out.println("[Current Event] -> Current is: " + -currentSensor1_value + "-----Number of times over threashold: " + String.format("%.0f", currentOverageCounter) + " maxOver: " + String.format("%.4f", maxOverageCurrent));
            }
        } catch (PhidgetException ex) {
            //System.err.println("currentSensorOne glitch? moving on anyway.");
            // Logger.getLogger(Electrical.class.getName()).log(Level.SEVERE, null, ex);
        }

        return currentSensor1_value;
    }

    public void connectCurrentSensorOne() {
        try {
            chCurrentSensor1 = new CurrentInput();
        } catch (PhidgetException ex) {
            Logger.getLogger(Electrical.class.getName()).log(Level.SEVERE, null, ex);
        }

        chCurrentSensor1.addAttachListener(new AttachListener() {
            public void onAttach(AttachEvent ae) {
                CurrentInput phid = (CurrentInput) ae.getSource();
                try {
                    if (phid.getDeviceClass() != DeviceClass.VINT) {
                        //System.out.println("channel " + phid.getChannel() + " on device " + phid.getDeviceSerialNumber() + " attached");
                    } else {
                        //System.out.println("channel " + phid.getChannel() + " on device " + phid.getDeviceSerialNumber() + " hub port " + phid.getHubPort() + " attached: " + phid.getDeviceName());
                    }
                } catch (PhidgetException ex) {
                    System.out.println("chCurrentSensor1 AttachListener error: " + ex.getDescription());
                }
            }
        });

        chCurrentSensor1.addDetachListener(new DetachListener() {
            public void onDetach(DetachEvent de) {
                CurrentInput phid = (CurrentInput) de.getSource();
                try {
                    if (phid.getDeviceClass() != DeviceClass.VINT) {
                        System.out.println("channel " + phid.getChannel() + " on device " + phid.getDeviceSerialNumber() + " detached");
                    } else {
                        System.out.println("channel " + phid.getChannel() + " on device " + phid.getDeviceSerialNumber() + " hub port " + phid.getHubPort() + " detached");
                    }
                } catch (PhidgetException ex) {
                    System.out.println("chCurrentSensor1 DetachListener error: " + ex.getDescription());
                }
            }
        });

        chCurrentSensor1.addErrorListener(new ErrorListener() {
            public void onError(ErrorEvent ee) {
                System.out.println("Error: " + ee.getDescription());
            }
        });

        /**
         * Outputs the CurrentInput's most recently detected current.
         * Fired when a CurrentInput channel with onCurrentChangeHandler registered detects a current change
         */
        /**
         chCurrentSensor1.addCurrentChangeListener(new CurrentInputCurrentChangeListener() {
         public void onCurrentChange(CurrentInputCurrentChangeEvent e) {
         //If you are unsure how to use more than one Phidget channel with this event, we recommend going to
         //www.phidgets.com/docs/Using_Multiple_Phidgets for information

         currentSensor1_value = e.getCurrent();
         System.out.println("[Current Event] -> Current: " + e.getCurrent());
         }
         });
         */

        try {
            chCurrentSensor1.setDeviceSerialNumber(495959);
            chCurrentSensor1.setHubPort(1);
            chCurrentSensor1.setChannel(0);
            chCurrentSensor1.open(5000);
        } catch (PhidgetException ex) {
            System.err.println("ERROR HERE??????? electrial current sensor connecting ------------------------------------- " + ex.getDescription());
        }
    }


}
