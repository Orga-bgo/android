# BabixGO Design System - Usage Examples

This document provides examples of how to use the newly implemented design system in your layouts.

## Button Examples

### Blue Button (Primary Action)
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btnSaveAccount"
    style="@style/BabixButton.Blue"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="Account speichern" />
```

### Gray Button (Secondary Action)
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btnCancel"
    style="@style/BabixButton.Gray"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Abbrechen" />
```

### Red Button (Destructive Action)
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btnDelete"
    style="@style/BabixButton.Red"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Löschen" />
```

### Green Button (Success Action)
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btnRestore"
    style="@style/BabixButton.Green"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="Wiederherstellen" />
```

## Card Examples

### Simple Card
```xml
<com.google.android.material.card.MaterialCardView
    style="@style/BabixCard"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">
    
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">
        
        <TextView
            style="@style/BabixText.Header"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Account Details" />
            
        <TextView
            style="@style/BabixText.Body"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:text="User ID: 123456789" />
            
    </LinearLayout>
    
</com.google.android.material.card.MaterialCardView>
```

## Text Styles

### Header Text
```xml
<TextView
    style="@style/BabixText.Header"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="Accountverwaltung" />
```

### Body Text
```xml
<TextView
    style="@style/BabixText.Body"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="Wählen Sie einen Account aus der Liste" />
```

### Label Text
```xml
<TextView
    style="@style/BabixText.Label"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Account Name:" />
```

### Small Text
```xml
<TextView
    style="@style/BabixText.Small"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Zuletzt aktualisiert: 20.01.2026" />
```

## Color Usage

### Backgrounds
```xml
<!-- Light background -->
<View
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/background_light" />

<!-- Dark header -->
<View
    android:layout_width="match_parent"
    android:layout_height="56dp"
    android:background="@color/header_dark" />

<!-- Card background -->
<View
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@color/card_white" />
```

### Text Colors
```xml
<!-- Dark text (primary) -->
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:textColor="@color/text_dark"
    android:text="Primary text" />

<!-- Gray text (secondary) -->
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:textColor="@color/text_gray"
    android:text="Secondary text" />

<!-- Light gray text (tertiary) -->
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:textColor="@color/text_light_gray"
    android:text="Tertiary text" />
```

### Status Colors
```xml
<!-- Error message -->
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:textColor="@color/error_red"
    android:text="Fehler: Account nicht gefunden" />

<!-- Success message -->
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:textColor="@color/success_green"
    android:text="Account erfolgreich gespeichert" />

<!-- Warning message -->
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:textColor="@color/warning_orange"
    android:text="Warnung: Backup empfohlen" />
```

## Complete Layout Example

### Account List Item
```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    style="@style/BabixCard"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">
    
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">
        
        <!-- Account name -->
        <TextView
            style="@style/BabixText.Header"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Mein Account" />
        
        <!-- User ID label and value -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginTop="8dp">
            
            <TextView
                style="@style/BabixText.Label"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="User ID: " />
                
            <TextView
                style="@style/BabixText.Body"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="123456789" />
                
        </LinearLayout>
        
        <!-- Last backup date -->
        <TextView
            style="@style/BabixText.Small"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:text="Letztes Backup: 20.01.2026 18:00" />
        
        <!-- Action buttons -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginTop="16dp"
            android:gravity="end">
            
            <com.google.android.material.button.MaterialButton
                style="@style/BabixButton.Gray"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginEnd="8dp"
                android:text="Bearbeiten" />
                
            <com.google.android.material.button.MaterialButton
                style="@style/BabixButton.Blue"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Wiederherstellen" />
                
        </LinearLayout>
        
    </LinearLayout>
    
</com.google.android.material.card.MaterialCardView>
```

## Best Practices

1. **Always use styles** instead of hardcoding colors and dimensions
2. **Use semantic color names** (e.g., `@color/error_red` instead of `#EF4444`)
3. **Maintain consistency** across all layouts
4. **Test on multiple screen sizes** to ensure responsive design
5. **Use Material Design components** (MaterialButton, MaterialCardView) for better compatibility

## Migration Guide

### Updating Existing Layouts

Replace hardcoded styles:
```xml
<!-- OLD -->
<Button
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:background="#3B82F6"
    android:textColor="#FFFFFF"
    android:text="Save" />

<!-- NEW -->
<com.google.android.material.button.MaterialButton
    style="@style/BabixButton.Blue"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Save" />
```

Replace hardcoded colors:
```xml
<!-- OLD -->
<TextView
    android:textColor="#1E252B"
    android:textSize="22sp"
    android:textStyle="bold"
    android:text="Title" />

<!-- NEW -->
<TextView
    style="@style/BabixText.Header"
    android:text="Title" />
```

---

**For Part 2**: These styles will be used in all new layouts (AccountListActivity, EventListActivity, etc.)
