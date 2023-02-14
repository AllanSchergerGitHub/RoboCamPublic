package RoboCam;

import java.util.ArrayList;
import java.util.Collections;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

public class WheelConfigTable extends JTable {
    private DefaultCellEditor mDeviceListCellEditor;
    private final ArrayList<Integer> mDeviceListRowIndies = new ArrayList<>();

    public void setDeviceListComboBox(JComboBox<String> comboBox) {
        mDeviceListCellEditor = new DefaultCellEditor(comboBox);
    }

    public void setDeviceListRowIndices(int[] indices) {
        mDeviceListRowIndies.clear();
        for(int i: indices) {
            mDeviceListRowIndies.add(i);
        }
    }

    @Override
    public TableCellEditor getCellEditor(int r, int c) {
        if (mDeviceListRowIndies.contains(r)) return mDeviceListCellEditor;
        return super.getCellEditor(r, c);
    }
}
