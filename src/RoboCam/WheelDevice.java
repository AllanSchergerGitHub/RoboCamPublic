package RoboCam;

import Chart.ChartParamType;
import Chart.ChartParamsDataset;
import DB.ConfigDB;
import PhiDevice.*;
import PhiDevice.Electrical_Etc.Potentiameters;
import Rover.Device.RotationLimitSwitch;
import RoverUI.Vehicle.DeviceInfo;
import RoverUI.Vehicle.Wheel;
import com.phidget22.*;
import mySQL.MysqlLogger;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.Thread.sleep;

public class WheelDevice {
    public static final String DCMOTOR_NAME = "DCMotor.Name";
    public static final String BLDCMOTOR_NAME = "BLDCMotor.Name";//Not used
    public static final String ENCODER_NAME = "Encoder.Name";
    public static final String LIMIT_SWITCH_LEFT_NAME = "LimitSwitch.Left.Name";
    public static final String LIMIT_SWITCH_RIGHT_NAME = "LimitSwitch.Right.Name";

    public static final String BLDCMOTOR_POSITION_CONTROLLER_NAME = "BLDCMotorPosCont.Name";//Deprecated, not we use list of names
    public static final String BLDCMOTOR_POSITION_CONTROLLER_NAMES = "BLDCMotorPosCont.Names";

    public static final String DCMOTOR_SPEED_MULTIPLIER = "DCMotor.Speed.Mult";
    public static final String BLDC_1_POSITION_MULTIPLIER = "BLDC.1.Pos.Mult";
    public static final String BLDC_2_POSITION_MULTIPLIER = "BLDC.2.Pos.Mult";

    public static Executor mCommonServiceExecutor = Executors.newCachedThreadPool();

    private MotorPositionController bldc1 = null;
    private final Wheel mWheel;
    //private String mDCMotorChannelName;
    //private String mBLDCMotorChannelName;
    private String mBLDCMotorPositionControllerChannelName;
    private String mEncoderChannelName;

    private double trimSettingFromAutoTrim = 0;

    private Preferences prefs;

    private final DeviceManager mDeviceManager;

    private DeviceChannelList mBLDCMotorPositionControllerChannel;
    private DeviceChannel mEncoderChannel;

    double ConnectionAttemptCount = 0;
    double SteeringBLDCTargetPosition = 0;
    double SteeringBLDCTargetPositionConvertedToDegrees = 0;

    private MotorPositionControllerList mPhidBLDCMotorPositionController = null;

    Potentiameters mPotentiameters = null;
    private double mPotValue = 0;
    private WheelDeviceSubClassAutoTrim mWheelDeviceSubClassAutoTrim = new WheelDeviceSubClassAutoTrim();

    private boolean pauseThis = false; // false means don't pause. keep going. true means we should pause now.
    private double mPotTargetValue = 0;
    private Encoder mPhidEncoder = null;

    private float encoderCounter = 0;
    private float BLDCPositionControllerCounter = 0;
    private int encoderPosition = 0;
    private double mReadDutyCycle = 0;

    double posActual_Steering = 0;
    double posActual_SteeringConvertedToDegrees = 0;

    private double avgActualWheelPos = 0;
    private double OldSR = 1;

    private double[] mBLDCmotorDeviceReadPos = new double[]{0, 0}; // because this is an array it can have multiple variables pointing to it and making changes to it.
    //private double[] mBLDCPositionDeviceReadPos_PRIVATE = new double[2]; // because this is an array it can have multiple variables pointing to it and making changes to it. Only using it here so named it PRIVATE as a reminder.
    private PositionList mBLDCMotorPosMult = new PositionList(1, 1);

    private String encoderDevice = "initializedValue";

    private String mBatch_time_stamp_into_mysql = "initialized_in_WheelDevice";

    private final ConfigDB mConfigDB;
    private final Config mAppConfig;

    private boolean wideDeadBandSet = false;
    private boolean mAllowMysqlLogging = false;
    private boolean mAllowMysqlLoggingALTERNATE = false;
    private int dutyCycleRatioAvgShortArrayListsize = 1;
    public int mCenterStraightValue = 0;
    public double MaxdutyCycleReading = 0;
    private double ratioPreTrunc = 1;
    //It contains the multipliers of the BLDC motor position controllers.

    private ArrayList<Double> mVelocityLimitSettingWithIndex = new ArrayList<Double>();
    private ArrayList<Double> dutyCycleRatioAvgArrayList = new ArrayList<Double>();

    private ArrayList<Double> dutyCycleRatioAvgShortArrayList = new ArrayList<Double>();
    private int mSteeringPositionAbsReadSpan = 7700;
    private int mSteeringPositionAbsWriteSpan = 2300;//This is to protect too large rotation
    private double SteeringBLDC_Multiplier = 22.66 / 15.3;// frontright bldc is a different gear ratio so need to set a factor for velocity; accleration; target position to compensate

    private double mStepperMultiplier = 1.0;
    private double mDistanceRemainingRover = 0;
    private ArrayList<Double> dutyCycleDeviceDeltaList = new ArrayList<Double>();
    private double removeFromTotal = 0;
    private double ratioFactorToFixDutyCycle = 1.0;
    private double mVelocityVariableSetting = 1.0;
    private double grandTotalNumInstances = 0;
    private double motor0dutycycle = 1.0;
    private double motor1dutycycle = 1.0;
    private double delta = 0.0;
    private double ratio = 1.0;

    double mTargetPositionMotor0 = 0;

    private int lopp = 0;
    private String recalcRatioFlag = "initialValue";
    private double ratioFactorToFixDutyCycl1e = 1;

    private String disengageWarning = "initializedV";

    private final ReentrantLock mOpLock = new ReentrantLock();
    private boolean mDevicesDisengaed = false;
    private boolean allowEngage = true;

    //It contains the base target position of BLDC Motors.
    //To get the target position for specific BLDC Motor
    //multiply it with mBLDCMotorPosMult
    private double mTargetPositionRoverBody = 0;
    private double mTargetPositionWheel = 0;
    private double mVelocityLimitSetting = 0;

    private ChartParamsDataset mChartParamsDataset;
    private HashMap<String, DeviceInfo> mDeviceInfoMap = new HashMap<>();

    //This listener is called when different properties of DeviceChannel are updated.
    //This function will generally fill up deviceInfo object's parameters
    private DeviceChannel.ChannelListener mDeviceChannelListener = new DeviceChannel.ChannelListener() {
        @Override
        public void onPropertyChange(DeviceChannel dc, String propertyName, Object value) {
            DeviceInfo deviceInfo = mDeviceInfoMap.get(dc.getLabel());
            if (deviceInfo == null) return;
            if (propertyName.equals("engaged")) {
                deviceInfo.setEngaged((boolean) value);
            } else if (propertyName.equals("attached")) {
                deviceInfo.setParam("Name", dc.getName());
            }
        }
    };

