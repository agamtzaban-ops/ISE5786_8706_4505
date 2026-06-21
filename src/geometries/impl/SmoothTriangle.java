package geometries.impl;

import geometries.api.Intersectable.Intersection;
import primitives.Point;
import primitives.Ray;
import primitives.Vector;

import java.util.List;

import static primitives.Util.alignZero;

/**
 * Smooth-shaded triangle with per-vertex normals.
 *
 * The face normal (used for plane intersection and back-face culling) is derived
 * from the vertex winding exactly as in a flat Triangle.  The *shading* normal
 * returned by {@link #getNormal(Point)} is interpolated from the three vertex
 * normals using barycentric coordinates (Phong normal interpolation), so the
 * lighting wraps smoothly across the surface instead of showing hard polygon edges.
 *
 * Typical uses: organic terrain meshes, curved cactus arm tubes, spheroid barrel cacti.
 */
public class SmoothTriangle extends Polygon {

    private final Vector vn0, vn1, vn2;

    /**
     * @param p0  first  vertex position
     * @param p1  second vertex position
     * @param p2  third  vertex position
     * @param vn0 shading normal at p0 (need not be normalized — will be on construction)
     * @param vn1 shading normal at p1
     * @param vn2 shading normal at p2
     */
    public SmoothTriangle(Point p0, Point p1, Point p2,
                          Vector vn0, Vector vn1, Vector vn2) {
        super(p0, p1, p2);
        this.vn0 = vn0.normalize();
        this.vn1 = vn1.normalize();
        this.vn2 = vn2.normalize();
    }

    /**
     * Returns the barycentric-interpolated shading normal at the given surface point.
     * Falls back to the flat face normal if the interpolation produces a zero vector
     * (degenerate case that should not arise in valid geometry).
     */
    @Override
    public Vector getNormal(Point point) {
        Point p0 = _vertices.get(0);
        Point p1 = _vertices.get(1);
        Point p2 = _vertices.get(2);

        Vector e0 = p1.subtract(p0);
        Vector e1 = p2.subtract(p0);
        Vector ep = point.subtract(p0);

        double d00 = e0.dotProduct(e0);
        double d01 = e0.dotProduct(e1);
        double d11 = e1.dotProduct(e1);
        double d20 = ep.dotProduct(e0);
        double d21 = ep.dotProduct(e1);
        double denom = d00 * d11 - d01 * d01;

        // Barycentric weights: w1 (for p1/vn1), w2 (for p2/vn2), w0 = 1-w1-w2 (for p0/vn0)
        double w1 = (d11 * d20 - d01 * d21) / denom;
        double w2 = (d00 * d21 - d01 * d20) / denom;
        double w0 = 1.0 - w1 - w2;

        try {
            return vn0.scale(w0).add(vn1.scale(w1)).add(vn2.scale(w2)).normalize();
        } catch (IllegalArgumentException ignored) {
            return _plane.getNormal(point); // fallback to face normal
        }
    }

    /**
     * Triangle-specific ray–triangle test (faster than Polygon's general loop).
     * Identical to Triangle's implementation but linked to this SmoothTriangle.
     */
    @Override
    protected List<Intersection> calcIntersectionsHelper(Ray ray, double maxDistance) {
        List<Intersection> planeHit = _plane.calcIntersections(ray, maxDistance);
        if (planeHit == null) return null;

        Point org = ray.origin();
        Vector dir = ray.direction();
        Point q0 = _vertices.get(0), q1 = _vertices.get(1), q2 = _vertices.get(2);

        Vector a = q0.subtract(org), b = q1.subtract(org), c = q2.subtract(org);
        Vector n1 = a.crossProduct(b).normalize();
        Vector n2 = b.crossProduct(c).normalize();
        Vector n3 = c.crossProduct(a).normalize();

        double d1 = alignZero(dir.dotProduct(n1));
        double d2 = alignZero(dir.dotProduct(n2));
        double d3 = alignZero(dir.dotProduct(n3));

        if ((d1 > 0 && d2 > 0 && d3 > 0) || (d1 < 0 && d2 < 0 && d3 < 0))
            return List.of(new Intersection(this, planeHit.get(0).p));
        return null;
    }
}
