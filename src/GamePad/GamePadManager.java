package GamePad;

import RoboCam.Config;
import RoboCam.RoverFrontEnd;
import com.robocam.Socket.ComPipe;
import com.robocam.Socket.FloatSubCommand;
import com.studiohartman.jamepad.ControllerAxis;
import com.studiohartman.jamepad.ControllerButton;
import com.studiohartman.jamepad.ControllerIndex;
import com.studiohartman.jamepad.ControllerManager;

/**
 * A class to manage all the GamePad
 * controllers that are used in RoboCam
 */
public class GamePadManager {
    private final GamePadController[] mGamePadControllers;
    private final ControllerManager mJamePadContollerManager;
    private boolean mConnected = false;
    
    public GamePadManager() {
        mJamePadContollerManager = new ControllerManager();
                
        mGamePadControllers = new GamePadController[] {
            new GamePadButtonController(
                    GamePadControllerActionEnum.STOPPED_STEER,
                    FloatSubCommand.STEER_STOP,
                    ControllerButton.A
            ),
            new GamePadButtonController(
                    GamePadControllerActionEnum.FRONT_STEER,
                    FloatSubCommand.STEER_FRONT,
                    ControllerButton.X
            ),
            new GamePadButtonController(
                    GamePadControllerActionEnum.STRAIGHT_STEER,
                    FloatSubCommand.STEER_STRAIGHT,
                    ControllerButton.Y
            ),
            new GamePadButtonController(
                    GamePadControllerActionEnum.SPEED_SLOWER,
                    FloatSubCommand.SPEED_SLOWER,
                    ControllerButton.LEFTBUMPER
            ),
            new GamePadButtonController(
                    GamePadControllerActionEnum.ROTATE_FORWARD,
                    FloatSubCommand.ROTATE_FORWARD,
                    ControllerButton.DPAD_UP
            ),
            new GamePadButtonController(
                    GamePadControllerActionEnum.ROTATE_BACKWARD,
                    FloatSubCommand.ROTATE_BACKWARD,
                    ControllerButton.DPAD_DOWN
            ),
            new GamePadButtonController(
                    GamePadControllerActionEnum.ROTATE_LEFT,
                    FloatSubCommand.ROTATE_LEFT,
                    ControllerButton.DPAD_LEFT
            ),
            new GamePadButtonController(
                    GamePadControllerActionEnum.ROTATE_RIGHT,
                    FloatSubCommand.ROTATE_RIGHT,
                    ControllerButton.DPAD_RIGHT
            ),
            new GamePadButtonController(
                    GamePadControllerActionEnum.VEHICLE_POINTER_HALT,
                    FloatSubCommand.VEHICLE_POINTER_HALT,
                    ControllerButton.RIGHTSTICK
            ),
            new GamePadTrackerController(
                    GamePadControllerActionEnum.VEHICLE_POINTER_LEFT_RIGHT,
                    FloatSubCommand.VEHICLE_POINTER_TRACK,
                    ControllerAxis.RIGHTX,
                    0.5
            ),
            new GamePadPositiveDiffTrackerController(
                    GamePadControllerActionEnum.SPEED_FASTER,
                    FloatSubCommand.SPEED_FASTER,
                    ControllerAxis.TRIGGERLEFT,
                    1
            )
        };
    }
    
    public void loadFromConfig(Config config) {
        for (GamePadController gamePadController: mGamePadControllers) {
            switch (gamePadController.getId()) {
                case VEHICLE_POINTER_LEFT_RIGHT:
                    GamePadTrackerController controller = 
                            (GamePadTrackerController) gamePadController;
                    controller.setChangeTolerance(
                            config.getGamePadVehicleLeftRightTolerance());
                    controller.setOutMultiplier(
                            config.getGamePadVehicleLeftRightMultiplier());
                    break;
            }
        } 
    }
    
    public GamePadController[] getGamePadContollers() {
        return mGamePadControllers;
    }
    
    public boolean connectToControllers() {
        if (mConnected) return false;

        mJamePadContollerManager.initSDLGamepad();
                
        // Just pick the first jamepad controller
        ControllerIndex jamePadController = mJamePadContollerManager.getControllerIndex(0);
        
        // And attach it rover controllers
        for (GamePadController gamePadController: mGamePadControllers) {
            gamePadController.setJamepadController(jamePadController);
        }        
        return true;
    }
    
    public boolean connectToControllers(ComPipe comPipe) {
        if (mConnected) return false;

        connectToControllers();
        // Attach the com pipe
        for (GamePadController gamePadController: mGamePadControllers) {
            gamePadController.setComPipe(comPipe);
        }        
        return true;
    }   
}
