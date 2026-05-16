package lighting;

import primitives.Color;
import primitives.Point;
import primitives.Vector;

/**
 * DirectionalLight represents a light source that is very far away.
 */
public class DirectionalLight extends Light implements LightSource {
    private final Vector _direction;

    public DirectionalLight(Color intensity, Vector direction) {
        super(intensity);
        this._direction = direction.normalize();
    }

    @Override
    public Color getIntensity(Point p) {
        return _intensity;
    }

    @Override
    public Vector getL(Point p) {
        return _direction;
    }

    @Override
    public double getDistance(Point point) {
        return Double.POSITIVE_INFINITY;
    }
}