    //Manager to be called when DeviceInfo's parameter gets changed.
    private DeviceInfo.ChangeManager mDeviceInfoChangeManager = new DeviceInfo.ChangeManager() {
        @Override
        public void onEngageRequest(DeviceInfo sourceDeviceInfo, boolean engaged) {
            mCommonServiceExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    if (mPhidBLDCMotorPositionController != null) {
                        MotorPositioner motorPositioner = null;
                        for (MotorPositioner positioner : mPhidBLDCMotorPositionController) {
                            DeviceChannel deviceChannel = positioner.getDeviceChannel();
                            DeviceInfo deviceInfo = mDeviceInfoMap.get(deviceChannel.getLabel());
                            if (deviceInfo != null && sourceDeviceInfo == deviceInfo) {
                                motorPositioner = positioner;
                                break;
                            }
                        }
                        if (motorPositioner != null) {
                            motorPositioner.setCanEngage(engaged);
                            try {
                                motorPositioner.getController().setEngaged(engaged);
                            } catch (PhidgetException ex) {
                                return;
                            }
                            try {
                                while (motorPositioner.getController().getEngaged() != engaged) {
                                    sleep(100);
                                }
                            } catch (PhidgetException ex) {
                                return;
                            } catch (InterruptedException ex) {
                                return;
                            }
                            motorPositioner.setEngagedForced(engaged);
                        }
                    }
                }
            });
        }
    };

    private RotationLimitSwitch mLeftRotationSwitch;
    private RotationLimitSwitch mRightRotationSwitch;
    //Listener for change of state of limit switches.
    private RotationLimitSwitch.StateChangeListener mRotationSwitchListener;

    public WheelDevice(Wheel wheel, DeviceManager deviceManager,
                       ConfigDB configDB, Config appConfig) {

        this.mVelocityLimitSettingWithIndex.add(1.0);
        this.mVelocityLimitSettingWithIndex.add(1.0);

        mWheel = wheel;
        mConfigDB = configDB;
        mAppConfig = appConfig;
        mDeviceManager = deviceManager;

        if (mWheel.getWheelName().equals("FrontLeft")) {
            mStepperMultiplier = -1.0;
        }

        mAllowMysqlLogging = mAppConfig.hasMySQL();
        mAllowMysqlLoggingALTERNATE = false;
        //mySQL.MySQL_Better MySQL_Better = new mySQL.MySQL_Better();
        for (int i = 0; i < 2; i++) {
            DeviceInfo bldcMotorInfo = new DeviceInfo(String.format("BLDC Motor# %d", i));
            bldcMotorInfo.setChangeManager(mDeviceInfoChangeManager);
            mDeviceInfoMap.put(String.format("BLDC%d", i), bldcMotorInfo);
        }
        wheel.setDeviceInfoList(new ArrayList<>(mDeviceInfoMap.values()));
    }

    /**
     * This function will update deviceInfo's parameters from
     * associated deviceChannel.
     *
     * @param deviceChannelList
     */
    public void updateDeviceInfoFromDeviceChannelList(DeviceChannelList deviceChannelList) {
        mCommonServiceExecutor.execute((Runnable) () -> {
            deviceChannelList.forEach((deviceChannel) -> {
                DeviceInfo deviceInfo = mDeviceInfoMap.get(deviceChannel.getLabel());
                if (deviceInfo != null) {
                    String getName = deviceChannel.getName();
                    if (getName != null) { // Allan added this May 17 2019
                        //System.out.println(String.format("  WheelDevice.java: %s, deviceNAe: %s, deviceInfo: %s", getWheel().getWheelName(), getName, deviceInfo.getName()));
                        deviceInfo.setParam("Name", getName);
                    }
                }
            });
        });
    }

    public Wheel getWheel() {
        return mWheel;
    }

    public String getConfigName(String paramName) {
        return mWheel.getWheelName() + "." + paramName;
    }

    public String getDisengageWarning() {
        return disengageWarning;
    }

    public double[] getBLCDCPosIndex() { // return the entire index based on values that have been set by a position listener
        return mBLDCmotorDeviceReadPos.clone();
    }

    public double[] getBLCDCPosMultiplier() {
        //mBLDCMotorPosMult.get(1);
        double[] temp = new double[2];
        temp[0] = mBLDCMotorPosMult.get(0);
        temp[1] = mBLDCMotorPosMult.get(1);
        return temp.clone();
    }

    public double getBLCDCPosAtIndex(int index) { // return one element of the index based on a call to the device
        int indexValue = index;
        if (mPhidBLDCMotorPositionController == null) return 0;

        double temp = mBLDCmotorDeviceReadPos[indexValue];
        return temp;
    }

    public double getBLCDCDutyCyleAtIndex(int index) {
        if (mPhidBLDCMotorPositionController == null) return 0;
        try {
            //double x = mPhidBLDCMotorPositionController.getkPAtIndex(index);
            //System.out.println(String.format(mWheel.getWheelName()+" target kP at index %d is %f", index, x));
            //if(mWheel.getWheelName()=="RearRight"){
            //    System.out.println();// spacer to make the printout look better
            //}
            //System.out.println(String.format("target pos at index %d is %f", index, mPhidBLDCMotorPositionController.getPositionAtIndex(index)));
            return mPhidBLDCMotorPositionController.getDutyCycleAtIndex(index);
        } catch (PhidgetException ex) {
            return 0;
        }
    }

    public String getBLDCPositionControllerStatus() {
        String status;
        if (mBLDCMotorPositionControllerChannelName == null) {
            status = "Null-Channel-Name";
        } else if (mBLDCMotorPositionControllerChannel == null) {
            status = "Null-Channel";
        } else if (!mBLDCMotorPositionControllerChannel.isOpen()) {
            status = "Not-All-Channel-Open" + "<br>Channels:" + mBLDCMotorPositionControllerChannel.getNames();
        } else if (mPhidBLDCMotorPositionController == null) {
            status = "Null-Motor-Controller" + "<br>Channels:" + mBLDCMotorPositionControllerChannel.getNames();
        } else {

            if (!mPhidBLDCMotorPositionController.getEngaged()) {
                status = "Not-Motor-Engaged";
            } else {
                status = "Ok";
            }

            status += "<br>Channels:" + mBLDCMotorPositionControllerChannel.getNames();
            status += "<br>Controllers:" + mPhidBLDCMotorPositionController.getNames();
            //status += "<br>" + mPhidBLDCMotorPositionController.getTargetPositionLimits();
        }
        return String.format("<html>%s</html>", status);
    }

    public String getEncoderStatus() {
        String status;
        if (mEncoderChannelName == null) {
            status = "Null-Channel-Name";
        } else if (mEncoderChannel == null) {
            status = "Null-Channel";
        } else if (!mEncoderChannel.isOpen()) {
            status = "Not-Channel-Open";
        } else if (mPhidEncoder == null) {
            status = "Null-Encoder-Controller";
        } else {
            status = "Ok";
        }
        return status;
    }

    public ChartParamsDataset getChartParamsDataset() {
        if (mChartParamsDataset != null) return mChartParamsDataset;
        mChartParamsDataset = new ChartParamsDataset(
                mWheel.getWheelName(),
                new ChartParamType[]{ // these are the labels below the chart
                        //ChartParamType.BLDC_1_POSITION,
                        //ChartParamType.BLDC_2_POSITION,
                        ChartParamType.BLDC_1_POS_DUTY_CYCLE,
                        ChartParamType.BLDC_2_POS_DUTY_CYCLE,
                        //ChartParamType.BLDC_POS_DUTY_CYCLE,
                        //ChartParamType.DC_VELOCITY
                });
        return mChartParamsDataset;
    }

    /*
     *
     * this updates the dutycycle in the wheel.java so it can be displayed in the rover GUI
     * and
     * it calculates the difference between the two motors and provides an adjustment so the motors stay in sync
     *
     * this could be done on a random basis - ie every 20% of the time or some other method to reduce frequency
     */
    public void updategetBLCDCDutyCyleAtIndex() {
        motor0dutycycle = getBLCDCDutyCyleAtIndex(0) * mBLDCMotorPosMult.get(0) * 1000;
        motor1dutycycle = getBLCDCDutyCyleAtIndex(1) * mBLDCMotorPosMult.get(1) * 1000;
        delta = motor0dutycycle - motor1dutycycle;
        ratio = Math.abs(motor0dutycycle) / Math.abs(motor1dutycycle);
        ratioPreTrunc = ratio;

        if (!Double.isNaN(delta) && Math.abs(delta) > 80) {
//            System.err.println(mWheel.getWheelName()+" add a _delta_ Disengage code here? "+ delta
//                    + " positions "+mBLDCmotorDeviceReadPos[0]+" : "+mBLDCmotorDeviceReadPos[1]
//                    + " mDistanceRemainingRover "+mDistanceRemainingRover);
        }
        double dutyCycleFloor = 50;
        if (!Double.isNaN(ratio) && !Double.isInfinite(ratio) && motor1dutycycle != 0) { //  && ratio<=1.1 && ratio>=0.9
            if (ratio > 1.2) { // truncate tail end values to centralized values
                //if (mWheel.getWheelName()!="FrontRight" && (motor0dutycycle>dutyCycleFloor || motor1dutycycle>dutyCycleFloor)){
                if ((motor0dutycycle > dutyCycleFloor || motor1dutycycle > dutyCycleFloor)) {
                    //System.err.println("TRUNCATING more than 1.2 - any problems? ratio pretrunc= "+ratio+" motor0dutycycle= "+motor0dutycycle+" motor1dutycycle "+motor1dutycycle+" "+mWheel.getWheelName());
                }
                ratio = 1.2;
            }
//            if(ratio<0){
//                if(ratio<-1.2){
//                    ratio=-1.2;
//                }
//                if(ratio>-.8){
//                    ratio=-.8;
//                }
//            }
            else {
                if (ratio < 0.8) { // truncate tail end values to centralized values
                    //if (mWheel.getWheelName()!="FrontRight" &&  (motor0dutycycle>dutyCycleFloor || motor1dutycycle>dutyCycleFloor)){
                    if ((motor0dutycycle > dutyCycleFloor || motor1dutycycle > dutyCycleFloor)) {
                        //System.err.println("TRUNCATING less than 0.8 - any problems? ratio pretrunc= "+ratio+" motor0dutycycle= "+motor0dutycycle+" motor1dutycycle "+motor1dutycycle+" "+mWheel.getWheelName());
                    }
                    ratio = 0.8;
                }
            }
            int recordsToKeep = 10;
            int recordToRemove = 8;
            if (dutyCycleRatioAvgArrayList.size() <= recordsToKeep && dutyCycleRatioAvgArrayList != null) {
                dutyCycleRatioAvgArrayList.add(0, ratio);

            } else {
                if (dutyCycleRatioAvgArrayList == null) {
                    System.err.println("dutyCycleRatioAvgArrayList is null");
                }
            }

            //lopp++;
            //dutyCycleDeviceDeltaList.add(0,(double)lopp);
            //System.err.println("lopp"+lopp);
            recalcRatioFlag = "pending/stale"; // for testing
            if (dutyCycleRatioAvgArrayList.size() > recordsToKeep && dutyCycleRatioAvgArrayList != null) {
                try {
                    removeFromTotal = (double) (dutyCycleRatioAvgArrayList.get(recordToRemove));
                } catch (Exception ex) {
                    System.err.println(mWheel.getWheelName() + " " +
                            removeFromTotal +
                            " dutyCycleRatioAvgArrayList.size= " +
                            dutyCycleRatioAvgArrayList.size() + " " +
                            getBLCDCDutyCyleAtIndex(0) * 1000 + " " +
                            getBLCDCDutyCyleAtIndex(1) * 1000 + " java.util.ConcurrentModificationException part 1 - doesn't seem to cause problems so moving on.");
//                for (int i = 0; i < dutyCycleRatioAvgArrayList.size(); i++){
//                    System.err.print(" "+String.format("%.4f",dutyCycleRatioAvgArrayList.get(i))+"; ");
//                }
//                System.err.println("");


                    // Logger.getLogger(RoverFrontEnd.class.getName()).log(Level.SEVERE, null, ex);
                }
                try {
                    // concurrent modification happening here
                    dutyCycleRatioAvgShortArrayList = new ArrayList<>(dutyCycleRatioAvgArrayList.subList(0, 8));
                } catch (Exception ex) {
                    System.err.println(mWheel.getWheelName() + " " +
                            removeFromTotal +
                            " dutyCycleRatioAvgArrayList.size = " +
                            dutyCycleRatioAvgArrayList.size() + " " +
                            getBLCDCDutyCyleAtIndex(0) * 1000 + " " +
                            getBLCDCDutyCyleAtIndex(1) * 1000 + " java.util.ConcurrentModificationException part 2 - doesn't seem to cause problems so moving on.");
//                for (int i = 0; i < dutyCycleRatioAvgArrayList.size(); i++){
//                    System.err.print(" "+dutyCycleRatioAvgArrayList.get(i)+"; ");
//                }                
//                System.err.println("");
                    // Logger.getLogger(RoverFrontEnd.class.getName()).log(Level.SEVERE, null, ex);
                }
                try {
                    if (dutyCycleRatioAvgShortArrayList.size() != 8) {
                        System.err.println("dutyCycleRatioAvgShortArrayList not equal to 8; it is -> " + dutyCycleRatioAvgShortArrayList.size());
                    }
                } catch (Exception ex) {
                    System.err.println(mWheel.getWheelName() + " " +
                            removeFromTotal +
                            " dutyCycleRatioAvgArrayList.size= " +
                            dutyCycleRatioAvgArrayList.size() + " " +
                            getBLCDCDutyCyleAtIndex(0) * 1000 + " " +
                            getBLCDCDutyCyleAtIndex(1) * 1000 + " java.util.ConcurrentModificationException part 3 - doesn't seem to cause problems so moving on.");
//                for (int i = 0; i < dutyCycleRatioAvgArrayList.size(); i++){
//                    System.err.print(" "+dutyCycleRatioAvgArrayList.get(i)+"; ");
//                }                
//                System.err.println("");
                    // Logger.getLogger(RoverFrontEnd.class.getName()).log(Level.SEVERE, null, ex);
                }
                try {
                    dutyCycleRatioAvgArrayList.remove(recordToRemove); // remove the last element in the list - is this being updated by two different devices and changing too fast?
                } catch (Exception ex) {
                    System.err.println(mWheel.getWheelName() + " " +
                            removeFromTotal +
                            " dutyCycleRatioAvgArrayList.size= " +
                            dutyCycleRatioAvgArrayList.size() + " " +
                            getBLCDCDutyCyleAtIndex(0) * 1000 + " " +
                            getBLCDCDutyCyleAtIndex(1) * 1000 + " java.util.ConcurrentModificationException part 4 - doesn't seem to cause problems so moving on.");
//                for (int i = 0; i < dutyCycleRatioAvgArrayList.size(); i++){
//                    System.err.print(" "+dutyCycleRatioAvgArrayList.get(i)+"; ");
//                }                
//                System.err.println("");
                    // Logger.getLogger(RoverFrontEnd.class.getName()).log(Level.SEVERE, null, ex);
                }

                grandTotalNumInstances = 0.0;
                dutyCycleRatioAvgShortArrayListsize = dutyCycleRatioAvgShortArrayList.size();
                for (int loo = 0; loo < dutyCycleRatioAvgShortArrayListsize; loo++) {
                    grandTotalNumInstances = grandTotalNumInstances + dutyCycleRatioAvgShortArrayList.get(loo);
                }

                ratioFactorToFixDutyCycle = grandTotalNumInstances / dutyCycleRatioAvgShortArrayListsize;
                if (Double.isInfinite(ratioFactorToFixDutyCycle)) {
                    ratioFactorToFixDutyCycle = 1;
                    System.err.println("WARNING - see if this can be handled better (ratioFactorToFixDutyCycle is infinite)");
                }
                recalcRatioFlag = "complete/current"; // for testing
            }
        }
            
            /*
            if  (mAllowMysqlLogging) {
                double ratioTemp = ratio;
                double deltaTemp = delta;
                double ratioPreTruncTemp = ratioPreTrunc;
                if(Double.isNaN(delta)){
                    deltaTemp = 999;
                };
                if(Double.isNaN(ratio) || Double.isInfinite(ratio)){
                    ratioTemp = 999;
                };
                if(Double.isNaN(ratioPreTrunc) || Double.isInfinite(ratioPreTrunc)){
                    ratioPreTruncTemp = 999;
                };
                //System.err.println("ratioPreTruncTemp "+ratioPreTruncTemp+" dutyCycleRatioAvgShortArrayListsize "+dutyCycleRatioAvgShortArrayListsize);
            
                        MysqlLogger.put(MysqlLogger.Type.DUTYCYCLE, (float)mReadDutyCycle, "mReadDutyCycleListener", 
                                mBatch_time_stamp_into_mysql, mWheel.getWheelName(),"MySQL_DUTYCYCLE",motor0dutycycle,motor1dutycycle,
                                deltaTemp,ratioTemp,ratioPreTruncTemp,
                                (double)dutyCycleRatioAvgShortArrayListsize,
                                grandTotalNumInstances,ratioFactorToFixDutyCycle,recalcRatioFlag,
                                mTargetPositionRoverBody,getBLCDCPosAtIndex(0),getBLCDCPosAtIndex(1));
                    }
              */

//            NumberFormat nf = new DecimalFormat("000.0");
//            System.out.print(lopp+": ");

//        if(mWheel.getWheelName().equals("RearRight")){
//                    System.out.println(mWheel.getWheelName()+": "+String.format("%02.1f",motor0dutycycle)
//                    +" : "+String.format("%02.1f",motor1dutycycle)+" = delta "+String.format("%02.1f",delta)
//                    +" ratio: "+String.format("%02.2f",ratio)
//                    +" ratioPreTrunc: "+String.format("%02.2f",ratioPreTrunc) 
//                    +" less: "+String.format("%02.3f",removeFromTotal)
//                    +" = "+String.format("%02.2f",grandTotalNumInstances)
//                    +" ratioFactorToFixDutyCycle: "+String.format("%02.2f",ratioFactorToFixDutyCycle)+"; "+
//                    recalcRatioFlag);
//        }

        if (!Double.isNaN(ratioFactorToFixDutyCycle)) { //  && ratio<=1.1 && ratio>=0.9
            if (ratioFactorToFixDutyCycle > 1.5) { // truncate tail end values to centralized values
                System.err.println(mWheel.getWheelName() + " - do we need to have a wider band here? over 1.5 now " + ratioFactorToFixDutyCycle);
                ratioFactorToFixDutyCycle = 1.5;
            }
            if (ratioFactorToFixDutyCycle < 0.4) { // truncate tail end values to centralized values
                //System.err.println(mWheel.getWheelName()+" TESTING - do we need to have a wider band here? under 0.4 now "+ratioFactorToFixDutyCycle);
            }
            if (ratioFactorToFixDutyCycle < 0.1) { // truncate tail end values to centralized values
                if (mWheel.getWheelName().equals("RearRight")) {
                    System.err.println(mWheel.getWheelName() + " - do we need to have a wider band here? under 0.1 now " + ratioFactorToFixDutyCycle);
                }
                ratioFactorToFixDutyCycle = 0.1;
            }
            setVelocity_AccelAtBLDC_MPC();
            //set target position?
        }

        mWheel.setBLCDCDutyCyleAtIndex(0, motor0dutycycle);
        mWheel.setBLCDCDutyCyleAtIndex(1, motor1dutycycle);
    }

    public void updateChartParamsDataset() {
        // speed of chart updates is controlled here -> mUpdaterTimer within UI and possibly Rover UI?
        if (mChartParamsDataset == null) return; // lines shown here show up on the chart
//        mChartParamsDataset.addValue(
//                ChartParamType.BLDC_1_POSITION, getBLCDCPosAtIndex(0)*(mBLDCMotorPosMult.get(0) < 0 ? -1: 1)); // if/then keeps charts looking correct even if motor physical connection reversed
//        mChartParamsDataset.addValue(
//                ChartParamType.BLDC_2_POSITION, getBLCDCPosAtIndex(1)*(mBLDCMotorPosMult.get(1) < 0 ? -1: 1)+(mBLDCMotorPosMult.get(1) < 0 ? -1: 1)*2); // small offset so chart lines don't overlap
        mChartParamsDataset.addValue(ChartParamType.BLDC_1_POS_DUTY_CYCLE, getBLCDCDutyCyleAtIndex(0) * 1000 * (mBLDCMotorPosMult.get(0) < 0 ? -1 : 1));
        //System.out.println(getBLCDCDutyCyleAtIndex(0)*1000);
        mChartParamsDataset.addValue(ChartParamType.BLDC_2_POS_DUTY_CYCLE, getBLCDCDutyCyleAtIndex(1) * 1000 * (mBLDCMotorPosMult.get(1) < 0 ? -1 : 1) + (mBLDCMotorPosMult.get(1) < 0 ? -1 : 1) * 2);
        //mChartParamsDataset.addValue(ChartParamType.BLDC_POS_DUTY_CYCLE, getBLDCDutyCycle());
        //mChartParamsDataset.addValue(ChartParamType.DC_VELOCITY, mPhidDCMotorVelocity);
    }

    /**
     * @param addForwardAngle in WheelDevice.java
     *                        prior to Sept 12 2018 the cumulative math was done within Truck.java addForwardDistance
     *                        speedratio in this method is actually a distance ratio - it allows wheels to move different distances when turning corners since inside wheels will need to travel a shorter distance than outside wheels to complete the circumfrance of the circle.
     */
    public void setVelocity_AccelAtBLDC_MPC() {
        mVelocityLimitSetting = mWheel.getVelocityLimitSetting() * mWheel.getSpeedRatio(mDistanceRemainingRover);
        //System.out.println(mWheel.getWheelName()+ " mVelocityLimitSetting " + mVelocityLimitSetting);
        // getVelocityLimitSetting() returns the ?rover? velocity set by the GUI
        //  mWheel.getSpeedRatio(mDistanceRemainingRover) sets the ratio for the wheels relative to each other and rover body

        double stopValue = 1;
//        if(Math.abs(mDistanceRemainingRover)<25){
//        if((mDistanceRemainingRover)<-25){
//            stopValue = 0;
//            System.err.println("");
//            System.err.println(mWheel.getWheelName()+" STOPPING with stopValue "+mVelocityLimitSetting +" "+ mVelocityVariableSetting );
//            System.err.println("");
//            //mDevicesDisengaed = true;
//            //if (!mDevicesDisengaed) {
//            if (mBLDCMotorPositionControllerChannel != null){ 
//                if(mBLDCMotorPositionControllerChannel.isOpen()) {
//                    if (mPhidBLDCMotorPositionController != null) {
//                        if(!mPhidBLDCMotorPositionController.getEngaged()){
//                            mPhidBLDCMotorPositionController.setEngaged(false);
//                            System.err.println("disengaged due to reaching target");
//                        }
//                    }
//                }
//            }
//        }

        mVelocityLimitSettingWithIndex.set(0, mVelocityLimitSetting * mVelocityVariableSetting * stopValue); //  mVelocityVariableSetting allows for slowing the motor at start and end of range so it doesn't stop so fast - perhaps doubling up on acceleration factor?
        mVelocityLimitSettingWithIndex.set(1, mVelocityLimitSetting * mVelocityVariableSetting * ratioFactorToFixDutyCycle * stopValue);// * ratioFactorToFixDutyCycle);

//        if(mWheel.getWheelName().equals("RearRight")){
//            System.err.println("velocitylimits "+ mWheel.getWheelName()
//                    +" mVelocityLimitSetting: "+String.format("%02.2f",mVelocityLimitSetting)
//                    +" mVelocityVariableSetting: "+String.format("%02.2f",mVelocityVariableSetting)
//                    +" ratioFactorToFixDutyCycle: "+String.format("%02.2f",ratioFactorToFixDutyCycle)
//                    +" equals: "+String.format("%02.2f",(mVelocityLimitSetting * mVelocityVariableSetting * ratioFactorToFixDutyCycle )));//?????? is this an int?
//        }

        if (mVelocityLimitSettingWithIndex.get(1) != 0) {
            double delta = (mVelocityLimitSettingWithIndex.get(0) - mVelocityLimitSettingWithIndex.get(1)) / mVelocityLimitSettingWithIndex.get(1);
            if (delta > 0.2) {
                //System.err.println("--------------------------------velocity ratio too high "+ mWheel.getWheelName()+" "+delta);
            }
            if (delta < -0.25) {
                //System.err.println("--------------------------------velocity ratio too low "+ mWheel.getWheelName()+" "+delta);
            }
        }

        if (mPhidBLDCMotorPositionController != null && mPhidBLDCMotorPositionController.getEngaged()) {
            mPhidBLDCMotorPositionController.setVelocityLimitWithIndex(mVelocityLimitSettingWithIndex, mWheel.getWheelName());
        } else {
            //System.err.println("---------------------error setting mVelocityLimitSettingWIthIndex --- is this causing problems? if not; just keep moving on...");
        }

    }

    public double getmTargetPositionRoverBody() {
        return mTargetPositionRoverBody;
    }

    /**
     * @param addForwardAngle in WheelDevice.java
     *                        speedratio in this method is actually a distance ratio - it allows wheels to move different distances when turning corners since inside wheels will need to travel a shorter distance than outside wheels to complete the circumference of the circle.
     *                        this is called by the TruckDevice.java. However, Wheel.java calls getBLDCPosAtIndex so it has the positions of all the devices at the wheel level.
     *                        For actions across all wheels it would be done at the TruckDevice or at a Static level in Wheel.java.
     */
    public void addForwardDistance(double mForwardDistance) {
        mTargetPositionRoverBody += mForwardDistance;// * mWheel.getSpeedRatio(); old code worked only if we used only 1 steering angle for the duration of the distance - if we changed angle during duration of travel it wouldn't work correctly
        if (mPhidBLDCMotorPositionController != null) {
            mPhidBLDCMotorPositionController.setAcceleration(25, mWheel.getWheelName());
            //System.out.println("Deadband set to 20.");
            wideDeadBandSet = false;
            mPhidBLDCMotorPositionController.setDeadBand(4, mWheel.getWheelName()); // this is the full range of the db. plus db/2 and minus db/2
        }
    }

    /**
     * this is called each time the user increments the distance and each time the steering position changes
     *
     * @param distanceRemainingRover
     */
    public void setDistanceTarget(double distanceRemainingRover) {

        mDistanceRemainingRover = distanceRemainingRover;

        if (mWheel.getsetEmergencyStopSetVelocityToZero()) {
            mDistanceRemainingRover = 0;
            // System.err.println(mWheel.getWheelName()+ " actual positions: "+mBLDCmotorDeviceReadPos[0] +" : "+ mBLDCmotorDeviceReadPos[1]+" target positions "+mTargetPositionWheel);
        }

        // this should be split into a new method so the mDistanceRemainingRover variable can be set seperately from the action - and/or the action can be taken from multiple places.

        double NewSR = mWheel.getSpeedRatio(mDistanceRemainingRover);

        if (NewSR != OldSR || Math.abs(mDistanceRemainingRover) > 25.0) {
            mTargetPositionWheel = mDistanceRemainingRover * mWheel.getSpeedRatio(mDistanceRemainingRover) + avgActualWheelPos; // in TruckDevice make sure total number of wheels is correct in 'double avgAllDistances = sumAllDistances / 4.00;'
            // mTargetPositionWheel is based partially on avgActualWheelPos which may cause some slight problems. For example; if the target is set to 150 but the wheel only goes to 147; 
            // the next increment doesn't add to 150, it adds to 147. So cumulatively there may be a problem.
            // Perhaps the new mTargetPositionWheel could be the same formula and substitute mTargetPositionWheel for avgActualWheelPos?
            // I'm not sure what the implications are during turns.

            String spacer = "";
            if (mWheel.getWheelName().equals("FrontLeft")) {
                spacer = " ";
            }
            if (mWheel.getWheelName().equals("RearLeft")) {
                spacer = " ";
            }
            if (mWheel.getWheelName().equals("RearRight")) {
                spacer = " ";

//                System.err.println(mWheel.getWheelName()+spacer+" mTargetPositionRoverBody "+String.format("%02.0f",mTargetPositionRoverBody)+
//                        "; mTargetPositionWheel = "+String.format("%02.1f",mTargetPositionWheel)+
//                        " based on mDistanceRemainingRover "+String.format("%02.1f",mDistanceRemainingRover)+
//                        " * mWheel.getSpeedRatio() = "+ String.format("%02.3f",mWheel.getSpeedRatio(mDistanceRemainingRover))+
//                        " + avgActualWheelPos "+avgActualWheelPos+" (note from wheeldevice.java)");
            }

//                            System.err.println(mWheel.getWheelName()+spacer+" mTargetPositionRoverBody "+String.format("%02.0f",mTargetPositionRoverBody)+
//                        "; mTargetPositionWheel = "+String.format("%02.1f",mTargetPositionWheel)+
//                        " based on mDistanceRemainingRover "+String.format("%02.1f",mDistanceRemainingRover)+
//                        " * mWheel.getSpeedRatio() = "+ String.format("%02.3f",mWheel.getSpeedRatio(mDistanceRemainingRover))+
//                        " + avgActualWheelPos "+avgActualWheelPos+" (note from wheeldevice.java)");

            OldSR = NewSR;
        }
//                        System.err.println(mWheel.getWheelName()+" mTargetPositionRoverBody "+String.format("%02.0f",mTargetPositionRoverBody)+
//                        "; mTargetPositionWheel = "+String.format("%02.1f",mTargetPositionWheel)+
//                        " based on mDistanceRemainingRover "+String.format("%02.1f",mDistanceRemainingRover)+
//                        " * mWheel.getSpeedRatio() = "+ String.format("%02.3f",mWheel.getSpeedRatio(mDistanceRemainingRover))+
//                        " + avgActualWheelPos "+avgActualWheelPos+" (note from wheeldevice.java)");
//        
        NumberFormat nf = new DecimalFormat("000.0");

        // problem solving notes:
        // delta variable doesn't seem to be needed at all with only 1 motor per wheel. It is used if there are 2 motors per wheel
        // ratio needs to be validated by looking at the gear ratios on each motor's gearbox.
        // why does frontleft motor0dutycycle start out negative when all the others are positive?

//                            System.err.println(mWheel.getWheelName()+" motor0dutycycle: "+nf.format(motor0dutycycle)+" motor1dutycycle: "+
//                                nf.format(motor1dutycycle)+
//                                " ratio: "+ String.format("%02.2f",ratio) +
//                                " less: "+String.format("%02.2f",removeFromTotal)+" = "+String.format("%02.1f",grandTotalNumInstances)+
//                                " avg> "+ String.format("%02.2f",ratioFactorToFixDutyCycle)+" "
//                                + " recalcRatioFlag [" + recalcRatioFlag+
//                                "] ActualPositions: "+mBLDCmotorDeviceReadPos[0]+" : "+mBLDCmotorDeviceReadPos[1]+ 
//                                " mTargetPositionWheel "+nf.format(mTargetPositionWheel));
//                            

        mWheel.setdistanceRemainingRover(mDistanceRemainingRover); // this is for displaying onto the GUI

        mVelocityVariableSetting = 1.0; // set/reset it to the default value prior to using the if statement to reduce it.
        if (Math.abs(mDistanceRemainingRover) < 400.0) {
            mVelocityVariableSetting = 0.5;


            if (Math.abs(mDistanceRemainingRover) < 150.0) {
                mVelocityVariableSetting = 0.25;
            }
            //System.out.println(" mDistanceRemainingRover: " + mDistanceRemainingRover + " delta " + delta);
            if (Math.abs(mDistanceRemainingRover) < 25.0 && Math.abs(delta) > 40) { // 15 + 40

//            mVelocityVariableSetting = 0.00;
//            mDistanceRemainingRover = 0;
//            setVelocity_AccelAtBLDC_MPC();

//                if (delta > 0){ // inside the larger if statement from above; the abs value of delta is already above 40 - this is used to determine if the actual value is above/below zero.
//                    if(mTargetPositionMotor0>0){
//                    mTargetPositionMotor0 = mTargetPositionMotor0-1; // reminder: "delta = motor0dutycycle-motor1dutycycle"
//                    }
//                    else {
//                    mTargetPositionMotor0 = mTargetPositionMotor0+1; // reminder: "delta = motor0dutycycle-motor1dutycycle"
//                    }
//                }
//                else { // if delta <=0
//                    if(mTargetPositionMotor0>0){
//                    mTargetPositionMotor0 = mTargetPositionMotor0-1; // reminder: "delta = motor0dutycycle-motor1dutycycle"
//                    }
//                    else {
//                    mTargetPositionMotor0 = mTargetPositionMotor0+1; // reminder: "delta = motor0dutycycle-motor1dutycycle"
//                    }
//                }
//                
//                if(mPhidBLDCMotorPositionController !=null){
//                    mPhidBLDCMotorPositionController.setTargetPositionOneMotor(0, mTargetPositionMotor0, mBLDCMotorPosMult);
//                }//
//                    double tem0 = 0;
//                    double tem1 = 0;
//                    double tem2 = 0;
//                    double tem3 = 0;
//                    double tem4 = 0;
//                    double tem5 = 0;
//                 
//                    /**
//                     * this line (tem0=....) seems to hang if it is called too early in the running of the app? Nov 2 2019.
//                     * it is called in another part of the code and it works ok there. It works here if the
//                     * 'if/then' statement has delta>40 in it but if I remove this it hangs... not sure why
//                     */
//                    tem0 = mPhidBLDCMotorPositionController.getPositionAtIndex(0, mWheel.getWheelName());
//                    tem1 = mPhidBLDCMotorPositionController.getPositionAtIndex(1, mWheel.getWheelName());
//                    tem2 = mPhidBLDCMotorPositionController.getWheelTargetPosition();
//                    tem3 = mPhidBLDCMotorPositionController.getTargetPositionOneMotor(0);
//                    tem4 = mPhidBLDCMotorPositionController.getTargetPositionOneMotor(1);
//                    tem5 = mPhidBLDCMotorPositionController.getDeadBand_AtIndex(0, mWheel.getWheelName());
//                  
//            System.err.println(tem5 + mWheel.getWheelName()+"_____adjusting_ "
//                    + " delta " + String.format("%02.1f",delta)
//                    + " WheelTarget " + String.format("%02.1f",mTargetPositionWheel)
//                    + " actual positions " + String.format("%02.1f",tem0)+" : " + String.format("%02.1f",tem1)
//                    + " duty0&1: " + String.format("%02.1f",motor0dutycycle)+" : " + String.format("%02.1f",motor1dutycycle)
//                    + " WheelTarget per MPC: "+String.format("%02.1f",tem2)
//                    + " motor target: "+String.format("%02.1f",tem3) + " : "+String.format("%02.1f",tem4)
//                    + " mDistanceRemainingRover " + String.format("%02.1f",mDistanceRemainingRover)
//                    );
                //System.err.println(mWheel.getWheelName()+" read this whole message! _____adjusting mVelocityVariableSetting to 0% (need to adjust mTargetPositionWheel so both motors dont force themselves to get to the same exact position) "); // and mTargetPositionWheel "+mTargetPositionWheel);

            }
        }

        if (Math.abs(mDistanceRemainingRover) < 25.0) {
//            System.err.println(mDistanceRemainingRover+ " reached target so setting wide deadband to stop motors;"
//                                + " disengaging would be better but hard to code.  Or, should we set speed to zero here? ");

            if (mPhidBLDCMotorPositionController != null && !wideDeadBandSet) {
                wideDeadBandSet = true;
                mPhidBLDCMotorPositionController.setDeadBand(4, mWheel.getWheelName());

                //System.err.println("Deadband has been set to: " + mPhidBLDCMotorPositionController.getDeadBand_AtIndex(0, "Frontleft"));
                /*
                following code works to setEngaged(false) but the rengage code 'reEngageUponCommand()' isn't correct yet.
                */
//                if(mWheel.getWheelName().equals("FrontRight")){
//                    //System.err.println("mReadDutyCycle "+mReadDutyCycle);
//                    if(Math.abs(mReadDutyCycle)>(0.1*1000)) {
//                        allowEngage = false;
//                        mPhidBLDCMotorPositionController.setEngaged(false);
//                        System.err.println("testing engage/disengage by wheel. Front Right is now disengaged........................................................");
//                        System.err.println("testing engage/disengage by wheel. Front Right is now disengaged........................................................");
//                        System.err.println("testing engage/disengage by wheel. Front Right is now disengaged........................................................");
//                    }
//                }
            }
        }

        if (mPhidBLDCMotorPositionController != null) {
            mPhidBLDCMotorPositionController.setTargetPosition(mTargetPositionWheel, mBLDCMotorPosMult);
        }
        //avgActualWheelPos = (Math.abs(mBLDCmotorDeviceReadPos[0])+Math.abs(mBLDCmotorDeviceReadPos[1]))/2.0; //instead of abs should i take this times the multiplier?
        avgActualWheelPos = (mBLDCmotorDeviceReadPos[0] * mBLDCMotorPosMult.get(0));//+(mBLDCmotorDeviceReadPos[1]*mBLDCMotorPosMult.get(1)))/2.0;
        if (mWheel.getWheelName() == "RearRight") {
            System.out.println(mWheel.getWheelName() + " mTargetPositionWheel " + String.format("%02.0f", mTargetPositionWheel) + " mBLDCMotorPosMult " + mBLDCMotorPosMult + " avgActualWheelPos " + avgActualWheelPos + " from " + mBLDCmotorDeviceReadPos[0] + " X " + mBLDCMotorPosMult.get(0) + " row 850 in WheelDevice.java");
        }


        //if(mWheel.getWheelName()=="RearRight"){System.out.println("");};//line spacing to make reading output easier
    }


    public void reEngageUponCommand() {
        if (mWheel.getWheelName().equals("FrontRight")) {
            allowEngage = true;
            mPhidBLDCMotorPositionController.setEngaged(true);
            System.err.println("testing engage/disengage by wheel. Front Right is ENGAGED........................................................");
            System.err.println("testing engage/disengage by wheel. Front Right is ENGAGED........................................................");
            System.err.println("testing engage/disengage by wheel. Front Right is ENGAGED........................................................");
        }
    }

    public void EncoderConnectionSetup() {
        try {
            mPhidEncoder = new Encoder();
        } catch (PhidgetException ex) {
            Logger.getLogger(RoverFrontEnd.class.getName()).log(Level.SEVERE, null, ex);
        }

        mPhidEncoder.addAttachListener(new AttachListener() {
            public void onAttach(AttachEvent ae) {
                Encoder phid = (Encoder) ae.getSource();
                try {
                    if (phid.getDeviceClass() != DeviceClass.VINT) {
                        System.out.println("ENCODERchannel " + phid.getChannel() + " " + phid.getDeviceName() + " on device " + phid.getDeviceSerialNumber() + " attached");
                    } else {
                        System.out.println("ENCODERchannel " + phid.getChannel() + " " + phid.getDeviceName() + " on device " + phid.getDeviceSerialNumber() + " hub port " + phid.getHubPort() + " attached");
                    }
                } catch (PhidgetException ex) {
                    System.out.println("ex.getDescription " + ex.getDescription());
                }
            }

        });
    }

    private void detectBLDCMotors() {
        if (mBLDCMotorPositionControllerChannelName == null) {
            mBLDCMotorPositionControllerChannelName = mConfigDB.getValue(
                    getConfigName(BLDCMOTOR_POSITION_CONTROLLER_NAMES), null);
        } else {
            String newBLDCPosNames = mConfigDB.getValue(
                    getConfigName(BLDCMOTOR_POSITION_CONTROLLER_NAMES), null);

            //Adjust the lsit if configuration is  changed while running
            if (!mBLDCMotorPositionControllerChannelName.equals(newBLDCPosNames) && mBLDCMotorPositionControllerChannel != null) {
                DeviceChannelList newBLDCList = mDeviceManager.getChannelListByNames(newBLDCPosNames.split(","));
                ArrayList<Integer> removableIndices = new ArrayList<Integer>();
                int i = mBLDCMotorPositionControllerChannel.size() - 1;
                //Remove the unused BLDC Channels
                while (i >= 0 && i < mBLDCMotorPositionControllerChannel.size()) {
                    if (newBLDCList.indexOf(mBLDCMotorPositionControllerChannel.get(i)) < 0) {
                        if (mPhidBLDCMotorPositionController != null) {
                            mPhidBLDCMotorPositionController.remove(i);
                        }
                        mBLDCMotorPositionControllerChannel.remove(i);
                    } else {
                        i--;
                    }
                }
                //Add the new BLDC Channels
                i = newBLDCList.size() - 1;
                while (i >= 0) {
                    if (mBLDCMotorPositionControllerChannel.indexOf(newBLDCList.get(i)) < 0) {
                        mBLDCMotorPositionControllerChannel.add(newBLDCList.get(i));
                        if (mPhidBLDCMotorPositionController != null) {
                            mPhidBLDCMotorPositionController.add(
                                    new MotorPositioner(
                                            newBLDCList.get(i).getMotorPos(),
                                            newBLDCList.get(i)
                                    )
                            );
                        }
                    }
                    i--;
                }
                //update channel lables
                mBLDCMotorPositionControllerChannel.setSequentialChannelLabel("BLDC");
                mBLDCMotorPositionControllerChannel.addDeviceChannelChangeListener(mDeviceChannelListener);
                updateDeviceInfoFromDeviceChannelList(mBLDCMotorPositionControllerChannel);
                mBLDCMotorPositionControllerChannelName = newBLDCPosNames;
            }
        }

        if (mBLDCMotorPositionControllerChannelName != null &&
                (mPhidBLDCMotorPositionController == null ||
                        !mPhidBLDCMotorPositionController.getEngaged())) {
            if (mBLDCMotorPositionControllerChannel == null) {
                mBLDCMotorPositionControllerChannel =
                        mDeviceManager.getChannelListByNames(
                                mBLDCMotorPositionControllerChannelName.split(","));
                mBLDCMotorPositionControllerChannel.setSequentialChannelLabel("BLDC");
                mBLDCMotorPositionControllerChannel.addDeviceChannelChangeListener(mDeviceChannelListener);
                updateDeviceInfoFromDeviceChannelList(mBLDCMotorPositionControllerChannel);
            }
            if (mBLDCMotorPositionControllerChannel != null) {
                mBLDCMotorPositionControllerChannel.open();
                if (mBLDCMotorPositionControllerChannel.isOpen()) {
                    if (mPhidBLDCMotorPositionController == null) {
                        mPhidBLDCMotorPositionController =
                                mBLDCMotorPositionControllerChannel.getMotorPosList();
                    }
                    if (mPhidBLDCMotorPositionController != null) {
                        if (allowEngage) {
                            mPhidBLDCMotorPositionController.setEngaged(true);
                            //if(mWheel.getWheelName().equals("FrontRight")){
                            System.err.println("row 897 s/b now engaged (wheel driving motors).............." + mWheel.getWheelName()
                                    + " Engaged? " + mPhidBLDCMotorPositionController.getEngaged()
                            );
                            //}                            
                        }
                        add_BLDC_POSChangeListener();
                        add_DutyCycleListener();
                    }
                }
            }
        }
    }

    /**
     * Waring: This method should not be called on main thread
     */
    public boolean detectDevices(boolean forced) {
        if (mDevicesDisengaed && !forced) return false;
        boolean result = false;
        synchronized (mOpLock) {
            if (forced) {
                //mBLDCMotorChannel = null;
                mEncoderChannel = null;
                //mBLDCMotorChannelName = null;
                mBLDCMotorPositionControllerChannelName = null;
                mEncoderChannelName = null;
                mDevicesDisengaed = false;
            }
            result = detectDevices();
        }
        return result;
    }

    /**
     * Waring: This method should not be called on main thread
     */
    private boolean detectDevices() {
        if (mDevicesDisengaed) return false;
        //add something to the image views that shows the same point as the rover moves forward - to follow a single plant as i drive - can then weed everywere except whre the plant it
        detectBLDCMotors();

        if (mWheel.getWheelName().equals("FrontLeft") || mWheel.getWheelName().equals("FrontRight")) {
            if (bldc1 == null) {
                createBLDC_forSteering();
            }
        }

        if (mEncoderChannelName == null) {
            mEncoderChannelName = mConfigDB.getValue(
                    getConfigName(ENCODER_NAME), null);
        }

        if (mEncoderChannelName != null) {
            mEncoderChannel = mDeviceManager.getChannelByName(mEncoderChannelName);
            if (mEncoderChannel != null && mPhidEncoder == null) {
                mEncoderChannel.open();
                //System.out.println("mEncoderChannel channel connected 1b2 "+mEncoderChannel);
                if (mEncoderChannel.isOpen()) {
                    mPhidEncoder = mEncoderChannel.getEncoder();
                }
            }
        }

        mBLDCMotorPosMult.set(0, mConfigDB.getValue(
                getConfigName(BLDC_1_POSITION_MULTIPLIER), mBLDCMotorPosMult.get(0)));
        mBLDCMotorPosMult.set(1, mConfigDB.getValue(
                getConfigName(BLDC_2_POSITION_MULTIPLIER), mBLDCMotorPosMult.get(1)));

//        if(mWheel.getWheelName().equals("RearRight")){
//            System.out.println("");
//            System.out.println(mWheel.getDeviceInfoListString()+" mBLDCMotorPosMult.get(0) "+mBLDCMotorPosMult.get(0)+" mBLDCMotorPosMult.get(1) "+mBLDCMotorPosMult.get(1));
//        }

        return (mPhidBLDCMotorPositionController != null && mPhidEncoder != null);
    }

    public void setVelocityLimitSettingWithIndex(int index, double velocity) {
        mVelocityLimitSettingWithIndex.set(index, velocity);
    }

    public void add_BLDC_POSChangeListener() {
        while (mPhidBLDCMotorPositionController == null) {
            System.out.println("----- waiting to connect add_BLDC_POSChangeListener on " + mWheel.getWheelName());
            try {
                sleep(2000);
            } catch (InterruptedException ex) {
                Logger.getLogger(WheelDevice.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        mPhidBLDCMotorPositionController.addPositionChangeListener(// null pointer error here may mean that add_DutyCycleListener is starting too soon in the startup sequence
                new MotorPositionControllerPositionChangeListener() {
                    public void onPositionChange(MotorPositionControllerPositionChangeEvent e) {

                        mBLDCmotorDeviceReadPos[0] = mPhidBLDCMotorPositionController.getPositionAtIndex(0, mWheel.getWheelName()); //getBLCDCPosAtIndex(0);

                        mWheel.setBLDCmotorReadPos(0, mBLDCmotorDeviceReadPos[0], (String) "WheelDevice");

//                        mBLDCmotorDeviceReadPos[1] = mPhidBLDCMotorPositionController.getPositionAtIndex(1,mWheel.getWheelName()); //getBLCDCPosAtIndex(1);
//                    
//                    mWheel.setBLDCmotorReadPos(1, mBLDCmotorDeviceReadPos[1], (String)"WheelDevice");

                        mWheel.setMotorPositionControllerList(mBLDCMotorPositionControllerChannel.getMotorPosList());

//                    if  (mAllowMysqlLogging) {                        
//                        MysqlLogger.put(MysqlLogger.Type.BETTER, (float)mBLDCmotorDeviceReadPos[0], "mBLDCmotorReadPos", mBatch_time_stamp_into_mysql, mWheel.getWheelName(),"MySQL_Better5");
//                    }
                    }
                });
    }

    public void add_DutyCycleListener() {
        while (mPhidBLDCMotorPositionController == null) {
            System.out.println("----- waiting to connect add_dutycyclelistener on " + mWheel.getWheelName());
            try {
                sleep(3000);
            } catch (InterruptedException ex) {
                Logger.getLogger(WheelDevice.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        mPhidBLDCMotorPositionController.addDutyCycleUpdateListener( // null pointer error here may mean that add_DutyCycleListener is starting too soon in the startup sequence
                new MotorPositioner.DutyCycleListener() {
                    @Override
                    public void onUpdate(MotorPositioner positioner, MotorPositionControllerDutyCycleUpdateEvent mpcdc) {

                        //System.err.println(tsNow+mWheel.getWheelName());
                        mReadDutyCycle = mpcdc.getDutyCycle() * 1000;
                        //mWheel.setReadDutyCycle(mReadDutyCycle);
                        updategetBLCDCDutyCyleAtIndex();
                        //mWheel.setBLCDCDutyCyleAtIndex(0, getBLCDCDutyCyleAtIndex(0));
                        //mWheel.setBLCDCDutyCyleAtIndex(1, getBLCDCDutyCyleAtIndex(1));
                        DeviceInfo deviceInfo = mDeviceInfoMap.get(positioner.getDeviceChannel().getLabel());
                        //deviceInfo.setParam(DeviceInfo.DUTY_CYCLE, mReadDutyCycle);
//                    if(mWheel.getWheelName()=="FrontRight"){
//       // 1 device               mPhidBLDCMotorPositionController.get(0).getController().setEngaged(true);
//       //  both devices               mPhidBLDCMotorPositionController.setEngaged(true);
//                        //System.err.println(mBLDCMotorPositionControllerChannel.get(0).getName());//String tsNow = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(new Date());                        
//                    }
                        if ((Math.abs(mReadDutyCycle)) > (0.7 * 1000)) {
                            String timeS = new SimpleDateFormat("HH.mm.ss.SSS").format(new Date());
                            System.err.println("- duty cycle warning - over 0.7*1000 - - - - ratio = " + ratioFactorToFixDutyCycle +
                                    " " + recalcRatioFlag +
                                    " " + mReadDutyCycle + " - - - " + mWheel.getWheelName() + " " + timeS);
                            NumberFormat nf = new DecimalFormat("000.0");
                            System.err.print(lopp + ": ");
                            System.err.println(mWheel.getWheelName() + ": " + nf.format(motor0dutycycle) + " : " +
                                    nf.format(motor1dutycycle) + " = delta " + String.format("%02.1f", delta) + " ratio: " + String.format("%02.2f", ratio) +
                                    " less: " + String.format("%02.2f", removeFromTotal) + " = " + String.format("%02.1f", grandTotalNumInstances) + " avg> " +
                                    String.format("%02.2f", ratioFactorToFixDutyCycle) + " " +
                                    recalcRatioFlag + " ActualPositions: " + mBLDCmotorDeviceReadPos[0] + " : " + mBLDCmotorDeviceReadPos[1] +
                                    " mTargetPositionWheel " + mTargetPositionWheel);
                        }
                        if (Math.abs(mReadDutyCycle) > (0.9 * 1000)) {
                            deviceInfo.requestToSetEngaged(false);
                            mDevicesDisengaed = true; // needed to keep the devices from 'flapping' (going on and off repeatedly in rapid succession which is dangerous)

                            disengageDevicesCloseChannels();
//                            if (mPhidBLDCMotorPositionController != null) {
//                                System.err.println(mWheel.getWheelName() +" "+mPhidBLDCMotorPositionController.getNames()+" SETENGAGED(FALSE)");
//                                mPhidBLDCMotorPositionController.setEngaged(false);
//                            }

                            Long cycle = Math.round(mReadDutyCycle * 1000) / 1000;
                            System.err.println("DISENGAGING!! Duty Cycle Higher than 0.8*1000 (max of 0.8*1000 before error) : " + cycle + " " + mWheel.getWheelName());
                            disengageWarning = "DISENGAGED_" + mWheel.getWheelName();
                            mWheel.setDisnegageWarning(disengageWarning);
                        }

                        if (mAllowMysqlLoggingALTERNATE) {
                            MysqlLogger.put(MysqlLogger.Type.BETTER, (float) mReadDutyCycle, "mReadDutyCycleUPDATER", mBatch_time_stamp_into_mysql, mWheel.getWheelName(), "MySQL_duty2");
                        }
                    }
                }
        );
    }


    /**
     * Channels need to be closed upon app exit or there are
     * difficulties when restarting the app (not 100% of the time
     * so it is difficult to identify the problem).
     * <p>
     * It is good to stop all motors at the same time. if only the offending
     * motor is stopped (aka disengaged) it tends to have
     * bad behaviour (ie it might lurch, etc) when reengaged).
     */
    public void disengageDevicesCloseChannels() {
        mDevicesDisengaed = true;
        synchronized (mOpLock) {
            if (mPhidBLDCMotorPositionController != null) {
                mPhidBLDCMotorPositionController.setEngaged(false);
            }
            if (mBLDCMotorPositionControllerChannel != null) {
                mBLDCMotorPositionControllerChannel.close();
            }
            if (mEncoderChannel != null) {
                mEncoderChannel.close();
            }
        }
    }

    public void updateDevices() {
        if (mWheel.getsetEmergencyStopSetVelocityToZero()) {
            setVelocity_AccelAtBLDC_MPC();
        }

        if (mDevicesDisengaed) return;

        if (mPhidBLDCMotorPositionController != null &&
                mPhidBLDCMotorPositionController.getEngaged()) {
            updateBLDcMotor();
        }

        if (mWheel.getWheelName().equals("FrontLeft") || mWheel.getWheelName().equals("FrontRight")) {
            //System.err.println("preparing to updateSteering "+bldc1+" "+mWheel.getWheelName());
            if (bldc1 != null) {
//                System.err.println("going to updateSteering now "+bldc1+" "+mWheel.getWheelName());        
                updateSteering();
            }
        }

        /**
         * The rear wheels don't have GhostAngles so this is set to zero.
         * setGhostAngle is fired to trigger the math to set speed ratios for rear wheels.
         * If additional steering modes are added this can probably be removed ? and other code can be used for all 4 wheels (assuming all 4 wheels have motors to control their steering).
         */

        if (mWheel.getWheelName().equals("RearLeft") || mWheel.getWheelName().equals("RearRight")) {
            mWheel.setGhostAngle(0);
        }

        if (mPhidEncoder != null && mEncoderChannel != null && mEncoderChannel.isOpen()) {
            updateEncoders();
        }
    }

    public void setBatchTime(String Batch_time_stamp_into_mysql) {
        mBatch_time_stamp_into_mysql = Batch_time_stamp_into_mysql;
    }

    private void updateEncoders() {
        if (mAllowMysqlLogging) {
            if (encoderCounter < 5) {
                MysqlLogger.put(MysqlLogger.Type.BETTER, 0000, "launchingEncoders", mBatch_time_stamp_into_mysql, "reset", "MySQL_Better6");
                encoderCounter = 5;
            }
        }

        try {
            encoderPosition = (int) mPhidEncoder.getPosition();
        } catch (PhidgetException ex) {
            System.err.println("encoder error (not attached?).");
        }
        encoderDevice = mWheel.getWheelName();
        try {
            mWheel.setEncoderPositionxyz(encoderPosition);

        } catch (InterruptedException ex) {
            Logger.getLogger(WheelDevice.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (mAllowMysqlLogging) {
            MysqlLogger.put(MysqlLogger.Type.BETTER, encoderPosition, "encoderPosition", mBatch_time_stamp_into_mysql, encoderDevice, "MySQL_Better7");
        }
    }


    /**
     * It appears that this 'updateBODcMotor()' code only reads values of dutycycle and postion; it doesn't write any commands to the BLDC Motors.
     * <p>
     * It updates mysqllogger and prints a warning to screen output if dutyCycle exceeds a threshold.
     */
    private void updateBLDcMotor() {
        if (BLDCPositionControllerCounter < 5) { // once more than 4 BLDC motors this may need to change - ie if I have 8 BLDC motors (2 per wheel x 4 wheels)
            MysqlLogger.put(MysqlLogger.Type.BETTER, 0000, "launchingBLDcMotor", mBatch_time_stamp_into_mysql, "reset", "MySQL_Better6");
            BLDCPositionControllerCounter = 5;
        }

        double dutyCycle = -99999999; // out of range default value to be used if real value can't be retreived
        double mPhidBLDCMotorPosition = -99999999; // out of range default value to be used if real value can't be retreived

        dutyCycle = mPhidBLDCMotorPositionController.getDutyCycle();
        mPhidBLDCMotorPosition = mPhidBLDCMotorPositionController.getPosition();

        if (dutyCycle > 0.4) {
            String timeS = new SimpleDateFormat("HH.mm.ss.SSS").format(new Date());
            System.err.println((String) (mWheel.getWheelName() + "; power (duty) over 0.4; reading = " + dutyCycle + " ___ " + timeS + "--------------------------------------------------"));
        }

        if (dutyCycle > 0.75) {
            String timeS = new SimpleDateFormat("HH.mm.ss.SSS").format(new Date());
            System.err.println((String) (mWheel.getWheelName() + "; WARNING!! Power (duty) over 0.75; reading = " + dutyCycle + " ___ " + timeS + "--------------------------------------------------"));
        }

        if (mAllowMysqlLoggingALTERNATE) {
            MysqlLogger.put(MysqlLogger.Type.BETTER, (float) mPhidBLDCMotorPosition, "wheel_Position", mBatch_time_stamp_into_mysql, mWheel.getWheelName(), "MySQL_Better8a");

            MysqlLogger.put(MysqlLogger.Type.BETTER, (float) mTargetPositionRoverBody, "mTargetPosition", mBatch_time_stamp_into_mysql, mWheel.getWheelName(), "MySQL_Better8b");

            MysqlLogger.put(MysqlLogger.Type.BETTER, (float) dutyCycle, "dutyCycleWhenSettingTargetPos", mBatch_time_stamp_into_mysql, mWheel.getWheelName(), "MySQL_Better8c");
        }
    }

    public void createBLDC_forSteering() {
        try {
            //System.out.println("----------------------------------------------------- "+mWheel.getWheelName());
            bldc1 = new MotorPositionController();
            if (mWheel.getWheelName().equals("FrontLeft")) {
                bldc1.setDeviceSerialNumber(527307); // this is for the BLDC motor; not the potentiameter that measures steering angle
                bldc1.setHubPort(1); // front left - increments have to be switched too
            }
            if (mWheel.getWheelName().equals("FrontRight")) {
                bldc1.setDeviceSerialNumber(527307); // this is for the BLDC motor; not the potentiameter that measures steering angle
                bldc1.setHubPort(4); //  increments have to be switched too
            }
            bldc1.setChannel(0);
            bldc1.setIsLocal(true);
            bldc1.setIsHubPortDevice(false);
            bldc1.setIsRemote(false);


        } catch (PhidgetException ex) {
            System.out.println("PhidgetException (need to set phidget DeviceSerialNumber, Hubport here: " + ex.getErrorCode() + " (" + ex.getDescription() + "): " + ex.getDescription());
        }

        WheelDeviceSubclassCreate();
    }

    public void openBLDCConnection_forSteering() {
        try {
            ConnectionAttemptCount = ConnectionAttemptCount + 1;

            bldc1.open(2000);
        } catch (PhidgetException ex) {
            System.err.println("bldc1.open(2000) steering setup failed for: " + mWheel.getWheelName() + ". Be sure the hub and device info is set properly above in createBLDC_forSteering");
            // Logger.getLogger(WheelDevice.class.getName()).log(Level.SEVERE, null, ex);
        }
        //try {
        //System.err.println(mWheel.getWheelName()+" "+bldc1.getDeviceName()+" OpenBLDCConnection_forSteering. Attempt # "+ConnectionAttemptCount);
        //} catch (PhidgetException ex) {
        // Logger.getLogger(WheelDevice.class.getName()).log(Level.SEVERE, null, ex);
        //}
        try {
            sleep(5100);
        } catch (InterruptedException ex) {
            // Logger.getLogger(WheelDevice.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            bldc1.setEngaged(true);
        } catch (PhidgetException ex) {
            // Logger.getLogger(WheelDevice.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            sleep(500);
        } catch (InterruptedException ex) {
            // Logger.getLogger(WheelDevice.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            System.err.println("Update the motor ratios here depending on which motor gear ratios are used.");
            double Acceleration = 1600; // for FrontLeft with a 15.3:1 ratio gearbox
            double Velocity = 600; // for FrontLeft with a 15.3:1 ratio gearbox
            if (mWheel.getWheelName().equals("FrontLeft")) {
                SteeringBLDC_Multiplier = 1;
                bldc1.setAcceleration(Acceleration);
                bldc1.setVelocityLimit(Velocity);
            }
            if (mWheel.getWheelName().equals("FrontRight")) {
                SteeringBLDC_Multiplier = 22.66 / 15.3;
                bldc1.setAcceleration(Acceleration * SteeringBLDC_Multiplier);// 22 2/3 gearbox vs 15.3 gearbox
                bldc1.setVelocityLimit(Velocity * SteeringBLDC_Multiplier);
            }
        } catch (PhidgetException ex) {
            System.err.println("openBLDCConnection_forSteering error: " + mWheel.getWheelName() + " - " + ex.getDescription());
        }
        addBLDCSteeringDutyCycleListener();
    }

    private void addBLDCSteeringDutyCycleListener() {
        while (bldc1 == null) {
            System.out.println("----- waiting to connect addBLDCSteeringDutyCycleListener on " + mWheel.getWheelName());
            try {
                sleep(3000);
            } catch (InterruptedException ex) {
                Logger.getLogger(WheelDevice.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        bldc1.addDutyCycleUpdateListener(new MotorPositionControllerDutyCycleUpdateListener() {
            @Override
            public void onDutyCycleUpdate(MotorPositionControllerDutyCycleUpdateEvent mpcdc) {
                double mReadSteeringDutyCycle = 0;
                try {
                    mReadSteeringDutyCycle = bldc1.getDutyCycle() * 1000;
                } catch (PhidgetException ex) {
                    Logger.getLogger(WheelDevice.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (Math.abs(mReadSteeringDutyCycle) > MaxdutyCycleReading) {
                    MaxdutyCycleReading = Math.abs(mReadSteeringDutyCycle);
//                    System.err.println( "duty cycle of steering bldc1 has changed "+mReadSteeringDutyCycle+
//                                        " MaxdutyCycleReading: "+MaxdutyCycleReading+" "+mWheel.getWheelName());
                }
                mWheel.setMaxdutyCycleReading(MaxdutyCycleReading);
                MaxdutyCycleReading = MaxdutyCycleReading - 3; // slowly decrease the max reading so it gets back to green if no further spikes occur - othwise it stays pegged at the peak.
                if (MaxdutyCycleReading < 10) { // don't let it go below zero.
                    MaxdutyCycleReading = 10;
                }
            }
        });
    }

    private void checkIfSteeringBLDCAttached() {
        try {
            if (!bldc1.getAttached()) {
                openBLDCConnection_forSteering();
                try {
                    sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(WheelDevice.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (PhidgetException ex) {
            Logger.getLogger(WheelDevice.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void WheelDeviceSubclassCreate() {
        mWheelDeviceSubClassAutoTrim = new WheelDeviceSubClassAutoTrim();
        mWheelDeviceSubClassAutoTrim.setValues(bldc1, mWheel.getWheelName());
    }

//    public void pushPotValuesDown(double potValue){
//        mPotValue = potValue;
//        //mWheelDeviceSubClassAutoTrim.setPotValue(mPotValue);
//    }

    /**
     * send a pot object down to WheelDevice and then to the mWheelDeviceSubClassAutoTrim so it can take readings at that level.
     *
     * @param pot
     */
    public void pushPotDown(Potentiameters pot) {
        mPotentiameters = pot;
        mWheelDeviceSubClassAutoTrim.pushPotDown(mPotentiameters);
    }

    /**
     * @param PotTargetValue
     */
    void updateSteeringCalibrateTrimAutomatically(double PotTargetValue) {
        mPotTargetValue = PotTargetValue;
        pauseThis = true;
        System.err.println("this currently is commented out because it doesnt work right");
        trimSettingFromAutoTrim = mWheelDeviceSubClassAutoTrim.runWheelDeviceSubClassAutoTrim(mPotTargetValue);
        System.out.println("");
        System.out.println("back from calculating trim value for " + mWheel.getWheelName());
        System.out.println("trim value " + trimSettingFromAutoTrim);
        pauseThis = false;
        // updateSteering();
    }


    private void updateSteering() { // FKA = 'Formerlly Known As'; this is the old name of this method.
        /*  Note Dec 26, 2019 - removed s_tepper motors and started using BLDC Motor Position Controllers instead. 
            Code prior to Dec 26 2019 worked with s_teppers.
            S_tepper motors were not strong enough; BLDC motors are much stronger.
        */

        //System.err.println("--updateSteering-----------------------------------------updateSteering---");
        checkIfSteeringBLDCAttached();

        if (mWheel.getWheelName().equals("FrontLeft")) {

        }
        try {// add some calibration here to determine center point of potentiameter value when starting up the app????

            // check to see that steering is within acceptable range before doing any more steering
            // if(testPotValue<0.65 && testPotValue>0.35){
            double angleFraction = mWheel.getAngleFraction();
            SteeringBLDCTargetPosition = -convertToSteeringPos(
                    angleFraction * mStepperMultiplier) * SteeringBLDC_Multiplier;
//            if (mWheel.getWheelName().equals("FrontLeft") && !pauseThis) {
//            System.err.println(" TargetPos "+String.format("%02.1f",SteeringBLDCTargetPosition)+
//                               " "+mWheel.getWheelName()+ 
//                               " previous TargetPos " + bldc1.getTargetPosition() +
//                               " MaxdutyCycle "+MaxdutyCycleReading+
//                               " angleFraction "+String.format("%02.5f",angleFraction)+
//                               " pauseThis "+pauseThis);
//            }

            if (!pauseThis) {
                bldc1.setTargetPosition(SteeringBLDCTargetPosition);
            }

            SteeringBLDCTargetPositionConvertedToDegrees = convertFromSteeringBLDCPos(SteeringBLDCTargetPosition);

            try {
                posActual_Steering = bldc1.getPosition() / SteeringBLDC_Multiplier;
            } catch (PhidgetException ex) {
                System.err.println("error with updateSteering() in WheelDevice.java getting position isn't working. " + ex.getDescription() + " " + mWheel.getWheelName());
            }

            posActual_SteeringConvertedToDegrees = convertFromSteeringBLDCPos(posActual_Steering * mStepperMultiplier);
            mWheel.setGhostAngle(convertFromSteeringBLDCPos(posActual_Steering * mStepperMultiplier));
            if (mAllowMysqlLogging) {
                MysqlLogger.put(MysqlLogger.Type.BETTER, (float) posActual_Steering, "SteeringPosition", mBatch_time_stamp_into_mysql, mWheel.getWheelName() + "", "MySQL_Better10");
            }
        } catch (PhidgetException ex) {
            System.err.println("error with updateSteering() in WheelDevice.java. " + ex.getDescription() + " " + mWheel.getWheelName());
        }
    }

    public double convertToSteeringPos(double AngleFraction) {
        double TargetPOS = Math.min(
                Math.max(
                        -mSteeringPositionAbsWriteSpan,
                        -mSteeringPositionAbsReadSpan * AngleFraction + (trimSettingFromAutoTrim * mStepperMultiplier)
                ),

                mSteeringPositionAbsWriteSpan
        );

//        System.err.println(mWheel.getWheelName()+ " SteeringBLDCTargetPosition "+SteeringBLDCTargetPosition + " TargetPOS "+ TargetPOS+
//                " mSteeringPositionAbsReadSpan "+-mSteeringPositionAbsReadSpan+
//                " mSteeringPositionAbsReadSpan*AngleFraction " + -mSteeringPositionAbsReadSpan*AngleFraction+
//                " mSteeringPositionAbsWriteSpan "+ -mSteeringPositionAbsWriteSpan+
//                " trimSettingFromAutoTrim " + trimSettingFromAutoTrim);

        return TargetPOS;
    }

    public double convertFromSteeringBLDCPos(double pos) {
        // System.out.println("mSteeringPositionAbsReadSpan "+mSteeringPositionAbsReadSpan);
        return 180 * ((pos / mSteeringPositionAbsReadSpan) % 1.0);
    }
}
