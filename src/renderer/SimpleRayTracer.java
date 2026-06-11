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
     * Total shadow rays per light = {@code SOFT_SHADOW_SAMPLES²}.
     *
     * <ul>
     *   <li>1  – disabled (single shadow ray, hard shadows)</li>
     *   <li>3  – 9 rays  (debug quality)</li>
     *   <li>9  – 81 rays (demo quality)</li>
     *   <li>17 – 289 rays (production quality)</li>
     * </ul>
     *
     * Set via {@link #setSoftShadowSamples(int)}.
     */
    private int _softShadowSamples = 1;

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
     * <p>This parameter is placed on the ray tracer (not on the camera) because
     * soft shadows are a property of the <em>shading calculation</em>, not of
     * how the camera fires primary rays. By RDD, the ray tracer owns shadow logic.</p>
     *
     * <p>Soft Shadows are only active when a light source also has
     * {@code size > 0} (set via {@link lighting.PointLight#setSize(double)}).
     * If the light size is 0, a single central shadow ray is always used
     * regardless of this setting.</p>
     *
     * @param samples number of samples per row/column (must be ≥ 1)
     * @return this ray tracer (for method chaining)
     * @throws IllegalArgumentException if {@code samples < 1}
     */
    public SimpleRayTracer setSoftShadowSamples(int samples) {
        if (samples < 1)
            throw new IllegalArgumentException("Soft shadow samples must be at least 1");
        this._softShadowSamples = samples;
        return this;
    }

    // ========================= Public API =========================

    /**
     * Traces the given ray into the scene and returns the computed color.
     * Returns the scene background color if no intersection is found.
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

    /**
     * Entry point for color calculation at an intersection.
     * Adds the scene's ambient light after the recursive computation.
     *
     * @param intersection the intersection point
     * @param ray          the ray that hit the geometry
     * @return the final color including ambient light
     */
    private Color calcColor(Intersection intersection, Ray ray) {
        Color base = calcColor(intersection, ray, MAX_CALC_COLOR_LEVEL, INITIAL_K);
        // Self-lit surfaces (skybox, star-fields) are their own light source —
        // adding scene ambient would wash out the black void between stars.
        if (intersection.material.emissionTexture) return base;
        return base.add(_scene.ambientLight.getIntensity());
    }

    /**
     * Recursive color calculation combining local and global lighting effects.
     *
     * @param intersection the intersection point
     * @param ray          the intersecting ray
     * @param level        remaining recursion depth
     * @param k            accumulated attenuation factor
     * @return the computed color at the intersection
     */
    private Color calcColor(Intersection intersection, Ray ray, int level, Double3 k) {
        Color emission = resolveEmission(intersection, ray);
        Color color = emission.add(calcColorLocalEffects(intersection, ray, k));
        return level == 1 ? color : color.add(calcGlobalEffects(intersection, ray, level, k));
    }

    /**
     * Resolves the emission component of an intersection.
     *
     * <ul>
     *   <li>If the material has a texture with {@code emissionTexture = true}
     *       (e.g. skybox, sun surface): returns the texture sample — always at full
     *       brightness, independent of scene lighting.</li>
     *   <li>Otherwise: returns the geometry's fixed emission color (may be BLACK
     *       when a diffuse texture is set, so the planet is lit only by lights).</li>
     * </ul>
     */
    /**
     * Resolves the emission component of an intersection.
     *
     * <ul>
     *   <li><b>Glow falloff</b> ({@code material.glowFalloff > 0}): used for
     *       self-luminous bodies such as the Sun.  The emission colour is multiplied
     *       by {@code |N·V|^glowFalloff}, producing smooth limb-darkening: the
     *       centre of the disk is at full brightness and the silhouette edge fades
     *       to black without any hard geometric boundary.</li>
     *   <li><b>Emission texture</b> ({@code emissionTexture = true}): skybox and
     *       other self-lit surfaces — returns the raw texture colour.</li>
     *   <li><b>Diffuse texture</b>: 60 % of the texture colour is emitted so the
     *       planet's appearance is visible on both the lit and the shadow side;
     *       the remaining 40 % is contributed by the Phong diffuse term.</li>
     *   <li><b>No texture</b>: returns the geometry's fixed emission colour.</li>
     * </ul>
     *
     * @param intersection the surface intersection
     * @param ray          the ray that produced the intersection (needed for N·V)
     * @return the emission colour at this intersection
     */
    private Color resolveEmission(Intersection intersection, Ray ray) {
        primitives.Material mat = intersection.material;

        // ── Glow falloff (Sun, stars): smooth limb-darkening effect ──────────
        if (mat.glowFalloff > 0.0) {
            Vector n  = intersection.geometry.getNormal(intersection.p);
            double nv = Math.abs(primitives.Util.alignZero(n.dotProduct(ray.direction())));
            double falloff = Math.pow(nv, mat.glowFalloff);
            return intersection.geometry.getEmission().scale(falloff);
        }

        // ── Texture-based emission ─────────────────────────────────────────
        if (mat.texture == null) return intersection.geometry.getEmission();

        double[] uv = intersection.geometry.getUV(intersection.p);
        Color texColor = mat.texture.getColor(uv[0], uv[1]);

        if (mat.emissionTexture)
            return texColor;                  // skybox / full self-lit surface

        // Planet: 60 % emitted (night side visible), 40 % from diffuse lighting
        return texColor.scale(0.60);
    }

    /**
     * Returns a [0,1]³ scale factor derived from the diffuse texture at the
     * intersection point.  Used to tint the Phong diffuse term so that lighting
     * illuminates the texture colours rather than a fixed kD value.
     *
     * <p>Returns {@link Double3#ONE} when no diffuse texture is set (normal
     * Phong behaviour unchanged).</p>
     */
    private Double3 getTextureDiffuseScale(Intersection intersection) {
        primitives.Material mat = intersection.material;
        if (mat.texture == null || mat.emissionTexture) return Double3.ONE;
        double[] uv = intersection.geometry.getUV(intersection.p);
        java.awt.Color c = mat.texture.getColor(uv[0], uv[1]).getColor();
        return new Double3(c.getRed() / 255.0, c.getGreen() / 255.0, c.getBlue() / 255.0);
    }

    // ========================= Global Effects =========================

    /**
     * Computes and sums all global effects (reflection and refraction).
     *
     * @param intersection the intersection point
     * @param ray          the intersecting ray
     * @param level        remaining recursion depth
     * @param k            accumulated attenuation factor
     * @return the color contributed by global effects
     */
    private Color calcGlobalEffects(Intersection intersection, Ray ray, int level, Double3 k) {
        Vector v = ray.direction();
        Vector n = intersection.geometry.getNormal(intersection.p);
        Material material = intersection.material;

        return calcGlobalEffect(constructRefractionRay(intersection.p, v, n), level, k, material.kT)
                .add(calcGlobalEffect(constructReflectionRay(intersection.p, v, n), level, k, material.kR));
    }

    /**
     * Computes a single global effect (reflection or refraction) recursively.
     * Stops recursion when the accumulated attenuation drops below
     * {@link #MIN_CALC_COLOR_K}.
     *
     * @param ray   the secondary ray (reflection or refraction)
     * @param level remaining recursion depth
     * @param k     accumulated attenuation factor
     * @param kx    the material's attenuation for this effect (kR or kT)
     * @return the color contributed by this global effect
     */
    private Color calcGlobalEffect(Ray ray, int level, Double3 k, Double3 kx) {
        Double3 kkx = k.product(kx);
        if (kkx.isLowerThan(MIN_CALC_COLOR_K))
            return Color.BLACK;

        Intersection closestPoint = findClosestIntersection(ray);
        if (closestPoint == null)
            return _scene.background.scale(kx);

        return calcColor(closestPoint, ray, level - 1, kkx).scale(kx);
    }

    /**
     * Constructs a reflection ray offset along the surface normal to avoid
     * self-intersection.
     *
     * @param p the intersection point
     * @param v direction of the incoming ray
     * @param n surface normal at the intersection
     * @return the reflection ray
     */
    private Ray constructReflectionRay(Point p, Vector v, Vector n) {
        double vn = v.dotProduct(n);
        Vector r = v.subtract(n.scale(2 * vn));
        return new Ray(p, r, n);
    }

    /**
     * Constructs a refraction (transparency) ray offset along the surface normal
     * to avoid self-intersection.
     *
     * @param p the intersection point
     * @param v direction of the incoming ray
     * @param n surface normal at the intersection
     * @return the refraction ray
     */
    private Ray constructRefractionRay(Point p, Vector v, Vector n) {
        return new Ray(p, v, n);
    }

    // ========================= Local Effects =========================

    /**
     * Computes the local lighting contribution (diffuse + specular) from all
     * light sources using the Phong reflection model.
     *
     * <p>Each light source is tested for visibility via {@link #transparency}.
     * When a light has {@code size > 0} and {@link #_softShadowSamples} &gt; 1,
     * multiple shadow rays are averaged to produce soft shadows.</p>
     *
     * @param intersection the intersection point
     * @param ray          the intersecting ray
     * @param k            accumulated attenuation factor
     * @return the local lighting color
     */
    private Color calcColorLocalEffects(Intersection intersection, Ray ray, Double3 k) {
        Vector v = ray.direction();
        Vector n = intersection.geometry.getNormal(intersection.p);
        double nv = primitives.Util.alignZero(n.dotProduct(v));
        if (nv == 0)
            return Color.BLACK;

        Color color = Color.BLACK;
        Material material = intersection.material;
        // Compute diffuse texture scale once (expensive UV lookup, done per pixel not per light)
        Double3 texScale = getTextureDiffuseScale(intersection);

        for (LightSource lightSource : _scene.lights) {
            Vector l = lightSource.getL(intersection.p);
            double nl = primitives.Util.alignZero(n.dotProduct(l));

            // Sign check: light must hit the same side of the surface as the camera
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
     * Computes the transparency factor {@code ktr} between the intersection point
     * and the light source.
     *
     * <h3>Soft Shadows</h3>
     * <p>When {@code lightSource.getSize() > 0} and {@link #_softShadowSamples} &gt; 1,
     * multiple shadow rays are cast toward different points spread across the
     * light's area (a disk orthogonal to {@code l}), using the {@link Blackboard}
     * infrastructure. The {@code ktr} values of all rays are averaged.</p>
     *
     * <p>Even rays that are fully blocked (ktr = 0) are included in the average,
     * so that the penumbra transition from light to shadow is physically correct.</p>
     *
     * <h3>Hard Shadows (default)</h3>
     * <p>When the light size is 0 or samples = 1, a single central shadow ray is
     * used (original behaviour, no performance cost).</p>
     *
     * @param lightSource  the light source being evaluated
     * @param l            normalized direction vector from the light to the point
     * @param n            surface normal at the intersection
     * @param intersection the intersection data
     * @return the averaged transparency attenuation factor
     */
    private Double3 transparency(LightSource lightSource, Vector l, Vector n,
                                 Intersection intersection) {
        // Hard shadows: single shadow ray (light has no area, or samples == 1)
        if (lightSource.getSize() == 0 || _softShadowSamples == 1)
            return calcShadowRayKtr(intersection.p, l, n, lightSource);

        // ---- Soft Shadows via Blackboard ----

        // Build an orthonormal basis for the light's target area.
        // The area disk is orthogonal to l (the direction from light to point).
        // We need a vector perpendicular to l to serve as vRight.
        Vector lightDir = l.normalize();
        Vector vRight = buildPerpendicularVector(lightDir);
        Vector vUp    = lightDir.crossProduct(vRight).normalize();

        // Shift the origin point along the normal once — avoids repeated self-intersection
        // offset per ray while keeping all rays consistent with each other.
        Point shiftedOrigin = intersection.p.add(n.scale(
                n.dotProduct(l) > 0 ? 0.0001 : -0.0001));

        // Compute the light center in 3D:
        // Start from the intersection point, go in the direction opposite to l
        // (i.e., toward the light) by the light's distance.
        double lightDist = lightSource.getDistance(intersection.p);
        Vector toLight   = l.scale(-1);                       // direction: point → light
        Point  lightCenter = intersection.p.add(toLight.scale(lightDist));

        // Generate sample points spread across the light's area disk
        List<Point> samplePoints = new Blackboard()
                .setCenter(lightCenter)
                .setSize(lightSource.getSize())
                .setVRight(vRight)
                .setVUp(vUp)
                .setNumSamples(_softShadowSamples)
                .generatePoints();

        // Accumulate ktr over all sample rays and average.
        // Even rays that are fully blocked (ktr = 0) are counted in the average
        // to ensure a physically correct penumbra transition.
        double totalR = 0, totalG = 0, totalB = 0;
        for (Point samplePoint : samplePoints) {
            Vector shadowDir = samplePoint.subtract(shiftedOrigin).normalize();
            Ray    shadowRay = new Ray(shiftedOrigin, shadowDir);
            double sampleDist = lightSource.getDistance(shiftedOrigin);
            Double3 sampleKtr = calcKtrAlongRay(shadowRay, sampleDist);
            totalR += sampleKtr._d1();
            totalG += sampleKtr._d2();
            totalB += sampleKtr._d3();
        }

        int count = samplePoints.size();
        return new Double3(totalR / count, totalG / count, totalB / count);
    }

    /**
     * Computes the transparency factor for a single shadow ray from the
     * intersection point toward the light source.
     *
     * @param origin      the (offset) shadow ray origin
     * @param l           normalized direction from light to point
     * @param n           surface normal at the intersection
     * @param lightSource the light source
     * @return the transparency factor along the shadow ray
     */
    private Double3 calcShadowRayKtr(Point origin, Vector l, Vector n,
                                     LightSource lightSource) {
        Vector lightDirection = l.scale(-1);
        Ray shadowRay = new Ray(origin, lightDirection, n);
        return calcKtrAlongRay(shadowRay, lightSource.getDistance(origin));
    }

    /**
     * Traces a shadow ray and accumulates the transparency product of all
     * blocking geometries between the origin and {@code lightDistance}.
     *
     * <p>Returns {@link Double3#ZERO} immediately if any opaque geometry is
     * encountered (kT below {@link #MIN_CALC_COLOR_K}).</p>
     *
     * @param shadowRay     the shadow ray to trace
     * @param lightDistance maximum distance to consider for intersections
     * @return the accumulated transparency factor along the ray
     */
    private Double3 calcKtrAlongRay(Ray shadowRay, double lightDistance) {
        var shadowIntersections = _scene.geometries.calcIntersections(shadowRay, lightDistance);
        if (shadowIntersections == null)
            return Double3.ONE;

        Double3 ktr = Double3.ONE;
        for (Intersection intersect : shadowIntersections) {
            // Light-source geometries (the Sun) must never block their own light.
            // Without this skip the PointLight inside the Sun sphere would be
            // occluded by the Sun's own geometry, leaving all planets in shadow.
            if (intersect.geometry.isLightSource()) continue;

            ktr = ktr.product(intersect.material.kT);
            if (ktr.isLowerThan(MIN_CALC_COLOR_K))
                return Double3.ZERO;
        }
        return ktr;
    }

    /**
     * Builds an arbitrary unit vector perpendicular to the given vector.
     *
     * <p>Used to construct a local coordinate system for the light's area disk.
     * The approach avoids degenerate cases by choosing the least-dominant axis.</p>
     *
     * @param v the reference vector (must be normalized)
     * @return a unit vector perpendicular to {@code v}
     */
    private Vector buildPerpendicularVector(Vector v) {
        // Use dotProduct with unit axes to extract components — avoids accessing
        // the protected _xyz field of Point from outside the primitives package.
        double ax = Math.abs(v.dotProduct(Vector.AXIS_X));
        double ay = Math.abs(v.dotProduct(Vector.AXIS_Y));
        double az = Math.abs(v.dotProduct(Vector.AXIS_Z));

        // Choose the axis least aligned with v to avoid a near-zero cross product
        Vector axis = ax <= ay && ax <= az ? Vector.AXIS_X
                : ay <= az             ? Vector.AXIS_Y
                : Vector.AXIS_Z;

        return v.crossProduct(axis).normalize();
    }
    // ========================= Intersection Helper =========================

    /**
     * Finds the closest intersection of the given ray with any geometry in the scene.
     *
     * @param ray the ray to test
     * @return the closest {@link Intersection}, or {@code null} if none exists
     */
    private Intersection findClosestIntersection(Ray ray) {
        var intersections = _scene.geometries.calcIntersections(ray);
        return intersections == null ? null : ray.findClosestIntersection(intersections);
    }

    // ========================= Phong Components =========================

    /**
     * Computes the diffuse lighting component.
     * Formula: {@code kD × |n·l|}
     *
     * @param material the surface material
     * @param nl       dot product of the surface normal and light direction
     * @return the diffuse attenuation factor
     */
    /**
     * Computes the diffuse lighting component, optionally tinted by a texture.
     * Formula: {@code kD × texScale × |n·l|}
     *
     * @param material the surface material
     * @param nl       dot product of the surface normal and light direction
     * @param texScale per-channel [0,1] texture colour from {@link #getTextureDiffuseScale};
     *                 pass {@link Double3#ONE} for plain (untextured) surfaces
     * @return the diffuse attenuation factor
     */
    private Double3 calcDiffuse(Material material, double nl, Double3 texScale) {
        return material.kD.product(texScale).scale(Math.abs(nl));
    }

    /**
     * Computes the specular lighting component.
     * Formula: {@code kS × max(0, −v·r)^nShininess}
     *
     * @param material the surface material
     * @param n        surface normal
     * @param l        light direction vector
     * @param nl       dot product of {@code n} and {@code l}
     * @param v        camera view direction
     * @return the specular attenuation factor
     */
    private Double3 calcSpecular(Material material, Vector n, Vector l, double nl, Vector v) {
        Vector r = l.subtract(n.scale(2 * nl));
        double minusVR = -primitives.Util.alignZero(v.dotProduct(r));
        if (minusVR <= 0)
            return Double3.ZERO;
        return material.kS.scale(Math.pow(minusVR, material.nShininess));
    }

    // ========================= Legacy =========================

    /**
     * Legacy boolean shadow check (unshaded).
     * Retained for backwards compatibility with earlier homework stages.
     *
     * @param lightSource  the light source
     * @param l            direction from light to point
     * @param n            surface normal
     * @param intersection the intersection data
     * @return {@code true} if the point is unshaded
     */
    @SuppressWarnings("unused")
    private boolean unshaded(LightSource lightSource, Vector l, Vector n,
                             Intersection intersection) {
        Vector pointToLight = l.scale(-1);
        Ray shadowRay = new Ray(intersection.p, pointToLight, n);
        double lightDistance = lightSource.getDistance(intersection.p);
        var shadowIntersections = _scene.geometries.calcIntersections(shadowRay, lightDistance);

        if (shadowIntersections == null)
            return true;

        for (Intersection intersect : shadowIntersections) {
            if (intersect.material.kT.isLowerThan(MIN_CALC_COLOR_K))
                return false;
        }
        return true;
    }
}