package renderer;

import geometries.api.Intersectable.Intersection;
import lighting.LightSource;
import primitives.*;
import scene.Scene;

import java.util.List;

/**
 * Implementation of RayTracer using the Phong reflection model.
 *
 * <p>Supports:</p>
 * <ul>
 *   <li>Local effects: diffuse and specular (Phong model)</li>
 *   <li>Global effects: reflection and refraction (recursive)</li>
 *   <li>Partial shadows: transparency-weighted shadow rays</li>
 *   <li>Soft Shadows: multiple shadow rays sampled across an area light
 *       via the {@link Blackboard} infrastructure (when {@code lightSource.getSize() > 0})</li>
 * </ul>
 */
public class SimpleRayTracer extends RayTracerBase {

    /** Maximum recursion depth for global color calculation. */
    private static final int MAX_CALC_COLOR_LEVEL = 10;

    /** Minimum accumulated attenuation below which recursion is stopped. */
    private static final double MIN_CALC_COLOR_K = 0.001;

    /** Initial attenuation factor for the primary ray. */
    private static final Double3 INITIAL_K = Double3.ONE;

    /**
     * Number of shadow-ray samples per dimension for Soft Shadows.
     * Total shadow rays per light = softShadowSamples^2.
     * 1 = disabled (hard shadows), 9 = demo quality, 17 = production quality.
     */
    private int _softShadowSamples = 1;

    /**
     * Sampling pattern used by the Blackboard for shadow ray distribution.
     * GRID     - deterministic, uniform grid (default).
     * JITTERED - random offset within each cell, reduces Moire patterns.
     */
    private Blackboard.SamplingPattern _samplingPattern = Blackboard.SamplingPattern.GRID;

    /**
     * Constructs a SimpleRayTracer for the given scene.
     *
     * @param scene the 3D scene to render
     */
    public SimpleRayTracer(Scene scene) {
        super(scene);
    }

    // ========================= Configuration =========================

    /**
     * Sets the number of shadow-ray samples per dimension for Soft Shadows.
     *
     * @param samples number of samples per row/column (must be >= 1)
     * @return this ray tracer (for method chaining)
     */
    public SimpleRayTracer setSoftShadowSamples(int samples) {
        if (samples < 1)
            throw new IllegalArgumentException("Soft shadow samples must be at least 1");
        this._softShadowSamples = samples;
        return this;
    }

    /**
     * Sets the sampling pattern used when generating shadow ray samples.
     *
     * GRID     - sample points at exact cell centers (deterministic, good for debugging).
     * JITTERED - random offset within each cell (reduces Moire patterns, more natural noise).
     *
     * This parameter belongs on the ray tracer because sampling pattern is a
     * shading-quality decision, not a camera or geometry decision (RDD principle).
     *
     * @param pattern the desired sampling pattern
     * @return this ray tracer (for method chaining)
     */
    public SimpleRayTracer setSamplingPattern(Blackboard.SamplingPattern pattern) {
        this._samplingPattern = pattern;
        return this;
    }

    // ========================= Public API =========================

    /**
     * Traces the given ray into the scene and returns the computed color.
     *
     * @param ray the ray to trace
     * @return the color seen along the ray
     */
    @Override
    public Color traceRay(Ray ray) {
        Intersection closestPoint = findClosestIntersection(ray);
        return closestPoint == null ? _scene.background : calcColor(closestPoint, ray);
    }

    // ========================= Color Calculation =========================

    private Color calcColor(Intersection intersection, Ray ray) {
        Color base = calcColor(intersection, ray, MAX_CALC_COLOR_LEVEL, INITIAL_K);
        if (intersection.material.emissionTexture) return base;
        return base.add(_scene.ambientLight.getIntensity());
    }

    private Color calcColor(Intersection intersection, Ray ray, int level, Double3 k) {
        Color emission = resolveEmission(intersection, ray);
        Color color = emission.add(calcColorLocalEffects(intersection, ray, k));
        return level == 1 ? color : color.add(calcGlobalEffects(intersection, ray, level, k));
    }

