package RoverUI.Vehicle;

import Chart.ChartParamsDataset;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;


public class Truck {
    private static final int POINTER_RADIUS = 10;
    private static final double STRAIGHT_ANGLE_TOLERANCE = 1;

    private final Wheel mWheelFrontLeft;
    private final Wheel mWheelFrontRight;
    private final Wheel mWheelRearLeft;
    private final Wheel mWheelRearRight;

    //private final Arm mArm;

    private int wheel_ID = 3;

    private final Wheel[] mWheels;
    private final SteeringArc mSteeringArc;

    Point.Double modifiedMousePos = new Point.Double(427.5, 180); //(434.5 when on UI?) - adjusting the x here can help if the wheels don't face straight ahead when the 'forwardsteer' button is pressed.
    private Boolean mTroubleshootingMessages = false; // set this to true for additional messages to help with modifiedMousePos problems.
    Point.Double modifiedRelMousePos = new Point.Double();

    private final int encoderPosition_display = 0;

    //Point.Double rotateWheelsTo_toPoint;// = (Point.Double)(100.0,100.0);
    private double mDrawWidth = 103.5;
    private double mTruckDrawLength = 100;
    private double mDrawScale = 2;
    private double mWheelDiameter = 13;
    private double mTruckSpeedBaseline = 1; // speed of truck based on hypotenuse - this defines the denominator of the speed ratio calculations
    private double mTruckSpeed = 0; // speed of truck that can be controlled by user
    private double mTruckHypotenuse = 1;
    private double frontRightHyp = 1;
    private double frontLeftHyp = 1;
    private double rearRightHyp = 1;
    private double rearLeftHyp = 1;
    private double frontRightDeltaY = 1; // deltaY can be used in math with angle to determine the radius of a circle
    private double frontLeftDeltaY = 1;
    private double rearRightDeltaY = 1;
    private double rearLeftDeltaY = 1;
    private double frontRightDeltaX = 1; // deltaY can be used in math with angle to determine the radius of a circle
    private double frontLeftDeltaX = 1;
    private double rearRightDeltaX = 1;
    private double rearLeftDeltaX = 1;

    private Point.Double mDrawLocation = new Point.Double(434, 180);
    private Point.Double rotateWheelsTo_toPoint;

    private double mDrawAngle = 0;

    private SteeringMode mSteeringMode = SteeringMode.NONE;
    private double mForwardAngle = 0.0;// Negative value means backward
    private double mForwardAngleRaw = 0.0;// Negative value means backward

    private final Color mTruckColor = Color.BLUE;


    private final Rectangle.Double mTruckRectangle = new Rectangle.Double();
    Rectangle.Double mMouseBoundingBox = new Rectangle.Double();

    private final Stroke mTruckStroke = new BasicStroke(2);

    int[] startX = new int[8];
    int[] startY = new int[8];
    int[] endX = new int[8];
    int[] endY = new int[8];
    int deviceNumber = 0;
    double deltaX = 0;
    double deltaY = 0;

    double[] speedRatioTarget = new double[8];
    double[] anglesActual = {0, 0};
    double[] BaseLengthActual = {0, 0};

    double slopeRoverBodyTarget = 0;
    double slopeFrontOuter = 0;
    double slopeFrontInner = 0;
    double slopeRearOuter = 0;
    double slopeRearInner = 0;
    double angleRoverBodyTarget = 0;
    double angleFrontOuter = 1;
    double angleFrontInner = 1;
    double angleRearOuter = 1;
    double angleRearInner = 1;

    double wheelDiameter = 13;

    double roverBodySpeedTarget = 1;
    double frontLeftSpeedTarget = 1;
    double frontRightSpeedTarget = 1;
    double rearLeftSpeedTarget = 1;
    double rearRightSpeedTarget = 1;

    // Holds the Y value of steering circle
    private double mSteerCircleEdgeY = 0;
    private double mSteerCircleEdgeX;
    private double slopeSteerHeading;
    private double angleSteerHeading;
    private double slopeRoverBodySteerHypot;
    private double angleRoverBodySteerHypot;
    private double crossingPoint;
    private double bOfcrossingPoint;
    private Point2D.Double relMousePos = new Point2D.Double();

    public Truck(Point.Double drawLocation) {
        mWheelFrontLeft = new Wheel(Wheel.WHEEL_FRONT_LEFT, Color.GREEN);
        mWheelFrontRight = new Wheel(Wheel.WHEEL_FRONT_RIGHT, Color.GREEN);
        mWheelRearLeft = new Wheel(Wheel.WHEEL_REAR_LEFT, Color.RED);
        mWheelRearRight = new Wheel(Wheel.WHEEL_REAR_RIGHT, Color.RED);

        mWheels = new Wheel[]{
                mWheelFrontLeft, mWheelFrontRight,
                mWheelRearLeft, mWheelRearRight
        };

        mSteeringArc = new SteeringArc();

        mDrawLocation = new Point.Double();
        if (mTroubleshootingMessages) {
            System.out.println("AA Draw Location defined here " + mDrawLocation);
        }
        drawLocation = new Point.Double(427.5, 180.0); //427.5 if this is set to a lower value the bounding box is closer to the correct starting point but there must be a negative that is lower so the circle is farther right as this gets smaller.
        mDrawLocation.setLocation(drawLocation);
        if (mTroubleshootingMessages) {
            System.out.println("BB Draw Location defined here " + mDrawLocation);
        }

        calculateDimensions();
        mWheelFrontLeft.setDrawAngle(10); // runs only once at start up
    }

    public double getSteerCircleEdgeY() {
        return mDrawLocation.y + mSteerCircleEdgeY;
    }

    public double getSteerCircleEdgeMaxX() {
        return mMouseBoundingBox.getMaxX();
    }

    public double getSteerCircleEdgeMinX() {
        return mMouseBoundingBox.getMinX();
    }

    public Wheel[] getWheels() {
        return mWheels;
    }

    public void setSteeringMode(String steeringModeName) {
        mSteeringMode = SteeringMode.getByName(steeringModeName);
    }

    public void setDrawWidth(double drawWidth) {
        mDrawWidth = drawWidth;
        calculateDimensions();
    }

    public void setDrawLength(double drawHeight) {
        mTruckDrawLength = drawHeight;
        calculateDimensions();
    }

    public void setDrawScale(double drawScale) {
        mDrawScale = drawScale;
        calculateDimensions();
    }

    public double getDrawScale() {
        return mDrawScale;
    }


    public Point.Double getDrawLocation() {
        return mDrawLocation;
    }


    public double getForwardAngle() {
        return mForwardAngle;
    }

    /**
     * getForwardAngleRaw is the cumulative forward angle. Raw means it is not weighted by the speedratios
     */
    public double getForwardAngleRaw() {// this is the cumulative forward angle. Raw means it is not weighted by the speedratios
        return mForwardAngleRaw;
    }

    /**
     * addForwardAngle is the incremental amount to add
     */
    public void addForwardAngle(double angle) {
        mForwardAngle = angle; // mForwardAngle += angle;//Why this line is commented?
    }

    /**
     * addForwardAngleRaw  is the cumulative forward angle. Raw means it is not weighted by the speedratios.
     * this value is primarily used to update the label on GUI
     */
    public void addForwardAngleRaw(double angle) { // this is the cumulative forward angle. Raw means it is not weighted by the speedratios
        mForwardAngleRaw += angle;
    }

