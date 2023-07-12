package PhiDevice;

import java.util.ArrayList;

public class MotorTemperatureSensorList extends ArrayList<MotorTemperature> {

    public double getTemperatureAtIndex(int index) {
        if (this == null) return 0;
        try {
            return this.get(index).getTemperature();
        } catch (Exception ex) {
            return 0;
        }
    }
}