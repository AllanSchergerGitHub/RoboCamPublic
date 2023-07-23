package RoboCam;

import DB.ConfigDB;
import PhiDevice.DeviceManager;
import InterfaceComponents.DeviceCheckBoxList;
import InterfaceComponents.DeviceListComboBoxModel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;

/**
 * Helps with assigning motors to each wheel in a dynamic fashion via
 * checkboxes on the GUI. Other parts of the code are also involved in this
 * step.
 * @author sujoy
 */
public class WheelConfigPanel extends javax.swing.JPanel {
    private String mWheelName;
    private int mWheelIndex;

    private DeviceManager mDeviceManager;
    private ConfigDB mConfigDB;
    private Config mConfig;
    private boolean mDataPopulated = false;

    private String mPatternEncoder;
    private String mPatternBLDCPositionController;
    private String mPatternTemperatureSensor;

    private WheelDevice mWheelDevice;

    public static interface ChangeListener {
        public void onChange(int wheelIndex, String paramName);
    }

    private final ArrayList<ChangeListener> mChangeListeners = new ArrayList<>();

    class WidgetParamChangeListener implements
            ActionListener, ListSelectionListener, FocusListener {
        private final String mConfigName;
        private final boolean mIsNumber;

        public WidgetParamChangeListener(String configName, Boolean isNumber) {
            mConfigName = configName;
            mIsNumber = isNumber;
        }

        public void saveChange(Object source) {
            if (source instanceof JComboBox) {
                JComboBox comboBox = (JComboBox) source;
                saveConfigValue(comboBox.getSelectedItem().toString());
            } else if (source instanceof DeviceCheckBoxList) {
                DeviceCheckBoxList deviceCheckBoxList = (DeviceCheckBoxList) source;
                String[][] selectedItems = deviceCheckBoxList.getSelectedNamesAndMultipliers();
                setConfigValue(
                        WheelDevice.BLDCMOTOR_POSITION_CONTROLLER_NAMES,
                        String.join(",", selectedItems[0])
                );
                if (selectedItems[1].length > 0) {
                    setConfigValue(
                            WheelDevice.BLDC_1_POSITION_MULTIPLIER,
                            selectedItems[1][0]
                    );
                }
                if (selectedItems[1].length > 1) {
                    setConfigValue(
                            WheelDevice.BLDC_2_POSITION_MULTIPLIER,
                            selectedItems[1][1]
                    );
                }
            } else {
                JTextField textField = (JTextField) source;
                //System.out.println("textField.getText()" + textField.getText());
                saveConfigValue(textField.getText());
            }
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            Object source = ae.getSource();
            saveChange(source);
        }

        @Override
        public void valueChanged(ListSelectionEvent lse) {//@deprecated
            // System.err.println("error here with unchecked cast? May 20 2019"); fixed it temporarily by 
            // changing the int i = 0 to int i = 1; 6 rows below this one (row 92 on May 20 2019). 
            // the error appeared to be due to two devices in the same part of the array???
            javax.swing.JList<Object> jList = (javax.swing.JList<Object>) lse.getSource();
            Object[] selectedValues = jList.getSelectedValuesList().toArray();
            String[] stringValues = new String[selectedValues.length];
            for (int i = 0; i < selectedValues.length; i++) {
                stringValues[i] = selectedValues[i].toString();
                /*//Multiple selection is allowed since there is two BLDC motors per wheel.
                if(selectedValues.length>1){
                    System.err.println("ERROR likely is here - "+stringValues[i] +" int i = "+i+"; if selectedValues.length>1 "+selectedValues.length);
                }*/
            }
            String values = String.join(",", stringValues);
            //System.out.println("selected list values " + values+" ---end--- ");
            saveConfigValue(values);
        }

        public void saveConfigValue(String text) {
            //System.out.println(String.format("configname:%s, text=%s", mConfigName, text));
            if (text == null) return;
            if (mIsNumber) {
                try {
                    Double.parseDouble(text);
                } catch (NumberFormatException ex) {
                    return;
                }
            }
            setConfigValue(mConfigName, text);
        }

        @Override
        public void focusGained(FocusEvent fe) {
            //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void focusLost(FocusEvent fe) {
            Object source = fe.getSource();
            saveChange(source);
        }
    }

    private void addActionFocusListener(
            JTextField component, WidgetParamChangeListener listener) {
        component.addFocusListener(listener);
        component.addActionListener(listener);
    }

    /**
     * Creates new form WheelConfigPanel
     */
    public WheelConfigPanel() {
        initComponents();
        mComboBoxEncoder.addActionListener(new WidgetParamChangeListener(
                WheelDevice.ENCODER_NAME, Boolean.FALSE)
        );
        mComboBoxTemperatureSensor.addActionListener(new WidgetParamChangeListener(
                WheelDevice.BLDCMOTOR_TEMPERATURE_SENSOR_NAME, Boolean.FALSE)
        );
        mDeviceCheckBoxList.addActionListener(new WidgetParamChangeListener(
                WheelDevice.BLDCMOTOR_POSITION_CONTROLLER_NAMES, Boolean.FALSE));
    }

