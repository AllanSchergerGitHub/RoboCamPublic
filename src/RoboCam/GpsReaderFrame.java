package RoboCam;

import Utility.GpsReader;
import Utility.Waypointer;
import net.sf.marineapi.nmea.util.GpsFixQuality;
import net.sf.marineapi.nmea.util.Position;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GpsReaderFrame extends javax.swing.JFrame {
    static final int CANVAS_PAD = 20;

    GpsReader mGpsReader;
    PrintWriter mCsvWriter;
    String mCsvFilePath;
    Boolean mCanWriteGps = true;
    int SequenceNumber = 0;

    Position mLastPosition;

    Position mWaypointCurrent = new Position(0, 0);
    Waypointer mWaypointer = new Waypointer();
    final Canvas mCanvasWaypoint;
    Waypointer.Tracker mWaypointTracker;

    ScheduledExecutorService mWaypoitUpdater = Executors.newScheduledThreadPool(1);
    ScheduledFuture mWaypointFuture = null;


    String mPositonPickType = "";

    RoverUI.TruckSteerPanel mTruckSteerPanel = null;

    /**
     * Creates new form GpsReaderFrame
     */
    public GpsReaderFrame() {
        initComponents();
        mGpsReader = new GpsReader();
        mGpsReader.addListener(new GpsReader.GpsListener() {
            @Override
            public void onRead(Position position, GpsFixQuality fixQuality) {
                mLblGpsFixQuality.setText(String.format(
                        "Quality: %s", fixQuality.name()));
                if (mLastPosition != null &&
                        mLastPosition.equals(position)) {
                    return;
                }
                mLastPosition = position;

                mTxtGpsPosition.setText(String.format(
                        "Lat: %.8f, Long: %.8f, Alt:%.2f",
                        position.getLatitude(),
                        position.getLongitude(),
                        position.getAltitude()
                ));
                textFeet.setText(String.format(
                        "LatFeet: %.2f, LongFeet: %.2f",
                        (position.getLatitude() - 43.6678417) * (10000 / 90) * 3280.4, // convert lat into KM and then into Feet (3280.4 feet per KM)
                        (position.getLongitude() + 91.0064575) * (10000 / 90) * 3280.4 // convert long into KM and then into Feet (3280.4 feet per KM)
                ));
                textMeters.setText(String.format(
                        "LatMeters: %.2f, LongMeters: %.2f",
                        (position.getLatitude() - 43.6678417) * (10000 / 90) * 1000, // 1000 meters per KM
                        (position.getLongitude() + 91.0064575) * (10000 / 90) * 1000 // 1000 meters per KM
                ));
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String mTimeNow = String.format(LocalDateTime.now().format(fmt));

                if (mCsvWriter != null && mCanWriteGps) {
                    SequenceNumber++;
                    StringBuilder sb = new StringBuilder();
                    sb.append(SequenceNumber);
                    sb.append(',');
                    sb.append(position.getLatitude());
                    sb.append(',');
                    sb.append(position.getLongitude());
                    sb.append(',');
                    sb.append(position.getAltitude());
                    sb.append(',');
                    sb.append((position.getLatitude() - 43.6678417) * (10000 / 90) * 3280.4);
                    sb.append(',');
                    sb.append((position.getLongitude() + 91.0064575) * (10000 / 90) * 3280.4);
                    sb.append(',');
                    sb.append((position.getLatitude() - 43.6678417) * (10000 / 90) * 1000);
                    sb.append(',');
                    sb.append((position.getLongitude() + 91.0064575) * (10000 / 90) * 1000);
                    sb.append(',');
                    sb.append(fixQuality.name());
                    sb.append(',');
                    sb.append(mTimeNow);
                    sb.append('\n');

                    mCsvWriter.write(sb.toString());
                }
            }

        });
        //mGpsReader.connectToFile("<filepath>");
        mGpsReader.connectToCommPort("GNSS");
        mGpsReader.startReading();
        
        /*
        int steps = 10;
        for (int i = 0; i < steps; i++) {
            double x = 0.5 + 0.5*Math.cos(i*2*Math.PI/steps);
            double y = 0.5 + 0.5*Math.sin(i*2*Math.PI/steps);
            mWaypointer.addPoint(new Position(x, y));
        }
        mWaypointer.calculateBoundary();
        */

        mCanvasWaypoint = new Canvas() {
            @Override
            public void paint(Graphics grphcs) {
                super.paint(grphcs);

                int pad = CANVAS_PAD;
                int w = this.getWidth() - 2 * pad;
                int h = this.getHeight() - 2 * pad;
                int[][] polyPoints = mWaypointer.getPolylinePoints(pad, w, h);
                grphcs.drawPolyline(
                        polyPoints[0], polyPoints[1], polyPoints[0].length);
                for (int i = 0; i < polyPoints[0].length; i++) {
                    grphcs.drawOval(
                            polyPoints[0][i] - 3,
                            polyPoints[1][i] - 3, 6, 6);
                }
                Point2D.Double pos = mWaypointer.getPosition2Fraction(mWaypointCurrent);
                grphcs.fillOval(
                        pad + (int) (pos.x * w) - 3,
                        pad + (int) (pos.y * h) - 3, 6, 6);

                if (mWaypointTracker != null) {
                    pos = mWaypointTracker.getCurrentPositionFraction();
                    Graphics2D g2d = (Graphics2D) grphcs.create();
                    g2d.translate(pad, pad);
                    g2d.translate((int) (pos.x * w), (int) (pos.y * h));
                    g2d.setColor(Color.black);
                    g2d.drawLine(0, 0, 40, 0);
                    g2d.drawLine(0, 0, 0, 20);
                    g2d.rotate(-mWaypointTracker.getCurrentAngle());

                    g2d.setColor(Color.red);
                    g2d.drawLine(0, 0, 20, 0);
                    g2d.drawLine(0, 0, 0, 10);
                    g2d.drawRect(-3, -3, 20, 6);
                    g2d.fillOval(-2, -2, 4, 4);

                    grphcs.setColor(Color.green);
                    pos = mWaypointTracker.getTargetPositionFraction();
                    grphcs.fillOval(
                            pad + (int) (pos.x * w) - 3,
                            pad + (int) (pos.y * h) - 3, 6, 6);
                    grphcs.setColor(Color.blue);
                    pos = mWaypointTracker.getFinalPositionFraction();
                    grphcs.fillOval(
                            pad + (int) (pos.x * w) - 3,
                            pad + (int) (pos.y * h) - 3, 6, 6);
                }
            }

        };
        mCanvasWaypoint.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (!mPositonPickType.isEmpty()) {
                    Position position = mWaypointer.getFraction2Position(
                            new Point2D.Double(
                                    (me.getX() - CANVAS_PAD) * 1f / (mCanvasWaypoint.getWidth() - 2 * CANVAS_PAD),
                                    (me.getY() - CANVAS_PAD) * 1f / (mCanvasWaypoint.getHeight() - 2 * CANVAS_PAD)
                            )
                    );
                    String posString = String.format(
                            "%f, %f", position.getLatitude(), position.getLongitude());
                    if (mPositonPickType == "Current") {
                        mTxtWaypointCurrent.setText(posString);
                        mBtnPickWaypointCurrent.setSelected(false);
                    } else if (mPositonPickType == "Final") {
                        mTxtWaypointFinal.setText(posString);
                        mBtnPickWaypointFinal.setSelected(false);
                    }
                    setTrackerValues();
                    mWaypointTracker.update();
                    mCanvasWaypoint.repaint();
                }
                mPositonPickType = "";
            }

            @Override
            public void mousePressed(MouseEvent me) {
            }

            @Override
            public void mouseReleased(MouseEvent me) {
            }

            @Override
            public void mouseEntered(MouseEvent me) {

            }

            @Override
            public void mouseExited(MouseEvent me) {

            }
        });
        mPanelWaypoint.add(mCanvasWaypoint, BoxLayout.X_AXIS);
    }

    public void setTruckSteerPanel(RoverUI.TruckSteerPanel truck) {
        mTruckSteerPanel = truck;
    }

    public void setTrackerValues() {
        mWaypointTracker = mWaypointer.createTracker();
        String[] arr;
        if (mLastPosition != null) {
            mWaypointTracker.setCurrentPosition(
                    mLastPosition.getLatitude(), mLastPosition.getLongitude()
            );
        } else {
            arr = mTxtWaypointCurrent.getText().split(",");
            mWaypointTracker.setCurrentPosition(
                    Double.parseDouble(arr[0]), Double.parseDouble(arr[1]));
        }
        arr = mTxtWaypointFinal.getText().split(",");
        mWaypointTracker.setFinalPosition(
                Double.parseDouble(arr[0]), Double.parseDouble(arr[1]));


    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        mLblGpsFixQuality = new java.awt.Label();
        mTxtGpsPosition = new java.awt.TextField();
        labelFeet = new java.awt.Label();
        textFeet = new java.awt.TextField();
        textMeters = new java.awt.TextField();
        labelFeet1 = new java.awt.Label();
        mLblCsvFolder = new java.awt.Label();
        mTxtCsvFolder = new java.awt.TextField();
        mBtnRecord = new javax.swing.JToggleButton();
        mBtnPauseRecord = new javax.swing.JToggleButton();
        mLblCsvFilePath = new javax.swing.JLabel();
        label3 = new java.awt.Label();
        jPanel2 = new javax.swing.JPanel();
        mLblWaypointCsvFile = new java.awt.Label();
        mTxtWaypointCsvFile = new java.awt.TextField();
        mlblWaypointCurrent = new java.awt.Label();
        mTxtWaypointCurrent = new java.awt.TextField();
        mBtnWaypointLoad = new javax.swing.JButton();
        mPanelWaypoint = new javax.swing.JPanel();
        mlblWaypointFinal = new java.awt.Label();
        mTxtWaypointFinal = new java.awt.TextField();
        mBtnPickWaypointCurrent = new javax.swing.JToggleButton();
        mBtnPickWaypointFinal = new javax.swing.JToggleButton();
        mToggleBtnMoveTracker = new javax.swing.JToggleButton();
        mChkMoveRover = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setPreferredSize(new java.awt.Dimension(600, 450));

        jTabbedPane1.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jTabbedPane1.setPreferredSize(new java.awt.Dimension(400, 100));

        mLblGpsFixQuality.setText("Fix Quality");

        mTxtGpsPosition.setText("textField1");

        labelFeet.setText("Feet");

        textFeet.setText("textField1");

        textMeters.setText("textField1");

        labelFeet1.setText("Meters:Centemeters");

        mLblCsvFolder.setText("CSV folder");

        mTxtCsvFolder.setText(".");

        mBtnRecord.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        mBtnRecord.setText("Start Record");
        mBtnRecord.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBtnRecordActionPerformed(evt);
            }
        });

        mBtnPauseRecord.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        mBtnPauseRecord.setText("Pause Record");
        mBtnPauseRecord.setEnabled(false);
        mBtnPauseRecord.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBtnPauseRecordActionPerformed(evt);
            }
        });

        mLblCsvFilePath.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        mLblCsvFilePath.setText("Full path to CSV file");

        label3.setText("GPS Position");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                                .addGap(74, 74, 74)
                                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                                        .addComponent(mLblCsvFolder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                                                .addComponent(labelFeet, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addGap(22, 22, 22))))
                                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addComponent(labelFeet1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(mTxtCsvFolder, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                                                .addComponent(textFeet, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 354, Short.MAX_VALUE)
                                                                                .addComponent(textMeters, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                                                .addComponent(mTxtGpsPosition, javax.swing.GroupLayout.PREFERRED_SIZE, 231, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                .addComponent(mLblGpsFixQuality, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                                .addGap(0, 0, Short.MAX_VALUE))))
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addContainerGap()
                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                                .addComponent(mBtnRecord, javax.swing.GroupLayout.PREFERRED_SIZE, 195, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(68, 68, 68)
                                                                .addComponent(mBtnPauseRecord, javax.swing.GroupLayout.PREFERRED_SIZE, 195, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(0, 124, Short.MAX_VALUE))
                                                        .addComponent(mLblCsvFilePath, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                                .addContainerGap())
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addGap(47, 47, 47)
                                        .addComponent(label3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addContainerGap(476, Short.MAX_VALUE)))
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(mLblGpsFixQuality, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(mTxtGpsPosition, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(textFeet, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                                .addGap(4, 4, 4)
                                                .addComponent(labelFeet, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(labelFeet1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(textMeters, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(mLblCsvFolder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(mTxtCsvFolder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(mBtnRecord)
                                        .addComponent(mBtnPauseRecord))
                                .addGap(18, 18, 18)
                                .addComponent(mLblCsvFilePath)
                                .addContainerGap(191, Short.MAX_VALUE))
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addGap(20, 20, 20)
                                        .addComponent(label3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addContainerGap(338, Short.MAX_VALUE)))
        );

        jTabbedPane1.addTab("Gsp Recording", jPanel1);

        mLblWaypointCsvFile.setText("CSV file");

        mTxtWaypointCsvFile.setText("waypoints.csv");

        mlblWaypointCurrent.setText("Current Location");

        mTxtWaypointCurrent.setText("0,0");

        mBtnWaypointLoad.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        mBtnWaypointLoad.setText("Load");
        mBtnWaypointLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBtnWaypointLoadActionPerformed(evt);
            }
        });

        mPanelWaypoint.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        mPanelWaypoint.setMaximumSize(new java.awt.Dimension(10000, 2000));
        mPanelWaypoint.setMinimumSize(new java.awt.Dimension(100, 200));
        mPanelWaypoint.setLayout(new javax.swing.BoxLayout(mPanelWaypoint, javax.swing.BoxLayout.LINE_AXIS));

        mlblWaypointFinal.setText("FInal Location");

        mTxtWaypointFinal.setText("0,0");

        mBtnPickWaypointCurrent.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        mBtnPickWaypointCurrent.setText("Pick");
        mBtnPickWaypointCurrent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBtnPickWaypointCurrentActionPerformed(evt);
            }
        });

        mBtnPickWaypointFinal.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        mBtnPickWaypointFinal.setText("Pick");
        mBtnPickWaypointFinal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBtnPickWaypointFinalActionPerformed(evt);
            }
        });

        mToggleBtnMoveTracker.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        mToggleBtnMoveTracker.setText("Move");
        mToggleBtnMoveTracker.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mToggleBtnMoveTrackerActionPerformed(evt);
            }
        });

        mChkMoveRover.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        mChkMoveRover.setText("Use Rover");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(91, 91, 91)
                                .addComponent(mLblWaypointCsvFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(81, 81, 81)
                                .addComponent(mTxtWaypointCsvFile, javax.swing.GroupLayout.PREFERRED_SIZE, 277, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 26, Short.MAX_VALUE)
                                .addComponent(mBtnWaypointLoad, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(12, 12, 12))
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                                .addComponent(mTxtWaypointCurrent, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(mBtnPickWaypointCurrent, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                                .addComponent(mTxtWaypointFinal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(mBtnPickWaypointFinal, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                .addGap(26, 26, 26))
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(mToggleBtnMoveTracker, javax.swing.GroupLayout.PREFERRED_SIZE, 231, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                                .addComponent(mChkMoveRover, javax.swing.GroupLayout.PREFERRED_SIZE, 114, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addComponent(mlblWaypointCurrent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addComponent(mlblWaypointFinal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                .addComponent(mPanelWaypoint, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(34, 34, 34))
        );
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addGap(20, 20, 20)
                                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addComponent(mLblWaypointCsvFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(mTxtWaypointCsvFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                                .addContainerGap()
                                                .addComponent(mBtnWaypointLoad)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addComponent(mlblWaypointCurrent, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addComponent(mTxtWaypointCurrent, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(mBtnPickWaypointCurrent))
                                                .addGap(20, 20, 20)
                                                .addComponent(mlblWaypointFinal, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addComponent(mTxtWaypointFinal, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(mBtnPickWaypointFinal))
                                                .addGap(42, 42, 42)
                                                .addComponent(mChkMoveRover)
                                                .addGap(18, 18, 18)
                                                .addComponent(mToggleBtnMoveTracker))
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addGap(9, 9, 9)
                                                .addComponent(mPanelWaypoint, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Waypoint", jPanel2);

        jTabbedPane1.setSelectedIndex(1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 614, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 412, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void mBtnRecordActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBtnRecordActionPerformed
        if (mBtnRecord.isSelected()) {
            mCanWriteGps = true;
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
            mCsvFilePath = mTxtCsvFolder.getText() + File.separator + String.format(
                    "Gps-%s.csv", LocalDateTime.now().format(fmt)
            );
            mLblCsvFilePath.setText(mCsvFilePath);
            try {
                mCsvWriter = new PrintWriter(new File(mCsvFilePath));
                StringBuilder sb = new StringBuilder();
                sb.append("Sequence_Num,");
                sb.append("Latitude,");
                sb.append("Longitude,");
                sb.append("Altitude,");
                sb.append("X-Feet,");
                sb.append("Y-Feet,");
                sb.append("X-Meter,");
                sb.append("Y-Meter,");
                sb.append("GPS_Quality,");
                sb.append("TimeStamp");
                sb.append('\n');
                mCsvWriter.write(sb.toString());

            } catch (FileNotFoundException ex) {
                Logger.getLogger(GpsReaderFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
            mBtnRecord.setText("Stop Recording");
            mBtnPauseRecord.setEnabled(true);
        } else {
            if (mCsvWriter != null) {
                mCsvWriter.close();
            }
            mBtnRecord.setText("Start Recording");
            mBtnPauseRecord.setEnabled(false);
        }
    }//GEN-LAST:event_mBtnRecordActionPerformed

    private void mBtnPauseRecordActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBtnPauseRecordActionPerformed
        mCanWriteGps = !mBtnPauseRecord.isSelected();
    }//GEN-LAST:event_mBtnPauseRecordActionPerformed

    private void mBtnWaypointLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBtnWaypointLoadActionPerformed
        try {
            mWaypointer.addPointFromCSV(
                    new File(mTxtWaypointCsvFile.getText()),
                    "Longitude", "Latitude"
            );
        } catch (IOException ex) {
            Logger.getLogger(GpsReaderFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        mCanvasWaypoint.repaint();
    }//GEN-LAST:event_mBtnWaypointLoadActionPerformed

    private void mBtnPickWaypointCurrentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBtnPickWaypointCurrentActionPerformed
        mPositonPickType = mBtnPickWaypointCurrent.isSelected() ? "Current" : "";
    }//GEN-LAST:event_mBtnPickWaypointCurrentActionPerformed

    private void mBtnPickWaypointFinalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBtnPickWaypointFinalActionPerformed
        mPositonPickType = mBtnPickWaypointFinal.isSelected() ? "Final" : "";
    }//GEN-LAST:event_mBtnPickWaypointFinalActionPerformed

    private void mToggleBtnMoveTrackerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mToggleBtnMoveTrackerActionPerformed
        if (!mToggleBtnMoveTracker.isSelected()) {
            if (mWaypointFuture != null) {
                mWaypointFuture.cancel(false);
                mWaypointFuture = null;
            }
            return;
        }

        mWaypointFuture = mWaypoitUpdater.scheduleAtFixedRate(new Runnable() {
            long mLastGuiUpdatedAt = System.currentTimeMillis();

            @Override
            public void run() {
                if (mWaypointTracker.update()) {
                    if (mChkMoveRover.isSelected()) {
                        if (mTruckSteerPanel != null) {
                            mTruckSteerPanel.addForwardAngle(1);
                            mTruckSteerPanel.setMousePosAngle(
                                    mWaypointTracker.getTargetAngle()
                                            -
                                            mWaypointTracker.getCurrentAngle()
                            );
                        }
                        if (mLastPosition != null) {
                            mWaypointTracker.setCurrentPosition(
                                    mLastPosition.getLatitude(),
                                    mLastPosition.getLongitude()
                            );
                        } else {
                            //Since we are getting GPS yet, let use force movement.
                            mWaypointTracker.move(mWaypointTracker.getDistanceThreshold());
                        }
                    } else {
                        mWaypointTracker.move(mWaypointTracker.getDistanceThreshold());
                    }
                    if ((System.currentTimeMillis() - mLastGuiUpdatedAt) > 100) {
                        mCanvasWaypoint.repaint();
                        mLastGuiUpdatedAt = System.currentTimeMillis();
                        //System.out.println(mWaypointTracker.getTargetAngle());
                    }
                } else {
                    mWaypointFuture.cancel(false);
                    mWaypointFuture = null;
                    mToggleBtnMoveTracker.setSelected(false);
                    mWaypointTracker = null;
                }
                // System.out.println(String.format("angle=%f", mWaypointTracker.getTargetAngle()));
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
    }//GEN-LAST:event_mToggleBtnMoveTrackerActionPerformed

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
            java.util.logging.Logger.getLogger(GpsReaderFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(GpsReaderFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(GpsReaderFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(GpsReaderFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new GpsReaderFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private java.awt.Label label3;
    private java.awt.Label labelFeet;
    private java.awt.Label labelFeet1;
    private javax.swing.JToggleButton mBtnPauseRecord;
    private javax.swing.JToggleButton mBtnPickWaypointCurrent;
    private javax.swing.JToggleButton mBtnPickWaypointFinal;
    private javax.swing.JToggleButton mBtnRecord;
    private javax.swing.JButton mBtnWaypointLoad;
    private javax.swing.JCheckBox mChkMoveRover;
    private javax.swing.JLabel mLblCsvFilePath;
    private java.awt.Label mLblCsvFolder;
    private java.awt.Label mLblGpsFixQuality;
    private java.awt.Label mLblWaypointCsvFile;
    private javax.swing.JPanel mPanelWaypoint;
    private javax.swing.JToggleButton mToggleBtnMoveTracker;
    private java.awt.TextField mTxtCsvFolder;
    private java.awt.TextField mTxtGpsPosition;
    private java.awt.TextField mTxtWaypointCsvFile;
    private java.awt.TextField mTxtWaypointCurrent;
    private java.awt.TextField mTxtWaypointFinal;
    private java.awt.Label mlblWaypointCurrent;
    private java.awt.Label mlblWaypointFinal;
    private java.awt.TextField textFeet;
    private java.awt.TextField textMeters;
    // End of variables declaration//GEN-END:variables
}
