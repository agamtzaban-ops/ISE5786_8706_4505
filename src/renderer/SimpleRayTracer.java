package renderer;

import geometries.api.Intersectable.Intersection;
import primitives.Color;
import primitives.Ray;
import scene.Scene;

/**
 * Basic implementation of a ray tracer.
 */
public class SimpleRayTracer extends RayTracerBase {

    /**
     * Constructor receiving the scene.
     *
     * @param scene the scene to render
     */
    public SimpleRayTracer(Scene scene) {
        super(scene);
    }

    @Override
    public Color traceRay(Ray ray) {
        // Find intersections of the ray with scene geometries using NVI
        var intersections = _scene.geometries.calcIntersections(ray);

        // If no intersections, return background color
        if (intersections == null) {
            return _scene.background;
        }

        // Find the closest intersection object
        Intersection closestIntersection = ray.findClosestIntersection(intersections);

        // Calculate and return the color for the closest intersection
        return calcColor(closestIntersection);
    }

    /**
     * Helper method to calculate the color at a specific intersection.
     * Calculates the color by adding the ambient light scaled by the material's
     * attenuation factor, and the geometry's emission color.
     * Formula: Color = (Ambient * kA) + Emission
     *
     * @param intersection the intersection object containing the geometry, point, and material
     * @return the calculated color
     */
    private Color calcColor(Intersection intersection) {
        return _scene.ambientLight.getIntensity()
                // Scale ambient light by the intersection material's attenuation factor
                .scale(intersection.material.kA)
                // Add the emission color of the intersected geometry
                .add(intersection.geometry.getEmission());
    }
}