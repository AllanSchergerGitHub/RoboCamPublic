package RoverUI.Vehicle;

public enum SteeringMode {
    NONE("Stopped"),
    STOPPED("Stopped"),
    STRAIGHT("Straight"),
    TURN_AROUND("Turn Around"),
    SIDE_TO_SIDE("Side to Side"),
    PIVOT("Pivot"),
    FRONT_STEER("Front Steer"),
    MOUSE_FREE("Mouse Free");

    private final String mName;

    SteeringMode(String name) {
        mName = name;
    }

    @Override
    public String toString() {
        return mName;
    }

    public static SteeringMode getByName(String name) {
        //System.out.println("getByName from SteeringMode.java: "+name);
        for (SteeringMode steeringMode : SteeringMode.values()) {
            if (steeringMode.mName.equals(name)) return steeringMode;
        }
        return null;
    }
}
