package geometries.api;

import primitives.Material;
import primitives.Point;
import primitives.Ray;
import java.util.List;
import java.util.Objects;

/**
 * Abstract class for all intersectable objects in the scene.
 */
public abstract class Intersectable {

    /**
     * Helper class to store data about a ray-geometry intersection.
     * This is a Plain Data Structure (PDS).
     */
    public static class Intersection {
        /** The geometry that was intersected */
        public final Geometry geometry;
        /** The point of intersection */
        public final Point point;
        /** The material of the intersected geometry */
        public final Material material;

        /**
         * Constructor for Intersection data.
         * Initializes the material from the geometry, or uses a default material if geometry is null.
         *
         * @param geometry the intersected geometry
         * @param point    the intersection point
         */
        public Intersection(Geometry geometry, Point point) {
            this.geometry = geometry;
            this.point = point;

            // Extract material from geometry, or use default if geometry is null
            this.material = geometry == null ? new Material() : geometry.getMaterial();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Intersection that = (Intersection) o;
            // Compare geometry by reference (==) and point by value (.equals())
            return this.geometry == that.geometry &&
                    Objects.equals(this.point, that.point);
        }

        @Override
        public String toString() {
            return "Intersection{" +
                    "geometry=" + geometry +
                    ", point=" + point +
                    ", material=" + material +
                    '}';
        }
    }

    /**
     * Finds intersections of a ray with the geometry.
     * NVI Pattern: public non-virtual interface for finding intersections.
     *
     * @param ray the ray to check for intersections
     * @return list of intersection objects, or null if none
     */
    public final List<Intersection> calcIntersections(Ray ray) {
        return calcIntersectionsHelper(ray);
    }

    /**
     * Helper method for calculating intersections.
     * NVI Pattern: the only abstract method to be implemented by derived classes.
     *
     * @param ray the ray to check for intersections
     * @return list of intersection objects, or null if none
     */
    protected abstract List<Intersection> calcIntersectionsHelper(Ray ray);

    /**
     * Finds intersections of a ray with the geometry.
     * Backward compatibility method for old API using streams.
     *
     * @param ray the ray to check for intersections
     * @return list of intersection points, or null if none
     */
    public final List<Point> findIntersections(Ray ray) {
        var intersections = calcIntersections(ray);
        return intersections == null ? null
                : intersections.stream()
                .map(intersection -> intersection.point)
                .toList();
    }
}