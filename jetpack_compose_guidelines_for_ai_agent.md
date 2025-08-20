# Jetpack Compose Guidelines for AI Agent

---


## Table of Contents

## Updated Table of Contents

1. Jetpack Compose Animation APIs & Patterns
2. Shared Element Transitions (with code)
3. Optimistic UI Patterns in Compose
4. Glassmorphism Effects in Compose
5. Pull-to-Refresh in Compose
6. Advanced Compose Layout Components
7. Advanced Floating Action Button (FAB) Animations
8. Advanced Scaffold Techniques
9. Animated WebP in Compose (Coil)
10. CompositionLocal: Implicit State & Configuration
11. Kotlin map vs flatMap
12. Gradle & Build System Notes


---

## 1. Jetpack Compose Animation APIs & Patterns

- **State-based:** `animate*AsState` for color, size, etc.
- **Transition:** `AnimatedVisibility`, `AnimatedContent` for state transitions.
- **Explicit:** Animate size, rotation, etc. with `animateDpAsState`, `animateFloatAsState`.
- **Gesture-based:** Use `Modifier.draggable`, `Modifier.swipeable` for gesture-driven animations.
- **Custom:** Use `Animatable` for custom value animations.
- **updateTransition:** Animate multiple values in sync.
- **rememberInfiniteTransition:** Looping/pulsating effects.
- **animateContentSize:** Animate size changes on content updates.

**Best Practices:**

- Use high-level APIs for simple cases, lower-level for advanced control.
- Use `Modifier.graphicsLayer` for performant alpha/scale/rotation.
- Test on various devices for smoothness.

---


## 2. Shared Element Transitions (with code)

- Use `SharedTransitionLayout` as the root for transitions.
- Mark elements with `Modifier.sharedElement` or `Modifier.sharedBounds`.
- Use `rememberSharedContentState(key)` for unique element keys.
- Customize with `boundsTransform` for advanced motion (e.g., arcs, keyframes).
- Use `skipToLookaheadSize()` to prevent text reflow.
- Works with Navigation Compose and AnimatedVisibility.

**Example:**


```kotlin
SharedTransitionLayout {
	val sharedState = rememberSharedContentState(key = item.id)
	Box(
		Modifier
			.sharedElement(sharedState, boundsTransform = SharedBoundsTransform())
			.size(100.dp)
	) {
		// ...content
	}
}
```


**Navigation Integration:**

- Use shared element transitions between navigation destinations by providing the same key and state.
- Place `SharedTransitionLayout` at the navigation root.


**Troubleshooting:**

- Ensure unique keys for each shared element.
- Place size/padding modifiers before sharedElement/sharedBounds.
- Test on all target devices for performance.
- Use `skipToLookaheadSize()` to avoid text reflow issues.

---

## 3. Optimistic UI Patterns in Compose

Optimistic UI updates immediately reflect user actions in the UI before confirmation from the backend, improving perceived responsiveness.

**Pattern:**

1. Update local state as if the operation succeeded.
2. Launch the backend/network operation.
3. If the operation fails, rollback the state and show an error.

**Example:**

```kotlin
var items by remember { mutableStateOf(listOf<Item>()) }
var error by remember { mutableStateOf<String?>(null) }

fun onAddItem(newItem: Item) {
	val oldItems = items
	items = items + newItem // Optimistic update
	scope.launch {
		val result = repo.addItem(newItem)
		if (!result.isSuccess) {
			items = oldItems // Rollback
			error = "Failed to add item"
		}
	}
}
```


**Best Practices:**

- Always keep a copy of the previous state for rollback.
- Provide user feedback (e.g., Snackbar) on failure.
- Use `LaunchedEffect` or `rememberCoroutineScope` for async work.
- For lists, use stable keys to avoid recomposition issues.


**Advanced:**

- For complex UIs, use a ViewModel and sealed UI state classes.
- Consider using a state machine for multi-step optimistic flows.

---

---

## 3. Glassmorphism Effects in Compose

- Simulate frosted glass with layered transparency, gradients, and borders.
- Use `Box`/`Card` with `background(Brush.linearGradient(...))` and `border`.
- For blur, use `Modifier.blur()` (Android 12+) or bitmap blur for older versions.
- Layer glass cards over vibrant backgrounds for best effect.
- Use consistent corner radius and subtle borders.

**Performance:**

- Use `remember` for expensive brush calculations.
- Provide fallback for low-end devices.

**Production Examples:**

