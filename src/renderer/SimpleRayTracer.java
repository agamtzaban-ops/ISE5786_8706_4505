package renderer;

import geometries.api.Intersectable.Intersection;
import lighting.LightSource;
import primitives.*;
import scene.Scene;
import java.util.List;

/**
 * Implementation of RayTracer using the Phong reflection model.
 */
public class SimpleRayTracer extends RayTracerBase {

    /** Small shift factor to prevent self-shadowing due to floating-point inaccuracies */
    private static final double DELTA = 0.1;

    /**
     * Constructor for SimpleRayTracer.
     * @param scene The 3D scene to render.
     */
    public SimpleRayTracer(Scene scene) {
        super(scene);
    }

    @Override
    public Color traceRay(Ray ray) {
        var intersections = _scene.geometries.calcIntersections(ray);
        if (intersections == null) {
            return _scene.background;
        }

        Intersection closestPoint = ray.findClosestIntersection(intersections);
        return calcColor(closestPoint, ray);
    }

    /**
     * Calculates the color at an intersection point.
     * Combines emission, ambient light, and local lighting effects.
     *
     * @param intersection The intersection point.
     * @param ray The ray that intersected the geometry.
     * @return The calculated color.
     */
    private Color calcColor(Intersection intersection, Ray ray) {
        // Compute base color: Emission + Ambient Light (no kA scaling required here)
        return intersection.geometry.getEmission()
                .add(_scene.ambientLight.getIntensity())
                .add(calcColorLocalEffects(intersection, ray));
    }

    /**
     * Calculates local lighting effects (Diffuse + Specular) from all light sources.
     * Integrates shadow checking for each light source.
     *
     * @param intersection The intersection point.
     * @param ray The intersecting ray.
     * @return The color resulting from local effects.
     */
    private Color calcColorLocalEffects(Intersection intersection, Ray ray) {
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

                // --- STAGE 8: SHADOW CHECKING ---
                // If the intersection is updated to hold light and vectors as per course slides:
                // We validate the light source and check if the point is unshaded
                if (unshaded(lightSource, l, n, intersection)) {
                    Color iL = lightSource.getIntensity(intersection.p);
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
     * Checks if a specific point is unshaded (has a direct line of sight to the light source).
     *
     * @param lightSource The light source being evaluated.
     * @param l The vector from the light source to the point.
     * @param n The normal vector at the intersection point.
     * @param intersection The intersection data.
     * @return true if the point is unshaded, false if it is blocked by another geometry.
     */
    private boolean unshaded(LightSource lightSource, Vector l, Vector n, Intersection intersection) {
        // The shadow ray direction is opposite to the light vector (from point to light) [cite: 28]
        Vector pointToLight = l.scale(-1);

        // Calculate the tiny shift along the normal vector to prevent self-shadowing [cite: 28, 30]
        double nl = n.dotProduct(l);
        double shift = nl < 0 ? DELTA : -DELTA; // Shifting outwards based on the angle [cite: 28]
        Vector deltaVector = n.scale(shift);

        // Move the ray head slightly away from the geometry surface [cite: 28, 30]
        Point shiftedPoint = intersection.p.add(deltaVector);
        Ray shadowRay = new Ray(shiftedPoint, pointToLight);

        // 1. Calculate the distance from the intersection point to the light source
        double lightDistance = lightSource.getDistance(intersection.p);

        // 2. Call the updated method and pass the maximum distance to filter distant intersections!
        List<Intersection> shadowIntersections = _scene.geometries.calcIntersections(shadowRay, lightDistance);

        // 3. If null is returned - nothing blocks the light in this range -> the point is unshaded (true)
        // If a list is returned - there is an object blocking the light -> the point is in shadow (false)
        return shadowIntersections == null;
    }

    /**
     * Calculates the diffuse component of the light reflection.
     * Formula: kD * |n * l|
     *
     * @param material The material of the geometry.
     * @param nl The dot product of the normal and light vectors.
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
     * @param n The normal vector.
     * @param l The light direction vector.
     * @param nl The dot product of n and l.
     * @param v The camera view vector.
     * @return The specular attenuation factor.
     */
    private Double3 calcSpecular(Material material, Vector n, Vector l, double nl, Vector v) {
        // Calculate reflection vector: r = l - 2 * (l * n) * n
        Vector r = l.subtract(n.scale(2 * nl));

        // Calculate the dot product between the view vector and the reflection vector
        double minusVR = -primitives.Util.alignZero(v.dotProduct(r));

        // If the angle is such that the camera cannot see the specular reflection
        if (minusVR <= 0) {
            return Double3.ZERO;
        }

        // Raise to the power of shininess and scale by kS
        double factor = Math.pow(minusVR, material.nShininess);
        return material.kS.scale(factor);
    }
}