    public void changeTroubleshootingDisplay(int mWheel_ID) {
        System.err.println("pressed " + wheel_ID + " ------------------------------------------------------------------------------");
        wheel_ID = mWheel_ID;
    }

    private void calculateDimensions() {
        mTruckRectangle.setRect(-mDrawWidth / 2 * mDrawScale,
                -mTruckDrawLength / 2 * mDrawScale,
                mDrawWidth * mDrawScale,
                mTruckDrawLength * mDrawScale
        );
        mWheelFrontLeft.setDrawLocation(
                mTruckRectangle.x, mTruckRectangle.y);

        mWheelFrontLeft.setDrawScale(mDrawScale);

        mWheelFrontRight.setDrawLocation(
                mTruckRectangle.getMaxX(), mTruckRectangle.y);
        mWheelFrontRight.setDrawScale(mDrawScale);

        mWheelRearLeft.setDrawLocation(
                mTruckRectangle.x, mTruckRectangle.getMaxY());
        mWheelRearLeft.setDrawScale(mDrawScale);

        mWheelRearRight.setDrawLocation(
                mTruckRectangle.getMaxX(), mTruckRectangle.getMaxY());
        mWheelRearRight.setDrawScale(mDrawScale);
    }

    public boolean isWithinTruckBody(Point.Double point) {
        return mTruckRectangle.contains(point);
    }

    public boolean isWithinTruckBody(double pointX, double pointY) {
        pointX -= mDrawLocation.x;
        pointY -= mDrawLocation.y;
        return mTruckRectangle.contains(pointX, pointY);
    }

    public void moveForwardxSteps() {
        updateWheelSpeed();
    }

    public void moveBackxSteps() {
        updateWheelSpeed();
    }

    public void addSpeed() {
        mTruckSpeed++;
        updateWheelSpeed();
    }

    public void reduceSpeed() {
        mTruckSpeed--;
        updateWheelSpeed();
    }

    public double getSpeed() {
        return mTruckSpeed;
    }

    public void setSpeed(double speed) {
        mTruckSpeed = speed;
        System.out.println("mTruckSpeed setSpeed = " + mTruckSpeed);
        updateWheelSpeed();
    }

    public void updateEmergencyStopToFalse() {
        for (Wheel wheel : mWheels) {
            wheel.setEmergencyStopSetVelocityToFalse();
        }
    }

    public void updateWheelVelocityLimit(double velocityLimit) {
        for (Wheel wheel : mWheels) {
            wheel.setWheelVelocityLimit(velocityLimit);
        }
    }

    public void updateWheelSpeed() {
        for (Wheel wheel : mWheels) {
            wheel.setWheelSpeed(mTruckSpeed);
        }
    }

    public void moveTo(double pointX, double pointY) { // this runs continuously
        testValue = pointX;
        mDrawLocation.setLocation(pointX, pointY);
        calculateDimensions();
    }

    double testValue = 0;

    public void moveRelative(double pointX, double pointY) {
        mDrawLocation.x += pointX;
        mDrawLocation.y += pointY;
        calculateDimensions();
    }

    public void increaseWheelsAngle(double angle) {
        for (Wheel wheel : mWheels) {
            wheel.increaseDrawAngle(angle);
        }
    }

    public void setWheelsAngle(double angle) {
        for (Wheel wheel : mWheels) {
            wheel.setDrawAngle(angle);
        }
    }

    public void setWheelsAngle(int wheelIndex, double angle) {
        if (wheelIndex >= mWheels.length || wheelIndex < 0) return;
        mWheels[wheelIndex].setDrawAngle(angle);
    }

    public void increaseWheelsAngle(int wheelIndex, double angle) {
        if (wheelIndex >= mWheels.length || wheelIndex < 0) return;

        mWheels[wheelIndex].increaseDrawAngle(angle);
    }

    public void stopMoving() {
        mSteeringMode = SteeringMode.NONE;
        for (Wheel wheel : mWheels) {
            //System.out.println("testing stopped code: "+wheel.getWheelName());
            wheel.setDrawAngle(0);
            wheel.setSpeeed("Truck.java_stopMoving", angleRoverBodyTarget);
            wheel.setWheelVelocityLimit(0);
            wheel.setEmergencyStopSetVelocityToTrue();
            //wheel.setSpeedRatio(1);
        }
    }

