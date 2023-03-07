package Utility;

import java.awt.*;
import java.awt.geom.Point2D;

public class Drawing {
    public static final Color TRANS_WHITE = new Color(1f, 1f, 1f, 0.8f);

    public static Point2D.Double drawString(Color textColor, Color backgroundColor, int mFontSetting, int mFontSize,
                                            Graphics2D g2d, String text, double x, double y,
                                            boolean reversed) {
        g2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
        g2d.setRenderingHint(
                RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setFont(new Font("default", mFontSetting, mFontSize));

        FontMetrics metrics = g2d.getFontMetrics();
        int textHeight = metrics.getHeight();

        int textWidth = 0;
        for (String line : text.split("\n")) {
            textWidth = metrics.stringWidth(line);

            g2d.setColor(backgroundColor);
            g2d.fillRect(
                    (int) (x - (reversed ? textWidth : 0)),
                    (int) y - textHeight,
                    textWidth,
                    (int) (textHeight * 1.5));

            g2d.setColor(textColor);

            g2d.drawString(
                    line,
                    (int) (x - (reversed ? textWidth : 0)),
                    (int) y);
            y += textHeight * 1.5;
        }
        return new Point2D.Double(x + textWidth, y);
    }
}