package lighting;

import primitives.Color;
import primitives.Point;
import primitives.Vector;

/**
 * Represents a point light source — a light that radiates from a single
 * position in all directions with distance-based attenuation.
 *
 * <p>Attenuation is modelled as:</p>
 * <pre>
 *   I(p) = I₀ / (kC + kL·d + kQ·d²)
 * </pre>
 * where {@code d} is the distance from the light to point {@code p}.
 *
 * <p>When {@code size > 0}, this light acts as an <em>area light</em>:
 * the {@link renderer.SimpleRayTracer} will cast multiple shadow rays spread
 * across a disk of the given radius, producing soft shadow penumbras.
 * A size of {@code 0} (the default) produces hard shadows.</p>
 */
public class PointLight extends Light implements LightSource {

    /** The position of the light source in world space. */
    protected final Point _position;

    /** Constant attenuation coefficient (default: 1). */
    private double _kC = 1.0;

    /** Linear attenuation coefficient (default: 0). */
    private double _kL = 0.0;

    /** Quadratic attenuation coefficient (default: 0). */
    private double _kQ = 0.0;

    /**
     * Physical radius of the light source area.
     *
     * <ul>
     *   <li>0 – point light (hard shadows, default)</li>
     *   <li>&gt;0 – area light; larger values widen the soft-shadow penumbra</li>
     * </ul>
     */
    private double _size = 0;

    /**
     * Constructs a point light at the given position with the given intensity.
     *
     * @param intensity the color intensity of the light
     * @param position  the position of the light in world space
     */
    public PointLight(Color intensity, Point position) {
        super(intensity);
        this._position = position;
    }

    // ========================= Setters (Fluent API) =========================

    /**
     * Sets the constant attenuation coefficient.
     *
     * @param kC constant attenuation (must be ≥ 1 to avoid amplification)
     * @return this light (for method chaining)
     */
    public PointLight setKc(double kC) {
        this._kC = kC;
        return this;
    }

    /**
     * Sets the linear attenuation coefficient.
     *
     * @param kL linear attenuation (≥ 0)
     * @return this light (for method chaining)
     */
    public PointLight setKl(double kL) {
        this._kL = kL;
        return this;
    }

    /**
     * Sets the quadratic attenuation coefficient.
     *
     * @param kQ quadratic attenuation (≥ 0)
     * @return this light (for method chaining)
     */
    public PointLight setKq(double kQ) {
        this._kQ = kQ;
        return this;
    }

    /**
     * Sets the physical radius of this light's area.
     *
     * <p>Setting a positive size enables Soft Shadows: the ray tracer will
     * sample multiple points within a disk of this radius around the light
     * position and average their shadow contributions.</p>
     *
     * <p>Set to {@code 0} (the default) for a hard-shadow point light.</p>
     *
     * @param size radius of the area light (must be ≥ 0)
     * @return this light (for method chaining)
     * @throws IllegalArgumentException if {@code size < 0}
     */
    public PointLight setSize(double size) {
        if (size < 0)
            throw new IllegalArgumentException("Light size must be non-negative");
        this._size = size;
        return this;
    }

    // ========================= LightSource Implementation =========================

    /**
     * Returns the attenuated intensity of the light at point {@code p}.
     *
     * @param p the point in the scene
     * @return the attenuated color intensity
     */
    @Override
    public Color getIntensity(Point p) {
        double d = _position.distance(p);
        double attenuation = _kC + _kL * d + _kQ * d * d;
        return _intensity.scale(1d / attenuation);
    }

    /**
     * Returns the normalized direction vector from this light to point {@code p}.
     *
     * @param p the point in the scene
     * @return normalized direction vector from the light toward {@code p}
     */
    @Override
    public Vector getL(Point p) {
        return p.subtract(_position).normalize();
    }

    /**
     * Returns the Euclidean distance from this light's position to {@code point}.
     *
     * @param point the point to measure distance to
     * @return distance from the light to {@code point}
     */
    @Override
    public double getDistance(Point point) {
        return _position.distance(point);
    }

    /**
     * Returns the physical radius of this light source's area.
     * Used by the Soft Shadows feature to define the shadow-ray sampling region.
     *
     * @return the area radius (0 = hard shadows)
     */
    @Override
    public double getSize() {
        return _size;
    }
}