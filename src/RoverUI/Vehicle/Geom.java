package RoverUI.Vehicle;

import java.awt.*;

public class Geom {
    public static double getSlope(Point.Double point1, Point.Double point2) {
        double distX = point2.x - point1.x;
        double distY = point2.y - point1.y;
        double angle = Math.toDegrees(Math.atan2(distY, distX));
        return angle;
    }

    public static Point.Double getDiff(Point.Double point2, Point.Double point1) {
        Point.Double diffPoint = new Point.Double();
        diffPoint.x = point2.x - point1.x;
        diffPoint.y = point2.y - point1.y;
        return diffPoint;
    }
    
    /*
    public static double getHypotenuse(Point.Double point) {
        return Math.sqrt(point.x*point.x+point.y*point.y);
    }
    */
}
