/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package RoboCam;

import PhiDevice.Electrical_Etc.Potentiameters;
import com.phidget22.MotorPositionController;
import com.phidget22.PhidgetException;

import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

/**
 * @author pc
 */
public class WheelDeviceSubClassAutoTrim {
    private String wheelName = "initalizedValue"; // FrontLeft
    private int multiplierForThisWheel = 0; // -1 for FrontRight and 1 for FrontLeft (This is set correctly later so don't change it here - this is just initializing the variable)
    private MotorPositionController bldc1 = null;
    private double ConnectionAttemptCount = 0;
    private double mPotTargetValue = 0;
    private double voltageTrim = 0;
    private double mPotValue = 0;
    private Potentiameters mPotentiameters = null;

    public void setValues(MotorPositionController bldc1, String wheelName) {
        this.bldc1 = bldc1;
        this.wheelName = wheelName;
    }

    public void pushPotDown(Potentiameters pot) {
        this.mPotentiameters = pot;
    }

    private double getPotValue() {
        //System.err.println("mPotentiameters.toString() "+mPotentiameters.toString());
        double potValue = mPotentiameters.getPotentiameterValue("WheelDeviceSubClassAutoTrim.java");
        return potValue;
    }

    public WheelDeviceSubClassAutoTrim() {
    }

    int selectThisElement = 0;

    public double runWheelDeviceSubClassAutoTrim(double PotTargetValue) {
        this.mPotTargetValue = PotTargetValue;
        double trimSettingFromAutoTrim = 0;

        double testPotValue = 0;
        double stepperPositionTarget = 0;
        double potHighTarget = mPotTargetValue + .0005;
        double potLowTarget = mPotTargetValue - .0005;
        double currentStepperPosition = 0;
        double movingCount = 0;

        if (wheelName.equals("FrontLeft")) {
            multiplierForThisWheel = 1;
        }
        if (wheelName.equals("FrontRight")) {
            multiplierForThisWheel = -1;
        }

        int b = 0;
        //System.out.println("Loop Number:"+b+" based on ConnectionAttemptCount: "+ConnectionAttemptCount);

        boolean runWhileTrue = true;

        try {
            if (bldc1 != null) {
                if (bldc1.getAttached()) {
                    currentStepperPosition = bldc1.getPosition(); // this is no longer a stepper - it is a bldc motor
                    stepperPositionTarget = currentStepperPosition;
                    while (runWhileTrue) {
                        if (bldc1.getAttached()) {
                            try {
                                if (mPotentiameters != null) {
                                    testPotValue = getPotValue();
                                }
                                if (testPotValue < PotTargetValue * 1.10 && testPotValue > PotTargetValue * 0.90) { // we want the testing to be within 10%+/-
                                    if (testPotValue < potLowTarget) {
                                        stepperPositionTarget = stepperPositionTarget - (multiplierForThisWheel * 10);
                                    }
                                    if (testPotValue > potHighTarget) {
                                        stepperPositionTarget = stepperPositionTarget + (multiplierForThisWheel * 10);
                                    }

                                    trimSettingFromAutoTrim = stepperPositionTarget;
                                    if (stepperPositionTarget < 1800 && stepperPositionTarget > -1800) {
                                        bldc1.setTargetPosition(stepperPositionTarget);
                                    } else {
                                        System.err.println("BLDC proposed target position is outside of an acceptable range at: " + stepperPositionTarget);
                                    }
                                    try {
                                        sleep(400);
                                    } catch (InterruptedException ex) {
                                        Logger.getLogger(WheelDeviceSubClassAutoTrim.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                    currentStepperPosition = bldc1.getPosition();
                                    if (mPotentiameters != null) {
                                        testPotValue = getPotValue();
                                    }

                                    System.err.println("Loop: " + b +
                                            " wheelName " + wheelName +
                                            " selectThisElement: " + selectThisElement +
                                            " bldc1.getDutyCycle: " + bldc1.getDutyCycle() +
                                            " engaged? " + bldc1.getEngaged() +
                                            " movingCount " + movingCount +
                                            " getMaxVelocity " + bldc1.getMaxVelocityLimit() +
                                            " PotValue: " + String.format("%02.4f", testPotValue) +
                                            " potLowTar " + String.format("%02.4f", potLowTarget) +
                                            " potHighTar " + String.format("%02.4f", potHighTarget) +
                                            " Position:" + currentStepperPosition +
                                            " stepperPositionTarget: " + stepperPositionTarget +
                                            " bldc1.getTargetPos: " + bldc1.getTargetPosition()
                                    );

                                } else {
                                    System.out.println("Doublecheck the if/then statemnt - for some reason the pot is outside a +/-10% range.");
                                    System.out.println("The 'startingTargetPotPositionFront...[Left/Right]' variable in 'RoverFrontEnd.java' may not be set correctly.");
                                }
                            } catch (PhidgetException ex) {
                                Logger.getLogger(WheelDeviceSubClassAutoTrim.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        if (testPotValue > potLowTarget && testPotValue < potHighTarget) {
                            runWhileTrue = false;
                        }
                    }
                }
            }
        } catch (PhidgetException ex) {
            Logger.getLogger(WheelDeviceSubClassAutoTrim.class.getName()).log(Level.SEVERE, null, ex);
        }
        return trimSettingFromAutoTrim * multiplierForThisWheel;
    }
}
    
    
    
