
package Utility;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Serializer {
    static final String POINT_TO_STRING_PATTERN = "Point(%d, %d)";
    static final Pattern STRING_TO_POINT_PATTERN = Pattern.compile(
            "Point\\((?<x>\\d+), (?<y>\\d+)\\)");

    public static String getStringOf(Point point) {
        return String.format(POINT_TO_STRING_PATTERN, point.x, point.y);
    }

    public static Point getPointFromString(String value) {
        Matcher matcher = STRING_TO_POINT_PATTERN.matcher(value);
        Point point = new Point();
        if (matcher.find()) {
            point.x = Integer.parseInt(matcher.group("x"));
            point.y = Integer.parseInt(matcher.group("y"));
        }
        return point;
    }

    static final String RECTANGLE_TO_STRING_PATTERN = "Rectangle(%d, %d, %d, %d)";
    static final Pattern STRING_TO_RECTANGLE_PATTERN = Pattern.compile(
            "Rectangle\\((?<x>-?\\d+), (?<y>-?\\d+), (?<w>\\d+), (?<h>\\d+)\\)");

    public static String getStringOf(Rectangle rectangle) {
        return String.format(
                RECTANGLE_TO_STRING_PATTERN,
                rectangle.x, rectangle.y,
                rectangle.width, rectangle.height);
    }

    public static Rectangle getRectangleFromString(String value) {
        Matcher matcher = STRING_TO_RECTANGLE_PATTERN.matcher(value);
        Rectangle rectangle = new Rectangle();
        if (matcher.find()) {
            rectangle.x = Integer.parseInt(matcher.group("x"));
            rectangle.y = Integer.parseInt(matcher.group("y"));
            rectangle.width = Integer.parseInt(matcher.group("w"));
            rectangle.height = Integer.parseInt(matcher.group("h"));

        }
        return rectangle;
    }

    static final String LINE_TO_STRING_PATTERN = "Line(%f, %f, %f, %f, %d, %d)";
    static final Pattern STRING_TO_LINE_PATTERN = Pattern.compile(
            "Line\\((?<x1>-?[\\d\\.]+), " +
                    "(?<y1>-?[\\d\\.]+), (?<x2>[\\d\\.]+), (?<y2>[\\d\\.]+)" +
                    "(, (?<color>-?\\d+), (?<lineWidth>\\d+))?" +
                    "\\)");

    public static String getStringOf(UiLine.Line line) {
        return String.format(
                LINE_TO_STRING_PATTERN,
                line.getPoint1X(), line.getPoint1Y(),
                line.getPoint2X(), line.getPoint2Y(),
                line.getColor().getRGB(), line.getThickness());
    }

    public static UiLine.Line getUiLineFromString(String value) {
        Matcher matcher = STRING_TO_LINE_PATTERN.matcher(value);
        UiLine.Line line = new UiLine.Line();
        if (matcher.find()) {
            line.setStart(
                    Float.parseFloat(matcher.group("x1")),
                    Float.parseFloat(matcher.group("y1")),
                    1, 1);
            line.setEnd(
                    Float.parseFloat(matcher.group("x2")),
                    Float.parseFloat(matcher.group("y2")),
                    1, 1);
            if (matcher.group("color") != null) {
                line.setColor(new Color(
                        Integer.parseInt(matcher.group("color"))
                ));
            }
            if (matcher.group("lineWidth") != null) {
                line.setThickness(Integer.parseInt(matcher.group("lineWidth")));
            }
        }
        return line;
    }
}
