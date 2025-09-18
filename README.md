# MifareClassicPlugin

A Cordova plugin for reading MIFARE Classic NFC cards with sector-based data extraction, designed for Ionic 6 applications.

## Features

- ✅ Read MIFARE Classic card sectors and convert to ASCII text
- ✅ Support for both Key A and Key B authentication with default keys
- ✅ Cross-platform support (Android and iOS)
- ✅ Comprehensive error handling and validation
- ✅ NFC availability and status checking
- ✅ Foreground NFC listening capabilities

## Platform Support

| Platform | Version | NFC Support |
|----------|---------|-------------|
| Android  | API 14+ | Full MIFARE Classic support |
| iOS      | 13.0+   | Limited (Core NFC constraints) |

## Installation

### Prerequisites

- Ionic 6 project
- Cordova CLI installed
- For Android: Android SDK with API level 14+
- For iOS: Xcode 11+ with iOS 13.0+ deployment target

### Option 1: Using Ionic Native Wrapper (Recommended)

The modern way to use this plugin with full TypeScript support:

```bash
# 1. Install the Cordova plugin
ionic cordova plugin add path/to/cordova-plugin-mifare-classic

# 2. Install the Ionic Native wrapper
npm install ./ionic-native

# 3. Add platforms
ionic cordova platform add android
ionic cordova platform add ios
```

### Option 2: Direct Cordova Plugin Usage

```bash
# Install from local directory
ionic cordova plugin add path/to/cordova-plugin-mifare-classic

# Or install from git repository
ionic cordova plugin add https://github.com/yourusername/cordova-plugin-mifare-classic.git

# Add platforms
ionic cordova platform add android
ionic cordova platform add ios
```

### Platform-Specific Setup

#### Android

The plugin automatically configures the required permissions and NFC intent filters. Ensure your device has NFC capability and it's enabled in settings.

#### iOS

1. Enable NFC capability in your Apple Developer account
2. Add NFC entitlements to your app
3. The plugin requires iOS 13.0+ for Core NFC support

**Note**: iOS has limited MIFARE Classic support due to Core NFC framework constraints.

## Usage Methods

### Method 1: Ionic Native Wrapper (Recommended)

Modern TypeScript approach with full type safety and Angular integration:

```typescript
import { Component } from '@angular/core';
import { MifareClassic, NfcStatus, SectorReadResult } from '@ionic-native/mifare-classic/ngx';

@Component({
  selector: 'app-nfc',
  templateUrl: 'nfc.page.html'
})
export class NfcPage {

  constructor(private mifareClassic: MifareClassic) {}

  async initializeNfc() {
    try {
      // Modern async/await syntax
      const status: NfcStatus = await this.mifareClassic.initialize();
      console.log('NFC ready:', status);
    } catch (error) {
      console.error('NFC initialization failed:', error);
    }
  }

  async readCard() {
    try {
      // Type-safe sector reading
      const result: SectorReadResult = await this.mifareClassic.readSector(0);
      console.log('Card data:', result.asciiData);

      // Read multiple sectors
      const multiResults = await this.mifareClassic.readMultipleSectors([0, 1, 2, 3]);
      multiResults.forEach(r => console.log(`Sector ${r.sectorNumber}:`, r.asciiData));
    } catch (error) {
      console.error('Read failed:', error);
    }
  }
}
```

### Method 2: Direct Cordova Plugin Usage

Traditional callback-based approach:

```javascript
// Check NFC and read sector using callbacks
cordova.plugins.MifareClassicPlugin.isNfcAvailable(
  function(available) {
    if (available) {
      cordova.plugins.MifareClassicPlugin.startNfcListener(
        function() {
          cordova.plugins.MifareClassicPlugin.readMifareClassicSector(
            0,
            function(data) { console.log('Data:', data); },
            function(error) { console.error('Error:', error); }
          );
        },
        function(error) { console.error('Listener error:', error); }
      );
    }
  },
  function(error) { console.error('NFC check error:', error); }
);
```

## API Reference

### Methods

#### `isNfcAvailable(successCallback, errorCallback)`

Check if NFC is available on the device.

```javascript
cordova.plugins.MifareClassicPlugin.isNfcAvailable(
  function(available) {
    console.log('NFC Available:', available);
  },
  function(error) {
    console.error('Error:', error);
  }
);
```

#### `isNfcEnabled(successCallback, errorCallback)`

Check if NFC is enabled on the device.

```javascript
cordova.plugins.MifareClassicPlugin.isNfcEnabled(
  function(enabled) {
    console.log('NFC Enabled:', enabled);
  },
  function(error) {
    console.error('Error:', error);
  }
);
```

#### `startNfcListener(successCallback, errorCallback)`

Start listening for NFC tags.

```javascript
cordova.plugins.MifareClassicPlugin.startNfcListener(
  function(result) {
    console.log('NFC Listener started');
  },
  function(error) {
    console.error('Error starting listener:', error);
  }
);
```

#### `stopNfcListener(successCallback, errorCallback)`

Stop listening for NFC tags.

```javascript
cordova.plugins.MifareClassicPlugin.stopNfcListener(
  function(result) {
    console.log('NFC Listener stopped');
  },
  function(error) {
    console.error('Error stopping listener:', error);
  }
);
```

#### `readMifareClassicSector(sectorNumber, successCallback, errorCallback)`

