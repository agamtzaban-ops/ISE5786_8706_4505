package renderer;

import primitives.Color;
import primitives.Ray;
import scene.Scene;

/**
 * Abstract base class for ray tracers.
 * Immutable class.
 */
abstract class RayTracerBase {
    /** The scene to be rendered */
    protected final Scene _scene;

    /**
     * Constructor receiving the scene.
     * @param scene the scene to render
     */
    protected RayTracerBase(Scene scene) {
        _scene = scene;
    }

    /**
     * Traces a ray and calculates the color of the hit point.
     * @param ray the ray to trace
     * @return the color of the point
     */
    abstract Color traceRay(Ray ray);
}