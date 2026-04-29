package renderer;

import primitives.Color;
import primitives.Point;
import primitives.Ray;
import scene.Scene;
import java.util.List;

/**
 * Basic implementation of a ray tracer.
 */
class SimpleRayTracer extends RayTracerBase {

    /**
     * Constructor receiving the scene.
     * @param scene the scene to render
     */
    public SimpleRayTracer(Scene scene) {
        super(scene);
    }

    @Override
    public Color traceRay(Ray ray) {
        // Find intersections of the ray with scene geometries
        List<Point> intersections = _scene.geometries.findIntersections(ray);

        // If no intersections, return background color
        if (intersections == null) {
            return _scene.background;
        }

        // Find the closest intersection point using the method we wrote in Ray
        Point closestPoint = ray.findClosestPoint(intersections);

        return calcColor(closestPoint);
    }

    /**
     * Helper method to calculate the color at a specific point.
     * Currently returns the ambient light intensity.
     * @param intersection the intersection point
     * @return the calculated color
     */
    private Color calcColor(Point intersection) {
        return _scene.ambientLight.getIntensity();
    }
}