import RoboCam.Config;
import RoboCam.RoverFrontEnd;
import RoboCam.UIFrontEnd;
import com.phidget22.PhidgetException;
import java.awt.Font;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

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
        Utility.ComputerMachineNameService.setComputerMachineName(machineName, "InitialSetup_Launcher.java");

        if (machineName.equals("UI")) {
            
            {InetAddress localhost = null;
            try {
                localhost = InetAddress.getLocalHost();
            } catch (UnknownHostException ex) {
                Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (localhost != null) {
                System.out.println("Private IP Address of the UI Computer: " +
                    (localhost.getHostAddress()).trim());
            } else {
                System.out.println("Could not determine Private IP Address of UI Computer.");
            }
            System.out.println("Rover Machine info based on robo-config.ini file: " + roverHost + "; " + roverPort
                    + ". If this doesn't match the private IP Address of the Rover Computer, the network connection won't work."
                    + " It may mean the IP Address of the Rover Computer has changed and the robo-config.ini file needs to be updated.");
            }
            
            try {
                UIFrontEnd uiFrontEnd = new UIFrontEnd(roverHost, roverPort);
                uiFrontEnd.start_mSocketClientThread();
                uiFrontEnd.setVisible(true);
                uiFrontEnd.loadFromConfig(config);
            } catch (IOException ex) {
                Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
            } catch (PhidgetException ex) {
                Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (machineName.equals("Rover")) {
            try {
                System.out.println("Rover Machine roverPort = " + roverPort + ". (from robo-config.ini file). Make sure the ip address in the robo-config.ini on rover is also port forwarded in router that rover connects to.\n");
                RoverFrontEnd roverFrontEnd = new RoverFrontEnd(roverPort);
                roverFrontEnd.setVisible(true);
                roverFrontEnd.loadFromConfig(config);
            } catch (Exception ex) {
                Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Gets the IP address of Rover so it can be used for testing connection issues between Rover and UI.
     * Returns the instance of InetAddress containing
     * local host name and address
     * @param zMachineName 
     */
    public static void whatIsMyIP(String zMachineName) {
        InetAddress localhost = null;
        if (zMachineName.equals("Rover")) {
            try {
                localhost = InetAddress.getLocalHost();
            } catch (UnknownHostException ex) {
                Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (localhost != null) {
                System.out.println("Private IP Address of the Rover Computer: " +
                    (localhost.getHostAddress()).trim());
            } else {
                System.out.println("Could not determine Private IP Address of Rover Computer.");
            }

            // Find network IP address visible to the public (and therefore UI could use this to find Rover or vise versa).
            String systemipaddress = "";
            // List of URLs to check. Pinging one of these will return the IP address
            // of my network, not my specific computer. Sometimes these ip addresses change
            // or stop working so I ping all four of them and hopefully at least one will
            // still be in service. Eventually I may need to come up with new urlsToCheck.
            String[] urlsToCheck = {
                "http://checkip.amazonaws.com",
                "http://icanhazip.com",
                "http://ifconfig.me",
                "http://api.ipify.org"
            };
            for (String urlBeingChecked : urlsToCheck) {
                try {
                    URL url_name = new URL(urlBeingChecked);

                    BufferedReader sc =
                            new BufferedReader(new InputStreamReader(url_name.openStream()));

                    // reads system IPAddress
                    systemipaddress = sc.readLine().trim();

                    // If we got an IP address, break out of the loop
                    if (!systemipaddress.isEmpty()) {
                        break;
                    }
                    
                    } catch (MalformedURLException e) {
                        // If the URL is not properly formed, try the next URL
                        continue;
                    } catch (IOException e) {
                        // If an I/O error occurs, try the next URL
                        continue;
                    } catch (NullPointerException e) {
                        // If the stream is null, try the next URL
                        continue;
                    }
                }

            // If systemipaddress is still empty after trying all URLs, set it to an error message
            if (systemipaddress.isEmpty()) {
                systemipaddress = "Cannot Execute Properly";
            
                { // This block of code raises an error on the Rover computer if the network connection isn't possible
                  // due to the 2 computers not being on the same network.
                JLabel label = new JLabel("Error! Are both computers on the 'test' network?");
                label.setFont(new Font("Arial", Font.PLAIN, 24)); // Set font size to 24
                // Create a JOptionPane with the JLabel
                JOptionPane optionPane = new JOptionPane(label, JOptionPane.INFORMATION_MESSAGE);
                // Create a JDialog that contains the JOptionPane
                JDialog dialog = optionPane.createDialog("Message");
                dialog.setAlwaysOnTop(true); // Make the dialog always on top
                dialog.setVisible(true); // Show the dialog
                //JOptionPane.showMessageDialog(null, "Error! Are both computers on the 'test' network?");
                }
                System.out.println("Are both computers (UI computer and Rover computer) on the same network? "
                        + "Typically this is the 'test' network as of July 2023. "
                        + "Currently the codebase requires them to be on the same network. A "
                        + "future update would be to allow them to connect between 2 networks.");
                System.out.println("If UI machine IP address refreshes or changes you will need to go into "
                        + "UNIFI and update source ip addresses in the port forwarding section.");
            }
            System.out.println("Public IP Address of my network (all devices on my network will have this same public IP Address): " 
                    + systemipaddress + " (useful for connecting to Rover from UI Machine)");
        }
    }
}
