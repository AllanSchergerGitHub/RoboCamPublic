package InterfaceComponents;

import PhiDevice.DeviceManager;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.util.regex.Pattern;

public class DeviceListComboBoxModel implements ComboBoxModel<String> {
    private DeviceManager mDeviceManager;
    private String mSelectedChannelName;
    private Pattern mMatchPattern;

    public DeviceListComboBoxModel(DeviceManager deviceManager) {
        mDeviceManager = deviceManager;
    }

    public DeviceListComboBoxModel(DeviceManager deviceManager, String matchPattern) {
        mDeviceManager = deviceManager;
        if (matchPattern != null && matchPattern.length() > 0) {
            mMatchPattern = Pattern.compile(matchPattern);
        }
    }

    @Override
    public void setSelectedItem(Object o) {
        mSelectedChannelName = (String) o;
    }

    @Override
    public Object getSelectedItem() {
        return mSelectedChannelName;
    }

    @Override
    public int getSize() {
        if (mMatchPattern != null) {
            return mDeviceManager.getChannelNames(mMatchPattern).length;
        }
        return mDeviceManager.getChannelCount();
    }

    @Override
    public String getElementAt(int i) {
        if (mMatchPattern != null) {
            String[] channelNames = mDeviceManager.getChannelNames(mMatchPattern);
            if (i < channelNames.length) {
                return channelNames[i];
            }
            return "";
        }
        return mDeviceManager.getChannelNames()[i];
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