    public void setWheelName(String wheelName) {
        mWheelName = wheelName;
    }

    public void setWheelIndex(int wheelIndex) {
        mWheelIndex = wheelIndex;
    }

    public void setDeviceManager(DeviceManager deviceManager) {
        if (mDeviceManager != null) return;
        mDeviceManager = deviceManager;
        populateData();
        //Attach a listener to Device Manager to update the device list
        //whenever new device is attached 
        mDeviceManager.addDeviceChangeListener(new DeviceManager.DeviceChangeListener() {
            @Override
            public void onChange() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        mDeviceCheckBoxList.loadDevices();
                        mDataPopulated = false;
                        populateData();
                        //Other combox can be updated here.
                    }
                });
            }
        });
    }

    public void setConfigDB(ConfigDB configDB) {
        mConfigDB = configDB;
        populateData();
    }

    public void loadFromConfig(Config config) {
        mConfig = config;
        mPatternEncoder = config.getPhidgetPatternFor("Encoder");
        mPatternBLDCPositionController = config.getPhidgetPatternFor("BLDCPositionController");
        mPatternTemperatureSensor = config.getPhidgetPatternFor("TemperatureSensor");
        populateData();
    }

    public void setWheelDevice(WheelDevice wheelDevice) {
        mWheelDevice = wheelDevice;
    }

    public void updateWheelDeviceStatus() {
        mLabelEncoderStatusValue.setText(mWheelDevice.getEncoderStatus());
        mLabelBLDCTempSensorStatusValue.setText(mWheelDevice.getBLDCMotor_TemperatureSensorStatus());
        mLabelBLDCPosControllerStatusValue.setText(mWheelDevice.getBLDCPositionControllerStatus());
        mDeviceCheckBoxList.loadDevices();
    }

    public synchronized void populateData() {
        if (mDataPopulated) return;
        if (mDeviceManager != null && mConfig != null) {
            /*Build Combo Box lists*/
            mComboBoxTemperatureSensor.setModel(new DeviceListComboBoxModel(
                    mDeviceManager, mPatternTemperatureSensor));
            mComboBoxEncoder.setModel(new DeviceListComboBoxModel(
                    mDeviceManager, mPatternEncoder));
            mDeviceCheckBoxList.loadDevices(
                    mDeviceManager, mPatternBLDCPositionController);
        }

        /*Show values in combo boxes.*/
        if (mConfigDB != null) {
            mComboBoxEncoder.setSelectedItem(mConfigDB.getValue(
                            getConfigName(WheelDevice.ENCODER_NAME), ""
                    ));
            mComboBoxTemperatureSensor.setSelectedItem(mConfigDB.getValue(
                            getConfigName(WheelDevice.BLDCMOTOR_TEMPERATURE_SENSOR_NAME), ""
                    ));
            String[] controllerNames = mConfigDB.getValue(
                    getConfigName(WheelDevice.BLDCMOTOR_POSITION_CONTROLLER_NAMES), ""
            ).split(",");

            //System.out.println("controllerNames "  + mWheelName + " '" + controllerNames.length + "' " + String.join(",",  controllerNames));
            mDeviceCheckBoxList.selectDevices(
                    controllerNames, new String[]{
                            mConfigDB.getValue(
                                    getConfigName(
                                            WheelDevice.BLDC_1_POSITION_MULTIPLIER
                                    ), "1"
                            ), mConfigDB.getValue(
                                    getConfigName(
                                            WheelDevice.BLDC_2_POSITION_MULTIPLIER
                                    ), "1"
                            )
                    }
            );

        }
        mDataPopulated = (mDeviceManager != null &&
                mConfig != null && mConfigDB != null);
    }

    public void addChangeListener(ChangeListener listener) {
        mChangeListeners.add(listener);
    }

    private void setConfigValue(String configName, Object value) {
        if (mConfigDB == null) return;
        mConfigDB.setValue(getConfigName(configName), (String) value);
        for (ChangeListener listener : mChangeListeners) {
            listener.onChange(mWheelIndex, configName);
        }
    }

    private String getConfigName(String configName) {
        return mWheelName + "." + configName;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        deviceCheckBoxList1 = new InterfaceComponents.DeviceCheckBoxList();
        mLabelEncoder = new javax.swing.JLabel();
        mComboBoxEncoder = new javax.swing.JComboBox<>();
        mLabelBLDCPosControllerStatusValue = new javax.swing.JLabel();
        mScrollPaneDeviceCheckBoxList = new javax.swing.JScrollPane();
        mDeviceCheckBoxList = new InterfaceComponents.DeviceCheckBoxList();
        mLabelBLDCPositionControllers = new javax.swing.JLabel();
        mLabelBLDCPosControllerStatus = new javax.swing.JLabel();
        mLabelEncoderStatusValue = new javax.swing.JLabel();
        btnShowUpdatedStatus = new javax.swing.JButton();
        mComboBoxTemperatureSensor = new javax.swing.JComboBox<>();
        mLabelTempSensor = new javax.swing.JLabel();
        mLabelBLDCTempSensorStatusValue = new javax.swing.JLabel();

        setAlignmentY(0.0F);
        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        mLabelEncoder.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        mLabelEncoder.setText("<html>Encoder (we use a dropdown <br> because there is only one <br> of these per wheel.)</html>");
        mLabelEncoder.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        add(mLabelEncoder, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 20, 170, 60));

        mComboBoxEncoder.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        add(mComboBoxEncoder, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 20, 560, -1));

        mLabelBLDCPosControllerStatusValue.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        mLabelBLDCPosControllerStatusValue.setText("< Status>");
        mLabelBLDCPosControllerStatusValue.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        mLabelBLDCPosControllerStatusValue.setAlignmentX(0.5F);
        mLabelBLDCPosControllerStatusValue.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        add(mLabelBLDCPosControllerStatusValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 470, 520, 100));

        mDeviceCheckBoxList.setAutoscrolls(true);
        mScrollPaneDeviceCheckBoxList.setViewportView(mDeviceCheckBoxList);

        add(mScrollPaneDeviceCheckBoxList, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 200, 600, 250));

        mLabelBLDCPositionControllers.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        mLabelBLDCPositionControllers.setText("<html>BLDC Position Controllers <br> (we use a checkbox because<br> there could be two of these per <br> wheel) </html>");
        mLabelBLDCPositionControllers.setAlignmentY(0.0F);
        mLabelBLDCPositionControllers.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        add(mLabelBLDCPositionControllers, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 200, -1, -1));

        mLabelBLDCPosControllerStatus.setText("BLDC Position Controller Status");
        add(mLabelBLDCPosControllerStatus, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 470, -1, -1));

        mLabelEncoderStatusValue.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        mLabelEncoderStatusValue.setText("< Status>");
        mLabelEncoderStatusValue.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        mLabelEncoderStatusValue.setAlignmentX(0.5F);
        mLabelEncoderStatusValue.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        add(mLabelEncoderStatusValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 50, 520, 40));

        btnShowUpdatedStatus.setText("Show Updated Status");
        btnShowUpdatedStatus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnShowUpdatedStatusActionPerformed(evt);
            }
        });
        add(btnShowUpdatedStatus, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 630, -1, -1));

        mComboBoxTemperatureSensor.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        add(mComboBoxTemperatureSensor, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 100, 560, -1));

        mLabelTempSensor.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        mLabelTempSensor.setText("<html>BLDC Motor TempSensor<br> (we use a dropdown <br> because there is only one <br> of these per wheel.)</html>");
        mLabelTempSensor.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        add(mLabelTempSensor, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 100, 170, 80));

        mLabelBLDCTempSensorStatusValue.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        mLabelBLDCTempSensorStatusValue.setText("< Status>");
        mLabelBLDCTempSensorStatusValue.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        mLabelBLDCTempSensorStatusValue.setAlignmentX(0.5F);
        mLabelBLDCTempSensorStatusValue.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        add(mLabelBLDCTempSensorStatusValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 130, 520, 40));
    }// </editor-fold>//GEN-END:initComponents

    private void btnShowUpdatedStatusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnShowUpdatedStatusActionPerformed
        updateWheelDeviceStatus();
    }//GEN-LAST:event_btnShowUpdatedStatusActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnShowUpdatedStatus;
    private InterfaceComponents.DeviceCheckBoxList deviceCheckBoxList1;
    private javax.swing.JComboBox<String> mComboBoxEncoder;
    private javax.swing.JComboBox<String> mComboBoxTemperatureSensor;
    private InterfaceComponents.DeviceCheckBoxList mDeviceCheckBoxList;
    private javax.swing.JLabel mLabelBLDCPosControllerStatus;
    private javax.swing.JLabel mLabelBLDCPosControllerStatusValue;
    private javax.swing.JLabel mLabelBLDCPositionControllers;
    private javax.swing.JLabel mLabelBLDCTempSensorStatusValue;
    private javax.swing.JLabel mLabelEncoder;
    private javax.swing.JLabel mLabelEncoderStatusValue;
    private javax.swing.JLabel mLabelTempSensor;
    private javax.swing.JScrollPane mScrollPaneDeviceCheckBoxList;
    // End of variables declaration//GEN-END:variables
}
