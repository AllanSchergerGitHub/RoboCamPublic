/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PhiDevice.Electrical_Etc;
import com.phidget22.AttachEvent;
import com.phidget22.AttachListener;
import com.phidget22.CurrentInput;
import com.phidget22.DetachEvent;
import com.phidget22.DetachListener;
import com.phidget22.DeviceClass;
import com.phidget22.ErrorEvent;
import com.phidget22.ErrorListener;
import com.phidget22.PhidgetException;
import com.phidget22.VoltageRatioInput;
import java.util.logging.Level;
import java.util.logging.Logger;
import mySQL.MysqlLogger;

/**
 *
 * @author pc
 */
public class Potentiameters {

    //Create your Phidget channels
    VoltageRatioInput voltageRatioInput3 = null;
    double Potentiameter_value = 0;
    private boolean mAllowMysqlLogging = false;
    private String mBatch_time_stamp_into_mysql = "initialized_in_Potentiameters.java";

    public void Potentiameters(){
        // do not delete this - it doesn't do anything?? but deleting it causes pain! (July 18 2018)
    }
    
    public void set_mAllowMysalLogging(boolean AllowMysqlLogging) {
        mAllowMysqlLogging = AllowMysqlLogging;
    }

    public double getPotentiameterValue(String CallingLocation) {
        try {
            Potentiameter_value = voltageRatioInput3.getVoltageRatio();
//            if(!CallingLocation.equals("RoverFrontEndJava")){
//            System.err.println("Calling Location "+CallingLocation + " "+Potentiameter_value);
//            }
            } catch (PhidgetException ex) {
                System.err.println("PotentiameterOne glitch? moving on anyway. CallingLocation:"+CallingLocation);
                //Logger.getLogger(Electrical.class.getName()).log(Level.SEVERE, null, ex);
            }

        if  (mAllowMysqlLogging) {
               MysqlLogger.put(MysqlLogger.Type.BETTER, (float)Potentiameter_value, "Potentiameters", mBatch_time_stamp_into_mysql, "Truck", "PotentiametersOne");
            }

        return Potentiameter_value;
    }

    public void setBatchTime(String Batch_time_stamp_into_mysql){
        mBatch_time_stamp_into_mysql = Batch_time_stamp_into_mysql;
    }

    public void connectPotentiameter(String wheelName) {
        try {
        voltageRatioInput3 = new VoltageRatioInput();
        } catch (PhidgetException ex) {
            Logger.getLogger(Potentiameters.class.getName()).log(Level.SEVERE, null, ex);
        }

        voltageRatioInput3.addAttachListener(new AttachListener() {
            public void onAttach(AttachEvent ae) {
                    VoltageRatioInput phid = (VoltageRatioInput) ae.getSource();
                    try {
                        if(phid.getDeviceClass() != DeviceClass.VINT){
                                //System.out.println("channel " + phid.getChannel() + " on device " + phid.getDeviceSerialNumber() + " attached");
                        }
                        else{
                                //System.out.println("channel " + phid.getChannel() + " on device " + phid.getDeviceSerialNumber() + " hub port " + phid.getHubPort() + " attached: " + phid.getDeviceName());
                        }
                    } catch (PhidgetException ex) {
                            System.out.println("voltageRatioInput3 AttachListener: " + ex.getDescription());
                    }
            }
        });
        
        voltageRatioInput3.addDetachListener(new DetachListener() {
            public void onDetach(DetachEvent de) {
                    VoltageRatioInput phid = (VoltageRatioInput) de.getSource();
                    try {
                        if (phid.getDeviceClass() != DeviceClass.VINT) {
                                System.out.println("channel " + phid.getChannel() + " on device " + phid.getDeviceSerialNumber() + " detached");
                        } else {
                                System.out.println("channel " + phid.getChannel() + " on device " + phid.getDeviceSerialNumber() + " hub port " + phid.getHubPort() + " detached");
                        }
                    } catch (PhidgetException ex) {
                            System.out.println("voltageRatioInput3 DetachListener: " + ex.getDescription());
                    }
            }
        });
                
        voltageRatioInput3.addErrorListener(new ErrorListener() {
            public void onError(ErrorEvent ee) {
                    System.out.println("Error: " + ee.getDescription());
            }
        });
                
        int setHubPort = 0;
        int setDeviceSerialNumber = 0;
            
        if (wheelName.equals("FrontLeft")){
            setDeviceSerialNumber = 527307;
            setHubPort = 3;
        }
        if (wheelName.equals("FrontRight")){
            setDeviceSerialNumber = 527307;
            setHubPort = 2;
        }
        try {
            voltageRatioInput3.setIsHubPortDevice(true);
            voltageRatioInput3.setHubPort(setHubPort);
            voltageRatioInput3.setDeviceSerialNumber(setDeviceSerialNumber);
            //voltageRatioInput3.setChannel(0);
            voltageRatioInput3.open(5000);
            } catch (PhidgetException ex) {
            System.err.println("ERROR HERE ------------------------------------- potentiameter connecting problem -------------------------------------");
            System.err.println("      ERROR with potentiameter at address: " + setDeviceSerialNumber + " " + setHubPort + " - update Hub and Port Here in the Code.");
            System.err.println(ex.getDescription());
            System.err.println("ERROR HERE ------------------------------------- potentiameter connecting problem -------------------------------------");
        }
    }
}
