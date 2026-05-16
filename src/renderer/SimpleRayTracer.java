package renderer;

import geometries.api.Intersectable.Intersection;
import lighting.LightSource;
import primitives.*;
import scene.Scene;

/**
 * Implementation of RayTracer using Phong reflection model.
 */
public class SimpleRayTracer extends RayTracerBase {

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
     */
    private Color calcColor(Intersection intersection, Ray ray) {
        if (!preprocessIntersection(intersection, ray)) {
            return intersection.geometry.getEmission()
                    .add(_scene.ambientLight.getIntensity());
        }

        return intersection.geometry.getEmission()
                .add(_scene.ambientLight.getIntensity())
                .add(calcColorLocalEffects(intersection));
    }

    /**
     * Calculates local lighting effects (Diffuse + Specular).
     */
    private Color calcColorLocalEffects(Intersection intersection) {
        Color color = Color.BLACK;
        for (LightSource lightSource : _scene.lights) {
            if (preprocessLightSource(intersection, lightSource)) {
                Color iL = lightSource.getIntensity(intersection.p);
                color = color.add(iL.scale(calcDiffuse(intersection)
                        .add(calcSpecular(intersection))));
            }
        }
        return color;
    }

    /**
     * Calculate Diffuse: kD * |n.l|
     */
    private Double3 calcDiffuse(Intersection intersection) {
        return intersection.material.kD.scale(Math.abs(intersection.nl));
    }

    /**
     * Calculate Specular: kS * (v.(-r))^nShininess
     */
    private Double3 calcSpecular(Intersection intersection) {
        double vrn = Math.pow(Math.max(0, intersection.vminusR),
                intersection.material.nShininess);
        return intersection.material.kS.scale(vrn);
    }
}