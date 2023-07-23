package RoboCam;

import Chart.ChartParamsDataset;
import DB.ConfigDB;
import PhiDevice.DeviceManager;
import PhiDevice.PositionList;
import RoverUI.Vehicle.Truck;
import RoverUI.Vehicle.Wheel;

public class TruckDevice {
    private WheelDevice[] mWheelDevices;
    double distanceRemainingRover = 0;
    private PositionList mBLDCMotorPosMult = new PositionList(1, 1);
    private Wheel[] wheels = null;

    public TruckDevice(Truck truck, DeviceManager deviceManager,
                       ConfigDB configDB) {
        wheels = truck.getWheels();
        mWheelDevices = new WheelDevice[wheels.length];
        int index = 0;
        for (Wheel wheel : wheels) {
            mWheelDevices[index] = new WheelDevice(wheel, deviceManager, configDB);
            index += 1;
        }
    }

    /*
     * This is for the Rover UI charts only.
    */
    public Chart.ChartParamsDataset[] getChartParamsDatasets() {
        Chart.ChartParamsDataset[] chartParamsDatasets =
                new ChartParamsDataset[mWheelDevices.length];
        int i = 0;
        for (WheelDevice wheelDevice : mWheelDevices) {
            chartParamsDatasets[i] = wheelDevice.getChartParamsDataset();
            i++;
        }
        return chartParamsDatasets;
    }

    public void updateChartParamsDataset() {
        for (WheelDevice wheelDevice : mWheelDevices) {
            wheelDevice.updateChartParamsDataset();
        }
    }

    public void updateDutyCycleAtIndex() {
        for (WheelDevice wheelDevice : mWheelDevices) {
            wheelDevice.updategetBLCDCDutyCyleAtIndex();
        }
    }

    public WheelDevice getWheelDeviceAt(int index) {
        return mWheelDevices[index];
    }

    public void addForwardDistance(double distance) {
        //wheelDevice.getBLCDCPosIndex();
        for (WheelDevice wheelDevice : mWheelDevices) {
            wheelDevice.addForwardDistance(distance);
        }
    }

    private void calcRemainingDistanceRatio() {
        int index = 0;
        double[] allPositions = {0, 0, 0, 0, 0, 0, 0, 0};
        double[] allMultipliers = {0, 0, 0, 0, 0, 0, 0, 0};
        double mTargetPositionRoverBody = 0.0;
        //System.out.println(" ");
        for (WheelDevice wheelDevice : mWheelDevices) {
            allPositions[index] = wheelDevice.getBLCDCPosIndex()[0];
            allPositions[index + 4] = wheelDevice.getBLCDCPosIndex()[1];
            allMultipliers[index] = wheelDevice.getBLCDCPosMultiplier()[0];
            allMultipliers[index + 4] = wheelDevice.getBLCDCPosMultiplier()[1];
            //PositionList mult = wheelDevice.getBLCDCPosMultiplier();
            mTargetPositionRoverBody = wheelDevice.getmTargetPositionRoverBody();
            //System.out.print(mTargetPositionRoverBody+" ");
            index += 1;
        }
        //System.out.print("TargetPositionRoverBody "+mTargetPositionRoverBody);//+" allPositions: "+Arrays.toString(allPositions));//+" allMultipliers: "+Arrays.toString(allMultipliers));

        double sumAllDistances = 0;

        for (int i = 0; i < allPositions.length; i++) {
            sumAllDistances = sumAllDistances + (allPositions[i] * allMultipliers[i]);
        }
        double avgAllDistances = sumAllDistances / 4.00; // this is the same as the RoverBodyActualPosition. the divisor is the number of motors on all wheels. reduced to 4 total in Dec 2019.

        distanceRemainingRover = mTargetPositionRoverBody - avgAllDistances;
        //double[] RemainingDistances = Wheel.getBaseLengthStatic();
        //System.out.println(" avgAllDistances "+String.format("%.1f", avgAllDistances)+ " distanceRemainingRover "+String.format("%.1f", distanceRemainingRover));
    }
    
    public double getDistanceRemainingRover() {
        return distanceRemainingRover;
    }

    public String[] getDisengage2Warning() {
        String[] warning = new String[mWheelDevices.length];
        int i2 = 0;
        for (WheelDevice wheelDevice : mWheelDevices) {
            warning[i2] = wheelDevice.getDisengageWarning();
            i2++;
        }
        return warning;
    }

    public void updateDevices() {
        calcRemainingDistanceRatio(); // 1st - go get the position from all devices and calculate the distanceremainingrover.
        for (WheelDevice wheelDevice : mWheelDevices) {
            wheelDevice.setDistanceTarget(distanceRemainingRover); // 2nd - calculate and set a Remaining Distance value
            wheelDevice.updateDevices(); // 3rd - update the device including the device's new target position
        }
    }

    public void add_DutyCycleListener() {
        for (WheelDevice wheelDevice : mWheelDevices) {
            wheelDevice.add_DutyCycleListener();
        }
    }

    public void add_BLDC_POSChangeListener() {
        for (WheelDevice wheelDevice : mWheelDevices) {
            wheelDevice.add_BLDC_POSChangeListener();
        }
    }

    public void disengageDevicesCloseChannels() {
        for (WheelDevice wheelDevice : mWheelDevices) {
            wheelDevice.disengageDevicesCloseChannels();
        }
    }

    public boolean detectDevices(boolean forced) {
        boolean allDetected = false;
        for (WheelDevice wheelDevice : mWheelDevices) {
            allDetected &= wheelDevice.detectDevices(forced);
        }

        return allDetected;
    }

}
