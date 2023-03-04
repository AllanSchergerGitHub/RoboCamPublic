package GamePad;

import com.studiohartman.jamepad.ControllerAxis;
import com.studiohartman.jamepad.ControllerUnpluggedException;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

/**
 * This class wraps a tracker controller in GamePad
 */
public class GamePadTrackerController extends GamePadController {
    // Maximum number of past values to store
    protected final int MAX_VALUE_LENGTH = 3;

    // X or Y axis of the controller that needs to be tracked
    private final ControllerAxis mJamepadAxis;

    // Storage of last N values
    protected final ArrayList<Double> mValues = new ArrayList<>();

    // Holds active status
    private boolean mActive = false;

    // How much value change must be tolerated
    // before calling it a real value change to send command to socket
    protected double mChangeTolerance = .1;

    // Multiplier to be used to sendcommand to socket
    protected double mOutMultiplier = 1.0;

    public GamePadTrackerController(
            GamePadControllerActionEnum idValue, Character subCommad,
            ControllerAxis jamepadAxis,
            double outMutliplier) {
        super(idValue, subCommad);
        mJamepadAxis = jamepadAxis;
        mOutMultiplier = outMutliplier;
    }

    public void setOutMultiplier(double value) {
        try {
            sleep(500);
        } catch (InterruptedException ex) {
            Logger.getLogger(GamePadTrackerController.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println(String.format("Gamepad Controller - setOutMultiplier value=%f", value));
        mOutMultiplier = value;
    }

    public void setChangeTolerance(double value) {
        mChangeTolerance = value;
    }

    /**
     * Deactivate to not to track the controller
     */
    public void deactivate() {
        System.out.println(
                String.format(
                        "deactivate: %s is deactivated now.",
                        getFormattedName()
                )
        );
        mActive = false;
    }

    @Override
    public String getFormattedName() {
        return String.format("%s -[%s]", mId, mJamepadAxis.name());
    }

    /**
     * Add the value in the history and
     * returns the average of historical values
     *
     * @param value
     * @return
     */
    private double _appendValue(double value) {
        if (mValues.size() == MAX_VALUE_LENGTH) {
            mValues.remove(0);
        }
        mValues.add(value);
        return mOutMultiplier * mValues.stream().mapToDouble(x -> x).average().getAsDouble();
    }

    @Override
    public boolean canSendCommand() {
        if (!mActive) {
            if (mOutValue == null || mChangeTolerance < Math.abs(mOutValue - mValue)) {
                // System.out.println(String.format("mActive %s", mActive));
                mActive = true;
                System.out.println(
                        String.format(
                                "canSendCommand: %s is active now.",
                                getFormattedName()
                        )
                );
            }
        }
        return mActive;
    }

    /**
     * Sets the final value based on historical values.
     *
     * @param value
     * @param shouldSendCommand
     */
    @Override
    public void setValue(double value, boolean shouldSendCommand) {
        System.out.println(String.format("a=%f,s=%f", value, getCubicScaledDeadband(value)));
        value = getCubicScaledDeadband(value);
        double finalValue = _appendValue(value);
        super.setValue(finalValue, shouldSendCommand);
    }

    @Override
    public void updateValue() {
        try {
            setValue(mJamePadController.getAxisState(mJamepadAxis), false);
        } catch (ControllerUnpluggedException ex) {
        }
    }

    private double getCubicValue(double x, double weight) {
        return weight * x * x * x + (1.0 - weight) * x;
    }

    /**
     * http://www.mimirgames.com/articles/games/joystick-input-and-using-deadbands/
     *
     * @param x
     * @return
     */
    private double getCubicScaledDeadband(double x) {
        double deadbandCutoff = 0.1f;
        double weight = 0.2f;

        if (Math.abs(x) < deadbandCutoff) {
            return 0;
        } else {
            return (
                    getCubicValue(x, weight) -
                            (Math.abs(x) / x) * getCubicValue(deadbandCutoff, weight))
                    / (1.0 - getCubicValue(deadbandCutoff, weight));
        }
    }
}
