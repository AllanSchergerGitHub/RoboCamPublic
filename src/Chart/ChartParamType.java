package Chart;

public enum ChartParamType {
    VELOCITY("velocity"),
    ANGLE("steer angle"),
    BLDC_1_POSITION("BLDC Motor 1 Position"),
    BLDC_2_POSITION("BLDC Motor 2 Position"),
    BLDC_1_DUTY_CYCLE("BLDC Motor 1 DutyCycle"),
    BLDC_2_DUTY_CYCLE("BLDC Motor 2 DutyCycle"),
    BLDC_TEMPERATURE("BLDC Motor Temperature")

    ;

    private String mName;

    ChartParamType(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }
}