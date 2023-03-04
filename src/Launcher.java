import RoboCam.Config;
import RoboCam.RoverFrontEnd;
import RoboCam.UIFrontEnd;
import com.phidget22.PhidgetException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author sujoy
 */
public class Launcher {
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(UIFrontEnd.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(UIFrontEnd.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(UIFrontEnd.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(UIFrontEnd.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        System.out.println("Possible Future Updates");
        System.out.println(" - if one motor drops out (resets position to zero) the average actual position "
                + "changes and 'distance remaining' changes");
        System.out.println(" - if using remote gui and then switching to on board gui the target position "
                + "hasn't been updated which causes it to attempt to drive to zero target position immediately");

        String configFilePath = "robo-config.ini";
        String machineName = null;
        int roverPort = 0;
        String roverHost = null;
//        String fileTimestamp = "phidgetlog"+(new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss").format(new Date()))+".log";
//        System.err.println("lgger file name: "+fileTimestamp);
//        try {
//            Log.enable(LogLevel.VERBOSE, "phidgetlog"+fileTimestamp+".log");
//        } catch (PhidgetException ex) {
//            Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        try {
//            Log.enable(LogLevel.VERBOSE, fileTimestamp); // this is a phidget logger; not the java logger.
//        } catch (PhidgetException ex) {
//            Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
//        }
        /* Parse the command line arguments and set parameters */
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--machine":
                    machineName = args[i + 1];
                    i += 1;
                    break;
                case "--roverhost":
                    roverHost = args[i + 1];
                    i += 1;
                    break;
                case "--roverport":
                    roverPort = Integer.parseInt(args[i + 1]);
                    i += 1;
                    break;
                case "--config":
                    configFilePath = args[i + 1];
                    i += 1;
                    break;
                default:
                    break;
            }
        }

        /* Load config and set parameters from it */
        Config config = new Config(configFilePath);
        if (machineName == null) {
            machineName = config.getMachineName();
        }
        if (roverPort == 0) {
            roverPort = config.getRoverPort();
        }
        if (roverHost == null) {
            roverHost = config.getRoverHost();
        }

        System.out.println("the '--machine' command line arg = " + machineName + " (options are UI or Rover");

        whatIsMyIP(machineName);

        if (machineName.equals("UI")) {
            System.out.println("UI Machine " + roverHost + " " + roverPort);
            try {
                UIFrontEnd uiFrontEnd = new UIFrontEnd(roverHost, roverPort);
                uiFrontEnd.setVisible(true);
                uiFrontEnd.loadFromConfig(config);
                uiFrontEnd.setMachineName(machineName);
            } catch (IOException ex) {
                Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
            } catch (PhidgetException ex) {
                Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (machineName.equals("Rover")) {
            try {
                System.out.println("Rover Machine roverPort = " + roverPort + ". Make sure the ip address in the robo-config.ini on rover is also port forwarded in router that rover connects to.\n");
                RoverFrontEnd roverFrontEnd = new RoverFrontEnd(roverPort);
                roverFrontEnd.setVisible(true);
                roverFrontEnd.loadFromConfig(config);
                roverFrontEnd.setMachineName(machineName);
            } catch (Exception ex) {
                Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void whatIsMyIP(String zMachineName) {
        // Returns the instance of InetAddress containing
        // local host name and address
        InetAddress localhost = null;
        if (zMachineName.equals("Rover")) {
            try {
                localhost = InetAddress.getLocalHost();
            } catch (UnknownHostException ex) {
                Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("System IP Address : " +
                    (localhost.getHostAddress()).trim());

            // Find public IP address
            String systemipaddress = "";
            try {
                URL url_name = new URL("http://bot.whatismyipaddress.com");

                BufferedReader sc =
                        new BufferedReader(new InputStreamReader(url_name.openStream()));

                // reads system IPAddress
                systemipaddress = sc.readLine().trim();
            } catch (Exception e) {
                systemipaddress = "Cannot Execute Properly";
            }
            System.out.println("Public IP Address: " + systemipaddress + " (useful for connecting to Rover from UI Machine)");
            System.out.println("If UI machine IP address refreshes or changes you will need to go into UNIFI and update source ip addresses in the port forwarding section.");
        }
    }
}
