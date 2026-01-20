# 🎮 babixGO - MonopolyGo Manager

Native Android application for managing MonopolyGo accounts with Tycoon Racers event automation.

## ✨ Features

### Account Management
- 📱 Root-based account backup and restore
- 🔄 Multi-device synchronization via Supabase
- 🆔 Automatic device ID extraction (SSAID, GAID, Device-ID)
- 📊 Suspension tracking and status management
- ☁️ Cloud storage for account metadata

### Event Management (Tycoon Racers)
- 📅 Event creation with date ranges
- 👥 Customer management with friend links
- 🏆 Team organization with 4 account slots
- 🔗 Account-to-team assignments
- 📋 Event overview and statistics

## 🚀 Quick Start

### Prerequisites
- Android device (API 21+)
- Root access (SuperSU or Magisk)
- MonopolyGo app installed
- Internet connection

### Installation

1. **Setup Supabase Backend**
   ```bash
   # See SUPABASE_SETUP.md for detailed instructions
   # 1. Create Supabase project
   # 2. Run supabase_schema.sql in SQL Editor
   # 3. Copy Project URL and anon key
   ```

2. **Configure Credentials**
   ```properties
   # gradle.properties
   SUPABASE_URL=https://xxxxx.supabase.co
   SUPABASE_ANON_KEY=eyJhbGc...your-key-here
   ```

3. **Build and Install**
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

4. **Grant Root Permission**
   - Open app
   - Allow root access when prompted
   - Start using!

## 📚 Documentation

### Setup & Configuration
- [**SUPABASE_SETUP.md**](SUPABASE_SETUP.md) - Complete Supabase setup guide
- [**supabase_schema.sql**](supabase_schema.sql) - Database schema

### Testing
- [**TESTING_GUIDE.md**](TESTING_GUIDE.md) - Comprehensive testing procedures
- Manual testing scripts
- Acceptance criteria checklists

### Implementation Details
- [**IMPLEMENTATION_SUMMARY_TEIL4.md**](IMPLEMENTATION_SUMMARY_TEIL4.md) - Event management (Teil 4)
- [**IMPLEMENTATION_SUMMARY_TEIL5.md**](IMPLEMENTATION_SUMMARY_TEIL5.md) - Team automation (Teil 5)
- [**IMPLEMENTATION_SUMMARY_TEIL6.md**](IMPLEMENTATION_SUMMARY_TEIL6.md) - Database & final steps (Teil 6)

## 🏗️ Architecture

### Tech Stack
- **Frontend:** Native Android (Java)
- **Backend:** Supabase (PostgreSQL)
- **Root Access:** libsu
- **UI:** Material Design Components
- **Async:** CompletableFuture

### Data Flow
```
Android App (Root)
    ↓
Local Account Extraction
    ↓
Supabase PostgreSQL
    ↓
Multi-Device Sync
```

### Key Components

**Activities:**
- `MainActivity` - Entry point
- `EventListActivity` - Event overview
- `EventDetailActivity` - Team management
- `AccountManagementActivity` - Account operations

**Repositories:**
- `EventRepository` - Event CRUD
- `CustomerRepository` - Customer CRUD
- `TeamRepository` - Team CRUD
- `AccountRepository` - Account CRUD

**Utilities:**
- ~~`EventExecutor` - Automated event processing~~ (REMOVED)
- `AccountManager` - Root operations
- `SupabaseManager` - API integration

## 🗄️ Database Schema

### Tables
- **accounts** - MonopolyGo accounts with device IDs
- **events** - Tycoon Racers events
- **customers** - Event customers
- **teams** - Teams with 4 account slots

### Features
- Foreign key relationships
- Auto-updating timestamps
- Computed columns (suspension_count, is_suspended)
- Useful views (active_accounts, teams_with_details)
- Row Level Security (RLS)

## 🧪 Testing

### Run Tests
```bash
# Unit tests
./gradlew testDebug

# Integration tests (requires device)
./gradlew connectedDebugAndroidTest

# Lint
./gradlew lint
```

### Manual Testing
See [TESTING_GUIDE.md](TESTING_GUIDE.md) for comprehensive test procedures.

## 🐛 Troubleshooting

### Common Issues

**"Failed to load accounts"**
- Check internet connection
- Verify Supabase credentials
- Check RLS policies in Supabase

**"UserID not found"**
- Open MonopolyGo at least once
- Verify root access
- Check if preference file exists

**"Event execution stops"**
- Verify root access granted
- Check account backup files exist
- Review progress logs

See [SUPABASE_SETUP.md](SUPABASE_SETUP.md) for more troubleshooting tips.

## 📈 Roadmap

### Version 1.1 (Current)
- ✅ Account management
- ✅ Event management
- ✅ Team automation
- ✅ Multi-device sync

### Version 1.2
- [ ] Local SQLite cache
- [ ] Offline mode
- [ ] Conflict resolution
- [ ] Backup/restore as ZIP

### Version 2.0
- [ ] Realtime sync
- [ ] Push notifications
- [ ] Shared events
- [ ] Advanced analytics

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📄 License

This project is for personal use. MonopolyGo is a trademark of Scopely.

## 🙏 Acknowledgments

- Supabase for backend infrastructure
- Material Design for UI components
- libsu for root access management
- MonopolyGo community

## 📞 Support

- 📖 Documentation: See docs above
- 🐛 Bug Reports: GitHub Issues
- 💬 Questions: GitHub Discussions

---

**Built with ❤️ for the MonopolyGo community**

*Version 1.0 - Production Ready* 🚀
