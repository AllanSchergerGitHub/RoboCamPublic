package Utility;

import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import net.sf.marineapi.nmea.util.Position;

public class Waypointer {
    public static double EARTH_RADIUS_METER =  6371.230*1000;
    public static double EARTH_RADIUS_PERIMETER_METER = 2 * Math.PI * EARTH_RADIUS_METER;
    public static double EARTH_DEGREE_PER_METER = 360/EARTH_RADIUS_PERIMETER_METER;
    
    private final ArrayList<Position> mPoints = new ArrayList<>();

    private final Rectangle2D.Double mBoundary = new Rectangle2D.Double();
    private double mXSpan = 1;
    private double mYSpan = 1;
    private Point2D.Double mCenter = new Point2D.Double(0, 0);
    
    public void addPoint(Position position) {
        mPoints.add(position);
    }
    
    public void addPointFromCSV(
            File file, String xColumn, String yColumn) throws FileNotFoundException, IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file)); 
        String line;
        int lineIndex = 0;
        int xColumnIndex = -1, yColumnIndex = -1;
        while ((line=reader.readLine()) != null) {
            String[] items = line.split(",");
            if (lineIndex == 0) {
                for (int j = 0; j < items.length; j++) {
                    if (items[j].equals(xColumn)) xColumnIndex = j;
                    else if (items[j].equals(yColumn)) yColumnIndex = j;
                }
            } else {
                addPoint(new Position(
                        Double.parseDouble(items[yColumnIndex]),
                        Double.parseDouble(items[xColumnIndex])
                ));
            }
            lineIndex++;
        }
        calculateBoundary();
    }
    
    public int[][] getPolylinePoints(int pad, int width, int height) {
        int[] xPoints = new int[mPoints.size()];
        int[] yPoints = new int[mPoints.size()];
        int i = 0;
        for (Position position: mPoints) {
            Point2D.Double point = getPosition2Fraction(position);
            xPoints[i] = pad + (int) (point.x*width);
            yPoints[i] = pad + (int) (point.y*height);
            /*System.out.println(String.format("x=%d, y=%d, xf=%f, yf=%f, xp=%f, yp=%f", 
                        xPoints[i], yPoints[i], point.x, point.y, 
                        position.getLatitude(), position.getLatitude()));
            */
            i++;
        }
        return new int[][] {xPoints, yPoints};
    }
    
    public void calculateBoundary() {
        Double minX=null, maxX=null, minY = null, maxY = null;
        double x, y;
        for (Position position: mPoints) {
            x = position.getLongitude();
            y = position.getLatitude();
            minX = minX == null ? x : Math.min(minX, x);
            maxX = maxX == null ? x : Math.max(maxX, x);
            minY = minY == null ? y : Math.min(minY, y);
            maxY = maxY == null ? y : Math.max(maxY, y);
        }
        mXSpan = maxX-minX;
        mYSpan = maxY - minY;
        mXSpan = mYSpan = Math.max(mXSpan, mYSpan);
        mBoundary.setRect(minX, minY, mXSpan, mYSpan);
        mCenter.x = mBoundary.getCenterX();
        mCenter.y = mBoundary.getCenterY();
        
        // System.out.println(String.format("xspan=%f, yspan=%f", mXSpan, mYSpan));
    }
    
    public Point2D.Double getPosition2Fraction(Position position) {
        Point2D.Double point = new Point2D.Double(
                position.getLongitude(), position.getLatitude()
        );
        point.x = (point.x-mBoundary.getMinX())/mXSpan;
        point.y = (mBoundary.getMaxY()-point.y)/mYSpan;
        return point;
    }
    
    public Position getFraction2Position(Point2D.Double point) {
        return new Position(
                mBoundary.getMaxY() - point.y*mYSpan,
                point.x*mXSpan + mBoundary.getMinX()
        );
    }
    
    public Position getClosestPoint(Position position) {
        return Collections.min(mPoints, new Comparator<Position>() {
            @Override
            public int compare(Position t1, Position t2) {
                return (int) (t1.distanceTo(position)-t2.distanceTo(position));
            }            
        });
    }
    
    public Tracker createTracker() {
        return new Tracker();
    }
    
    public class Tracker {
        private Position mLastCurrentPosition ;
        private Position mCurrentPosition = new Position(0, 0);
        private Position mTargetPosition  = new Position(0, 0);
        
        private int mLastPointIndex = -1;
        private int mTargetPointIndex = -1;        
        private int mFinalPointIndex = -1;        
        private double mDistanceThresholdInMeter = 0.1;
        
        private double mTargetAngle = 0;
        private double mCurrentAngle = 0;
        
        public double getDistanceThreshold() {
            return mDistanceThresholdInMeter;
        }
        
        public void setFinalPosition(double lat, double lon) {
            Position position = getClosestPoint(new Position(lat, lon));
            mFinalPointIndex = mPoints.indexOf(position);
        }
        
        public void setCurrentPosition(double lat, double lon) {
            if (mLastCurrentPosition == null) {
                mLastCurrentPosition = new Position(0, 0);
            }
            mLastCurrentPosition.setLatitude(mCurrentPosition.getLatitude());
            mLastCurrentPosition.setLongitude(mCurrentPosition.getLongitude());
            
            mCurrentPosition.setLatitude(lat);
            mCurrentPosition.setLongitude(lon);
            
            double dx = lon - mLastCurrentPosition.getLongitude();
            double dy = lat - mLastCurrentPosition.getLatitude();
            mCurrentAngle = Math.atan2(dy, dx);
            //System.out.println(String.format("current=%f,target=%f",
            //        180*mCurrentAngle/Math.PI, getTargetAngleDeg()));
            //System.out.println(String.format("dx=%.12f,dy=%.12f", dx, dy));
        }

        public Point2D.Double getCurrentPositionFraction() {
            return getPosition2Fraction(mCurrentPosition);
        }
        
        public Point2D.Double getFinalPositionFraction() {
            return getPosition2Fraction(mPoints.get(mFinalPointIndex));
        }
        
        public Point2D.Double getTargetPositionFraction() {
            return getPosition2Fraction(mTargetPosition);
        }
        
        public double getTargetAngleDeg() {
            return 180*mTargetAngle/Math.PI;
        }
        
        public double getTargetAngle() {
            return mTargetAngle;
        }
        
        public double getCurrentAngle() {
            return mCurrentAngle;
        }
        
        public double getMoveX() {
            return Math.cos(mTargetAngle);
        }
        
        public double getMoveY() {
            return Math.sin(mTargetAngle);
        }
        
        public void move(double distanceInMeter) {
            setCurrentPosition(
                    mCurrentPosition.getLatitude() +
                            distanceInMeter*Math.sin(mTargetAngle)*EARTH_DEGREE_PER_METER,
                    mCurrentPosition.getLongitude()+
                            distanceInMeter*Math.cos(mTargetAngle)*EARTH_DEGREE_PER_METER
            );
        }
        
        public boolean update() {
            if (mLastPointIndex == -1) {
                Position position = getClosestPoint(mCurrentPosition);
                mTargetPointIndex = mPoints.indexOf(position);
                int nextIndex = mTargetPointIndex + (mFinalPointIndex > mTargetPointIndex ? 1 : -1);
                if (mCurrentPosition.distanceTo(mPoints.get(mTargetPointIndex)) >
                        mCurrentPosition.distanceTo(mPoints.get(nextIndex))) {
                    mTargetPointIndex = nextIndex;
                }
                mTargetPosition = mPoints.get(mTargetPointIndex);
                mLastPointIndex = 0; //dummy
            }
            if (mLastPointIndex == mFinalPointIndex) return false;

            // Check if tracker did not reach close enough of next target postion
            // System.out.println(String.format("dist=%f, thres=%f", mCurrentPosition.distanceTo(mTargetPosition), mDistanceThresholdInMeter));
            if (mCurrentPosition.distanceTo(mTargetPosition) > mDistanceThresholdInMeter ) {
                double dy = mTargetPosition.getLatitude()-mCurrentPosition.getLatitude();
                double dx = mTargetPosition.getLongitude()-mCurrentPosition.getLongitude();
                mTargetAngle =  Math.atan2(dy, dx);
                // System.out.println(String.format("mTargetAngle=%f", getTargetAngleDeg()));
            } else {
                mLastPointIndex = mTargetPointIndex;
                if (mTargetPointIndex != mFinalPointIndex) {
                    mTargetPointIndex += mFinalPointIndex > mLastPointIndex ? 1 : -1;
                }
                mTargetPosition = mPoints.get(mTargetPointIndex);
            }
            //System.out.println(String.format("last=%d, final=%d, target=%d", mLastPointIndex, mFinalPointIndex, mTargetPointIndex));
            return true;
        }        
    }    
}
