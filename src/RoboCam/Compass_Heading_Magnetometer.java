/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package RoboCam;

import com.phidget22.PhidgetException;
import com.phidget22.Spatial;
import com.phidget22.SpatialSpatialDataEvent;
import com.phidget22.SpatialSpatialDataListener;

import java.util.ArrayList;

class Compass_Heading_Magnetometer {
    public static void main(String[] args) {
        Magna workWithThis = new Magna();
        //workWithThis.calculateCompassBearing(new double[]{1, 1, 1}, new double[]{2, 3, 4});
        //System.out.println(workWithThis.getBearingDegree());
        workWithThis.dostuff();
    }
}

class Magna {
    //This finds a magnetic north bearing, correcting for board tilt and roll as measured by the accelerometer
    //This doesn't account for dynamic acceleration - ie accelerations other than gravity will throw off the calculation
    //Calculations based on AN4248

    private final double[] mLastAngles = {0, 0, 0};

    private static final int FILTER_MAX_SIZE = 3;
    private final ArrayList<double[]> mCompassBearingFilter = new ArrayList<>();

    private double mCompasBearingDeg = 0;

    /**
     * Ref: // https://www.phidgets.com/docs/Magnetometer_Primer#Calculating_Heading_from_Magnetometer_Data
     *
     * @param gravity
     * @param magField
     */
    public void calculateCompassBearing(double[] gravity, double[] magField) {
        //Roll Angle - about axis 0
        //  tan(roll angle) = gy/gz
        //  Use Atan2 so we have an output os (-180 - 180) degrees
        double rollAngle = Math.atan2(gravity[1], gravity[2]);

        //Pitch Angle - about axis 1
        //  tan(pitch angle) = -gx / (gy * sin(roll angle) * gz * cos(roll angle))
        //  Pitch angle range is (-90 - 90) degrees
        double pitchAngle = Math.atan(
                -gravity[0] / (gravity[1] * Math.sin(rollAngle) + gravity[2] * Math.cos(rollAngle)));

        //Yaw Angle - about axis 2
        //  tan(yaw angle) = (mz * sin(roll) – my * cos(roll)) /
        //                   (mx * cos(pitch) + my * sin(pitch) * sin(roll) + mz * sin(pitch) * cos(roll))
        //  Use Atan2 to get our range in (-180 - 180)
        //
        //  Yaw angle == 0 degrees when axis 0 is pointing at magnetic north
        double yawAngle = Math.atan2(
                magField[2] * Math.sin(rollAngle) - magField[1] * Math.cos(rollAngle),
                magField[0] * Math.cos(pitchAngle)
                        + magField[1] * Math.sin(pitchAngle) * Math.sin(rollAngle)
                        + magField[2] * Math.sin(pitchAngle) * Math.cos(rollAngle));

        double[] angles = {rollAngle, pitchAngle, yawAngle};

        //we low-pass filter the angle data so that it looks nicer on-screen
        try {
            //make sure the filter buffer doesn't have values passing the -180<->180 mark
            //Only for Roll and Yaw - Pitch will never have a sudden switch like that
            for (int i = 0; i < 3; i += 2) {
                if (Math.abs(angles[i] - mLastAngles[i]) > 3) {
                    for (double[] value : mCompassBearingFilter) {
                        if (angles[i] > mLastAngles[i]) {
                            value[i] += 2 * Math.PI;
                        } else {
                            value[i] -= 2 * Math.PI;
                        }
                    }
                }
            }

            // Store the current angles as last angles
            System.arraycopy(angles, 0, mLastAngles, 0, 3);

            //Store current values in filter/history
            mCompassBearingFilter.add(angles.clone());

            //Truncate is size exceeded
            if (mCompassBearingFilter.size() > FILTER_MAX_SIZE) {
                mCompassBearingFilter.remove(0);
            }

            rollAngle = pitchAngle = yawAngle = 0;
            for (double[] filterAngles : mCompassBearingFilter) {
                rollAngle += filterAngles[0];
                pitchAngle += filterAngles[1];
                yawAngle += filterAngles[2];
            }
            rollAngle = rollAngle / mCompassBearingFilter.size();
            pitchAngle = pitchAngle / mCompassBearingFilter.size();
            yawAngle = yawAngle / mCompassBearingFilter.size();
            mCompasBearingDeg = yawAngle * (180 / Math.PI);
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    public double getBearingDegree() {
        return mCompasBearingDeg;
    }

    public void dostuff() {
        try {
            Spatial ch = new Spatial();

            // Register for event before calling open
            ch.addSpatialDataListener(new SpatialSpatialDataListener() {
                public void onSpatialData(SpatialSpatialDataEvent e) {
                    // Access event source with getSource()
                    Spatial ch = (Spatial) e.getSource();

                    // Access event data via the Event object
                    double[] acceleration = e.getAcceleration();
                    double[] angularRate = e.getAngularRate();
                    double[] magneticField = e.getMagneticField();
                    double timestamp = e.getTimestamp();

                    calculateCompassBearing(acceleration, magneticField);
                    System.out.println(String.format("Bearing Angle: %2.f°", getBearingDegree()));

                    // System.out.println(magneticField[0]+" "+magneticField[1]+" "+magneticField[2]+" /n ");
                    // Events can also be printed
                    // System.out.println(e.toString());
                }
            });

            ch.open();
            while (true) {
                // Do work, wait for events, etc.
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                }
            }
        } catch (PhidgetException ex) {
            System.out.println("Failure: " + ex);
        }
    }

    ;

}

    

