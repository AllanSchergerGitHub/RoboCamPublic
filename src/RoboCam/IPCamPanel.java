/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package RoboCam;

import Utility.UiLine;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IPCamPanel extends javax.swing.JPanel
        implements MouseMotionListener, MouseListener {

    private final UiLine.Collection mLineCollection = new UiLine.Collection();
    private UiLine.Line mActiveLine = null;
    private Color mLineColor = Color.BLACK;
    private int mLineThickness = 1;

    public interface ConnectionListener {
        void onConnect();

        void onDisconnect();

        void onImageUpdate();
    }

    public interface LineListener {
        void onSelect(UiLine.Line line);
    }

    static final boolean SAVE_IMAGE = true;

    private int mImageSaveLag = 1000;//
    private int mImageSaveLagTime = 1 * 10000;//milliseconds
    private int mFps = 300;
    private static int imageCount = 0; // counts each image so we can save every x images - saving all of them is too much data
    // flag to capture each image stream only once
    private static String[] ImageID = new String[5];

    private String mUrlAddress;
    private URL mURL;
    private final ImageLoader mImageLoader = new ImageLoader();
    private BufferedImage mImage;
    private float mImageScale;
    private final Point mImageOffset = new Point(0, 0);

    private final ArrayList<ConnectionListener> mConnListeners = new ArrayList<>();
    private final ArrayList<LineListener> mLineListeners = new ArrayList<>();


    /**
     * Creates new form IPCamPanel
     */
    public IPCamPanel() {
        initComponents();
        addMouseListener((this));
        addMouseMotionListener((this));
        mImageScale = 1;
        mImageLoader.execute();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

    String ts = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(new Date());
    private static int oldCounter;
    private static String tsStatic = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(new Date());
    private static int counter;
    private static long oldLag;
    private static long currLag;
    private static int CONNECITON_TIMEOUT = 2 * 1000;

    final class ImageLoader extends SwingWorker<Void, BufferedImage> {
        @Override
        protected Void doInBackground() {
            //long lastUpdated = System.currentTimeMillis();
            //System.err.println(" lastUpdated "+lastUpdated + " ts:"+ts+" static:"+tsStatic);
            while (!isCancelled()) {
                BufferedImage image = null;
                //
                if (mURL != null) {
                    try {
                        URLConnection conn = mURL.openConnection();
                        conn.setConnectTimeout(CONNECITON_TIMEOUT);
                        conn.setReadTimeout(CONNECITON_TIMEOUT);
                        InputStream inStream = conn.getInputStream();
                        image = ImageIO.read(inStream);
                        notifyConnectionListeners(true);
                    } catch (IOException e) {
                        notifyConnectionListeners(false);
                    }
                    if (image != null) {
                        publish(image);
                    }
                }
                //long currentTime = System.currentTimeMillis();
                //System.out.println(String.format("IP Cam Delay %d ms", (currentTime-lastUpdated)));
                //System.out.println(String.format("IP Cam Actual FPS %f", 1000./(currentTime-lastUpdated)));
                //System.out.println("mFps set to : "+mFps);
                //lastUpdated = currentTime;
                //long currentTime = System.currentTimeMillis();
                //System.out.println(String.format("IP Cam Delay %d ms", (currentTime-lastUpdated)));
                //String mURL_TEST = mURL+"";
                //if(mURL_TEST.contains("8080")){
                //    tsStatic = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(new Date());
                //    counter = 0;
                //}
                //ts =  new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(new Date());
                //counter++;

                //if (counter> oldCounter){
                //    oldCounter++;
                //}

                //System.out.println(oldCounter+" "+counter+" static:"+tsStatic+" ts:"+ts+" "+String.format("IP Cam Actual FPS %f", 1000./(currentTime-lastUpdated))+"  "+String.format("IP Cam Delay %d ms", (currentTime-lastUpdated))+" "+mURL);                                
                //currLag = currentTime-lastUpdated;

                //if(currLag > oldLag){
                //    oldLag=currLag;
                //    System.out.println("oldLag:"+oldLag);
                //}

                //System.out.println("mFps set to : "+mFps);
                //lastUpdated = currentTime;

                try {
                    Thread.sleep(1000 / mFps);
                    //System.out.println("mFps "+mFps);
                } catch (InterruptedException ex) {
                    Logger.getLogger(IPCamPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return null;
        }

        @Override
        protected void process(List<BufferedImage> list) {
            mImage = list.get(0);
            repaint();
            for (ConnectionListener connListener : mConnListeners) {
                connListener.onImageUpdate();
            }
        }
    }

    public void setFps(int fps) {
        //this.mFps = fps; removed Jan 9 2019 by Allan - this appears to be hard coded in UIFrontend.java at 20 fps so removing this.
    }

    /**
     * image save lag is set on the UIFrontEnd.java GUI in a slider.
     * it is used to increase/decrease the time between when images are saved to disk.
     */
    public void setImageSaveLag(int ImageSaveLag) {
        mImageSaveLag = ImageSaveLag;
    }

    public void setUrlAddrress(String urlPath) {
        if (urlPath == null || urlPath.trim().length() == 0) return;
        mUrlAddress = urlPath;
        try {
            mURL = new URL(mUrlAddress);
        } catch (MalformedURLException ex) {
            Logger.getLogger(IPCamPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public BufferedImage getImage() {
        return mImage;
    }

    public Point getImageXYFromLocal(int x, int y) {
        x -= mImageOffset.x;
        y -= mImageOffset.y;
        x /= mImageScale;
        y /= mImageScale;
        return new Point(x, y);
    }

    private void notifyConnectionListeners(boolean connected) {
        for (ConnectionListener connListener : mConnListeners) {
            if (connected) {
                connListener.onConnect();
            } else {
                connListener.onDisconnect();
            }
        }
    }

    public void addConnectionListener(ConnectionListener connListener) {
        mConnListeners.add(connListener);
    }

    public void addLineListener(LineListener lineListener) {
        mLineListeners.add(lineListener);
    }

    private String mImageFolder = null;
    private Executor mImageSaveService;
    private String mImageId;
    private long mLastImageSavedAt = 0;
    private Date mCurrentTimestamp;
    private long mImageCount = 0;

    private class ImageSaveTask implements Runnable {
        private final BufferedImage mImageToSave;
        private final Date mTimestamp;

        public ImageSaveTask(BufferedImage image, Date timestamp) {
            mImageToSave = image;
            mTimestamp = timestamp;
        }

        @Override
        public void run() {
            File outputFile;
            String filePath = String.format(
                    "%s%s%d_saved_%s_%s_%s.jpg",
                    mImageFolder,
                    File.separator,
                    mImageCount,
                    new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(mTimestamp),
                    mImageId, mURL.getPort()
            );
            try {
                outputFile = new File(filePath);
                ImageIO.write(mImageToSave, "jpg", outputFile);
                System.out.println(String.format(
                        "saving %d done %d", mURL.getPort(), mImageCount));
            } catch (IOException ex) {
                Logger.getLogger(IPCamPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    ;

    public void setImageFolder(String imageId, String path) {
        mImageFolder = path;
        mImageId = imageId;
        System.err.println("Images for " + mImageId + " will be saved here: " + mImageFolder);
        mImageSaveService = Executors.newFixedThreadPool(1);
    }

    public void setLineColor(Color color) {
        mLineColor = color;
        if (mActiveLine != null) {
            mActiveLine.setColor(color);
            mLineCollection.fireOnChangeListeners();
            repaint();
        }
    }

    public void setLineThickness(int value) {
        mLineThickness = value;
        if (mActiveLine != null) {
            mActiveLine.setThickness(mLineThickness);
            mLineCollection.fireOnChangeListeners();
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics grphcs) {
        super.paintComponent(grphcs);
        if (mImage == null) {
            return;
        }

        mImageScale = Float.min(
                getWidth() / (float) mImage.getWidth(),
                getHeight() / (float) mImage.getHeight());
        int scaledWidth = (int) (mImageScale * mImage.getWidth());
        int scaledHeight = (int) (mImageScale * mImage.getHeight());
        mImageOffset.x = (int) ((getWidth() - scaledWidth) * 0.5);
        mImageOffset.y = (int) ((getHeight() - scaledHeight) * 0.5);


        String mURL_TEST = mURL + "";
        //System.out.println(mURL_TEST+" + "+ mURL_TEST.contains("8080"));
//        Graphics g = mImage.createGraphics();
//        
//        g.setColor(Color.yellow);
//        g.drawLine(96, 127, 200, 127);

        Graphics2D g2 = mImage.createGraphics();// with this line of code i can add a grapics line to the saved image
        //Graphics2D g2 = (Graphics2D) grphcs;  // with this line of code the graphics line is only on the image on screen
        //g2.setStroke(new BasicStroke(6));
        g2.setColor(Color.green);


        //if(mURL_TEST.contains("8080") ){
        //    g2.setColor(Color.red);
        //grphcs.drawLine(280, 500, 375, 50); // bottom over, bottom down,  top over, top down
        //}
        //g2.drawLine((int) (mImage.getWidth()*.53),(int) (mImage.getHeight()*.8), (int) (mImage.getWidth()*.517), (int) (mImage.getHeight()*.36)); // bottom over, bottom down,  top over, top down 
        //g2.drawLine(600,200, 600,100); // bottom over, bottom down,  top over, top down // if this line is uncommented the line will show up on screen

        grphcs.drawImage(mImage,
                mImageOffset.x, mImageOffset.y,
                mImageOffset.x + scaledWidth, mImageOffset.y + scaledHeight,
                0, 0, mImage.getWidth(), mImage.getHeight(),
                null);
        /*grphcs.drawImage(mImage,
                0, 0,
                getWidth(), getHeight(), this);*/

        if (mImageFolder != null && SAVE_IMAGE) {
            mCurrentTimestamp = new Date();
            if ((mCurrentTimestamp.getTime() - mLastImageSavedAt) > mImageSaveLagTime) {
                mLastImageSavedAt = mCurrentTimestamp.getTime();
                mImageCount++;
                mImageSaveService.execute(new ImageSaveTask(mImage, mCurrentTimestamp));
            }
        }
        //g2.drawLine(100,800, 600,100); // bottom over, bottom down,  top over, top down // if this line is uncommented the line will show up saved image

        /*
        if (SAVE_IMAGE) {
            String fileTime = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(new Date());
            File outputfile2 = new File("saved"+fileTime+".jpg");
        
            if(imageCount<1){
                ImageID[0] = "initialized";
                ImageID[1] = "initialized";
                ImageID[2] = "initialized";
            }

            if(imageCount % mImageSaveLag == 0 // 20000 is every 3-4 minutes?
                ) 
            //System.out.println(imageCount+ "; "+fileTime+" "+mURL_TEST);
                {
                ImageID[0] = "save0";
                //System.out.println("updating 8080 flag "+imageCount);
        
                ImageID[1] = "save1";
                //System.out.println("updating 8081 flag "+imageCount);
            
                ImageID[2] = "save2";
                //System.out.println("updating 88 flag "+imageCount);
            }
        
                // flag to capture each image stream only once
                mURL_TEST = mURL+"";
                if(mURL_TEST.contains("8080") 
                        && ImageID[0]=="save0"){
                    ImageID[0]="saved";
                    try {
                        outputfile2=new File("C://Users//images//"+imageCount+"_saved"+fileTime+"_8080.jpg");
                        ImageIO.write(mImage, "jpg", outputfile2);
                        System.out.println("saving 8080 done "+imageCount);
                    } catch (IOException ex) {
                        Logger.getLogger(IPCamPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                if(mURL_TEST.contains("8081") 
                        && ImageID[1]=="save1"){
                    ImageID[1]="saved";
                    try {
                        outputfile2=new File("C://Users//images//"+imageCount+"_saved"+fileTime+"_8081.jpg");
                        ImageIO.write(mImage, "jpg", outputfile2);
                        System.out.println("saving 8081 done "+imageCount);
                    } catch (IOException ex) {
                        Logger.getLogger(IPCamPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                if(mURL_TEST.contains("88") 
                        && ImageID[2]=="save2"){
                    ImageID[2]="saved";
                    try {
                        outputfile2=new File("C://Users//images//"+imageCount+"_saved"+fileTime+"_88.jpg");
                        ImageIO.write(mImage, "jpg", outputfile2);
                        System.out.println("saving 88 done "+imageCount);
                    } catch (IOException ex) {
                        Logger.getLogger(IPCamPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                //System.out.println(ImageID[0]+" "+ImageID[1]+" "+ImageID[2]+" "+imageCount) ;
        }
            imageCount++;          
        */
        mLineCollection.draw((Graphics2D) grphcs, getWidth(), getHeight(), mActiveLine);
    }

    @Override
    public void mouseClicked(MouseEvent me) {
    }

    @Override
    public void mousePressed(MouseEvent me) {
        mActiveLine = mLineCollection.findAndLock(me.getX(), me.getY(), getWidth(), getHeight());
        if (mActiveLine == null) {
            mActiveLine = mLineCollection.addNew();
            mActiveLine.setColor(mLineColor);
            mActiveLine.setThickness(mLineThickness);
            mActiveLine.setStart(me.getX(), me.getY(), getWidth(), getHeight());
        }


        for (LineListener listener : mLineListeners) {
            listener.onSelect(mActiveLine);
        }
        repaint();
    }

    @Override
    public void mouseReleased(MouseEvent me) {
        if (mActiveLine != null) {
            if (mActiveLine.isTooShort()) mLineCollection.remove(mActiveLine);
            repaint();
            mLineCollection.fireOnChangeListeners();
        }
        //mActiveLine = null;
    }

    @Override
    public void mouseEntered(MouseEvent me) {
    }

    @Override
    public void mouseExited(MouseEvent me) {
    }

    @Override
    public void mouseDragged(MouseEvent me) {
        if (mActiveLine != null) {
            mActiveLine.setEnd(me.getX(), me.getY(), getWidth(), getHeight());
            repaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent me) {
    }

    public UiLine.Collection getUiLineCollection() {
        return mLineCollection;
    }
}