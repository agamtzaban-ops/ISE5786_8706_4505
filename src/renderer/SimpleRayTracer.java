package renderer;

import geometries.api.Intersectable.Intersection;
import lighting.LightSource;
import primitives.*;
import scene.Scene;
import java.util.List;

/**
 * Implementation of RayTracer using the Phong reflection model.
 * Supports local effects (diffuse, specular) and global effects (reflection, refraction),
 * including partial shadows for transparent objects.
 */
public class SimpleRayTracer extends RayTracerBase {

    /** Maximum recursion depth for color calculation */
    private static final int MAX_CALC_COLOR_LEVEL = 10;

    /** Minimum attenuation factor to continue recursion */
    private static final double MIN_CALC_COLOR_K = 0.001;

    /** Initial attenuation factor for the first ray */
    private static final Double3 INITIAL_K = Double3.ONE;

    /**
     * Constructor for SimpleRayTracer.
     * @param scene The 3D scene to render.
     */
    public SimpleRayTracer(Scene scene) {
        super(scene);
    }

    /**
     * Finds the closest intersection point to the origin of the ray.
     *
     * @param ray The ray to trace.
     * @return The closest Intersection object, or null if no intersections are found.
     */
    private Intersection findClosestIntersection(Ray ray) {
        var intersections = _scene.geometries.calcIntersections(ray);
        return intersections == null ? null : ray.findClosestIntersection(intersections);
    }

    @Override
    public Color traceRay(Ray ray) {
        Intersection closestPoint = findClosestIntersection(ray);
        return closestPoint == null ? _scene.background : calcColor(closestPoint, ray);
    }

    /**
     * Calculates the color at an intersection point.
     * Combines emission, ambient light, and local/global lighting effects.
     *
     * @param intersection The intersection point.
     * @param ray          The ray that intersected the geometry.
     * @return The calculated color.
     */
    private Color calcColor(Intersection intersection, Ray ray) {
        return calcColor(intersection, ray, MAX_CALC_COLOR_LEVEL, INITIAL_K)
                .add(_scene.ambientLight.getIntensity());
    }

    /**
     * Recursive method for calculating the color at an intersection point.
     * Computes both local effects and recursive global effects.
     *
     * @param intersection The intersection point.
     * @param ray          The intersecting ray.
     * @param level        The current recursion depth.
     * @param k            The accumulated attenuation factor.
     * @return The calculated color.
     */
    private Color calcColor(Intersection intersection, Ray ray, int level, Double3 k) {
        Color color = intersection.geometry.getEmission()
                .add(calcColorLocalEffects(intersection, ray, k));

        // Stopping condition: reached maximum recursion depth
        return level == 1 ? color : color.add(calcGlobalEffects(intersection, ray, level, k));
    }

    /**
     * Calculates and sums up all global effects (reflection and refraction).
     *
     * @param intersection The intersection point.
     * @param ray          The intersecting ray.
     * @param level        The current recursion depth.
     * @param k            The accumulated attenuation factor.
     * @return The resulting color from global effects.
     */
    private Color calcGlobalEffects(Intersection intersection, Ray ray, int level, Double3 k) {
        Vector v = ray.direction();
        Vector n = intersection.geometry.getNormal(intersection.p);
        Material material = intersection.material;

        return calcGlobalEffect(constructRefractionRay(intersection.p, v, n), level, k, material.kT)
                .add(calcGlobalEffect(constructReflectionRay(intersection.p, v, n), level, k, material.kR));
    }

    /**
     * Calculates a single global effect (either reflection or refraction) via a recursive call.
     *
     * @param ray   The secondary ray (reflection or refraction).
     * @param level The current recursion depth.
     * @param k     The accumulated attenuation factor.
     * @param kx    The specific attenuation factor for this effect (kR or kT).
     * @return The color contributed by this global effect.
     */
    private Color calcGlobalEffect(Ray ray, int level, Double3 k, Double3 kx) {
        Double3 kkx = k.product(kx);
        // Stopping condition: the attenuation is too small to make a visible impact
        if (kkx.isLowerThan(MIN_CALC_COLOR_K))  {
            return Color.BLACK;
        }

        Intersection closestPoint = findClosestIntersection(ray);
        if (closestPoint == null) {
            return _scene.background.scale(kx);
        }

        return calcColor(closestPoint, ray, level - 1, kkx).scale(kx);
    }

    /**
     * Constructs a reflection ray by shifting the origin point slightly.
     *
     * @param p The intersection point.
     * @param v The direction of the incoming ray.
     * @param n The normal vector at the intersection point.
     * @return The constructed reflection ray.
     */
    private Ray constructReflectionRay(Point p, Vector v, Vector n) {
        double vn = v.dotProduct(n);
        Vector r = v.subtract(n.scale(2 * vn));
        return new Ray(p, r, n);
    }

    /**
     * Constructs a refraction (transparency) ray by shifting the origin point slightly.
     *
     * @param p The intersection point.
     * @param v The direction of the incoming ray.
     * @param n The normal vector at the intersection point.
     * @return The constructed refraction ray.
     */
    private Ray constructRefractionRay(Point p, Vector v, Vector n) {
        return new Ray(p, v, n);
    }

