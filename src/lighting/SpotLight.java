package lighting;

import primitives.Color;
import primitives.Point;
import primitives.Vector;

/**
 * SpotLight is a type of PointLight with a specific direction.
 */
public class SpotLight extends PointLight {
    private final Vector _direction;
    private int _narrowBeam = 1; // Default is regular spotlight

    public SpotLight(Color intensity, Point position, Vector direction) {
        super(intensity, position);
        this._direction = direction.normalize();
    }

    /**
     * Sets the narrowness of the beam (for bonus/advanced effects).
     */
    public SpotLight setNarrowBeam(int narrowBeam) {
        this._narrowBeam = narrowBeam;
        return this;
    }

    @Override
    public Color getIntensity(Point p) {
        double cosTheta = _direction.dotProduct(getL(p));
        if (cosTheta <= 0) return Color.BLACK;

        // If narrowBeam > 1, it will sharpen the beam (bonus)
        double factor = _narrowBeam == 1 ? cosTheta : Math.pow(cosTheta, _narrowBeam);
        return super.getIntensity(p).scale(factor);
    }

    // --- Overrides for Fluent API ---
    @Override
    public SpotLight setKc(double kC) { super.setKc(kC); return this; }
    @Override
    public SpotLight setKl(double kL) { super.setKl(kL); return this; }
    @Override
    public SpotLight setKq(double kQ) { super.setKq(kQ); return this; }
}