package PhiDevice;

import com.phidget22.MotorPositionController;
import com.phidget22.TemperatureSensor;

import java.util.ArrayList;

/**
 * @author sujoy
 */
public class DeviceChannelList extends ArrayList<DeviceChannel> {
    public String getNames() {
        ArrayList<String> names = new ArrayList<>();
        this.forEach((channel) -> {
            names.add(channel.getName());
        });
        return names.toString();
    }

    public boolean isOpen() {
        for (DeviceChannel channel : this) {
            if (!channel.isOpen()) return false;
        }
        return (size() > 0);
    }

    public void open() {
        for (DeviceChannel channel : this) {
            if (!channel.isOpen()) channel.open();
        }
    }

    public void close() {
        for (DeviceChannel channel : this) {
            channel.close();
        }
    }

    public void addDeviceChannelChangeListener(DeviceChannel.ChannelListener listener) {
        for (DeviceChannel channel : this) {
            channel.addChannelListener(listener);
        }
    }

    public void setSequentialChannelLabel(String prefix) {
        int index = 0;
        for (DeviceChannel channel : this) {
            channel.setLabel(String.format("%s%d", prefix, index));
            //System.out.println(String.format("setSequentialChannelLabel in DeviceChannelList.java.  " + " %s%d", prefix, index));
            index++;
        }
    }
    /*
    @Override
    public DeviceChannel remove(int index) {
        DeviceChannel dc = super.remove(index);
        if (dc.isOpen()) dc.close();
        return dc;
    }*/

    public MotorPositionControllerList getMotorPosList() {
        MotorPositionControllerList controllers = new MotorPositionControllerList();
        for (DeviceChannel channel : this) {
            MotorPositionController controller = channel.getMotorPos();
            if (controller != null) {
                controllers.add(new MotorPositioner(controller, channel));
            }
        }
        return controllers;
    }
    
    public MotorTemperatureSensorList getMotorTemperatureSensorList() {
        MotorTemperatureSensorList controllers = new MotorTemperatureSensorList();
        for (DeviceChannel channel : this) {
            TemperatureSensor controller = channel.getTemperatureSensor();
            if (controller != null) {
                controllers.add(new MotorTemperature(controller, channel));
            }
        }
        return controllers;
    }
}
