# Utils Package

This package contains utility classes and helper functions.

## Planned Classes

- **DeviceIdExtractor.java** - Extract device ID using RootManager (wraps root operations)
- **DateFormatter.java** - Date formatting utilities
- **ValidationHelper.java** - Input validation helpers

## Purpose

Utility classes provide common functionality used across the application. The DeviceIdExtractor will use the existing RootManager internally without modifying it.

## Integration with Root Layer

```java
// Example: DeviceIdExtractor uses RootManager without modifying it
public class DeviceIdExtractor {
    public static String extractDeviceId() {
        // Uses RootManager.runRootCommand() internally
        // RootManager.java is NOT modified
    }
}
```

---
**Status**: Part 1/6 - Package structure created
**Next**: Part 4 - Implement utility classes
