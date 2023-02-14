package RoverUI;

import PhiDevice.DeviceChannel;
import PhiDevice.DeviceManager;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;


public class DeviceCheckBoxList extends JPanel {
    private DeviceManager mDeviceManager;
    private Pattern mMatchPattern;
    private DeviceChannel[] mChannels;

    private HashMap<String, DeviceListItem> mCheckBoxMap = new HashMap<>();
    private ArrayList<ActionListener> mActionListeners = new ArrayList<>();
    
    private DeviceListItem.ListItemActionListener mCheckBoxActionListener = 
            new DeviceListItem.ListItemActionListener() {
        @Override
        public void actionPerformed(ActionEvent ae) {
            fireActionListeners();
        }

        @Override
        public void focusGained(FocusEvent fe) {
            fireActionListeners();
        }

        @Override
        public void focusLost(FocusEvent fe) {
            fireActionListeners();
        }
    };
    private ActionEvent mActionEvent = new ActionEvent(
            this, ActionEvent.ACTION_PERFORMED, null);

    public DeviceCheckBoxList() {
        setLayout(new GridLayout(0, 1));
        //dummyBuildUI();
    }
    
    public void loadDevices(DeviceManager deviceManager, String matchPattern) {
        mDeviceManager = deviceManager;
        if (matchPattern != null && matchPattern.length() > 0) {
            mMatchPattern = Pattern.compile(matchPattern);
        }
        loadDevices();
    }
    
    public void addActionListener(ActionListener al) {
        mActionListeners.add(al);
    }
    
    private void fireActionListeners() {
        for(ActionListener listener: mActionListeners) {
            listener.actionPerformed(mActionEvent);
        }
    }
    
    public String[][] getSelectedNamesAndMultipliers() {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> multipliers = new ArrayList<>();
        for (Map.Entry<String, DeviceListItem> entry: mCheckBoxMap.entrySet()) {
            if (entry.getValue().isSelected()) {
                names.add(entry.getKey());
                multipliers.add(
                    String.format(
                        "%f", entry.getValue().getMultiplier())
                );
            }
        }
        return new String[][] {
            names.toArray(new String[names.size()]),
            multipliers.toArray(new String[multipliers.size()]),
        };
    }
    
    public void selectDevices(String[] deviceNames, String[] multipliers) {
        int i = -1;
        for(String deviceName: deviceNames) {
            i++;
            double multiplier = Double.parseDouble(multipliers[i]);
            if (mCheckBoxMap.containsKey(deviceName)) {
                mCheckBoxMap.get(deviceName).setSelected(true);
                mCheckBoxMap.get(deviceName).setMultiplier(multiplier);
            }
        }
    }
    
    public void loadDevices() {
        if (mDeviceManager == null) return;
        for (DeviceChannel channel: mDeviceManager.getChannels(mMatchPattern)) {
            String channelName = channel.getName();
            DeviceListItem listItem;
            if (!mCheckBoxMap.containsKey(channelName)) {;
                listItem = new DeviceListItem(channelName);
                listItem.setPreferredSize(new Dimension(0, 50));
                listItem.addActionListener(mCheckBoxActionListener);
                mCheckBoxMap.put(channelName, listItem);
                add(listItem);
            } else {
                listItem = mCheckBoxMap.get(channelName);
            }
            listItem.setStatus(channel.isOpen() ? "(Open)": "(-)");
        }
    }
    
    private void dummyBuildUI() {
        for (int i = 0; i < 5; i++) {
            String channelName = String.format("Channel %d", i);
            if (mCheckBoxMap.containsKey(channelName)) continue;
            DeviceListItem listItem = new DeviceListItem(channelName);
            listItem.setPreferredSize(new Dimension(0, 50));
            listItem.addActionListener(mCheckBoxActionListener);
            mCheckBoxMap.put(channelName, listItem);
            add(listItem);
        }
    }
}
