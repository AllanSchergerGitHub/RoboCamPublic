package RoboCam;

public class WheelDeviceUpdateTask implements Runnable {
    private WheelDevice mWheelDevice;
    private String mParamName = "";

    public WheelDeviceUpdateTask(WheelDevice wheelDevice) {
        mWheelDevice = wheelDevice;
    }
    
    public WheelDeviceUpdateTask(WheelDevice wheelDevice, String paramName) {
        mWheelDevice = wheelDevice;
        mParamName = paramName;
    }

    @Override
    public void run() {
        //System.out.println("paramName " + mParamName);
        if (mParamName.contains(".Name")) {
            //mWheelDevice.disengageDevices();
            mWheelDevice.detectDevices(false);
        }
    }
}