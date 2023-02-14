package RoboCam;

import DB.ConfigDB;
import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.AbstractTableModel;

public class WheelConfigModel extends AbstractTableModel {
    private static final int PHIDGET_DEVICE_ROW_COUNT = 5;
    
    private static final String[] PARAM_NAMES = new String[] {
        WheelDevice.DCMOTOR_NAME,
        WheelDevice.ENCODER_NAME,
        WheelDevice.BLDCMOTOR_NAME,
        WheelDevice.BLDCMOTOR_POSITION_CONTROLLER_NAME,
        WheelDevice.DCMOTOR_SPEED_MULTIPLIER,
        WheelDevice.BLDC_1_POSITION_MULTIPLIER,
        WheelDevice.BLDC_2_POSITION_MULTIPLIER,
    };

    public static interface WheelConfigChangeListener {
        public void onChange(WheelDevice wheelDevice, String paramName);
    }

    private final ConfigDB mConfigDB;
    private final TruckDevice mTruckDevice;
    private final ArrayList<WheelConfigChangeListener> mChangeListeners = new ArrayList<>();


    public WheelConfigModel(TruckDevice truckDevice, ConfigDB configDB) {
        mConfigDB = configDB;
        mTruckDevice = truckDevice;
    }

    public void addChangeListener(WheelConfigChangeListener listener) {
        mChangeListeners.add(listener);
    }

    @Override
    public int getRowCount() {
        return PARAM_NAMES.length;
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public String getColumnName(int i) {
        if (i == 0) return "Parameter";
        return mTruckDevice.getWheelDeviceAt(i-1).getWheel().getWheelName();
    }

    public boolean isCellEditable(int row, int col) {
        return (col != 0);
    }

    @Override
    public Object getValueAt(int r, int c) {
        if (c == 0) return PARAM_NAMES[r];
        return mConfigDB.getValue(getConfigName(r, c), "");
    }

    @Override
    public void setValueAt(Object o, int r, int c) {
        String configName = getConfigName(r, c);
        if (c > 0) {
            if (r >= PHIDGET_DEVICE_ROW_COUNT) {
                try {
                    Double.parseDouble((String) o);
                } catch (NumberFormatException ex) {
                    return;
                }
            }
            mConfigDB.setValue(getConfigName(r, c), (String) o);
            for (WheelConfigChangeListener listener: mChangeListeners) {
                listener.onChange(mTruckDevice.getWheelDeviceAt(c-1), configName);
            }
        }
    }

    private String getConfigName(int r, int c) {
        String configName = mTruckDevice.getWheelDeviceAt(c-1).getWheel().getWheelName();
        configName += "." + PARAM_NAMES[r];
        return configName;
    }
}
