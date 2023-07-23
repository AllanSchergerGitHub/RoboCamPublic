package RoboCam;

import Chart.ChartParamsDataset;
import DB.ConfigDB;
import GamePad.GamePadManager;
import PhiDevice.DeviceManager;
import PhiDevice.Electrical_Etc.Electrical;
import PhiDevice.Electrical_Etc.Potentiameters;
import PhiDevice.Electrical_Etc.RoboLights;
import RoverUI.Vehicle.SteeringMode;
import RoverUI.Vehicle.Truck;
import com.phidget22.PhidgetException;
import com.robocam.Socket.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.Thread.sleep;


/**
 * @author sujoy
 */
public class RoverFrontEnd extends javax.swing.JFrame {
    private SocketServer mSocketServer = null;
    private final ComPipe mComPipe = new ComPipe();
    private Thread mSocketServerThread = null;
    private final ComParser mComParser = new ComParser();
    private WheelDeviceSubClassAutoTrim[] mWheelDeviceSubClassAutoTrim = new WheelDeviceSubClassAutoTrim[2];

    private final MousePosCommand mRemoteMousePosCommand = new MousePosCommand();
    private final SteeringCommand mRemoteSteeringCommand = new SteeringCommand();
    private final DirectionCommand mDirectionCommand = new DirectionCommand();
    private final SpeedCommand mRemoteSpeedCommand = new SpeedCommand();
    private final ForwardAngleCommand mForwardAngleCommand = new ForwardAngleCommand();
    private final TruckScaleCommand mTruckScaleCommand = new TruckScaleCommand();
    private final MouseHandednessCommand mRemoteMouseHandednessCommand = new MouseHandednessCommand();
    private final ConfigDBParamCommand mRemoteConfigDBParamCommand = new ConfigDBParamCommand();
    private final CommonSensorCommand mCommonSensorCommand = new CommonSensorCommand();
    private WheelDeviceParamCommand[] mWheelDeviceParamCommands;
    private WheelSteeringAngleCommand mWheelSteeringAngleCommand = new WheelSteeringAngleCommand();
    private FloatSubCommand mFloatSubCommand = new FloatSubCommand();

    private double maxDutyFrontLeftSteeringMotor = 0;
    private double maxDutyFrontRightSteeringMotor = 0;

    private int sleepValue = 80;
    private DeviceManager mDeviceManager;

    private TruckDevice mTruckDevice;
    private double mTruckDevice_distanceRemainingRover;
    private double prior_mTruckDevice_distanceRemainingRover_value;
    private ConfigDB mConfigDB;

    private double startingTargetPotPositionFrontLeft = .7753; // the Pot reading when steering is straight ahead.
    private double targetPotPositionFrontLeft = 0;
    private double startingTargetPotPositionFrontRight = .5113; // the Pot reading when steering is straight ahead.
    private double targetPotPositionFrontRight = 0;

    boolean sendingManualSteer = true;

    Electrical mElectricalCurrent = null;
    Potentiameters mPotentiometerFrontLeft = null;
    Potentiameters mPotentiometerFrontRight = null;
    RoboLights mLight = null;

    int warning_counter = 0;
    int oldValue = 0;
    int changeSize = 0;
    double waitTime = 1;

    double distanceSensor1_value = 0;
    double currentSensor1_value = 0;
    double[] potentiometerValues = {0, 0};

    double angleLeftBasedOnPot = 0;
    double angleRightBasedOnPot = 0;

    private TruckDeviceUpdater mTruckDeviceUpdater;
    ExecutorService mWheelDeviceExecutor = Executors.newFixedThreadPool(4);

    //JComboBox<String> mDeviceListComboBox = new JComboBox<String>();

    private boolean mInitialSettingsPrint = true;
    private int numberOfSettingsPrinted = 0;

    private double mFailSafeDelay = 5;//seconds

    private ChartParamsDataset[] mChartParamsDatasets;
    private Timer mUpdaterTimer;

    private double[] currentBLDCPOSValueDepreciated = {0, 0};
    private double MinBLDCPOS = 0;  // use this to set min/max ranges on charts (functionality doesn't currently work)
    private double MaxBLDCPOS = 20; // use this to set min/max ranges on charts (functionality doesn't currently work)

    private JFreeChart chart = null;
    private XYPlot xyplot = null;
    private ValueAxis axis = null;
    private Double trimValueFrontLeft = 0.0;
    private Double trimValueFrontRight = 0.0;

    private final GamePadManager mGamePadManager = new GamePadManager();
    private GamePadUpdater mGamePadUpdater;

    public void connectLight() {
        mLight = new RoboLights();
        mLight.connectLight();
    }

    public void connectSensors() {
        mElectricalCurrent = new Electrical();
        mElectricalCurrent.connectCurrentSensorOne();
        mPotentiometerFrontLeft = new Potentiameters();
        mPotentiometerFrontLeft.connectPotentiameter("FrontLeft");
        mPotentiometerFrontRight = new Potentiameters();
        mPotentiometerFrontRight.connectPotentiameter("FrontRight");
    }

    /**
     * Creates new form RoverFrontEnd
     * @param port
     */
    public RoverFrontEnd(int port) {
        initComponents();
        initMoreComponents();
        System.out.println("Somewhere near here need to have a command to set the steering controls to straight "
                + "forward (or last setting from last time) otherwise it sometimes starts out with front wheels steering to an extreme angle");

        try {
            mSocketServer = new SocketServer(port, mComPipe);
        } catch (IOException ex) {
            Logger.getLogger(RoverFrontEnd.class.getName()).log(Level.SEVERE, null, ex);
        }
        mSocketServerThread = new Thread(mSocketServer);
        mSocketServerThread.start();
        mComParser.execute();
        //connectRoboArm(); //- disconnected physical devices - can turn this back on later
        connectSensors();
        connectLight();
        //jSlider_moveBaseMotor.setMaximum(45);
        //jSlider_moveBaseMotor.setMajorTickSpacing(8);

        //start GamePadController
        mGamePadManager.connectToControllers();
        mGamePadUpdater = new GamePadUpdater(
                mGamePadManager,
                mTruckSteerPanel,
                mLblGamePadStatusValue
        );
        mGamePadUpdater.execute();
    }

