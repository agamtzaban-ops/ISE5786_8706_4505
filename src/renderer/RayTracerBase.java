package renderer;

import geometries.api.Intersectable.Intersection;
import lighting.LightSource;
import primitives.*;
import scene.Scene;
import static primitives.Util.*;

/**
 * Base class for all ray tracers.
 */
public abstract class RayTracerBase {
    /** The scene to be rendered */
    protected Scene _scene;

    /**
     * Constructor for RayTracerBase.
     * @param scene the scene
     */
    public RayTracerBase(Scene scene) {
        _scene = scene;
    }

    /**
     * Abstract method to trace a ray and return the color.
     */
    public abstract Color traceRay(Ray ray);

    /**
     * Prepares data that is constant for all light sources.
     * @return true if lighting should be calculated
     */
    protected boolean preprocessIntersection(Intersection intersection, Ray ray) {
        intersection.v = ray.direction().scale(-1);
        intersection.n = intersection.geometry.getNormal(intersection.p);
        intersection.nv = alignZero(intersection.n.dotProduct(intersection.v));

        // If nv is zero, the ray is tangent to the surface
        return !isZero(intersection.nv);
    }

    /**
     * Prepares data specific to a light source.
     * @return true if light hits the camera side
     */
    protected boolean preprocessLightSource(Intersection intersection, LightSource light) {
        intersection.l = light.getL(intersection.p);
        intersection.nl = alignZero(intersection.n.dotProduct(intersection.l));

        // Check if light and camera are on the same side
        if (intersection.nl * intersection.nv <= 0) {
            return false;
        }

        // Calculate reflection vector: r = l - 2 * (l.n) * n
        double nl2 = 2 * intersection.nl;
        intersection.r = intersection.l.subtract(intersection.n.scale(nl2));

        // Calculate v . (-r)
        intersection.vminusR = alignZero(intersection.v.dotProduct(intersection.r.scale(-1)));

        return true;
    }
}