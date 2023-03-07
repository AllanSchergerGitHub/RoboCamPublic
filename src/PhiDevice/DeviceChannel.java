/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PhiDevice;

import com.phidget22.*;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

/**
 * @author pc
 */
public class DeviceChannel {
    Phidget mPhidget;
    private boolean mIsAttached = false;
    private boolean mIsOpenning = false;
    private String mLabel;//it is a easy non-unique indentification marker
    private String mName;

    public static abstract class ChannelListener {
        public abstract void onPropertyChange(DeviceChannel dc, String propertyName, Object value);
    }

    private ArrayList<ChannelListener> mChannelListeners = new ArrayList<>();

    public DeviceChannel(Phidget phidget) {
        mPhidget = phidget;
        try {
            mName = String.format("%d - %s - %s - %s - %sHub Port %d",
                    mPhidget.getDeviceSerialNumber(), mPhidget.getDeviceSKU(),
                    mPhidget.getDeviceName(), mPhidget.getChannelName(),
                    mPhidget.getChannel() > 0 ? String.format("Channel-%d - ", mPhidget.getChannel()) : "",
                    mPhidget.getHubPort());
        } catch (PhidgetException ex) {
        }
        /*mPhidget.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void onPropertyChange(PropertyChangeEvent pce) {
                if (mName.contains("Digital Input") || true) {
                  System.out.println(pce.getPropertyName() + " property is changed");
                }
             }
        });*/
        mPhidget.addAttachListener(new AttachListener() {
            @Override
            public void onAttach(AttachEvent ae) {
                mIsAttached = true;
                //System.out.println("DeviceChannel.java: "+getName() + " is attached");
                fireChannelListenersForProperty("attached", true);
                try {
                    sleep(10);
                } catch (InterruptedException ex) {
                    Logger.getLogger(DeviceChannel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        mPhidget.addDetachListener(new DetachListener() {
            @Override
            public void onDetach(DetachEvent de) {
                mIsAttached = false;
                System.out.println(getChannelName() + " is dettached");
                fireChannelListenersForProperty("attached", false);
            }
        });
        mPhidget.addErrorListener(new ErrorListener() {
            @Override
            public void onError(ErrorEvent ee) {
                //System.out.println(ee.toString());
                //System.err.println("add if statement here to highlight 'MOTOR_STALL_CONDITION'? may be due to disconnected BLDC motor encoder wires or blown fuses or is Phidget Control Panel open (it must be closed)?");
            }
        });
    }

    public void setLabel(String label) {
        mLabel = label;
    }

    public String getLabel() {
        return mLabel;
    }

    public void addChannelListener(ChannelListener listenr) {
        if (mChannelListeners.indexOf(listenr) < 0) {
            mChannelListeners.add(listenr);
        }
    }

    public void fireChannelListenersForProperty(String propertyName, Object value) {
        for (ChannelListener listener : mChannelListeners) {
            listener.onPropertyChange(this, propertyName, value);
        }
    }

    public String getChannelName() {
        try {
            return mPhidget.getChannelName();
        } catch (PhidgetException ex) {
            return "";
        }
    }

    public boolean isOpen() {
        return mIsAttached;
    }

    public void open() {
        if (mIsAttached || mIsOpenning) return;
        mIsOpenning = true;
        try {
            mPhidget.open(5000);
        } catch (PhidgetException ex) {
            //System.err.println("Check that Stepper motors have power - are they plugged in?");
            try {
                sleep(8000);
            } catch (InterruptedException ex1) {
                //Logger.getLogger(DeviceChannel.class.getName()).log(Level.SEVERE, null, ex1);
            }
            //Logger.getLogger(DeviceChannel.class.getName()).log(Level.SEVERE, null, ex);
            mIsOpenning = false;
        }
    }

    public void close() {
        try {
            mPhidget.close();
        } catch (PhidgetException ex) {
            //Logger.getLogger(DeviceChannel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public String toString() {
        return getName();
    }

    public String getName() {
        return mName;
    }

    public BLDCMotor getBLDCMotor() {
        if (mPhidget instanceof BLDCMotor) {
            return (BLDCMotor) mPhidget;
        }
        return null;
    }


    public Encoder getEncoder() {
        if (mPhidget instanceof Encoder) {
            return (Encoder) mPhidget;
        }
        return null;
    }

    public DCMotor getDCMotor() {
        if (mPhidget instanceof DCMotor) {
            return (DCMotor) mPhidget;
        }
        return null;
    }

    public MotorPositionController getMotorPos() {
        if (mPhidget instanceof MotorPositionController) {
            return (MotorPositionController) mPhidget;
        }
        return null;
    }

    public Stepper getStepper() {
        if (mPhidget instanceof Stepper) {
            return (Stepper) mPhidget;
        }
        return null;
    }

    public DigitalInput getDigitalInput() {
        if (mPhidget instanceof DigitalInput) {
            return (DigitalInput) mPhidget;
        }
        return null;
    }
}
