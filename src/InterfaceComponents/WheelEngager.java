package InterfaceComponents;

import RoverUI.Vehicle.DeviceInfo;
import RoverUI.Vehicle.Wheel;

public class WheelEngager extends javax.swing.JPanel {
    private Wheel mWheel;

    /**
     * Creates new form WheelEngager
     */
    public WheelEngager() {
        initComponents();
    }

    public DeviceEngager[] getEngagerList() {
        return new DeviceEngager[]{
                mEngager1, mEngager2
        };
    }

    Wheel.DeviceInfoListChangeListener mDeviceListInfoListListener = new Wheel.DeviceInfoListChangeListener() {
        @Override
        public void onChange() {
            connectEngagersToWheelDeviceInfos();
        }
    };

    public void setWheel(Wheel wheel) {
        mWheel = wheel;
        mWheel.addDeviceInfoListChangeListener(mDeviceListInfoListListener);
        mLblWheelName.setText("<html><b>" + wheel.getWheelName() + "</b></html>");
        connectEngagersToWheelDeviceInfos();
    }

    public void connectEngagersToWheelDeviceInfos() {
        DeviceEngager[] engagerList = getEngagerList();
        int deviceIndex = 0;
        for (DeviceInfo deviceInfo : mWheel.getDeviceInfoList()) {
            if (deviceIndex < engagerList.length) {
                engagerList[deviceIndex].setDeviceInfo(deviceInfo);
            }
            deviceIndex++;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        mLblWheelName = new javax.swing.JLabel();
        mEngager1 = new InterfaceComponents.DeviceEngager();
        mBtnAllEngager = new javax.swing.JButton();
        mEngager2 = new InterfaceComponents.DeviceEngager();

        java.awt.GridBagLayout layout = new java.awt.GridBagLayout();
        layout.columnWeights = new double[]{0.0};
        setLayout(layout);

        mLblWheelName.setText("<Wheel Name>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        add(mLblWheelName, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        add(mEngager1, gridBagConstraints);

        mBtnAllEngager.setText("DisEngage All");
        mBtnAllEngager.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBtnAllEngagerActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        add(mBtnAllEngager, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        add(mEngager2, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void mBtnAllEngagerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBtnAllEngagerActionPerformed
        mEngager1.setEngaged(false);
        mEngager2.setEngaged(false);
    }//GEN-LAST:event_mBtnAllEngagerActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton mBtnAllEngager;
    private InterfaceComponents.DeviceEngager mEngager1;
    private InterfaceComponents.DeviceEngager mEngager2;
    private javax.swing.JLabel mLblWheelName;
    // End of variables declaration//GEN-END:variables
}
