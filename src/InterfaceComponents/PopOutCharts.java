/*
 * A few charts are created and run from here to support the UIFrontEnd.java.
 * It is cleaner to have them split out into a separate class.
 */
package InterfaceComponents;

import Chart.ChartParamsDataset;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.dial.DialPlot;
import org.jfree.chart.plot.dial.DialPointer;
import org.jfree.chart.plot.dial.StandardDialFrame;
import org.jfree.chart.plot.dial.StandardDialScale;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.general.DefaultValueDataset;
import org.jfree.data.xy.XYDataset;

/**
 * PopOutCharts is used by UIFrontEnd to show other windows that aren't embedded in the 
 * main user interface.
 * @author allan
 */
public class PopOutCharts {
    // class Variables
    private DefaultValueDataset datasetAngleGaugeChart;
    private ChartPanel chartPanelGauge;
    private ChartParamsDataset chartParamsDataset;
    private Map<String, DefaultValueDataset> datasets = new HashMap<>(); // for the 2 anglegaugecharts' data
    private Map<String, ChartPanel> chartPanels = new HashMap<>(); // for the 2 anglegaugecharts' chartpanels
    ChartParamsDataset[] mChartParamsDatasets;
    Timer mUpdaterTimer;
    private InterfaceComponents.TruckSteerPanel mTruckSteerPanel;
    private JButton mBtnExitApp;
    
    /**
     * Constructor
     * PopOutCharts is used by UIFrontEnd to show other windows that aren't embedded in the 
     * main user interface.
     * @param mBtnExitApp
     * @param mTruckSteerPanel // is passed in from UIFrontEnd to ensure I'm using the same
     *  TruckSteerPanel here and in UIFrontEnd.
     */
    public PopOutCharts(JButton mBtnExitApp, InterfaceComponents.TruckSteerPanel mTruckSteerPanel) {
        this.mBtnExitApp = mBtnExitApp;
        this.mTruckSteerPanel = mTruckSteerPanel;
        //mTruckSteerPanel = new InterfaceComponents.TruckSteerPanel(); // don't use this as it creates a separate instance of the TruckSteerPanel
    }
    