    public final void initMoreComponents() {
        ToolTipManager.sharedInstance().setInitialDelay(100);
        ToolTipManager.sharedInstance().setReshowDelay(1150);
        ToolTipManager.sharedInstance().setDismissDelay(15000);
        Preferences prefs;
        
        /* Four Commands for four wheels */
        mWheelDeviceParamCommands = new WheelDeviceParamCommand[]{
                new WheelDeviceParamCommand(0),
                new WheelDeviceParamCommand(1),
                new WheelDeviceParamCommand(2),
                new WheelDeviceParamCommand(3)
        };

        //prefs = Preferences.userRoot().node(this.getClass().getName()); // if this doesn't run the first time try the instructions in the 'SavePreferencesExample_oneSession...' from Sept 2019
        prefs = Preferences.userRoot();
        String ID = "FrontRight_posActual";
        System.out.println(prefs.name() + " 'FrontRight_posActual _ending STEPPER Position from last session '" + prefs.getDouble(ID, 99999.9));

        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher((KeyEvent e) -> {
                    if (e.getID() == KeyEvent.KEY_PRESSED && mMainTabbedPane.getSelectedIndex() == 3) {
                        return mTruckSteerPanel.onKeyPressed(e);
                    }
                    return false;
        });

        try {
            mDeviceManager = new DeviceManager();
        } catch (PhidgetException ex) {
            Logger.getLogger(RoverFrontEnd.class.getName()).log(Level.SEVERE, null, ex);
        }

        new DeviceListLoader().execute();
        System.out.println("Starting UI_UpdaterAllan now.");
        new RoverInterface_Updater().execute();

        mTruckEngager.setTruck(mTruckSteerPanel.getTruck());

        mTruckSteerPanel.setRotateToStraight();
        mTruckSteerPanel.addForwardDistanceListener((double... numbers) -> {
            mTruckDevice.addForwardDistance(numbers[0]);
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanUpOnClose();
                super.windowClosing(e);
            }
        });

