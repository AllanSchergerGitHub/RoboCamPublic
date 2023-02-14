/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PhiDevice;

/**
 *
 * @author sujoy
 */
public class DeviceChannelJob extends BackgroundJob {
    interface JobAction {
        public void doAction(DeviceChannel channel);
        public void afterAction();
    }

    private final String mChannelName;
    private DeviceChannel mChannel;
    private final DeviceManager mDeviceManager;
    private final JobAction mJobAction;

    public DeviceChannelJob(DeviceManager deviceManager,
                            String channelName,
                            JobAction jobAction,
                            long timeOutPeriod) {
        super(timeOutPeriod);
        mChannelName = channelName;
        mDeviceManager = deviceManager;
        mJobAction = jobAction;
    }

    @Override
    public boolean isReady() {
        if (mChannel == null) {
            mChannel = mDeviceManager.getChannelByName(mChannelName);
            if (mChannel != null && !mChannel.isOpen()) {
                System.out.println("Trying to  open " + mChannelName);
                mChannel.open();
            }
        }
        if (mChannel == null) return false;
        return mChannel.isOpen();
    }

    @Override
    public void runInBackground() {
        mJobAction.doAction(mChannel);
    }

    @Override
    public void updateGUI() {
        mJobAction.afterAction();
    }

    @Override
    public void cleanUp() {
        if (mChannel != null && mChannel.isOpen()) {
            //mChannel.close();
        }
    }

    @Override
    public void onTimeOut() {
        System.out.println("On time out");
        super.onTimeOut();
    }
}
