package Chart;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.util.HashMap;

import static java.util.Objects.isNull;

public class ChartParamsDataset {
    private int mMaxXItem = 100; // how many data points are on the chart before data points start to drop off the chart

    private String mChartName;
    private ChartParamType[] mParamTypes;

    private boolean flagTripped = false;
    private HashMap<String, XYSeries> mParamsXYSeries;
    private XYSeriesCollection mDutyCycleXYSeriesCollection;
    private XYSeriesCollection mPosXYSeriesCollection;

    public ChartParamsDataset(String chartName, ChartParamType[] paramTypes) {
        mChartName = chartName; // chartName is based on the wheel name; for example "FrontLeft Chart", etc.
        mParamTypes = paramTypes; // Velocity, Angle, BLDC1_Position, BLDC2_Position, BLDC1_DutyCycle, BLDC2_DutyCycle

        mParamsXYSeries = new HashMap<>();
        mDutyCycleXYSeriesCollection = new XYSeriesCollection(); // this is a collection of different series; each series is one line on a chart.
        mPosXYSeriesCollection = new XYSeriesCollection();       // this is a collection of different series; each series is one line on a chart.
        
        for (ChartParamType paramType : paramTypes) {
            XYSeries series = new XYSeries(paramType.getName()); // this assigns a name to each series
            mParamsXYSeries.put(paramType.getName(), series);
            
            if (paramType == ChartParamType.BLDC_1_DUTY_CYCLE || paramType == ChartParamType.BLDC_2_DUTY_CYCLE) {
                mDutyCycleXYSeriesCollection.addSeries(series);
            } else if (paramType == ChartParamType.BLDC_1_POSITION || paramType == ChartParamType.BLDC_2_POSITION) {
                mPosXYSeriesCollection.addSeries(series);
            }
        }
    }

    public XYSeriesCollection getDutyCycleDataset() {
        return mDutyCycleXYSeriesCollection;
    }

    public XYSeriesCollection getPosDataset() {
        return mPosXYSeriesCollection;
    }

    public String getChartName() {
        return mChartName;
    }

    public void addValue(ChartParamType paramType, double value) {
        XYSeries series = mParamsXYSeries.get(paramType.getName());
        if (isNull(series)) {
            if (!flagTripped) { // The System.err.println will only execute the first time addValue is called with a paramType that does not exist in mParamsXYSeries. 
                System.err.println("This paramType does not exist in mParamsXYSeries. See code in Chart/ChartParamsDataset. : " + paramType.getName() + " : " + mParamsXYSeries.get(paramType.getName()));
            }
            flagTripped = true; // allows the error message to be printed one time per run as a warning; but since it seems harmless i don't need a warning every time.
        } else {
            series.add(System.currentTimeMillis(), value);
            if (series.getItemCount() > mMaxXItem) {
                series.delete(0, series.getItemCount() - mMaxXItem);
            }
        }
    }
}
