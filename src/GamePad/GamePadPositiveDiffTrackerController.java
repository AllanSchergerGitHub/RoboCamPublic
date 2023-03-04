package GamePad;

import com.studiohartman.jamepad.ControllerAxis;


/**
 * This class a special tracker controller
 * that track only the changes in positive direction.
 */
public class GamePadPositiveDiffTrackerController extends GamePadTrackerController {
    private Double mLastValueForSocket;

    public GamePadPositiveDiffTrackerController(
            GamePadControllerActionEnum idValue,
            Character subCommad,
            ControllerAxis jamepadAxis,
            double outMutliplier) {
        super(idValue, subCommad, jamepadAxis, outMutliplier);
    }

    private boolean mSocketHandlerRet = false; // for optimization

    /**
     * Sets the value of mValue to command of socket handler.
     *
     * @return true if the change value is beyond tolerance
     */
    @Override
    public boolean setSocketHandlerValue() {
        if (mLastValueForSocket == null ||
                mChangeTolerance < Math.abs(mValue - mLastValueForSocket)) {
            if (mLastValueForSocket == null ||
                    mChangeTolerance < mValue - mLastValueForSocket) {
                mSocketCommandHandler.setValue(
                        mValue - (mLastValueForSocket == null ? 0 : mLastValueForSocket)
                );
                mSocketHandlerRet = true;
            } else {
                mSocketHandlerRet = false;
            }
            mLastValueForSocket = mValue;
            return mSocketHandlerRet;
        }
        return false;
    }
}
