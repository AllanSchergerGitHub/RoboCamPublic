package Utility;

public class MinMax {
    static final double LIMIT_FRACTION = 1 - 0.05; // 1 - 0.05 provides a buffer of safety so the rotation stops just short of the limit switch

    private double mMin = 0;
    private double mMax = 0;
    private boolean mMinSet = false;
    private boolean mMaxSet = false;
    private double mMidFraction = 0.42; // higher number means center (straight) is more to the left

    public MinMax() {}
    
    public double getMaxValue() {
        return mMax;
    }
    
    public double getMinValue() {
        return mMin;
    }
    
    public double getMidValue() {
        if (!mMaxSet || !mMinSet) return 0;
        return mMin + (mMax-mMin)*mMidFraction;
    }
    
    /*
    * once both MinMax ranges are are set it is ok to use higher velocity on steppers since we know they will stop appropriately.
    */
    public boolean bothMinMaxSet() {
        if( mMaxSet && mMinSet){
            return true;
        }
        return false;
    }
    
    public void setMidFraction(double value) {
        mMidFraction = value;
    }
    
    public double getTrimmedValue(double value) {
        if (mMaxSet && value > mMax) return mMax;
        if (mMinSet && value < mMin) return mMin;
        return value;
    }
    
    public void setValue(double value) {
        if (value < mMin) {
            mMin = value*LIMIT_FRACTION;
            mMinSet = true;
            System.out.println("limit set to: "+mMin);
        } else if (value > mMax) {
            mMax = value*LIMIT_FRACTION;
            mMaxSet = true;
            System.out.println("limit set to: "+mMax);
        }
    }
    
    public String toString() {
        return String.format("Min:%.2f, Max:%.2f", mMin, mMax);
    }
}
