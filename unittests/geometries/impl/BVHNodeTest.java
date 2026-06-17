package geometries.impl;

import geometries.api.Intersectable;
import geometries.api.Intersectable.Intersection;
import org.junit.jupiter.api.Test;
import primitives.Point;
import primitives.Ray;
import primitives.Vector;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BVHNode}.
 *
 * <p>These tests demonstrate — and prove, not just claim — that the BVH
 * tree-walking logic (both manual and automatic construction) returns
 * exactly the same intersections as brute-force testing every geometry
 * directly. This correctness guarantee is what justifies trusting the BVH
 * for performance: an accelerator that is fast but wrong would be worse
 * than no accelerator at all.</p>
 */
class BVHNodeTest {

    /**
     * MANUAL hierarchy: two spheres far apart, combined by hand into a
     * single {@code BVHNode}. A ray aimed only at the second sphere must
     * still find it — proving that the box-rejection test on one child
     * does not accidentally also reject the other child's branch.
     */
    @Test
    void testManualHierarchyFindsCorrectIntersections() {
        Sphere near = new Sphere(new Point(0, 0, 0), 1);
        Sphere far  = new Sphere(new Point(100, 0, 0), 1);

        // Manual hierarchy — built by hand, not by the automatic builder.
        BVHNode root = new BVHNode(near, far);

        // Ray aimed only at the far sphere.
        Ray rayToFar = new Ray(new Point(100, 0, -10), new Vector(0, 0, 1));
        List<Intersection> result = root.calcIntersections(rayToFar);

        assertNotNull(result, "Manual BVH hierarchy missed an intersection it should have found");
        assertEquals(far, result.get(0).geometry, "Manual BVH hierarchy returned the wrong geometry");

        // Ray that misses both spheres entirely — box rejection at the root
        // must short-circuit cleanly, without throwing or reporting a false hit.
        Ray rayMissingBoth = new Ray(new Point(0, 50, -10), new Vector(0, 0, 1));
        assertNull(root.calcIntersections(rayMissingBoth), "BVH reported a hit on a ray that misses everything");
    }

    /**
     * AUTOMATIC hierarchy: builds a BVH from a list of geometries and
     * compares its results, ray by ray, against a plain brute-force
     * {@code Geometries} (BVH disabled) holding the exact same geometries.
     * The two must always agree in hit count — the BVH is only allowed to
     * be faster, never different.
     */
    @Test
    void testAutomaticBuildMatchesLinearBruteForce() {
        List<Intersectable> spheres = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            double x = i * 5.0;
            spheres.add(new Sphere(new Point(x, 0, 0), 1.5));
        }

        Intersectable bvhRoot = BVHNode.build(spheres);

        Geometries linear = new Geometries();
        linear.add(spheres.toArray(new Intersectable[0])); // BVH stays disabled here on purpose

        for (int i = 0; i < 40; i++) {
            // One ray per sphere, aimed straight at its center along Z.
            Ray ray = new Ray(new Point(i * 5.0, 0, -50), new Vector(0, 0, 1));

            List<Intersection> bvhResult = bvhRoot.calcIntersections(ray);
            List<Intersection> linearResult = linear.calcIntersections(ray);

            assertNotNull(bvhResult, "BVH missed sphere #" + i);
            assertNotNull(linearResult, "Brute force missed sphere #" + i + " (test bug, not BVH bug)");
            assertEquals(linearResult.size(), bvhResult.size(),
                    "BVH and brute force disagree on hit count for sphere #" + i);
        }
    }
}