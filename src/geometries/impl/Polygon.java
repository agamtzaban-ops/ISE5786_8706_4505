package geometries.impl;

import geometries.api.AABB;
import geometries.api.Geometry;
import geometries.api.Intersectable.Intersection;
import primitives.Point;
import primitives.Ray;
import primitives.Vector;

import java.util.List;

import static primitives.Util.isZero;

/**
 * Represents a convex polygon in a 3D Cartesian coordinate system.
 * <p>
 * The polygon is defined by an ordered sequence of vertices.
 * All vertices must lie in the same plane and be arranged along the
 * polygon edge path.
 * </p>
 * <p>
 * The polygon must be convex.
 * </p>
 *
 * @author Dan Zilberstein
 */
public class Polygon extends Geometry {
    /**
     * Ordered list of polygon vertices
     */
    protected final List<Point> _vertices;
    /**
     * Plane containing the polygon
     */
    protected final Plane _plane;
    /**
     * Number of vertices
     */
    private final int _size;

    /**
     * Constructs a convex polygon from ordered vertices.
     * <p>
     * The vertices must:
     * </p>
     * <ul>
     * <li>Contain at least three points</li>
     * <li>Be ordered along the polygon edge path</li>
     * <li>Lie in the same plane</li>
     * <li>Form a convex polygon</li>
     * </ul>
     *
     * @param vertices polygon vertices in edge order
     * @throws IllegalArgumentException if the vertices do not form a valid convex
     * polygon
     */
    public Polygon(Point... vertices) {
        if (vertices.length < 3)
            throw new IllegalArgumentException("A polygon can't have less than 3 vertices");
        _vertices = List.of(vertices);
        _size = vertices.length;

        // Create the supporting plane using the first three vertices.
        // The plane stores the constant normal of the polygon.
        _plane = new Plane(vertices[0], vertices[1], vertices[2]);
        if (_size == 3) return; // no need for more tests for a Triangle

        Vector n = _plane.getNormal(vertices[0]);
        // Subtracting identical vertices would create a zero vector (illegal)
        Vector edge1 = vertices[_size - 1].subtract(vertices[_size - 2]);
        Vector edge2 = vertices[0].subtract(vertices[_size - 1]);

        // Cross product of consecutive edges determines orientation.
        // All edge pairs must produce the same sign relative to the normal,
        // otherwise the polygon is concave or vertices are unordered.
        boolean positive = edge1.crossProduct(edge2).dotProduct(n) > 0;
        for (var i = 1; i < _size; ++i) {
            // Test that the point is in the same plane as calculated originally
            if (!isZero(vertices[i].subtract(vertices[0]).dotProduct(n)))
                throw new IllegalArgumentException("All vertices of a polygon must lay in the same plane");
            // Test the consequent edges have
            edge1 = edge2;
            edge2 = vertices[i].subtract(vertices[i - 1]);
            if (positive != (edge1.crossProduct(edge2).dotProduct(n) > 0))
                throw new IllegalArgumentException("All vertices must be ordered and the polygon must be convex");
        }
    }

    @Override
    public Vector getNormal(Point point) {
        return _plane.getNormal(point);
    }

    /**
     * Computes the bounding box of the polygon as the min/max extent of its
     * vertices on each axis. This is a tight box for a planar polygon (the
     * polygon never extends beyond the box formed by its own corners).
     * Triangle inherits this implementation unchanged.
     *
     * @return the axis-aligned bounding box of this polygon
     */
    @Override
    public AABB getBoundingBox() {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

        for (Point vertex : _vertices) {
            minX = Math.min(minX, vertex.getX());
            minY = Math.min(minY, vertex.getY());
            minZ = Math.min(minZ, vertex.getZ());
            maxX = Math.max(maxX, vertex.getX());
            maxY = Math.max(maxY, vertex.getY());
            maxZ = Math.max(maxZ, vertex.getZ());
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Helper method for calculating intersections using the NVI pattern.
     * The algorithm first checks for intersections with the polygon's plane.
     * If an intersection exists, it checks if the point is strictly inside the polygon's boundaries.
     *
     * @param ray the ray intersecting the polygon
     * @return a list containing the intersection object if it exists, or null otherwise
     */
    @Override
    protected List<Intersection> calcIntersectionsHelper(Ray ray,double maxDistance) {
        // 1. Intersect with the plane first
        //add maxDistance
        List<Intersection> planeIntersections = _plane.calcIntersections(ray,maxDistance);
        if (planeIntersections == null) {
            return null;
        }

        Point p0 = ray.origin();
        Vector v = ray.direction();
        int size = _vertices.size();
        boolean isPositive = false;

        // 2. Check if the point is inside the polygon
        for (int i = 0; i < size; i++) {
            Point p1 = _vertices.get(i);
            Point p2 = _vertices.get((i + 1) % size);

            Vector v1 = p1.subtract(p0);
            Vector v2 = p2.subtract(p0);
            Vector n = v1.crossProduct(v2).normalize();
            double vn = primitives.Util.alignZero(v.dotProduct(n));

            // Points on the edge or vertex are not considered intersections
            if (primitives.Util.isZero(vn)) {
                return null;
            }

            if (i == 0) {
                isPositive = vn > 0;
            } else if ((vn > 0) != isPositive) {
                // Sign changed, the point is outside the polygon
                return null;
            }
        }

        // Return the intersection point wrapped in an Intersection object linked to this Polygon
        return List.of(new Intersection(this, planeIntersections.get(0).p));
    }
    /**
     * Placeholder for texture mapping coordinates.
     * Currently not implemented for this geometry.
     *
     * @param p the 3D point
     * @return null temporarily
     */
    @Override
    public double[] getUV(Point p) {
        return null;
    }
}