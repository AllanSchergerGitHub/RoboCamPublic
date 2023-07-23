package PhiDevice;

import com.phidget22.PhidgetException;
import com.phidget22.TemperatureSensorTemperatureChangeListener;
import java.util.ArrayList;

public class MotorTemperatureSensorList extends ArrayList<MotorTemperature> {

    public double getTemperatureAtIndex(int index, String wheelName) {
        if (this == null) return 0;
        try {
            return this.get(index).getController().getTemperature();
        } catch (Exception ex) {
            System.err.println("Error reading TemperatureSensor in MotorTemperatureSensorList.java on wheel: " + wheelName + ";  Index: " + index);
            return 0;
        }
    }
    
    public String getNames() {
        ArrayList<String> names = new ArrayList<>();
        for (MotorTemperature temperaturesensor : this) {
            try {
                names.add(temperaturesensor.getController().getChannelName());
            } catch (PhidgetException ex) {
                //Logger.getLogger(MotorPositionControllerList.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return names.toString();
    }
    
    public final void addTemperatureChangeListener(TemperatureSensorTemperatureChangeListener tl) {
        for (MotorTemperature temperaturesensor : this) {
            temperaturesensor.getController().addTemperatureChangeListener(tl);
        }
    }    
}