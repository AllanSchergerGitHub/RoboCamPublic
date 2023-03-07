package GamePad;

import com.robocam.Socket.FloatSubCommand;

public class GamePadControllerValue {
    public GamePadControllerActionEnum actionType;
    public FloatSubCommand command;

    public GamePadControllerValue(GamePadControllerActionEnum actionType, FloatSubCommand command) {
        this.actionType = actionType;
        this.command = command;
    }
}
