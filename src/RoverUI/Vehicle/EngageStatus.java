package RoverUI.Vehicle;

public enum EngageStatus {
    NONE(""),
    ENGAGED("Engaged"),
    DISENGAGED("disEngaged"),
    REENGAGING("reEngaging"),
    DISENGAGING("disEngaging"),
    ENGAGING("Engaging");
        
    String mName;
    
    EngageStatus(String name) {
        mName = name;
    }
    
    public String getName() {
        return mName;
    }
}
