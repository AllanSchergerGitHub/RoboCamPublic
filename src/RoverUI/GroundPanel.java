package RoverUI;

import RoverUI.Vehicle.Truck;
import com.phidget22.Encoder;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class GroundPanel extends javax.swing.JPanel
        implements MouseMotionListener {
    private String roverOrUI_Flag;
    private final Truck mTruck;
    private final Point.Double mMousePos = new Point.Double();
    private final Point.Double mMouseInitPos = new Point.Double();
    private Object mSelectedObject = null;
    private boolean mIsLeftMouseUsed = true;
    private boolean mTroubleshootingMessages = false;

    public String Batch_time_stamp_into_mysql = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(new Date());

    private Encoder enCh = null;

    private ArrayList<XYDoubleListener> mXYListeners = new ArrayList<>();
    private BufferedImage mGroundImage;

    /**
     * Creates new form Floor
     */
    public GroundPanel() {
        initComponents();
        addMouseMotionListener(this);
        mTruck = new Truck(new Point.Double(getWidth() * 0.5, getHeight() * 0.6));
    }

    public void setGroundImageUrl(String url) {
        mIPCamPanelGround.setUrlAddrress(url);
    }

//    public void SetDutyCycleWeedClipperHead(double duty) throws InterruptedException, Exception{
////        mTruck.SetDutyCycleWeedClipperHead(duty);
//    }

//    public void baseMotorSetPosition(double baseMotorPosition) throws Exception{
//        mTruck.baseMotorSetPosition(baseMotorPosition);
//    }


    public String getBatchTime() {
        return Batch_time_stamp_into_mysql;
    }

    public Truck getTruck() {
        return mTruck;
    }

    public RoboCam.IPCamPanel getGroundIPCam() {
        return mIPCamPanelGround;
    }

    public void setTruckStreeMode(String streeModeName) {
        mTruck.setSteeringMode(streeModeName);
        System.err.println("streeModeName " + streeModeName);
        if (streeModeName == "Stopped") {
            mTruck.stopMoving();
        }
        if (mTroubleshootingMessages) {
            System.out.println("    mMousePos = " + mMousePos);
        }
        if (mTroubleshootingMessages) {
            System.out.println("1 ForwardSteer testing getWidth() = " + getWidth() + " mTruck.getSteerCircleEdgeY() = " + mTruck.getSteerCircleEdgeY());
        }
        mMousePos.setLocation(getWidth() * 0.5, mTruck.getSteerCircleEdgeY());
        adjustMousePos();
        mTruck.rotateWheelsTo(mMousePos);
        if (mTroubleshootingMessages) {
            System.out.println("    mMousePos = " + mMousePos);
        }
        if (mTroubleshootingMessages) {
            System.out.println("2 ForwardSteer testing getWidth() = " + getWidth() + " mTruck.getSteerCircleEdgeY() = " + mTruck.getSteerCircleEdgeY());
        }
        repaint();
        if (mTroubleshootingMessages) {
            System.out.println("    mMousePos = " + mMousePos);
        }
        if (mTroubleshootingMessages) {
            System.out.println("3 ForwardSteer testing getWidth() = " + getWidth() + " mTruck.getSteerCircleEdgeY() = " + mTruck.getSteerCircleEdgeY());
        }

    }

    public void setDrawScale(double setDrawScale) {
        mTruck.setDrawScale(setDrawScale);
    }

    public void moveForwardxSteps() {
        repaint();
    }

    public void moveBackxSteps() {
        repaint();
    }

    public void setVelocityLimit(double newVelocityLimit) {
        mTruck.updateEmergencyStopToFalse();
        mTruck.updateWheelVelocityLimit(newVelocityLimit);
    }

    public void addSpeed() {
        mTruck.addSpeed();
        repaint();
    }

    public void reduceSpeed() {
        mTruck.reduceSpeed();
        repaint();
    }


//public void EncoderExample() {
//
//        try {
//            com.phidget22.Log.enable(LogLevel.INFO, null);
//        } catch (PhidgetException ex) {
//            Logger.getLogger(RoverFrontEnd.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        
//        //Encoder enCh = null;
//        try {
//            enCh = new Encoder();
//        } catch (PhidgetException ex) {
//            Logger.getLogger(RoverFrontEnd.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        
//        enCh.addAttachListener(new AttachListener() {
//			public void onAttach(AttachEvent ae) {
//				Encoder phid = (Encoder) ae.getSource();
//				try {
//					if(phid.getDeviceClass() != DeviceClass.VINT){
//						System.out.println("channel " + phid.getChannel() + " on device " + phid.getDeviceSerialNumber() + " attached");
//					}
//					else{
//						System.out.println("channel " + phid.getChannel() + " on device " + phid.getDeviceSerialNumber() + " hub port " + phid.getHubPort() + " attached");
//					}
//				} catch (PhidgetException ex) {
//					System.out.println(ex.getDescription());
//				}
//			}
//        });
//
//        try {  
//            enCh.setDeviceSerialNumber(495959);            
//            enCh.setHubPort(5);
//            enCh.open(5000);
//            System.out.println("position "+enCh.getPosition());
//            } catch (PhidgetException ex) {
//            System.out.println(ex.getDescription());
//        }
//    }  

//    public long getEncoderPosition(){
//        
//        long position = 0;
//        try {
//            //enCh.setDeviceSerialNumber(495959);            
//            //enCh.setHubPort(5);
//            //enCh.open(5000);
//            position = enCh.getPosition();
//        } catch (PhidgetException ex) {
//            Logger.getLogger(GroundPanel.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        
//        return position;
//    }


    public void setMouseHandedness(String side) {
        if (side.equals("left")) {
            mIsLeftMouseUsed = true;
        } else {
            mIsLeftMouseUsed = false;
        }
    }

    public String getMouseHandedness() {
        return mIsLeftMouseUsed ? " left" : "right";
    }

    /**
     * This function assumes that the given pos
     * is relate to Truck's center location
     * adjusted by Truck's drawing scale.
     * This function will set the position of mouse
     * after reverting those relativeness.
     *
     * @param pos
     */
    public void setMousePosRelToTruck(Point.Double pos) {
        setMousePos(
                (pos.x * mTruck.getDrawScale()) + mTruck.getDrawLocation().x,
                (pos.y * mTruck.getDrawScale()) + mTruck.getDrawLocation().y
        );
    }

    /**
     * This function will adjust mouse position to keep it
     * within required bounds.
     */
    private void adjustMousePos() {
        if (mTroubleshootingMessages) {
            System.out.println("A adjustMousePos value set here " + mMousePos.x + " mTruck.getSteerCircleEdgeMaxX() = " + mTruck.getSteerCircleEdgeMaxX());
        }
        mMousePos.x = Math.min(mMousePos.x, mTruck.getSteerCircleEdgeMaxX());
        if (mTroubleshootingMessages) {
            System.out.println("B adjustMousePos value set here " + mMousePos.x + " mTruck.getSteerCircleEdgeMaxX() = " + mTruck.getSteerCircleEdgeMaxX());
        }
        mMousePos.x = Math.max(mMousePos.x, mTruck.getSteerCircleEdgeMinX());
        if (mTroubleshootingMessages) {
            System.out.println("C adjustMousePos value set here " + mMousePos.x + " mTruck.getSteerCircleEdgeMinX() = " + mTruck.getSteerCircleEdgeMinX());
        }
    }

    public void increaseMousePosXFraction(double pos) {
        setHypotheticalMousePos(
                mMousePos.x + pos * 0.5 * getWidth(),
                mMousePos.y
        );
    }

    public void setHypotheticalMousePos(double xPos, double yPos) {
        mMousePos.setLocation(xPos, yPos);
        adjustMousePos();
        mTruck.rotateWheelsTo(mMousePos);
        repaint();
        fireMousePosListeners();
    }

    public void setHypotheticalMousePosX(double xPos) {
        mMousePos.setLocation(xPos, mMousePos.y);
        adjustMousePos();
        mTruck.rotateWheelsTo(mMousePos);
        repaint();
    }

    public void setMousePosFromCenter(double xPos, double yPos) {
        setMousePos(getWidth() * (1 + xPos) * 0.5, getHeight() * (1 + yPos) * 0.5);
    }

    public void setMousePosAngle(double angleRad) {
        /*System.out.println(String.format(
                "angleRad=%f", 180*angleRad/Math.PI);*/
        setMousePos(
                getWidth() * (1 - Math.sin(angleRad)) * 0.5,
                getHeight() * (1 - Math.cos(angleRad)) * 0.5
        );
    }

    public void setMousePos(double xPos, double yPos) {
        if (!mIsLeftMouseUsed) {
            xPos = getWidth() - xPos;
        }
        mMousePos.setLocation(xPos, yPos);
        mTruck.rotateWheelsTo(mMousePos);
        //System.out.println(mMousePos.x + " this tracks any motion of the mouse"); //this tracks any motion of the mouse
        repaint();
    }

    public double getMousePosX() {
        return mMousePos.x;
    }

    public double getSteerCircleEdgeY() {
        return mTruck.getSteerCircleEdgeY();
    }

    public void addMousePosListener(XYDoubleListener listener) {
        if (mXYListeners.indexOf(listener) < 0) {
            mXYListeners.add(listener);
        }
    }
    
    public void setRoverOrUI_Flag(String roverOrUI_Flag) {
        this.roverOrUI_Flag = roverOrUI_Flag;
        mTruck.setRoverOrUI_Flag(roverOrUI_Flag);
    }
 
    @Override
    protected void paintComponent(Graphics grphcs) {
        super.paintComponent(grphcs);
        mGroundImage = mIPCamPanelGround.getImage();
        if (mGroundImage != null) {
            grphcs.drawImage(mGroundImage, 0, 0, getWidth(), getHeight(),
                    0, 0, mGroundImage.getWidth(), mGroundImage.getHeight(), null);
        }
        mTruck.moveTo((int) (getWidth() * 0.5), (int) (getHeight() * 0.5));
        mTruck.drawTruck((Graphics2D) grphcs); // this runs repeatedly
    }

    @Override
    public void mouseDragged(MouseEvent me) {
        if (mSelectedObject == null) {
            if (mTruck.isWithinTruckBody(me.getX(), me.getY())) {
                mSelectedObject = mTruck;
            }
        } else if (mSelectedObject == mTruck) {
            mTruck.moveRelative(me.getX() - mMousePos.x,
                    me.getY() - mMousePos.y);
            repaint();
        }
        mMousePos.setLocation(me.getX(), me.getY());
    }

    public void fireMousePosListeners() {
        mTruck.calc(mMousePos);
        for (XYDoubleListener listener : mXYListeners) {
            listener.onChange(
                    (mMousePos.x - mTruck.getDrawLocation().x) / mTruck.getDrawScale(),
                    (mMousePos.y - mTruck.getDrawLocation().y) / mTruck.getDrawScale()
            );
            //System.out.println("mouse moved to: "+mMousePos.x+ " and "+mMousePos.y);
        }
    }

    /**
     * Upload movement of mouse, the attached mXYListeners
     * will be fired. The listener gets the current position of
     * mouse with respect to Truck's center point adjusted
     * by Truck scale.
     *
     * @param me
     */
    @Override
    public void mouseMoved(MouseEvent me) {
        setMousePos(me.getX(), me.getY());
        fireMousePosListeners();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mIPCamPanelGround = new RoboCam.IPCamPanel();

        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.LINE_AXIS));

        mIPCamPanelGround.setBackground(new java.awt.Color(176, 142, 150));
        mIPCamPanelGround.setUrlAddrress("");

        javax.swing.GroupLayout mIPCamPanelGroundLayout = new javax.swing.GroupLayout(mIPCamPanelGround);
        mIPCamPanelGround.setLayout(mIPCamPanelGroundLayout);
        mIPCamPanelGroundLayout.setHorizontalGroup(
                mIPCamPanelGroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 574, Short.MAX_VALUE)
        );
        mIPCamPanelGroundLayout.setVerticalGroup(
                mIPCamPanelGroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 453, Short.MAX_VALUE)
        );

        add(mIPCamPanelGround);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private RoboCam.IPCamPanel mIPCamPanelGround;
    // End of variables declaration//GEN-END:variables

}
