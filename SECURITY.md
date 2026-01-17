# Security Policy

## Security Considerations for MonopolyGo Manager

This document outlines the security considerations and best practices for using the MonopolyGo Manager Android application.

## ⚠️ Root Access Requirements

### Critical Security Notice
This application **requires root access** to function properly. Root access provides elevated privileges that can potentially be misused if the device is compromised.

### Root Access is Used For:
1. **Reading protected app data**: `/data/data/com.scopely.monopolygo/`
2. **Copying account files**: Account backup and restoration
3. **App control**: Force-stopping and starting MonopolyGo app
4. **SharedPreferences access**: Reading UserID from app settings

### Security Implications:
- ✅ **Legitimate use**: This app only uses root to manage MonopolyGo data
- ⚠️ **Risk**: Root access bypasses Android's security model
- ⚠️ **Device security**: A rooted device is inherently less secure
- ⚠️ **Malware risk**: Other apps with root could potentially access this app's data

## 🔐 Data Security

### Sensitive Data Handled:
1. **UserIDs**: MonopolyGo user identifiers
2. **Account files**: Game save data
3. **API Keys**: Short.io API key (hardcoded)
4. **Friend links**: Deep links containing user information

### Data Protection Measures:
- All data stored in standard Android storage locations
- No data transmitted except to Short.io API
- No analytics or tracking implemented
- No cloud synchronization

### Areas of Concern:
1. **API Key**: Currently hardcoded in `ShortLinkManager.java`
   - Consider moving to secure configuration
   - Implement key rotation mechanism
   - Use Android Keystore for storage

2. **File Permissions**: Account files stored with world-readable permissions
   - Consider encrypting sensitive backup files
   - Implement file-level encryption for account data

## 🛡️ Permissions

### Required Permissions:
```xml
WRITE_EXTERNAL_STORAGE  - Save account backups
READ_EXTERNAL_STORAGE   - Read account backups
INTERNET                - Short.io API access
ACCESS_NETWORK_STATE    - Check connectivity
```

### Root Permission:
- Requested at runtime
- User must explicitly grant via SuperSU/Magisk
- App checks for root before critical operations

## 🔍 Known Security Limitations

### 1. Hardcoded API Credentials
**Issue**: Short.io API key is hardcoded in source code
**Risk**: API key exposure if APK is decompiled
**Mitigation**: 
- Store key in native library
- Use Android Keystore
- Implement server-side API proxy

### 2. No Data Encryption
**Issue**: Account backups stored in plain text
**Risk**: Anyone with file access can read account data
**Mitigation**:
- Implement AES encryption for backups
- Use Android Keystore for key management
- Require device authentication for sensitive operations

### 3. Root Command Injection
**Issue**: Commands constructed from user input
**Risk**: Potential command injection if input not sanitized
**Current Protection**: Limited user input in command construction
**Recommendation**: Implement strict input validation

### 4. No Certificate Pinning
**Issue**: HTTPS connections to Short.io not pinned
**Risk**: Potential MITM attacks
**Mitigation**: Implement certificate pinning for API calls

## 📋 Security Best Practices for Users

### Device Security:
1. ✅ Only install from trusted sources
2. ✅ Keep SuperSU/Magisk updated
3. ✅ Use device encryption
4. ✅ Set strong lock screen password
5. ⚠️ Be aware that root voids some security guarantees

### App Usage:
1. ✅ Only grant root when necessary
2. ✅ Review permissions before installation
3. ✅ Keep backups on external secure storage
4. ✅ Don't share account files publicly
5. ⚠️ Understand that root apps can access sensitive data

### Network Security:
1. ✅ Only use on trusted networks for API operations
2. ⚠️ Avoid public WiFi when creating short links
3. ✅ Consider using VPN for additional security

## 🐛 Reporting Security Vulnerabilities

If you discover a security vulnerability in MonopolyGo Manager:

### Please DO:
1. **Report privately** via GitHub Security Advisories
2. Provide detailed description of the vulnerability
3. Include steps to reproduce
4. Suggest potential fixes if possible
5. Allow reasonable time for fix before public disclosure

### Please DON'T:
1. Don't publicly disclose the vulnerability immediately
2. Don't exploit the vulnerability maliciously
3. Don't test on systems you don't own

### Response Timeline:
- **Initial Response**: Within 48 hours
- **Fix Development**: Within 2 weeks for critical issues
- **Fix Release**: As soon as testing is complete
- **Public Disclosure**: After fix is deployed

## 🔄 Security Update Policy

### Update Channels:
- Security fixes released as soon as possible
- Updates published via GitHub releases
- Critical security updates marked clearly

### Versioning:
- Security patches increment patch version (1.0.x)
- Major security overhauls increment minor version (1.x.0)

## 🛠️ Recommended Security Improvements

### High Priority:
1. **Encrypt API keys**: Move to secure storage
2. **Encrypt backups**: Implement file-level encryption
3. **Input validation**: Strengthen command sanitization
4. **Certificate pinning**: Add for API connections

### Medium Priority:
1. **Secure logging**: Remove sensitive data from logs
2. **Memory protection**: Clear sensitive data after use
3. **Integrity checks**: Verify file integrity before restoration
4. **Rate limiting**: Implement for API calls

### Low Priority:
1. **Code obfuscation**: Additional ProGuard rules
2. **Root detection**: Add SafetyNet attestation
3. **Jailbreak detection**: Enhanced root checks
4. **Tamper detection**: Verify app signature

## 📚 Security Resources

### Android Security:
- [Android Security Best Practices](https://developer.android.com/topic/security/best-practices)
- [Android Keystore System](https://developer.android.com/training/articles/keystore)
- [Network Security Configuration](https://developer.android.com/training/articles/security-config)

### Root Security:
- [Magisk Documentation](https://topjohnwu.github.io/Magisk/)
- [Root Detection Techniques](https://github.com/scottyab/rootbeer)

### OWASP:
- [OWASP Mobile Security](https://owasp.org/www-project-mobile-security/)
- [OWASP Mobile Top 10](https://owasp.org/www-project-mobile-top-10/)

## ⚖️ Disclaimer

This application is provided "as-is" without warranties. Users assume all risks associated with:
- Using root access
- Managing game account data
- Using the Short.io API integration
- Storing backup files

**Use at your own risk. The developers are not responsible for:**
- Account loss or corruption
- Security breaches
- Device damage
- Violations of MonopolyGo terms of service

## 📝 License

This security policy is part of the MonopolyGo Manager project and is subject to the project's license terms.

---

**Last Updated**: 2026-01-12
**Version**: 1.0
**Contact**: Open a GitHub issue for security concerns
