# Mini-Project 2 — Performance Measurements

## Scene: Desert Sunset

A low-poly desert scene with atmospheric depth, procedural geometry, and multiple light sources.

### Geometry
- **Terrain**: 1,568 triangles (28×28 grid, 6-octave procedural height field)
- **Sky dome**: 280 triangles (10×14 gradient panels)
- **Mountain ridges**: ~80 triangles (2 procedural fractal ridgelines)
- **Light-ray fan**: 9 triangles
- **Saguaro cacti (×5)**: 5 ribbed 16-sided trunks + 20 ribbed 8-sided arm segments (prisms) + 10 tip spheres ≈ 680 triangles + 10 spheres
- **Barrel cacti (×2)**: 2 ribbed 16-sided prisms + 2 dome spheres ≈ 240 triangles + 2 spheres
- **Mesa**: 12 triangles (flat-top plateau)
- **Boulders**: 4 spheres
- **Desert shrubs**: 8 spheres (2 clusters × 4)
- **Sun bloom**: 4 concentric spheres (KT transparent halos + glowFalloff corona)
- **Backdrop plane**: 1 plane
- **Total**: ~2,869 triangles + 29 spheres + 1 plane

### Lights
- 1 DirectionalLight (low-angle sunset sweep, warm orange)
- 1 PointLight (at sun sphere, soft-shadow radius 46)
- 2 SpotLights (cool purple fill + warm ground bounce)

### Render settings (timing runs)
- Resolution: 600 × 375
- Anti-aliasing samples: 1 (JITTERED pattern)
- Soft-shadow samples: 1
- Effects active: shadows, soft shadows, transparency (sun KT halos), glowFalloff

---

## Results

| Configuration              | BVH | Multithreading | Render time  | Speedup vs baseline |
|----------------------------|-----|----------------|--------------|---------------------|
| 1. Baseline                | OFF | OFF            | 395,769 ms   | ×1.00               |
| 2. Multithreading only     | OFF | ON             | 168,528 ms   | ×2.35               |
| 3. BVH only                | ON  | OFF            |   5,973 ms   | ×66.26              |
| 4. BVH + Multithreading    | ON  | ON             |   1,974 ms   | ×200.49             |

---

## Analysis

**BVH dominates:** BVH alone yields a **×66 speedup** — the O(N) brute-force scan over ~2,900 primitives is replaced by O(log N) tree traversal. This is by far the largest single improvement.

**Multithreading is additive:** Threading alone gives ×2.35 on 8 cores / 12 threads (Intel i5-12450H). The sub-linear scaling is expected due to memory-bandwidth contention and pixel-task granularity.

**Combined BVH + MT = ×200:** The two optimizations compose nearly multiplicatively (×66 × ×2.35 ≈ ×155 expected; actual ×200 suggests BVH also improves cache locality, amplifying the MT benefit).

**Conclusion:** BVH is the critical optimization for complex scenes. Without it, a 600×375 render takes ~6.6 minutes; with BVH + multithreading it takes under 2 seconds — a **200× improvement**.

---

## Machine

- CPU: 12th Gen Intel Core i5-12450H
- Physical cores: 8 · Logical threads: 12
- OS: Windows 11 Home
