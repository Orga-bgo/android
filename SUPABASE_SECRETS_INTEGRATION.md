# Supabase Secrets Integration Guide

## Overview

This guide explains how Supabase credentials (SB_KEY and SB_URL) are integrated into the Android app build process.

## Architecture

The app uses a **multi-tier configuration system** with the following priority:

1. **Environment Variables** (Highest Priority)
2. **gradle.properties** (Fallback)
3. **Empty String** (Default)

## GitHub Secrets Configuration

### Required Secrets

Add the following secrets to your GitHub repository:

1. **SB_URL**: Your Supabase project URL
   - Example: `https://xxxxx.supabase.co`
   - Location: GitHub Repository → Settings → Secrets and variables → Actions → New repository secret

2. **SB_KEY**: Your Supabase anon/public key
   - Example: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`
   - Location: Same as above

### Finding Your Supabase Credentials

1. Go to your Supabase project dashboard: https://app.supabase.com
2. Navigate to: Settings → API
3. Copy:
   - **Project URL** → Use as `SB_URL`
   - **anon public** key → Use as `SB_KEY`

⚠️ **NEVER** commit the service_role key to GitHub secrets or code!

## How It Works

### GitHub Actions Workflow

The workflow file `.github/workflows/build-apk.yml` injects secrets as environment variables:

```yaml
- name: Build Debug APK
  env:
    SUPABASE_URL: ${{ secrets.SB_URL }}
    SUPABASE_ANON_KEY: ${{ secrets.SB_KEY }}
  run: ./gradlew assembleDebug --stacktrace
```

### Build Configuration

The `app/build.gradle` file reads these values:

```gradle
buildConfigField "String", "SUPABASE_URL", 
    "\"${System.getenv('SUPABASE_URL') ?: project.findProperty('SUPABASE_URL') ?: ''}\""
buildConfigField "String", "SUPABASE_ANON_KEY", 
    "\"${System.getenv('SUPABASE_ANON_KEY') ?: project.findProperty('SUPABASE_ANON_KEY') ?: ''}\""
```

This creates constants in `BuildConfig.java`:

```java
public static final String SUPABASE_URL = "https://xxxxx.supabase.co";
public static final String SUPABASE_ANON_KEY = "eyJhbGc...";
```

### SupabaseManager

The `SupabaseManager.java` class uses these constants:

```java
private SupabaseManager() {
    this.supabaseUrl = BuildConfig.SUPABASE_URL;
    this.supabaseKey = BuildConfig.SUPABASE_ANON_KEY;
    // ...
}
```

## Local Development

### Option 1: Environment Variables (Recommended)

Set environment variables before building:

```bash
export SUPABASE_URL="https://xxxxx.supabase.co"
export SUPABASE_ANON_KEY="eyJhbGc..."
./gradlew assembleDebug
```

### Option 2: gradle.properties

Edit `gradle.properties` and add your credentials:

```properties
SUPABASE_URL=https://xxxxx.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

⚠️ **Important**: If you add real credentials to `gradle.properties`, add it to `.gitignore`:

```bash
echo "gradle.properties" >> .gitignore
```

Alternatively, create a local `gradle.properties` file that is git-ignored and keep the repository version with placeholder values.

### Option 3: secrets.xml (Alternative)

While not currently used by the build system, you can also store secrets in:
`app/src/main/res/values/secrets.xml` (this file is already in `.gitignore`)

## Verification

### Check BuildConfig Generation

After building, verify the generated constants:

```bash
./gradlew assembleDebug
cat app/build/generated/source/buildConfig/debug/de/babixgo/monopolygo/BuildConfig.java
```

You should see:
```java
public static final String SUPABASE_URL = "https://xxxxx.supabase.co";
public static final String SUPABASE_ANON_KEY = "eyJhbGc...";
```

### Test Supabase Connection

The app will automatically use these credentials when making Supabase API calls through `SupabaseManager`.

To verify the connection works:
1. Build and install the app
2. Launch the app and check LogCat for Supabase-related logs:
   ```bash
   adb logcat | grep -i "supabase"
   ```

## Security Best Practices

### ✅ DO

- Store credentials in GitHub Secrets for CI/CD
- Use environment variables for local development
- Keep `.gitignore` up-to-date to exclude secrets
- Use the **anon/public** key only (not service_role)
- Rotate keys if accidentally committed

### ❌ DON'T

- Commit real credentials to Git
- Use service_role key in the app
- Share credentials in chat/email
- Hard-code credentials in source files

## Troubleshooting

### Build fails with "SUPABASE_URL not found"

**Solution**: Ensure GitHub secrets are set or gradle.properties has values.

### App shows "Supabase not configured"

**Solution**: Check that BuildConfig contains non-empty, non-placeholder values:
```java
// Bad:
SUPABASE_URL = ""
SUPABASE_URL = "https://your-project.supabase.co"

// Good:
SUPABASE_URL = "https://xxxxx.supabase.co"
```

### Gradle sync fails

**Solution**: Run:
```bash
./gradlew clean
./gradlew assembleDebug --refresh-dependencies
```

## Migration from Previous Setup

If you previously used a different configuration method:

1. **Update GitHub Secrets**: Add `SB_URL` and `SB_KEY` secrets
2. **No code changes needed**: The app already uses `BuildConfig.SUPABASE_URL` and `BuildConfig.SUPABASE_ANON_KEY`
3. **Test the build**: Run a GitHub Actions build to verify

## References

- [Supabase Documentation](https://supabase.com/docs)
- [GitHub Actions Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [Android BuildConfig](https://developer.android.com/studio/build/gradle-tips#share-custom-fields-and-resource-values-with-your-app-code)

## Support

For issues or questions:
1. Check existing documentation: `SUPABASE_SETUP.md`, `SUPABASE_INTEGRATION_GUIDE.md`
2. Review Supabase Dashboard logs
3. Check Android LogCat output
4. Open a GitHub issue with relevant logs
