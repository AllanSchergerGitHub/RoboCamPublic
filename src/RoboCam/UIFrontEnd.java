/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package RoboCam;

import DB.ConfigDB;
import com.robocam.Socket.*;
import PhiDevice.DeviceListJob;
import PhiDevice.ChannelType;
import PhiDevice.DeviceManager;
import PhiDevice.DeviceChannel;
import RoverUI.DoubleListener;
import RoverUI.DoubleVarArgListener;
import RoverUI.StringListener;
import RoverUI.XYDoubleListener;
import com.phidget22.PhidgetException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.IOException;
import static java.lang.Thread.sleep;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import Chart.ChartParamsDataset;
import GamePad.GamePadManager;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import RoverUI.Vehicle.Wheel;
import Utility.CameraHelper;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 *
 * @author sujoy
 */
public class UIFrontEnd extends javax.swing.JFrame {
    private final SocketClient mSocketClient;    
    private ComPipe mComPipe;
    private final Thread mSocketClientThread;
    private final ComParser mComParser = new ComParser();
    DeviceManager mDeviceManager;
    DeviceListJob mDeviceListJob;

    private String mMachineName = "init";
    
    String targetValue;    
    int targetValue_int;

    private final MousePosCommand mMousePosCommand = new MousePosCommand();
    private final SteeringCommand mSteeringCommand = new SteeringCommand();
    private final DirectionCommand mDirectionCommand = new DirectionCommand();
    private final SpeedCommand mSpeedCommand = new SpeedCommand();
    private final MouseHandednessCommand mMouseHandednessCommand = new MouseHandednessCommand();
    private final ForwardAngleCommand mForwardAngleCommand = new ForwardAngleCommand();
    private WheelDeviceParamCommand[] mWheelDeviceParamCommands;
    private final TruckScaleCommand mTruckScaleCommand = new TruckScaleCommand();
    private final CommonSensorCommand mCommonSensorCommand = new CommonSensorCommand();

    int imageSaveTimeLag = 1000;
    
    ArrayList<IPCamFrame> mIpCamFrames = new ArrayList<>();
    ConfigDB mConfigDB;
    Config mConfig;
    DynamicConfig mDyanmicConfig;
    ChartParamsDataset[] mChartParamsDatasets;
    
    private double grandTotalLagTime1000Instances = 0;
    private final ArrayList<Double> avgLagTime = new ArrayList<Double>();
    private long mLagTime  = 1;
    private Double priorMax = 0.0;
    int doInBackgroundCounter = 0;
    
    Timer mUpdaterTimer;
    
    private HashMap<String, IPCamSetting> mCamSettingMap = new HashMap<>();
    
    private final GamePadManager mGamePadManager = new GamePadManager();
    private GamePadUpdater mGamePadUpdater;
    
     ExecutorService mCameraExecutor = Executors.newFixedThreadPool(1);

    /**
     * Creates new form RoboFrontEnd
     */
    public UIFrontEnd(){
        initComponents();
        initMoreComponents();
        mSocketClientThread = null;
        mSocketClient = null;
    }

    public UIFrontEnd(String roverHost, int roverPort) throws IOException, PhidgetException {
        System.out.println("UI Machine: (poor variables name) roverHost and roverPort "+roverHost+" "+roverPort);
        initComponents();
        initMoreComponents();
        mSocketClient = new SocketClient(roverHost, roverPort, mComPipe);
        mSocketClientThread = new Thread(mSocketClient);
        mSocketClientThread.start();
        
        //mSocketWriteClient = new SocketWriteClient(roverHost, roverPort, mComPipe);
        //mSocketWriteClientThread = new Thread(mSocketWriteClient);
        //mSocketWriteClientThread.start();
        
        mComParser.execute();
        
        mDeviceManager = new DeviceManager();
        mDeviceListJob = new DeviceListJob(mDeviceManager, cmbPhidgetChannels);
        mDeviceListJob.execute();
        
        //start GamePadController
        mGamePadManager.connectToControllers();
        mGamePadUpdater = new GamePadUpdater(
                mGamePadManager, mTruckSteerPanel, mLblGamePadStatusValue);
        mGamePadUpdater.execute();
    }

    public void setMachineName(String machineName) {
        mMachineName = machineName;
        mTruckSteerPanel.setMachineName(mMachineName);
    }
        