    /**
     * Calculates local lighting effects (Diffuse + Specular) from all light sources.
     * Integrates partial shadow checking for each light source using transparency.
     *
     * @param intersection The intersection point.
     * @param ray          The intersecting ray.
     * @param k            The accumulated attenuation factor.
     * @return The color resulting from local effects.
     */
    private Color calcColorLocalEffects(Intersection intersection, Ray ray, Double3 k) {
        Vector v = ray.direction();
        Vector n = intersection.geometry.getNormal(intersection.p);
        double nv = primitives.Util.alignZero(n.dotProduct(v));
        if (nv == 0) {
            return Color.BLACK;
        }

        Color color = Color.BLACK;
        Material material = intersection.material;

        // Iterate over all light sources in the scene
        for (LightSource lightSource : _scene.lights) {
            Vector l = lightSource.getL(intersection.p);
            double nl = primitives.Util.alignZero(n.dotProduct(l));

            // Sign check: Ensures the light hits the surface from the same side the camera sees
            if (nl * nv > 0) {
                Double3 ktr = transparency(lightSource, l, n, intersection);

                // TIKUN: Only calculate if the overall impact (ktr * k) is noticeable!
                if (ktr.product(k).isGreaterThan(MIN_CALC_COLOR_K)) {
                    Color iL = lightSource.getIntensity(intersection.p).scale(ktr);
                    color = color.add(
                            iL.scale(calcDiffuse(material, nl)
                                    .add(calcSpecular(material, n, l, nl, v)))
                    );
                }
            }
        }
        return color;
    }

    /**
     * Calculates the transparency coefficient (ktr) for the shadow ray.
     * Accumulated attenuation factor of light reaching the intersection.
     *
     * @param lightSource  The light source being evaluated.
     * @param l            The vector from the light source to the point.
     * @param n            The normal vector at the intersection point.
     * @param intersection The intersection data.
     * @return The transparency attenuation factor (Double3).
     */
    private Double3 transparency(LightSource lightSource, Vector l, Vector n, Intersection intersection) {
        Vector lightDirection = l.scale(-1);
        Ray shadowRay = new Ray(intersection.p, lightDirection, n);

        double lightDistance = lightSource.getDistance(intersection.p);
        var shadowIntersections = _scene.geometries.calcIntersections(shadowRay, lightDistance);

        if (shadowIntersections == null) {
            return Double3.ONE;
        }

        Double3 ktr = Double3.ONE;
        for (Intersection intersect : shadowIntersections) {
            ktr = ktr.product(intersect.material.kT);
            if (intersect.material.kT.isLowerThan(MIN_CALC_COLOR_K)) {
                return Double3.ZERO;
            }
        }
        return ktr;
    }

    /**
     * Legacy method for basic shadow checking (Boolean unshaded).
     * Kept for backwards compatibility and academic requirements.
     *
     * @param lightSource  The light source being evaluated.
     * @param l            The vector from the light source to the point.
     * @param n            The normal vector at the intersection point.
     * @param intersection The intersection data.
     * @return true if the point is unshaded, false if it is blocked by an opaque geometry.
     */
    @SuppressWarnings("unused")
    private boolean unshaded(LightSource lightSource, Vector l, Vector n, Intersection intersection) {
        Vector pointToLight = l.scale(-1);
        Ray shadowRay = new Ray(intersection.p, pointToLight, n);
        double lightDistance = lightSource.getDistance(intersection.p);
        List<Intersection> shadowIntersections = _scene.geometries.calcIntersections(shadowRay, lightDistance);

        if (shadowIntersections == null) {
            return true;
        }

        for (Intersection intersect : shadowIntersections) {
            if (intersect.material.kT.isLowerThan(MIN_CALC_COLOR_K)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates the diffuse component of the light reflection.
     * Formula: kD * |n * l|
     *
     * @param material The material of the geometry.
     * @param nl       The dot product of the normal and light vectors.
     * @return The diffuse attenuation factor.
     */
    private Double3 calcDiffuse(Material material, double nl) {
        return material.kD.scale(Math.abs(nl));
    }

    /**
     * Calculates the specular component of the light reflection.
     * Formula: kS * (max(0, -v * r))^nShininess
     *
     * @param material The material of the geometry.
     * @param n        The normal vector.
     * @param l        The light direction vector.
     * @param nl       The dot product of n and l.
     * @param v        The camera view vector.
     * @return The specular attenuation factor.
     */
    private Double3 calcSpecular(Material material, Vector n, Vector l, double nl, Vector v) {
        Vector r = l.subtract(n.scale(2 * nl));
        double minusVR = -primitives.Util.alignZero(v.dotProduct(r));

        if (minusVR <= 0) {
            return Double3.ZERO;
        }

        double factor = Math.pow(minusVR, material.nShininess);
        return material.kS.scale(factor);
    }
}