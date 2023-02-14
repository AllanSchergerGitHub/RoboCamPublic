package RoverUI;

import PhiDevice.DeviceChannel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class DeviceListCellRenderer extends JLabel implements ListCellRenderer<PhiDevice.DeviceChannel>{
    public DeviceListCellRenderer() {
        setOpaque(true);
        setHorizontalAlignment(CENTER);
        setVerticalAlignment(CENTER);
    }
    
    @Override
    public Component getListCellRendererComponent(JList<? extends DeviceChannel> list, DeviceChannel value, int index, boolean isSelected, boolean cellHasFocus) {
        PhiDevice.DeviceChannel channel = (PhiDevice.DeviceChannel) value;
        //if (channel == null) return this;
        String status = "";
        if (channel.isOpen()) {
            status += "Open";
            setIcon(new javax.swing.ImageIcon(
                    getClass().getResource("/Images/green_box.png")));            
        } else {
            setIcon(new javax.swing.ImageIcon(
                    getClass().getResource("/Images/red_box.png")));            
        }
        if (status.length() > 0) status = "(" + status + ") ";
        setText(status + channel.getName());
        //System.out.println(" getListCellRendererComponent " + channel.getName());
        if (isSelected) {
            //setBackground(list.getSelectionBackground());
            //setForeground(list.getSelectionForeground());
            setBackground(Color.BLUE);
            setForeground(Color.WHITE);
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }
        return this;
    }
    
}
