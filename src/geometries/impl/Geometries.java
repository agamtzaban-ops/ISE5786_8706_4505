package geometries.impl;

import geometries.api.Intersectable;
import primitives.Point;
import primitives.Ray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Composite class representing a collection of intersectable geometries.
 * This class uses the Composite Design Pattern.
 */
public class Geometries extends Intersectable { // התיקון כאן: extends במקום implements

    /** List to store all geometries in the collection */
    private final List<Intersectable> _geometries = new ArrayList<>();

    /** * Default constructor for an empty collection
     */
    public Geometries() {}

    /**
     * Constructor receiving a variable number of geometries.
     * @param geometries objects to add to the collection
     */
    public Geometries(Intersectable... geometries) {
        add(geometries);
    }

    /**
     * Adds geometries to the collection.
     * Uses Collections.addAll for efficiency (DRY principle).
     * @param geometries objects to add
     */
    public void add(Intersectable... geometries) {
        if (geometries != null) {
            Collections.addAll(_geometries, geometries);
        }
    }

    /**
     * Finds intersections between the ray and all geometries in the collection.
     * Performance optimization: Lazy initialization of the result list.
     * @param ray the ray to intersect with
     * @return list of intersection points or null if none found
     */
    @Override
    public List<Point> findIntersections(Ray ray) {
        List<Point> result = null;

        // Using for-each loop to iterate through all geometries
        for (Intersectable geo : _geometries) {
            List<Point> geoIntersections = geo.findIntersections(ray);

            if (geoIntersections != null) {
                // Lazy initialization: create the list only when the first intersection is found
                if (result == null) {
                    result = new ArrayList<>();
                }
                result.addAll(geoIntersections);
            }
        }

        return result;
    }
}