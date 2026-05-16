package lighting;

import primitives.Color;
import primitives.Point;
import primitives.Vector;

/**
 * Interface for all light sources in the scene.
 * Provides methods to calculate intensity and direction at a specific point.
 */
public interface LightSource {

    /**
     * Calculates the intensity of the light at a given point.
     * Takes into account attenuation (distance).
     *
     * @param p The point in the scene
     * @return The color intensity at point p
     */
    public Color getIntensity(Point p);

    /**
     * Calculates the direction vector from the light source to a point.
     * The vector must be normalized.
     *
     * @param p The point in the scene
     * @return Normalized direction vector L
     */
    public Vector getL(Point p);

    /**
     * Calculates the distance between the light source and a given point.
     * Used for shadow and attenuation calculations.
     *
     * @param point The point to measure distance to
     * @return Distance from light source to the point
     */
    double getDistance(Point point);
}