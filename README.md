# Ray Tracer — 3D Rendering Engine



---

## Authors

| Name |
|------|
| Osnat Zimmerman |
| Agam Tzaban |

---

## Table of Contents

1. [Project Description](#project-description)
2. [Architecture](#architecture)
3. [Packages and Classes](#packages-and-classes)
4. [Visual Effects](#visual-effects)
5. [Mini-Project 1 — Multi-Sampling](#mini-project-1--multi-sampling)
6. [Mini-Project 2 — Performance Acceleration](#mini-project-2--performance-acceleration)
7. [Running the Project](#running-the-project)
8. [Performance Measurements](#performance-measurements)

---

## Project Description

A complete **Ray Tracing** engine built in Java, implementing the **Phong Reflection Model** with full support for:

- Reflection and Transparency (Refraction)
- Soft Shadows and Hard Shadows
- Anti-Aliasing (multi-sampling per pixel)
- Smooth Shading via `SmoothTriangle` with per-vertex normals
- Texture Mapping on spheres and triangles
- BVH (Bounding Volume Hierarchy) acceleration structure
- Multi-threaded rendering via raw threads and parallel streams

---

## Architecture

The project follows **RDD** (Responsibility-Driven Design) principles, standard design patterns, and **TDD**:

```
+---------------------------------------------------------+
|                        Camera                           |
|  (Builder Pattern, Multi-threading, VP calculations)    |
+------------------------+--------------------------------+
                         | castRay()
                         v
+---------------------------------------------------------+
|              RayTracerBase (abstract)                   |
|                        |                                |
|               SimpleRayTracer                           |
|    (Phong model, global effects, soft shadows)          |
+------------------------+--------------------------------+
                         | calcIntersections()
                         v
+---------------------------------------------------------+
|                       Scene                             |
|  +----------------+    +-----------------------------+  |
|  |   Geometries   |    |      LightSource list       |  |
|  | (Composite +   |    |  DirectionalLight           |  |
|  |  BVH tree)     |    |  PointLight / SpotLight     |  |
|  +----------------+    +-----------------------------+  |
+---------------------------------------------------------+
```

### Design Patterns Used

| Pattern | Location | Role |
|---------|----------|------|
| **Builder** | `Camera.Builder` | Constructs camera with full validation |
| **Composite** | `Geometries` | Manages a collection of geometries uniformly |
| **NVI** (Non-Virtual Interface) | `Intersectable` | Enforces `calcIntersections` contract |
| **Strategy** | `RayTracerType` | Selects the rendering algorithm at runtime |
| **Chain of Responsibility** | Recursive color calculation | Handles reflection and transparency |

---

## Packages and Classes

### `primitives` — Mathematical Foundation

| Class | Description |
|-------|-------------|
| `Point` | A point in 3D space |
| `Vector` | A vector with add, scale, dot, cross, normalize |
| `Ray` | A ray: origin point + normalized direction |
| `Color` | RGB color supporting add, scale, reduce |
| `Double3` | Triplet of doubles used for material coefficients |
| `Material` | Material properties: kD, kS, kT, kR, shininess, texture |
| `Texture` | Loads a JPEG/PNG image and samples it by UV coordinates |

### `geometries.api` — Interfaces and Base Classes

| Class | Description |
|-------|-------------|
| `Intersectable` | Abstract base for all hittable objects; defines `calcIntersections()` and `getBoundingBox()` |
| `Geometry` | Adds emission color, material, and `isLightSource` flag |
| `AABB` | Axis-Aligned Bounding Box — the core building block of the BVH |

### `geometries.impl` — Geometric Shapes

| Class | Description |
|-------|-------------|
| `Sphere` | Sphere — exact (tight) AABB |
| `Plane` | Infinite plane — `getBoundingBox()` returns `null` (unbounded) |
| `Triangle` | Triangle — exact AABB from vertex min/max |
| `Polygon` | General convex polygon |
| `Cylinder` | Finite cylinder — conservative AABB from both end-caps |
| `Tube` | Infinite tube — `getBoundingBox()` returns `null` (unbounded) |
| `SmoothTriangle` | Triangle with per-vertex normals for smooth Phong shading |
| `Geometries` | Composite — collection of Intersectables; supports BVH |
| `BVHNode` | A single BVH tree node (manual and automatic Median Split construction) |
| `MeshLoader` | Parses a Wavefront OBJ file and converts faces into Triangle objects |

### `lighting` — Light Sources

| Class | Description |
|-------|-------------|
| `AmbientLight` | Uniform ambient light: IA x KA |
| `DirectionalLight` | Directional light — no attenuation, no soft shadows |
| `PointLight` | Point light — kC/kL/kQ attenuation, supports soft shadows via `setSize(r)` |
| `SpotLight` | Spot light — cos^narrowBeam falloff, supports soft shadows |

### `renderer` — Rendering Engine

| Class | Description |
|-------|-------------|
| `Camera` | Casts rays, manages threads, writes pixels to image |
| `SimpleRayTracer` | Phong local effects + global effects + soft shadows |
| `Blackboard` | Sampling infrastructure — supports GRID and JITTERED patterns |
| `ImageWriter` | Writes the pixel buffer to a PNG file |
| `PixelManager` | Thread-safe pixel allocator for multi-threaded rendering |
| `RayTracerType` | Enum: `SIMPLE`, `GRID` |

### `scene`

| Class | Description |
|-------|-------------|
| `Scene` | Holds Geometries, lights, background color, and ambient light |
| `SceneLoader` | Parses a scene from an XML file |

---

## Visual Effects

### Reflection

A reflected ray is cast from every intersection point.
Strength is controlled by `kR` (Double3 coefficient).

```java
material.setKR(0.8)   // near-perfect mirror
material.setKR(0.3)   // subtle reflection
```

### Transparency (Refraction)

A refraction ray is transmitted through the geometry.
Strength is controlled by `kT`.

```java
material.setKT(0.9)   // nearly transparent (glass)
material.setKT(0.5)   // semi-transparent
```

### Soft Shadows

Multiple shadow rays are cast toward different sample points on the light area disk.
Enabled by `setSize(r)` on PointLight/SpotLight, and `setSoftShadowSamples(n)` on SimpleRayTracer.

```java
new PointLight(color, pos).setSize(20)    // area light with radius 20
tracer.setSoftShadowSamples(9)            // 9x9 = 81 shadow rays per light
```

### Anti-Aliasing

Multiple rays are cast through different sub-pixel positions and their colors are averaged.

```java
camera.setAntiAliasingSamples(9)   // 9x9 = 81 rays per pixel
```

### Glow Falloff (Limb Darkening)

A natural brightness fade toward the silhouette edges of a self-luminous body.
Computed as |N dot V|^exponent.

```java
material.setGlowFalloff(0.35)   // moderate fade at silhouette edges
```

### Texture Mapping

A 2D image is mapped onto a geometry via UV coordinates.

```java
material.setTexture(new Texture("images/textures/earth.jpg"))
```

---

## Mini-Project 1 — Multi-Sampling

**Assigned feature:** Anti-Aliasing + Soft Shadows
**Test class:** `renderer/MiniProject1Tests.java`

### Scene: Solar System

A Solar System poster layout with real texture maps on every planet.
Planets are arranged in a mathematical arc around the Sun.

**Geometries in the scene:**
- Skybox — large Sphere with a galaxy texture (emission, self-lit)
- Sun — Sphere with sun texture (emission texture, acts as light source)
- 8 planets: Mercury, Venus, Earth (+ Moon), Mars, Jupiter, Saturn (+ 4 ring Cylinders), Uranus, Neptune
- Saturn rings — 4 transparent Cylinders at different radii

### Test Methods

| Test | Description |
|------|-------------|
| `testTempleNoAntiAliasing` | Baseline — 1 ray per pixel |
| `testTempleWithAntiAliasingDemo` | Anti-Aliasing 9x9 |
| `testTempleWithAntiAliasingFinal` | Anti-Aliasing 17x17 — high quality |
| `testTempleNoSoftShadows` | Hard shadows only |
| `testTempleWithSoftShadowsDemo` | Soft Shadows 9x9 |
| `testTempleJitteredVsGrid` | Compares JITTERED vs GRID sampling patterns |
| `testTempleCombinedFinal` | Full quality: AA + Soft Shadows combined |
![space_combined_ON.png](images/space_combined_ON.png)

---

## Mini-Project 2 — Performance Acceleration

**Assigned feature:** BVH (Bounding Volume Hierarchy)
**Test class:** `renderer/MiniProject2Tests.java`

### How BVH Works

**Without BVH:** Every ray is tested against every geometry — O(n) per ray.
**With BVH:** Each ray is first tested against a bounding box (AABB). If it misses the box, the entire subtree is skipped — O(log n) on average.

#### Three required BVH components:

**1. AABB / CBR — Axis-Aligned Bounding Box**

Every finite geometry computes its own bounding box via `getBoundingBox()`:
- `Sphere` — exact tight box: center +/- radius on each axis
- `Triangle` / `Polygon` — exact box from vertex min/max
- `Cylinder` — conservative box from the two end-cap spheres
- `Plane` / `Tube` — returns `null` (infinite, cannot be bounded)

**2. Manual Hierarchy — BVHNode(left, right)**

A `BVHNode` can be constructed by hand from two explicit children.
Used in `BVHNodeTest` to prove that the tree-walking logic is correct
on small, controlled examples before trusting the automatic builder.

```java
// Manual hierarchy — explicit shape of the tree
BVHNode root = new BVHNode(sphereA, sphereB);
```

**3. Automatic Hierarchy — Median Split**

`BVHNode.build(list)` automatically organizes a flat list of geometries
into a balanced tree using the Median Split algorithm:
1. Compute the union bounding box of all items.
2. Find the longest axis (X, Y, or Z).
3. Sort items by their bounding-box centroid along that axis.
4. Split the sorted list in half and recurse on each half.
5. Stop and create a leaf when the list is small enough (maxLeafSize).

#### Enabling / disabling BVH from the tests:

```java
scene.geometries.add(sphere1, sphere2, triangle1, plane1, ...);

// BVH OFF (default) — brute-force linear scan
camera.renderImage();

// BVH ON — one call, nothing else changes
scene.geometries.buildBVH();
camera.renderImage();
```

Nothing in `SimpleRayTracer`, `Camera`, or `Scene` changes.
The acceleration is entirely encapsulated inside `Geometries`.

### Multi-Threading

Two implementations selectable via `setMultithreading(n)`:

| Value | Method |
|-------|--------|
| `0` | No multi-threading (default) |
| `-1` | Parallel Streams |
| `-2` | Raw Threads — auto-detects available CPU cores |
| `n > 0` | Exactly n raw threads |

### Scene: Desert Sunset

A low-poly canyon landscape at golden hour.

**Geometries (total: ~2,869 triangles + 29 spheres + 1 plane):**
- Backdrop — infinite Plane (always tested directly, never placed in the BVH)
- Sun — 4 nested transparent Spheres with glowFalloff (KT halos + limb darkening)
- Sky — 280 Triangles with a 3-stop sunset gradient
- Sun rays — 9 fan Triangles
- Terrain — 1,568 Triangles (6-octave sinusoidal height map)
- Fractal mountain — approximately 2,048 SmoothTriangles (Diamond-Square 33x33 heightmap)
- 5 saguaro cacti — ribbed prism trunk + 2 Bezier-curve arms + procedural thorns
- 2 barrel cacti — ribbed UV-spheroid + thorns
- 2 desert shrubs — Sphere clusters

**Light sources (5, covering all supported types):**
1. `AmbientLight` — warm ambient fill
2. `DirectionalLight` — sunset sun at approximately 7 degrees above the horizon
3. `PointLight` — the sun sphere with soft-shadow radius 46
4. `SpotLight` — cool purple fill from sky zenith
5. `SpotLight` — warm bounce light from the sunlit ground

### Mandatory Measurement Tests

| Test Method | BVH | Threads | Output File |
|-------------|-----|---------|-------------|
| `measurement_NoAcceleration_NoMultithreading` | OFF | OFF | mp2_accelOFF_mtOFF.png |
| `measurement_NoAcceleration_WithMultithreading` | OFF | ON (auto) | mp2_accelOFF_mtON.png |
| `measurement_WithBVH_NoMultithreading` | ON | OFF | mp2_accelON_mtOFF.png |
| `measurement_WithBVH_WithMultithreading` | ON | ON (auto) | mp2_accelON_mtON.png |

### Comparison Tests (AA / SS — same structure as Mini-Project 1)

| Test Method | Description |
|-------------|-------------|
| `testDesert_NoAntiAliasing` | Baseline — 1 ray per pixel |
| `testDesert_WithAntiAliasing` | Anti-Aliasing 9x9 — smooth edges |
| `testDesert_NoSoftShadows` | Hard shadows only |
| `testDesert_WithSoftShadows` | Soft Shadows 9x9 |

### BVHNodeTest — Correctness Verification

Located at: `unittests/geometries/impl/BVHNodeTest.java`

| Test | What it proves |
|------|----------------|
| `testManualHierarchyFindsCorrectIntersections` | A manually built BVH tree finds the correct geometry and cleanly rejects misses |
| `testAutomaticBuildMatchesLinearBruteForce` | The automatic builder returns exactly the same intersections as brute-force linear scan across 40 spheres |

### The final image
<img width="750" height="469" alt="mp2_final_presentation" src="https://github.com/user-attachments/assets/e91ed5ac-5202-453f-b982-4455b39e7ff6" />

---

## Running the Project

### Requirements
- Java 21 or higher
- Maven or Gradle (as configured in the project)
- JUnit 5

### Folder Structure

```
project-root/
+-- src/
|   +-- geometries/
|   |   +-- api/         <- Intersectable, Geometry, AABB
|   |   +-- impl/        <- Sphere, Triangle, BVHNode, MeshLoader, ...
|   +-- lighting/        <- AmbientLight, PointLight, SpotLight, ...
|   +-- primitives/      <- Point, Vector, Ray, Color, Material, Texture
|   +-- renderer/        <- Camera, SimpleRayTracer, Blackboard, ...
|   +-- scene/           <- Scene, SceneLoader
+-- unittests/
|   +-- geometries/impl/ <- BVHNodeTest, SphereTests, ...
|   +-- primitives/      <- PointTests, VectorTests, ...
|   +-- renderer/        <- MiniProject1Tests, MiniProject2Tests, ...
+-- images/
|   +-- textures/        <- Texture files (earth.jpg, mars.jpg, ...)
|   +-- *.png            <- Rendered output images
+-- models/
|   +-- *.obj            <- 3D model files
+-- MEASUREMENTS.md      <- Performance measurement report
+-- README.md            <- This file
```

### Running a Single Test in IntelliJ

Click the green triangle next to any test method name and select Run.

### Running All Tests

Go to Run > Run All Tests, or press Ctrl + Shift + F10.

### Output Images

All rendered images are saved automatically to the `images/` folder as PNG files.

---

## Performance Measurements

### Test Machine

| Property | Value |
|----------|-------|
| CPU | 12th Gen Intel Core i5-12450H |
| Physical cores | 8 |
| Logical threads | 12 |
| OS | Windows 11 Home |

### Scene: Desert Sunset — Render Settings

- Resolution: 600 x 375
- Anti-aliasing samples: 1 (disabled for timing runs)
- Soft-shadow samples: 1 (disabled for timing runs)
- Effects active: shadows, transparency (sun KT halos), glowFalloff

### Results

| Configuration | BVH | Multithreading | Render Time | Speedup vs Baseline |
|---------------|-----|----------------|-------------|---------------------|
| 1. Baseline | OFF | OFF | 395,769 ms (~6.6 min) | x1.00 |
| 2. Multithreading only | OFF | ON (12 threads) | 168,528 ms (~2.8 min) | x2.35 |
| 3. BVH only | ON | OFF | 5,973 ms (~6 sec) | x66.26 |
| 4. BVH + Multithreading | ON | ON (12 threads) | 1,974 ms (~2 sec) | x200.49 |

### Analysis

**BVH dominates:** BVH alone yields a x66 speedup.
The O(N) brute-force scan over ~2,900 primitives is replaced by O(log N) tree traversal,
skipping entire subtrees that the ray cannot possibly hit.

**Multithreading is additive:** Threading alone gives x2.35 on 12 logical threads.
Sub-linear scaling is expected due to memory-bandwidth contention and task-granularity overhead.

**Combined BVH + Multithreading = x200:** The two optimizations compose nearly multiplicatively.
Expected: x66 x x2.35 = ~x155. Actual: x200 — the extra gain suggests BVH also improves
cache locality, which amplifies the multithreading benefit.

**Conclusion:** Without BVH a 600x375 render takes 6.6 minutes.
With BVH + multithreading it takes under 2 seconds — a 200x improvement.

---

## Software Engineering Principles

| Principle | How it is applied |
|-----------|------------------|
| **DRY** | All sampling logic lives in `Blackboard` only — shared by AA and soft shadows |
| **RDD** | Each class holds exactly the data it is responsible for |
| **Open/Closed** | Adding a new geometry type requires only a new class, no changes to existing code |
| **NVI** | `calcIntersections` is `final`; shape-specific logic goes in `calcIntersectionsHelper` |
| **Null convention** | `getBoundingBox() == null` means unbounded — correct for `Plane` and `Tube` |

---

