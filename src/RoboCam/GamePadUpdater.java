package RoboCam;

import GamePad.GamePadController;
import GamePad.GamePadControllerValue;
import GamePad.GamePadManager;
import RoverUI.TruckSteerPanel;
import RoverUI.Vehicle.SteeringMode;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.SwingWorker;

/**
 * This class will periodically read the GamePad controllers
 * and update the user interface objects and make the Rover or UI
 * run.
*/
public class GamePadUpdater extends  SwingWorker<Void, GamePadControllerValue> {

    private final GamePadManager mGamePadManager;
    private final TruckSteerPanel mTruckSteerPanel;
    private final JLabel mLblGamePadStatusValue;
    private boolean mActive = false;
    
    public GamePadUpdater(
                GamePadManager gamePadManager,
                TruckSteerPanel truckSteerPanel,
                JLabel gamePadStatusValueLabel) {
        mGamePadManager = gamePadManager;
        mTruckSteerPanel = truckSteerPanel;
        mLblGamePadStatusValue = gamePadStatusValueLabel;
    }
    
    public void setActive(boolean active) {
        mActive = active;
    }
    
    @Override
        protected Void doInBackground() throws Exception {
            while(true) {
                if (mActive) {
                    for(GamePadController controller: mGamePadManager.getGamePadContollers()) {
                         // For non-ps4 testing
                         /*
                         if (controller.getId() == GamePadControllerActionEnum.ROTATE_RIGHT) {
                             if (Math.random() > 0.5) {
                                 controller.setValue(-1 + 2*Math.random(), true);
                             }
                         }
                         */
                         controller.updateValue();
                         if (controller.setSocketHandlerValue()) {
                             publish(
                                 new GamePadControllerValue(
                                     controller.getId(), 
                                     controller.getCommand().copy()
                                 )
                             );
                         }
                    }
                } else {
                    publish((GamePadControllerValue) null);
                }
                try {
                    Thread.sleep(100);
                    //System.out.println("sleep_100");
                } catch (InterruptedException ex) {
                    break;
                }
            }
            return null;
        }

        @Override
        protected void process(List<GamePadControllerValue> chunks) {
            mLblGamePadStatusValue.setText("");
            for(GamePadControllerValue controllerValue: chunks) {
                if (controllerValue == null) continue;
                switch (controllerValue.actionType) {
                    case FRONT_STEER:
                        mTruckSteerPanel.setSteeringMode(SteeringMode.FRONT_STEER.toString());
                        break;
                    case ROTATE_LEFT:
                        if (controllerValue.command.getValue() > 0) {
                            mTruckSteerPanel.clickRotateLeftButton();
                        }
                        break;
                    case ROTATE_RIGHT:
                        if (controllerValue.command.getValue() > 0) {
                            mTruckSteerPanel.clickRotateRightButton();
                        }
                        break;
                    case ROTATE_BACKWARD:
                        if (controllerValue.command.getValue() > 0) {
                            mTruckSteerPanel.clickRotateBackwardButton();
                        }
                        break;
                    case ROTATE_FORWARD:
                        if (controllerValue.command.getValue() > 0) {
                            mTruckSteerPanel.clickRotateForwardButton();
                        }
                        break;
                    case SPEED_FASTER:
                        if (controllerValue.command.getValue() > 0) {
                            mTruckSteerPanel.clickSpeedFasterButton();
                        }
                        break;
                    case SPEED_SLOWER:
                        if (controllerValue.command.getValue() > 0) {                            
                            mTruckSteerPanel.clickSpeedSlowerButton();
                        }
                        break;
                    case STRAIGHT_STEER:
                        mTruckSteerPanel.setSteeringMode(SteeringMode.STRAIGHT.toString());
                        break;
                    case STOPPED_STEER:
                        mTruckSteerPanel.setSteeringMode(SteeringMode.NONE.toString());
                        break;
                    case VEHICLE_POINTER_LEFT_RIGHT:
                        mTruckSteerPanel
                            .getGroundPanel()
                            .increaseMousePosXFraction(
                                controllerValue.command.getValue()
                            );
                        break;
                }
                mLblGamePadStatusValue.setText(controllerValue.actionType.getName());
            }
        }
}
