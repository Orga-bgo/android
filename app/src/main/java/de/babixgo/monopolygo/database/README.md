# Database Package

This package contains Supabase integration and repository classes.

## Planned Classes

- **SupabaseClient.java** - Supabase client initialization and configuration
- **AccountRepository.java** - Account CRUD operations
- **CustomerRepository.java** - Customer data operations
- **EventRepository.java** - Event management operations

## Purpose

This layer handles all database interactions with Supabase, abstracting the data access logic from the UI layer. The root-level operations (RootManager, AccountManager, DataExtractor) are NOT modified and work independently.

## Architecture

```
Database Layer (NEW - Supabase)
├─ SupabaseClient.java
├─ AccountRepository.java
└─ EventRepository.java

Root Layer (EXISTING - DO NOT MODIFY)
├─ RootManager.java
├─ AccountManager.java
└─ DataExtractor.java
```

---
**Status**: Part 1/6 - Package structure created
**Next**: Part 2 - Implement Supabase client and repositories
