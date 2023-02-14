package RoverUI;

import PhiDevice.DeviceChannel;
import PhiDevice.DeviceManager;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.ComboBoxModel;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;

/**
 *
 * @author sujoy
 */
public class DeviceChannelListModel implements  ListModel<PhiDevice.DeviceChannel>{
    private final DeviceManager mDeviceManager;
    private Pattern mMatchPattern;
    private DeviceChannel[] mChannels;

    public DeviceChannelListModel(DeviceManager deviceManager) {
        mDeviceManager = deviceManager;
    }

    public DeviceChannelListModel(DeviceManager deviceManager, String matchPattern) {
        mDeviceManager = deviceManager;
        if (matchPattern != null && matchPattern.length() > 0) {
            mMatchPattern = Pattern.compile(matchPattern);
        }
        mChannels = mDeviceManager.getChannels(mMatchPattern);
    }

    @Override
    public int getSize() {
        return mChannels.length;
    }

    @Override
    public DeviceChannel getElementAt(int i) {
        return mChannels[i];
    }

    @Override
    public void addListDataListener(ListDataListener ll) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeListDataListener(ListDataListener ll) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
