package lighting;

import primitives.Color;
import primitives.Point;
import primitives.Vector;

/**
 * Represents a spot light source — a {@link PointLight} with a focused beam
 * directed toward a specific direction.
 *
 * <p>The intensity at a point is scaled by the cosine of the angle between
 * the light's direction and the vector toward the point:</p>
 * <pre>
 *   I(p) = I_point(p) · max(0, dir · l)^narrowBeam
 * </pre>
 *
 * <p>Like {@link PointLight}, a spot light can be given a physical
 * {@code size > 0} to act as an area light and produce soft shadows.</p>
 */
public class SpotLight extends PointLight {

    /** The normalized direction the spot light points toward. */
    private final Vector _direction;

    /**
     * Beam narrowness exponent.
     * <ul>
     *   <li>1 – standard spotlight (default)</li>
     *   <li>&gt;1 – sharper, more focused beam</li>
     * </ul>
     */
    private int _narrowBeam = 1;

    /**
     * Constructs a spot light at the given position, pointing in the given direction.
     *
     * @param intensity  the color intensity of the light
     * @param position   the position of the light in world space
     * @param direction  the direction the beam points (need not be normalized)
     */
    public SpotLight(Color intensity, Point position, Vector direction) {
        super(intensity, position);
        this._direction = direction.normalize();
    }

    // ========================= Setters (Fluent API) =========================

    /**
     * Sets the narrowness exponent of the beam.
     * Higher values produce a tighter, more focused cone of light.
     *
     * @param narrowBeam beam narrowness exponent (≥ 1)
     * @return this light (for method chaining)
     */
    public SpotLight setNarrowBeam(int narrowBeam) {
        this._narrowBeam = narrowBeam;
        return this;
    }

    // ---- Fluent API overrides (return SpotLight instead of PointLight) ----

    /** {@inheritDoc} */
    @Override
    public SpotLight setKc(double kC) {
        super.setKc(kC);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public SpotLight setKl(double kL) {
        super.setKl(kL);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public SpotLight setKq(double kQ) {
        super.setKq(kQ);
        return this;
    }

    /**
     * Sets the physical radius of this spot light's area.
     * Overridden to preserve the fluent {@code SpotLight} return type.
     *
     * @param size radius of the area light (must be ≥ 0)
     * @return this light (for method chaining)
     */
    @Override
    public SpotLight setSize(double size) {
        super.setSize(size);
        return this;
    }

    // ========================= LightSource Implementation =========================

    /**
     * Returns the attenuated and beam-focused intensity of the light at point {@code p}.
     *
     * <p>If the point lies outside the beam cone ({@code dir · l ≤ 0}),
     * the contribution is {@link Color#BLACK}.</p>
     *
     * @param p the point in the scene
     * @return the color intensity at {@code p}
     */
    @Override
    public Color getIntensity(Point p) {
        double cosTheta = _direction.dotProduct(getL(p));
        if (cosTheta <= 0)
            return Color.BLACK;

        double factor = _narrowBeam == 1 ? cosTheta : Math.pow(cosTheta, _narrowBeam);
        return super.getIntensity(p).scale(factor);
    }
}