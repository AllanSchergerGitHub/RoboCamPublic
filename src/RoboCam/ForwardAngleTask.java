package RoboCam;

public class ForwardAngleTask {
    public static final double TICK_PER_ANGLE = 1;
    
    private double mStep;
    private double mStart;
    private double mEnd;
    private boolean mActive = false;
    
    ForwardAngleTask(double step) {
        mStep = step;
    }
    
    boolean isActive() {
        return mActive;
    }

    void activate(double startAngle) {
        mActive = true;
        mStart = startAngle;
        mEnd = startAngle + mStep;
    }
    
    double getTarget() {
        return mEnd*TICK_PER_ANGLE;
    }
}
