package RoverUI;

import RoboCam.Config;
import RoboCam.IPCamPanel;
import RoverUI.Vehicle.SteeringMode;
import RoverUI.Vehicle.Truck;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import mySQL.MysqlLogger;

public class TruckSteerPanel extends javax.swing.JPanel{
    double mForwardAngleMult = 3;
    private String mMachineName = "init";
    private final ArrayList<StringListener> mSteeringListeners = new ArrayList<>();
    private final ArrayList<StringListener> mSteerDirectionListeners = new ArrayList<>();
    private final ArrayList<DoubleVarArgListener> mSpeedListeners = new ArrayList<>();
    private final ArrayList<StringListener> mMouseHandednessListeners = new ArrayList<>();
    private final ArrayList<DoubleVarArgListener> mForwardAngleListeners = new ArrayList<>();
    private final ArrayList<DoubleListener> mTruckScaleListeners = new ArrayList<>();
    private final ArrayList<Double> avgLagTime = new ArrayList<Double>();
    private double grandTotalLagTime1000Instances = 0;

    private double angleLeftBasedOnPot = 0;
    private double angleRightBasedOnPot = 0;
    private String StoppedFlag = "NotStopped";
    String mBatch_time_stamp_into_mysql = "init";
    Timer mScreenUpdateTimer = null;
    double mWheelVerticalAngleIncrement = 10;
    double mWheelVerticalAngleMultiple = 1;
    
    private int mouseHypotheticalX = 395; // 390 on UI and 318 on Rover (see btnRotateStraightActionPerformed)
    private int mousePosHypotheticalIncrement = 5;
    
    private double mVelocityLimitIncrement = 5;
    private double mPrivVelocityLimit = 150;
    private boolean mAllowMySqlLogging = false;

