/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package RoboCam;

import DB.ConfigDB;
import PhiDevice.DeviceManager;
import RoverUI.DeviceCheckBoxList;
import RoverUI.DeviceListComboBoxModel;

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

    private String mPatternDCMotor;
    private String mPatternBLDCMotor;
    private String mPatternEncoder;
    private String mPatternStepperMotor;
    private String mPatternBLDCPositionController;
    private String mPatternLimitSwitch;

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
        mComboBoxDCMotor.addActionListener(new WidgetParamChangeListener(
                WheelDevice.DCMOTOR_NAME, Boolean.FALSE)
        );
        mComboBoxEncoder.addActionListener(new WidgetParamChangeListener(
                WheelDevice.ENCODER_NAME, Boolean.FALSE)
        );

        mCmbLeftLimitSwitch.addActionListener(new WidgetParamChangeListener(
                WheelDevice.LIMIT_SWITCH_LEFT_NAME, Boolean.FALSE)
        );
        mCmbRightLimitSwitch.addActionListener(new WidgetParamChangeListener(
                WheelDevice.LIMIT_SWITCH_RIGHT_NAME, Boolean.FALSE)
        );

        mDeviceCheckBoxList.addActionListener(new WidgetParamChangeListener(
                WheelDevice.BLDCMOTOR_POSITION_CONTROLLER_NAMES, Boolean.FALSE));

        addActionFocusListener(mTextFieldDCMotorSpeedMult, new WidgetParamChangeListener(
                WheelDevice.DCMOTOR_SPEED_MULTIPLIER, Boolean.TRUE));
    }

    public void setWheelName(String wheelName) {
        mWheelName = wheelName;
    }

    public String getWheelName() {
        return mWheelName;
    }

    public void setWheelIndex(int wheelIndex) {
        mWheelIndex = wheelIndex;
    }

    public int getWheelIndex() {
        return mWheelIndex;
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
        mPatternDCMotor = config.getPhidgetPatternFor("DCMotor");
        mPatternBLDCMotor = config.getPhidgetPatternFor("BLDCMotor");
        mPatternStepperMotor = config.getPhidgetPatternFor("StepperMotor");
        mPatternEncoder = config.getPhidgetPatternFor("Encoder");
        mPatternLimitSwitch = config.getPhidgetPatternFor("LimitSwitch");
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
        if (mDeviceManager != null && mConfig != null &&
                !(mComboBoxDCMotor.getModel() instanceof DeviceListComboBoxModel)) {
            /*Build Combo Box lists*/
            mComboBoxDCMotor.setModel(new DeviceListComboBoxModel(
                    mDeviceManager, mPatternDCMotor));
            mComboBoxStepperMotor.setModel(new DeviceListComboBoxModel(
                    mDeviceManager, mPatternStepperMotor));
            mComboBoxEncoder.setModel(new DeviceListComboBoxModel(
                    mDeviceManager, mPatternEncoder));
            mCmbLeftLimitSwitch.setModel(new DeviceListComboBoxModel(
                    mDeviceManager, mPatternLimitSwitch));
            mCmbRightLimitSwitch.setModel(new DeviceListComboBoxModel(
                    mDeviceManager, mPatternLimitSwitch));
            mDeviceCheckBoxList.loadDevices(
                    mDeviceManager, mPatternBLDCPositionController);
        }

        /*Show values in combo boxes.*/
        if (mConfigDB != null) {
            mComboBoxDCMotor.setSelectedItem(mConfigDB.getValue(
                            getConfigName(WheelDevice.DCMOTOR_NAME), ""
                    )
            );
            mComboBoxEncoder.setSelectedItem(mConfigDB.getValue(
                            getConfigName(WheelDevice.ENCODER_NAME), ""
                    )
            );
            mCmbLeftLimitSwitch.setSelectedItem(mConfigDB.getValue(
                            getConfigName(WheelDevice.LIMIT_SWITCH_LEFT_NAME), ""
                    )
            );
            mCmbRightLimitSwitch.setSelectedItem(mConfigDB.getValue(
                            getConfigName(WheelDevice.LIMIT_SWITCH_RIGHT_NAME), ""
                    )
            );

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

            /* Show values in text boxes */
            mTextFieldDCMotorSpeedMult.setText(
                    mConfigDB.getValue(
                            getConfigName(
                                    WheelDevice.DCMOTOR_SPEED_MULTIPLIER
                            ), "1"
                    )
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

        deviceCheckBoxList1 = new RoverUI.DeviceCheckBoxList();
        mLabelDCMotor = new javax.swing.JLabel();
        mLabelSterpperMotor = new javax.swing.JLabel();
        mComboBoxDCMotor = new javax.swing.JComboBox<>();
        mComboBoxStepperMotor = new javax.swing.JComboBox<>();
        mLabelEncoder = new javax.swing.JLabel();
        mComboBoxEncoder = new javax.swing.JComboBox<>();
        mLabelStepperCurrent = new javax.swing.JLabel();
        mTextFieldStepperCurrent = new javax.swing.JTextField();
        mLabelStepperPosReadScale = new javax.swing.JLabel();
        mTextFieldStepperPositionReadScale = new javax.swing.JTextField();
        mLabelStepperPosWriteScale = new javax.swing.JLabel();
        mTextFieldStepperPositionWriteScale = new javax.swing.JTextField();
        mLabelStepperMaxSpeed = new javax.swing.JLabel();
        mTextFieldStepperMaxSpeed = new javax.swing.JTextField();
        mLblLimitSwitchLeft = new javax.swing.JLabel();
        mTextFieldDCMotorSpeedMult = new javax.swing.JTextField();
        mLabelBLDCPosControllerStatusValue = new javax.swing.JLabel();
        mScrollPaneDeviceCheckBoxList = new javax.swing.JScrollPane();
        mDeviceCheckBoxList = new RoverUI.DeviceCheckBoxList();
        mLabelBLDCPositionControllers = new javax.swing.JLabel();
        mLabelStepperStatusValue = new javax.swing.JLabel();
        mLabelBLDCPosControllerStatus = new javax.swing.JLabel();
        mLabelStepperStatus = new javax.swing.JLabel();
        mLabelEncoderStatusValue = new javax.swing.JLabel();
        mLabelEncoderStatus = new javax.swing.JLabel();
        btnShowUpdatedStatus = new javax.swing.JButton();
        mLabelDCMotorSpeedMult = new javax.swing.JLabel();
        mCmbLeftLimitSwitch = new javax.swing.JComboBox<>();
        mLblLimitSwitchRight = new javax.swing.JLabel();
        mCmbRightLimitSwitch = new javax.swing.JComboBox<>();

        setAlignmentY(0.0F);
        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        mLabelDCMotor.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        mLabelDCMotor.setText("DC Motor");
        add(mLabelDCMotor, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 3, -1, -1));

        mLabelSterpperMotor.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        mLabelSterpperMotor.setText("Stepper Motor");
        add(mLabelSterpperMotor, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 23, -1, -1));

        mComboBoxDCMotor.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{"Item 1", "Item 2", "Item 3", "Item 4"}));
        add(mComboBoxDCMotor, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 0, 560, -1));

        mComboBoxStepperMotor.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{"Item 1", "Item 2", "Item 3", "Item 4"}));
        add(mComboBoxStepperMotor, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 20, 560, -1));

        mLabelEncoder.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        mLabelEncoder.setText("Encoder");
        add(mLabelEncoder, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 43, -1, -1));

        mComboBoxEncoder.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{"Item 1", "Item 2", "Item 3", "Item 4"}));
        add(mComboBoxEncoder, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 40, 560, -1));

        mLabelStepperCurrent.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        mLabelStepperCurrent.setText("Stepper Current");
        add(mLabelStepperCurrent, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 63, -1, -1));

        mTextFieldStepperCurrent.setText("<Number>");
        add(mTextFieldStepperCurrent, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 60, 435, -1));

        mLabelStepperPosReadScale.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        mLabelStepperPosReadScale.setText("Stepper Position Read Scale");
        add(mLabelStepperPosReadScale, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 83, -1, -1));

        mTextFieldStepperPositionReadScale.setText("<Number>");
        add(mTextFieldStepperPositionReadScale, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 80, 435, -1));

        mLabelStepperPosWriteScale.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        mLabelStepperPosWriteScale.setText("Stepper Position Write Scale");
        add(mLabelStepperPosWriteScale, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 103, -1, -1));

        mTextFieldStepperPositionWriteScale.setText("<Number>");
        add(mTextFieldStepperPositionWriteScale, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 100, 435, -1));

        mLabelStepperMaxSpeed.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        mLabelStepperMaxSpeed.setText("Stepper Max Speed");
        add(mLabelStepperMaxSpeed, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 123, -1, -1));

        mTextFieldStepperMaxSpeed.setText("<Number>");
        add(mTextFieldStepperMaxSpeed, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 120, 435, -1));

        mLblLimitSwitchLeft.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        mLblLimitSwitchLeft.setText("Left Limit Switch");
        add(mLblLimitSwitchLeft, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 170, -1, -1));

        mTextFieldDCMotorSpeedMult.setText("<Number>");
        add(mTextFieldDCMotorSpeedMult, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 140, 435, -1));

        mLabelBLDCPosControllerStatusValue.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        mLabelBLDCPosControllerStatusValue.setText("< Status>");
        mLabelBLDCPosControllerStatusValue.setAlignmentX(0.5F);
        mLabelBLDCPosControllerStatusValue.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        add(mLabelBLDCPosControllerStatusValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 470, 435, -1));

        mDeviceCheckBoxList.setAutoscrolls(true);
        mScrollPaneDeviceCheckBoxList.setViewportView(mDeviceCheckBoxList);

        add(mScrollPaneDeviceCheckBoxList, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 240, 560, 190));

        mLabelBLDCPositionControllers.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        mLabelBLDCPositionControllers.setText("BLDC Position Controllers");
        mLabelBLDCPositionControllers.setAlignmentY(0.0F);
        mLabelBLDCPositionControllers.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        add(mLabelBLDCPositionControllers, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 230, -1, -1));

        mLabelStepperStatusValue.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        mLabelStepperStatusValue.setText("< Status>");
        mLabelStepperStatusValue.setAlignmentX(0.5F);
        mLabelStepperStatusValue.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        add(mLabelStepperStatusValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 430, 435, -1));

        mLabelBLDCPosControllerStatus.setText("BLDC Position Controller Status");
        add(mLabelBLDCPosControllerStatus, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 470, -1, -1));

        mLabelStepperStatus.setText("Stepper Status");
        add(mLabelStepperStatus, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 430, -1, -1));

        mLabelEncoderStatusValue.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        mLabelEncoderStatusValue.setText("< Status>");
        mLabelEncoderStatusValue.setAlignmentX(0.5F);
        mLabelEncoderStatusValue.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        add(mLabelEncoderStatusValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 450, 435, -1));

        mLabelEncoderStatus.setText("Encoder Status");
        add(mLabelEncoderStatus, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 450, -1, -1));

        btnShowUpdatedStatus.setText("Show Updated Status");
        btnShowUpdatedStatus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnShowUpdatedStatusActionPerformed(evt);
            }
        });
        add(btnShowUpdatedStatus, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 500, -1, -1));

        mLabelDCMotorSpeedMult.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        mLabelDCMotorSpeedMult.setText("DC Motor Speed Multiplier");
        add(mLabelDCMotorSpeedMult, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 143, -1, -1));

        mCmbLeftLimitSwitch.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{"Item 1", "Item 2", "Item 3", "Item 4"}));
        add(mCmbLeftLimitSwitch, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 170, 560, -1));

        mLblLimitSwitchRight.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        mLblLimitSwitchRight.setText("Right Limit Switch");
        add(mLblLimitSwitchRight, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 200, -1, -1));

        mCmbRightLimitSwitch.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{"Item 1", "Item 2", "Item 3", "Item 4"}));
        add(mCmbRightLimitSwitch, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 200, 560, -1));
    }// </editor-fold>//GEN-END:initComponents

    private void btnShowUpdatedStatusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnShowUpdatedStatusActionPerformed
        updateWheelDeviceStatus();
    }//GEN-LAST:event_btnShowUpdatedStatusActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnShowUpdatedStatus;
    private RoverUI.DeviceCheckBoxList deviceCheckBoxList1;
    private javax.swing.JComboBox<String> mCmbLeftLimitSwitch;
    private javax.swing.JComboBox<String> mCmbRightLimitSwitch;
    private javax.swing.JComboBox<String> mComboBoxDCMotor;
    private javax.swing.JComboBox<String> mComboBoxEncoder;
    private javax.swing.JComboBox<String> mComboBoxStepperMotor;
    private RoverUI.DeviceCheckBoxList mDeviceCheckBoxList;
    private javax.swing.JLabel mLabelBLDCPosControllerStatus;
    private javax.swing.JLabel mLabelBLDCPosControllerStatusValue;
    private javax.swing.JLabel mLabelBLDCPositionControllers;
    private javax.swing.JLabel mLabelDCMotor;
    private javax.swing.JLabel mLabelDCMotorSpeedMult;
    private javax.swing.JLabel mLabelEncoder;
    private javax.swing.JLabel mLabelEncoderStatus;
    private javax.swing.JLabel mLabelEncoderStatusValue;
    private javax.swing.JLabel mLabelStepperCurrent;
    private javax.swing.JLabel mLabelStepperMaxSpeed;
    private javax.swing.JLabel mLabelStepperPosReadScale;
    private javax.swing.JLabel mLabelStepperPosWriteScale;
    private javax.swing.JLabel mLabelStepperStatus;
    private javax.swing.JLabel mLabelStepperStatusValue;
    private javax.swing.JLabel mLabelSterpperMotor;
    private javax.swing.JLabel mLblLimitSwitchLeft;
    private javax.swing.JLabel mLblLimitSwitchRight;
    private javax.swing.JScrollPane mScrollPaneDeviceCheckBoxList;
    private javax.swing.JTextField mTextFieldDCMotorSpeedMult;
    private javax.swing.JTextField mTextFieldStepperCurrent;
    private javax.swing.JTextField mTextFieldStepperMaxSpeed;
    private javax.swing.JTextField mTextFieldStepperPositionReadScale;
    private javax.swing.JTextField mTextFieldStepperPositionWriteScale;
    // End of variables declaration//GEN-END:variables
}
