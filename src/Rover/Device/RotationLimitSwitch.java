package Rover.Device;

import PhiDevice.BackgroundJob;
import PhiDevice.DeviceChannel;
import PhiDevice.DeviceManager;
import com.phidget22.DigitalInput;
import com.phidget22.DigitalInputStateChangeEvent;
import com.phidget22.DigitalInputStateChangeListener;
import com.phidget22.PhidgetException;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RotationLimitSwitch {
    private DeviceManager mDeviceManager;
    private String mSwitchDeviceName;
    private DeviceChannel mSwitchDeviceChannel;
    private DigitalInput mSwitchDigitalInput;

    public interface StateChangeListener {
        public void onStateChange(RotationLimitSwitch source, boolean state);
    }

    private StateChangeListener mStateChangeListener;

    public RotationLimitSwitch(
            DeviceManager deviceManager, StateChangeListener stateChangeListener) {
        mDeviceManager = deviceManager;
        mStateChangeListener = stateChangeListener;
    }

    public boolean isDetected() {
        if (mSwitchDeviceChannel == null) return false;
        if (mSwitchDigitalInput == null) return false;
        //System.out.println(String.format("%s is detected as Limit Switch", mSwitchDeviceChannel.getName()));
        return true;
    }

    private void fireStateChangeListener() {
        if (mStateChangeListener == null) return;
        try {
            mStateChangeListener.onStateChange(this, mSwitchDigitalInput.getState());
        } catch (PhidgetException ex) {
            Logger.getLogger(RotationLimitSwitch.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getSwitchDeviceName() {
        return mSwitchDeviceName;
    }

    public boolean setSwitchDeviceName(String switchDeviceName) {
        if (switchDeviceName.length() == 0) return false;
        if (mSwitchDeviceName != null && mSwitchDeviceName.equals(switchDeviceName)) return true;
        mSwitchDeviceName = switchDeviceName;
        if (mSwitchDeviceName == null) return false;
        //System.out.println("setSwitchDeviceName: " + switchDeviceName);
        new BackgroundJob(TimeUnit.SECONDS.toMillis(60)) {
            @Override
            public boolean isReady() {
                if (isDetected()) return true;
                if (mSwitchDeviceChannel == null) {
                    mSwitchDeviceChannel =
                            mDeviceManager.getChannelByName(mSwitchDeviceName);
                }
                if (mSwitchDeviceChannel != null) {
                    mSwitchDigitalInput = mSwitchDeviceChannel.getDigitalInput();
                    mSwitchDigitalInput = mSwitchDeviceChannel.getDigitalInput();
                    try {
                        mSwitchDigitalInput.open();
                    } catch (PhidgetException ex) {
                        Logger.getLogger(RotationLimitSwitch.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                return false;
            }

            @Override
            public void runInBackground() {
                //System.out.println("runInBakground: " + mSwitchDeviceName);
                if (mSwitchDigitalInput != null) {
                    mSwitchDigitalInput.addStateChangeListener(new DigitalInputStateChangeListener() {
                        @Override
                        public void onStateChange(DigitalInputStateChangeEvent disce) {
                            fireStateChangeListener();
                        }
                    });
                    System.out.println(String.format("addStateChangeListener is added on %s", mSwitchDeviceChannel.getChannelName()));
                }
            }
        };
        return false;
    }
}
