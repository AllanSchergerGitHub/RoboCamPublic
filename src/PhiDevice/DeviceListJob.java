/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PhiDevice;

import javax.swing.*;
import java.util.List;

/**
 * @author sujoy
 */
public class DeviceListJob extends SwingWorker<List<String>, String> {
    private DeviceManager mDeviceManager;
    private JComboBox<String> mDeviceListComboBox;
    private String mDefaultDeviceName = null;

    public DeviceListJob(DeviceManager deviceManager,
                         JComboBox<String> deviceListComboBox) {
        mDeviceManager = deviceManager;
        mDeviceListComboBox = deviceListComboBox;
    }

    public void setSelectedDevice(String deviceName) {
        mDefaultDeviceName = deviceName;
        if (deviceName == null) return;
        System.out.println(String.format("mDefaultDeviceName %s", mDefaultDeviceName));
        for (int i = 0; i < mDeviceListComboBox.getItemCount(); i++) {
            String itemName = mDeviceListComboBox.getItemAt(i);
            if (itemName.startsWith(mDefaultDeviceName)) {
                mDeviceListComboBox.setSelectedIndex(i);
                break;
            }
        }
    }

    @Override
    protected List<String> doInBackground() throws Exception {
        int prevCount = 0;
        while (true) {
            int currCount = mDeviceManager.getChannelCount();
            if (prevCount != currCount) {
                prevCount = currCount;
                publish(mDeviceManager.getChannelNames());
            }
            Thread.sleep(10);
        }
    }

    @Override
    protected void process(List<String> list) {
        Object selectedItem = mDeviceListComboBox.getSelectedItem();
        mDeviceListComboBox.removeAllItems();
        for (String item : list) {
            mDeviceListComboBox.addItem(item);
        }
        if (selectedItem != null) {
            mDeviceListComboBox.setSelectedIndex(
                    list.indexOf(selectedItem.toString()));
        } else {
            setSelectedDevice(mDefaultDeviceName);
        }
    }
}