    private Color resolveEmission(Intersection intersection, Ray ray) {
        primitives.Material mat = intersection.material;

        if (mat.glowFalloff > 0.0) {
            Vector n  = intersection.geometry.getNormal(intersection.p);
            double nv = Math.abs(primitives.Util.alignZero(n.dotProduct(ray.direction())));
            double falloff = Math.pow(nv, mat.glowFalloff);
            return intersection.geometry.getEmission().scale(falloff);
        }

        if (mat.texture == null) return intersection.geometry.getEmission();

        double[] uv = intersection.geometry.getUV(intersection.p);
        Color texColor = mat.texture.getColor(uv[0], uv[1]);

        if (mat.emissionTexture) return texColor;

        return texColor.scale(0.60);
    }

    private Double3 getTextureDiffuseScale(Intersection intersection) {
        primitives.Material mat = intersection.material;
        if (mat.texture == null || mat.emissionTexture) return Double3.ONE;
        double[] uv = intersection.geometry.getUV(intersection.p);
        java.awt.Color c = mat.texture.getColor(uv[0], uv[1]).getColor();
        return new Double3(c.getRed() / 255.0, c.getGreen() / 255.0, c.getBlue() / 255.0);
    }

    // ========================= Global Effects =========================

    private Color calcGlobalEffects(Intersection intersection, Ray ray, int level, Double3 k) {
        Vector v = ray.direction();
        Vector n = intersection.geometry.getNormal(intersection.p);
        Material material = intersection.material;

        return calcGlobalEffect(constructRefractionRay(intersection.p, v, n), level, k, material.kT)
                .add(calcGlobalEffect(constructReflectionRay(intersection.p, v, n), level, k, material.kR));
    }

    private Color calcGlobalEffect(Ray ray, int level, Double3 k, Double3 kx) {
        Double3 kkx = k.product(kx);
        if (kkx.isLowerThan(MIN_CALC_COLOR_K)) return Color.BLACK;

        Intersection closestPoint = findClosestIntersection(ray);
        if (closestPoint == null) return _scene.background.scale(kx);

        return calcColor(closestPoint, ray, level - 1, kkx).scale(kx);
    }

    private Ray constructReflectionRay(Point p, Vector v, Vector n) {
        double vn = v.dotProduct(n);
        Vector r = v.subtract(n.scale(2 * vn));
        return new Ray(p, r, n);
    }

    private Ray constructRefractionRay(Point p, Vector v, Vector n) {
        return new Ray(p, v, n);
    }

    // ========================= Local Effects =========================

    private Color calcColorLocalEffects(Intersection intersection, Ray ray, Double3 k) {
        Vector v = ray.direction();
        Vector n = intersection.geometry.getNormal(intersection.p);
        double nv = primitives.Util.alignZero(n.dotProduct(v));
        if (nv == 0) return Color.BLACK;

        Color color = Color.BLACK;
        Material material = intersection.material;
        Double3 texScale = getTextureDiffuseScale(intersection);

        for (LightSource lightSource : _scene.lights) {
            Vector l = lightSource.getL(intersection.p);
            double nl = primitives.Util.alignZero(n.dotProduct(l));

            if (nl * nv > 0) {
                Double3 ktr = transparency(lightSource, l, n, intersection);

                if (ktr.product(k).isGreaterThan(MIN_CALC_COLOR_K)) {
                    Color iL = lightSource.getIntensity(intersection.p).scale(ktr);
                    color = color.add(
                            iL.scale(calcDiffuse(material, nl, texScale)
                                    .add(calcSpecular(material, n, l, nl, v)))
                    );
                }
            }
        }
        return color;
    }

    // ========================= Shadow / Transparency =========================

