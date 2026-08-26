# Mesh Gradient Playground

An Android app for designing mesh gradients with Jetpack Compose's `MeshGradientPainter`, and for
getting the result back out as an image or as the Kotlin that draws it.

| Playground | Control points | Saved meshes | Full screen |
|---|---|---|---|
| <img src="docs/playground.png" width="200"> | <img src="docs/control-point.png" width="200"> | <img src="docs/saved-meshes.png" width="200"> | <img src="docs/full-screen.png" width="200"> |

## What it does

- **Edit the mesh directly.** Drag vertices on the canvas, or select one — or several — and set
  position, colour and Bézier control points from the panel below. The gradient stays in view while
  you work, so every change is visible as you make it.
- **Up to 10 × 10 patches**, with bilinear or bicubic colour interpolation.
- **Control points with a proper editor.** Each vertex has four tangents (left, top, right,
  bottom). They are *offsets from the vertex*, so their pad is centred on zero and reaches into
  negative values. Switching one on seeds the tangent the renderer would have inferred anyway, so
  the gradient does not jump.
- **Save, name and reopen meshes.** Stored as JSON in the app's files directory, listed with live
  thumbnails rendered from the mesh itself rather than cached images.
- **Full screen** for a clean look at the gradient — no chrome, no vertex markers, system bars
  hidden. Back returns.
- **Share as PNG** — the gradient alone, at canvas resolution, with markers excluded.
- **Share painter code** — the `MeshGradientPainter` call that reproduces the mesh, ready to paste.

## Requirements

`MeshGradientPainter` is an alpha API, so the project is pinned accordingly:

| | |
|---|---|
| Compose BOM | `2026.08.00` (ui-graphics `1.13.0-alpha01`) |
| AGP | `9.5.0-alpha01` |
| Kotlin | `2.2.10` |
| minSdk / targetSdk | 24 / 37 |

On API 34+ the renderer uses the platform `Mesh` API with a fragment shader; below that it
tessellates and draws through hardware-accelerated `drawVertices`. Both paths work — the shader
path gives smoother bicubic colour.

## Building

```
./gradlew installDebug
```

Instrumented tests need a connected device or emulator:

```
./gradlew connectedDebugAndroidTest
```

Everything is tested on device rather than on the JVM, because the two things worth testing —
`org.json` and the renderer's own output — are unavailable or stubbed out in local unit tests. The
suite covers the JSON codec, the store's seeding rules, PNG export and sharing, painter-code
generation, and the UI flows including a pixel check that vertex markers never reach the exported
image.

## Bundled meshes

`DefaultMeshes.kt` holds the meshes copied into a fresh install. It is a plain
`List<MeshPreset>` — add entries there to ship more. They are seeded on first launch and behave
like any saved mesh afterwards, so editing or deleting one sticks, and new defaults reach fresh
installs only.

## A note on coordinates

Vertex positions and control points are normalized: `(0, 0)` is the top left of the drawing bounds
and `(1, 1)` the bottom right. Two things follow that are easy to trip over:

- The mesh paints **only** the area its vertices span. Pull a vertex inward and the space outside
  is left transparent, not stretched to fill.
- Positions outside `0..1` are legal and useful — pushing a corner out to `(-0.4, -0.4)` keeps the
  bounds covered while moving that colour's centre off-canvas. The app's editor clamps to `0..1`,
  but meshes written in `DefaultMeshes.kt` are not clamped.

## Licence

MIT — see [LICENSE](LICENSE).
