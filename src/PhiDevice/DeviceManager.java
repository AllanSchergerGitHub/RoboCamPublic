package PhiDevice;

import com.phidget22.*;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

    /**
    * https://www.phidgets.com/docs/Phidget_Manager
    * General Overview
    * The Phidget Manager is an interface into the device channels available to the Phidget library. The API is strictly asynchronous, and 
    * continuously monitors channels as they attach and detach.

    * Each Phidget exports one or more device channels, and when a Phidget is plugged into a system (or becomes available over the network), a 
    * manager attach event is fired for each channel available from the Phidget. When the Phidget is removed from the system, a manger detach 
    * event is fired for each channel that is no longer available.

    * It is important to understand the concepts of attach and detach as the they relate to the manager. A manager attach does not imply that a 
    * user channel has attached to a device channel, but that the device channel has appeared, and the device channel is now ready to be attached 
    * to a user channel. When a user channel closes and detaches from a device channel, a manager event is not fired. A manager detach event 
    * is fired when the Phidget is removed from the system.
    * 
    */
public class DeviceManager {
    private final Manager mphManager;
    private final ArrayList<DeviceChannel> mDeviceChannels;


    //Abstract listener class
    public static abstract class DeviceChangeListener {
        public DeviceChangeListener() {
        }

        ;

        public abstract void onChange();
    }

    ;

    //Holds the list of change listeners
    private ArrayList<DeviceChangeListener> mDeviceChangeListeners = new ArrayList<>();

