<img width="200" height="200" alt="Başlıksız" src="https://github.com/user-attachments/assets/8e0a2206-7e74-49ad-9081-442764ca9734" />

# Windomium Optimisation Engine

**Windomium** is an open-source, modular optimisation engine and performance framework built for **Minecraft 1.21.1 (NeoForge)** on **Java 21**. Designed with a strict *"measure first, optimise later"* philosophy, Windomium targets both average framerate and **1% / 0.1% low frame time stability** whilst preserving complete visual parity and modpack compatibility.

## Core Architecture & Features

* **Dual Culling Pipeline (`FRUSTUM`, `OCCLUSION`, `COMBINED`):** Real-time switchable section and entity culling with zero-allocation ray-marched occlusion and instant underground cave culling.
* **Asynchronous Chunk Mesher:** Priority-based background thread pool for chunk compilation.
* **Entity & Block Entity Optimiser:** Distance-based shadow, equipment layer, and nametag skipping alongside a nanosecond-budgeted Block Entity rate limiter.
* **Fastutil Memory & Biome Colour Cache:** Thread-local `Long2IntLinkedOpenHashMap` biome tint caching and `MutableBlockPos` object pooling to drastically reduce Garbage Collector (GC) allocation pressure.
* **Adaptive Runtime Governor:** Continuously monitors frame time variance and 1% lows, dynamically scaling culling thresholds with hysteresis.
* **Modpack & Shader Companion Mode:** Automatic detection and safe-bypass profiles for **Sodium, Embeddium, Iris, Oculus, Create, Mekanism, AE2, Immersive Engineering, Botania, and GeckoLib**.

## Controls & In-Game Commands

* **Press `O`:** Open the in-game Windomium Settings GUI.
* **Press `F8`:** Toggle the real-time Profiler Overlay (FPS, 1% Low, 0.1% Low, RAM Allocation Rate, Culling counters).
* `/windomium config` — Opens the configuration screen.
* `/windomium culling <frustum|occlusion|combined>` — Switches the active culling algorithm on the fly.
* `/windomium profile [seconds]` — Runs a benchmark session and outputs average FPS, 1% low, 0.1% low, and frame variance.
* `/windomium diag` — Outputs system and mod compatibility diagnostics.
* `/windomium jvm` — Analyses active JVM Garbage Collector flags and provides tuning advice.
* `/windomium memory` — Displays heap allocation rate, biome cache hit ratio, and trims thread-local pools.

## Developer API (`org.windomium.api`)

Third-party mod developers can register custom `IOptimisationModule` implementations or declare explicit rendering behaviours (`DO_NOT_TOUCH`, `THROTTLEABLE`, `CULLABLE`) for custom entities and block entities via `WindomiumAPI`.

## Licence

Licensed under the **GNU Lesser General Public License v3.0 (LGPL-3.0)**.
