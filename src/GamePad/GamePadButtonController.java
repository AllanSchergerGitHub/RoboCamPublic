package GamePad;

import com.studiohartman.jamepad.ControllerButton;
import com.studiohartman.jamepad.ControllerUnpluggedException;

/**
 * This class wraps a button controller in GamePad
 */
public class GamePadButtonController extends GamePadController {
    
    private final ControllerButton mJamepadButton;
    
    public GamePadButtonController(
            GamePadControllerActionEnum idValue, Character subCommand,
            ControllerButton jamepadButton) {
        super(idValue, subCommand);
        mJamepadButton = jamepadButton;
    }

    /**
     * Gives descent name of the button controller
     * @return 
     */
    @Override
    public String getFormattedName() {
        return String.format("%s -[%s]", mId, mJamepadButton.name());
    }

    
    /**
     * Read the button press status and update mValue
     * and/or sends command to socket.
     */
    @Override
    public void updateValue() {
        try {
            if (mJamePadController.isButtonJustPressed(mJamepadButton)) {
                setValue(1, false);
            } else {
                setValue(0, false);
            }
        } catch (ControllerUnpluggedException ex) {
            //Logger.getLogger(RoverButtonController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
