package renderer;

import primitives.Point;
import primitives.Vector;

import java.util.ArrayList;
import java.util.List;

import static primitives.Util.isZero;

/**
 * Blackboard - target area sampling infrastructure for super-sampling.
 *
 * <p>Generates a list of 3D sample points spread evenly across a planar
 * target area. Supports two sampling patterns: GRID and JITTERED.</p>
 *
 * <p>The target area is defined by a center point, a side length ({@code size}),
 * and two orthonormal direction vectors ({@code vRight}, {@code vUp}) that form
 * the local coordinate system of the area.</p>
 *
 * <h3>Turning super-sampling off:</h3>
 * <ul>
 *   <li>{@code size == 0}       -> returns only the center point</li>
 *   <li>{@code numSamples == 1} -> returns only the center point</li>
 * </ul>
 */
public class Blackboard {

    /**
     * Sampling pattern type.
     * GRID    - sample points are exactly at the center of each cell (deterministic).
     * JITTERED - sample points are randomly shifted within each cell (reduces Moire patterns).
     */
    public enum SamplingPattern { GRID, JITTERED }

    /** Number of samples per row/column in the grid (total = numSamples^2). */
    private int numSamples = 1;

    /**
     * Side length of the square target area.
     * 0 means super-sampling is disabled — only the center point is returned.
     */
    private double size = 0;

    /** Center point of the target area in 3D world space. */
    private Point center;

    /** Local X-axis direction of the target area (must be normalized). */
    private Vector vRight;

    /** Local Y-axis direction of the target area (must be normalized). */
    private Vector vUp;

    /** Active sampling pattern. Default is GRID. */
    private SamplingPattern pattern = SamplingPattern.GRID;

    // ========================= Setters (fluent) =========================

    /**
     * Sets the number of samples per row/column.
     *
     * @param numSamples samples per dimension (must be >= 1)
     * @return this blackboard
     * @throws IllegalArgumentException if numSamples < 1
     */
    public Blackboard setNumSamples(int numSamples) {
        if (numSamples < 1)
            throw new IllegalArgumentException("numSamples must be >= 1");
        this.numSamples = numSamples;
        return this;
    }

    /**
     * Sets the side length of the square target area.
     * Pass 0 to disable super-sampling.
     *
     * @param size side length (must be >= 0)
     * @return this blackboard
     * @throws IllegalArgumentException if size < 0
     */
    public Blackboard setSize(double size) {
        if (size < 0)
            throw new IllegalArgumentException("size must be non-negative");
        this.size = size;
        return this;
    }

    /**
     * Sets the center point of the target area.
     *
     * @param center center point in 3D world space
     * @return this blackboard
     */
    public Blackboard setCenter(Point center) {
        this.center = center;
        return this;
    }

    /**
     * Sets the local X-axis direction vector of the target area.
     *
     * @param vRight normalized right direction
     * @return this blackboard
     */
    public Blackboard setVRight(Vector vRight) {
        this.vRight = vRight;
        return this;
    }

    /**
     * Sets the local Y-axis direction vector of the target area.
     *
     * @param vUp normalized up direction
     * @return this blackboard
     */
    public Blackboard setVUp(Vector vUp) {
        this.vUp = vUp;
        return this;
    }

    /**
     * Sets the sampling pattern (GRID or JITTERED).
     *
     * GRID     - deterministic, uniform grid. Good for debugging.
     * JITTERED - random offset within each cell. Reduces Moire patterns,
     *            produces more natural-looking noise instead of aliasing.
     *
     * @param pattern the desired sampling pattern
     * @return this blackboard
     */
    public Blackboard setPattern(SamplingPattern pattern) {
        this.pattern = pattern;
        return this;
    }

    // ========================= Core method =========================

    /**
     * Generates a list of 3D sample points spread across the target area.
     *
     * <p>GRID pattern: divides the area into numSamples x numSamples equal cells,
     * places a sample at the exact center of each cell.</p>
     *
     * <p>JITTERED pattern: same grid division, but each sample is randomly
     * offset within its cell by up to half a cell width in each direction.
     * This breaks up regular aliasing patterns into less noticeable noise.</p>
     *
     * <p>Returns only the center point when super-sampling is disabled
     * (size == 0 or numSamples == 1).</p>
     *
     * @return list of 3D sample points (at least one — the center)
     */
    public List<Point> generatePoints() {
        List<Point> points = new ArrayList<>();

        // Super-sampling disabled — return center only
        if (size == 0 || numSamples == 1) {
            points.add(center);
            return points;
        }

        double step  = size / numSamples;
        double start = -size / 2.0 + step / 2.0;

        for (int row = 0; row < numSamples; row++) {
            for (int col = 0; col < numSamples; col++) {

                double offsetRight = start + col * step;
                double offsetUp    = start + row * step;

                // JITTERED: add a random shift within the cell bounds.
                // (Math.random() - 0.5) * step gives a value in [-step/2, +step/2],
                // so the point stays inside its own cell — coverage stays uniform.
                if (pattern == SamplingPattern.JITTERED) {
                    offsetRight += (Math.random() - 0.5) * step;
                    offsetUp    += (Math.random() - 0.5) * step;
                }

                Point p = center;

                // Use isZero() instead of != 0 to guard against floating-point
                // near-zero values — calling scale(~0) would create a zero vector
                // and throw an IllegalArgumentException.
                if (!isZero(offsetRight)) p = p.add(vRight.scale(offsetRight));
                if (!isZero(offsetUp))    p = p.add(vUp.scale(offsetUp));

                points.add(p);
            }
        }

        return points;
    }
}