    public DeviceManager() throws PhidgetException {
        mDeviceChannels = new ArrayList<>();
        mphManager = new Manager();
        mphManager.addAttachListener((ManagerAttachEvent mae) -> {
            DeviceChannel device = new DeviceChannel(mae.getChannel());
            /**
             * For testing; print out the phidget devices that are connected.
             */
            //System.out.println("list devices x of y getName: "+device.getName());
            /**
             * For testing; print just the temperaturesensors.
             */
//            try {
//                TemperatureSensor tempSensor = device.getTemperatureSensor();
//                if(tempSensor != null) {
//                    System.out.println("list devices x of y getTemperatureSensor(): "+ device.getTemperatureSensor());
//                    System.out.println("list devices x of y SKU: "+ device.getTemperatureSensor().getDeviceSKU());
//                }
//                
//                
//            } catch (PhidgetException ex) {
//                Logger.getLogger(DeviceManager.class.getName()).log(Level.SEVERE, null, ex);
//            }
            synchronized (mDeviceChannels) {
                mDeviceChannels.add(device);
            }
            //Notify the listeners.e.g. WheelConfgiModel is notified of new device 
            // attachement from here.

            // Thanks for the notes here - this does a good job of explaining it.

            for (DeviceChangeListener listener : mDeviceChangeListeners) {
                //onChange does not need any argument at present,
                //can be added later if needed in the owner of listener.
                
                /** 
                 * onChange() is defined in WheelConfigPanel.java; it listens for changes in the phidget manager.
                 * These changes are the results of phidgets being connected, disconnected, etc(??) from 
                 * the system.
                 */
                listener.onChange();
            }
        });

        mphManager.addDetachListener(new ManagerDetachListener() {
            @Override
            public void onDetach(ManagerDetachEvent mde) {
                //Notify the listeners.e.g. WheelConfgiModel is notified of new device 
                // attachement from here.
                for (DeviceChangeListener listener : mDeviceChangeListeners) {
                    listener.onChange();
                }
            }
        });
        try {
            mphManager.open();
        } catch (PhidgetException ex) {
            Logger.getLogger(DeviceManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void addDeviceChangeListener(DeviceChangeListener listener) {
        mDeviceChangeListeners.add(listener);
    }

    /**
     * This for adding fake Phidget for easy testing.
     *
     * @param phidget
     */
    public void addPhidget(Phidget phidget, int serialNo) {
        mDeviceChannels.add(new DeviceChannel(phidget));
        try {
            phidget.setDeviceSerialNumber(serialNo);
        } catch (PhidgetException ex) {
            //Logger.getLogger(DeviceManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * This for adding fake BLDC for easy testing.
     *
     * @param phidget
     */
    public void addBLDCMotor(int serialNo) {
        try {
            addPhidget((Phidget) new BLDCMotor(), serialNo);
        } catch (PhidgetException ex) {
            //Logger.getLogger(DeviceManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int getChannelCount() {
        int count = 0;
        synchronized (mDeviceChannels) {
            count = mDeviceChannels.size();
        }
        return count;
    }

    public String[] getChannelNames() {
        ArrayList<String> channelNames = new ArrayList<>();
        synchronized (mDeviceChannels) {
            for (DeviceChannel channel : mDeviceChannels) {
                channelNames.add(channel.getName());
            }
        }
        return (String[]) channelNames.toArray(new String[0]);
    }

    public String[] getChannelNames(Pattern matchPattern) {
        ArrayList<String> channelNames = new ArrayList<>();
        synchronized (mDeviceChannels) {
            for (DeviceChannel channel : mDeviceChannels) {
                String channelName = channel.getName();
                //System.out.println("channelName " + channelName + " pattern " + matchPattern.pattern());
                if (channelName == null) continue;
                if (matchPattern.matcher(channelName).find()) {
                    channelNames.add(channelName);
                }
            }
        }
        return (String[]) channelNames.toArray(new String[0]);
    }

    public DeviceChannel[] getChannels(Pattern matchPattern) {
        ArrayList<DeviceChannel> channels = new ArrayList<>();
        synchronized (mDeviceChannels) {
            for (DeviceChannel channel : mDeviceChannels) {
                String channelName = channel.getName();
                //System.out.println("channelName " + channelName + " pattern " + matchPattern.pattern());
                if (channelName == null) continue;
                if (matchPattern != null && matchPattern.matcher(channelName).find()) {
                    channels.add(channel);
                }
            }
        }
        return (DeviceChannel[]) channels.toArray(new DeviceChannel[0]);
    }

    public DeviceChannel getChannelByName(String name) {
        if (name == null) return null;
        name = name.trim();
        DeviceChannel matchedChannel = null;
        synchronized (mDeviceChannels) {
            for (DeviceChannel channel : mDeviceChannels) {
                //System.out.println(channel.getName());
                if (name.equals(channel.getName())) {
                    matchedChannel = channel;
                    break;
                }
            }
        }
        return matchedChannel;
    }

    public DeviceChannelList getChannelListByNames(String[] names) {
        if (names == null) return null;
        DeviceChannelList channels = new DeviceChannelList();
        for (String name : names) {
            DeviceChannel channel = getChannelByName(name);
            if (channel != null) {
                channels.add(channel);
            }
        }
        return channels;
    }

    public void addJob(String channelName,
                       DeviceChannelJob.JobAction jobAction,
                       long timeOutPeriod) {
        new DeviceChannelJob(this, channelName, jobAction, timeOutPeriod);
    }

    public void getSettings_from_phidget() {

    }

    public void disconnectChannel(String deviceChannelName,
                                  String paramName, double paramValue) {
        addJob(deviceChannelName, new DeviceChannelJob.JobAction() {
            @Override
            public void doAction(DeviceChannel channel) {
                ChannelType channelType = ChannelType.getChannelTypeByName(channel.getChannelName());
                if (channelType == null) return;
                ChannelParamType paramType = channelType.getChannelParamTypeByName(paramName);
                System.out.println("paramType " + paramType);
                if (channelType == ChannelType.DC_MOTOR) {
                    DCMotor dcMotor = channel.getDCMotor();
                    if (paramType == ChannelParamType.VELOCITY) {
                        try {
                            dcMotor.setTargetVelocity(paramValue);
                        } catch (PhidgetException ex) {
                            Logger.getLogger(DeviceManager.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                } else if (channelType == ChannelType.STEPPER) {
                    Stepper stepper = channel.getStepper();
                    try {
                        System.out.println("getDeviceName " + stepper.getDeviceName() + " current limit = " + stepper.getCurrentLimit());
                        stepper.setCurrentLimit(2.0); // must be set to 2.0 or the stepper motor won't work well.
                        System.out.println("current limit = " + stepper.getCurrentLimit());
                        System.out.println("stepper position = " + stepper.getPosition());
                    } catch (PhidgetException ex) {
                        Logger.getLogger(DeviceManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if (paramType == ChannelParamType.POSITION) {
                        try {
                            stepper.setEngaged(true);
                            stepper.setTargetPosition(paramValue);
                        } catch (PhidgetException ex) {
                            Logger.getLogger(DeviceManager.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }

            @Override
            public void afterAction() {
            }
        }, 10000);
    }

    public void setChannelParam(String deviceChannelName,
                                String paramName, double paramValue) {
        addJob(deviceChannelName, new DeviceChannelJob.JobAction() {
            @Override
            public void doAction(DeviceChannel channel) {
                ChannelType channelType = ChannelType.getChannelTypeByName(channel.getChannelName());
                if (channelType == null) return;
                ChannelParamType paramType = channelType.getChannelParamTypeByName(paramName);
                System.out.println("paramType " + paramType);
                if (channelType == ChannelType.DC_MOTOR) {
                    DCMotor dcMotor = channel.getDCMotor();
                    if (paramType == ChannelParamType.VELOCITY) {
                        try {
                            dcMotor.setTargetVelocity(paramValue);
                        } catch (PhidgetException ex) {
                            Logger.getLogger(DeviceManager.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                } else if (channelType == ChannelType.STEPPER) {
                    Stepper stepper = channel.getStepper();
                    try {
                        System.out.println("getDeviceName " + stepper.getDeviceName() + " current limit = " + stepper.getCurrentLimit());
                        stepper.setCurrentLimit(2.0); // must be set to 2.0 or the stepper motor won't work well.
                        System.out.println("current limit = " + stepper.getCurrentLimit());
                        System.out.println("stepper position = " + stepper.getPosition());
                    } catch (PhidgetException ex) {
                        Logger.getLogger(DeviceManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if (paramType == ChannelParamType.POSITION) {
                        try {
                            stepper.setEngaged(true);
                            stepper.setTargetPosition(paramValue);
                        } catch (PhidgetException ex) {
                            Logger.getLogger(DeviceManager.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }

            @Override
            public void afterAction() {
            }
        }, 10000);
    }

    public void getChannelParam(String deviceChannelName,
                                String paramName, ParamRunnable.Reader reader) {
        addJob(deviceChannelName, new DeviceChannelJob.JobAction() {
            Double paramValue = null;

            @Override
            public void doAction(DeviceChannel channel) {
                ChannelType channelType = ChannelType.getChannelTypeByName(channel.getChannelName());
                if (channelType == null) return;
                ChannelParamType paramType = channelType.getChannelParamTypeByName(paramName);
                if (channelType == ChannelType.DC_MOTOR) {
                    DCMotor dcMotor = channel.getDCMotor();
                    if (paramType == ChannelParamType.VELOCITY) {
                    }
                } else if (channelType == ChannelType.STEPPER) {
                    Stepper stepper = channel.getStepper();
                    if (paramType == ChannelParamType.POSITION) {
                        try {
                            stepper.setEngaged(true);
                            paramValue = stepper.getTargetPosition();
                        } catch (PhidgetException ex) {
                            Logger.getLogger(DeviceManager.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }

            @Override
            public void afterAction() {
                if (paramValue != null) {
                    reader.onRead(paramValue);
                }
            }
        }, 10000);
    }
}
