# Animation Utilities

This folder contains small, reusable animation helpers:

- `SequentialAnimation.kt`: Minimal runner for chaining suspend steps.
- `AnimationSequencer.kt`: A builder/timeline to compose steps with delays.
- `GestureAnimation.kt`: Gesture modifiers to trigger animations.
- `PhysicsAnimations.kt`: Predefined spring specs.

Extra:
- `ConfettiEmitter.kt`: Lightweight confetti overlay Composable. Trigger with a changing key, e.g., a timestamp from `AnimationViewModel.celebrationEvent`.

Usage tip:
- Inject `AnimationViewModel` and call `runGroupCreateSequence()` on success. Collect `celebrationEvent` and render `ConfettiEmitter(triggerKey = celebrationEvent)` at the root of the screen tree to show celebratory particles.

See `AnimationShowcaseScreen` for usage examples.
