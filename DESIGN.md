---
name: Suntimes Prayer Times Addon
description: A calm, dependable Android interface for location-aware prayer times.
colors:
  earth-teal-light: "#00695C"
  earth-teal-dark: "#16A394"
  prayer-gold-light: "#D4A017"
  prayer-gold-dark: "#D8B14B"
  quiet-parchment-light: "#F6F1E6"
  quiet-parchment-dark: "#101D22"
  sapphire-light: "#1B5EAB"
  sapphire-dark: "#7AB7FF"
  rose-light: "#8B2F4A"
  rose-dark: "#FF8AB6"
  surface-light: "#FFFFFF"
  surface-dark: "#18282E"
  surface-muted-light: "#F1ECE1"
  surface-muted-dark: "#142227"
  charcoal-ink-light: "#263238"
  charcoal-ink-dark: "#E7EEF0"
  calendar-prayer: "#2E7D32"
  calendar-prohibited: "#E65100"
  calendar-night: "#1565C0"
typography:
  headline:
    fontFamily: "Roboto, system-ui, sans-serif"
    fontSize: "28sp"
    fontWeight: 400
    lineHeight: "36sp"
  title:
    fontFamily: "Roboto, system-ui, sans-serif"
    fontSize: "16sp"
    fontWeight: 500
    lineHeight: "24sp"
    letterSpacing: "0.15sp"
  body:
    fontFamily: "Roboto, system-ui, sans-serif"
    fontSize: "14sp"
    fontWeight: 400
    lineHeight: "20sp"
    letterSpacing: "0.25sp"
  label:
    fontFamily: "Roboto, system-ui, sans-serif"
    fontSize: "11sp"
    fontWeight: 500
    lineHeight: "16sp"
    letterSpacing: "0.5sp"
rounded:
  option: "12dp"
  compact-card: "14dp"
  card: "16dp"
  widget: "18dp"
  prayer: "26dp"
  material-extra-large: "28dp"
spacing:
  xs: "2dp"
  sm: "6dp"
  md: "8dp"
  lg: "12dp"
  xl: "14dp"
  xxl: "16dp"
components:
  widget-container:
    backgroundColor: "{colors.quiet-parchment-light}"
    textColor: "{colors.charcoal-ink-light}"
    rounded: "{rounded.widget}"
    padding: "6dp"
  location-option-card:
    backgroundColor: "{colors.surface-light}"
    textColor: "{colors.charcoal-ink-light}"
    rounded: "{rounded.option}"
    padding: "10dp 12dp"
  prayer-card:
    backgroundColor: "{colors.surface-light}"
    textColor: "{colors.charcoal-ink-light}"
    rounded: "{rounded.prayer}"
    padding: "8dp 14dp"
---

# Design System: Suntimes Prayer Times Addon

## Overview

**Creative North Star: "The Quiet Almanac"**

The interface should feel like a dependable daily reference: calm enough for repeated use, precise enough for alarms, and structured enough that the active date, location, and next prayer are never ambiguous. Native Material 3 patterns carry interaction behavior while a restrained, earth-led palette gives the product its own identity.

Information hierarchy must survive small widgets, large system fonts, English, and Arabic RTL. Prayer times lead; dates, method details, prohibited windows, and night portions recede or disappear as space narrows. The system explicitly rejects ornamental religious motifs, flashy clock styling, information-dense layouts without hierarchy, decorative effects that compete with times, and ambiguous failure states.

**Key Characteristics:**
- Calm earth-led color with user-selectable sapphire, rose, and dynamic alternatives.
- Native Android typography and controls.
- Tonal hierarchy, soft borders, and no decorative elevation.
- Location, date, next prayer, and failures remain explicit.
- Responsive reduction rather than compressed text.

## Colors

Earth Teal and Prayer Gold establish the default identity over Quiet Parchment. Sapphire and Rose are complete user-selected accent alternatives, not extra decoration. Dark mode retains the same semantic roles with brighter accents and deep tinted surfaces.

### Primary
- **Earth Teal:** Primary actions, selected states, the widget accent rule, next-prayer emphasis, and light prohibited periods.
- **Sapphire:** Cool primary alternative for users who select the Sapphire palette.
- **Rose:** Warm primary alternative for users who select the Rose palette.

### Secondary
- **Prayer Gold:** Secondary emphasis and heavier prohibited-period context in the default palette.

