package geometries.api;

import primitives.*;
import java.util.List;
import java.util.Objects;

/**
 * Abstract class for all objects that can be intersected by a ray.
 */
public abstract class Intersectable {

    /**
     * Inner PDS class to store intersection data and cache vector calculations.
     */
    public static class Intersection {
        /** The geometry that was intersected */
        public Geometry geometry;
        /** The point of intersection */
        public Point p;
        /** The material of the intersected geometry */
        public Material material;

        /* --- Intersection Cache Fields (Stage 7) --- */
        public Vector n;
        public Vector v;
        public double nv;
        public Vector l;
        public double nl;
        public Vector r;
        public double vminusR;

        /**
         * Constructor for Intersection.
         * @param geometry the intersected geometry
         * @param p        the intersection point
         */
        public Intersection(Geometry geometry, Point p) {
            this.geometry = geometry;
            this.p = p;
            this.material = (geometry == null) ? new Material() : geometry.getMaterial();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Intersection that = (Intersection) o;
            return this.geometry == that.geometry && Objects.equals(this.p, that.p);
        }

        @Override
        public String toString() {
            return "Intersection{geometry=" + geometry + ", p=" + p + "}";
        }
    }

    /**
     * Finds intersections of a ray with the geometry.
     * @param ray the ray to check
     * @return list of intersection objects
     */
    public final List<Intersection> calcIntersections(Ray ray) {
        return calcIntersectionsHelper(ray);
    }

    /**
     * Helper method for calculating intersections (NVI Pattern).
     */
    protected abstract List<Intersection> calcIntersectionsHelper(Ray ray);

    /**
     * Finds intersections as points only.
     */
    public final List<Point> findIntersections(Ray ray) {
        var intersections = calcIntersections(ray);
        return intersections == null ? null
                : intersections.stream()
                .map(intersection -> intersection.p)
                .toList();
    }
}