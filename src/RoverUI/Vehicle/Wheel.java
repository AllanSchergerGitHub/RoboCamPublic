package RoverUI.Vehicle;

import Chart.ChartParamType;
import Chart.ChartParamsDataset;
import PhiDevice.MotorPositionControllerList;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public final class Wheel {
    public static final String WHEEL_FRONT_LEFT = "FrontLeft";
    public static final String WHEEL_FRONT_RIGHT = "FrontRight";
    public static final String WHEEL_REAR_LEFT = "RearLeft";
    public static final String WHEEL_REAR_RIGHT = "RearRight";

    private static final Stroke DASHED_STROKE = new BasicStroke(
            2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{2}, 0);

    private final String mName;

    private double mDrawWidth = 10;
    private double mWheelDrawLength = 50;
    private double mTruckDrawLength = 100;
    private double mDrawScale = 1;

    private double distanceRemainingRover = 0;

    private boolean EmergencyStop = false;

    public static final Color TRANS_WHITE = new Color(1f, 1f, 1f, 0.98f);
    private Color textColor = Color.BLACK;
    private Color textBackground = TRANS_WHITE;
    private int fontSetting = Font.PLAIN;
    private int fontSize = 16;
    private int fontSizeDefault = fontSize;

    public double MaxdutyCycleReading = 0;
    private double mHypotenuse = 1;
    private double mDeltaY = 1;
    private double mAngleRoverBodyTarget = 1;
    private double speedRatioThisWheelActual = 1;
    private double mReadAbsSpeed = -1;
    private double[] mBLDCmotorReadPosList = {0, 0};
    private double[] mBLDCmotorDutyCycleList = {0, 0};
    private MotorPositionControllerList mcontrolerList;
    private ArrayList<DeviceInfo> mDeviceInfoList = new ArrayList<>();
    private double mWheelSpeed = 1;
    private double mWheelVelocityLimit = 1;

    private double mWheelDiameter = 13;

    double modified_mSpeedRatio = 1;

    private Point.Double mDrawLocation = new Point.Double();
    private double mDrawAngle = 0;
    private double mGhostAngle = 0;
    static double[] mGhostAngleStatic = {0, 0}; // actual angle of left and right wheels (as seen from top)
    static double[] mBaseLengthStatic = {0, 0}; // length of horizontal base of a triangle extending out from the left and right wheels

    private final Rectangle.Double mOuterRectangle = new Rectangle.Double();
    private Color mFillColor = Color.GREEN;
    private final Stroke mDrawStroke = new BasicStroke(2);

    private String disengageWarning = "engaged";

    private final int[] mPolyPointXs = new int[5];
    private final int[] mPolyPointYs = new int[5];

    private boolean reengageNowPhidBLDCMotorPositionController = false;

    private int mEncoderPositionxyz = 0;

    private ChartParamsDataset mChartParamsDataset;

    public Wheel(String name) {
        mName = name;
        calculateDimensions();
    }

    public Wheel(String name, Color fillColor) {
        mName = name;
        mFillColor = fillColor;
        calculateDimensions();
    }

    public void setDrawLocation(double centerX, double centerY) {
        mDrawLocation.x = centerX;
        mDrawLocation.y = centerY;
    }

    public Point.Double getDrawLocation() {
        return mDrawLocation;
    }

    public double getAngleFraction() {
        double frac;
        if (mDrawAngle < 180) {
            frac = mDrawAngle / 180;
        } else {
            frac = -(mDrawAngle - 180) / 180;
        }
        //System.err.println("frac before"+frac);
        frac %= 1.;
        //System.err.println("frac after "+frac);
        return frac;
    }

    public void setHypotenuseTo(Point.Double toPoint) {
        mHypotenuse = mDrawLocation.distance(toPoint);
    }

    public void setHypotenuse(double TruckDrawLength, double hypotenuse, double deltaY, double deltaX) {
        mTruckDrawLength = TruckDrawLength;
        mHypotenuse = hypotenuse;
        mDeltaY = deltaY;
    }

    public double getHypotenuse() {
        return mHypotenuse;
    }

    /**
     * @param setSpeeed in Wheel.java
     *                  String source is used for debugging - it tells me where the setSpeeed action was initiated from
     */
    public void setSpeeed(String source, double angleRoverBodyTarget) {
        mAngleRoverBodyTarget = angleRoverBodyTarget;
    }

    /**
     * SpeeedRatioRear needs to be set at Truck level since rear wheels don't have stepper motors themselves.
     * The rear wheels rely on the front wheel stepper motor positions for their speed ratios.
     */
    public void setSpeeedRatioRear(double speedRatio) {
        speedRatioThisWheelActual = speedRatio;
    }

    public void setEmergencyStopSetVelocityToTrue() { // this just forces the wheel device to set the velocity (presumably to zero) - it isn't a true emergency stop
        EmergencyStop = true;
        mWheelVelocityLimit = 0;
    }

    public void setEmergencyStopSetVelocityToFalse() {
        EmergencyStop = false;
    }

    public boolean getsetEmergencyStopSetVelocityToZero() { // this just forces the wheel device to set the velocity (presumably to zero) - it isn't a true emergency stop
        return EmergencyStop;
    }

    public double getVelocityLimitSetting() {
        return mWheelVelocityLimit;
    }

    public void setMaxdutyCycleReading(double MaxdutyCycleReadingFromDevice) {
        MaxdutyCycleReading = MaxdutyCycleReadingFromDevice;
    }

    ;

    public double getSpeedRatio(double mDistanceRemainingRover) {
        double wheelVelocityFactor = 1.04; // for wheels start with front wheels going faster than rear and assuming moving forward (this is updated below)
        if (mName.equals("FrontLeft") || mName.equals("FrontRight")) {

            // do this in increments so there isn't a sudden jolt when going from 1.04 to .98 or vice versa. May need to tweak these.
            if (mDistanceRemainingRover < 80) { // if going backwards then the front wheels should turn slower than rear wheels
                wheelVelocityFactor = 1.02; // probably less of a discount here than going the other way due to steering needs - eventually need to test for the right ratios.
            }
            if (mDistanceRemainingRover < 40) { // if going backwards then the front wheels should turn slower than rear wheels
                wheelVelocityFactor = 1.0; // probably less of a discount here than going the other way due to steering needs - eventually need to test for the right ratios.
            }
            if (mDistanceRemainingRover < -40) { // if going backwards then the front wheels should turn slower than rear wheels
                wheelVelocityFactor = 0.98; // probably less of a discount here than going the other way due to steering needs - eventually need to test for the right ratios.
            }
        }
        if (mName.equals("RearLeft") || mName.equals("RearRight")) {
            wheelVelocityFactor = 1.0; // rear wheels should always maintain factor of 1.0
        }
        //System.out.println("DistanceRemainingRover " + mDistanceRemainingRover);
        //System.out.println(mName + " speedRatioThisWheelActual " + speedRatioThisWheelActual);
        modified_mSpeedRatio = speedRatioThisWheelActual * wheelVelocityFactor;
        return modified_mSpeedRatio;
    }

    public void setBLCDCDutyCyleAtIndex(int index, double value) {
        mBLDCmotorDutyCycleList[index] = value;
    }

    public double getReadAbsSpeed() {
        return mReadAbsSpeed;
    }

    public double getReadDutyCycle(int index) {
        return mBLDCmotorDutyCycleList[index];
    }

    public double[] getBLDCmotorReadPosAllDevices() {
        return mBLDCmotorReadPosList;
    }

    public double getBLDCmotorReadPos1Device(int index) {
        return mBLDCmotorReadPosList[index];
    }

    public void setdistanceRemainingRover(double value) {
        distanceRemainingRover = value;
    }

    public double getdistanceRemainingRover() {
        return distanceRemainingRover;
    }

    public void setBLDCmotorReadPos(int index, double value, String source) {
        //System.err.println("......................................................."+source + " value " + value);
        mBLDCmotorReadPosList[index] = value;
    }

    public void setMotorPositionControllerList(MotorPositionControllerList controlerList) {
        mcontrolerList = controlerList;
    }

    public interface DeviceInfoListChangeListener {
        public void onChange();

    }

    ArrayList<DeviceInfoListChangeListener> mDeviceInfoListChangeListeners = new ArrayList<>();

    public void addDeviceInfoListChangeListener(DeviceInfoListChangeListener listener) {
        if (!mDeviceInfoListChangeListeners.contains(listener)) {
            mDeviceInfoListChangeListeners.add(listener);
        }
    }

    public void setDeviceInfoList(List<DeviceInfo> deviceInfoList) {
        mDeviceInfoList.clear();
        mDeviceInfoList.addAll(deviceInfoList);
        for (DeviceInfoListChangeListener listener : mDeviceInfoListChangeListeners) {
            listener.onChange();
        }
    }

    public List<DeviceInfo> getDeviceInfoList() {
        return mDeviceInfoList;
    }

    public String getDeviceInfoListString() {
        StringBuilder stringBuilder = new StringBuilder();
        int index = 0;
        for (DeviceInfo deviceInfo : mDeviceInfoList) {
            if (index > 0) {
                stringBuilder.append("\n");
            }
            stringBuilder.append(String.format("Device: %s\n", deviceInfo.getName()));
            stringBuilder.append(deviceInfo.getParamsInfoString());
            index++;
        }
        return stringBuilder.toString();
    }

    public void setReadAbsSpeed(double value) {
        mReadAbsSpeed = value;
    }

    public void setDisnegageWarning(String warning) {
        disengageWarning = warning;
    }

    public int getEncoderPositionxyz() throws InterruptedException {
        return mEncoderPositionxyz;
    }

    public void setEncoderPositionxyz(int value2) throws InterruptedException {
        mEncoderPositionxyz = value2;
    }

    public void setWheelVelocityLimit(double WheelVelocityLimit) {
        mWheelVelocityLimit = WheelVelocityLimit;
    }

    public void setWheelSpeed(double speed) {
        mWheelSpeed = speed;
    }

    public double getAbsSpeed() {
        return mWheelSpeed * speedRatioThisWheelActual;
    }

    /**
     * use this one - it updates immediately upon mouse click to rotate
     *
     * @param angle
     */
    public void setDrawAngle(double angle) {
        mDrawAngle = angle;
    }

    public void setReengageNowPhidBLDCMotorPositionController() {
        reengageNowPhidBLDCMotorPositionController = true;
        //System.err.println(" reengaging at Wheel Level? "+mName+" "+reengageNowPhidBLDCMotorPositionController);
    }

    public boolean getReengageNowPhidBLDCMotorPositionController() {
        //System.err.println(" get - reengaging at Wheel Level? "+mName+" "+reengageNowPhidBLDCMotorPositionController);
        return reengageNowPhidBLDCMotorPositionController;
    }

    /**
     * not used (technically it is used but angle increment is zero so it has no affect.)
     *
     * @param angle
     */
    public void increaseDrawAngle(double angle) {
        mDrawAngle += angle;
    }

    public double getGhostAngle() {
        return mGhostAngle;
    }

    public static double[] getGhostAngleStatic() {
        return mGhostAngleStatic;
    }

    public static double[] getBaseLengthStatic() {
        return mBaseLengthStatic;
    }

    public void setGhostAngle(double angle) {
        mGhostAngle = angle;
        if (mName == "FrontLeft") {
            mGhostAngleStatic[0] = mGhostAngle;
        }
        if (mName == "FrontRight") {
            mGhostAngleStatic[1] = mGhostAngle;
        }

        //double mBaseLengthAtWheel = java.lang.Math.abs(-mDeltaY/Math.tan(Math.toRadians(mGhostAngle))); // mDeltaY shows the length this wheel is from bottom(rear) of rover
        double mBaseLengthAtWheel = -mDeltaY / Math.tan(Math.toRadians(mGhostAngle)); // mDeltaY shows the length this wheel is from bottom(rear) of rover

        double baseLengthThreshold = 15000;
        if (java.lang.Math.abs(mGhostAngle) < 1 && java.lang.Math.abs(mBaseLengthAtWheel) > baseLengthThreshold) {
            mBaseLengthAtWheel = baseLengthThreshold;
        } // keep this reasonably around 15,000 or the speedratio becomes extremely large/small when close to straight ahead.

        if (mName == "FrontLeft") {
            mBaseLengthStatic[0] = mBaseLengthAtWheel;
        }
        ; // mBaseLengthStatic is length of a horizontal line extending 0 degrees straight out from center of each wheel (parallel to horizon)
        if (mName == "FrontRight") {
            mBaseLengthStatic[1] = mBaseLengthAtWheel;
        } // mBaseLengthStatic is length of a horizontal line extending 0 degrees straight out from center of each wheel (parallel to horizon)

        double multiplier = 1;
        //if(mGhostAngle>=0){multiplier=1;}else{multiplier=-1;} // when turning to the left; the measurement point flips to the other side of the rover.

        if (mName == "RearLeft") {
            mBaseLengthAtWheel = (mBaseLengthStatic[0] + mBaseLengthStatic[1]) / 2 + (50 * multiplier);
        }
        if (mName == "RearRight") {
            mBaseLengthAtWheel = (mBaseLengthStatic[0] + mBaseLengthStatic[1]) / 2 - (50 * multiplier);
        }
        if (java.lang.Math.abs(mGhostAngle) < 1 && java.lang.Math.abs(mBaseLengthAtWheel) > baseLengthThreshold - 100) {
            mBaseLengthAtWheel = baseLengthThreshold;
        } // keep this reasonably around 15,000 or the speedratio becomes extremely large/small when close to straight ahead.


        double mBaseLengthMidRover = 0;
        if (mBaseLengthAtWheel < (baseLengthThreshold - 100)) {
            if (mName == "FrontLeft" || mName == "RearLeft") {
                mBaseLengthMidRover = mBaseLengthAtWheel - (50 * multiplier);
            }
            if (mName == "FrontRight" || mName == "RearRight") {
                mBaseLengthMidRover = mBaseLengthAtWheel + (50 * multiplier);
            }
        } else {
            mBaseLengthMidRover = mBaseLengthAtWheel;
        }


        //double mBaseLengthMidRover = (mBaseLengthStatic[0]+mBaseLengthStatic[1])/2;
        double roverAngleActualCalcd = 0;

        double yPositionOfThisWheel = 0; // for rear wheels this is zero.
        if (mName == "FrontLeft" || mName == "FrontRight") {
            yPositionOfThisWheel = mTruckDrawLength;
        }

        double hypotThisWheelActual = Math.pow(Math.pow(mBaseLengthAtWheel, 2) + Math.pow(-yPositionOfThisWheel, 2), .5);
        double hypotRoverActual = Math.pow((Math.pow(mBaseLengthMidRover, 2) + Math.pow(-mTruckDrawLength / 2, 2)), .5); // divide deltaY by 2 since rover midpoint is 1/2 way up the body.

//        System.err.println(mName
//                        +" hypotRoverActual "+String.format("%.4f", hypotRoverActual)
//                        +" Math.pow(mBaseLengthMidRover,2) "+String.format("%.3f", Math.pow(mBaseLengthMidRover,2))
//                        +" mBaseLengthMidRover "+String.format("%.4f", mBaseLengthMidRover)
//                        +" Math.pow(-mTruckDrawLength/2,2) " +String.format("%.4f", Math.pow(-mTruckDrawLength/2,2))
//                        +" mTruckDrawLength "+mTruckDrawLength);

        double slope = 1;
        if (mBaseLengthMidRover == 0) {
            slope = 1;
            roverAngleActualCalcd = 0;
        } else {
            slope = (-mDeltaY / 2) / (mBaseLengthMidRover); // the angle is ~1/2 what i expected since it is 1/2 way down on the rover... but that is the correct measurement.
            roverAngleActualCalcd = Math.toDegrees(Math.atan(slope));
        }

        double CircThisWheelActual = hypotThisWheelActual * Math.PI * 2;
        double CircRoverActual = hypotRoverActual * Math.PI * 2;

        double speedThisWheelActual = CircThisWheelActual / 13;
        double speedRoverActual = CircRoverActual / 13;
        speedRatioThisWheelActual = speedThisWheelActual / speedRoverActual;
        //System.err.println(mName+" speedRatioThisWheelActual(pre truncate) "+speedRatioThisWheelActual+"; mAngleRoverBodyTarget: "+String.format("%.4f", mAngleRoverBodyTarget));

        if (mName == "FrontLeft" || mName == "FrontRight") {
            if (java.lang.Math.abs(mGhostAngle) < 1.2) {
                speedRatioThisWheelActual = 1;
                //System.err.println(mName+" narrowing band of speedRatioThiswheelActual-------------------------------------- ");
            } // stablizes the speedRatio to avoid large swings when near straight ahead (hypotenuse changes become large which causes volitility)
        }
//        if (mName=="FrontLeft" || mName=="FrontRight" ){
//                    speedRatioThisWheelActual=speedRatioThisWheelActual*1.1;
//        }

        // mWheelDrawLength // from bottom of rover to top of rover (also this is one side of a right triangle)

        //mSpeed = Math.tan(Math.toRadians(90-angle));

        //what is rover speed based on? probably on target rotation and not actual rotation
        double spedRoverBodyTarget = Math.pow(Math.pow((-mDeltaY) / Math.tan(Math.toRadians(mAngleRoverBodyTarget)), 2) + Math.pow(-mDeltaY / 2, 2), .5) * Math.PI * 2 / 13; // divide deltaY by 2 since rover midpoint is 1/2 way up the body.
        //System.err.println(mName+"; formula: "+String.format("%.4f", ((-mDeltaY/2)/Math.tan(Math.toRadians(mAngleRoverBodyTarget))))+" mDeltaX "+String.format("%.2f", -mDeltaX)+" mDeltaY:"+String.format("%.2f", mDeltaY));


//double spedRatiTarget =      Math.pow(Math.pow(-mDeltaY/Math.tan(Math.toRadians(mDrawAngle)),2)+Math.pow(-mDeltaY,2),.5)*Math.PI*2/13/spedRoverBodyTarget; // this is a proxy for the target wheel speed
        
         /*
        //if(speedRatioThisWheelActual>.2){

        System.err.println(mName+" angl "+String.format("%.4f", mGhostAngle)
                                +" rverAnglAcClcd "+String.format("%.4f", roverAngleActualCalcd)
                                +" spedRatoWhelAct "+String.format("%.4f", speedRatioThisWheelActual)
                                +" bseLn "+String.format("%.2f", mBaseLengthAtWheel)
                                +" bseLnRvrAvg "+String.format("%.2f", mBaseLengthMidRover)
                                +" mAngleRvrBodyTar "+String.format("%.4f", mAngleRoverBodyTarget)
        // /*
                                
                                +" spedWhlAct "+String.format("%.2f", speedThisWheelActual)
                                +" spedRverActual "+String.format("%.4f", speedRoverActual)
                // target
                                +" spedRvrBdy "+String.format("%.4f", spedRoverBodyTarget) // calc'd in Wheel.java
                
                // actual
                                

                                +" hyptWhelAct "+String.format("%.2f", hypotThisWheelActual)                
                                +" hyptRvrAct "+String.format("%.4f", hypotRoverActual)

                                +" CircWhlAct "+String.format("%.2f", CircThisWheelActual)
                                +" CircRvrAct "+String.format("%.2f", CircRoverActual)
                                +"; mDrawScale: "+mDrawScale
                                +" mDeltaY "+String.format("%.1f", mDeltaY)
                                +" mWhelSped "+mWheelSpeed+")"                

                                );
         */

    }

    public String getWheelName() {
        return mName;
    }

    /**
     * not used. try to use 'public void rotateTo(Point.Double point, double offset)' instead
     *
     * @param point
     */
    public void rotateTo(Point.Double point) {
        mDrawAngle = Geom.getSlope(mDrawLocation, point);
    }

    /**
     * only used with steer method 'turnaround' (and this could probably be simplified with some work).
     *
     * @param point
     * @param offset
     */
    public void rotateTo(Point.Double point, double offset) {
        mDrawAngle = offset + Geom.getSlope(mDrawLocation, point);
    }

    public void setDrawScale(double drawScale) {
        mDrawScale = drawScale;
        calculateDimensions();
    }

    /**
     * It returns the Y value of outer rectangle
     * including the Y value of the center drawing point
     * of the wheel. That means this value is relative to
     * the drawing location of the Truck
     *
     * @return double
     */
    public Double getOuterTopY() {
        return mDrawLocation.y + mOuterRectangle.y;
    }

    /**
     * It returns the absolute height of the outer
     * rectangle of the wheel.
     *
     * @return double
     */
    public Double getOuterHeight() {
        return mOuterRectangle.getHeight();
    }

    public void calculateDimensions() {
        mOuterRectangle.setRect(
                -mDrawWidth / 2 * mDrawScale, -mWheelDrawLength / 2 * mDrawScale, mDrawWidth * mDrawScale, mWheelDrawLength * mDrawScale);

        mPolyPointXs[0] = (int) mOuterRectangle.x;
        mPolyPointXs[1] = (int) (mOuterRectangle.x + mOuterRectangle.width * 0.5);
        mPolyPointXs[2] = (int) (mOuterRectangle.x + mOuterRectangle.width);
        mPolyPointXs[3] = mPolyPointXs[2];
        mPolyPointXs[4] = mPolyPointXs[0];

        int arrowThickness = (int) (mOuterRectangle.height * 0.25);

        mPolyPointYs[0] = (int) mOuterRectangle.y + arrowThickness;
        mPolyPointYs[1] = (int) mOuterRectangle.y;
        mPolyPointYs[2] = mPolyPointYs[0];
        mPolyPointYs[3] = (int) (mOuterRectangle.y + mOuterRectangle.height);
        mPolyPointYs[4] = mPolyPointYs[3];
    }

    public ChartParamsDataset getChartParamsDataset() {
        if (mChartParamsDataset != null) return mChartParamsDataset;
        mChartParamsDataset = new ChartParamsDataset(
                getWheelName() + " Chart",
                new ChartParamType[]{
                        ChartParamType.VELOCITY,
                        ChartParamType.ANGLE,
                        //ChartParamType.BLDC_1_POSITION,
                        //ChartParamType.BLDC_2_POSITION,
                        ChartParamType.BLDC_1_POS_DUTY_CYCLE,
                        ChartParamType.BLDC_2_POS_DUTY_CYCLE,
                        //ChartParamType.BLDC_POS_DUTY_CYCLE
                }
        );
        return mChartParamsDataset;
    }

    public void updateChartParamsDataset() { // commenting this out May 17 2019 - not used?
        if (mChartParamsDataset == null) return;
        mChartParamsDataset.addValue(ChartParamType.ANGLE, mDrawAngle);
        //mChartParamsDataset.addValue(ChartParamType.VELOCITY, mWheelSpeed);
        mChartParamsDataset.addValue(ChartParamType.BLDC_1_POSITION, getBLDCmotorReadPos1Device(0));
        mChartParamsDataset.addValue(ChartParamType.BLDC_2_POSITION, getBLDCmotorReadPos1Device(1));

        mChartParamsDataset.addValue(ChartParamType.BLDC_1_POS_DUTY_CYCLE, getReadDutyCycle(0));
        mChartParamsDataset.addValue(ChartParamType.BLDC_2_POS_DUTY_CYCLE, getReadDutyCycle(1));
        //mChartParamsDataset.addValue(ChartParamType.BLDC_POS_DUTY_CYCLE, getReadDutyCycle());
    }


    static final int DEFAULT_LINE_SPACING = 2;

    public void draw(Graphics2D g2d) {
        AffineTransform saveAT;

        saveAT = g2d.getTransform();
        g2d.translate(mDrawLocation.x, mDrawLocation.y);
        g2d.rotate(Math.toRadians(mGhostAngle));

        g2d.setColor(Color.BLACK);
        g2d.setStroke(DASHED_STROKE);
        g2d.drawPolygon(mPolyPointXs, mPolyPointYs, mPolyPointXs.length);

        g2d.setTransform(saveAT);

        g2d.setStroke(mDrawStroke);
        g2d.setBackground(Color.BLACK);

        saveAT = g2d.getTransform();
        g2d.translate(mDrawLocation.x, mDrawLocation.y);
        g2d.rotate(Math.toRadians(mDrawAngle));

//        if(mName.equals("FrontRight")){
//            //System.err.println(mName+" mDrawAngle: "+mDrawAngle+" mGhostAngle "+mGhostAngle+ " mDrawAngle updates but mGhostAngle is one click behind");
//        }

        g2d.setColor(mFillColor);
        g2d.fillPolygon(mPolyPointXs, mPolyPointYs, mPolyPointXs.length);

        g2d.setColor(Color.BLACK);
        g2d.drawPolygon(mPolyPointXs, mPolyPointYs, mPolyPointXs.length);

        g2d.setTransform(saveAT);

        g2d.setColor(Color.black);

        Point2D.Double cursor;
//        cursor = Utility.Drawing.drawString(
//                    g2d,
//                    "S/R = "+ String.format("%.2f", getSpeedRatio()),
//                    mDrawLocation.x, mDrawLocation.y-20, true);

        double modifiedDrawLocationY = 0; // need a way to change the text drawing location without affecting the wheel drawing positions
        double modifiedDrawLocationX = 0; // need a way to change the text drawing location without affecting the wheel drawing positions
        if (mName == "RearRight" || mName == "FrontRight") // two || means or (this or that)
        {
            modifiedDrawLocationY = mDrawLocation.y + 1;
            modifiedDrawLocationX = mDrawLocation.x + 35;
        } else {
            modifiedDrawLocationY = mDrawLocation.y;
            modifiedDrawLocationX = mDrawLocation.x;
        }
        fontSetting = Font.BOLD;
        cursor = Utility.Drawing.drawString(Color.BLACK, TRANS_WHITE, fontSetting, fontSize,
                g2d,
                "DutyCycle: " + String.format("%.0f", getReadDutyCycle(0)),
                modifiedDrawLocationX, modifiedDrawLocationY + DEFAULT_LINE_SPACING, true);

//        cursor = Utility.Drawing.drawString(Color.BLACK, TRANS_WHITE, fontSetting, fontSize,
//                    g2d,
//                    "dutyCycle2: "+ String.format("%.1f", getReadDutyCycle(1)),
//                    mDrawLocation.x, cursor.y + DEFAULT_LINE_SPACING, true);        
//                 
//        cursor = Utility.Drawing.drawString(Color.BLACK, TRANS_WHITE, fontSetting, fontSize,
//                    g2d,
//                    "BLDC POS: "+ String.format("%.3f", getBLDCmotorReadPos1Device()),
//                    mDrawLocation.x, cursor.y + DEFAULT_LINE_SPACING, true);

//        cursor = Utility.Drawing.drawString(g2d,
//                    "Read Abs Speed = "+ String.format("%.2f", getReadAbsSpeed()),
//                    mDrawLocation.x, cursor.y + 10, true);
//        cursor = Utility.Drawing.drawString(
//                    g2d,
//                    "Radius = "+ String.format("%.1f", getHypotenuse()),
//                    mDrawLocation.x, cursor.y + 10, true);
        //Note(by Sujoy on March-21-2019): mcontrolerList accesses phdigets directly, while Wheel class 
        //shoubld be independent of phdiget.
        /*cursor = Utility.Drawing.drawString(Color.BLACK, TRANS_WHITE, fontSetting, fontSize,
                    g2d,
                    "Device: "+ mcontrolerList, // this is also relative distance
                    mDrawLocation.x+55, cursor.y+10, true);
        */
        /*cursor = Utility.Drawing.drawString(Color.BLACK, TRANS_WHITE, fontSetting, fontSize,
                    g2d,
                    getDeviceInfoListString(), // this is also relative distance
                    mDrawLocation.x+55, cursor.y+10, true);        */

        for (DeviceInfo deviceInfo : mDeviceInfoList) {
            if (!deviceInfo.getEngageStatus().equals(EngageStatus.ENGAGED) &&
                    !deviceInfo.getEngageStatus().equals(EngageStatus.NONE)) {
                String deviceEngageStatus = deviceInfo.getEngageStatus().getName() +
                        "(" + deviceInfo.getName() + ")";


                cursor = Utility.Drawing.drawString(Color.WHITE, Color.RED, Font.BOLD, 18,
                        g2d, deviceEngageStatus,
                        modifiedDrawLocationX, cursor.y + DEFAULT_LINE_SPACING, true);
            }
        }

//        cursor = Utility.Drawing.drawString(Color.BLACK, TRANS_WHITE, fontSetting, fontSize,
//                    g2d,
//                    "AbsSpeed: "+ mReadAbsSpeed, // this is also relative distance
//                    mDrawLocation.x, cursor.y+DEFAULT_LINE_SPACING, true);

        cursor = Utility.Drawing.drawString(Color.BLACK, TRANS_WHITE, fontSetting, fontSize,
                g2d,
                "BLDC POS0: " + String.format("%.0f", getBLDCmotorReadPos1Device(0)),
                modifiedDrawLocationX, cursor.y + DEFAULT_LINE_SPACING, true);

//        cursor = Utility.Drawing.drawString(Color.BLACK, TRANS_WHITE, fontSetting, fontSize,
//                    g2d,
//                    "BLDC POS1: "+ String.format("%.1f", getBLDCmotorReadPos1Device(1)),  
//                    mDrawLocation.x, cursor.y + DEFAULT_LINE_SPACING, true);   
//        
        fontSizeDefault = fontSize;
        if (disengageWarning.startsWith("DISENGAGED")) {
            textColor = Color.WHITE;
            textBackground = Color.RED;
            fontSize = 20;
            fontSetting = Font.BOLD;
        }

        cursor = Utility.Drawing.drawString(textColor, textBackground, fontSetting, fontSize,
                g2d,
                "" + disengageWarning,
                modifiedDrawLocationX, cursor.y + DEFAULT_LINE_SPACING, true);

        cursor = Utility.Drawing.drawString(Color.BLACK, TRANS_WHITE, fontSetting, 10,
                g2d,
                "Rover Dist Remaining",
                45, -8, true);
        cursor = Utility.Drawing.drawString(Color.BLACK, TRANS_WHITE, fontSetting, 20,
                g2d,
                String.format("  " + "%.0f", distanceRemainingRover) + "  ",
                20, -35, true);

        textColor = Color.BLACK; // reset these variables to their 'defaults'
        textBackground = TRANS_WHITE; // reset these variables to their 'defaults'
        fontSetting = Font.PLAIN; // reset these variables to their 'defaults'
        fontSize = fontSizeDefault;
    }
}