    public boolean isStraight() {
        for (Wheel wheel : mWheels) {
            if (Math.abs(wheel.getGhostAngle()) > STRAIGHT_ANGLE_TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    private void rotateWheels() {

        Point.Double diffPoint;
        double angle;
        //System.out.print("delta:");
        //System.out.println((mWheelFrontRight.getReadDutyCycle(0)-mWheelFrontRight.getReadDutyCycle(1)));

        switch (mSteeringMode) {
            case NONE:
                for (Wheel wheel : mWheels) {
                    wheel.setDrawAngle(0);
                    wheel.setSpeeed("Truck.java_rotateWheelsTo", angleRoverBodyTarget);
                    wheel.setWheelVelocityLimit(0);
                    wheel.setWheelSpeed(0);
                    wheel.setEmergencyStopSetVelocityToTrue(); // this just forces the wheel device to set the velocity (presumably to zero) - it isn't a true emergency stop
                    //wheel.setSpeedRatio(1);
                }
                break;
            case MOUSE_FREE:
                //DO NOTHING
                break;
            case STRAIGHT:
                angle = 0;
                mTruckSpeedBaseline = 1;
                mWheelFrontLeft.setDrawAngle(angle);
                mWheelFrontLeft.setSpeeed("Truck.java_rotateWheelsTo case STRAIGHT", angle);
                //mWheelFrontLeft.setSpeedRatio(mTruckSpeedBaseline);

                mWheelFrontRight.setDrawAngle(angle);
                mWheelFrontRight.setSpeeed("Truck.java_rotateWheelsTo case STRAIGHT", angle);
                //mWheelFrontRight.setSpeedRatio(mTruckSpeedBaseline);

                mWheelRearLeft.setDrawAngle(angle);
                mWheelRearLeft.setSpeeed("Truck.java_rotateWheelsTo case STRAIGHT", angle);
                //mWheelRearLeft.setSpeedRatio(mTruckSpeedBaseline);

                mWheelRearRight.setDrawAngle(angle);
                mWheelRearRight.setSpeeed("Truck.java_rotateWheelsTo case STRAIGHT", angle);
                //mWheelRearRight.setSpeedRatio(mTruckSpeedBaseline);

                break;

            case TURN_AROUND:
                rotateWheelsTo_toPoint.setLocation(rotateWheelsTo_toPoint.x,
                        mDrawLocation.y - mTruckDrawLength / 2 * mDrawScale); // need to measure off the center point of the wheel and not the corner of the wheel.
                diffPoint = Geom.getDiff(rotateWheelsTo_toPoint, mDrawLocation);

                mTruckHypotenuse = mDrawLocation.distance(rotateWheelsTo_toPoint);
                mTruckSpeedBaseline = (2 * mTruckHypotenuse * Math.PI) / mWheelDiameter;

                mWheelFrontLeft.setDrawAngle(0);
                mWheelFrontLeft.setHypotenuseTo(diffPoint);

                mWheelFrontRight.setDrawAngle(0);
                mWheelFrontRight.setHypotenuseTo(diffPoint);

                mWheelRearLeft.rotateTo(diffPoint, 0);
                mWheelRearLeft.setHypotenuseTo(diffPoint);

                mWheelRearRight.rotateTo(diffPoint, 0);
                mWheelRearRight.setHypotenuseTo(diffPoint);

                break;
            case SIDE_TO_SIDE:
                mTruckSpeedBaseline = 1;
                angle = 90 + Geom.getSlope(mDrawLocation, rotateWheelsTo_toPoint);
                mWheelFrontLeft.setDrawAngle(angle);
                mWheelFrontLeft.setSpeeed("Truck.java_rotateWheelsTo case SIDE_TO_SIDE", angleRoverBodyTarget);
                //mWheelFrontLeft.setSpeedRatio(mTruckSpeedBaseline);

                mWheelFrontRight.setDrawAngle(angle);
                mWheelFrontRight.setSpeeed("Truck.java_rotateWheelsTo case SIDE_TO_SIDE", angleRoverBodyTarget);
                // mWheelFrontRight.setSpeedRatio(mTruckSpeedBaseline);

                mWheelRearLeft.setDrawAngle(angle);
                mWheelRearLeft.setSpeeed("Truck.java_rotateWheelsTo case SIDE_TO_SIDE", angleRoverBodyTarget);
                //mWheelRearLeft.setSpeedRatio(mTruckSpeedBaseline);

                mWheelRearRight.setDrawAngle(angle);
                mWheelRearRight.setSpeeed("Truck.java_rotateWheelsTo case SIDE_TO_SIDE", angleRoverBodyTarget);
                //mWheelRearRight.setSpeedRatio(mTruckSpeedBaseline);

                break;

            case PIVOT:
                diffPoint = Geom.getDiff(rotateWheelsTo_toPoint, mDrawLocation);

                mTruckHypotenuse = mDrawLocation.distance(rotateWheelsTo_toPoint);
                mTruckSpeedBaseline = (2 * mTruckHypotenuse * Math.PI) / mWheelDiameter;

                mWheelFrontLeft.rotateTo(diffPoint, 0);
                mWheelFrontLeft.setHypotenuseTo(diffPoint);

                mWheelFrontRight.rotateTo(diffPoint, 0);
                mWheelFrontRight.setHypotenuseTo(diffPoint);

                mWheelRearLeft.rotateTo(diffPoint, 0);
                mWheelRearLeft.setHypotenuseTo(diffPoint);

                mWheelRearRight.rotateTo(diffPoint, 0);
                mWheelRearRight.setHypotenuseTo(diffPoint);

                break;

            case FRONT_STEER:
                rotateWheelsTo_toPoint.setLocation(
                        rotateWheelsTo_toPoint.x,
                        rotateWheelsTo_toPoint.y); // need to measure off the center point of the wheel and not the corner of the wheel.
                diffPoint = Geom.getDiff(rotateWheelsTo_toPoint, mDrawLocation);
                angle = 90 + Geom.getSlope(mDrawLocation, rotateWheelsTo_toPoint);
                mTruckHypotenuse = mDrawLocation.distance(rotateWheelsTo_toPoint);
                mTruckSpeedBaseline = 1;

                mWheelFrontLeft.setDrawAngle(angleFrontOuter);
                mWheelFrontLeft.setHypotenuse(mTruckDrawLength, frontLeftHyp, frontLeftDeltaY, frontLeftDeltaX);
                mWheelFrontLeft.setSpeeed("Truck.java_rotateWheelsTo case FRONT_STEER", angleRoverBodyTarget);

                mWheelFrontRight.setDrawAngle(angleFrontInner);
                mWheelFrontRight.setHypotenuse(mTruckDrawLength, frontRightHyp, frontRightDeltaY, frontRightDeltaX);
                mWheelFrontRight.setSpeeed("Truck.java_rotateWheelsTo case FRONT_STEER", angleRoverBodyTarget);

                mWheelRearLeft.setDrawAngle(0);
                mWheelRearLeft.setHypotenuse(mTruckDrawLength, rearLeftHyp, rearLeftDeltaY, rearLeftDeltaX);
                mWheelRearLeft.setSpeeedRatioRear(speedRatioTarget[3]);
                mWheelRearLeft.setSpeeed("Truck.java_rotateWheelsTo case FRONT_STEER", angleRoverBodyTarget);

                mWheelRearRight.setDrawAngle(0);
                mWheelRearRight.setHypotenuse(mTruckDrawLength, rearRightHyp, rearRightDeltaY, rearRightDeltaX);
                mWheelRearRight.setSpeeedRatioRear(speedRatioTarget[4]);
                mWheelRearRight.setSpeeed("Truck.java_rotateWheelsTo case FRONT_STEER", angleRoverBodyTarget);
                break;
        }
    }

    public void rotateWheelsTo(Point.Double mRotateWheelsTo_toPoint) {
        rotateWheelsTo_toPoint = mRotateWheelsTo_toPoint;
        rotateWheels();
    }

    public Chart.ChartParamsDataset[] getChartParamsDatasets() {
        Chart.ChartParamsDataset[] chartParamsDatasets =
                new ChartParamsDataset[mWheels.length];
        int i = 0;
        for (Wheel wheel : mWheels) {
            chartParamsDatasets[i] = wheel.getChartParamsDataset();
            i++;
        }
        return chartParamsDatasets;
    }

    public void updateChartParamsDataset() {
        for (Wheel wheel : mWheels) {
            wheel.updateChartParamsDataset();
        }
    }

    /**
     * Calculate & update different angles and positions based
     * on given mouse position
     *
     * @param mousePos
     */
    public void calc(Point.Double mousePos) {

        relMousePos.setLocation(Geom.getDiff(mousePos, mDrawLocation));
        // MouseBoundingBox holds the area in which
        // the the movement of mouse will control the movement of steercircle
        mMouseBoundingBox = new Rectangle.Double();
        if (mTroubleshootingMessages) {
            System.out.println("This needs to be set sooner: mDrawLocation.x + mTruckRectangle.getCenterX() - mTruckRectangle.getWidth() * 0.5, " + mDrawLocation.x + " " + mTruckRectangle.getCenterX() + " " + mTruckRectangle.getWidth() / 2);
        }
        mMouseBoundingBox.setRect(
                // Set the left X value of the the left value of the truck box
                mDrawLocation.x + mTruckRectangle.getCenterX() - mTruckRectangle.getWidth() * 0.5,
                // Set the top Y value before the tip of front wheels including
                // some fraction of wheel outer box height
                mDrawLocation.y + mWheelFrontLeft.getOuterTopY() - mWheelFrontLeft.getOuterHeight() / 2,
                // Set the width to that of truck box
                mTruckRectangle.getWidth(),
                // Set the height to some fraction of the wheel outerbox
                // for modest display
                mWheelFrontLeft.getOuterHeight() / 2
        );

        if ((mousePos.x >= mMouseBoundingBox.getMinX() && mousePos.x <= mMouseBoundingBox.getMaxX())
                && (mousePos.y >= mMouseBoundingBox.getMinY() && mousePos.y <= mMouseBoundingBox.getMaxY())
        ) {
            modifiedMousePos.x = mousePos.x;
            modifiedRelMousePos.x = mousePos.x;
            if (mTroubleshootingMessages) {
                System.err.println("This fixes the bounding box (too late though) changing modifiedMousePos.x and modifiedRelMousePos.x " + modifiedMousePos.x + " " + modifiedRelMousePos.x);
            }
        }

        modifiedMousePos.y = +40 * mDrawScale + mTruckDrawLength / mDrawScale * 2;
        // see steerCircleEdgeY variable as it is related to this setting
        modifiedRelMousePos.y = -40 * mDrawScale - mTruckDrawLength;//-180;

        // what is mSteerCircleEdgeX? it should be zero for 'straight'
        // can i just set mSteerCircleEdgeX = modifiedMousePos.x?

        mSteerCircleEdgeX = -mDrawLocation.x + modifiedMousePos.x;
        if (mTroubleshootingMessages) {
            System.out.println("mSteerCircleEdgeX " + mSteerCircleEdgeX + " (this should read zero when driving straight forward) " + -mDrawLocation.x + " " + modifiedMousePos.x + " " + mousePos.x);
        }
        // first number determines how high above the rover the steer heading dot is
        mSteerCircleEdgeY = -mDrawLocation.y + mMouseBoundingBox.getMinY() + 12; // a positive number makes the circle lower on the screen

// -----------------------------------------------------------------------------------        
        deviceNumber = 4; // set to steering point
        startX[deviceNumber] = (int) (mTruckRectangle.x + mDrawWidth / 2 * mDrawScale);
        // Aug 8 2019 - added +mTruckDrawLength/2*mDrawScale to the code. without this the line started at the top of rover
        startY[deviceNumber] = (int) (mTruckRectangle.y + mTruckDrawLength / 2 * mDrawScale);
        double leftEdge = modifiedMousePos.x;
        endX[deviceNumber] = (int) (leftEdge);
        // endX is the end of the line drawn out from the wheel - this is the center point of the turning radius (for mode 2, 4)
        endY[deviceNumber] = (int) mTruckRectangle.getMaxY();
        double steerCircleHyptotenuse = 1;

        deltaX = startX[deviceNumber] - (int) mSteerCircleEdgeX;
        deltaY = startY[deviceNumber] - (int) mSteerCircleEdgeY + mTruckDrawLength / 2 * mDrawScale;
        slopeSteerHeading = (deltaY) / (deltaX);
        angleSteerHeading = Math.toDegrees(Math.atan(slopeSteerHeading));

        // this is an incredibly long line that isn't fully displayed. We intersect this line to determine the crossingpoint.
        double endX2 = startX[deviceNumber] + 999999 * Math.cos(Math.toRadians(angleSteerHeading + 90)) * mDrawScale;
        double endY2 = startY[deviceNumber] + 999999 * Math.sin(Math.toRadians(angleSteerHeading + 90)) * mDrawScale;
        // this is an imaginary line so don't draw it
        //g2d.drawLine(startX[deviceNumber], startY[deviceNumber], (int)endX2, (int)endY2);
        deltaX = startX[deviceNumber] - (int) endX2; //should this be divided by mDrawScale? this feeds crossingpoint so it explains why cp is what it is.
        deltaY = startY[deviceNumber] - (int) endY2;
        slopeRoverBodySteerHypot = (deltaY) / (deltaX);
        angleRoverBodySteerHypot = Math.toDegrees(Math.atan(slopeRoverBodySteerHypot));
        // y = m x + b;  m is the slope; b is a constant indicating above/below on the y axis
        // flat line across the lower part of rover has equation of 0 x + b since the slope is zero.  Thus y = mTruckRectangle.getMaxY());

        // line 1 crosses x at unknow position - it is at 400, 200 - so first we need to find b
        // b = y - (mx)
        // b = startY[deviceNumber] - (slopeRoverBodySteerHypot * startX[deviceNumber])
        bOfcrossingPoint = startY[deviceNumber] - (slopeRoverBodySteerHypot * startX[deviceNumber]);
        // line 1 formula: y = slopeRoverBodySteerHypot * ?? + b
        //                 slopeRoverBodySteerHypot * ?? = y - b
        //                 ?? = (y - b)/slopeRoverBodySteerHypot
        //                 ?? = (mTruckRectangle.getMaxY()) - b])/slopeRoverBodySteerHypot

        // line 1 formula: y = slopeRoverBodySteerHypot * ?? + startY[deviceNumber]
        //                 slopeRoverBodySteerHypot * ?? = y - startY[deviceNumber]
        //                 ?? = (y - startY[deviceNumber])/slopeRoverBodySteerHypot
        //                 ?? = (mTruckRectangle.getMaxY()) - startY[deviceNumber])/slopeRoverBodySteerHypot
        crossingPoint = (mTruckRectangle.getMaxY() - bOfcrossingPoint) / slopeRoverBodySteerHypot;

        endX[deviceNumber] = (int) crossingPoint;

        deltaX = (startX[deviceNumber] - endX[deviceNumber]) / mDrawScale;
        deltaY = (startY[deviceNumber] - endY[deviceNumber]) / mDrawScale;
        double hypotenuse = Math.pow((Math.pow(deltaX, 2) + Math.pow(deltaY, 2)), 0.5); // don't divide deltaY by 2 even though midpoint because in this case (the rover case) we're already only 1/2 way up???

        // Rover body actuals (based on calcs derived from actual front wheel steering positions
        anglesActual = Wheel.getGhostAngleStatic(); // must use actual steering angle values when calc'ing speed or the wheels will turn at wrong speeds relative to each other.
        BaseLengthActual = Wheel.getBaseLengthStatic();

//        System.out.println("anglesActual "
//                            + String.format("%.4f",anglesActual[0])+" "
//                            + String.format("%.4f",anglesActual[1])
//                            +" BaseLengthActual:"
//                            + String.format("%.2f",BaseLengthActual[0])+" "
//                            + String.format("%.2f",BaseLengthActual[1])+"; crossingPoint: "
//                            + String.format("%.2f",crossingPoint)+"; deltaY: "
//                            + String.format("%.2f",deltaY)+"; deltaX: "
//                            + String.format("%.2f",deltaX)
//                                ); //crossing point should be same as base length plus part of the width of rover?

        //double slopeRoverBodySteerHypotActual = (deltaY)/(deltaX);
        //crossingPointActual = (mTruckRectangle.getMaxY() - b_Actual)/slopeRoverBodySteerHypotActual;

        if (deltaX == 0) {
            slopeRoverBodyTarget = 1;
        } else {
            slopeRoverBodyTarget = (deltaY) / (deltaX); // target slope (updates as fast as the mouse moves to a new position). The steppers are slow to reach target position.

        }
        angleRoverBodyTarget = Math.toDegrees(Math.atan(slopeRoverBodyTarget));
        roverBodySpeedTarget = (2 * hypotenuse * Math.PI) / wheelDiameter;


//        int labelX_hyp = (int) endX[deviceNumber]+(int)deltaX/2*(int)mDrawScale+4; // all of these could be removed after some more testing. use xDisplay and yDisplay instead. note made Jan 1, 2020
//        int labelY_hyp = (int) endY[deviceNumber]+(int)deltaY/2*(int)mDrawScale+4;
//        labelX_hyp = (int) -100;
//        labelY_hyp = (int) 60;
        //g2d.drawString("roverBodySpeedTarget "+roverBodySpeedTarget+"; angleFrontInner "+angleFrontInner+"; radius "+String.format("%.1f", hypotenuse)+" startX "+startX[deviceNumber] +" startY "+startY[deviceNumber]+" endX "+endX[deviceNumber] +" endY "+endY[deviceNumber], labelX_hyp, labelY_hyp);

        mSteeringArc.SteeringArc(); // doesn't do anything - this is a placeholder to move the arc math into
        // first wheel circle starts here -> mDrawLocation.x, mDrawLocation.y

        // 1 - centerX and centerY can be defined as:
        //      centerX = min( top of rover, max(mousex, bottom of rover) )
        //      centerY = max( side of rover, mouseY*1000)
        // 2 - radius = 1/2 of (centerX - mDrawLocation.x)
        // 3 - top / left of circle are:
        //      top = centerX - radius
        //      left = centerY - radius

        // device 0 - front left
        deviceNumber = 0;
        startX[deviceNumber] = (int) (mTruckRectangle.x);//+mDrawWidth); // +wheelWidth_int/2
        startY[deviceNumber] = (int) (mTruckRectangle.y);//+mTruckDrawLength);
        endX[deviceNumber] = (int) (crossingPoint);

        endY[deviceNumber] = (int) mTruckRectangle.getMaxY();

        deltaX = (startX[deviceNumber] - endX[deviceNumber]) / mDrawScale;
        deltaY = (startY[deviceNumber] - endY[deviceNumber]) / mDrawScale;
        hypotenuse = Math.pow((Math.pow(deltaX, 2) + Math.pow(deltaY, 2)), 0.5);
        frontLeftDeltaX = deltaX;
        frontLeftDeltaY = deltaY;
        frontLeftHyp = hypotenuse;

        if (deltaX == 0) {
            slopeFrontOuter = 1;
        } else {
            slopeFrontOuter = (deltaY) / (deltaX);
            angleFrontOuter = Math.toDegrees(Math.atan(slopeFrontOuter));
        }
        frontLeftSpeedTarget = (2 * hypotenuse * Math.PI) / wheelDiameter;

        // front left oval
//        labelX_hyp = (int) -385;
//        labelY_hyp = (int) -170;

        // device 1 - front right
        deviceNumber = 1;
        startX[deviceNumber] = (int) (mTruckRectangle.getMaxX());//+mDrawWidth); // +wheelWidth_int/2
        startY[deviceNumber] = (int) (mTruckRectangle.y);//+mTruckDrawLength);
        endX[deviceNumber] = (int) (crossingPoint);
        endY[deviceNumber] = (int) mTruckRectangle.getMaxY();

        deltaX = (startX[deviceNumber] - endX[deviceNumber]) / mDrawScale;
        deltaY = (startY[deviceNumber] - endY[deviceNumber]) / mDrawScale;
        hypotenuse = Math.pow((Math.pow(deltaX, 2) + Math.pow(deltaY, 2)), 0.5);
        frontRightDeltaX = deltaX;
        frontRightDeltaY = deltaY;
        frontRightHyp = hypotenuse;
        if (deltaX == 0) {
            slopeFrontInner = 1;
        } else {
            slopeFrontInner = (deltaY) / (deltaX);
            angleFrontInner = Math.toDegrees(Math.atan(slopeFrontInner));
        }
        frontRightSpeedTarget = (2 * hypotenuse * Math.PI) / wheelDiameter;

        // front right steer circle
//        labelX_hyp = (int) -385;
//        labelY_hyp = (int) -150;

        // device 2 - rear left
        deviceNumber = 2;
        startX[deviceNumber] = (int) (mTruckRectangle.getMinX());//+mDrawWidth); // +wheelWidth_int/2
        startY[deviceNumber] = (int) (mTruckRectangle.getMaxY());//+mTruckDrawLength);
        endX[deviceNumber] = (int) (crossingPoint); //(int) BaseLengthActual[0]; //(int)(crossingPoint); changed Aug 3 to use actual rotation of steppers vs target rotation
        endY[deviceNumber] = (int) (mTruckRectangle.getMaxY());//+mTruckDrawLength);

        deltaX = (startX[deviceNumber] - endX[deviceNumber]) / mDrawScale;
        deltaY = (startY[deviceNumber] - endY[deviceNumber]) / mDrawScale;
        hypotenuse = Math.pow((Math.pow(deltaX, 2) + Math.pow(deltaY, 2)), 0.5);
//        System.out.println("rLft - deltaX: "+deltaX+"; crossingPoint(not divided by mDrawScale!): "+String.format("%.2f", crossingPoint)+"; deltaY "+deltaY+"; hypotenuse: "+hypotenuse+ 
//                " startX: "+(startX[deviceNumber]+"; endX: "+endX[deviceNumber])+"; mDrawScale: "+mDrawScale);

//                " BaseLengthActual[0] "
//                +String.format("%.2f", BaseLengthActual[0])+ " BaseLengthActual[1] "
//                +String.format("%.2f", BaseLengthActual[1])+
        //deltaX = -BaseLengthActual[0];
        //hypotenuse = Math.pow((Math.pow(deltaX, 2) + Math.pow(deltaY,2)), 0.5);
        //System.out.println("rearLeft - deltaX_After: "+deltaX+"; deltaY_After "+deltaY+"; hypotenuse: "+hypotenuse+ " BaseLengthActual[1] "+BaseLengthActual[1]);
        rearLeftDeltaX = deltaX;
        rearLeftDeltaY = deltaY;
        rearLeftHyp = hypotenuse;
        if (deltaX == 0) {
            slopeRearOuter = 1;
        } else {
            slopeRearOuter = (deltaY) / (deltaX);
            angleRearOuter = Math.toDegrees(Math.atan(slopeRearOuter));
        }
        rearLeftSpeedTarget = (2 * hypotenuse * Math.PI) / wheelDiameter;

        // rear left steer circle
//        labelX_hyp = (int) -385;
//        labelY_hyp = (int) -110;
        //g2d.drawString("rearLeftSpeedTarget "+String.format("%.1f",rearLeftSpeedTarget)+"; angleRearOuter "+String.format("%.1f",angleRearOuter)+"; radius "+String.format("%.1f", hypotenuse), labelX_hyp, labelY_hyp);

        // device 3 - rear right
        deviceNumber = 3;
        startX[deviceNumber] = (int) (mTruckRectangle.getMaxX());//+mDrawWidth); // +wheelWidth_int/2
        startY[deviceNumber] = (int) (mTruckRectangle.getMaxY());//+mTruckDrawLength);
        endX[deviceNumber] = (int) (crossingPoint);
        endY[deviceNumber] = (int) (mTruckRectangle.getMaxY());//+mTruckDrawLength);

        deltaX = (startX[deviceNumber] - endX[deviceNumber]) / mDrawScale;
        deltaY = (startY[deviceNumber] - endY[deviceNumber]) / mDrawScale;
        hypotenuse = Math.pow((Math.pow(deltaX, 2) + Math.pow(deltaY, 2)), 0.5);
        rearRightDeltaX = deltaX;
        rearRightDeltaY = deltaY;
        rearRightHyp = hypotenuse;
        if (deltaX == 0) {
            slopeRearInner = 1;
        } else {
            slopeRearInner = (deltaY) / (deltaX);
            angleRearInner = Math.toDegrees(Math.atan(slopeRearInner));
        }
        rearRightSpeedTarget = (2 * hypotenuse * Math.PI) / wheelDiameter;

        // rear right steer circle
//        labelX_hyp = (int) -385;
//        labelY_hyp = (int) -130;
        //g2d.drawString("rearRightSpeedTarget "+String.format("%.1f",rearRightSpeedTarget)+"; angleRearOuter "+String.format("%.1f",angleRearInner)+"; radius "+String.format("%.1f", hypotenuse), labelX_hyp, labelY_hyp);


        speedRatioTarget[0] = 1; // device 0 is the rover body so should be ratio 1.  roverBodySpeedTarget is only used to calc the speedRatioTarget[x] values - speedRatioTarget[0] is the target speed of the rover
        speedRatioTarget[1] = frontLeftSpeedTarget / roverBodySpeedTarget;
        speedRatioTarget[2] = frontRightSpeedTarget / roverBodySpeedTarget;
        speedRatioTarget[3] = rearLeftSpeedTarget / roverBodySpeedTarget;
//        System.err.println(
//                "angleRearOuter "+String.format("%.4f", angleRearOuter)+
//                " speedRatioTarget[3]: "+String.format("%.4f", speedRatioTarget[3])+
//                "; roverBodySpeedTarget "+String.format("%.2f", roverBodySpeedTarget)+
//                "; rearLeftSpeed: "+String.format("%.2f", rearLeftSpeedTarget)+
//                " rearLeftHyp: "+String.format("%.2f", rearLeftHyp));
        speedRatioTarget[4] = rearRightSpeedTarget / roverBodySpeedTarget;
//        System.err.println(
//                "angleRearInner "+String.format("%.4f", angleRearInner)+
//                " speedRatioTarget[4]: "+String.format("%.4f", speedRatioTarget[4])+
//                "; roverBodySpeedTarget "+String.format("%.2f", roverBodySpeedTarget)+
//                "; rearRightSpeed: "+String.format("%.2f", rearRightSpeedTarget)+
//                " rearRightHyp: "+String.format("%.2f", rearRightHyp));        
        rotateWheels();
    }


    public void draw(Graphics2D g2d) {

        boolean displayTroubleshootingValues = true;
        int xDisplayBase = 120;
        int yDisplayBase = -150;
        int xDisplay = 0;
        int yDisplay = 0;

        //g2d.drawRect((int)(mDrawLocation.x-mDrawWidth), (int)(-1*mDrawScale-mTruckDrawLength), (int)(mDrawWidth*1.5), (int)(mTruckDrawLength*0.35));

        int alpha = 127; // 127 is 50% transparent
        Color myColour = new Color(255, 0, 0, alpha);
        g2d.setColor(myColour);
        g2d.draw(mMouseBoundingBox);
        myColour = new Color(128, 128, 128, alpha);
        g2d.setColor(myColour);
        g2d.fill(mMouseBoundingBox);

        g2d.setStroke(mTruckStroke);

        AffineTransform saveAT = g2d.getTransform();
        g2d.translate(mDrawLocation.x, mDrawLocation.y);
        g2d.rotate(Math.toRadians(mDrawAngle));

        g2d.setColor(mTruckColor);
        g2d.fill(mTruckRectangle);

        g2d.setColor(Color.BLACK);
        g2d.draw(mTruckRectangle);

        //g2d.drawString("TRUCK: mTruckSpeedRatio "+String.format("%.1f",1.0)+" mTruckHypotenuse "+String.format("%.1f",mTruckHypotenuse)+" mTruckSpeedBaseline "+String.format("%.1f", mTruckSpeedBaseline),(int)mDrawLocation.x-100,(int)mDrawLocation.y+15);

        // steer line. line going up from middle of rover // line from top rover center to upper end of steering heading
        g2d.setColor(Color.GREEN);
        deviceNumber = 4; // // set to steering point

        // small oval on upper end of steer line
        g2d.fillOval((int) mSteerCircleEdgeX - 12 / 2, (int) mSteerCircleEdgeY - 12 / 2, 12, 12);
        g2d.setColor(Color.RED);
        g2d.drawLine((int) mSteerCircleEdgeX, (int) mSteerCircleEdgeY, startX[deviceNumber], startY[deviceNumber]);
        deltaX = startX[deviceNumber] - (int) mSteerCircleEdgeX;
        deltaY = startY[deviceNumber] - (int) mSteerCircleEdgeY + mTruckDrawLength / 2 * mDrawScale;
        //g2d.drawString("slopeSteerHeading / angleSteerHeading "+String.format("%.2f",slopeSteerHeading)+" / "+String.format("%.2f",angleSteerHeading), -385, -220);

        if (displayTroubleshootingValues && wheel_ID == 4) {
            g2d.setColor(Color.RED);
            xDisplay = xDisplayBase + 0;
            yDisplay = yDisplayBase + 0;
            g2d.drawString(testValue + "    modifiedMousePos.x: " + modifiedMousePos.x, xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString("    mDrawLocation.x: " + mDrawLocation.x, xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString("    steerCircleEdgeX: " + mSteerCircleEdgeX, xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString("    steerCircleEdgeY: " + mSteerCircleEdgeY, xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString("    mTruckDrawLength: " + mTruckDrawLength, xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString("    mDrawScale: " + mDrawScale, xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString("    deltaX: " + deltaX, xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString("    deltaY: " + deltaY + " slopeSteerHeading: " + slopeSteerHeading, xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString("    angleSteerHeading: " + String.format("%.3f", angleSteerHeading), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 25;
            g2d.drawString("    relMousePos.x: " + String.format("%.3f", relMousePos.x), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString("    relMousePos.y: " + String.format("%.3f", relMousePos.y), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString("    modifiedMousePos.x: " + String.format("%.3f", modifiedMousePos.x), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString("    modifiedMousePos.y: " + String.format("%.3f", modifiedMousePos.y), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString("    modifiedRelMousePos.x: " + String.format("%.3f", modifiedRelMousePos.x), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString("    modifiedRelMousePos.y: " + String.format("%.3f", modifiedRelMousePos.y), xDisplay, yDisplay);
        }

        // line that goes at a right angle to the steer heading line. 
        // this is an incredibly long line that isn't fully displayed. We intersect this line to determine the crossingpoint.
        double endX2 = startX[deviceNumber] + 999999 * Math.cos(Math.toRadians(angleSteerHeading + 90)) * mDrawScale;
        double endY2 = startY[deviceNumber] + 999999 * Math.sin(Math.toRadians(angleSteerHeading + 90)) * mDrawScale;
        // this is an imaginary line so don't draw it
        //g2d.drawLine(startX[deviceNumber], startY[deviceNumber], (int)endX2, (int)endY2);
        deltaX = startX[deviceNumber] - (int) endX2; //should this be divided by mDrawScale? this feeds crossingpoint so it explains why cp is what it is.
        deltaY = startY[deviceNumber] - (int) endY2;

        if (displayTroubleshootingValues && wheel_ID == 4) {
            g2d.setColor(Color.RED);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 30;
            g2d.drawString("angleSteerHeading: " + String.format("%.3f", angleSteerHeading) + " crossingPoint: " + crossingPoint, xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString("    mTruckRectangle.getMaxY(): " + mTruckRectangle.getMaxY(), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString("    b: " + bOfcrossingPoint + " slopeRoverBodyHypot: " + slopeRoverBodySteerHypot, xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString("    angleRoverBodyHypot: " + String.format("%.3f", angleRoverBodySteerHypot), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString("    startX: " + startX[deviceNumber] + " startY: " + (startY[deviceNumber]), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString("    deltaX: " + deltaX + " deltaY: " + String.format("%.1f", deltaY), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString("    endX2: " + String.format("%.1f", endX2), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString("    endY2: " + String.format("%.1f", endY2), xDisplay, yDisplay);
        }

        //g2d.drawString("crossingPoint "+String.format("%.2f",crossingPoint)+". slope: "+String.format("%.4f",slopeRoverBodySteerHypot)+". angle: "+String.format("%.2f",angleRoverBodySteerHypot), -125, -190);
        g2d.setColor(Color.ORANGE);
        g2d.drawOval((int) crossingPoint - 8 / 2, (int) mTruckRectangle.getMaxY() - 8 / 2, 8, 8); // this is a little circle drawn at the crossing point.
        g2d.drawLine((int) crossingPoint, (int) mTruckRectangle.getMaxY(), startX[deviceNumber], (int) startY[deviceNumber]); // startY is the location up/down on rover where the line starts.

        g2d.setColor(Color.ORANGE);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawLine(startX[deviceNumber], startY[deviceNumber] + 1, endX[deviceNumber], endY[deviceNumber]); // hypotenuse line that goes out from center of rover body

        if (displayTroubleshootingValues && (wheel_ID == 1 || wheel_ID == 2)) {
            g2d.setColor(Color.RED);
            xDisplay = xDisplayBase + 0;
            yDisplay = yDisplayBase + 0;
            g2d.drawString("Rover Body Center Orange Line", xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString("    crossingPoint: " + String.format("%.1f", crossingPoint), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString("    EndX: " + endX[deviceNumber] + "  EndY: " + endY[deviceNumber], xDisplay, yDisplay);
        }

        g2d.setColor(Color.BLUE);
        deltaX = (startX[deviceNumber] - endX[deviceNumber]) / mDrawScale;
        deltaY = (startY[deviceNumber] - endY[deviceNumber]) / mDrawScale;
        double hypotenuse = Math.pow((Math.pow(deltaX, 2) + Math.pow(deltaY, 2)), 0.5); // don't divide deltaY by 2 even though midpoint because in this case (the rover case) we're already only 1/2 way up???

        // Rover body actuals (based on calcs derived from actual front wheel steering positions
        anglesActual = Wheel.getGhostAngleStatic(); // must use actual steering angle values when calc'ing speed or the wheels will turn at wrong speeds relative to each other.
        BaseLengthActual = Wheel.getBaseLengthStatic();

//        System.out.println("anglesActual "
//                            + String.format("%.4f",anglesActual[0])+" "
//                            + String.format("%.4f",anglesActual[1])
//                            +" BaseLengthActual:"
//                            + String.format("%.2f",BaseLengthActual[0])+" "
//                            + String.format("%.2f",BaseLengthActual[1])+"; crossingPoint: "
//                            + String.format("%.2f",crossingPoint)+"; deltaY: "
//                            + String.format("%.2f",deltaY)+"; deltaX: "
//                            + String.format("%.2f",deltaX)
//                                ); //crossing point should be same as base length plus part of the width of rover?

        //double slopeRoverBodySteerHypotActual = (deltaY)/(deltaX);
        //crossingPointActual = (mTruckRectangle.getMaxY() - b_Actual)/slopeRoverBodySteerHypotActual;

        if (deltaX == 0) {
            slopeRoverBodyTarget = 1;
        } else {
            slopeRoverBodyTarget = (deltaY) / (deltaX); // target slope (updates as fast as the mouse moves to a new position). The steppers are slow to reach target position.

        }
        angleRoverBodyTarget = Math.toDegrees(Math.atan(slopeRoverBodyTarget));
        roverBodySpeedTarget = (2 * hypotenuse * Math.PI) / wheelDiameter;

        Color colortemp = g2d.getColor();
        g2d.setColor(Color.ORANGE);
        // roverBody steering oval (circle that goes through center of rover body)
        g2d.drawOval((int) (crossingPoint - hypotenuse * mDrawScale), (int) (mTruckRectangle.getMaxY() - hypotenuse * mDrawScale), (int) (hypotenuse * 2 * mDrawScale), (int) (hypotenuse * 2 * mDrawScale));

        if (displayTroubleshootingValues && (wheel_ID == 1 || wheel_ID == 2)) {
            g2d.setColor(Color.RED);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString(" Rover Body SpeedTarget " + String.format("%.1f", roverBodySpeedTarget), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString(" hypotenuse: " + (int) hypotenuse, xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 18;
            g2d.drawString(" anglesActual[0]BLDC: " + String.format("%.3f", anglesActual[0]), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString(" anglesActual[1]BLDC: " + String.format("%.3f", anglesActual[1]), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString(" BaseLengthActual[0]: " + String.format("%.3f", BaseLengthActual[0]), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString(" BaseLengthActual[1]: " + String.format("%.3f", BaseLengthActual[1]), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString(" mWheelFrontLeftSR: " + String.format("%.3f", mWheelFrontLeft.modified_mSpeedRatio), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString(" mWheelFrontRightSR: " + String.format("%.3f", mWheelFrontRight.modified_mSpeedRatio), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 18;
            g2d.drawString(" angleRoverBodyTarget: " + String.format("%.2f", angleRoverBodyTarget), xDisplay, yDisplay);
        }

        g2d.setColor(colortemp);

//        int labelX_hyp = (int) endX[deviceNumber]+(int)deltaX/2*(int)mDrawScale+4; // all of these could be removed after some more testing. use xDisplay and yDisplay instead. note made Jan 1, 2020
//        int labelY_hyp = (int) endY[deviceNumber]+(int)deltaY/2*(int)mDrawScale+4;
//        labelX_hyp = (int) -100;
//        labelY_hyp = (int) 60;
        //g2d.drawString("roverBodySpeedTarget "+roverBodySpeedTarget+"; angleFrontInner "+angleFrontInner+"; radius "+String.format("%.1f", hypotenuse)+" startX "+startX[deviceNumber] +" startY "+startY[deviceNumber]+" endX "+endX[deviceNumber] +" endY "+endY[deviceNumber], labelX_hyp, labelY_hyp);

        mSteeringArc.SteeringArc(); // doesn't do anything - this is a placeholder to move the arc math into
        // first wheel circle starts here -> mDrawLocation.x, mDrawLocation.y

        // 1 - centerX and centerY can be defined as:
        //      centerX = min( top of rover, max(mousex, bottom of rover) )
        //      centerY = max( side of rover, mouseY*1000)
        // 2 - radius = 1/2 of (centerX - mDrawLocation.x)
        // 3 - top / left of circle are:
        //      top = centerX - radius
        //      left = centerY - radius

        // device 0 - front left
        deviceNumber = 0;
        g2d.setColor(Color.RED);
        g2d.drawLine(startX[deviceNumber], startY[deviceNumber], endX[deviceNumber], endY[deviceNumber]);

        hypotenuse = frontLeftHyp;
        // front left oval
        g2d.drawOval((int) (crossingPoint - hypotenuse * mDrawScale), (int) (mTruckRectangle.getMaxY() - hypotenuse * mDrawScale), (int) (hypotenuse * 2 * mDrawScale), (int) (hypotenuse * 2 * mDrawScale));
//        labelX_hyp = (int) -385;
//        labelY_hyp = (int) -170;
        //g2d.drawString("frontLeftSpeedTarget "+String.format("%.1f",frontLeftSpeedTarget)+"; angleFrontOuter "+String.format("%.1f",angleFrontOuter)+"; radius "+String.format("%.1f", hypotenuse), labelX_hyp, labelY_hyp);

        if (displayTroubleshootingValues && wheel_ID == 1) {
            g2d.setColor(Color.RED);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 25;
            g2d.drawString("Front Left Wheel Red Line " + wheel_ID, xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString(" X: " + startX[deviceNumber] + " Y: " + (startY[deviceNumber]), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString(" EndX: " + endX[deviceNumber] + "  EndY: " + endY[deviceNumber], xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString(" SpeedTarget " + String.format("%.1f", frontLeftSpeedTarget), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString(" AngleTarget " + String.format("%.1f", angleFrontOuter) + "; radius " + String.format("%.1f", hypotenuse), xDisplay, yDisplay);
        }

        // device 1 - front right
        deviceNumber = 1;
        hypotenuse = frontRightHyp;
        // front right steer circle
        g2d.drawOval((int) (crossingPoint - hypotenuse * mDrawScale), (int) (mTruckRectangle.getMaxY() - hypotenuse * mDrawScale), (int) (hypotenuse * 2 * mDrawScale), (int) (hypotenuse * 2 * mDrawScale));
//        labelX_hyp = (int) -385;
//        labelY_hyp = (int) -150;
        //g2d.drawString("frontRightSpeedTarget "+String.format("%.1f",frontRightSpeedTarget)+"; angleFrontInner "+String.format("%.1f",angleFrontInner)+"; radius "+String.format("%.1f", hypotenuse), labelX_hyp, labelY_hyp);

        if (displayTroubleshootingValues && wheel_ID == 1) {
            g2d.setColor(Color.RED);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 25;
            g2d.drawString("Front Right Wheel Red Line " + wheel_ID, xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString(" X: " + startX[deviceNumber] + " Y: " + (startY[deviceNumber]), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString(" EndX: " + endX[deviceNumber] + "  EndY: " + endY[deviceNumber], xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString(" SpeedTarget " + String.format("%.1f", frontRightSpeedTarget), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString(" AngleTarget " + String.format("%.1f", angleFrontInner) + "; radius " + String.format("%.1f", hypotenuse), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 25;
            g2d.drawString("FrontLeft  speedRatioT[1] " + String.format("%.4f", speedRatioTarget[1]), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString("FrontRight speedRatioT[2] " + String.format("%.4f", speedRatioTarget[2]), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString("Speed ratios printed don't match target speed ratios ", xDisplay, yDisplay);
        }

        // device 2 - rear left
        deviceNumber = 2;
        g2d.setColor(Color.BLACK);
        g2d.drawLine(startX[deviceNumber], startY[deviceNumber] + 1, endX[deviceNumber], endY[deviceNumber] + 1);
        hypotenuse = rearLeftHyp;

        // rear left steer circle
        g2d.drawOval((int) (crossingPoint - hypotenuse * mDrawScale), (int) (mTruckRectangle.getMaxY() - hypotenuse * mDrawScale), (int) (hypotenuse * 2 * mDrawScale), (int) (hypotenuse * 2 * mDrawScale));
//        labelX_hyp = (int) -385;
//        labelY_hyp = (int) -110;
        //g2d.drawString("rearLeftSpeedTarget "+String.format("%.1f",rearLeftSpeedTarget)+"; angleRearOuter "+String.format("%.1f",angleRearOuter)+"; radius "+String.format("%.1f", hypotenuse), labelX_hyp, labelY_hyp);

        if (displayTroubleshootingValues && wheel_ID == 2) {
            g2d.setColor(Color.RED);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 25;
            g2d.drawString("Rear Left Wheel Red Line " + wheel_ID, xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString(" X: " + startX[deviceNumber] + " Y: " + (startY[deviceNumber]), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString(" EndX: " + endX[deviceNumber] + "  EndY: " + endY[deviceNumber], xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString(" SpeedTarget " + String.format("%.1f", rearLeftSpeedTarget), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString(" Angle " + String.format("%.1f", angleRearOuter) + "; radius " + String.format("%.1f", hypotenuse), xDisplay, yDisplay);
        }

        // device 3 - rear right
        deviceNumber = 3;
        g2d.setColor(Color.BLACK);
        g2d.drawLine(startX[deviceNumber], startY[deviceNumber] + 1, endX[deviceNumber], endY[deviceNumber] + 1);
        hypotenuse = rearRightHyp;

        // rear right steer circle
        g2d.drawOval((int) (crossingPoint - hypotenuse * mDrawScale), (int) (mTruckRectangle.getMaxY() - hypotenuse * mDrawScale), (int) (hypotenuse * 2 * mDrawScale), (int) (hypotenuse * 2 * mDrawScale));
//        labelX_hyp = (int) -385;
//        labelY_hyp = (int) -130;
        //g2d.drawString("rearRightSpeedTarget "+String.format("%.1f",rearRightSpeedTarget)+"; angleRearOuter "+String.format("%.1f",angleRearInner)+"; radius "+String.format("%.1f", hypotenuse), labelX_hyp, labelY_hyp);

        if (displayTroubleshootingValues && wheel_ID == 2) {
            g2d.setColor(Color.RED);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 25;
            g2d.drawString("Rear Right Wheel Red Line " + wheel_ID, xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString(" X: " + startX[deviceNumber] + " Y: " + (startY[deviceNumber]), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString(" EndX: " + endX[deviceNumber] + "  EndY: " + endY[deviceNumber], xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString(" SpeedTarget " + String.format("%.1f", rearRightSpeedTarget), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString(" Angle " + String.format("%.1f", angleRearInner) + "; radius " + String.format("%.1f", hypotenuse), xDisplay, yDisplay);
        }

        if (displayTroubleshootingValues && wheel_ID == 2) {
            g2d.setColor(Color.RED);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 25;
            g2d.drawString("RearLeft  speedRatioT[3] " + String.format("%.4f", speedRatioTarget[3]), xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 15;
            g2d.drawString("RearRight speedRatioT[4] " + String.format("%.4f", speedRatioTarget[4]), xDisplay, yDisplay);
        }

        if (displayTroubleshootingValues && wheel_ID == 3) {
            g2d.setColor(Color.RED);
            xDisplay = xDisplayBase + 10;
            yDisplay = yDisplayBase + 148;
            g2d.drawString("Press Keys(1-4) for additional info:", xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 25;
            g2d.drawString("  1 = Front Wheels", xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 25;
            g2d.drawString("  2 = Rear Wheels", xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 25;
            g2d.drawString("  3 = This screen", xDisplay, yDisplay);
            xDisplay = xDisplay + 0;
            yDisplay = yDisplay + 25;
            g2d.drawString("  4 = Misc", xDisplay, yDisplay);
        }

        //g2d.setColor(Color.ORANGE);
        //g2d.drawOval(200, 200, 20, 20);

        for (Wheel wheel : mWheels) {
            g2d.setColor(Color.YELLOW);
            //System.out.println("test condition - only some wheels need lines?  "+wheel.getWheelName()+" or "+mWheels[1].getWheelName());

            g2d.drawLine(
                    (int) wheel.getDrawLocation().x,
                    (int) wheel.getDrawLocation().y,
                    (int) relMousePos.x,
                    (int) relMousePos.y);

            wheel.draw(g2d);
        }

        g2d.setTransform(saveAT);

//        g2d.setColor(Color.BLACK);
//        g2d.drawLine( // line from center of rover to mouse
//                (int)mDrawLocation.x, (int)mDrawLocation.y,
//                (int) modifiedRelMousePos.x, (int) modifiedRelMousePos.y);
        //g2d.drawString("mousePos "+String.format("%.1f", mousePos.x)+" "+String.format("%.1f",  mousePos.y), (int) mousePos.x, (int) mousePos.y);

        g2d.fillOval(  // dot in center of truck
                (int) mDrawLocation.x - POINTER_RADIUS / 2,
                (int) mDrawLocation.y - POINTER_RADIUS / 2,
                POINTER_RADIUS, POINTER_RADIUS);
        g2d.setColor(Color.RED);
    }
}
