package RoboCam;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.Thread.sleep;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import Utility.UiLine;
/**
 *
 * @author sujoy
 */
public class Config {
    static final String CONFIG_FOLDER = ".robocam-config";

    private int mStepperPositionAbsReadSpan = 100000;
    private int mStepperPositionAbsWriteSpan = 100000;//This is to protect too large rotation
    private double mSteppCurrentLimit = 2;
    private Properties mProps;
    private String mComputerNetworkLocation = "initialized"; //"ExternalNetwork FKA "House" or "LocalNetwork" FKA "Barn"
    private double mWheelVerticalAngleIncrement = 10;//in degree
    private String mIpCamPrefix;

    public Config(String filepath) {
        mProps = new Properties();
        try {
            mProps.load(new FileInputStream(filepath));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        }

        String prop;

        prop = mProps.getProperty("ComputerNetworkLocation", "");
        if (prop.length() > 0) {
            mComputerNetworkLocation = (prop);
            System.out.println("Network Location per Config.java = "+mComputerNetworkLocation);
        }

        prop = mProps.getProperty("Stepper.Position.Absolute.Read.Span", "");
        if (prop.length() > 0) {
            mStepperPositionAbsReadSpan = Integer.parseInt(prop);
        }

        prop = mProps.getProperty("Stepper.Position.Absolute.Write.Span", "");
        if (prop.length() > 0) {
            mStepperPositionAbsWriteSpan = Integer.parseInt(prop);
        }

        prop = mProps.getProperty("StepperCurrentLimit", "");
        if (prop.length() > 0) {
            mSteppCurrentLimit = Double.parseDouble(prop);
        }

        prop = mProps.getProperty("WheelVerticalAngleIncrement", "");
        if (prop.length() > 0) {
            mWheelVerticalAngleIncrement = Double.parseDouble(prop);
        }

        mIpCamPrefix = mProps.getProperty("IPCam.Prefix", "");
    }

    public double getWheelVerticalAngleIncrement() {
        return mWheelVerticalAngleIncrement;
    }

    public double convertToStepperPos(double pos) {
//        System.out.println(String.format("pos %f", pos));
        pos = Math.min(Math.max(-mStepperPositionAbsWriteSpan,
                        -mStepperPositionAbsWriteSpan + 2*mStepperPositionAbsWriteSpan*pos
                ),
                mStepperPositionAbsWriteSpan
        );
//        System.out.println(String.format("convert to stepepr %f", pos));
//        try {
//            sleep(500);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
//        }
        return pos;
    }

    public double convertFromStepperPos(double pos) {
        //System.out.println(String.format("convert from stepepr %f", pos));
        return 360*(((pos-mStepperPositionAbsReadSpan)/(2.0*mStepperPositionAbsReadSpan)) % 1.0);
    }

    public double getStepperCurrentLimit() {
        return mSteppCurrentLimit;
    }

    public String[] getIPCamNames() {
        if(mComputerNetworkLocation.equals("ExternalNetwork")) {
            return mProps.getProperty("houseIPCam.Names", "").split(",");
        } else {
            String camNames = mProps.getProperty(mIpCamPrefix+"IPCam.Names");
            if (camNames == null) {
                camNames = mProps.getProperty("IPCam.Names");
            }
            if (camNames == null) camNames = "";
            return camNames.split(",");
        }
    }

    public String getIPCamAddress(String name) {
        name = name.trim();
        
        if(mComputerNetworkLocation.equals("ExternalNetwork")){
            return mProps.getProperty("houseIPCam."+name+".Addr");
        } else  {
            String addr = mProps.getProperty(mIpCamPrefix+"IPCam."+name+".Addr");
            if (addr == null) {
                addr = mProps.getProperty("IPCam."+name+".Addr");
            }
            if (addr == null) addr = "";
            return addr;
        }
    }

    public String getIPCamUser(String name) {
        if(mComputerNetworkLocation.equals("ExternalNetwork")){
            return mProps.getProperty("houseIPCam."+name+".User");
        } else {
            String user = mProps.getProperty(mIpCamPrefix+"IPCam."+name+".User");
            if (user == null) {
                user = mProps.getProperty("IPCam."+name+".User");
            }
            if (user == null) user = "";
            return user;
        }
    }

    public String getIPCamPassword(String name) {
        String ggg = mProps.getProperty("ComputerNetworkLocation");
        System.out.println(ggg);
        if(mComputerNetworkLocation.equals("ExternalNetwork")){
            return mProps.getProperty("houseIPCam."+name+".Pwd");
        } else {
            String passwd = mProps.getProperty(mIpCamPrefix+"IPCam."+name+".Pwd");
            if (passwd == null) {
                passwd = mProps.getProperty("IPCam."+name+".Pwd");
            }
            if (passwd == null) passwd = "";
            return passwd;
        }
    }

    public boolean hasIPCam(String name) {
        return (getIPCamAddress(name) != null);
    }