    ActionListener mScreenUpdateTask = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent ae) {
            repaint();
        }
    };

    /**
     * Creates new form TruckSteerPanel
     */
    public TruckSteerPanel() {
        initComponents();
        rdStreerModeTurnAround.setToolTipText("a = 'TurnAround'");
        rdStreerModeStraight.setToolTipText("s = 'Straight'");
        rdStreerModeSideToSide.setToolTipText("w = 'SideToSide'");
        rdStreerModeMouseFree.setToolTipText("e = 'MouseFree'");
        rdStreerModeFrontStear.setToolTipText("r = 'FrontSteer'");
        rdStreerModePivot.setToolTipText("p = 'Pivot'");
        rdStreerModeStopped.setToolTipText("q = 'Stopped'");
        setSteeringMode("Stopped");
    }

    public void loadFromConfig(Config config){
        mAllowMySqlLogging = config.hasMySQL();
        System.out.println("mAllowMySqlLogging?..."+mAllowMySqlLogging);
        mWheelVerticalAngleIncrement = config.getWheelVerticalAngleIncrement();
        mGroundPanel.getTruck().setDrawScale(config.getTruckDrawingScale());
        if (config.hasIPCam("Ground")) {
            mGroundPanel.setGroundImageUrl(config.getIPCamUrl("Ground"));
            mGroundPanel.getGroundIPCam().addConnectionListener(new IPCamPanel.ConnectionListener() {
                @Override
                public void onConnect() {}

                @Override
                public void onDisconnect() {}

                @Override
                public void onImageUpdate() {
                    repaint();
                }
            });
        }
        updateVelocityIncrementLabel();
        updateVelocityLimitLabel();
    }

    public void setMachineName(String MachineName){
        mMachineName = MachineName;
    }
    
    public void startPeriodicScreenUpdate(int delay) {
        mScreenUpdateTimer = new Timer(delay, mScreenUpdateTask);
        mScreenUpdateTimer.start();
    }

    public boolean onKeyPressed(KeyEvent e) {
        boolean handled = false;
        switch(e.getKeyChar()){
            case 'q':
                rdStreerModeStopped.doClick();
                handled = true;
                break;
            case 's':
                rdStreerModeStraight.doClick(); // set the steering mode.
                btnRotateStraight.doClick(); // set the graphics to match the steering mode.
                handled = true;
                break;
            case 'a':
                rdStreerModeTurnAround.doClick();
                handled = true;
                break;
            case 'w':
                rdStreerModeSideToSide.doClick();
                handled = true;
                break;
            case 'e':
                rdStreerModeMouseFree.doClick();
                handled = true;
            break;
            case 'p':
                rdStreerModePivot.doClick();
                handled = true;
                break;
            case 'r':
                rdStreerModeFrontStear.doClick();
                handled = true;
                break;
            case 'h':
                mBtnSpeedFaster.doClick();
                handled = true;
                break;
            case 'b':
                mBtnSpeedSlower.doClick();
                handled = true;
                break;
            case 'f':
                btnRotateLeft.doClick();
                handled = true;
                break;
            case 'd':
                btnRotateStraight.doClick();
                handled = true;
                break;
            case 'g':
                btnRotateRight.doClick();
                handled = true;
                break;
            case 't':
                btnRotateForward.doClick();
                handled = true;
                break;
            case 'v':
                btnRotateBackward.doClick();
                handled = true;
                break;
            case 'n':
                mGroundPanel.setMouseHandedness("left");
                repaint();
                onMouseHandednessChange();
                handled = true;
                break;
            case 'm':
                mGroundPanel.setMouseHandedness("right");
                repaint();
                onMouseHandednessChange();
                handled = true;
                break;
            case '1': // this is a 'one'. 1 is hard to read (easy to confuse with small L).
                changeTroubleshootingDisplay(1);
                handled = true;
                break;
            case '2':
                changeTroubleshootingDisplay(2);
                handled = true;
                break;
            case '3':
                changeTroubleshootingDisplay(3);
                handled = true;
                break;
            case '4':
                changeTroubleshootingDisplay(4);
                handled = true;
                break;
        }
        return handled;
    }

    public Truck getTruck() {
        return mGroundPanel.getTruck();
    }
    
    public GroundPanel getGroundPanel() {
        return mGroundPanel;
    }

    public void setTruckSpeed(double speed) {
        getTruck().setSpeed(speed);
    }
    
    public void setVelocityLimit(double velocityLimit) {
        mPrivVelocityLimit = velocityLimit;
        updateVelocityLimitLabel();
        mGroundPanel.setVelocityLimit(mPrivVelocityLimit);
    }
    
    public void setVelocityIncrement(double velocityIncrement) {
        mVelocityLimitIncrement = velocityIncrement;
        mVelocityLimitIncrementScrollBar.setValue((int)velocityIncrement);
        updateVelocityIncrementLabel();
    }

    private void changeTroubleshootingDisplay(int wheel_ID) {
        mGroundPanel.getTruck().changeTroubleshootingDisplay(wheel_ID);
    }
    
    public void addForwardAngle(double angle) {
        getTruck().addForwardAngleRaw(angle); // for GUI display, cumulative
        getTruck().addForwardAngle(angle);
        onForwardAngleChange(angle);
    }
    
    public void rotateForward(int direction) {
        addForwardAngle(direction*FORWARD_ANGLE_STEP*mForwardAngleMult);
    }
    
    public void setForwardAngleMultiplier(double angleMultiplier) {
        mForwardAngleMult = angleMultiplier;
        mScrbForwardAngleMult.setValue((int)angleMultiplier);
    }

    public void setGroundPanelMousePosRelToTruck(Point.Double pos) {
        mGroundPanel.setMousePosRelToTruck(pos);
    }

    public void addGroudPanelMousePosListener(XYDoubleListener listener) {
        mGroundPanel.addMousePosListener(listener);
    }

    public void addSteeringModeListener(StringListener listener) {
        if (mSteeringListeners.indexOf(listener) < 0 ) {
            mSteeringListeners.add(listener);
        }
    }

    public void addSteerDirectionListener(StringListener listener) {
        if (mSteerDirectionListeners.indexOf(listener) < 0 ) {
            mSteerDirectionListeners.add(listener);
        }
    }

    public void addForwardDistanceListener(DoubleVarArgListener listener) {
        if (mForwardAngleListeners.indexOf(listener) < 0 ) {
            mForwardAngleListeners.add(listener);
        }
    }

    public void addSpeedListener(DoubleVarArgListener listener) {
        if (mSpeedListeners.indexOf(listener) < 0 ) {
            mSpeedListeners.add(listener);
        }
    }

    public void addMouseHandednessListener(StringListener listener) {
        if (mMouseHandednessListeners.indexOf(listener) < 0 ) {
            mMouseHandednessListeners.add(listener);
        }
    }

    public void addTruckScaleListener(DoubleListener listener) {
        if (mTruckScaleListeners.indexOf(listener) < 0 ) {
            mTruckScaleListeners.add(listener);
        }
    }
    
    public void setTruckScale(double scale) {
        mGroundPanel.setDrawScale(scale);
        mGroundPanel.repaint();
        //jSlider1Mouse.setValue((int) scale);
        //mLabelRoverScale.setText(String.format(
        //        "Rover Scale Setting (%.2f)", getTruck().getDrawScale()));
    }

    
    public void setDirectionMode(String directionMode) {
        System.err.println(directionMode);
        switch(directionMode) {
            case "lft":
                btnRotateLeft.doClick();
                break;
            case "rgt":
                btnRotateRight.doClick();
                break;
            case "str":
                btnRotateStraight.doClick();
                break;
        }
    }
    
    /**
     * This function is used to act as if user clicks
     * steering radio options;
    */
    public void setSteeringMode(String steeringMode) {
        //System.err.println("steeringMode= "+steeringMode);//+" ; this was fixed in 2019?? lft and rgt (o and i) come into here incorrectly and cause error - coming in from UI commands headed toward rover - also rover doesn't respond to these commands where it should");
        switch(SteeringMode.getByName(steeringMode)) {
            case NONE:
                rdStreerModeStopped.doClick();
                break;
            case STOPPED:
                rdStreerModeStopped.doClick();
                break;            
            case MOUSE_FREE:
                rdStreerModeMouseFree.doClick();
                break;
            case PIVOT:
                rdStreerModePivot.doClick();
                break;
            case FRONT_STEER:
                rdStreerModeFrontStear.doClick();
                break;
            case SIDE_TO_SIDE:
                rdStreerModeSideToSide.doClick();
                break;
            case STRAIGHT:
                rdStreerModeStraight.doClick();
                break;
            case TURN_AROUND:
                rdStreerModeTurnAround.doClick();
                break;
        }
    }

    /**
     * This function transfers the steeringMode to GroundPanel.
     * And also fires the listeners.
     * It should be called from within radio button click event.
     */
    public void onSteeringModeChange(String steeringMode) {
        mGroundPanel.setTruckStreeMode(steeringMode);
        mSteeringListeners.forEach((listener) -> {
            listener.onChange(steeringMode);
        });
    }

    public void setBatchTime(String Batch_time_stamp_into_mysql){
        mBatch_time_stamp_into_mysql = Batch_time_stamp_into_mysql;
        //System.out.println("mBatch_time_stamp_into_mysql "+mBatch_time_stamp_into_mysql);
    }

    public void updateLagProgress(long lag)  {
        int lagInt;
//            try {
//                lagInt = Math.toIntExact(lag);
//            } catch (ArithmeticException ex) {
//                lagInt = mBarHeartBeatLag.getMaximum();
//            }
            
        if (lag>1200) {
            try {
                lagInt = Math.toIntExact(lag);
            } catch (ArithmeticException ex) {
                lagInt = mBarHeartBeatLag.getMaximum();
            }
            mBarHeartBeatLag.setValue(lagInt+1);
            mLblHeatBeat.setText(""+lagInt);
            if (mAllowMySqlLogging) {
                MysqlLogger.put(MysqlLogger.Type.INSERT, lag,  "lagTimeFromUI_to_Rover", mBatch_time_stamp_into_mysql, "lagTime");
            }
        } else {
            mBarHeartBeatLag.setValue((int)500);
            mLblHeatBeat.setText(""+(int)500);
//            mBarHeartBeatLag.setValue(lagInt+1);
//            mLblHeatBeat.setText(""+lagInt);
        }

//        Double addThis = (double)lag;
//        avgLagTime.add(0,addThis);
//        //System.out.println("array size = "+avgLagTime.size());
//        grandTotalLagTime1000Instances = grandTotalLagTime1000Instances + lag;
//        if ((avgLagTime.size())>1000){
//            double removeFromTotal = (double)(avgLagTime.get(1000));
//            grandTotalLagTime1000Instances = grandTotalLagTime1000Instances - removeFromTotal;
//            double averageValue = (double)(grandTotalLagTime1000Instances/1000);
//            //System.out.println(averageValue+" .....  "+removeFromTotal+" .... "+(double)(avgLagTime.get(500)));
//            jLabel3.setText(""+averageValue);
//            avgLagTime.remove(1000);
//        }
    }


    /**
     * And also fires the listeners of Speeds.
     * It should be called from within radio button click event.
     */
    public void onSpeedChange() {
        mSpeedListeners.forEach((listener) -> {
            listener.onChange(
                    mGroundPanel.getTruck().getSpeed(),
                    mPrivVelocityLimit, mVelocityLimitIncrement);
        });
    }
    
    public void updateVelocityLimitLabel() {
        // this is the manually set velocity limit to set max velocity
        mLblVelocityLimitValue.setText(
                String.format("%.1f", mPrivVelocityLimit)); 
    }
    
    public void updateVelocityIncrementLabel() {
        // this is the manually set velocity limit to set max velocity
        mLblVelocityIncreValue.setText(
                String.format("%.1f", mVelocityLimitIncrement)); 
    }

    public void setLabel_mLblForwardAngleValue(double value) {
        mLblForwardAngleValue.setText(String.format("%.1f", value)); // this is the incremented value 10, 20, 30, etc
    }
    
    
    private void updateForwardAngleMultiplier() {
        mTxtForwardAngleMult.setText(mForwardAngleMult+"");
    }
    
    public void setMousePosFromCenter(double xPos, double yPos) {
        mGroundPanel.setMousePosFromCenter(xPos, yPos);
    }
    
    public void setMousePosAngle(double angleRad) {
        mGroundPanel.setMousePosAngle(angleRad);
    }

    public void setLabel_LeftPotentiameterANDmaxDutyCycle(double potValue, double newAngleLeftBasedOnPot, double maxDutyCycle){
        jTextFieldLeftPot.setText(String.format("%.3f", potValue*100));
        jTextFieldLeftPot.setOpaque(true);
        jTextFieldLeftPot.setBackground(Color.green);

        angleLeftBasedOnPot = newAngleLeftBasedOnPot;
        jTextFieldAngleBasedOnLeftPot.setText(String.format("%02.1f",(angleLeftBasedOnPot))+"");
        jTextFieldAngleBasedOnLeftPot.setOpaque(true);

        jTextFieldLeftSteeringMaxDutyCycle.setText(maxDutyCycle+"");
        jTextFieldLeftSteeringMaxDutyCycle.setOpaque(true);
        jTextFieldLeftSteeringMaxDutyCycle.setBackground(Color.green);
        if(maxDutyCycle>750){
            jTextFieldLeftSteeringMaxDutyCycle.setBackground(Color.red);        
        }
    }
    
    public void setLabel_RightPotentiameterANDmaxDutyCycle(double potValue, double newAngleRightBasedOnPot, double maxDutyCycle){
        jTextFieldRightPot.setText(String.format("%.3f", potValue*100));
        jTextFieldRightPot.setOpaque(true);
        jTextFieldRightPot.setBackground(Color.green);

        angleRightBasedOnPot = newAngleRightBasedOnPot;
        jTextFieldAngleBasedOnRightPot.setText(String.format("%02.1f",(angleRightBasedOnPot))+"");
        jTextFieldAngleBasedOnRightPot.setOpaque(true);

        jTextFieldRightSteeringMaxDutyCycle.setText(maxDutyCycle+"");
        jTextFieldRightSteeringMaxDutyCycle.setOpaque(true);
        jTextFieldRightSteeringMaxDutyCycle.setBackground(Color.green);
        if(maxDutyCycle>750){
            jTextFieldRightSteeringMaxDutyCycle.setBackground(Color.red);        
        }
    }
    
    public void setLabel_jLabel_ElectricalCurrent(double value){
        jLabel_electicalCurrent.setText(String.format("%.2f", value));
    }
    
    private void onDirectionChange(String dir) {
        setLabel_mLblForwardAngleValue(getTruck().getForwardAngleRaw());
        mSteerDirectionListeners.forEach((listener) -> {
            listener.onChange(dir);
        });
    }
    
    private void onForwardAngleChange(double angleChange) {
        setLabel_mLblForwardAngleValue(getTruck().getForwardAngleRaw());
        //mLblForwardAngleValue.setText(String.format("%.5f", getTruck().getForwardAngle())); // this is the incremented value 10, 20, 30, etc
        mForwardAngleListeners.forEach((listener) -> {
            //listener.onChange(angleChange);
            listener.onChange(angleChange, mForwardAngleMult);
        });
    }

    /**
     * And also fires the listeners of changing mouse handedness.
     * It should be called from within radio button click(?TODO) event.
     */
    public void onMouseHandednessChange() {
        mMouseHandednessListeners.forEach((listener) -> {
            listener.onChange(mGroundPanel.getMouseHandedness());
        });
    }

    public void listenX() {
                JFrame frame = new JFrame();
                JPanel panel = new JPanel();
                frame.setTitle("key listener frame & panel - click in the panel below and press a key");
                panel.setToolTipText("s = 'Straight'; a = 'TurnAround'; d = 'SideToSide';  e = 'Pivot'; r = 'RearSteer' ");

//  using one of these options should be better than tooltiptext - want the user to be able to see which letters to push to switch modes                
//                JTextArea keyCodes = new JTextArea(10,50);
//                keyCodes.append("s = 'Straight'; a = 'TurnAround'; d = 'SideToSide';  e = 'Pivot'; r = 'RearSteer' ");
//                keyCodes.setVisible(true);
//                keyCodes.setText("s = 'Straight'; a = 'TurnAround'; d = 'SideToSide';  e = 'Pivot'; r = 'RearSteer' ");
                
                frame.setLocation(800, 20);

                frame.getContentPane().add(panel);

                panel.addKeyListener(new KeyListener() {

                    @Override
                    public void keyTyped(KeyEvent e) {}

                    @Override
                    public void keyReleased(KeyEvent e) {}

                    @Override
                    public void keyPressed(KeyEvent e) {
                        System.out.println("Pressed " + e.getKeyChar());
                        switch(e.getKeyChar()){
                            case 'a':
                                System.out.println("define steering mode: a = 'TurnAround' ");
                                rdStreerModeTurnAround.doClick();
                                break;

                            case 's':
                                System.out.println("define steering mode: s = 'Straight' ");
                                mGroundPanel.setTruckStreeMode("Straight");
                                rdStreerModeStraight.doClick();
                                break;

                            case 'w':
                                System.out.println("define steering mode: w = 'SideToSide' ");
                                rdStreerModeSideToSide.doClick();
                                break;

                            case 'e':
                                System.out.println("define steering mode: e = 'MouseFree' ");
                                rdStreerModeMouseFree.doClick();
                                break;

                            case 'r':
                                System.out.println("define steering mode: r = 'FrontSteer' ");
                                rdStreerModeFrontStear.doClick();
                                break;

                            case 'p':
                                System.out.println("define steering mode: p = 'Pivot' ");
                                rdStreerModePivot.doClick();
                                break;
                        }
                    }
                });

                panel.setFocusable(true);
                panel.requestFocusInWindow();

                frame.setSize(new Dimension(700, 300));
                frame.setVisible(true);
            }
    
    
    public void clickRotateLeftButton() {
        btnRotateLeft.doClick();
    }
    
    public void clickRotateRightButton() {
        btnRotateRight.doClick();
    }
    
    public void clickRotateForwardButton() {
        btnRotateForward.doClick();
    }
    
    public void clickRotateBackwardButton() {
        btnRotateBackward.doClick();
    }
    
    public void clickSpeedFasterButton() {
        mBtnSpeedFaster.doClick();
    }
    
    public void clickSpeedSlowerButton() {
        mBtnSpeedSlower.doClick();
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

        btnGroupSteerMode = new javax.swing.ButtonGroup();
        mStreerModePanel = new javax.swing.JPanel();
        rdStreerModeStopped = new javax.swing.JRadioButton();
        rdStreerModeMouseFree = new javax.swing.JRadioButton();
        rdStreerModeStraight = new javax.swing.JRadioButton();
        rdStreerModeTurnAround = new javax.swing.JRadioButton();
        rdStreerModeSideToSide = new javax.swing.JRadioButton();
        rdStreerModePivot = new javax.swing.JRadioButton();
        rdStreerModeFrontStear = new javax.swing.JRadioButton();
        mWheelMoverPanel = new javax.swing.JPanel();
        btnRotateLeft = new javax.swing.JButton();
        btnRotateStraight = new javax.swing.JButton();
        btnRotateRight = new javax.swing.JButton();
        btnRotateForward = new javax.swing.JButton();
        btnRotateBackward = new javax.swing.JButton();
        mLblForwardAngleName = new javax.swing.JLabel();
        mLblForwardAngleValue = new javax.swing.JLabel();
        mTxtForwardAngleMult = new javax.swing.JTextField();
        mScrbForwardAngleMult = new javax.swing.JScrollBar();
        mLblForwardAngleName1 = new javax.swing.JLabel();
        mVelocityControllerPanel = new javax.swing.JPanel();
        mBtnSpeedFaster = new javax.swing.JButton();
        mBtnSpeedSlower = new javax.swing.JButton();
        mLblVelocityLimitName = new javax.swing.JLabel();
        mLblVelocityLimitValue = new javax.swing.JLabel();
        mLblVelocityIncreName = new javax.swing.JLabel();
        mLblVelocityIncreValue = new javax.swing.JLabel();
        mVelocityLimitIncrementScrollBar = new javax.swing.JScrollBar();
        mGroundPanel = new RoverUI.GroundPanel();
        mLblAverage = new javax.swing.JLabel();
        mLblCurrent = new javax.swing.JLabel();
        jLabel_electicalCurrent = new javax.swing.JLabel();
        mBarHeartBeatLag = new javax.swing.JProgressBar();
        mLblHeatBeat = new javax.swing.JLabel();
        jTextFieldLeftPot = new javax.swing.JTextField();
        jTextFieldRightPot = new javax.swing.JTextField();
        jTextFieldRightSteeringMaxDutyCycle = new javax.swing.JTextField();
        jTextFieldLeftSteeringMaxDutyCycle = new javax.swing.JTextField();
        jTextFieldAngleBasedOnRightPot = new javax.swing.JTextField();
        jTextFieldAngleBasedOnLeftPot = new javax.swing.JTextField();

        setPreferredSize(new java.awt.Dimension(860, 702));
        setLayout(new java.awt.GridBagLayout());

        mStreerModePanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        mStreerModePanel.setMinimumSize(new java.awt.Dimension(0, 35));
        mStreerModePanel.setOpaque(false);
        mStreerModePanel.setPreferredSize(new java.awt.Dimension(600, 35));

        btnGroupSteerMode.add(rdStreerModeStopped);
        rdStreerModeStopped.setSelected(true);
        rdStreerModeStopped.setText("Stopped");
        rdStreerModeStopped.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rdStreerModeStoppedActionPerformed(evt);
            }
        });
        mStreerModePanel.add(rdStreerModeStopped);

        btnGroupSteerMode.add(rdStreerModeMouseFree);
        rdStreerModeMouseFree.setText("Mouse Free");
        rdStreerModeMouseFree.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rdStreerModeMouseFreeActionPerformed(evt);
            }
        });
        mStreerModePanel.add(rdStreerModeMouseFree);

        btnGroupSteerMode.add(rdStreerModeStraight);
        rdStreerModeStraight.setText("Straight");
        rdStreerModeStraight.setToolTipText("s");
        rdStreerModeStraight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rdStreerModeStraightActionPerformed(evt);
            }
        });
        mStreerModePanel.add(rdStreerModeStraight);

        btnGroupSteerMode.add(rdStreerModeTurnAround);
        rdStreerModeTurnAround.setText("Turn Around");
        rdStreerModeTurnAround.setEnabled(false);
        rdStreerModeTurnAround.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rdStreerModeTurnAroundActionPerformed(evt);
            }
        });
        mStreerModePanel.add(rdStreerModeTurnAround);

        btnGroupSteerMode.add(rdStreerModeSideToSide);
        rdStreerModeSideToSide.setText("Side to Side");
        rdStreerModeSideToSide.setEnabled(false);
        rdStreerModeSideToSide.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rdStreerModeSideToSideActionPerformed(evt);
            }
        });
        mStreerModePanel.add(rdStreerModeSideToSide);

        btnGroupSteerMode.add(rdStreerModePivot);
        rdStreerModePivot.setText("Pivot");
        rdStreerModePivot.setEnabled(false);
        rdStreerModePivot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rdStreerModePivotActionPerformed(evt);
            }
        });
        mStreerModePanel.add(rdStreerModePivot);

        btnGroupSteerMode.add(rdStreerModeFrontStear);
        rdStreerModeFrontStear.setText("Front Steer");
        rdStreerModeFrontStear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rdStreerModeFrontStearActionPerformed(evt);
            }
        });
        mStreerModePanel.add(rdStreerModeFrontStear);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 720;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 1, 0, 12);
        add(mStreerModePanel, gridBagConstraints);

        mWheelMoverPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        mWheelMoverPanel.setPreferredSize(new java.awt.Dimension(100, 50));

        btnRotateLeft.setText("rotate left");
        btnRotateLeft.setToolTipText("f = rotate left");
        btnRotateLeft.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnRotateLeft.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRotateLeftActionPerformed(evt);
            }
        });

        btnRotateStraight.setText("straight");
        btnRotateStraight.setToolTipText("d = straight");
        btnRotateStraight.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnRotateStraight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRotateStraightActionPerformed(evt);
            }
        });

        btnRotateRight.setText("rotate right");
        btnRotateRight.setToolTipText("g = rotate right");
        btnRotateRight.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnRotateRight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRotateRightActionPerformed(evt);
            }
        });

        btnRotateForward.setText("rotate forward");
        btnRotateForward.setToolTipText("h = forward");
        btnRotateForward.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnRotateForward.setMaximumSize(new java.awt.Dimension(85, 25));
        btnRotateForward.setMinimumSize(new java.awt.Dimension(85, 25));
        btnRotateForward.setPreferredSize(new java.awt.Dimension(85, 25));
        btnRotateForward.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRotateForwardActionPerformed(evt);
            }
        });

        btnRotateBackward.setText("rotate backward");
        btnRotateBackward.setToolTipText("b = backward");
        btnRotateBackward.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnRotateBackward.setMaximumSize(new java.awt.Dimension(85, 25));
        btnRotateBackward.setMinimumSize(new java.awt.Dimension(85, 25));
        btnRotateBackward.setPreferredSize(new java.awt.Dimension(85, 25));
        btnRotateBackward.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRotateBackwardActionPerformed(evt);
            }
        });

        mLblForwardAngleName.setText("Forward Rotation Angle:");
        mLblForwardAngleName.setOpaque(true);

        mLblForwardAngleValue.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        mLblForwardAngleValue.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        mLblForwardAngleValue.setText("0.0");
        mLblForwardAngleValue.setOpaque(true);

        mTxtForwardAngleMult.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        mTxtForwardAngleMult.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        mTxtForwardAngleMult.setText("5");

        mScrbForwardAngleMult.setBlockIncrement(1);
        mScrbForwardAngleMult.setMaximum(48);
        mScrbForwardAngleMult.setMinimum(1);
        mScrbForwardAngleMult.setUnitIncrement(5);
        mScrbForwardAngleMult.setValue(10);
        mScrbForwardAngleMult.setVisibleAmount(8);
        mScrbForwardAngleMult.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        mScrbForwardAngleMult.setPreferredSize(new java.awt.Dimension(17, 10));
        mScrbForwardAngleMult.addAdjustmentListener(new java.awt.event.AdjustmentListener() {
            public void adjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {
                mScrbForwardAngleMultAdjustmentValueChanged(evt);
            }
        });

        mLblForwardAngleName1.setText("Fwd Rot. Multiplier:");
        mLblForwardAngleName1.setOpaque(true);

        javax.swing.GroupLayout mWheelMoverPanelLayout = new javax.swing.GroupLayout(mWheelMoverPanel);
        mWheelMoverPanel.setLayout(mWheelMoverPanelLayout);
        mWheelMoverPanelLayout.setHorizontalGroup(
            mWheelMoverPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, mWheelMoverPanelLayout.createSequentialGroup()
                .addGroup(mWheelMoverPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mWheelMoverPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(btnRotateLeft, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnRotateStraight, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnRotateRight, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(mWheelMoverPanelLayout.createSequentialGroup()
                        .addGroup(mWheelMoverPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(mWheelMoverPanelLayout.createSequentialGroup()
                                .addGap(31, 31, 31)
                                .addGroup(mWheelMoverPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(mLblForwardAngleName1)
                                    .addComponent(mLblForwardAngleName))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(mWheelMoverPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(mLblForwardAngleValue, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(mTxtForwardAngleMult, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mWheelMoverPanelLayout.createSequentialGroup()
                                .addComponent(btnRotateForward, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnRotateBackward, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(18, 18, 18)
                        .addComponent(mScrbForwardAngleMult, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(20, 20, 20))
        );
        mWheelMoverPanelLayout.setVerticalGroup(
            mWheelMoverPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mWheelMoverPanelLayout.createSequentialGroup()
                .addGroup(mWheelMoverPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnRotateLeft, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnRotateStraight, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnRotateRight, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(mWheelMoverPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mWheelMoverPanelLayout.createSequentialGroup()
                        .addGroup(mWheelMoverPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnRotateBackward, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnRotateForward, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(mWheelMoverPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(mWheelMoverPanelLayout.createSequentialGroup()
                                .addComponent(mLblForwardAngleName, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(mLblForwardAngleName1, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(mWheelMoverPanelLayout.createSequentialGroup()
                                .addComponent(mLblForwardAngleValue, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(mTxtForwardAngleMult, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(32, 32, 32))
                    .addGroup(mWheelMoverPanelLayout.createSequentialGroup()
                        .addGap(33, 33, 33)
                        .addComponent(mScrbForwardAngleMult, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        add(mWheelMoverPanel, gridBagConstraints);

        mVelocityControllerPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        mVelocityControllerPanel.setToolTipText("h = speed faster; b = speed slower.");

        mBtnSpeedFaster.setText("speed faster");
        mBtnSpeedFaster.setToolTipText("Be Sure the STOPPED button is not clicked.  h = speed faster");
        mBtnSpeedFaster.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBtnSpeedFasterActionPerformed(evt);
            }
        });

        mBtnSpeedSlower.setText("speed slower");
        mBtnSpeedSlower.setToolTipText("Be Sure the STOPPED button is not clicked.  b = speed slower");
        mBtnSpeedSlower.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBtnSpeedSlowerActionPerformed(evt);
            }
        });

        mLblVelocityLimitName.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        mLblVelocityLimitName.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        mLblVelocityLimitName.setText("Vel. Limit:");

        mLblVelocityLimitValue.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        mLblVelocityLimitValue.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        mLblVelocityLimitValue.setText("000.0");

        mLblVelocityIncreName.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        mLblVelocityIncreName.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        mLblVelocityIncreName.setText("Vel. Increment:");

        mLblVelocityIncreValue.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        mLblVelocityIncreValue.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        mLblVelocityIncreValue.setText("000.0");

        mVelocityLimitIncrementScrollBar.setMaximum(250);
        mVelocityLimitIncrementScrollBar.setMinimum(1);
        mVelocityLimitIncrementScrollBar.setToolTipText("");
        mVelocityLimitIncrementScrollBar.setUnitIncrement(5);
        mVelocityLimitIncrementScrollBar.setValue(25);
        mVelocityLimitIncrementScrollBar.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        mVelocityLimitIncrementScrollBar.addAdjustmentListener(new java.awt.event.AdjustmentListener() {
            public void adjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {
                mVelocityLimitIncrementScrollBarAdjustmentValueChanged(evt);
            }
        });

        javax.swing.GroupLayout mVelocityControllerPanelLayout = new javax.swing.GroupLayout(mVelocityControllerPanel);
        mVelocityControllerPanel.setLayout(mVelocityControllerPanelLayout);
        mVelocityControllerPanelLayout.setHorizontalGroup(
            mVelocityControllerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mVelocityControllerPanelLayout.createSequentialGroup()
                .addGroup(mVelocityControllerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mVelocityControllerPanelLayout.createSequentialGroup()
                        .addComponent(mBtnSpeedFaster, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(mBtnSpeedSlower, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(mVelocityControllerPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(mVelocityControllerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(mLblVelocityIncreName, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(mLblVelocityLimitName, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(mVelocityControllerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(mLblVelocityLimitValue, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(mLblVelocityIncreValue, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(mVelocityLimitIncrementScrollBar, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(20, 20, 20))
        );
        mVelocityControllerPanelLayout.setVerticalGroup(
            mVelocityControllerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mVelocityControllerPanelLayout.createSequentialGroup()
                .addGroup(mVelocityControllerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(mBtnSpeedFaster, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(mBtnSpeedSlower, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(mVelocityControllerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mVelocityControllerPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(mVelocityControllerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(mLblVelocityLimitName)
                            .addComponent(mLblVelocityLimitValue))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(mVelocityControllerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(mLblVelocityIncreValue, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(mLblVelocityIncreName)))
                    .addGroup(mVelocityControllerPanelLayout.createSequentialGroup()
                        .addGap(32, 32, 32)
                        .addComponent(mVelocityLimitIncrementScrollBar, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
        add(mVelocityControllerPanel, gridBagConstraints);

        mGroundPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        mGroundPanel.setMinimumSize(new java.awt.Dimension(700, 485));
        mGroundPanel.setPreferredSize(new java.awt.Dimension(700, 485));

        mLblAverage.setText("Average");
        mLblAverage.setOpaque(true);

        mLblCurrent.setText("Current:");
        mLblCurrent.setOpaque(true);

        jLabel_electicalCurrent.setBackground(new java.awt.Color(255, 255, 255));
        jLabel_electicalCurrent.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel_electicalCurrent.setText("Electrical Current");
        jLabel_electicalCurrent.setOpaque(true);

        mBarHeartBeatLag.setMaximum(4000);
        mBarHeartBeatLag.setOrientation(1);

        mLblHeatBeat.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        mLblHeatBeat.setText("Lag from rover to UI");
        mLblHeatBeat.setOpaque(true);

        jTextFieldLeftPot.setText("Left Pot");
        jTextFieldLeftPot.setMaximumSize(new java.awt.Dimension(6, 28));
        jTextFieldLeftPot.setMinimumSize(new java.awt.Dimension(6, 28));

        jTextFieldRightPot.setText("Right Pot");
        jTextFieldRightPot.setMaximumSize(new java.awt.Dimension(6, 28));
        jTextFieldRightPot.setMinimumSize(new java.awt.Dimension(6, 28));

        jTextFieldRightSteeringMaxDutyCycle.setText("MaxSteerDC");
        jTextFieldRightSteeringMaxDutyCycle.setToolTipText("Shows duty cycle for motors that steer front wheels.");
        jTextFieldRightSteeringMaxDutyCycle.setMaximumSize(new java.awt.Dimension(6, 20));

        jTextFieldLeftSteeringMaxDutyCycle.setText("MaxSteerDC");
        jTextFieldLeftSteeringMaxDutyCycle.setToolTipText("Shows duty cycle for motors that steer front wheels.");
        jTextFieldLeftSteeringMaxDutyCycle.setMaximumSize(new java.awt.Dimension(6, 20));

        jTextFieldAngleBasedOnRightPot.setText("Steering Angle");
        jTextFieldAngleBasedOnRightPot.setToolTipText("Steering Angle (in degrees).  This will read zero when heading straight forward. Set the default \"startingTargetPotPositionFrontRight\" </br>\nvalues in RoverFrontEnd.java if the reading is not zero when facing forward. Or use the autocalibrate.");
        jTextFieldAngleBasedOnRightPot.setMaximumSize(new java.awt.Dimension(6, 20));

        jTextFieldAngleBasedOnLeftPot.setText("Steering Angle");
        jTextFieldAngleBasedOnLeftPot.setToolTipText("Steering Angle (in degrees).  This will read zero when heading straight forward. Set the default \"startingTargetPotPositionFrontLeft\" </br>\nvalues in RoverFrontEnd.java if the reading is not zero when facing forward. Or use the autocalibrate.");
        jTextFieldAngleBasedOnLeftPot.setMaximumSize(new java.awt.Dimension(6, 20));

        javax.swing.GroupLayout mGroundPanelLayout = new javax.swing.GroupLayout(mGroundPanel);
        mGroundPanel.setLayout(mGroundPanelLayout);
        mGroundPanelLayout.setHorizontalGroup(
            mGroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mGroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mGroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mGroundPanelLayout.createSequentialGroup()
                        .addGap(2, 2, 2)
                        .addComponent(mLblCurrent, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(26, 26, 26)
                        .addComponent(jTextFieldLeftSteeringMaxDutyCycle, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(mGroundPanelLayout.createSequentialGroup()
                        .addGroup(mGroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(mBarHeartBeatLag, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(mLblAverage, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(mLblHeatBeat, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel_electicalCurrent, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(10, 10, 10)
                        .addGroup(mGroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jTextFieldAngleBasedOnLeftPot, javax.swing.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)
                            .addComponent(jTextFieldLeftPot, javax.swing.GroupLayout.DEFAULT_SIZE, 50, Short.MAX_VALUE))))
                .addGap(300, 300, 300)
                .addGroup(mGroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jTextFieldRightPot, javax.swing.GroupLayout.DEFAULT_SIZE, 60, Short.MAX_VALUE)
                    .addComponent(jTextFieldAngleBasedOnRightPot, javax.swing.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jTextFieldRightSteeringMaxDutyCycle, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(233, Short.MAX_VALUE))
        );
        mGroundPanelLayout.setVerticalGroup(
            mGroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mGroundPanelLayout.createSequentialGroup()
                .addGroup(mGroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, mGroundPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(mLblCurrent, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jTextFieldLeftSteeringMaxDutyCycle, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jTextFieldLeftPot, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, mGroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jTextFieldRightSteeringMaxDutyCycle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jTextFieldRightPot, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGroup(mGroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mGroundPanelLayout.createSequentialGroup()
                        .addGroup(mGroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jTextFieldAngleBasedOnLeftPot, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextFieldAngleBasedOnRightPot, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(mGroundPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jLabel_electicalCurrent, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(57, 57, 57)
                        .addComponent(mBarHeartBeatLag, javax.swing.GroupLayout.PREFERRED_SIZE, 179, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(mLblHeatBeat, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(mLblAverage, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(97, 97, 97))))
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 0, 2);
        add(mGroundPanel, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void rdStreerModeStoppedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rdStreerModeStoppedActionPerformed
        onSteeringModeChange(rdStreerModeStopped.getText());
        StoppedFlag="Stopped";
    }//GEN-LAST:event_rdStreerModeStoppedActionPerformed

    private void rdStreerModeStraightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rdStreerModeStraightActionPerformed
       onSteeringModeChange(rdStreerModeStraight.getText());
       StoppedFlag="NotStopped";
//        repaint();
    }//GEN-LAST:event_rdStreerModeStraightActionPerformed

    private void rdStreerModeTurnAroundActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rdStreerModeTurnAroundActionPerformed
        onSteeringModeChange(rdStreerModeTurnAround.getText());
    }//GEN-LAST:event_rdStreerModeTurnAroundActionPerformed

    private void rdStreerModeSideToSideActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rdStreerModeSideToSideActionPerformed
        onSteeringModeChange(rdStreerModeSideToSide.getText());
        StoppedFlag="NotStopped";
    }//GEN-LAST:event_rdStreerModeSideToSideActionPerformed

    private void rdStreerModePivotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rdStreerModePivotActionPerformed
        onSteeringModeChange(rdStreerModePivot.getText());
        StoppedFlag="NotStopped";
    }//GEN-LAST:event_rdStreerModePivotActionPerformed

    private void rdStreerModeFrontStearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rdStreerModeFrontStearActionPerformed
        onSteeringModeChange(rdStreerModeFrontStear.getText());
        StoppedFlag="NotStopped";
    }//GEN-LAST:event_rdStreerModeFrontStearActionPerformed

    private void btnRotateRightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRotateRightActionPerformed
        doRotateRight();
    }//GEN-LAST:event_btnRotateRightActionPerformed

    public void doRotateRight() {
        getTruck().increaseWheelsAngle(0);//mWheelVerticalAngleIncrement);
        String dir = "rgt"; // rgt
        onDirectionChange(dir);
        // mouseHypotheticalX = mouseHypotheticalX + mousePosHypotheticalIncrement;
        //System.err.println("mouseHypotheticalX "+mouseHypotheticalX);
        mGroundPanel.setHypotheticalMousePos(
                mGroundPanel.getMousePosX() + mousePosHypotheticalIncrement,
                mGroundPanel.getSteerCircleEdgeY()
        ); // this will allow us to use the button as a proxy for the mouse - need to set it up for increments
        repaint();
    }
    
    private void btnRotateLeftActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRotateLeftActionPerformed
        doRotateLeft();
    }//GEN-LAST:event_btnRotateLeftActionPerformed

    public void doRotateLeft() {
        getTruck().increaseWheelsAngle(0);//-mWheelVerticalAngleIncrement);
        String dir = "lft"; // lft
        onDirectionChange(dir);
        // mouseHypotheticalX = mouseHypotheticalX - mousePosHypotheticalIncrement;
        // System.err.println("mouseHypotheticalX "+mouseHypotheticalX);
        mGroundPanel.setHypotheticalMousePos(
                mGroundPanel.getMousePosX() - mousePosHypotheticalIncrement,
                mGroundPanel.getSteerCircleEdgeY()
        ); // this will allow us to use the button as a proxy for the mouse - need to set it up for increments
        repaint();;
    }
    /**
     * sets the rotation (left / right) position of the front wheels to straight
     * forward by moving the HypotheticalMousePos to an appropriate point
     * wrt the computer monitor being used ("UI" computer or other computer).
     */
    public void setRotateToStraight(){
        getTruck().setWheelsAngle(0);
        String dir = "str";
        onDirectionChange(dir);
        // Sets the mouse position to the middle of the sceeen.
        mouseHypotheticalX = (int) (mGroundPanel.getWidth() * 0.5);
        mGroundPanel.setHypotheticalMousePos(
                mouseHypotheticalX,
                mGroundPanel.getSteerCircleEdgeY()
        ); // this will allow us to use the button as a proxy for the mouse - need to set it up for increments
        repaint();
    }
    
    private void btnRotateStraightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRotateStraightActionPerformed
        setRotateToStraight();
    }//GEN-LAST:event_btnRotateStraightActionPerformed
    static private final double FORWARD_ANGLE_STEP = 25;
    
    private void btnRotateForwardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRotateForwardActionPerformed
        //String timeS = new SimpleDateFormat("HH.mm.ss.SSS").format(new Date()); 
        //System.out.println( "timestamp - forward button pushed "+timeS);
        addForwardAngle(FORWARD_ANGLE_STEP*mForwardAngleMult);
    }//GEN-LAST:event_btnRotateForwardActionPerformed

    private void btnRotateBackwardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRotateBackwardActionPerformed
        addForwardAngle(-FORWARD_ANGLE_STEP*mForwardAngleMult);
    }//GEN-LAST:event_btnRotateBackwardActionPerformed

    private void rdStreerModeMouseFreeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rdStreerModeMouseFreeActionPerformed
        onSteeringModeChange(rdStreerModeMouseFree.getText());
        StoppedFlag="NotStopped";
    }//GEN-LAST:event_rdStreerModeMouseFreeActionPerformed

    private void mScrbForwardAngleMultAdjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {//GEN-FIRST:event_mScrbForwardAngleMultAdjustmentValueChanged
        mForwardAngleMult = mScrbForwardAngleMult.getValue();
        //System.out.println("mForwardAngleMult "+mForwardAngleMult);
        updateForwardAngleMultiplier();
        onForwardAngleChange(0);
    }//GEN-LAST:event_mScrbForwardAngleMultAdjustmentValueChanged

    private void mVelocityLimitIncrementScrollBarAdjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {//GEN-FIRST:event_mVelocityLimitIncrementScrollBarAdjustmentValueChanged
        mVelocityLimitIncrement = mVelocityLimitIncrementScrollBar.getValue();
        updateVelocityIncrementLabel();
        onSpeedChange();
    }//GEN-LAST:event_mVelocityLimitIncrementScrollBarAdjustmentValueChanged

    public void increaseSpeed(int multiplier) {
        mVelocityLimitIncrement = mVelocityLimitIncrementScrollBar.getValue();
        if (multiplier > 0) {
            mGroundPanel.addSpeed();
            mPrivVelocityLimit = mPrivVelocityLimit+mVelocityLimitIncrement;
        } else {
            mGroundPanel.reduceSpeed();
            mPrivVelocityLimit = mPrivVelocityLimit-mVelocityLimitIncrement;            
        }
        
        //System.err.println("new privVelocityLimit "+privVelocityLimit);
        mGroundPanel.setVelocityLimit(mPrivVelocityLimit);

        getTruck().addForwardAngleRaw(0); // for GUI display
        getTruck().addForwardAngle(0);
        onForwardAngleChange(0);

        updateVelocityLimitLabel();
        onSpeedChange();
    }
    
    private void mBtnSpeedSlowerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBtnSpeedSlowerActionPerformed
        if(StoppedFlag.equals("Stopped")){
            System.err.println("not doing this since stopped button is pushed");
        }
        else{
            increaseSpeed(-1);
        }
    }//GEN-LAST:event_mBtnSpeedSlowerActionPerformed

    private void mBtnSpeedFasterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBtnSpeedFasterActionPerformed
        if(StoppedFlag.equals("Stopped")){
            System.err.println("not doing this since stopped button is pushed");
        }
        else{
            increaseSpeed(1);
        }
    }//GEN-LAST:event_mBtnSpeedFasterActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup btnGroupSteerMode;
    private javax.swing.JButton btnRotateBackward;
    private javax.swing.JButton btnRotateForward;
    private javax.swing.JButton btnRotateLeft;
    private javax.swing.JButton btnRotateRight;
    private javax.swing.JButton btnRotateStraight;
    private javax.swing.JLabel jLabel_electicalCurrent;
    private javax.swing.JTextField jTextFieldAngleBasedOnLeftPot;
    private javax.swing.JTextField jTextFieldAngleBasedOnRightPot;
    private javax.swing.JTextField jTextFieldLeftPot;
    private javax.swing.JTextField jTextFieldLeftSteeringMaxDutyCycle;
    private javax.swing.JTextField jTextFieldRightPot;
    private javax.swing.JTextField jTextFieldRightSteeringMaxDutyCycle;
    private javax.swing.JProgressBar mBarHeartBeatLag;
    private javax.swing.JButton mBtnSpeedFaster;
    private javax.swing.JButton mBtnSpeedSlower;
    private RoverUI.GroundPanel mGroundPanel;
    private javax.swing.JLabel mLblAverage;
    private javax.swing.JLabel mLblCurrent;
    private javax.swing.JLabel mLblForwardAngleName;
    private javax.swing.JLabel mLblForwardAngleName1;
    private javax.swing.JLabel mLblForwardAngleValue;
    private javax.swing.JLabel mLblHeatBeat;
    private javax.swing.JLabel mLblVelocityIncreName;
    private javax.swing.JLabel mLblVelocityIncreValue;
    private javax.swing.JLabel mLblVelocityLimitName;
    private javax.swing.JLabel mLblVelocityLimitValue;
    private javax.swing.JScrollBar mScrbForwardAngleMult;
    private javax.swing.JPanel mStreerModePanel;
    private javax.swing.JTextField mTxtForwardAngleMult;
    private javax.swing.JPanel mVelocityControllerPanel;
    private javax.swing.JScrollBar mVelocityLimitIncrementScrollBar;
    private javax.swing.JPanel mWheelMoverPanel;
    private javax.swing.JRadioButton rdStreerModeFrontStear;
    private javax.swing.JRadioButton rdStreerModeMouseFree;
    private javax.swing.JRadioButton rdStreerModePivot;
    private javax.swing.JRadioButton rdStreerModeSideToSide;
    private javax.swing.JRadioButton rdStreerModeStopped;
    private javax.swing.JRadioButton rdStreerModeStraight;
    private javax.swing.JRadioButton rdStreerModeTurnAround;
    // End of variables declaration//GEN-END:variables

}

