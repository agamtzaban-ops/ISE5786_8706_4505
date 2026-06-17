package geometries.api;

import primitives.Point;
import primitives.Ray;
import primitives.Vector;

import static primitives.Util.isZero;

/**
 * AABB - Axis-Aligned Bounding Box.
 *
 * <p>Represents the smallest box, aligned with the world X/Y/Z axes, that
 * fully contains a geometry (or an entire sub-tree of geometries). This is
 * the core building block of the Bounding Volume Hierarchy (BVH)
 * acceleration structure: testing a ray against a box is far cheaper than
 * testing it against the real geometry, so the BVH uses AABBs to quickly
 * reject whole branches of the scene that a ray cannot possibly hit.</p>
 *
 * <p>The class is immutable.</p>
 */
public final class AABB {

    /** Minimum coordinates of the box on each axis. */
    private final double _minX, _minY, _minZ;

    /** Maximum coordinates of the box on each axis. */
    private final double _maxX, _maxY, _maxZ;

    /**
     * Constructs a box from explicit min/max coordinates on each axis.
     * The caller is responsible for passing min &lt;= max on every axis.
     *
     * @param minX minimum X coordinate
     * @param minY minimum Y coordinate
     * @param minZ minimum Z coordinate
     * @param maxX maximum X coordinate
     * @param maxY maximum Y coordinate
     * @param maxZ maximum Z coordinate
     */
    public AABB(double minX, double minY, double minZ,
                double maxX, double maxY, double maxZ) {
        _minX = minX;
        _minY = minY;
        _minZ = minZ;
        _maxX = maxX;
        _maxY = maxY;
        _maxZ = maxZ;
    }

    /**
     * Returns the smallest box that contains both given boxes.
     * <p>A {@code null} argument is treated as "nothing to union with yet"
     * and is simply ignored — it does NOT mean "unbounded". Callers that
     * need to propagate "unbounded" semantics (e.g. a group of geometries
     * that contains an infinite Plane) must detect that case explicitly
     * before calling this method.</p>
     *
     * @param a first box (may be null)
     * @param b second box (may be null)
     * @return the union box, or null if both inputs are null
     */
    public static AABB union(AABB a, AABB b) {
        if (a == null) return b;
        if (b == null) return a;
        return new AABB(
                Math.min(a._minX, b._minX), Math.min(a._minY, b._minY), Math.min(a._minZ, b._minZ),
                Math.max(a._maxX, b._maxX), Math.max(a._maxY, b._maxY), Math.max(a._maxZ, b._maxZ));
    }

    /**
     * Returns the index of the axis along which this box is the longest
     * (0 = X, 1 = Y, 2 = Z). Used by the BVH builder to decide where to
     * split a node when partitioning geometries automatically.
     *
     * @return the longest axis index
     */
    public int longestAxis() {
        double dx = _maxX - _minX;
        double dy = _maxY - _minY;
        double dz = _maxZ - _minZ;
        if (dx >= dy && dx >= dz) return 0;
        return dy >= dz ? 1 : 2;
    }

    /**
     * Returns the coordinate of this box's center along the given axis
     * (0 = X, 1 = Y, 2 = Z). Used by the BVH builder to sort geometries by
     * their bounding-box centroid when splitting a node.
     *
     * @param axis the axis index (0, 1 or 2)
     * @return the center coordinate of the box on that axis
     */
    public double centroid(int axis) {
        return switch (axis) {
            case 0 -> (_minX + _maxX) / 2.0;
            case 1 -> (_minY + _maxY) / 2.0;
            default -> (_minZ + _maxZ) / 2.0;
        };
    }

    /**
     * Tests whether the given ray intersects this box within the parameter
     * range {@code [0, maxDistance]}, using the classic "slab" method: the
     * box is treated as the intersection of three pairs of parallel planes
     * (slabs), one pair per axis. The ray's valid parameter range is
     * intersected with each slab in turn; if the range ever becomes empty,
     * the ray misses the box.
     *
     * <p>This test does not look at what is inside the box at all — it only
     * answers "is it worth looking inside?", which is exactly what makes it
     * a cheap early-rejection test for the BVH.</p>
     *
     * @param ray         the ray to test
     * @param maxDistance the maximum valid distance along the ray (mirrors
     *                    the {@code maxDistance} parameter used throughout
     *                    the {@code Intersectable} hierarchy)
     * @return true if the ray's segment {@code [0, maxDistance]} crosses this box
     */
    public boolean intersects(Ray ray, double maxDistance) {
        Point origin = ray.origin();
        Vector dir = ray.direction();

        // tRange[0] = current lower bound, tRange[1] = current upper bound
        // of the parameter t for which the ray is still possibly inside the box.
        double[] tRange = {0.0, maxDistance};

        return clipAxis(origin.getX(), dir.getX(), _minX, _maxX, tRange)
                && clipAxis(origin.getY(), dir.getY(), _minY, _maxY, tRange)
                && clipAxis(origin.getZ(), dir.getZ(), _minZ, _maxZ, tRange);
    }

    /**
     * Narrows the valid parameter range {@code tRange} to the portion of the
     * ray that lies within the slab {@code [min, max]} on a single axis.
     *
     * @param o      ray origin coordinate on this axis
     * @param d      ray direction coordinate on this axis
     * @param min    slab minimum on this axis
     * @param max    slab maximum on this axis
     * @param tRange mutable {@code {low, high}} parameter range, updated in place
     * @return false if the range became empty (ray misses the slab)
     */
    private static boolean clipAxis(double o, double d, double min, double max, double[] tRange) {
        if (isZero(d)) {
            // Ray is parallel to this pair of planes: it stays inside the slab
            // for its entire length only if the origin is already within it.
            return o >= min && o <= max;
        }
        double invD = 1.0 / d;
        double t1 = (min - o) * invD;
        double t2 = (max - o) * invD;
        if (t1 > t2) {
            double tmp = t1;
            t1 = t2;
            t2 = tmp;
        }
        if (t1 > tRange[0]) tRange[0] = t1;
        if (t2 < tRange[1]) tRange[1] = t2;
        return tRange[0] <= tRange[1];
    }

    @Override
    public String toString() {
        return "AABB[(" + _minX + "," + _minY + "," + _minZ + ") - ("
                + _maxX + "," + _maxY + "," + _maxZ + ")]";
    }
}