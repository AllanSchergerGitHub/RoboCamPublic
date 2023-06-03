package PhiDevice;

import java.util.ArrayList;

/**
 * @author pc
 */
public class PositionList extends ArrayList<Double> {
    public PositionList(double... positions) {
        for (double position : positions) {
            add(position);
        }
    }
//
//    public double getAbsAvg() {
//        double sum = 0;
//        for(double val: this) sum += Math.abs(val);
//        return sum/size();
//    }
}
