package GamePad;

import com.robocam.Socket.ComPipe;
import com.robocam.Socket.FloatSubCommand;
import com.studiohartman.jamepad.ControllerIndex;
import java.util.ArrayList;

/**
 * This is base class to catch
 * different input signals from GamePad
 * buttons or trackers
 * A child class should be implemented for
 * specific controller for the GamePad.
 */
public abstract class GamePadController {
    // Name of the controller
    protected final GamePadControllerActionEnum mId;
    
    // The value of the controller
    protected double mValue;
    
    // The value that is sent to socket
    // may not be same as mValue
    protected Double mOutValue;
    
    // Handler to send command on socket
    protected FloatSubCommand mSocketCommandHandler;
    
    // Index of the controller within the Gamepad
    protected ControllerIndex mJamePadController;
    
    // Socket communicator
    private ComPipe mComPipe;
    
    public interface ValueListener {
        void valueChange(double value);
    }
    
    private ArrayList<ValueListener> mValueListeners;
    
    public GamePadController(GamePadControllerActionEnum idValue, Character subCommand) {
        mId = idValue;
        mValue = 0;
        mSocketCommandHandler = new FloatSubCommand();
        mSocketCommandHandler.setSubCommand(subCommand);
    }
    
    /**
     * Add value listener that will be fired when
     * the fireEventListeners will be called
     * @param listener 
     */
    public void addValueListener(ValueListener listener) {
        if (mValueListeners == null) {
            mValueListeners = new ArrayList<>();
        }
        mValueListeners.add(listener);
    }
    
    /**
     * Fires all the value listeners with the given value
     * @param value 
     */
    public void fireEventListeners(double value) {
        if (mValueListeners == null) return;
        for(ValueListener listener: mValueListeners) {
            listener.valueChange(value);
        }
    }
    
    /**
     * Sets the controller index for the specific Gamepad button/tracker
     * @param jamePadController 
     */
    public void setJamepadController(ControllerIndex jamePadController) {
        mJamePadController = jamePadController;
    }
    
    public void setComPipe(ComPipe comPipe) {
        mComPipe = comPipe;
    }

    public String getName() {
        return mId.getName();
    }
    
    public GamePadControllerActionEnum getId() {
        return mId;
    }
    
    public FloatSubCommand getCommand() {
        return mSocketCommandHandler;
    }
    
    public double getValue() {
        return mValue;
    }
    
    /**
     * Checks whether the controller can
     * send command via communication pipe
     * @return true
     */
    public boolean canSendCommand() {
        return mComPipe != null;
    }
    
    /**
     * Sets the value of mValue to command of socket handler.
     * @return true if the value is set
     */
    public boolean setSocketHandlerValue() {
        return mSocketCommandHandler.setValue(mValue);
    } 
    
    /***
     * Checks whether the controller can send command
     * and if true then it build socket command  and sends
     * via communication pipe.
     * @return true if command is ready
     */
    public boolean sendCommand() {
        if (!canSendCommand()) {
            return false;
        }
        if (setSocketHandlerValue()) {
            mOutValue = mValue;
            if (mComPipe != null) {
                // System.out.println(String.format("send socket value=%f", mSocketCommandHandler.getValue()));
                String sendThis = mSocketCommandHandler.buildCommand();
                mComPipe.putOut(sendThis);
            }
            return true;
        }
        return false;
    }
    
    /**
     * Sets the value of mValue and fire listeners.
     * And if shouldSendCommand s true then
     * command will be sent to socket pipe.
     * @param value
     * @param shouldSendCommand 
     */
    public void setValue(double value, boolean shouldSendCommand) {
        if (value != mValue) {
            fireEventListeners(value);
        }
        mValue = value;
        if (shouldSendCommand) {
            sendCommand();
        }
        //System.out.println(mSockerCommandHandler.buildCommand());
    }
    
    /**
     * This is supposed to give more descent name of the controller
     * @return 
     */
    public abstract String getFormattedName();
    
    /**
     * This is supposed to update the mValue
     */
    public abstract void updateValue();
}