- Music player overlays, banking cards, weather overlays, profile cards.

---

## 10. CompositionLocal: Implicit State & Configuration

- Use `compositionLocalOf` or `staticCompositionLocalOf` to define shared values.
- Provide values with `CompositionLocalProvider`.
- Access with `.current` in child composables.
- Use for theming, localization, configuration, or dependency injection.

**Advanced:**

- Share transition/animation scopes for navigation flows.
- Use judiciously to avoid hidden dependencies.

---

## 5. Pull-to-Refresh in Compose

- Use `PullToRefreshBox` to enable pull-to-refresh in scrollable content.
- Key params: `isRefreshing`, `onRefresh`, `indicator` (customizable).
- Example: Wrap your `LazyColumn` or scrollable content in `PullToRefreshBox`.
- You can customize the indicator color and style, or create a fully custom indicator using Compose animation APIs.

---

## 6. Advanced Compose Layout Components

- **Foundational:** Box, Column, Row, LazyColumn, LazyRow, Surface, ConstraintLayout, BoxWithConstraints
- **Scaffolds:** Scaffold, TopAppBar, BottomAppBar, ModalDrawer, BottomDrawer, NavigationRail, BottomSheetScaffold
- **Text/Input:** Text, TextField, OutlinedTextField, ClickableText, SelectionContainer
- **Buttons:** Button, OutlinedButton, IconButton, FloatingActionButton, Modifier.clickable
- **Inputs:** Checkbox, RadioButton, Switch, Slider
- **Progress:** CircularProgressIndicator, LinearProgressIndicator
- **Cards/Containers:** Card, Surface, BackdropScaffold
- **Lists/Grids:** LazyVerticalGrid, StaggeredGrid, FlowRow, FlowColumn
- **Visual:** Image, Icon, Divider, Shape Modifier, Spacer, Placeholder API, BadgeBox
- **Gestures:** PointerInput, Transformable, Swipeable, pointerInteropFilter
- **Paging/Scrolling:** HorizontalPager, VerticalPager, PagerTabIndicator, Snapper

---

## 7. Advanced Floating Action Button (FAB) Animations

- **Expanding FAB:** Use `AnimatedVisibility` and `AnimatedContent` to reveal secondary actions
- **Morphing FAB:** Use `Modifier.graphicsLayer` to animate scale, rotation, and translation
- **Extended FAB Collapse:** Monitor scroll state and animate `extended` parameter
- **Gesture-based animations:** Use `Modifier.draggable` or `Modifier.swipeable`
- **Sequential animations:** Chain animations using coroutines for complex effects

**Best Practices:**

- Use `AnimationSpec` to customize duration, easing curves
- Use `Animatable` for fine-grained control
- Follow Material Design guidelines for FAB behavior

---

## 8. Advanced Scaffold Techniques

- **Window Insets:** Use `contentWindowInsets` for system bars handling
- **Responsive Layouts:** Use `BoxWithConstraints` and window size classes
- **State Management:** Use `remember` and `MutableState` for drawer/scaffold state
- **Nested Navigation:** Implement multiple navigation stacks within Scaffold
- **Custom Back Handlers:** Override system back button with `BackHandler`

**Performance Tips:**

- Avoid excessive nesting of Scaffold components
- Optimize composables to minimize recompositions

---

## 9. Animated WebP in Compose (Coil)

- Use Coil's `AsyncImage` or `rememberAsyncImagePainter` to load animated WebP.
- For local drawables, use `ImageRequest.Builder(context).data(drawableResId)`.
- Control playback with `repeatCount` in `ImageRequest`.
- Use `SubcomposeAsyncImage` for custom loading/error states.
- For custom frame effects, implement `AnimatedTransformation`.

**Performance:**

- Animated WebP is more efficient than GIF.
- Optimize frame durations and compression.

---

## 11. Kotlin map vs flatMap

- `map`: Transforms each element, returns nested structure if mapping to collections.
- `flatMap`: Transforms and flattens nested collections into a single list.
- Use `map` for simple transformations, `flatMap` for flattening collections of collections.

---

## 12. Gradle & Build System Notes

- Gradle 9+ uses configuration cache for faster builds.
- Requires JVM 17+.
- Kotlin 2.x and Groovy 4.x supported.
- Use `./gradlew wrapper --gradle-version=9.0.0` to upgrade.
- Archive tasks now produce reproducible outputs by default.
- See Gradle and Kotlin release notes for migration details.

---


