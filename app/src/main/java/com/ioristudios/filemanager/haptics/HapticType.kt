package com.ioristudios.filemanager.haptics

/**
 * Semantic tactile profiles mapping to distinct physical linear resonant actuator (LRA)
 * vibration waveforms and composition primitives.
 */
enum class HapticType(val displayName: String, val description: String) {
    NAV_TICK(
        displayName = "Navigation Tick",
        description = "Ultra-light 12ms detent for bottom tabs & breadcrumbs"
    ),
    CLICK(
        displayName = "Tactile Click",
        description = "Crisp 22ms pop for glass buttons & cards"
    ),
    MENU_POP(
        displayName = "Menu Pop",
        description = "Fast soft notch when 3-dot dropdown appears"
    ),
    FOLDER_OPEN(
        displayName = "Folder Open",
        description = "Dual-pulse tactile bounce for entering folders"
    ),
    SWITCH_TOGGLE(
        displayName = "Switch Toggle",
        description = "Mechanical magnetic latch for toggle switches"
    ),
    SEGMENT_SLIDE(
        displayName = "Segment Slide",
        description = "Sliding gear-tooth detent for Light / Dark / Auto"
    ),
    SELECTION_NOTCH(
        displayName = "Selection Notch",
        description = "Index step notch when choosing sort options"
    ),
    LONG_PRESS(
        displayName = "Long Press",
        description = "Deep 40ms inertial thud for multi-select activation"
    ),
    DESTRUCTIVE(
        displayName = "Destructive Alert",
        description = "Cautionary double heavy pulse for deletion actions"
    ),
    HERO_CLEAN_PULSE(
        displayName = "Hero Clean Wave",
        description = "Rising frequency sweep + confirmation pop for storage clean"
    ),
    SUCCESS_CONFIRM(
        displayName = "Success Confirm",
        description = "Positive confirmation notch for saved operations"
    ),
    ERROR_REJECT(
        displayName = "Error Rejection",
        description = "Staccato triple-tick rejection for boundaries or errors"
    )
}