    /**
     * Computes the transparency factor between the intersection point and the light.
     *
     * When soft shadows are enabled (lightSource.getSize() > 0 and _softShadowSamples > 1),
     * uses the Blackboard to spread shadow rays across the light's area.
     * The active _samplingPattern (GRID or JITTERED) is passed to the Blackboard,
     * allowing the pattern to be switched from the test without touching this logic.
     */
    private Double3 transparency(LightSource lightSource, Vector l, Vector n,
                                 Intersection intersection) {
        // Hard shadows: single shadow ray
        if (lightSource.getSize() == 0 || _softShadowSamples == 1)
            return calcShadowRayKtr(intersection.p, l, n, lightSource);

        // ---- Soft Shadows via Blackboard ----
        Vector lightDir = l.normalize();
        Vector vRight   = buildPerpendicularVector(lightDir);
        Vector vUp      = lightDir.crossProduct(vRight).normalize();

        Point shiftedOrigin = intersection.p.add(n.scale(
                n.dotProduct(l) > 0 ? 0.0001 : -0.0001));

        double lightDist   = lightSource.getDistance(intersection.p);
        Vector toLight     = l.scale(-1);
        Point  lightCenter = intersection.p.add(toLight.scale(lightDist));

        // Pass the active sampling pattern to the Blackboard —
        // switching between GRID and JITTERED requires no changes here.
        List<Point> samplePoints = new Blackboard()
                .setCenter(lightCenter)
                .setSize(lightSource.getSize())
                .setVRight(vRight)
                .setVUp(vUp)
                .setNumSamples(_softShadowSamples)
                .setPattern(_samplingPattern)       // GRID or JITTERED from config
                .generatePoints();

        double totalR = 0, totalG = 0, totalB = 0;
        for (Point samplePoint : samplePoints) {
            Vector shadowDir  = samplePoint.subtract(shiftedOrigin).normalize();
            Ray    shadowRay  = new Ray(shiftedOrigin, shadowDir);
            double sampleDist = lightSource.getDistance(shiftedOrigin);
            Double3 sampleKtr = calcKtrAlongRay(shadowRay, sampleDist);
            totalR += sampleKtr._d1();
            totalG += sampleKtr._d2();
            totalB += sampleKtr._d3();
        }

        int count = samplePoints.size();
        return new Double3(totalR / count, totalG / count, totalB / count);
    }

    private Double3 calcShadowRayKtr(Point origin, Vector l, Vector n,
                                     LightSource lightSource) {
        Vector lightDirection = l.scale(-1);
        Ray shadowRay = new Ray(origin, lightDirection, n);
        return calcKtrAlongRay(shadowRay, lightSource.getDistance(origin));
    }

    private Double3 calcKtrAlongRay(Ray shadowRay, double lightDistance) {
        var shadowIntersections = _scene.geometries.calcIntersections(shadowRay, lightDistance);
        if (shadowIntersections == null) return Double3.ONE;

        Double3 ktr = Double3.ONE;
        for (Intersection intersect : shadowIntersections) {
            if (intersect.geometry.isLightSource()) continue;
            ktr = ktr.product(intersect.material.kT);
            if (ktr.isLowerThan(MIN_CALC_COLOR_K)) return Double3.ZERO;
        }
        return ktr;
    }

    private Vector buildPerpendicularVector(Vector v) {
        double ax = Math.abs(v.dotProduct(Vector.AXIS_X));
        double ay = Math.abs(v.dotProduct(Vector.AXIS_Y));
        double az = Math.abs(v.dotProduct(Vector.AXIS_Z));

        Vector axis = ax <= ay && ax <= az ? Vector.AXIS_X
                : ay <= az             ? Vector.AXIS_Y
                : Vector.AXIS_Z;

        return v.crossProduct(axis).normalize();
    }

    // ========================= Intersection Helper =========================

    private Intersection findClosestIntersection(Ray ray) {
        var intersections = _scene.geometries.calcIntersections(ray);
        return intersections == null ? null : ray.findClosestIntersection(intersections);
    }

    // ========================= Phong Components =========================

    private Double3 calcDiffuse(Material material, double nl, Double3 texScale) {
        return material.kD.product(texScale).scale(Math.abs(nl));
    }

    private Double3 calcSpecular(Material material, Vector n, Vector l, double nl, Vector v) {
        Vector r = l.subtract(n.scale(2 * nl));
        double minusVR = -primitives.Util.alignZero(v.dotProduct(r));
        if (minusVR <= 0) return Double3.ZERO;
        return material.kS.scale(Math.pow(minusVR, material.nShininess));
    }

    // ========================= Legacy =========================

    @SuppressWarnings("unused")
    private boolean unshaded(LightSource lightSource, Vector l, Vector n,
                             Intersection intersection) {
        Vector pointToLight = l.scale(-1);
        Ray shadowRay = new Ray(intersection.p, pointToLight, n);
        double lightDistance = lightSource.getDistance(intersection.p);
        var shadowIntersections = _scene.geometries.calcIntersections(shadowRay, lightDistance);

        if (shadowIntersections == null) return true;

        for (Intersection intersect : shadowIntersections) {
            if (intersect.material.kT.isLowerThan(MIN_CALC_COLOR_K)) return false;
        }
        return true;
    }
}