        GpsReaderFrame mGpsReaderFrame = new GpsReaderFrame();
        mGpsReaderFrame.setVisible(true);
        mGpsReaderFrame.setTruckSteerPanel(mTruckSteerPanel);
    }

    private void cleanUpOnClose() {
        System.out.println("app is closing");
        long startTime = System.currentTimeMillis();
        mComPipe.sendCloseMessage();
        mLight.turnLightOnOff(0); // ensure the light is turned off (otherwise it stays on indefinately if the app closed)
        while ((System.currentTimeMillis() - startTime) < 1000 * 7) {
            mTruckSteerPanel.getTruck().stopMoving();
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ex) {
                Logger.getLogger(RoverFrontEnd.class.getName()).log(Level.SEVERE, null, ex);
                break;
            }

        }
        mTruckDevice.disengageDevicesCloseChannels();
        System.out.println("Devices are disengaged and channels are closed.");
    }

    public void loadFromConfig(Config config) { // there is also a loadfromConfig for the UI UI
        //Separate thread is used as setIPCamPosition blocks the execution
        //of main thread
        new Thread(() -> {
            config.setIPCamPosition();
        }).start();

        setLocation(520, 25); // set the location of the window on rover upon startup (right, down)

        mFailSafeDelay = config.getFailSafeDelay(mFailSafeDelay);
        try {
            mConfigDB = new ConfigDB(config.getConfigDBPath());
        } catch (SQLException ex) {
            Logger.getLogger(RoverFrontEnd.class.getName()).log(Level.SEVERE, null, ex);
        }

        mTruckDevice = new TruckDevice(
                mTruckSteerPanel.getTruck(),
                mDeviceManager,
                mConfigDB);

        Truck truck = mTruckSteerPanel.getTruck();
        mTruckSteerPanel.loadFromConfig(config);

        mWheelConfigPanelFrontLeft.setWheelDevice(mTruckDevice.getWheelDeviceAt(0));
        mWheelConfigPanelFrontRight.setWheelDevice(mTruckDevice.getWheelDeviceAt(1));
        mWheelConfigPanelRearLeft.setWheelDevice(mTruckDevice.getWheelDeviceAt(2));
        mWheelConfigPanelRearRight.setWheelDevice(mTruckDevice.getWheelDeviceAt(3));

        mTruckDeviceUpdater = new TruckDeviceUpdater();
        mTruckDeviceUpdater.execute();

        //Attache change listerner to Wheel Config Panels
        WheelConfigPanel.ChangeListener mWheelConfigListener = (int wheelIndex, String paramName) -> {
            mWheelDeviceExecutor.execute(new WheelDeviceUpdateTask(
                    mTruckDevice.getWheelDeviceAt(wheelIndex), paramName
            ));
        };

        WheelConfigPanel[] wheelConfigPanels = new WheelConfigPanel[]{
                mWheelConfigPanelFrontLeft, mWheelConfigPanelFrontRight,
                mWheelConfigPanelRearLeft, mWheelConfigPanelRearRight
        };
        for (WheelConfigPanel wheelConfigPanel : wheelConfigPanels) {
            wheelConfigPanel.addChangeListener(mWheelConfigListener);
            wheelConfigPanel.loadFromConfig(config);
            wheelConfigPanel.setConfigDB(mConfigDB);
        }

        mTruckSteerPanel.startPeriodicScreenUpdate(80);

        try {
            sleep(80);
        } catch (InterruptedException ex) {
            Logger.getLogger(RoverFrontEnd.class.getName()).log(Level.SEVERE, null, ex);
        }
        mChartParamsDatasets = mTruckDevice.getChartParamsDatasets();
        for (ChartParamsDataset chartParamsDataset : mChartParamsDatasets) {
            chart = ChartFactory.createXYLineChart(
                    chartParamsDataset.getChartName(),
                    "Time",
                    null,
                    chartParamsDataset.getDutyCycleDataset(),
                    PlotOrientation.VERTICAL,
                    false,
                    false,
                    false
            );

            xyplot = chart.getXYPlot();
            axis = xyplot.getDomainAxis();
            axis = xyplot.getRangeAxis();

            ChartPanel chartPanel = new ChartPanel(chart);
            mPanelCharts.add(chartPanel);
        }

        /**
         * mComPipe.putOut timer to send data from Rover (server) to UI (client) on a
         * regular time interval.
         */
        mUpdaterTimer = new Timer(200, e -> {
            mTruckDevice.updateChartParamsDataset();
            mTruckDevice.updateDutyCycleAtIndex();
            for (WheelDeviceParamCommand wdpc : mWheelDeviceParamCommands) {
                mComPipe.putOut(wdpc.buildCommandIfChanged());
            }
        });
        mUpdaterTimer.start();

        //Used for testing with no actual phdiget
        if (config.getPhidgetUseFake()) {
            mDeviceManager.addBLDCMotor(1000);
        }
        mGamePadManager.loadFromConfig(config);
    }

    /*
    * This is data that is sent from the Rover to the UI. This code runs on the Rover.
    * This data goes into the charts on the UI Interface.
    * Min/MaxBLDCPOS variables are designed to limit the range displayed on the charts; however,
    * this part of the code currently doesn't work.
    */
    private void updateWheelDeviceParamsForCom() {
        for (int i = 0; i < 4; i++) {
            WheelDevice wheelDevice = mTruckDevice.getWheelDeviceAt(i);
            WheelDeviceParamCommand wdpc = mWheelDeviceParamCommands[i];
            //System.out.println(String.format("updateWheelDeviceParamsForCom %d %f", i, wheelDevice.getBLCDCPosIndex()));
            currentBLDCPOSValueDepreciated = wheelDevice.getBLCDCPosIndex();
            currentBLDCPOSValueDepreciated[0] = java.lang.Math.abs(currentBLDCPOSValueDepreciated[0]);
            if (currentBLDCPOSValueDepreciated[0] < MinBLDCPOS) {
                MinBLDCPOS = currentBLDCPOSValueDepreciated[0];
            }

            currentBLDCPOSValueDepreciated[0] = java.lang.Math.abs(currentBLDCPOSValueDepreciated[0]);
            if (currentBLDCPOSValueDepreciated[0] > MaxBLDCPOS) {
                MaxBLDCPOS = currentBLDCPOSValueDepreciated[0];
            }

            /*
            * This is the specific part of the code that sends data from Rover to UI
            * so it can be displayed in the UI charts.
            * 
            * wheelDevice.getBLCDCPosMultiplier()[0] this compensates for devices that are wired backward.
            */
            if(wheelDevice.getWheel().getWheelName().equals("FrontLeft")){
                // index zero refers to how many motors are on each wheel; not the wheel number.
                wdpc.setActualSteeringAngle(0, angleLeftBasedOnPot);
            } else if(wheelDevice.getWheel().getWheelName().equals("FrontRight")){
                wdpc.setActualSteeringAngle(0, angleRightBasedOnPot);
            }
            
            // index zero refers to which motor to look at on each wheel; not the wheel number.
            wdpc.setBLDCMotorActualTemperature(0, wheelDevice.getBLDC_Temperature());
            //look at getDutyCycle() in WheelDevice.java as a template to get temperature data
            
            // setGhostAngle in WheelDevice.java?
                        
            wdpc.setBLDCMotorPos(0, wheelDevice.getBLCDCPosAtIndex(0)*wheelDevice.getBLCDCPosMultiplier()[0]);
            wdpc.setBLDCMotorPos(1, wheelDevice.getBLCDCPosAtIndex(1));
            wdpc.setBLDCMotorDutyCycle(0, wheelDevice.getBLCDCDutyCyleAtIndex(0) * 1000*wheelDevice.getBLCDCPosMultiplier()[0]);
            wdpc.setBLDCMotorDutyCycle(1, wheelDevice.getBLCDCDutyCyleAtIndex(1) * 1000);
        }
    }

    /**
     * This class periodically update actual Phidget devices.
     */
    final class TruckDeviceUpdater extends SwingWorker<Void, Void> {
        @Override
        protected Void doInBackground() {

            //String timeS = new SimpleDateFormat("HH.mm.ss.SSS").format(new Date());
            int doInBackgroundCounter = 0;
            while (true) {
                if (mTruckDevice == null) {
                    System.err.println("error mTruckDevice is null");
                }
                if (mTruckDevice != null) {
                    //TODO: updating should be optimized to prevent
                    // continuous signal transmission to devices
                    if (sendingManualSteer) {
                    }
                    // have a controlMethod option 1) by speed; 2) by position...
                    if (doInBackgroundCounter % 250 == 0) {
                        String timeS = new SimpleDateFormat("HH.mm.ss.SSS").format(new Date());
                        System.err.println("doInBackground counter: " + doInBackgroundCounter + " " + timeS);
                    }
                    doInBackgroundCounter++;

                    //long startTime = System.nanoTime();
                    mTruckDevice.updateDevices();
                    /**
                     * The distanceRemainingRover shows how much further Rover needs to drive
                     * to reach it's target position.
                     * Bring the distanceRemainingRover into RoverFrontEnd so it can be sent to UI.
                     * This same value is displayed on the Rover Interface via a call to
                     * wheel.draw(g2d) in Truck.java. This causes Wheel.java to display this value
                     * via a cursor = Utility.Drawing.drawString command of the variable distanceRemainingRover.
                     */
                    mTruckDevice_distanceRemainingRover = mTruckDevice.getDistanceRemainingRover();
                    //timeS = new SimpleDateFormat("HH.mm.ss.SSS").format(new Date()); 
                    //System.err.println("updateDevices "+timeS);
                    //
                    //long estimatedTime = System.nanoTime() - startTime;
                    //estimatedTime = System.nanoTime() - startTime;
                    //System.err.println(" loop time: "+estimatedTime/1000000);
                    updateWheelDeviceParamsForCom();

                } else {
                    System.out.println("Is this why it doesn't work?");
                }

                try {
                    sleep(sleepValue);
                } catch (InterruptedException ex) {
                    Logger.getLogger(RoverFrontEnd.class.getName()).log(Level.SEVERE, null, ex);
                    break;
                }
            }
            System.err.println("errror here???");
            return null;
        }
    }

    /**
     * 1) Picks up a value of the ElectricalCurrent and updates Rover Screen with value;
     * and sends value to UI via compipe.
     * 2) Prints a few sample Front Pot readings to screen for visibility at the beginning of a run.
     * 3) Updates the pot values on the screen (measurement of steering angle for both front wheel).
     * 4) Updates the max duty cycle reading from the motors that steer the wheels. Steering puts a lot
     * of pressure on these motors so we track it on the screen.
     */
    final class RoverInterface_Updater extends SwingWorker<Void, String> {
        @Override
        protected Void doInBackground() {

            try {
                sleep(7500);
            } catch (InterruptedException ex) {
                //Logger.getLogger(RoverFrontEnd.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.err.println("Into the RoverInterface_Updater command now.");
            double oldCurrentValue = -1;
            while (true) {
                currentSensor1_value = mElectricalCurrent.currentSensor1();
                boolean shouldSendCommand = false;

                if (currentSensor1_value != oldCurrentValue) {
                    oldCurrentValue = currentSensor1_value;
                    publish("current");
                    mCommonSensorCommand.setElectricalCurrent(currentSensor1_value);
                    shouldSendCommand = true;
                }

                if (mTruckDevice_distanceRemainingRover != prior_mTruckDevice_distanceRemainingRover_value) {
                    prior_mTruckDevice_distanceRemainingRover_value = mTruckDevice_distanceRemainingRover;
                    /**
                     * Publish code stubbed in to be consistent with how the electricalcurrent reading is done above.
                     * However, publish is commented out since publish here is for rover interface and
                     * this same value is displayed on the Rover Interface via a call to
                     * wheel.draw(g2d) in Truck.java. Truck.java causes Wheel.java to display this value
                     * via a cursor = Utility.Drawing.drawString command of the variable distanceRemainingRover.
                     */
                    //publish("distanceRemaining");
                    mCommonSensorCommand.setDistanceRemainingRover(mTruckDevice_distanceRemainingRover);
                    /**
                     * If True the distanceRemainingRover will be sent to UI via the mComPipe.putOut.
                    */
                    shouldSendCommand = true;
                }

                if (shouldSendCommand) {
                    mComPipe.putOut(mCommonSensorCommand.buildCommand());
                }                        

                //jTextFieldLeftPot_2.setOpaque(true);
                //jTextFieldLeftPot_2.setBackground(Color.red);
                for (int i = 0; i < 2; i++) { // only need to check the front two wheel devices since rear wheels aren't steering
                    WheelDevice wheelDevice = mTruckDevice.getWheelDeviceAt(i);

                    if (wheelDevice.getWheel().getWheelName() == "FrontLeft") {
                        maxDutyFrontLeftSteeringMotor = wheelDevice.MaxdutyCycleReading;
                        angleLeftBasedOnPot = (potentiometerValues[0] - startingTargetPotPositionFrontLeft) * (360);
                        numberOfSettingsPrinted = numberOfSettingsPrinted + 1; // print out a few settings and then stop printing them. This can be adjusted for troubleshooting.
                        if (numberOfSettingsPrinted > 8) {
                            mInitialSettingsPrint = false;
                        }
                        if (mInitialSettingsPrint) {
                            System.err.println("" + wheelDevice.getWheel().getWheelName() +
                                    " PotentiometerValues: " + String.format("%02.4f", (potentiometerValues[0])) +
                                    " PotentiometerValues-startingTargetPotPositionFrontLeft: " + String.format("%02.4f", (potentiometerValues[0] - startingTargetPotPositionFrontLeft)) +
                                    " (AngleBasedOnPot - startingTargetPotPositionFrontLeft)*360: " + String.format("%02.3f", (angleLeftBasedOnPot) // straight ahead for this pot is 0.7753
                            ));
                        }
                    }
                    if (wheelDevice.getWheel().getWheelName() == "FrontRight") {
                        maxDutyFrontRightSteeringMotor = wheelDevice.MaxdutyCycleReading;
                        angleRightBasedOnPot = (potentiometerValues[1] - startingTargetPotPositionFrontRight) * (360);
                        numberOfSettingsPrinted = numberOfSettingsPrinted + 1; // print out a few settings and then stop printing them. This can be adjusted for troubleshooting.
                        if (numberOfSettingsPrinted > 5) {
                            mInitialSettingsPrint = false;
                        }
                        if (mInitialSettingsPrint) {
                            System.err.println(wheelDevice.getWheel().getWheelName() +
                                    " Change the hard coded factor by looking at the Pot reading when steering straight forward. The Pot reading should be used as the hard coded factor. PotentiometerValues: " + String.format("%02.4f", (potentiometerValues[1])) +
                                    " PotentiometerValues-startingTargetPotPositionFrontRight: " + String.format("%02.4f", (potentiometerValues[1] - startingTargetPotPositionFrontRight)) +
                                    " (AngleBasedOnPot -startingTargetPotPositionFrontRight)*360: " + String.format("%02.3f", (angleRightBasedOnPot) // straight ahead for this pot is 0.5113
                            ));
                        }
                    }
                }
                // this should be down in the wheels and not here
                potentiometerValues[0] = mPotentiometerFrontLeft.getPotentiameterValue("RoverFrontEndJava0");
                mTruckSteerPanel.setLabel_LeftPotentiameterANDmaxDutyCycle(potentiometerValues[0], angleLeftBasedOnPot, maxDutyFrontLeftSteeringMotor);
                jTextFieldLeftPot_2.setText(String.format("%.3f", potentiometerValues[0] * 100) + "");
                potentiometerValues[1] = (mPotentiometerFrontRight.getPotentiameterValue("RoverFrontEndJava1"));
                mTruckSteerPanel.setLabel_RightPotentiameterANDmaxDutyCycle(potentiometerValues[1], angleRightBasedOnPot, maxDutyFrontRightSteeringMotor);
                jTextFieldRightPot_2.setText(String.format("%.3f", potentiometerValues[1] * 100) + "");
                // calibration loop - runs only when user selects button in GUI


                // this should be down in the wheels and not here

                //System.err.println("currentSensor1_value "+currentSensor1_value);
                String[] warning = mTruckDevice.getDisengage2Warning();
                for (int i = 0; i < 4; i++) {
                    if (warning[i].startsWith("DISENGAGED")) {
                        //System.err.println("WARNING "+warning[i]);
                    }
                }

                publish("");
                //System.err.println("ending due to ?????");
                try {
                    sleep(80);
                } catch (InterruptedException ex) {
                    //Logger.getLogger(RoverFrontEnd.class.getName()).log(Level.SEVERE, null, ex);
                    break;
                }
            }
            System.err.println("always here?");
            return null;
        }

        @Override
        protected void process(List<String> list) {//Works on GUI thread
            for (String command : list) {
                if (command.equals("current")) { // this is a bit awkward
                    mTruckSteerPanel.setLabel_jLabel_ElectricalCurrent(
                            currentSensor1_value);
                }
            }
        }
    }


    final class DeviceListLoader extends SwingWorker<Void, String> {
        @Override
        protected Void doInBackground() {
            int prevCount = 0;
            while (true) {
                int currCount = mDeviceManager.getChannelCount();
                //System.out.println("currCount "+currCount);
                if (prevCount != currCount) {
                    prevCount = currCount;
                    publish(mDeviceManager.getChannelNames());
                }
                try {
                    if (mTruckDevice != null && mTruckDevice.detectDevices(false)) {
                        System.err.println("BREAK IN ROVERFrontEnd.java");
                        break;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                try {
                    Thread.sleep(100);
                    //System.out.println("sleep_100"); // cpu seems to run at 100% if this is below 100
                } catch (InterruptedException ex) {
                    Logger.getLogger(RoverFrontEnd.class.getName()).log(Level.SEVERE, null, ex);
                    break;
                }
            }
            return null;
        }

        @Override
        protected void process(List<String> channelNames) {
            mWheelConfigPanelFrontLeft.setDeviceManager(mDeviceManager);
            mWheelConfigPanelFrontRight.setDeviceManager(mDeviceManager);
            mWheelConfigPanelRearLeft.setDeviceManager(mDeviceManager);
            mWheelConfigPanelRearRight.setDeviceManager(mDeviceManager);
        }
    }
/**
 * On the Rover UI this processes messages from the UI computer.
 * 
 */
    final class ComParser extends SwingWorker<Void, String> {
        @Override
        protected Void doInBackground() throws Exception {
            long lastUpdatedAt = 0;
            while (true) {
                String line = mComPipe.getIn();
                if (line != null) {
                    //System.out.println("line: " + line);
                    publish(line);
                }
                if (System.currentTimeMillis() - lastUpdatedAt > 200) {
                    lastUpdatedAt = System.currentTimeMillis();
                    publish("");
                }
                Thread.sleep(10);
            }
        }

        @Override
        /**
         * multiple line values could be published and then processed all at once via the list
         */
        protected void process(List<String> list) {
            boolean showLog = true;
            for (String command : list) {
                if (command.length() == 0) {//pseudo signal to update other gui widgets
                    //mTruckSteerPanel.getTruck().getSpeed();                
                    long lastHeartBeatAge = mComPipe.getLastReceviedMessageAge();
                    if (lastHeartBeatAge > mFailSafeDelay * 1000) {

                        //Below lines are commented temporarily for sujoy's testing - it can be used with Allan's set up and the UI machine running
                        mTruckSteerPanel.setSteeringMode("Stopped");
                        //System.err.println("Sending Stop Signal.");
//                        if(mTruckDevice!=null){
//                            //mTruckDevice.disengageDevices();
//                        }
                        String time_stamp_of_stop = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(new Date());


                        // to avoid annoying printing on Sujoy's machine during testing we probably need a flag in the robo-confi.ini 
                        // for 'AllanMachine' or 'SujoyMachine' linked to allow/not allow error message
                        if(warning_counter%75 == 0){
                            System.err.println("STOPPED due to no connection - UI driver needs to reselect drive mode to continue.  Time: " + time_stamp_of_stop + " " + lastHeartBeatAge + " " + mFailSafeDelay);
                            //warning_counter=0;
                        }
                        warning_counter++;


//                          this seems to hang it up when 'exit button' is pressed
//                        try {
//                            sleep(1250);
//                        } catch (InterruptedException ex) {
//                            Logger.getLogger(RoverFrontEnd.class.getName()).log(Level.SEVERE, null, ex);
//                        }
                    }
                    mTruckSteerPanel.updateLagProgress(lastHeartBeatAge);
                } else if (mRemoteMousePosCommand.canServeCommand(command)) {
                    mRemoteMousePosCommand.parseCommand(command);
                    //System.out.println("setting groud panel mouse POS Fraction: "+command);
                    mTruckSteerPanel.setGroundPanelMousePosRelToTruck(
                            mRemoteMousePosCommand.getPos());
                } else if (mRemoteSteeringCommand.canServeCommand(command)) {
                    mRemoteSteeringCommand.parseCommand(command);
                    mTruckSteerPanel.setSteeringMode(
                            mRemoteSteeringCommand.getSteeringMode());

                } else if (mDirectionCommand.canServeCommand(command)) {
                    mDirectionCommand.parseCommand(command);
                    mTruckSteerPanel.setDirectionMode( // had previously been 'setSteeringMode' which appears to be incorrect - Feb 19, 2019
                            mDirectionCommand.getDirectionMode());

                } else if (mRemoteSpeedCommand.canServeCommand(command)) {
                    //System.out.println("command:" + command);
                    mRemoteSpeedCommand.parseCommand(command);
                    mTruckSteerPanel.setTruckSpeed(
                            mRemoteSpeedCommand.getSpeed());
                    mTruckSteerPanel.setVelocityLimit(
                            mRemoteSpeedCommand.getVelocityLimit());
                    mTruckSteerPanel.setVelocityIncrement(
                            mRemoteSpeedCommand.getVelocityLimitIncrement());
                } else if (mRemoteMouseHandednessCommand.canServeCommand(command)) {
                    mRemoteMouseHandednessCommand.parseCommand(command);
                    //mTruckSteerPanel.getGroundPanel().setMouseHandedness(
                    //    mRemoteMouseHandednessCommand.getHandedness());
                } else if (mRemoteConfigDBParamCommand.canServeCommand(command)) {
                    mRemoteConfigDBParamCommand.parseCommand(command);
                    if (mConfigDB != null) {
                        mConfigDB.setValue(
                                mRemoteConfigDBParamCommand.getName(),
                                mRemoteConfigDBParamCommand.getValue(),
                                mRemoteConfigDBParamCommand.getValueType()
                        );
                    }
                } else if (mForwardAngleCommand.canServeCommand(command)) {
                    mForwardAngleCommand.parseCommand(command);
                    mTruckSteerPanel.setForwardAngleMultiplier(
                            mForwardAngleCommand.getForwardAngleMultiplier());
                    mTruckSteerPanel.addForwardAngle(
                            mForwardAngleCommand.getForwardAngle());
                    //mTruckSteerPanel.setLabel_mLblForwardAngleValue(mForwardAngleCommand.getForwardAngle());
                } else if (mTruckScaleCommand.canServeCommand(command)) {
                    mTruckScaleCommand.parseCommand(command);
                    mTruckSteerPanel.setTruckScale(mTruckScaleCommand.getTruckScale());
                } else if (mWheelSteeringAngleCommand.canServeCommand(command)) { // this isn't used?? on either rover nor UI as of 6/22/2023
                    mWheelSteeringAngleCommand.parseCommand(command);
                    double angle = mWheelSteeringAngleCommand.getAngle();
                    switch (mWheelSteeringAngleCommand.getMode()) {
                        case WheelSteeringAngleCommand.MODE_ABSOLUTE:
                            for (int wheelIndex : mWheelSteeringAngleCommand.getWheelIndices()) {
                                mTruckSteerPanel.getTruck().setWheelsAngle(
                                        wheelIndex, angle
                                );
                            }
                            break;
                        case WheelSteeringAngleCommand.MODE_INCREMENTAL:
                            for (int wheelIndex : mWheelSteeringAngleCommand.getWheelIndices()) {
                                mTruckSteerPanel.getTruck().increaseWheelsAngle( // this isn't used?? on either rover nor UI as of 6/22/2023
                                        wheelIndex, angle
                                );
                            }
                            break;
                    }
                } else if (mFloatSubCommand.canServeCommand(command)) {
                    mFloatSubCommand.parseCommand(command);

                    if (mFloatSubCommand.getSubCommand().equals(FloatSubCommand.STEER_STOP)) {
                        mTruckSteerPanel.setSteeringMode(SteeringMode.NONE.toString());
                    } else if (mFloatSubCommand.getSubCommand().equals(FloatSubCommand.STEER_FRONT)) {
                        mTruckSteerPanel.setSteeringMode(SteeringMode.FRONT_STEER.toString());
                    } else if (mFloatSubCommand.getSubCommand().equals(FloatSubCommand.ROTATE_LEFT)) {
                        if (mFloatSubCommand.getValue() > 0) {
                            mTruckSteerPanel.doRotateLeft();
                        }
                    } else if (mFloatSubCommand.getSubCommand().equals(FloatSubCommand.ROTATE_STRAIGHT)) {
                        if (mFloatSubCommand.getValue() > 0) {
                            mTruckSteerPanel.setRotateToStraight();
                        }
                    } else if (mFloatSubCommand.getSubCommand().equals(FloatSubCommand.ROTATE_RIGHT)) {
                        if (mFloatSubCommand.getValue() > 0) {
                            mTruckSteerPanel.doRotateRight();
                        }
                    } else if (mFloatSubCommand.getSubCommand().equals(FloatSubCommand.ROTATE_FORWARD)) {
                        if (mFloatSubCommand.getValue() > 0) {
                            mTruckSteerPanel.rotateForward(1);
                        }
                    } else if (mFloatSubCommand.getSubCommand().equals(FloatSubCommand.ROTATE_BACKWARD)) {
                        if (mFloatSubCommand.getValue() > 0) {
                            mTruckSteerPanel.rotateForward(-1);
                        }
                    } else if (mFloatSubCommand.getSubCommand().equals(FloatSubCommand.SPEED_FASTER)) {
                        if (mFloatSubCommand.getValue() > 0) {
                            mTruckSteerPanel.increaseSpeed(1);
                        }
                    } else if (mFloatSubCommand.getSubCommand().equals(FloatSubCommand.SPEED_SLOWER)) {
                        if (mFloatSubCommand.getValue() > 0) {
                            mTruckSteerPanel.increaseSpeed(-1);
                        }
                    } else if (mFloatSubCommand.getSubCommand().equals(FloatSubCommand.VEHICLE_POINTER_HALT)) {

                    } else if (mFloatSubCommand.getSubCommand().equals(FloatSubCommand.VEHICLE_POINTER_TRACK)) {
                        mTruckSteerPanel
                                .getGroundPanel()
                                .increaseMousePosXFraction(
                                        mFloatSubCommand.getValue());
                    } else if (mFloatSubCommand.getSubCommand().equals(FloatSubCommand.STEER_STRAIGHT)) {
                        System.out.println("testing here " + SteeringMode.STRAIGHT.toString());
                        if (mFloatSubCommand.getValue() > 0) {
                            mTruckSteerPanel.setSteeringMode(SteeringMode.STRAIGHT.toString());
                        }
                    }
                }

                if (showLog) {
                    mTxtAreaLog.append(command + "\n"); // this shows the command on the debugger tab of the RoboCam GUI on Rover.
                }
            }
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

        mBtnAppExit = new javax.swing.JButton();
        mMainTabbedPane = new javax.swing.JTabbedPane();
        mTabConfig = new javax.swing.JPanel();
        mWheelConfigTabbedPane = new javax.swing.JTabbedPane();
        mWheelConfigPanelFrontLeft = new RoboCam.WheelConfigPanel();
        mWheelConfigPanelFrontRight = new RoboCam.WheelConfigPanel();
        mWheelConfigPanelRearLeft = new RoboCam.WheelConfigPanel();
        mWheelConfigPanelRearRight = new RoboCam.WheelConfigPanel();
        jPanel1 = new javax.swing.JPanel();
        jButton_LeftFrontMoreStepperLeft = new javax.swing.JButton();
        jButton_LeftFrontMoreStepperRight = new javax.swing.JButton();
        jButton_RightFrontMoreStepperLeft = new javax.swing.JButton();
        jButton_RightFrontMoreStepperRight = new javax.swing.JButton();
        jTextField1 = new javax.swing.JTextField();
        jTextField2 = new javax.swing.JTextField();
        jSeparator1 = new javax.swing.JSeparator();
        jToggleButtonLightsOnOff = new javax.swing.JToggleButton();
        jButtonUpdateSteppersManually = new javax.swing.JButton();
        jTextFieldFrontRightTrim = new javax.swing.JTextField();
        jTextFieldFrontLeftTrim = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jTextFieldRightPot_2 = new javax.swing.JTextField();
        jTextFieldLeftPot_2 = new javax.swing.JTextField();
        jButtonCalibrateToTargetPotValue = new javax.swing.JButton();
        mPanelVehicle = new javax.swing.JPanel();
        mTruckSteerPanel = new InterfaceComponents.TruckSteerPanel();
        mPanelCharts = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        mTruckEngager = new InterfaceComponents.TruckEngager();
        jButtonFrontRightReEngage = new javax.swing.JButton();
        mPanelDebug = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        mBtnGetConnSockets = new javax.swing.JButton();
        mScrollPaneLog = new javax.swing.JScrollPane();
        mTxtAreaLog = new javax.swing.JTextArea();
        mPanelStatusInfo = new javax.swing.JPanel();
        mBtnEnableGamePad = new javax.swing.JToggleButton();
        mLblGamePadStatus = new javax.swing.JLabel();
        mLblGamePadStatusValue = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(400, 325));
        setPreferredSize(new java.awt.Dimension(900, 823));
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));

        mBtnAppExit.setText("Exit");
        mBtnAppExit.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        mBtnAppExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBtnAppExitActionPerformed(evt);
            }
        });
        getContentPane().add(mBtnAppExit);

        mMainTabbedPane.setMinimumSize(new java.awt.Dimension(400, 325));
        mMainTabbedPane.setPreferredSize(new java.awt.Dimension(1000, 800));

        mTabConfig.setMinimumSize(new java.awt.Dimension(750, 400));
        mTabConfig.setPreferredSize(new java.awt.Dimension(1000, 800));
        mTabConfig.setLayout(new java.awt.GridBagLayout());

        mWheelConfigTabbedPane.setTabPlacement(javax.swing.JTabbedPane.LEFT);
        mWheelConfigTabbedPane.setAlignmentX(0.0F);
        mWheelConfigTabbedPane.setAlignmentY(0.0F);

        mWheelConfigPanelFrontLeft.setWheelName("FrontLeft");
        mWheelConfigTabbedPane.addTab("Front Left", mWheelConfigPanelFrontLeft);

        mWheelConfigPanelFrontRight.setWheelIndex(1);
        mWheelConfigPanelFrontRight.setWheelName("FrontRight");
        mWheelConfigTabbedPane.addTab("Front Right", mWheelConfigPanelFrontRight);

        mWheelConfigPanelRearLeft.setWheelIndex(2);
        mWheelConfigPanelRearLeft.setWheelName("RearLeft");
        mWheelConfigTabbedPane.addTab("Rear Left", mWheelConfigPanelRearLeft);

        mWheelConfigPanelRearRight.setWheelIndex(3);
        mWheelConfigPanelRearRight.setWheelName("RearRight");
        mWheelConfigTabbedPane.addTab("Rear Right", mWheelConfigPanelRearRight);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 5.0;
        mTabConfig.add(mWheelConfigTabbedPane, gridBagConstraints);

        mMainTabbedPane.addTab("Configuration", mTabConfig);

        jPanel1.setMinimumSize(new java.awt.Dimension(383, 33));
        jPanel1.setPreferredSize(new java.awt.Dimension(383, 33));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jButton_LeftFrontMoreStepperLeft.setText("stepper more left");
        jButton_LeftFrontMoreStepperLeft.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_LeftFrontMoreStepperLeftActionPerformed(evt);
            }
        });
        jPanel1.add(jButton_LeftFrontMoreStepperLeft, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 70, -1, 60));

        jButton_LeftFrontMoreStepperRight.setText("stepper more right");
        jButton_LeftFrontMoreStepperRight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_LeftFrontMoreStepperRightActionPerformed(evt);
            }
        });
        jPanel1.add(jButton_LeftFrontMoreStepperRight, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 70, -1, 60));

        jButton_RightFrontMoreStepperLeft.setText("stepper more left");
        jButton_RightFrontMoreStepperLeft.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_RightFrontMoreStepperLeftActionPerformed(evt);
            }
        });
        jPanel1.add(jButton_RightFrontMoreStepperLeft, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 70, -1, 60));

        jButton_RightFrontMoreStepperRight.setText("stepper more right");
        jButton_RightFrontMoreStepperRight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_RightFrontMoreStepperRightActionPerformed(evt);
            }
        });
        jPanel1.add(jButton_RightFrontMoreStepperRight, new org.netbeans.lib.awtextra.AbsoluteConstraints(550, 70, -1, 60));

        jTextField1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jTextField1.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextField1.setText("Right Front Trim");
        jPanel1.add(jTextField1, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 30, 150, 30));

        jTextField2.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jTextField2.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextField2.setText("Left Front Trim");
        jPanel1.add(jTextField2, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 30, 150, 30));

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jPanel1.add(jSeparator1, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 20, 10, 170));

        jToggleButtonLightsOnOff.setText("Light On_Off (click to Turn Light On)");
        jToggleButtonLightsOnOff.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonLightsOnOffActionPerformed(evt);
            }
        });
        jPanel1.add(jToggleButtonLightsOnOff, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 410, 450, 110));

        jButtonUpdateSteppersManually.setText("update steppers manually");
        jPanel1.add(jButtonUpdateSteppersManually, new org.netbeans.lib.awtextra.AbsoluteConstraints(670, 470, -1, -1));

        jTextFieldFrontRightTrim.setText("Right Front");
        jPanel1.add(jTextFieldFrontRightTrim, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 140, 260, 40));

        jTextFieldFrontLeftTrim.setText("Left Front");
        jPanel1.add(jTextFieldFrontLeftTrim, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 140, 260, 40));

        jTextArea1.setColumns(20);
        jTextArea1.setFont(new java.awt.Font("Monospaced", 0, 14)); // NOI18N
        jTextArea1.setRows(5);
        jTextArea1.setText("Instructions:\n\nRover can be in the \"Stopped\" Position.\n\n1) set the correction requirements with the 'stepper \nmore left' or 'stepper more right' buttons.  The \nsteering won't update when you press these \nbuttons; the steering will update when you press\nthe AutoCalibrate button.\n\n2) press the AutoCalibrate button.\n\nDon't run rover after adjusting trim.\nWheel speeds won't be right.\nInstead, exit and restart after adjusting trim.");
        jScrollPane1.setViewportView(jTextArea1);

        jPanel1.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 190, 450, 200));

        jTextFieldRightPot_2.setText("Right Pot");
        jPanel1.add(jTextFieldRightPot_2, new org.netbeans.lib.awtextra.AbsoluteConstraints(610, 190, 80, 40));

        jTextFieldLeftPot_2.setText("Left Pot");
        jPanel1.add(jTextFieldLeftPot_2, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 190, 80, 40));

        jButtonCalibrateToTargetPotValue.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jButtonCalibrateToTargetPotValue.setText("AutoCalibrate");
        jButtonCalibrateToTargetPotValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCalibrateToTargetPotValueActionPerformed(evt);
            }
        });
        jPanel1.add(jButtonCalibrateToTargetPotValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 13, -1, 50));

        mMainTabbedPane.addTab("Lights and Trim Settings", jPanel1);

        mPanelVehicle.setMinimumSize(new java.awt.Dimension(1061, 645));
        mPanelVehicle.setLayout(new java.awt.GridBagLayout());

        mTruckSteerPanel.setMinimumSize(new java.awt.Dimension(800, 0));
        mTruckSteerPanel.setPreferredSize(new java.awt.Dimension(500, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        mPanelVehicle.add(mTruckSteerPanel, gridBagConstraints);

        mPanelCharts.setMinimumSize(new java.awt.Dimension(350, 0));
        mPanelCharts.setLayout(new java.awt.GridLayout(2, 2));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.75;
        gridBagConstraints.weighty = 1.0;
        mPanelVehicle.add(mPanelCharts, gridBagConstraints);

        mMainTabbedPane.addTab("Vehicle", mPanelVehicle);

        jPanel2.add(mTruckEngager);

        jButtonFrontRightReEngage.setText("FrontRightReEngage");
        jButtonFrontRightReEngage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonFrontRightReEngageActionPerformed(evt);
            }
        });
        jPanel2.add(jButtonFrontRightReEngage);

        mMainTabbedPane.addTab("Engager", jPanel2);

        mPanelDebug.setLayout(new javax.swing.BoxLayout(mPanelDebug, javax.swing.BoxLayout.Y_AXIS));

        jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3, javax.swing.BoxLayout.LINE_AXIS));

        mBtnGetConnSockets.setText("Get Connected Sockets");
        mBtnGetConnSockets.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBtnGetConnSocketsActionPerformed(evt);
            }
        });
        jPanel3.add(mBtnGetConnSockets);

        mPanelDebug.add(jPanel3);

        mScrollPaneLog.setMinimumSize(new java.awt.Dimension(23, 100));

        mTxtAreaLog.setColumns(20);
        mTxtAreaLog.setRows(5);
        mScrollPaneLog.setViewportView(mTxtAreaLog);

        mPanelDebug.add(mScrollPaneLog);

        mMainTabbedPane.addTab("Debug", mPanelDebug);

        mMainTabbedPane.setSelectedIndex(2);

        getContentPane().add(mMainTabbedPane);

        mBtnEnableGamePad.setText("GamePad Active");
        mBtnEnableGamePad.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        mBtnEnableGamePad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBtnEnableGamePadActionPerformed(evt);
            }
        });
        mPanelStatusInfo.add(mBtnEnableGamePad);

        mLblGamePadStatus.setText("GamePad Status: ");
        mLblGamePadStatus.setAlignmentX(0.5F);
        mPanelStatusInfo.add(mLblGamePadStatus);

        mLblGamePadStatusValue.setText("<None>");
        mPanelStatusInfo.add(mLblGamePadStatusValue);

        getContentPane().add(mPanelStatusInfo);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton_RightFrontMoreStepperRightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_RightFrontMoreStepperRightActionPerformed
        Double trimIncrement = .0025;
        trimValueFrontRight = trimValueFrontRight + trimIncrement;
        targetPotPositionFrontRight = startingTargetPotPositionFrontRight + trimValueFrontRight;
        jTextFieldFrontRightTrim.setText(String.format("%02.4f", trimValueFrontRight) + " + " + startingTargetPotPositionFrontRight + " = " + String.format("%02.4f", targetPotPositionFrontRight));
    }//GEN-LAST:event_jButton_RightFrontMoreStepperRightActionPerformed

    private void jButton_RightFrontMoreStepperLeftActionPerformed(java.awt.event.ActionEvent evt) {
        Double trimIncrement = -.0025;
        trimValueFrontRight = trimValueFrontRight + trimIncrement;
        targetPotPositionFrontRight = startingTargetPotPositionFrontRight + trimValueFrontRight;
        jTextFieldFrontRightTrim.setText(String.format("%02.4f", trimValueFrontRight) + " + " + startingTargetPotPositionFrontRight + " = " + String.format("%02.4f", targetPotPositionFrontRight));
    }

    private void jButton_LeftFrontMoreStepperRightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_LeftFrontMoreStepperRightActionPerformed
        Double trimIncrement = .0025;
        trimValueFrontLeft = trimValueFrontLeft + trimIncrement;
        targetPotPositionFrontLeft = startingTargetPotPositionFrontLeft + trimValueFrontLeft;
        jTextFieldFrontLeftTrim.setText(String.format("%02.4f", trimValueFrontLeft) + " + " + startingTargetPotPositionFrontLeft + " = " + String.format("%02.4f", targetPotPositionFrontLeft));
    }//GEN-LAST:event_jButton_LeftFrontMoreStepperRightActionPerformed

    private void jButton_LeftFrontMoreStepperLeftActionPerformed(java.awt.event.ActionEvent evt) {
        Double trimIncrement = -.0025;
        trimValueFrontLeft = trimValueFrontLeft + trimIncrement;
        targetPotPositionFrontLeft = startingTargetPotPositionFrontLeft + trimValueFrontLeft;
        jTextFieldFrontLeftTrim.setText(String.format("%02.4f", trimValueFrontLeft) + " + " + startingTargetPotPositionFrontLeft + " = " + String.format("%02.4f", targetPotPositionFrontLeft));
    }

    private void jToggleButtonLightsOnOffActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButtonLightsOnOffActionPerformed
        double lightOn = 0;
        if (jToggleButtonLightsOnOff.isSelected()) {
            lightOn = 1.0; // if true then ON
            jToggleButtonLightsOnOff.setBackground(Color.green);
            jToggleButtonLightsOnOff.setText("Light On (Click to turn light off)");
        } else {
            lightOn = 0.0; // if false then OFF
            jToggleButtonLightsOnOff.setBackground(Color.red);
            jToggleButtonLightsOnOff.setText("Light Off (Click to turn light on)");
        }
        mLight.turnLightOnOff(lightOn);
    }//GEN-LAST:event_jToggleButtonLightsOnOffActionPerformed

    private void jButtonCalibrateToTargetPotValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCalibrateToTargetPotValueActionPerformed
        targetPotPositionFrontLeft = startingTargetPotPositionFrontLeft + trimValueFrontLeft; // .5316
        targetPotPositionFrontRight = startingTargetPotPositionFrontRight + trimValueFrontRight; // .5113 startingTargetPotPositionFrontRight
        WheelDevice wheelDevice = null;
        for (int i = 0; i < 2; i++) { // only need to check the front two wheel devices since rear wheels aren't steering
            wheelDevice = mTruckDevice.getWheelDeviceAt(i); // all the wheel devices are in this collection

            if (wheelDevice.getWheel().getWheelName() == "FrontLeft") {
                wheelDevice.pushPotDown(mPotentiometerFrontLeft);
                wheelDevice.updateSteeringCalibrateTrimAutomatically(targetPotPositionFrontLeft);
            }

            if (wheelDevice.getWheel().getWheelName() == "FrontRight") {
                wheelDevice.pushPotDown(mPotentiometerFrontRight);
                wheelDevice.updateSteeringCalibrateTrimAutomatically(targetPotPositionFrontRight);
            }
        }
    }//GEN-LAST:event_jButtonCalibrateToTargetPotValueActionPerformed

    private void mBtnGetConnSocketsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBtnGetConnSocketsActionPerformed
        StringBuilder info = new StringBuilder();
        int i = 1;
        info.append("List of client cocket connections. (Approx.) \n");
        for (Socket sck : mSocketServer.getConnectedSockets()) {
            info.append(String.format(
                    "Connection %d: %s\n",
                    i,
                    sck.getRemoteSocketAddress()
            ));
            info.append("\n");
            i++;
        }
        if (i == 1) {
            info.append("No connection to show.!\n");
        }
        mTxtAreaLog.append(info.toString());
    }//GEN-LAST:event_mBtnGetConnSocketsActionPerformed

    private void jButtonFrontRightReEngageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonFrontRightReEngageActionPerformed
        WheelDevice wheelDevice2 = null;
        wheelDevice2 = mTruckDevice.getWheelDeviceAt(1); // all the wheel devices are in this collection
        System.err.println(wheelDevice2.getWheel().getWheelName());
        if (wheelDevice2.getWheel().getWheelName() == "FrontRight") {
            wheelDevice2.reEngageUponCommand();
        }
    }//GEN-LAST:event_jButtonFrontRightReEngageActionPerformed

    private void mBtnEnableGamePadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBtnEnableGamePadActionPerformed
        mGamePadUpdater.setActive(mBtnEnableGamePad.isSelected());
    }//GEN-LAST:event_mBtnEnableGamePadActionPerformed

    private void mBtnAppExitActionPerformed(java.awt.event.ActionEvent evt) {
        cleanUpOnClose();
        System.exit(0);
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonCalibrateToTargetPotValue;
    private javax.swing.JButton jButtonFrontRightReEngage;
    private javax.swing.JButton jButtonUpdateSteppersManually;
    private javax.swing.JButton jButton_LeftFrontMoreStepperLeft;
    private javax.swing.JButton jButton_LeftFrontMoreStepperRight;
    private javax.swing.JButton jButton_RightFrontMoreStepperLeft;
    private javax.swing.JButton jButton_RightFrontMoreStepperRight;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextFieldFrontLeftTrim;
    private javax.swing.JTextField jTextFieldFrontRightTrim;
    private javax.swing.JTextField jTextFieldLeftPot_2;
    private javax.swing.JTextField jTextFieldRightPot_2;
    private javax.swing.JToggleButton jToggleButtonLightsOnOff;
    private javax.swing.JButton mBtnAppExit;
    private javax.swing.JToggleButton mBtnEnableGamePad;
    private javax.swing.JButton mBtnGetConnSockets;
    private javax.swing.JLabel mLblGamePadStatus;
    private javax.swing.JLabel mLblGamePadStatusValue;
    private javax.swing.JTabbedPane mMainTabbedPane;
    private javax.swing.JPanel mPanelCharts;
    private javax.swing.JPanel mPanelDebug;
    private javax.swing.JPanel mPanelStatusInfo;
    private javax.swing.JPanel mPanelVehicle;
    private javax.swing.JScrollPane mScrollPaneLog;
    private javax.swing.JPanel mTabConfig;
    private InterfaceComponents.TruckEngager mTruckEngager;
    private InterfaceComponents.TruckSteerPanel mTruckSteerPanel;
    private javax.swing.JTextArea mTxtAreaLog;
    private RoboCam.WheelConfigPanel mWheelConfigPanelFrontLeft;
    private RoboCam.WheelConfigPanel mWheelConfigPanelFrontRight;
    private RoboCam.WheelConfigPanel mWheelConfigPanelRearLeft;
    private RoboCam.WheelConfigPanel mWheelConfigPanelRearRight;
    private javax.swing.JTabbedPane mWheelConfigTabbedPane;
    // End of variables declaration//GEN-END:variables

}

