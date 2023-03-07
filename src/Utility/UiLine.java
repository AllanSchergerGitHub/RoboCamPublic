package Utility;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public class UiLine {
    /**
     * This class holds a serializable drawling Line.
     */
    public static class Line {
        static final int OUTER_CIRCLE_RADIUS = 8;
        static final int INNER_CIRCLE_RADIUS = OUTER_CIRCLE_RADIUS / 2;
        static final Stroke END_POINT_STROKE = new BasicStroke(1);

        private Stroke mDrawStroke;

        // Fractional position of point 1
        private final Point2D.Float mFractionP1 = new Point2D.Float();
        // Fractional position of point 2
        private final Point2D.Float mFractionP2 = new Point2D.Float();

        // true if the P2 is the active point to move
        private boolean mIsEndActive = true;

        // Absolute position of point 1
        private final Point mAbsP1 = new Point();
        // Absolute position of point 2
        private final Point mAbsP2 = new Point();

        // Line color
        private Color mColor = Color.GREEN;
        private Color mEndColor = Color.BLACK;
        private int mThickness = 2;
        private boolean mIsActive = false;

        public Line() {
            mDrawStroke = new BasicStroke(mThickness);
        }

        public float getPoint1X() {
            return mFractionP1.x;
        }

        public float getPoint2X() {
            return mFractionP2.x;
        }

        public float getPoint1Y() {
            return mFractionP1.y;
        }

        public float getPoint2Y() {
            return mFractionP2.y;
        }

        public void setColor(Color color) {
            mColor = new Color(color.getRGB());
            mEndColor = new Color(
                    255 - mColor.getRed(),
                    255 - mColor.getGreen(),
                    255 - mColor.getBlue());
        }

        public Color getColor() {
            return mColor;
        }

        public void setThickness(int value) {
            mThickness = value;
            mDrawStroke = new BasicStroke(mThickness);
        }

        public int getThickness() {
            return mThickness;
        }

        public void setActive(boolean value) {
            mIsActive = value;
        }

        /**
         * Returns true if the line is of very short length
         *
         * @return
         */
        public boolean isTooShort() {
            return mAbsP1.distance(mAbsP2) < 5;
        }

        /**
         * Set the P1 and P2 to given position fractionally.
         *
         * @param x          - Absolute X position of point
         * @param y          - Absolute Y position of point
         * @param fullWidth  - Full width of the canvas
         * @param fullHeight - Full height of the canvas
         */
        public void setStart(float x, float y, int fullWidth, int fullHeight) {
            mFractionP1.x = x / (float) fullWidth;
            mFractionP1.y = y / (float) fullHeight;
            mFractionP2.x = mFractionP1.x;
            mFractionP2.y = mFractionP1.y;
        }

        /**
         * Set the position of the active end point.
         *
         * @param x          - Absolute X position of point
         * @param y          - Absolute Y position of point
         * @param fullWidth  - Full width of the canvas
         * @param fullHeight - Full height of the canvas
         */
        public void setEnd(float x, float y, int fullWidth, int fullHeight) {
            if (mIsEndActive) {
                mFractionP2.x = x / (float) fullWidth;
                mFractionP2.y = y / (float) fullHeight;
            } else {
                mFractionP1.x = x / (float) fullWidth;
                mFractionP1.y = y / (float) fullHeight;
            }
        }

        /**
         * Draw the line in the graphics area.
         *
         * @param grphcs
         * @param fullWidth
         * @param fullHeight
         */
        public void draw(Graphics2D grphcs, int fullWidth, int fullHeight) {
            mAbsP1.x = (int) (mFractionP1.x * fullWidth);
            mAbsP1.y = (int) (mFractionP1.y * fullHeight);
            mAbsP2.x = (int) (mFractionP2.x * fullWidth);
            mAbsP2.y = (int) (mFractionP2.y * fullHeight);

            grphcs.setColor(mColor);
            grphcs.setStroke(mDrawStroke);
            grphcs.drawLine(mAbsP1.x, mAbsP1.y, mAbsP2.x, mAbsP2.y);

            grphcs.setColor(mEndColor);
            grphcs.setStroke(END_POINT_STROKE);
            if (mIsActive) {
                grphcs.drawArc(
                        mAbsP1.x - OUTER_CIRCLE_RADIUS / 2,
                        mAbsP1.y - OUTER_CIRCLE_RADIUS / 2,
                        OUTER_CIRCLE_RADIUS,
                        OUTER_CIRCLE_RADIUS, 0, 360);
                grphcs.drawArc(
                        mAbsP2.x - INNER_CIRCLE_RADIUS / 2,
                        mAbsP2.y - INNER_CIRCLE_RADIUS / 2,
                        INNER_CIRCLE_RADIUS,
                        INNER_CIRCLE_RADIUS, 0, 360);
            }
            grphcs.drawArc(
                    mAbsP1.x - OUTER_CIRCLE_RADIUS,
                    mAbsP1.y - OUTER_CIRCLE_RADIUS,
                    OUTER_CIRCLE_RADIUS * 2,
                    OUTER_CIRCLE_RADIUS * 2, 0, 360);
            grphcs.drawArc(
                    mAbsP2.x - INNER_CIRCLE_RADIUS,
                    mAbsP2.y - INNER_CIRCLE_RADIUS,
                    INNER_CIRCLE_RADIUS * 2,
                    INNER_CIRCLE_RADIUS * 2, 0, 360);

        }

        public String getStringValue() {
            return Serializer.getStringOf(this);
        }
    }

    /**
     * This class holds list of lines and does
     * operations on them.
     */
    public static class Collection {
        // List of lines
        private final ArrayList<Line> mLines = new ArrayList<>();

        public interface OnChangeListener {
            public void onChange(Collection coll);
        }

        private final ArrayList<OnChangeListener> mOnChangeListener = new ArrayList<OnChangeListener>();

        public void addOnChangeListener(OnChangeListener listener) {
            mOnChangeListener.add(listener);
        }

        /**
         * Add a new Line and returns it.
         *
         * @return Line Newly added line
         */
        public Line addNew() {
            Line line = new Line();
            mLines.add(line);
            return line;
        }

        /**
         * Get last added Line if present.
         *
         * @return Line
         */
        public Line getLast() {
            if (mLines.isEmpty()) return null;
            return mLines.get(mLines.size() - 1);
        }

        /**
         * Removes the given line
         *
         * @param line The line to remove
         */
        public void remove(Line line) {
            mLines.remove(line);
        }

        /**
         * Find the line whose P1 or P2 is within a close area of the given point.
         * This function will set the active point of the matched line.
         *
         * @param x
         * @param y
         * @param fullWidth
         * @param fullHeight
         * @return Line - the matched Line
         */
        public Line findAndLock(int x, int y, int fullWidth, int fullHeight) {
            Point2D.Float point = new Point2D.Float(
                    x / (float) fullWidth, y / (float) fullHeight
            );
            float closeFactor = 10f / Math.min(fullWidth, fullHeight);
            for (Line line : mLines) {
                if (line.mFractionP2.distance(point) < closeFactor) {
                    line.mIsEndActive = true;
                    return line;
                } else if (line.mFractionP1.distance(point) < closeFactor) {
                    line.mIsEndActive = false;
                    return line;
                }
            }
            return null;
        }

        /**
         * Draw the lines on the graphics context.
         *
         * @param grphcs
         * @param fullWidth
         * @param fullHeight
         */
        public void draw(Graphics2D grphcs, int fullWidth, int fullHeight, Line activeLine) {
            for (Line line : mLines) {
                line.setActive(activeLine == line);
                line.draw(grphcs, fullWidth, fullHeight);
            }
        }

        public void loadFromString(String data) {
            for (String lineStr : data.split("\n")) {
                Line line = Serializer.getUiLineFromString(lineStr);
                if (line != null) {
                    mLines.add(line);
                }
            }
        }

        public String getStringValue() {
            if (mLines.size() == 0) return null;
            String[] lineStrList = new String[mLines.size()];
            for (int i = 0; i < mLines.size(); i++) {
                lineStrList[i] = mLines.get(i).getStringValue();
            }
            return String.join("\n", lineStrList);
        }

        public void fireOnChangeListeners() {
            for (OnChangeListener listener : mOnChangeListener) {
                listener.onChange(this);
            }
        }
    }
}