    public String getIPCamUrl(String name) {
        name = name.trim();
        String url = "http://%s/cgi-bin/CGIProxy.fcgi?cmd=snapPicture2&usr=%s&pwd=%s";
        url = String.format(
                url, getIPCamAddress(name),
                getIPCamUser(name), getIPCamPassword(name));
        return url;
    }
    
    public String getIPCamUrl(String name, String extraQueryString) {
        name = name.trim();
        String url = "http://%s/cgi-bin/CGIProxy.fcgi?usr=%s&pwd=%s&"+extraQueryString;
        url = String.format(
                url, getIPCamAddress(name),
                getIPCamUser(name), getIPCamPassword(name));
        return url;
    }
    
    public String getIPCamUrlFromAddr(String ipAddr, String user, String pwd) {
        ipAddr = ipAddr.trim();
        String url = "http://%s/cgi-bin/CGIProxy.fcgi?cmd=snapPicture2&usr=%s&pwd=%s";
        url = String.format(url, ipAddr, user, pwd);
        return url;
    }
    
    public void setIPCamPosition(){
        // https://www.safaribooksonline.com/library/view/learning-java/1565927184/ch12s04.html
        URL url = null;
        for(String ipCamName: getIPCamNames()) {
            try {
                url = new URL(getIPCamUrl(ipCamName, "cmd=ptzGotoPresetPoint&name=begPoint"));

                //System.out.println("GotoPresetPoing here - Cam URL:" + ipCamName + " " + url);
                
            } catch (MalformedURLException ex) {
                Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
            }
            BufferedReader bin = null;
            try {
                bin = new BufferedReader ( 
                        new InputStreamReader( url.openStream( ) ));
            } catch (IOException ex) {
                System.err.println("cam not working? is it connected? "+url);            
                //Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
//        String line; 
//        try {
//            while ( (line = bin.readLine( )) != null )
//                System.out.println("linetest "+url +" /n"+ line );
//        } catch (IOException ex) {
//            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }
    

    public String getDefaultDevice() {
        return mProps.getProperty("Default-Device", "");
    }

    public String getMachineName() {
        return mProps.getProperty("Machine-Name", "UI");
    }

    public String getRoverHost() {
        return mProps.getProperty("Rover-Host", "localhost");
    }

    public int getRoverPort() {
        return Integer.parseInt(mProps.getProperty("Rover-Port", "5001"));
    }
    
    public String getWheelMotorParam(String wheelName, String motorType, String param) {
        return mProps.getProperty(wheelName+"." + motorType + "Motor." + param);
    }

    public String getConfigDBPath() {
        return mProps.getProperty("ConfigDBPath", "robocam_config.db");
    }

    /**
     *
     * for computers on the local network that are trying to access the ip cameras the network ip addresses will be something like 192.173.1.xx
     * for computers on a external network the ip addressed of the ip cameras will be something like 98.173.227.xx (as set by port forwarding)
     */
    public String getConfigComputerNetworkLocation () {
        return mProps.getProperty("ConfigDBPath", "robocam_config.db");
    }

    public boolean hasMySQL() {
        return mProps.getProperty("MySQL.Allow", "").trim().equals("true");
    }
    
    public double getFailSafeDelay(double defaultValue) {
        try {
            return Double.parseDouble(
                mProps.getProperty("FailSafe.Seconds"));
        } catch (NullPointerException ex) {
            return defaultValue;
        } catch (NumberFormatException ex) {
           return defaultValue;
        }
    }

    /**
     * Returns the pattern to filter Phidget list
     */
    public String getPhidgetPatternFor (String phidgetType) {
        return mProps.getProperty("Phidget.Pattern." + phidgetType, "");
    }
    
    /**
     * Returns if fake phidget can be used.
     */
    public boolean getPhidgetUseFake () {
        return mProps.getProperty("Phidget.UseFake", "").trim().equals("true");
    }
    
    /**
     * Returns the folder where IP cam images will be saved. 
     * @return image folder path
    */
    public String getImageFolder() {
        String path = mProps.getProperty("ImageFolder", "").trim();
        if (path.length() ==0 ) path = null;
        System.out.println("path " + path);
        return path;
    }
    
    public static File getConfigFile(String fileName) {
        File rootFolder = new File(
                System.getProperty("user.dir") +
                      File.separator +  CONFIG_FOLDER);
        if (!rootFolder.exists()) {
            rootFolder.mkdirs();
        }
        return new File(
            rootFolder.getAbsolutePath() + File.separator +  fileName
        );
    }

    public double getTruckDrawingScale() {
        return getDoublePropValue("Truck.DrawScale", 2);
    }
    
    public double getGamePadVehicleLeftRightTolerance() {
        return getDoublePropValue("GamePad.VehicleLeftRightTolerance", 0.1);
    }
    
    public double getGamePadVehicleLeftRightMultiplier() {
        return getDoublePropValue("GamePad.VehicleLeftRightMultiplier", 1);
    }
    
    public double getDoublePropValue(String propName, double defaultValue) {
        try {
            return Double.parseDouble(
                mProps.getProperty(propName));
        } catch (NullPointerException ex) {
            return defaultValue;
        } catch (NumberFormatException ex) {
           return defaultValue;
        }
    }
}
