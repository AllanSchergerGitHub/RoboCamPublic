package PhiDevice;

import java.util.ArrayList;

/**
 *
 * @author pc
 */
public class PositionList extends ArrayList<Double>{
    public PositionList(double ...positions) {
        for (double position: positions) {
            add(position);
        }
    }
    
    public void addMultiplyAndSet(double mult, PositionList positionList) {
        for(int i = 0; i < positionList.size(); i++) {
            if (i >= size()) break;
            set(i, get(i) + mult*positionList.get(i));
        }
    }
    
//    public double getAbsAvg() {
//        double sum = 0;
//        for(double val: this) sum += Math.abs(val);
//        return sum/size();
//    }
}
