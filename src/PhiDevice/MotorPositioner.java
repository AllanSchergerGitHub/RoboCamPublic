package PhiDevice;

import Utility.MinMax;
import com.phidget22.MotorPositionController;
import com.phidget22.MotorPositionControllerDutyCycleUpdateEvent;
import com.phidget22.MotorPositionControllerDutyCycleUpdateListener;
import com.phidget22.PhidgetException;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MotorPositioner {
    private MotorPositionController mMotorPositionController;
    private DeviceChannel mDeviceChannel;
    private boolean mCanEngage = true;
    private MinMax mTargetLimit;

    public static interface DutyCycleListener {
        public void onUpdate(MotorPositioner positioner,
                             MotorPositionControllerDutyCycleUpdateEvent mpcdc);
    }

    public MotorPositioner(MotorPositionController controller, DeviceChannel deviceChannel) {
        mMotorPositionController = controller;
        mDeviceChannel = deviceChannel;
    }

    public MotorPositionController getController() {
        return mMotorPositionController;
    }

    public DeviceChannel getDeviceChannel() {
        return mDeviceChannel;
    }

    public String getTargetPositionLimit() {
        if (mTargetLimit == null) {
            return "";
        }
        return String.format(
                "Min:%.2f, Max:%.2f",
                mTargetLimit.getMinValue(), mTargetLimit.getMaxValue()
        );
    }

    public void copyTargetPositionAsLimit() {
        if (mTargetLimit == null) {
            mTargetLimit = new MinMax();
        }
        try {
            mTargetLimit.setValue(mMotorPositionController.getPosition());
        } catch (PhidgetException ex) {
            Logger.getLogger(MotorPositioner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setTargetPosition(double position) {
        if (mTargetLimit != null) {
            if (position > mTargetLimit.getMaxValue()) {
                position = mTargetLimit.getMaxValue();
                System.err.println("IS THIS CAUSING AN ERROR WITH THE BLDC MOTORS?");
            } else if (position < mTargetLimit.getMinValue()) {
                position = mTargetLimit.getMinValue();
                System.err.println("IS THIS CAUSING AN ERROR WITH THE BLDC MOTORS?");
            }
        }
        try {
            mMotorPositionController.setTargetPosition(position);
        } catch (PhidgetException ex) {
            System.err.println("can't set TargetPosition ERROR - see detailed message in code");
//            +"PhidgetException 0x34 (Device not Attached)\n" +
//            "This can happen for a number of common reasons. Be sure you are opening the channel before trying to use it. "
//            + "If you are opening the channel, the program may not be waiting for the channel to be attached. If possible "
//            + "use openWaitForAttachment. Otherwise, be sure to check the Attached property of the channel before trying to use it.\n");
//            //Logger.getLogger(MotorPositioner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setCanEngage(boolean value) {
        mCanEngage = value;
    }

    public void setEngaged(boolean value) {
        if (!mCanEngage) return;
        try {
            mMotorPositionController.setEngaged(value);
        } catch (PhidgetException ex) {
            System.err.println("trying '_s_etEngaged'; there may be a problem with the physical device - check the connections and fuses ");
            Logger.getLogger(MotorPositionControllerList.class.getName()).log(Level.SEVERE, null, ex);
        }
        mDeviceChannel.fireChannelListenersForProperty("engaged", value);
    }

    public void setEngagedForced(boolean value) {
        try {
            mMotorPositionController.setEngaged(value);
        } catch (PhidgetException ex) {
            Logger.getLogger(MotorPositioner.class.getName()).log(Level.SEVERE, null, ex);
        }
        mDeviceChannel.fireChannelListenersForProperty("engaged", value);
    }

    private void fireDutryCycleListener(DutyCycleListener listener,
                                        MotorPositionControllerDutyCycleUpdateEvent mpcdc) {
        listener.onUpdate(this, mpcdc);
    }

    public void addDutyCycleListener(DutyCycleListener listener) {
        mMotorPositionController.addDutyCycleUpdateListener(new MotorPositionControllerDutyCycleUpdateListener() {
            @Override
            public void onDutyCycleUpdate(MotorPositionControllerDutyCycleUpdateEvent mpcdc) {
                fireDutryCycleListener(listener, mpcdc);
            }
        });
    }
}