    private void initMoreComponents() {
        /* Four Commands for four wheels */
        mWheelDeviceParamCommands = new WheelDeviceParamCommand[] {
            new WheelDeviceParamCommand(0),
            new WheelDeviceParamCommand(1),
            new WheelDeviceParamCommand(2),
            new WheelDeviceParamCommand(3)
        };

        mComPipe = new ComPipe();
        mIPCamPanel.addConnectionListener(new IPCamPanel.ConnectionListener() {
            @Override
            public void onConnect() {
                lblIPConStatus.setText("Connected");
                lblIPConStatus.setBackground(Color.GREEN);
                lblIPConStatus.repaint();
            }

            @Override
            public void onDisconnect() {
                lblIPConStatus.setText("Disconnected");
                lblIPConStatus.setBackground(Color.RED);
                lblIPConStatus.repaint();
            }

            @Override
            public void onImageUpdate() {}
        });

        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getID() == KeyEvent.KEY_PRESSED && mMainTabbedPane.getSelectedIndex() == 2) {
                    return mTruckSteerPanel.onKeyPressed(e);
                }
                return false;
            }
        });

        mTruckSteerPanel.addGroudPanelMousePosListener(new XYDoubleListener() {
            @Override
            public void onChange(double xPos, double yPos) {
                mMousePosCommand.setPos(xPos, yPos);
                mComPipe.putOut(mMousePosCommand.buildCommand());
                mTruckSteerPanel.getTruck().updateChartParamsDataset();
            }
        });
        mTruckSteerPanel.addMouseHandednessListener(new StringListener() {
            @Override
            public void onChange(String string) {
                mMouseHandednessCommand.setHandedness(
                        mTruckSteerPanel.getGroundPanel().getMouseHandedness());
                mComPipe.putOut(mMouseHandednessCommand.buildCommand());
            }
        });
        mTruckSteerPanel.addSteeringModeListener(new StringListener() {
            @Override
            public void onChange(String string) {
                mSteeringCommand.setSteeringMode(string);
                //System.out.println(string);
                mComPipe.putOut(mSteeringCommand.buildCommand());
            }
        });
        mTruckSteerPanel.addSteerDirectionListener(new StringListener() {
            @Override
            public void onChange(String string) {
                mDirectionCommand.setDirectionMode(string);
                //System.out.println(string);
                mComPipe.putOut(mDirectionCommand.buildCommand());
            }
        });
        mTruckSteerPanel.addSpeedListener((double... numbers) -> {
            mSpeedCommand.setSpeed(numbers[0]);
            mSpeedCommand.setVelocityLimit(numbers[1]);
            mSpeedCommand.setVelocityLimitIncrement(numbers[2]);
            //System.out.println(mSpeedCommand.buildCommand());
            mComPipe.putOut(mSpeedCommand.buildCommand());
        });
        mTruckSteerPanel.addForwardDistanceListener(new DoubleVarArgListener() {
            @Override
            public void onChange(double... numbers) {
                mForwardAngleCommand.setForwardAngle(numbers[0]);
                mForwardAngleCommand.setForwardAngleMultiplier(numbers[1]);
                mComPipe.putOut(mForwardAngleCommand.buildCommand());
            }
        });
        mTruckSteerPanel.addTruckScaleListener(new DoubleListener() {
            @Override
            public void onChange(double number) {
                mTruckScaleCommand.setTruckScale(number);
                mComPipe.putOut(mTruckScaleCommand.buildCommand());
            }
        });

        mChartParamsDatasets = mTruckSteerPanel.getTruck()
                .getChartParamsDatasets();
        for (ChartParamsDataset chartParamsDataset: mChartParamsDatasets) {
            JFreeChart chart = ChartFactory.createXYLineChart(
                    chartParamsDataset.getChartName(),
                    "Time",
                    null,
                    chartParamsDataset.getDataset()
            );
            ChartPanel chartPanel = new ChartPanel(chart);
            mPanelCharts.add(chartPanel);
        }
        
        mUpdaterTimer = new Timer(200, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mTruckSteerPanel.getTruck().updateChartParamsDataset();
            }
        });
        mUpdaterTimer.start();
        
        addWindowListener(new  WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanUpOnClose();
                super.windowClosing(e);
            }
        });
    }

    public int getImageSaveLag() {
        int x = imageSaveTimeLag;
        return x;
    }
    
    public void loadFromConfig(Config config) { // there is also a loadFromConfig for the Rover UI
        mConfig = config;
        try {
            mConfigDB = new ConfigDB(config.getConfigDBPath());
        } catch (SQLException ex) {
            Logger.getLogger(UIFrontEnd.class.getName()).log(Level.SEVERE, null, ex);
        }
        mDyanmicConfig = new DynamicConfig();
        
        mDeviceListJob.setSelectedDevice(config.getDefaultDevice());
        mTruckSteerPanel.loadFromConfig(config);
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        /* Place this window at center and on top */
        setLocation(
            (int) ((screenSize.getWidth()-getWidth())/2-100), // updated to move window to lower right of my screen via '+200'. temp -120 allows me to see output window
            (int) ((screenSize.getHeight()-getHeight())/2-115));  // updated to move window to lower right of my screen via '+116'.  temp -175 allows me to see output window
        //setAlwaysOnTop(true); // brings screen to top for initial launch
        //setAlwaysOnTop(false); // don't always need this on top (after initial positioning)
        this.setName("UIFrontEnd");
        mDyanmicConfig.addComponent(this);

        /* Open up the IPCam windows and position them */
        String[] camNames = config.getIPCamNames();
        //HasMap<String, 

        double evenCamCount = Math.ceil(Math.sqrt(camNames.length)+1); // note - only cameras that are shown on screen are currently capable of being saved to disk... edited formula by adding '+1' to make images smaller (only showing 3 smaller images now)

        Dimension frameDimension = new Dimension(
                (int) (screenSize.getWidth()/evenCamCount),
                (int) (screenSize.getHeight()/evenCamCount));

        int row = 0;
        int col = 0;
        
        for (String camName : camNames) {
            if (camName.length() == 0) continue;
            IPCamFrame ipCamFrame = new IPCamFrame();
            ipCamFrame.setName(String.format("IPCam.%s", camName));

            ipCamFrame.setTitle(String.format("setTitle aka camName = %s", ipCamFrame.getName()));
            ipCamFrame.setUrl(config.getIPCamUrl(camName));

            ipCamFrame.setVisible(true);

            ipCamFrame.setSize(frameDimension);
            ipCamFrame.setLocation(
                    (int) (col*frameDimension.getWidth()),
                    (int) (row*frameDimension.getHeight()));
            ipCamFrame.getIPCamPanel().setImageFolder(camName, config.getImageFolder());
            mIpCamFrames.add(ipCamFrame);
            mDyanmicConfig.addComponent(ipCamFrame);
            mDyanmicConfig.addUiCollection(
                String.format("_uiLine_%s", ipCamFrame.getName()),
                ipCamFrame.getUiLineCollection());

            
            mCamSettingMap.put(camName, new IPCamSetting(camName, config));
            mCameraListBox.add(camName);
            //mCameraListBox.add(""+config.getIPCamAddress(camName));

            col++;
            if (col >= evenCamCount) {
                col = 0;
                row++;
            }
        }
        if (camNames.length > 0) {
            mIPCamPanel.setUrlAddrress(mCamSettingMap.get(camNames[0]).getPictureUrl());
        }
        mCameraListBox.select(0);
        
        mGamePadManager.loadFromConfig(config);

    }

    public void moveMotor(int targetValue_int)    {
        targetValue_int=this.targetValue_int;//targetValue_intINCREMENT+
        String channelName = cmbPhidgetChannels.getSelectedItem().toString();
        String paramName = cmbPhidgetParamNames.getSelectedItem().toString();
        targetValue = (String)(targetValue_int+"");
        System.out.println("targetValue - "+targetValue);
        mDeviceManager.setChannelParam(
                channelName,
                paramName,
                Float.parseFloat(targetValue)
        );
    }
    
    private void IPCameraPTZ_goto_preset(){
        String jComboBox1_camName2 = mCamSettingMap.get(mCameraListBox.getSelectedItem()+"").getIpAddress();
        URL url_presets = null;
        String presetName = jTextField10GoToPreset.getText();
        presetName = list1_ip_cam_presets.getSelectedItem();
                
        try {
            url_presets = new URL("http://"+jComboBox1_camName2+"//cgi-bin/CGIProxy.fcgi?cmd=ptzGotoPresetPoint&name="+presetName+"&usr=allanscherger&pwd=password");
             
        } catch (MalformedURLException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        }
        BufferedReader bin = null;
        try {
            bin = new BufferedReader ( 
                    new InputStreamReader( url_presets.openStream( ) ));
        } catch (IOException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void IPCameraPTZ_add_preset(){
        String jComboBox1_camName2 = mCamSettingMap.get(mCameraListBox.getSelectedItem()+"").getIpAddress();
        URL url_presets = null;
        String presetName = jTextField_presetName.getText();
        try {
            url_presets = new URL("http://"+jComboBox1_camName2+"//cgi-bin/CGIProxy.fcgi?cmd=ptzAddPresetPoint&name="+presetName+"&usr=allanscherger&pwd=password");
             
        } catch (MalformedURLException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        }
        BufferedReader bin = null;
        try {
            bin = new BufferedReader ( 
                    new InputStreamReader( url_presets.openStream( ) ));
        } catch (IOException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void IPCameraPTZ_delete_preset(){
        String jComboBox1_camName2 = mCamSettingMap.get(mCameraListBox.getSelectedItem()+"").getIpAddress();
        URL url_presets = null;
        String presetName = jTextFieldDeletePreset.getText();
        try {
            url_presets = new URL("http://"+jComboBox1_camName2+"//cgi-bin/CGIProxy.fcgi?cmd=ptzDeletePresetPoint&name="+presetName+"&usr=allanscherger&pwd=password");
             
        } catch (MalformedURLException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        }
        BufferedReader bin = null;
        try {
            bin = new BufferedReader ( 
                    new InputStreamReader( url_presets.openStream( ) ));
        } catch (IOException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void IPCameraPTZ_get_presets(){
        final String camName = mCamSettingMap.get(mCameraListBox.getSelectedItem()+"").getIpAddress();
        list1_ip_cam_presets.removeAll();
        mCameraExecutor.submit(() -> {
            final String[] presets = CameraHelper.getPresets(
                "http://"+camName+"/cgi-bin/CGIProxy.fcgi?cmd=getPTZPresetPointList&usr=allanscherger&pwd=password"
            );
            SwingUtilities.invokeLater(() -> {
                list1_ip_cam_presets.removeAll();
                for(String preset: presets) {
                    list1_ip_cam_presets.add(preset);
                }
            });
        });
        //new IPCameraPTZPresetLoader(jComboBox1_camName2).execute();
    }

    // Deprecated
    class IPCameraPTZPresetLoader extends SwingWorker<Void, String> {
        private String mCamName;
        
        public IPCameraPTZPresetLoader(String camName) {
            mCamName = camName;
        }

        @Override
        protected Void doInBackground() throws Exception {
            URL url_presets = null;
            String urlString = "http://"+mCamName+"//cgi-bin/CGIProxy.fcgi?cmd=getPTZPresetPointList&usr=allanscherger&pwd=password";
            try {
                url_presets = new URL(urlString);
                 
            } catch (MalformedURLException ex) {
                Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
            String str= null;
            ArrayList<String> lines = new ArrayList<String>();
            BufferedReader bin = null;
            try {
                bin = new BufferedReader (
                new InputStreamReader( url_presets.openStream()));
                while((str = bin.readLine()) != null){
                    lines.add(str);
                }
            } catch (IOException ex) {
                System.err.println("ip cam not selected? - IOException: "+url_presets);
                //Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NullPointerException ex){
                System.err.println("ip cam not selected? - NullPointerException: "+url_presets);
            }

            //BufferedReader in = new BufferedReader(new FileReader("path/of/text"));

            //        try {
            //            while((str = bin.readLine()) != null){
            //                lines.add(str);
            //            }
            //        } catch (IOException ex) {
            //            Logger.getLogger(UIFrontEnd.class.getName()).log(Level.SEVERE, null, ex);
            //        }
            //          catch (NullPointerException ex){
            //            System.err.println("ip cam not selected - warning: "+url_presets);
            //        }
            String[] linesArray = lines.toArray(new String[lines.size()]);
            publish(linesArray);
            //            String line = "";
            //            try {
            //                line = bin.readLine();
            //            } catch (IOException ex) {
            //                Logger.getLogger(UIFrontEnd.class.getName()).log(Level.SEVERE, null, ex);
            //            }
            //            while(line!=null){
            //            System.out.println(line);
            //                try {
            //                    line=bin.readLine();
            //                } catch (IOException ex) {
            //                    Logger.getLogger(UIFrontEnd.class.getName()).log(Level.SEVERE, null, ex);
            //                }
            //            }
            return null;
        }

        @Override
        protected void process(List<String> lines) {
            for (int linesPrint = 0; linesPrint < lines.size(); linesPrint++){
                if(lines.get(linesPrint) != "0"){
                     String s = lines.get(linesPrint);
                     Pattern p = Pattern.compile(">.*?<");
                     Matcher m = p.matcher(s);
                     if(m.find()){
                        String preset_detail = String.valueOf(m.group().subSequence(1, m.group().length()-1));
                        list1_ip_cam_presets.add(preset_detail);
                        System.out.println(m.group().subSequence(1, m.group().length()-1)+" "+linesPrint+ " --preset_detail "+preset_detail);
                     }

                    //System.out.println("first item in array: "+linesArray[linesPrint]+" "+linesPrint);
                }
            }
        }
        
    }

    private void updateIPCameraPOS_Settings(int moveDuration, String moveDirection){
        URL url = null;
        String jComboBox1_camName2 = mCamSettingMap.get(mCameraListBox.getSelectedItem()+"").getIpAddress();
        try {
            switch(moveDirection)
            {
                case "Right":
                    url = new URL("http://"+jComboBox1_camName2+"//cgi-bin/CGIProxy.fcgi?cmd=ptzMoveRight&usr=allanscherger&pwd=password");
                    break;
                case "Left":
                    url = new URL("http://"+jComboBox1_camName2+"//cgi-bin/CGIProxy.fcgi?cmd=ptzMoveLeft&usr=allanscherger&pwd=password");
                    break;
                case "Up":
                    url = new URL("http://"+jComboBox1_camName2+"//cgi-bin/CGIProxy.fcgi?cmd=ptzMoveUp&usr=allanscherger&pwd=password");
                    break;
                case "Down":
                    url = new URL("http://"+jComboBox1_camName2+"//cgi-bin/CGIProxy.fcgi?cmd=ptzMoveDown&usr=allanscherger&pwd=password");
                    break;
                default: //shouldn't need this - but just in case we'll stop if no cases apply
                    url = new URL("http:/"+jComboBox1_camName2+"//cgi-bin/CGIProxy.fcgi?cmd=ptzRunStop&usr=allanscherger&pwd=password");
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        }
        BufferedReader bin = null;
        try {
            bin = new BufferedReader ( 
                    new InputStreamReader( url.openStream( ) ));
        } catch (IOException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {
            sleep(moveDuration);
        } catch (InterruptedException ex) {
            Logger.getLogger(UIFrontEnd.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {
            url = new URL("http://"+jComboBox1_camName2+"//cgi-bin/CGIProxy.fcgi?cmd=ptzStopRun&usr=allanscherger&pwd=password");
        } catch (MalformedURLException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        }
        bin = null;
        try {
            bin = new BufferedReader ( 
                    new InputStreamReader( url.openStream( ) ));
        } catch (IOException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void zoomInOut(int moveDuration, String moveDirection) {
        URL url = null;
        String jComboBox1_camName2 = mCamSettingMap.get(mCameraListBox.getSelectedItem()+"").getIpAddress();
        try {
            switch(moveDirection)
            {
                case "ZoomIn":
                    url = new URL("http://"+jComboBox1_camName2+"//cgi-bin/CGIProxy.fcgi?cmd=zoomIn&usr=allanscherger&pwd=password");
                    break;
                case "ZoomOut":
                    url = new URL("http://"+jComboBox1_camName2+"//cgi-bin/CGIProxy.fcgi?cmd=zoomOut&usr=allanscherger&pwd=password");
                    break;
                case "ZoomStop":
                    url = new URL("http://"+jComboBox1_camName2+"//cgi-bin/CGIProxy.fcgi?cmd=zoomStop&usr=allanscherger&pwd=password");
                    break;
                default: //shouldn't need this - but just in case we'll stop if no cases apply
                    url = new URL("http:/"+jComboBox1_camName2+"//cgi-bin/CGIProxy.fcgi?cmd=zoomStop&usr=allanscherger&pwd=password");
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        }
        BufferedReader bin = null;
        try {
            bin = new BufferedReader ( 
                    new InputStreamReader( url.openStream( ) ));
        } catch (IOException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {
            System.err.println("moveDuration "+moveDuration);
            sleep(moveDuration);
        } catch (InterruptedException ex) {
            Logger.getLogger(UIFrontEnd.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {
            url = new URL("http://"+jComboBox1_camName2+"//cgi-bin/CGIProxy.fcgi?cmd=zoomStop&usr=allanscherger&pwd=password");
        } catch (MalformedURLException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        }
        bin = null;
        try {
            bin = new BufferedReader ( 
                    new InputStreamReader( url.openStream( ) ));
        } catch (IOException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
     
    final class ComParser extends SwingWorker<Void, String> {
        @Override
        protected Void doInBackground() throws Exception {
            long maxEstimateTime = 0;
            long lastUpdatedAt = 0;
            while(true) {
                String line = mComPipe.getIn();
                if (line != null) {
                    publish(line);
                }

                if (System.currentTimeMillis()-lastUpdatedAt > 200) {
                    lastUpdatedAt = System.currentTimeMillis();
                    publish("");
                }
                
                long startTime = System.nanoTime();
                //mTruckSteerPanel.updateLagProgress(lag); // Allan modified this Jan 15 2019
                
                long estimatedTime = System.nanoTime() - startTime;
                estimatedTime = System.nanoTime() - startTime;
                if(estimatedTime>maxEstimateTime){
                    //System.err.println(" new Max loop time: "+estimatedTime);
                    maxEstimateTime = estimatedTime;
                }
//                if(doInBackgroundCounter % 80 == 0){
//                        System.err.print(" Max: "+maxEstimateTime);
//                        //System.err.println("; avg lag:"+averageValue+" max lag:"+priorMax);
//                    }//+" .....  "+removeFromTotal+" .... "+(double)(avgLagTime.get(500)));
                
                //estimatedTime = estimatedTime-1000000;

                Double addThis = (double)estimatedTime;
                avgLagTime.add(0,addThis);
                grandTotalLagTime1000Instances = grandTotalLagTime1000Instances + addThis;
                if(addThis>priorMax){
                    priorMax = addThis;
                }
                if ((avgLagTime.size())>1000){
                    double removeFromTotal = (double)(avgLagTime.get(1000));
                    grandTotalLagTime1000Instances = grandTotalLagTime1000Instances - removeFromTotal;
                    double averageValue = (double)(grandTotalLagTime1000Instances/1000);
                    if(doInBackgroundCounter % 500 == 0){
                        System.err.println("avg lag: "+averageValue+" max lag: "+priorMax+" nanoseconds");
                    }//+" .....  "+removeFromTotal+" .... "+(double)(avgLagTime.get(500)));
                    avgLagTime.remove(1000);
                    
                }                
                doInBackgroundCounter++; 
                //mTruckSteerPanel.getTruck().executeNextCueTask();
                //System.out.println("sleeping for 5");
                Thread.sleep(5);
            }
        }

        @Override
        protected void process(List<String> list) {
            for (String command: list) {
                //mTxtAreaLog.append(command + "\n");
                //System.out.println("command " + command);
                if (command.length() == 0) {//update other gui widgets
                    long lastHeartBeatAge = mComPipe.getLastReceviedMessageAge();
                    mTruckSteerPanel.updateLagProgress(lastHeartBeatAge);
                    continue;
                }
                if (mCommonSensorCommand.canServeCommand(command)) {
                    mCommonSensorCommand.parseCommand(command);
                    mTruckSteerPanel.setLabel_jLabel_ElectricalCurrent(
                            mCommonSensorCommand.getElectricalCurrent());
                    continue;
                }
                for(WheelDeviceParamCommand wdpc: mWheelDeviceParamCommands) {
                    if (wdpc.canServeCommand(command)) {
                        //System.out.println("comman: " + command);
                        wdpc.parseCommand(command);
                        Wheel wheel = mTruckSteerPanel
                                .getTruck().getWheels()[wdpc.getWheelIndex()];
                        //wheel.setBLDCmotorReadPosDepreciated(wdpc.getBLDCMotorPosDepreciated());
                        wheel.setBLDCmotorReadPos(0, wdpc.getBLDCMotorPos(0), (String)"UIFront");
                        //System.err.println("???????????????????????????UIFront is setting BLDC MOTOR POS? Maybe this causes errors? "+wdpc.getBLDCMotorPos(0)+" : "+wdpc.getBLDCMotorPos(1));
                        wheel.setBLDCmotorReadPos(1, wdpc.getBLDCMotorPos(1), (String)"UIFront");
                        //wheel.setReadDutyCycle(wdpc.getBLDCMotorDutyCycle());
                        wheel.setBLCDCDutyCyleAtIndex(0,wdpc.getBLDCMotorDutyCycle(0));
                        wheel.setBLCDCDutyCyleAtIndex(1,wdpc.getBLDCMotorDutyCycle(1));
                        break;
                    }
                }
            }
        }
    }

    private String getXYString(Point point){
        moveMotor((int)point.x*20);
       System.out.println(point.x*20);
        try {
            sleep(10);
        } catch (InterruptedException ex) {
            Logger.getLogger(UIFrontEnd.class.getName()).log(Level.SEVERE, null, ex);
        }
        return String.format("(x, y) = (%d, %d)", point.x, point.y);

    }
    private void cleanUpOnClose() {
        long startTime = System.currentTimeMillis();
        mComPipe.sendCloseMessage();
        mDyanmicConfig.save();        
        while ((System.currentTimeMillis()-startTime) < 1000*5) {
            //mTruckSteerPanel.getTruck().stopMoving();
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ex) {
                Logger.getLogger(RoverFrontEnd.class.getName()).log(Level.SEVERE, null, ex);
                break;
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        mBtnExitApp = new javax.swing.JButton();
        mMainTabbedPane = new javax.swing.JTabbedPane();
        mCamTab = new javax.swing.JPanel();
        mIPCamPanel = new RoboCam.IPCamPanel();
        jButton3_moveIPCamRight = new javax.swing.JButton();
        jTextField9 = new javax.swing.JTextField();
        jSlider_moveDuration = new javax.swing.JSlider();
        jButton3_moveIPCamLeft = new javax.swing.JButton();
        jButton3_moveIPCamDown = new javax.swing.JButton();
        jButton3_moveIPCamUp = new javax.swing.JButton();
        mComboBoxCams = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        jTextField_presetName = new javax.swing.JTextField();
        jButton2 = new javax.swing.JButton();
        jButtonIPCameraPTZ_get_presets = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jTextField10GoToPreset = new javax.swing.JTextField();
        list1_ip_cam_presets = new java.awt.List();
        jLabel9 = new javax.swing.JLabel();
        jButtonZoomIn = new javax.swing.JButton();
        jButtonZoomOut = new javax.swing.JButton();
        mCameraListBox = new java.awt.List();
        jButtonDeletePreset = new javax.swing.JButton();
        jTextFieldDeletePreset = new javax.swing.JTextField();
        jSlider2 = new javax.swing.JSlider();
        jLabel10 = new javax.swing.JLabel();
        lblIPConStatus = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        lblPhidgetChannel = new javax.swing.JLabel();
        cmbPhidgetChannels = new javax.swing.JComboBox<>();
        lblPhidgetParamName = new javax.swing.JLabel();
        cmbPhidgetParamNames = new javax.swing.JComboBox<>();
        lblPhidgetParamTargetValue = new javax.swing.JLabel();
        txtPhidgetParamTargetValue = new javax.swing.JFormattedTextField();
        btnSetValue = new javax.swing.JButton();
        btnGetValue = new javax.swing.JButton();
        jSlider1 = new javax.swing.JSlider();
        jButton1 = new javax.swing.JButton();
        jPanelVehicle = new javax.swing.JPanel();
        mTruckSteerPanel = new RoverUI.TruckSteerPanel();
        mPanelCharts = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jTextField1 = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JTextField();
        jTextField4 = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jTextField5 = new javax.swing.JTextField();
        jTextField6 = new javax.swing.JTextField();
        jTextField7 = new javax.swing.JTextField();
        jTextField8 = new javax.swing.JTextField();
        mPanelStatusInfo = new javax.swing.JPanel();
        mBtnEnableGamePad = new javax.swing.JToggleButton();
        mLblGamePadStatus = new javax.swing.JLabel();
        mLblGamePadStatusValue = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("UI");
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));

        mBtnExitApp.setText("Exit");
        mBtnExitApp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBtnExitAppActionPerformed(evt);
            }
        });
        getContentPane().add(mBtnExitApp);

        mMainTabbedPane.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        mMainTabbedPane.setMinimumSize(new java.awt.Dimension(400, 325));
        mMainTabbedPane.setPreferredSize(new java.awt.Dimension(1000, 800));

        mCamTab.setMinimumSize(new java.awt.Dimension(750, 400));
        mCamTab.setPreferredSize(new java.awt.Dimension(1000, 800));
        mCamTab.setLayout(new java.awt.GridBagLayout());

        mIPCamPanel.setFps(20);
        mIPCamPanel.setMinimumSize(new java.awt.Dimension(600, 350));
        mIPCamPanel.setUrlAddrress("");

        jButton3_moveIPCamRight.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jButton3_moveIPCamRight.setForeground(new java.awt.Color(255, 0, 51));
        jButton3_moveIPCamRight.setText("right");
        jButton3_moveIPCamRight.setBorder(javax.swing.BorderFactory.createEtchedBorder(new java.awt.Color(0, 0, 0), null));
        jButton3_moveIPCamRight.setContentAreaFilled(false);
        jButton3_moveIPCamRight.setPreferredSize(new java.awt.Dimension(80, 40));
        jButton3_moveIPCamRight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3_moveIPCamRightActionPerformed(evt);
            }
        });

        jTextField9.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jTextField9.setText("slider value");

        jSlider_moveDuration.setMaximum(1500);
        jSlider_moveDuration.setMinimum(1);
        jSlider_moveDuration.setMinorTickSpacing(50);
        jSlider_moveDuration.setOrientation(javax.swing.JSlider.VERTICAL);
        jSlider_moveDuration.setPaintLabels(true);
        jSlider_moveDuration.setPaintTicks(true);
        jSlider_moveDuration.setToolTipText("Increase or Decrease the duration of camera movement.");
        jSlider_moveDuration.setValue(500);
        jSlider_moveDuration.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSlider_moveDurationStateChanged(evt);
            }
        });

        jButton3_moveIPCamLeft.setBackground(new java.awt.Color(242, 240, 240));
        jButton3_moveIPCamLeft.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jButton3_moveIPCamLeft.setForeground(new java.awt.Color(255, 0, 51));
        jButton3_moveIPCamLeft.setText("left");
        jButton3_moveIPCamLeft.setBorder(javax.swing.BorderFactory.createEtchedBorder(new java.awt.Color(0, 0, 0), null));
        jButton3_moveIPCamLeft.setContentAreaFilled(false);
        jButton3_moveIPCamLeft.setPreferredSize(new java.awt.Dimension(80, 40));
        jButton3_moveIPCamLeft.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3_moveIPCamLeftActionPerformed(evt);
            }
        });

        jButton3_moveIPCamDown.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jButton3_moveIPCamDown.setForeground(new java.awt.Color(255, 0, 51));
        jButton3_moveIPCamDown.setText("down");
        jButton3_moveIPCamDown.setBorder(javax.swing.BorderFactory.createEtchedBorder(new java.awt.Color(0, 0, 0), null));
        jButton3_moveIPCamDown.setContentAreaFilled(false);
        jButton3_moveIPCamDown.setPreferredSize(new java.awt.Dimension(80, 40));
        jButton3_moveIPCamDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3_moveIPCamDownActionPerformed(evt);
            }
        });

        jButton3_moveIPCamUp.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jButton3_moveIPCamUp.setForeground(new java.awt.Color(255, 0, 51));
        jButton3_moveIPCamUp.setText("up");
        jButton3_moveIPCamUp.setBorder(javax.swing.BorderFactory.createEtchedBorder(new java.awt.Color(0, 0, 0), null));
        jButton3_moveIPCamUp.setContentAreaFilled(false);
        jButton3_moveIPCamUp.setPreferredSize(new java.awt.Dimension(80, 40));
        jButton3_moveIPCamUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3_moveIPCamUpActionPerformed(evt);
            }
        });

        mComboBoxCams.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        mComboBoxCams.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel1.setText("Camara to adjust:");

        jTextField_presetName.setText("New Preset Name");

        jButton2.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jButton2.setText("Create New Preset");
        jButton2.setBorder(javax.swing.BorderFactory.createEtchedBorder(new java.awt.Color(0, 0, 0), null));
        jButton2.setContentAreaFilled(false);
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButtonIPCameraPTZ_get_presets.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jButtonIPCameraPTZ_get_presets.setText("Refresh Presets List.");
        jButtonIPCameraPTZ_get_presets.setBorder(javax.swing.BorderFactory.createEtchedBorder(new java.awt.Color(0, 0, 0), null));
        jButtonIPCameraPTZ_get_presets.setContentAreaFilled(false);
        jButtonIPCameraPTZ_get_presets.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonIPCameraPTZ_get_presetsActionPerformed(evt);
            }
        });

        jButton3.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jButton3.setText("Go to an existing Preset");
        jButton3.setBorder(javax.swing.BorderFactory.createEtchedBorder(new java.awt.Color(0, 0, 0), null));
        jButton3.setContentAreaFilled(false);
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jTextField10GoToPreset.setText("Manually enter Existing Preset Name here");

        list1_ip_cam_presets.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        list1_ip_cam_presets.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                list1_ip_cam_presetsMouseClicked(evt);
            }
        });

        jLabel9.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel9.setText("Presets List:");
        jLabel9.setToolTipText("Press the \"Get Existing Presets\" button to update this list.");

        jButtonZoomIn.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jButtonZoomIn.setForeground(new java.awt.Color(255, 0, 51));
        jButtonZoomIn.setText("Zoom In");
        jButtonZoomIn.setBorder(javax.swing.BorderFactory.createEtchedBorder(new java.awt.Color(0, 0, 0), null));
        jButtonZoomIn.setContentAreaFilled(false);
        jButtonZoomIn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jButtonZoomInMousePressed(evt);
            }
        });
        jButtonZoomIn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonZoomInActionPerformed(evt);
            }
        });

        jButtonZoomOut.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jButtonZoomOut.setForeground(new java.awt.Color(255, 0, 51));
        jButtonZoomOut.setText("Zoom Out");
        jButtonZoomOut.setBorder(javax.swing.BorderFactory.createEtchedBorder(new java.awt.Color(0, 0, 0), null));
        jButtonZoomOut.setContentAreaFilled(false);
        jButtonZoomOut.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jButtonZoomOutMousePressed(evt);
            }
        });
        jButtonZoomOut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonZoomOutActionPerformed(evt);
            }
        });

        mCameraListBox.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N
        mCameraListBox.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                mCameraListBoxMouseClicked(evt);
            }
        });
        mCameraListBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mCameraListBoxActionPerformed(evt);
            }
        });

        jButtonDeletePreset.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jButtonDeletePreset.setText("Delete a Preset");
        jButtonDeletePreset.setBorder(javax.swing.BorderFactory.createEtchedBorder(new java.awt.Color(0, 0, 0), null));
        jButtonDeletePreset.setContentAreaFilled(false);
        jButtonDeletePreset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDeletePresetActionPerformed(evt);
            }
        });

        jTextFieldDeletePreset.setText("Preset to Delete");

        jSlider2.setMaximum(2000);
        jSlider2.setMinimum(10);
        jSlider2.setMinorTickSpacing(100);
        jSlider2.setPaintLabels(true);
        jSlider2.setPaintTicks(true);
        jSlider2.setToolTipText(jSlider2.toString());
        jSlider2.setValue(600);
        jSlider2.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSlider2StateChanged(evt);
            }
        });

        jLabel10.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel10.setText("Image Save Lag");

        javax.swing.GroupLayout mIPCamPanelLayout = new javax.swing.GroupLayout(mIPCamPanel);
        mIPCamPanel.setLayout(mIPCamPanelLayout);
        mIPCamPanelLayout.setHorizontalGroup(
            mIPCamPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mIPCamPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mIPCamPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mIPCamPanelLayout.createSequentialGroup()
                        .addComponent(list1_ip_cam_presets, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(mIPCamPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextField10GoToPreset, javax.swing.GroupLayout.PREFERRED_SIZE, 248, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 248, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButtonIPCameraPTZ_get_presets, javax.swing.GroupLayout.PREFERRED_SIZE, 179, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mIPCamPanelLayout.createSequentialGroup()
                        .addGroup(mIPCamPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButtonZoomIn, javax.swing.GroupLayout.PREFERRED_SIZE, 153, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButtonZoomOut, javax.swing.GroupLayout.PREFERRED_SIZE, 153, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(133, 133, 133)
                        .addComponent(jButton3_moveIPCamLeft, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(mIPCamPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(mIPCamPanelLayout.createSequentialGroup()
                        .addGroup(mIPCamPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mIPCamPanelLayout.createSequentialGroup()
                                .addGroup(mIPCamPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jButton3_moveIPCamUp, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jButton3_moveIPCamDown, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(115, 115, 115))
                            .addGroup(mIPCamPanelLayout.createSequentialGroup()
                                .addGap(34, 34, 34)
                                .addGroup(mIPCamPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(jTextFieldDeletePreset)
                                    .addComponent(jTextField_presetName, javax.swing.GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                    .addComponent(jButton2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 177, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jButtonDeletePreset, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 177, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                        .addGroup(mIPCamPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(mIPCamPanelLayout.createSequentialGroup()
                                .addComponent(mComboBoxCams, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mIPCamPanelLayout.createSequentialGroup()
                                .addGap(0, 27, Short.MAX_VALUE)
                                .addGroup(mIPCamPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(mIPCamPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(mCameraListBox, javax.swing.GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE)
                                        .addComponent(jSlider2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))))
                    .addGroup(mIPCamPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 176, Short.MAX_VALUE)
                        .addGroup(mIPCamPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jTextField9, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(mIPCamPanelLayout.createSequentialGroup()
                                .addComponent(jButton3_moveIPCamRight, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(187, 187, 187)
                                .addComponent(jSlider_moveDuration, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addGap(21, 21, 21))
        );
        mIPCamPanelLayout.setVerticalGroup(
            mIPCamPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mIPCamPanelLayout.createSequentialGroup()
                .addGroup(mIPCamPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mIPCamPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jTextField9, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(30, 30, 30)
                        .addComponent(jSlider_moveDuration, javax.swing.GroupLayout.PREFERRED_SIZE, 232, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(mIPCamPanelLayout.createSequentialGroup()
                        .addGap(138, 138, 138)
                        .addComponent(jButtonZoomIn, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jButtonZoomOut, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(mIPCamPanelLayout.createSequentialGroup()
                        .addGap(35, 35, 35)
                        .addComponent(jButton3_moveIPCamUp, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(mIPCamPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton3_moveIPCamRight, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton3_moveIPCamLeft, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGroup(mIPCamPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mIPCamPanelLayout.createSequentialGroup()
                        .addGap(41, 41, 41)
                        .addGroup(mIPCamPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(mIPCamPanelLayout.createSequentialGroup()
                                .addGap(16, 16, 16)
                                .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jSlider2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(5, 5, 5)
                                .addComponent(mCameraListBox, javax.swing.GroupLayout.PREFERRED_SIZE, 172, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(mComboBoxCams, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(mIPCamPanelLayout.createSequentialGroup()
                                .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(list1_ip_cam_presets, javax.swing.GroupLayout.PREFERRED_SIZE, 348, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))))
                    .addGroup(mIPCamPanelLayout.createSequentialGroup()
                        .addGap(26, 26, 26)
                        .addComponent(jButton3_moveIPCamDown, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(mIPCamPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(mIPCamPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jTextField10GoToPreset, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField_presetName, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(mIPCamPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButtonIPCameraPTZ_get_presets, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButtonDeletePreset, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldDeletePreset, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addGap(6, 6, 6))
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 10.0;
        mCamTab.add(mIPCamPanel, gridBagConstraints);

        lblIPConStatus.setBackground(new java.awt.Color(178, 115, 52));
        lblIPConStatus.setText("CamStatus");
        lblIPConStatus.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        lblIPConStatus.setOpaque(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        mCamTab.add(lblIPConStatus, gridBagConstraints);

        mMainTabbedPane.addTab("Camera", mCamTab);

        jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3, javax.swing.BoxLayout.Y_AXIS));

        lblPhidgetChannel.setText("Phidget Channels");
        lblPhidgetChannel.setAlignmentX(0.5F);
        lblPhidgetChannel.setAlignmentY(0.0F);
        jPanel3.add(lblPhidgetChannel);

        cmbPhidgetChannels.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbPhidgetChannelsActionPerformed(evt);
            }
        });
        jPanel3.add(cmbPhidgetChannels);

        lblPhidgetParamName.setText("Parameter Name");
        lblPhidgetParamName.setAlignmentX(0.5F);
        jPanel3.add(lblPhidgetParamName);

        jPanel3.add(cmbPhidgetParamNames);

        lblPhidgetParamTargetValue.setText("Parameter Target Value");
        lblPhidgetParamTargetValue.setAlignmentX(0.5F);
        jPanel3.add(lblPhidgetParamTargetValue);

        txtPhidgetParamTargetValue.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0.00"))));
        txtPhidgetParamTargetValue.setText("0");
        jPanel3.add(txtPhidgetParamTargetValue);

        btnSetValue.setText("Set Value");
        btnSetValue.setAlignmentX(0.5F);
        btnSetValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSetValueActionPerformed(evt);
            }
        });
        jPanel3.add(btnSetValue);

        btnGetValue.setText("Get Value");
        btnGetValue.setAlignmentX(0.5F);
        btnGetValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGetValueActionPerformed(evt);
            }
        });
        jPanel3.add(btnGetValue);

        jPanel2.add(jPanel3);

        jSlider1.setMajorTickSpacing(2500);
        jSlider1.setMaximum(10000);
        jSlider1.setMinimum(-10000);
        jSlider1.setMinorTickSpacing(500);
        jSlider1.setPaintLabels(true);
        jSlider1.setPaintTicks(true);
        jSlider1.setSnapToTicks(true);
        jSlider1.setToolTipText("");
        jSlider1.setValue(0);
        jSlider1.setPreferredSize(new java.awt.Dimension(350, 55));
        jSlider1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jSlider1MouseReleased(evt);
            }
        });
        jPanel2.add(jSlider1);

        jButton1.setText("jButton1");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton1);

        mMainTabbedPane.addTab("Phidget", jPanel2);

        jPanelVehicle.setLayout(new java.awt.GridBagLayout());

        mTruckSteerPanel.setPreferredSize(new java.awt.Dimension(600, 600));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanelVehicle.add(mTruckSteerPanel, gridBagConstraints);

        mPanelCharts.setMinimumSize(new java.awt.Dimension(350, 0));
        mPanelCharts.setLayout(new java.awt.GridLayout(2, 2));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanelVehicle.add(mPanelCharts, gridBagConstraints);

        mMainTabbedPane.addTab("Vehicle", jPanelVehicle);

        jTextField1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jTextField1.setText("100");
        jTextField1.setMinimumSize(new java.awt.Dimension(30, 50));

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Left");

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel3.setText("Front Speed");

        jLabel4.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel4.setText("Front Steer");

        jTextField2.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jTextField2.setText("100");

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setText("Values represent % of value coded into application.");
        jLabel5.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jTextField3.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jTextField3.setText("100");

        jTextField4.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jTextField4.setText("100");

        jLabel6.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel6.setText("Rear Speed");

        jLabel7.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel7.setText("Rear Steer");

        jLabel8.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel8.setText("Right");

        jTextField5.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jTextField5.setText("100");
        jTextField5.setMinimumSize(new java.awt.Dimension(20, 20));

        jTextField6.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jTextField6.setText("100");
        jTextField6.setMinimumSize(new java.awt.Dimension(20, 20));

        jTextField7.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jTextField7.setText("100");
        jTextField7.setMinimumSize(new java.awt.Dimension(20, 20));

        jTextField8.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jTextField8.setText("100");
        jTextField8.setMinimumSize(new java.awt.Dimension(20, 20));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 295, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(27, 27, 27)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(27, 27, 27)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jTextField1, javax.swing.GroupLayout.DEFAULT_SIZE, 96, Short.MAX_VALUE)
                            .addComponent(jTextField2)
                            .addComponent(jTextField3)
                            .addComponent(jTextField4))
                        .addGap(80, 80, 80)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextField5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jTextField6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jTextField7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 110, Short.MAX_VALUE))
                            .addComponent(jTextField8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addGap(474, 474, 474))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(16, 16, 16)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(43, 43, 43)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(123, 123, 123)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(123, 123, 123)
                        .addComponent(jTextField7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jTextField8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(298, Short.MAX_VALUE))
        );

        mMainTabbedPane.addTab("Tuning Parameters", jPanel1);

        mMainTabbedPane.setSelectedIndex(2);

        getContentPane().add(mMainTabbedPane);

        mPanelStatusInfo.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        mBtnEnableGamePad.setText("GamePad Active");
        mBtnEnableGamePad.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        mBtnEnableGamePad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBtnEnableGamePadActionPerformed(evt);
            }
        });
        mPanelStatusInfo.add(mBtnEnableGamePad);

        mLblGamePadStatus.setText("GamePad Status: ");
        mLblGamePadStatus.setAlignmentX(0.5F);
        mPanelStatusInfo.add(mLblGamePadStatus);

        mLblGamePadStatusValue.setText("<None>");
        mPanelStatusInfo.add(mLblGamePadStatusValue);

        getContentPane().add(mPanelStatusInfo);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void mBtnExitAppActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBtnExitAppActionPerformed
        cleanUpOnClose();
        System.exit(0);
    }//GEN-LAST:event_mBtnExitAppActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

        targetValue_int=targetValue_int+300;// = txtPhidgetParamTargetValue.getText();
        String channelName = cmbPhidgetChannels.getSelectedItem().toString();
        String paramName = cmbPhidgetParamNames.getSelectedItem().toString();
        targetValue = (String)(targetValue_int+"");
        System.out.println("targetValue - "+targetValue);
        mDeviceManager.setChannelParam(
            channelName,
            paramName,
            Float.parseFloat(targetValue)
        );
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jSlider1MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jSlider1MouseReleased
        targetValue_int=jSlider1.getValue();
        String channelName = cmbPhidgetChannels.getSelectedItem().toString();
        String paramName = cmbPhidgetParamNames.getSelectedItem().toString();
        targetValue = (String)(targetValue_int+"");
        System.out.println("targetValue - "+targetValue);
        mDeviceManager.setChannelParam(
            channelName,
            paramName,
            Float.parseFloat(targetValue)
        );
    }//GEN-LAST:event_jSlider1MouseReleased

    private void btnGetValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGetValueActionPerformed
        if (cmbPhidgetChannels.getSelectedItem() == null) return;
        if (cmbPhidgetParamNames.getSelectedItem() == null) return;
        String channelName = cmbPhidgetChannels.getSelectedItem().toString();
        String paramName = cmbPhidgetParamNames.getSelectedItem().toString();
        String targetValue = txtPhidgetParamTargetValue.getText();
        mDeviceManager.getChannelParam(
            channelName,
            paramName,
            new PhiDevice.ParamRunnable.Reader() {
                @Override
                public void onRead(double value) {
                    txtPhidgetParamTargetValue.setText(Double.toString(value));
                }
            }
        );
    }//GEN-LAST:event_btnGetValueActionPerformed

    private void btnSetValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSetValueActionPerformed
        if (cmbPhidgetChannels.getSelectedItem() == null) return;
        if (cmbPhidgetParamNames.getSelectedItem() == null) return;
        String channelName = cmbPhidgetChannels.getSelectedItem().toString();
        String paramName = cmbPhidgetParamNames.getSelectedItem().toString();
        String targetValue = txtPhidgetParamTargetValue.getText();
        mDeviceManager.setChannelParam(
            channelName,
            paramName,
            Float.parseFloat(targetValue)
        );
    }//GEN-LAST:event_btnSetValueActionPerformed

    private void cmbPhidgetChannelsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbPhidgetChannelsActionPerformed
        if (cmbPhidgetChannels.getSelectedItem() == null) return;
        String channelName = cmbPhidgetChannels.getSelectedItem().toString();
        DeviceChannel channel = mDeviceManager.getChannelByName(channelName);
        System.out.println(channelName+"_<- channelName______channel ->__"+channel);
        if (channel != null) {
            ChannelType channelType = ChannelType.getChannelTypeByName(channel.getChannelName());
            if (channelType != null) {
                String[] paramNames = channelType.getParamNames();
                cmbPhidgetParamNames.removeAll();
                for(String paramName: paramNames) {
                    cmbPhidgetParamNames.addItem(paramName);
                }
            }
        }
    }//GEN-LAST:event_cmbPhidgetChannelsActionPerformed

    private void jButton3_moveIPCamRightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3_moveIPCamRightActionPerformed
        int moveDuration = 1;
        String moveDirection = "Right";
        moveDuration = jSlider_moveDuration.getValue();
        updateIPCameraPOS_Settings(moveDuration, moveDirection);
    }//GEN-LAST:event_jButton3_moveIPCamRightActionPerformed

    private void jButton3_moveIPCamLeftActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3_moveIPCamLeftActionPerformed
        int moveDuration = 1;
        String moveDirection = "Left";
        moveDuration = jSlider_moveDuration.getValue();
        updateIPCameraPOS_Settings(moveDuration, moveDirection);
    }//GEN-LAST:event_jButton3_moveIPCamLeftActionPerformed

    private void jButton3_moveIPCamDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3_moveIPCamDownActionPerformed
        int moveDuration = 1;
        String moveDirection = "Down";
        moveDuration = jSlider_moveDuration.getValue();
        updateIPCameraPOS_Settings(moveDuration, moveDirection);
    }//GEN-LAST:event_jButton3_moveIPCamDownActionPerformed

    private void jButton3_moveIPCamUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3_moveIPCamUpActionPerformed
        int moveDuration = 1;
        String moveDirection = "Up";
        moveDuration = jSlider_moveDuration.getValue();
        updateIPCameraPOS_Settings(moveDuration, moveDirection);
    }//GEN-LAST:event_jButton3_moveIPCamUpActionPerformed

    private void jSlider_moveDurationStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSlider_moveDurationStateChanged
        String sliderValueString = jSlider_moveDuration.getValue()+"";
        jTextField9.setText(sliderValueString);
    }//GEN-LAST:event_jSlider_moveDurationStateChanged

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        IPCameraPTZ_add_preset();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButtonIPCameraPTZ_get_presetsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonIPCameraPTZ_get_presetsActionPerformed
        IPCameraPTZ_get_presets();
    }//GEN-LAST:event_jButtonIPCameraPTZ_get_presetsActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        IPCameraPTZ_goto_preset();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void list1_ip_cam_presetsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_list1_ip_cam_presetsMouseClicked
        System.out.println("PTZ to: "+list1_ip_cam_presets.getSelectedItem());
        IPCameraPTZ_goto_preset();
    }//GEN-LAST:event_list1_ip_cam_presetsMouseClicked

    private void jButtonZoomInActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonZoomInActionPerformed
        int moveDuration = 1;
        String moveDirection = "ZoomIn";
        moveDuration = jSlider_moveDuration.getValue();
        zoomInOut( moveDuration,  moveDirection);
    }//GEN-LAST:event_jButtonZoomInActionPerformed

    private void jButtonZoomOutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonZoomOutActionPerformed
        int moveDuration = 1;
        String moveDirection = "ZoomOut";
        moveDuration = jSlider_moveDuration.getValue();
        zoomInOut( moveDuration,  moveDirection);
    }//GEN-LAST:event_jButtonZoomOutActionPerformed

    private void mCameraListBoxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mCameraListBoxMouseClicked
        IPCamSetting camSetting = mCamSettingMap.get(mCameraListBox.getSelectedItem()+"");
        String ipAddr = camSetting.getIpAddress();
        mIPCamPanel.setUrlAddrress(camSetting.getPictureUrl());
        //System.out.println("camera ip slected for movement: "+ ipAddr+"\n"+camSetting.getPictureUrl().substring(0, 30));
        if (ipAddr!=null && ipAddr != "null"){
            IPCameraPTZ_get_presets();
        }
    }//GEN-LAST:event_mCameraListBoxMouseClicked

    private void jButtonDeletePresetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDeletePresetActionPerformed
        IPCameraPTZ_delete_preset();
    }//GEN-LAST:event_jButtonDeletePresetActionPerformed

    private void jSlider2StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSlider2StateChanged
        //String sliderValueString = jSlider2.getValue()+"";
        imageSaveTimeLag = jSlider2.getValue();
        jLabel10.setText(imageSaveTimeLag+"");
        mIPCamPanel.setImageSaveLag(imageSaveTimeLag);
    }//GEN-LAST:event_jSlider2StateChanged

    private void mCameraListBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mCameraListBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_mCameraListBoxActionPerformed

    private void jButtonZoomInMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButtonZoomInMousePressed
        int moveDuration = 1;
        String moveDirection = "ZoomIn";
        moveDuration = jSlider_moveDuration.getValue();
        zoomInOut( moveDuration,  moveDirection);
    }//GEN-LAST:event_jButtonZoomInMousePressed

    private void jButtonZoomOutMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButtonZoomOutMousePressed
        int moveDuration = 1;
        String moveDirection = "ZoomOut";
        moveDuration = jSlider_moveDuration.getValue();
        zoomInOut( moveDuration,  moveDirection);
    }//GEN-LAST:event_jButtonZoomOutMousePressed

    private void mBtnEnableGamePadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBtnEnableGamePadActionPerformed
        mGamePadUpdater.setActive(mBtnEnableGamePad.isSelected());
    }//GEN-LAST:event_mBtnEnableGamePadActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnGetValue;
    private javax.swing.JButton btnSetValue;
    private javax.swing.JComboBox<String> cmbPhidgetChannels;
    private javax.swing.JComboBox<String> cmbPhidgetParamNames;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton3_moveIPCamDown;
    private javax.swing.JButton jButton3_moveIPCamLeft;
    private javax.swing.JButton jButton3_moveIPCamRight;
    private javax.swing.JButton jButton3_moveIPCamUp;
    private javax.swing.JButton jButtonDeletePreset;
    private javax.swing.JButton jButtonIPCameraPTZ_get_presets;
    private javax.swing.JButton jButtonZoomIn;
    private javax.swing.JButton jButtonZoomOut;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanelVehicle;
    private javax.swing.JSlider jSlider1;
    private javax.swing.JSlider jSlider2;
    private javax.swing.JSlider jSlider_moveDuration;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField10GoToPreset;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    private javax.swing.JTextField jTextField8;
    private javax.swing.JTextField jTextField9;
    private javax.swing.JTextField jTextFieldDeletePreset;
    private javax.swing.JTextField jTextField_presetName;
    private javax.swing.JLabel lblIPConStatus;
    private javax.swing.JLabel lblPhidgetChannel;
    private javax.swing.JLabel lblPhidgetParamName;
    private javax.swing.JLabel lblPhidgetParamTargetValue;
    private java.awt.List list1_ip_cam_presets;
    private javax.swing.JToggleButton mBtnEnableGamePad;
    private javax.swing.JButton mBtnExitApp;
    private javax.swing.JPanel mCamTab;
    private java.awt.List mCameraListBox;
    private javax.swing.JComboBox<String> mComboBoxCams;
    private RoboCam.IPCamPanel mIPCamPanel;
    private javax.swing.JLabel mLblGamePadStatus;
    private javax.swing.JLabel mLblGamePadStatusValue;
    private javax.swing.JTabbedPane mMainTabbedPane;
    private javax.swing.JPanel mPanelCharts;
    private javax.swing.JPanel mPanelStatusInfo;
    private RoverUI.TruckSteerPanel mTruckSteerPanel;
    private javax.swing.JFormattedTextField txtPhidgetParamTargetValue;
    // End of variables declaration//GEN-END:variables

}
