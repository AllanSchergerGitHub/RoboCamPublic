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
    private XYSeriesCollection mXYSerisCollection;

    public ChartParamsDataset(String chartName, ChartParamType[] paramTypes) {
        mChartName = chartName;
        mParamTypes = paramTypes;

        mParamsXYSeries = new HashMap<>();
        mXYSerisCollection = new XYSeriesCollection();
        for (ChartParamType paramType : paramTypes) {
            XYSeries series = new XYSeries(paramType.getName());
            mParamsXYSeries.put(paramType.getName(), series);

            mXYSerisCollection.addSeries(series);
        }
    }

    public XYSeriesCollection getDataset() {
        return mXYSerisCollection;
    }

    public String getChartName() {
        return mChartName;
    }

    public void addValue(ChartParamType paramType, double value) {
        XYSeries series = mParamsXYSeries.get(paramType.getName());
        if (isNull(series)) {
            if (!flagTripped) {
                System.err.println(paramType.getName() + " : " + mParamsXYSeries.get(paramType.getName()));
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
