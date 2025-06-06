package com.dawitf.akahidegn.util

import androidx.compose.ui.semantics.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp

/**
 * Accessibility Utilities
 * Provides helper functions for improving app accessibility
 */

/**
 * Data class for centralized accessibility settings
 */
data class AccessibilitySettings(
    val enableHighContrast: Boolean = false,
    val enableFontScaling: Boolean = true,
    val fontScaleFactor: Float = 1.0f,
    val enableScreenReader: Boolean = true,
    val minTouchTargetDp: Int = 48
)

/**
 * Extensions for improving accessibility
 */
object AccessibilityUtils {
    /**
     * Applies semantic properties to make component accessible to screen readers
     */
    fun Modifier.accessibilityLabel(
        label: String,
        contentDescription: String? = null,
        stateDescription: String? = null,
        role: Role? = null,
        isClickable: Boolean = false
    ): Modifier {
        var modifier = this.semantics {
            if (contentDescription != null) {
                this.contentDescription = contentDescription
            } else {
                this.contentDescription = label
            }
            
            if (stateDescription != null) {
                this.stateDescription = stateDescription
            }
            
            if (role != null) {
                this.role = role
            }
            
            if (isClickable) {
                onClick { true }
            }
        }
        
        return modifier
    }
    
    /**
     * Ensures a clickable element has minimum touch target size of 48dp × 48dp
     * as per Material Design and WCAG accessibility guidelines
     */
    fun Modifier.minimumTouchTarget(minSize: Int = 48): Modifier {
        return this.defaultMinSize(minSize.dp, minSize.dp)
    }
    
    /**
     * Creates an accessible clickable element with appropriate semantics
     */
    fun Modifier.accessibleClickable(
        onClick: () -> Unit,
        enabled: Boolean = true,
        role: Role = Role.Button,
        label: String,
        actionLabel: String? = null,
        contentDescription: String? = null,
        stateDescription: String? = null
    ): Modifier {
        return this
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                this.contentDescription = contentDescription ?: label
                if (stateDescription != null) {
                    this.stateDescription = stateDescription
                }
                if (actionLabel != null) {
                    this.customActions = listOf(CustomAccessibilityAction(actionLabel) { onClick(); true })
                }
                this.role = role
            }
            .minimumTouchTarget()
    }
    
    /**
     * Creates a screenreader-friendly semantics description for a ride group
     */
    fun createGroupAccessibilityDescription(
        destination: String,
        availableSeats: Int,
        departureTime: String,
        price: Float
    ): String {
        val seatsDesc = if (availableSeats == 1) "1 seat available" else "$availableSeats seats available" 
        return "Group to $destination. $seatsDesc. Departing at $departureTime. Price: $price birr."
    }
}
