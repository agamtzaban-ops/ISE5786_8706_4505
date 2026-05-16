package lighting;

import primitives.Color;
import primitives.Point;
import primitives.Vector;

/**
 * PointLight represents a light source from a single point in space.
 */
public class PointLight extends Light implements LightSource {
    protected final Point _position;
    private double _kC = 1.0;
    private double _kL = 0.0;
    private double _kQ = 0.0;

    public PointLight(Color intensity, Point position) {
        super(intensity);
        this._position = position;
    }

    public PointLight setKc(double kC) {
        this._kC = kC;
        return this;
    }

    public PointLight setKl(double kL) {
        this._kL = kL;
        return this;
    }

    public PointLight setKq(double kQ) {
        this._kQ = kQ;
        return this;
    }

    @Override
    public Color getIntensity(Point p) {
        double d = _position.distance(p);
        double attenuation = _kC + _kL * d + _kQ * d * d;
        // Fix: using scale(1/attenuation) to avoid double-to-int error
        return _intensity.scale(1d / attenuation);
    }

    @Override
    public Vector getL(Point p) {
        return p.subtract(_position).normalize();
    }

    @Override
    public double getDistance(Point point) {
        return _position.distance(point);
    }
}