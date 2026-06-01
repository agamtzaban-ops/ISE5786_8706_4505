package renderer;

import primitives.Point;
import primitives.Vector;
import java.util.ArrayList;
import java.util.List;

/**
 * Blackboard - target area sampling infrastructure for super-sampling.
 * Generates a list of 3D sample points in a planar target area.
 * Used by all super-sampling features (anti-aliasing, soft shadows, etc.)
 */
public class Blackboard {

    /** Number of samples per row/column in the grid */
    private int numSamples = 1;

    /** Size (side length) of the square target area */
    private double size = 0;

    /** Center point of the target area */
    private Point center;

    /** Right direction vector of the target area (local X axis) */
    private Vector vRight;

    /** Up direction vector of the target area (local Y axis) */
    private Vector vUp;

    // =================== Setters (Builder style) ===================

    public Blackboard setNumSamples(int numSamples) {
        if (numSamples < 1)
            throw new IllegalArgumentException("numSamples must be >= 1");
        this.numSamples = numSamples;
        return this;
    }

    public Blackboard setSize(double size) {
        if (size < 0)
            throw new IllegalArgumentException("size must be non-negative");
        this.size = size;
        return this;
    }

    public Blackboard setCenter(Point center) {
        this.center = center;
        return this;
    }

    public Blackboard setVRight(Vector vRight) {
        this.vRight = vRight;
        return this;
    }

    public Blackboard setVUp(Vector vUp) {
        this.vUp = vUp;
        return this;
    }

    // =================== Core method ===================

    /**
     * Generates a list of 3D sample points spread evenly in the target area.
     * Uses a Grid pattern.
     * If size == 0 or numSamples == 1, returns only the center point.
     *
     * @return list of 3D sample points
     */
    public List<Point> generatePoints() {
        List<Point> points = new ArrayList<>();

        // No super-sampling — return center only
        if (size == 0 || numSamples == 1) {
            points.add(center);
            return points;
        }

        double step = size / numSamples;
        double start = -size / 2.0 + step / 2.0;

        for (int row = 0; row < numSamples; row++) {
            for (int col = 0; col < numSamples; col++) {
                double offsetRight = start + col * step;
                double offsetUp    = start + row * step;

                Point p = center;
                if (offsetRight != 0) p = p.add(vRight.scale(offsetRight));
                if (offsetUp    != 0) p = p.add(vUp.scale(offsetUp));
                points.add(p);
            }
        }

        return points;
    }
}
