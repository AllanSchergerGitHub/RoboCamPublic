package GamePad;

public enum GamePadControllerActionEnum {
    STOPPED_STEER("Stopped (Stear)"),
    FRONT_STEER("Front Stear"),
    STRAIGHT_STEER("Straight (Stear)"),
    SPEED_SLOWER("Speed Slower"),
    SPEED_FASTER("Speed Faster"),
    ROTATE_FORWARD("Rotate Forward"),
    ROTATE_BACKWARD("Rotate Backward"),
    ROTATE_LEFT("Rotate Left"),
    ROTATE_RIGHT("Rotate Right"),
    VEHICLE_POINTER_HALT("Vehicle Pointer Halt"),
    VEHICLE_POINTER_LEFT_RIGHT("Vehicle Pointer Left/Right");

    private final String name;

    GamePadControllerActionEnum(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
