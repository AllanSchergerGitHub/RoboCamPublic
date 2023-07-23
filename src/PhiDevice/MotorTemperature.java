package PhiDevice;

import com.phidget22.TemperatureSensor;
import com.phidget22.TemperatureSensorTemperatureChangeEvent;
import com.phidget22.TemperatureSensorTemperatureChangeListener;

public class MotorTemperature {
    private TemperatureSensor mMotorTemperatureReader;
    private DeviceChannel mDeviceChannel;
    private double currentTemperature;
    
    
    public static interface MotorTemperatureListener {
        public void onUpdate(TemperatureSensor positioner,
                             TemperatureSensorTemperatureChangeEvent tempSensorUpdateEvent);
    }
     
    public MotorTemperature(TemperatureSensor controller, DeviceChannel deviceChannel){
        mMotorTemperatureReader = controller;
        mDeviceChannel = deviceChannel;
        System.out.println("Added a MotorTemperature Object");
    }
    
    public TemperatureSensor getController() {
        return mMotorTemperatureReader;
    }
    
    // This method returns the current temperature of the motor
    public double getTemperature() {
        System.err.println("Reporting the temperature: " + currentTemperature);
        return currentTemperature;
    }
    
    private void fireTempSensorListener(MotorTemperatureListener listener,
                                        TemperatureSensorTemperatureChangeEvent tempSensorUpdateEvent) {
        listener.onUpdate(mMotorTemperatureReader, tempSensorUpdateEvent);
        currentTemperature = tempSensorUpdateEvent.getTemperature(); // Update the current temperature
        System.err.println("Checking the temperature: " + currentTemperature);
    }
    
  // Add a temperature change listener
    public void addMotorTemperatureListener (MotorTemperatureListener listener) {        
        mMotorTemperatureReader.addTemperatureChangeListener(new TemperatureSensorTemperatureChangeListener() {
                
            @Override            
            public void onTemperatureChange(TemperatureSensorTemperatureChangeEvent tempSensorUpdateEvent) {
                    fireTempSensorListener(listener, tempSensorUpdateEvent);
                    System.out.println("Action... Temperature changed to " + tempSensorUpdateEvent.getTemperature());
            }
        });   
    }
    
}