    public void initCreateAngleGaugeCharts() {
        mChartParamsDatasets = mTruckSteerPanel.getTruck().getChartParamsDatasets_From_Truck_For_UI();
        for (ChartParamsDataset chartParamsDataset : mChartParamsDatasets) {
                if(chartParamsDataset.getChartName().contains("Front")){
                String chartName = "Gauge: " + chartParamsDataset.getChartName();
                // Create a new datasetAngleGaugeChart
                datasetAngleGaugeChart = new DefaultValueDataset();

                // Extract the current steering angle from your ANGLE_Dataset
                // Replace getANGLE_Dataset() with the method that gets your ANGLE_Dataset
                XYDataset angleDataset = chartParamsDataset.getANGLE_Dataset();
                int itemCount = angleDataset.getItemCount(0);
                double currentSteeringAngle = 0;
                // Check if the datasetAngleGaugeChart is empty
                if (itemCount > 0) {
                    currentSteeringAngle = angleDataset.getYValue(0, itemCount - 1);

                    // Set the current steering angle as the value in the datasetAngleGaugeChart
                    datasetAngleGaugeChart.setValue(currentSteeringAngle);
                } else {
                    // Handle the case where the datasetAngleGaugeChart is empty
                    // For example, you could set the value in the datasetAngleGaugeChart to 0
                    datasetAngleGaugeChart.setValue(0);
                }

                // Create the dial plotGauge
                DialPlot plotGauge = new DialPlot();
                plotGauge.setDataset(datasetAngleGaugeChart);

                // Configure the plotGauge to look like a half-circle chart
                StandardDialScale scale = new StandardDialScale(-90, 90, -180, -180, 10, 4);
                scale.setTickRadius(0.88);
                scale.setTickLabelOffset(0.15);
                scale.setTickLabelFont(new Font("Dialog", Font.PLAIN, 14));
                plotGauge.addScale(0, scale);
                plotGauge.setDialFrame(new StandardDialFrame());

                // Add a pointer to the plot
                DialPointer.Pointer pointer = new DialPointer.Pointer();
                plotGauge.addPointer(pointer);
                
                // Create the chart
                JFreeChart angleChartGauge = new JFreeChart(chartName, plotGauge);
                angleChartGauge.setTitle(new TextTitle(chartName, new Font("Dialog", Font.BOLD, 24)));

                // add data to the 'datasets'
                datasets.put(chartParamsDataset.getChartName(), datasetAngleGaugeChart);
                //System.out.println("keyset " + datasets.keySet());

                // Create a new ChartPanel with the chart
                chartPanelGauge = new ChartPanel(angleChartGauge);
                chartPanelGauge.setPreferredSize(new Dimension(300, 350)); // width, height
                chartPanels.put(chartParamsDataset.getChartName(), chartPanelGauge);

                // Create a new JFrame to hold the ChartPanel
                JFrame chartFrame = new JFrame(chartName);
                chartFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                chartFrame.getContentPane().add(chartPanelGauge);
                chartFrame.pack();
                chartFrame.setLocationRelativeTo(null); // Center the frame
                
                // Load the saved window location
                Preferences prefs_ = Preferences.userNodeForPackage(getClass()).node("angleGaugeChartWindowLocation").node(chartName);
                int x_ = prefs_.getInt(chartName + "_x", 0);
                int y_ = prefs_.getInt(chartName + "_y", 0);
                System.out.println("Loaded Gauge Chart window location: " + chartName +"; " + x_ + ", " + y_);
                chartFrame.setLocation(x_, y_);
                chartFrame.setVisible(true);
                
                    // Add a Listener to the exit button that saves the window location when it is clicked
                    // If the window is dragged to a new location the new location is saved when the exit button is clicked.
                    // The next time the app is started these two windows are placed in the same spot they were previously placed.
                    mBtnExitApp.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                    // Save the window locations of the gauge chart frames (Dial Gauges of the steering angle)
                        for (String chartName : chartPanels.keySet()) {
                            ChartPanel chartPanel = chartPanels.get(chartName);
                                if (chartPanel != null) {
                                    JFrame chartFrame = (JFrame) SwingUtilities.getWindowAncestor(chartPanel);
                                    if (chartFrame != null) {
                                        chartName = "Gauge: " + chartName;
                                        System.out.println("Saving location of Gauge Charts with chartName = " + chartName + " x = " + chartFrame.getX() 
                                                + "; y = " + chartFrame.getY() + chartPanel.getName());
                                        prefs_.putInt(chartName + "_x", chartFrame.getX());
                                        prefs_.putInt(chartName + "_y", chartFrame.getY());
                                    } else {
                                        System.err.println("No JFrame found for ChartPanel with name: " + chartName);
                                    }
                                } else {
                                    System.err.println("No ChartPanel found with name: " + chartName);
                                }
                            }
                        }
                    });
                            }
        }
    }

    /**
     * These are time series charts showing the steering angle of the 2 front wheels.
     */
    public void initCreateAngleCharts() {
        mChartParamsDatasets = mTruckSteerPanel.getTruck().getChartParamsDatasets_From_Truck_For_UI();
            for (ChartParamsDataset chartParamsDataset : mChartParamsDatasets) {
                if(chartParamsDataset.getChartName().contains("Front")){
                    String chartName = "Angle: " + chartParamsDataset.getChartName();
                    // Create new popout charts with angle data.
                    // This version of the code produces 4 charts (one per wheel).
                    // Only 2 charts are needed for the 2 front wheels since the rear wheels never change.
                    // This displays the setting of the wheel angle; not the actual physical angles.
                    JFreeChart angleChart = ChartFactory.createXYLineChart(
                        chartName,
                        "Time",
                        "Angle",
                        chartParamsDataset.getANGLE_Dataset(),
                        PlotOrientation.VERTICAL,
                        false,
                        false,
                        false
                    );
                    
                    // Create a new ChartPanel with the angle chart
                    ChartPanel angleChartPanel = new ChartPanel(angleChart);
                    angleChartPanel.setPreferredSize(new Dimension(300, 350)); // width, height

                    // Create a new JFrame to hold the ChartPanel
                    JFrame angleChartFrame = new JFrame("Steer Angle: " + chartParamsDataset.getChartName());

                    // Load the saved window location
                    Preferences prefs = Preferences.userNodeForPackage(getClass()).node("angleChartWindowLocation").node(chartName);
                    int x = prefs.getInt("windowX", 0);
                    int y = prefs.getInt("windowY", 0);
                    System.out.println("Loaded window location (Angle Charts) " + chartName +"; " + x + ", " + y);
                    angleChartFrame.setLocation(x, y);

                    angleChartFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    angleChartFrame.getContentPane().add(angleChartPanel);
                    angleChartFrame.pack();

                    // Make the window always on top
                    angleChartFrame.setAlwaysOnTop(true);
                    angleChartFrame.setVisible(true);

                    // Add a Listener to the exit button that saves the window location when it is clicked
                    // If the window is dragged to a new location the new location is saved when the exit button is clicked.
                    // The next time the app is started these two windows are placed in the same spot they were previously placed.
                    mBtnExitApp.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // Save the window locations of the Angle Charts (trend charts of the steering angles)
                        prefs.putInt("windowX", angleChartFrame.getX());
                        prefs.putInt("windowY", angleChartFrame.getY());
                        
                        System.out.println("Saving location of Angle Charts with chartName = " + chartName + " x = " + angleChartFrame.getX() + "; y = " + angleChartFrame.getY());
                        
                        try {
                            prefs.flush();
                        } catch (BackingStoreException ex) {
                            System.err.println("Error saving the window positions. Check code in UIFrontEnd.java.");
                        }
                    }
                });
                
            }
        }
    }
    
    /**
     * Updates the data used in the charts every x milliseconds and repaints them.
     */
    public void initGaugeChartUpdateTimer(){        
        /**
         * The action is the firing of the timer
         */
        mUpdaterTimer = new Timer(200, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //========================================================================================
                /**
                 * This line causes the charts to be updated every x milliseconds.
                 * Without this line the charts only update when new data is added via a mouseclick on \
                 * one of the steering buttons.
                 */
                mTruckSteerPanel.getTruck().updateChartParamsDataset();
                //========================================================================================
                for (ChartParamsDataset chartParamsDataset : mChartParamsDatasets) {
                    if(chartParamsDataset.getChartName().contains("Front")){
                        datasetAngleGaugeChart = datasets.get(chartParamsDataset.getChartName());
                        chartPanelGauge = chartPanels.get(chartParamsDataset.getChartName());
                        XYDataset angleDataset = chartParamsDataset.getANGLE_Dataset();
                        int itemCount = angleDataset.getItemCount(0);
                        double currentSteeringAngle;
                        // Check if the datasetAngleGaugeChart is empty
                        if (itemCount > 0) {
                            currentSteeringAngle = angleDataset.getYValue(0, itemCount - 1);
                            datasetAngleGaugeChart.setValue(currentSteeringAngle);
                            datasets.get(chartParamsDataset.getChartName()).setValue(currentSteeringAngle);
//                            System.out.println("here ----------------------------------------------------" + currentSteeringAngle + " " +
//                                    chartParamsDataset.getChartName() +
//                                    datasets.get(chartParamsDataset.getChartName()).getValue() + " " +
//                                    datasetAngleGaugeChart.getValue() + " " +
//                                    chartPanels.keySet() + " " +
//                                     chartParamsDataset.getChartName()
//                                    );
                        } else {
                            // Handle the case where the datasetAngleGaugeChart is empty
                            System.err.println("There is a problem in UIFrontEnd with the gauge charts. Moving on.");
                            datasetAngleGaugeChart.setValue(0);
                        }
                        // Repaint the chart panel
                        /**
                         * repaint doesn't seem to be needed; keeping it here in case there are problems at some point.
                         */
                        //chartPanelGauge.repaint();
                }}
            }
        });
        mUpdaterTimer.start();
    }
}
        