### Neutral
- **Quiet Parchment:** Default app and widget background; never use it as decoration inside already layered surfaces.
- **Charcoal Ink:** Primary text and icons, preserving strong contrast in both themes.
- **Surface:** Cards and interactive containers above the page background.
- **Muted Surface:** Passed, secondary, or grouped content that should recede without becoming illegible.

### Named Rules

**The One Active Accent Rule.** Use Earth Teal, Sapphire, Rose, or Android dynamic color as the active palette—never mix palette primaries on one surface.

**The Meaning Before Color Rule.** Color may reinforce prayer, prohibited, and night states, but labels, structure, and icons must carry the meaning independently.

## Typography

**Display Font:** Roboto with the Android system sans fallback
**Body Font:** Roboto with the Android system sans fallback

**Character:** Familiar, neutral, and highly legible. The system uses the native Material 3 hierarchy rather than a decorative type pairing, allowing Arabic and system font substitution to remain first-class.

### Hierarchy
- **Headline:** Large prayer times and high-value summaries only.
- **Title:** App-bar titles, prayer names, dates, and selected location names.
- **Body:** Explanations, settings values, method summaries, and normal status text.
- **Label:** Compact widget labels and metadata; never shrink meaningful information below this role.

### Named Rules

**The Time Leads Rule.** A prayer time must be at least as prominent as its label, and the next prayer may receive weight and accent—not a larger decorative font.

**The No Squeezing Rule.** When content does not fit, reduce detail or change layout. Never solve narrow widths by pushing meaningful text below 11sp.

## Elevation

The system is flat by default. Depth comes from tonal surface changes and soft 1dp outlines; there is no custom shadow vocabulary. Material overlays may provide native transient elevation for dialogs and menus, but cards and widget containers remain visually grounded.

### Named Rules

**The Flat Almanac Rule.** Persistent content uses tonal layering and borders, never decorative drop shadows.

## Components

### Buttons
- **Shape:** Native Material 3 button geometry.
- **Primary:** Solid active-palette color with the theme's on-primary text; reserve it for decisive recovery or confirmation actions.
- **Focus / Pressed:** Use native Android state layers and accessible touch targets.
- **Text:** Text buttons handle dialog actions and low-emphasis navigation.

### Cards / Containers
- **Corner Style:** Gently rounded information cards; location options use the compact option radius, standard cards use the card radius, and timeline prayer cards use the larger prayer radius.
- **Background:** Surface colors separate cards from the parchment or tinted page background.
- **Shadow Strategy:** None; use a soft 1dp outline when separation or selection needs reinforcement.
- **Internal Padding:** Use the established 10–14dp content range.

### Inputs / Fields
- **Style:** Native Material 3 outlined fields on the active surface.
- **Focus:** The active palette supplies label and outline emphasis.
- **Error / Disabled:** Use native semantic states with explanatory text; never rely on color alone.

### Navigation
- **Style:** Background-colored Material 3 top app bars with title, optional location subtitle, and 44dp icon actions. Active location context stays adjacent to the screen title and truncates safely.

### Today Widget
- **Style:** An 18dp rounded, softly outlined container with a 3dp accent rule. Prayer times remain the primary row.
- **Responsive behavior:** Wide layouts may show all six prayers and optional rows. Narrow layouts must switch to a focused summary rather than compressing six columns.
- **Interaction:** Root opens today; header opens the calendar. Both preserve the widget's location scope.

## Do's and Don'ts

### Do:
- **Do** make date, location, and next prayer legible before secondary method details.
- **Do** preserve 44dp activity touch targets and native Android state feedback.
- **Do** use tonal surfaces and soft 1dp outlines for hierarchy.
- **Do** test English, Arabic RTL, 12/24-hour time, system font scaling, light/dark themes, and compact widget widths.
- **Do** replace unavailable data with an explicit, actionable state.

### Don't:
- **Don't** use ornamental religious motifs.
- **Don't** use flashy clock styling.
- **Don't** ship information-dense layouts without hierarchy.
- **Don't** add decorative effects that compete with times.
- **Don't** show ambiguous failure states or a normal-looking table of placeholders.
- **Don't** shrink meaningful widget text below 11sp to force six columns into a compact width.
- **Don't** combine multiple user-selectable palette accents on one surface.