Read a specific sector from a MIFARE Classic card and return ASCII data.

**Parameters:**
- `sectorNumber` (number): Sector number to read (0-39 for MIFARE Classic 4K)
- `successCallback` (function): Called with ASCII data on success
- `errorCallback` (function): Called with error message on failure

```javascript
cordova.plugins.MifareClassicPlugin.readMifareClassicSector(
  0, // Read sector 0
  function(asciiData) {
    console.log('Sector data:', asciiData);
  },
  function(error) {
    console.error('Read error:', error);
  }
);
```

## Usage Example

### Basic Implementation

```javascript
document.addEventListener('deviceready', function() {
  // Check NFC availability
  cordova.plugins.MifareClassicPlugin.isNfcAvailable(
    function(available) {
      if (available) {
        // Start NFC listener
        cordova.plugins.MifareClassicPlugin.startNfcListener(
          function() {
            console.log('Ready to read MIFARE Classic cards');
          },
          function(error) {
            console.error('Failed to start NFC listener:', error);
          }
        );
      } else {
        console.log('NFC not available on this device');
      }
    },
    function(error) {
      console.error('Error checking NFC availability:', error);
    }
  );
});

// Read sector when needed
function readCardSector(sectorNumber) {
  cordova.plugins.MifareClassicPlugin.readMifareClassicSector(
    sectorNumber,
    function(data) {
      console.log('Sector ' + sectorNumber + ' data:', data);
      // Process the ASCII data
    },
    function(error) {
      console.error('Failed to read sector:', error);
    }
  );
}
```

### Ionic Angular Implementation

```typescript
import { Component } from '@angular/core';
import { Platform } from '@ionic/angular';

declare var cordova: any;

@Component({
  selector: 'app-nfc',
  templateUrl: 'nfc.page.html'
})
export class NfcPage {
  constructor(private platform: Platform) {}

  async initializeNfc() {
    await this.platform.ready();
    
    if (cordova?.plugins?.MifareClassicPlugin) {
      // Check NFC status and start listener
      this.checkNfcAndStart();
    }
  }

  checkNfcAndStart() {
    cordova.plugins.MifareClassicPlugin.isNfcAvailable(
      (available: boolean) => {
        if (available) {
          this.startNfcListener();
        }
      },
      (error: string) => console.error(error)
    );
  }

  startNfcListener() {
    cordova.plugins.MifareClassicPlugin.startNfcListener(
      () => console.log('NFC listener started'),
      (error: string) => console.error(error)
    );
  }

  readSector(sectorNumber: number) {
    cordova.plugins.MifareClassicPlugin.readMifareClassicSector(
      sectorNumber,
      (data: string) => {
        console.log('Sector data:', data);
        // Handle the data
      },
      (error: string) => console.error(error)
    );
  }
}
```

## Error Handling

The plugin provides comprehensive error handling for common scenarios:

- **NFC not available**: Device doesn't support NFC
- **NFC not enabled**: NFC is disabled in device settings
- **Invalid sector number**: Sector number out of range (0-39)
- **Authentication failed**: Cannot authenticate with card using default keys
- **Read timeout**: Card removed during reading
- **Connection error**: Failed to connect to card

## Testing

The plugin includes a comprehensive test suite:

```bash
# Run the test HTML page
open tests/test-plugin.html

# Create Ionic test app
cd tests
npm run create-ionic-test
npm run install-plugin
```

## Troubleshooting

### Android Issues

1. **NFC not working**: Ensure NFC is enabled in device settings
2. **Permission denied**: Check that NFC permissions are granted
3. **Card not detected**: Ensure card is MIFARE Classic compatible

### iOS Issues

1. **Limited functionality**: iOS Core NFC has restrictions on MIFARE Classic
2. **iOS 13+ required**: Plugin requires iOS 13.0 or later
3. **Entitlements**: Ensure NFC entitlements are properly configured

### Common Issues

1. **Plugin not found**: Ensure plugin is properly installed and platforms are added
2. **Device ready**: Always wait for 'deviceready' event before using the plugin
3. **Card compatibility**: Only MIFARE Classic cards are supported

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

MIT License - see LICENSE file for details

## MIFARE Classic Card Information

### Supported Card Types
- MIFARE Classic 1K (16 sectors, 0-15)
- MIFARE Classic 4K (40 sectors, 0-39)

### Sector Structure
- Each sector contains multiple blocks (usually 4 blocks per sector)
- Last block in each sector is the sector trailer (contains keys)
- Data blocks contain the actual information
- Plugin automatically handles sector authentication and block reading

### Default Keys
The plugin uses MIFARE Classic default keys:
- Key A: `FF FF FF FF FF FF`
- Key B: `FF FF FF FF FF FF`

For cards with custom keys, you'll need to modify the authentication logic in the native code.

## Build Instructions

### Building for Android

```bash
# Ensure Android SDK is installed and configured
ionic cordova build android

# For release build
ionic cordova build android --release
```

### Building for iOS

```bash
# Ensure Xcode is installed
ionic cordova build ios

# Open in Xcode for further configuration
open platforms/ios/YourApp.xcworkspace
```

## Support

For issues and questions:
- Create an issue on GitHub
- Check the troubleshooting section
- Review the test examples
- Consult the MIFARE Classic documentation
