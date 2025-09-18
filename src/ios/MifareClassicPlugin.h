//
//  MifareClassicPlugin.h
//  MifareClassicPlugin
//
//  Created by Cordova Plugin Generator
//

#import <Cordova/CDVPlugin.h>
#import <CoreNFC/CoreNFC.h>

@interface MifareClassicPlugin : CDVPlugin <NFCTagReaderSessionDelegate>

// Plugin methods
- (void)readMifareClassicSector:(CDVInvokedUrlCommand*)command;
- (void)isNfcAvailable:(CDVInvokedUrlCommand*)command;
- (void)isNfcEnabled:(CDVInvokedUrlCommand*)command;
- (void)startNfcListener:(CDVInvokedUrlCommand*)command;
- (void)stopNfcListener:(CDVInvokedUrlCommand*)command;

// NFC Session management
@property (nonatomic, strong) NFCTagReaderSession *nfcSession;
@property (nonatomic, strong) NSString *currentCallbackId;
@property (nonatomic, assign) NSInteger requestedSector;

@end
