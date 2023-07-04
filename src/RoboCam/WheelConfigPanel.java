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
        populateData();
    }

    public void setWheelDevice(WheelDevice wheelDevice) {
        mWheelDevice = wheelDevice;
    }

    public void updateWheelDeviceStatus() {
        mLabelEncoderStatusValue.setText(mWheelDevice.getEncoderStatus());
        mLabelBLDCPosControllerStatusValue.setText(mWheelDevice.getBLDCPositionControllerStatus());
        mDeviceCheckBoxList.loadDevices();
    }

    public synchronized void populateData() {
        if (mDataPopulated) return;
        if (mDeviceManager != null && mConfig != null) {
            /*Build Combo Box lists*/
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
        mLabelEncoderStatus = new javax.swing.JLabel();
        btnShowUpdatedStatus = new javax.swing.JButton();

        setAlignmentY(0.0F);
        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        mLabelEncoder.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        mLabelEncoder.setText("Encoder");
        add(mLabelEncoder, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 20, -1, -1));

        mComboBoxEncoder.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        add(mComboBoxEncoder, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 10, 560, -1));

        mLabelBLDCPosControllerStatusValue.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        mLabelBLDCPosControllerStatusValue.setText("< Status>");
        mLabelBLDCPosControllerStatusValue.setAlignmentX(0.5F);
        mLabelBLDCPosControllerStatusValue.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        add(mLabelBLDCPosControllerStatusValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 460, 520, 110));

        mDeviceCheckBoxList.setAutoscrolls(true);
        mScrollPaneDeviceCheckBoxList.setViewportView(mDeviceCheckBoxList);

        add(mScrollPaneDeviceCheckBoxList, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 50, 600, 310));

        mLabelBLDCPositionControllers.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        mLabelBLDCPositionControllers.setText("BLDC Position Controllers");
        mLabelBLDCPositionControllers.setAlignmentY(0.0F);
        mLabelBLDCPositionControllers.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        add(mLabelBLDCPositionControllers, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 150, -1, -1));

        mLabelBLDCPosControllerStatus.setText("BLDC Position Controller Status");
        add(mLabelBLDCPosControllerStatus, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 490, -1, -1));

        mLabelEncoderStatusValue.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        mLabelEncoderStatusValue.setText("< Status>");
        mLabelEncoderStatusValue.setAlignmentX(0.5F);
        mLabelEncoderStatusValue.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        add(mLabelEncoderStatusValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 420, 520, -1));

        mLabelEncoderStatus.setText("Encoder Status");
        add(mLabelEncoderStatus, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 430, -1, -1));

        btnShowUpdatedStatus.setText("Show Updated Status");
        btnShowUpdatedStatus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnShowUpdatedStatusActionPerformed(evt);
            }
        });
        add(btnShowUpdatedStatus, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 630, -1, -1));
    }// </editor-fold>//GEN-END:initComponents

    private void btnShowUpdatedStatusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnShowUpdatedStatusActionPerformed
        updateWheelDeviceStatus();
    }//GEN-LAST:event_btnShowUpdatedStatusActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnShowUpdatedStatus;
    private InterfaceComponents.DeviceCheckBoxList deviceCheckBoxList1;
    private javax.swing.JComboBox<String> mComboBoxEncoder;
    private InterfaceComponents.DeviceCheckBoxList mDeviceCheckBoxList;
    private javax.swing.JLabel mLabelBLDCPosControllerStatus;
    private javax.swing.JLabel mLabelBLDCPosControllerStatusValue;
    private javax.swing.JLabel mLabelBLDCPositionControllers;
    private javax.swing.JLabel mLabelEncoder;
    private javax.swing.JLabel mLabelEncoderStatus;
    private javax.swing.JLabel mLabelEncoderStatusValue;
    private javax.swing.JScrollPane mScrollPaneDeviceCheckBoxList;
    // End of variables declaration//GEN-END:variables
}
