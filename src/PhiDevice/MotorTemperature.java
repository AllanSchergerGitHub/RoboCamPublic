package PhiDevice;

import com.phidget22.TemperatureSensor;
import com.phidget22.TemperatureSensorTemperatureChangeEvent;
import com.phidget22.TemperatureSensorTemperatureChangeListener;

public class MotorTemperature {
    private TemperatureSensor mMotorTemperatureReader;
    private DeviceChannel mDeviceChannel;
    
    
    public static interface MotorTemperatureListener {
        public void onUpdate(TemperatureSensor positioner,
                             TemperatureSensorTemperatureChangeEvent tempSensorUpdateEvent);
    }
     
    public MotorTemperature(TemperatureSensor controller, DeviceChannel deviceChannel){
        mMotorTemperatureReader = controller;
        mDeviceChannel = deviceChannel;
    }
    
    private void fireTempSensorListener(MotorTemperatureListener listener,
                                        TemperatureSensorTemperatureChangeEvent tempSensorUpdateEvent) {
        listener.onUpdate(mMotorTemperatureReader, tempSensorUpdateEvent);
    }
    
  // Add a temperature change listener
    public void addMotorTemperatureListener (MotorTemperatureListener listener) {        
        mMotorTemperatureReader.addTemperatureChangeListener(new TemperatureSensorTemperatureChangeListener() {
                
            @Override            
            public void onTemperatureChange(TemperatureSensorTemperatureChangeEvent tempSensorUpdateEvent) {
                    fireTempSensorListener(listener, tempSensorUpdateEvent);
                    //System.out.println("Temperature changed to " + e.getTemperature());
            }
        });   
    }
    
}
