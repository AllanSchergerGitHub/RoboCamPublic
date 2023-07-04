package PhiDevice;

import com.phidget22.MotorPositionControllerPositionChangeListener;
import com.phidget22.PhidgetException;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author sujoy
 */
public class MotorPositionControllerList extends ArrayList<MotorPositioner> {

    double mWheelTargetPosition = 0; // applies to both motor 1 and 2 (0 and 1?) This is a mWheelTargetPosition
    double mTargetPosition0 = 0; // use this when we set a diff target for each motor
    double mTargetPosition1 = 0; // use this when we set a diff target for each motor

    @Override
    public MotorPositioner remove(int index) {
        System.err.println("---------????????????????????????removing a motor?");
        MotorPositioner ms = super.remove(index);
        ms.setEngaged(false);
        return ms;
    }


    public String getNames() {
        ArrayList<String> names = new ArrayList<>();
        for (MotorPositioner positioner : this) {
            try {
                names.add(positioner.getController().getChannelName());
            } catch (PhidgetException ex) {
                //Logger.getLogger(MotorPositionControllerList.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return names.toString();
    }

    public boolean getEngaged() {
        int index = 0;
        for (MotorPositioner positioner : this) {
            try {
                if (!positioner.getController().getEngaged()) return false;
            } catch (PhidgetException ex) {
                System.err.println("trying 'getEngaged'; there may be a problem with the physical device - check the connections and fuses " + this.getNames() + get(index).getDeviceChannel());
                //Logger.getLogger(MotorPositionControllerList.class.getName()).log(Level.SEVERE, null, ex);
            }
            index++;
        }
        return (size() > 0);
    }

    public void setEngaged(boolean value) {
        for (MotorPositioner positioner : this) {
            positioner.setEngaged(value);
        }
    }

    public void setDeadBand(double deadband, String wheelName) {
        for (MotorPositioner positioner : this) {
            try {
                positioner.getController().setDeadBand(deadband);
            } catch (PhidgetException ex) {
                System.err.println("'Device not attached' error in " + wheelName);
                Logger.getLogger(MotorPositionControllerList.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void setAcceleration(double d, String wheelName) {
        for (MotorPositioner positioner : this) {
            try {
                //System.out.println(wheelName + " Acceleration set to: "+d);
                positioner.getController().setAcceleration(d);
//                        if(wheelName.equals("FrontRight")){
//                            // getPositionAtIndex <- when reading actuals some are neg and some are positive so we had to read each seperately
//                            // however, when setting acceleration (and a few other things) we were able to do it for both motors at once.
//                            // we need to setengaged for each motor individually as well. perhaps see the multiplier index? list? positionMults
//                            System.err.println("_accleration_ "+wheelName+ " "+positioner.getController().getHubPort());
//                        }

            } catch (PhidgetException ex) {
                Logger.getLogger(MotorPositionControllerList.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public String getTargetPositionLimits() {
        StringBuilder res = new StringBuilder();
        for (MotorPositioner positioner : this) {
            if (res.length() > 0) {
                res.append("<br>");
            }
            res.append(positioner.getDeviceChannel().getName() + "'s limits: ");
            res.append(positioner.getTargetPositionLimit());
        }
        return res.toString();
    }

    public void copyTargetPositionAsLimit() {
        for (MotorPositioner positioner : this) {
            positioner.copyTargetPositionAsLimit();
        }
    }

//    public void setTargetPosition(double d) {
//        for(MotorPositioner positioner: this) {
//            try {
//                positioner.setTargetPosition(d);
//            } catch (PhidgetException ex) {
//                //Logger.getLogger(MotorPositionControllerList.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//    }

    /**
     * this is probably conflicting with the values being set to both wheels in the other method 'setTargetPosition'.
     *
     * @param MotorNumber
     * @param targetPosition
     * @param positionMults
     */
    public void setTargetPositionOneMotor(int MotorNumber, double targetPosition, PositionList positionMults) {
        System.err.println("this is probably conflicting with the values being set to both wheels in the other method 'setTargetPosition'.");
        if (MotorNumber == 0) {
            mTargetPosition0 = targetPosition;
        }
        if (MotorNumber == 1) {
            mTargetPosition1 = targetPosition;
        }
        if (MotorNumber <= size()) {
            double posit = positionMults.get(MotorNumber);
            //System.err.println(MotorPositionControllerList.class.getName()+" '_posit_ ' "+posit+" targetPosition: "+targetPosition);
            get(MotorNumber).setTargetPosition(posit * targetPosition);
        }
    }

    /**
     * this is the value that was used to set the target position for one wheel - but it is not pulled from the device itself
     *
     * @param MotorNumber
     * @return
     */
    public double getTargetPositionOneMotor(int MotorNumber) {
        double returnThis = 0;
        if (MotorNumber == 0) {
            returnThis = mTargetPosition0;
        }
        if (MotorNumber == 1) {
            returnThis = mTargetPosition1;
        }
        return returnThis;
    }

    public void setTargetPosition(double targetPosition, PositionList positionMults) {
        mWheelTargetPosition = targetPosition;
        for (int i = 0; i < positionMults.size(); i++) {
            if (i >= size()) break;
            double posit = positionMults.get(i);
            //System.err.println(MotorPositionControllerList.class.getName()+" '_posit_ ' "+posit+" targetPosition: "+targetPosition);
            get(i).setTargetPosition(posit * mWheelTargetPosition);
        }
    }

    /**
     * this is the value that was used to set the target position for both wheels at once - but it is not pulled from the device itself
     *
     * @return
     */
    public double getWheelTargetPosition() {
        return mWheelTargetPosition;
    }

    public void setVelocityLimitWithIndex(ArrayList<Double> Velocities, String wheelName) {
        for (int i = 0; i < Velocities.size(); i++) {
            if (i >= size()) break;
            try {
                if (get(i).getController().getAttached()) {
                    try {
                        //System.err.println(getNames().toString()+" Velocities "+Velocities.get(i));
                        get(i).getController().setVelocityLimit(Velocities.get(i));
                    } catch (PhidgetException ex) {
                        try {
                            System.err.println("setVelocityLimitWithIndex error in MotionPositionControllerList.java " + wheelName + " " +
                                    get(i).getController().getDeviceName() + " Velocities.get(i) " + Velocities.get(i));
                        } catch (PhidgetException ex1) {
                            System.err.println("during 'catch' of one error another error occured");
                            //Logger.getLogger(MotorPositionControllerList.class.getName()).log(Level.SEVERE, null, ex1);
                        }
                        //Logger.getLogger(MotorPositionControllerList.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } catch (PhidgetException ex) {
                System.err.println("tried to set velocity limit; device was not attached - keep going");
                //Logger.getLogger(MotorPositionControllerList.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void setVelocityLimit(double d) {
        for (MotorPositioner positioner : this) {
            try {
                positioner.getController().setVelocityLimit(d);
            } catch (PhidgetException ex) {
                Logger.getLogger(MotorPositionControllerList.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public double getDutyCycle() {
        double dutyCycle = 0;
        for (MotorPositioner positioner : this) {
            try {
                dutyCycle += positioner.getController().getDutyCycle();
            } catch (PhidgetException ex) {
                System.err.println("can't get duty cycle but moving on anyway");//Logger.getLogger(WheelDevice.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (size() == 0) return 0;
        return dutyCycle / size();
    }

    public double getPosition() {
        double position = 0;
        for (MotorPositioner positioner : this) {
            try {
                position += positioner.getController().getPosition();
            } catch (PhidgetException ex) {
                System.err.println("can't get position but moving on anyway");//Logger.getLogger(WheelDevice.class.getName()).log(Level.SEVERE, null, ex);

            }
        }
        if (size() == 0) return 0;
        return position / size();
    }

    public double getkPAtIndex(int index) throws PhidgetException {
        if (index >= size()) return 0;
        if (this.get(index).getController() == null) {
            return 0;
        }
        //double uu = 
        //this.get(index).getController().setDataInterval(100);

        double x = this.get(index).getController().getKp();
        //System.err.println("uu = "+uu);
        return x;
    }

    /**
     * This goes down to the physical device to get a dutycycle reading from the motor.
     * @param index
     * @return
     * @throws PhidgetException 
     */
    public double getDutyCycleAtIndex(int index) throws PhidgetException {
        if (index >= size()) return 0;
        if (this.get(index).getController() == null) {
            return 0;
        }
        return this.get(index).getController().getDutyCycle();
    }

    public double getPositionAtIndex(int index, String wheelName) {
        if (index >= size()) return 0;
        double returnValue = 0;
        try {
            returnValue = this.get(index).getController().getPosition();
        } catch (PhidgetException ex) {
            Logger.getLogger(MotorPositionControllerList.class.getName()).log(Level.SEVERE, null, ex);
        }
        //if(wheelName.equals("FrontLeft")){
        //System.err.println(wheelName + " getPositionAtIndex "+this.getNames()+get(index).getDeviceChannel()+" Actual Position: "+returnValue+" <- this is where the read position happens - size: "+size());
        //}
        return returnValue;
    }

    public double getDeadBand_AtIndex(int index, String wheelName) {
        if (index >= size()) return 0;
        double mDeadBand = 0;
        try {
            mDeadBand = this.get(index).getController().getDeadBand();
        } catch (PhidgetException ex) {
            Logger.getLogger(MotorPositionControllerList.class.getName()).log(Level.SEVERE, null, ex);
        }
        //if(wheelName.equals("FrontLeft")){
        //System.err.println(wheelName + " getPositionAtIndex "+this.getNames()+get(index).getDeviceChannel()+" Actual Position: "+returnValue+" <- this is where the read position happens - size: "+size());
        //}
        return mDeadBand;
    }

    public final void addPositionChangeListener(MotorPositionControllerPositionChangeListener ml) {
        for (MotorPositioner positioner : this) {
            positioner.getController().addPositionChangeListener(ml);
        }
    }

    public final void addDutyCycleUpdateListener(MotorPositioner.DutyCycleListener ml) {
        for (MotorPositioner positioner : this) {
            //System.err.println("this.getNames() "+this.getNames()+get(0).getDeviceChannel());
            positioner.addDutyCycleListener(ml);
        }
    }
}
