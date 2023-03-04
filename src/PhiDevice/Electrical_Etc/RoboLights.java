/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PhiDevice.Electrical_Etc;

import com.phidget22.*;

import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

/**
 * @author pc
 */
public final class RoboLights {
    private DigitalOutput ch1 = null;
    private DigitalOutput digitalOutput1 = null;

    public void connectLight() {
        try {
            ch1 = new DigitalOutput();
        } catch (PhidgetException ex) {
            Logger.getLogger(RoboLights.class.getName()).log(Level.SEVERE, null, ex);
        }
        ch1.addAttachListener(new AttachListener() {
            public void onAttach(AttachEvent ae) {
                digitalOutput1 = (DigitalOutput) ae.getSource();
                try {
                    if (digitalOutput1.getDeviceClass() != DeviceClass.VINT) {
                        //System.out.println("channel " + digitalOutput1.getChannel() + " on device " + digitalOutput1.getDeviceSerialNumber() + " attached");
                    } else {
                        //System.out.println("channel " + digitalOutput1.getChannel() + " on device " + digitalOutput1.getDeviceSerialNumber() + " hub port " + digitalOutput1.getHubPort() + " attached");
                    }
                } catch (PhidgetException ex) {
                    System.out.println("connectLight()/ch1.addAttachListener " + ex.getDescription());
                }
            }
        });

        ch1.addErrorListener(new ErrorListener() {
                                 public void onError(ErrorEvent ee) {
                                     System.out.println("ErrorIn4xDevice for clipperhead: " + ee.getDescription());
                                 }
                             }
        );

        try {
            ch1.setDeviceSerialNumber(495959);
            ch1.setHubPort(4);
            ch1.setChannel(1);
            ch1.open(5000);
            digitalOutput1.setDutyCycle(0);
            //System.out.println("Light digitalOutput1.getDutyCycle() "+ch1.getDutyCycle());
            try {
                sleep(1);
                //ch1.close();
            } catch (InterruptedException ex) {
                Logger.getLogger(RoboLights.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (PhidgetException ex) {
            System.out.println("connectLight()/addErrorListner (need to set deviceserialnumber, hubport, channel here: " + ex.getDescription());
        }
    }

    /**
     * Sets the Duty Cycle via the 4x controller (on/off switches).  Currently used primarily for turning the light on or off.
     */
    public void turnLightOnOff(double setDutyTo) {
        try {
            ch1.open(5000);
            ch1.setDutyCycle(setDutyTo);
            //System.out.println("digitalOutput1.getDutyCycle() "+ch1.getDutyCycle());
            try {
                sleep(1);
            } catch (InterruptedException ex) {
                Logger.getLogger(RoboLights.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (PhidgetException ex) {
            System.out.println("turnLightOnOff error " + ex.getDescription());
        }
    }
}
