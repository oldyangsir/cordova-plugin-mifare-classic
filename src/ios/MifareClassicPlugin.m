//
//  MifareClassicPlugin.m
//  MifareClassicPlugin
//
//  Created by Cordova Plugin Generator
//

#import "MifareClassicPlugin.h"
#import <CoreNFC/CoreNFC.h>

@implementation MifareClassicPlugin

#pragma mark - Plugin Lifecycle

- (void)pluginInitialize {
    [super pluginInitialize];
    NSLog(@"MifareClassicPlugin initialized");
}

#pragma mark - Plugin Methods

/**
 * Check if NFC is available on the device
 */
- (void)isNfcAvailable:(CDVInvokedUrlCommand*)command {
    BOOL available = NO;
    
    if (@available(iOS 13.0, *)) {
        available = [NFCTagReaderSession readingAvailable];
    }
    
    NSLog(@"NFC available: %@", available ? @"YES" : @"NO");
    
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                                  messageAsBool:available];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

/**
 * Check if NFC is enabled (same as available on iOS)
 */
- (void)isNfcEnabled:(CDVInvokedUrlCommand*)command {
    [self isNfcAvailable:command];
}

/**
 * Start NFC listener for MIFARE Classic cards
 */
- (void)startNfcListener:(CDVInvokedUrlCommand*)command {
    if (@available(iOS 13.0, *)) {
        if (![NFCTagReaderSession readingAvailable]) {
            CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                                        messageAsString:@"NFC not available on this device"];
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
            return;
        }
        
        self.currentCallbackId = command.callbackId;
        
        // Create NFC session
        self.nfcSession = [[NFCTagReaderSession alloc] initWithPollingOption:NFCPollingISO14443
                                                                     delegate:self
                                                                        queue:dispatch_get_main_queue()];
        
        self.nfcSession.alertMessage = @"Hold your iPhone near a MIFARE Classic card";
        [self.nfcSession beginSession];
        
        NSLog(@"NFC listener started");
        
        // Keep callback for future use
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
        [result setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        
    } else {
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                                    messageAsString:@"NFC requires iOS 13.0 or later"];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }
}

/**
 * Stop NFC listener
 */
- (void)stopNfcListener:(CDVInvokedUrlCommand*)command {
    if (self.nfcSession) {
        [self.nfcSession invalidateSession];
        self.nfcSession = nil;
        self.currentCallbackId = nil;
        NSLog(@"NFC listener stopped");
    }
    
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                                messageAsString:@"NFC listener stopped"];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

/**
 * Read MIFARE Classic sector and return ASCII data
 */
- (void)readMifareClassicSector:(CDVInvokedUrlCommand*)command {
    NSNumber* sectorNumber = [command.arguments objectAtIndex:0];
    
    if (!sectorNumber || [sectorNumber integerValue] < 0) {
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                                    messageAsString:@"Invalid sector number"];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        return;
    }
    
    self.requestedSector = [sectorNumber integerValue];
    
    NSLog(@"Preparing to read sector: %ld", (long)self.requestedSector);
    
    // Start NFC session for reading
    [self startNfcSessionForReading:command.callbackId];
}

/**
 * Start NFC session specifically for reading a sector
 */
- (void)startNfcSessionForReading:(NSString*)callbackId {
    if (@available(iOS 13.0, *)) {
        if (![NFCTagReaderSession readingAvailable]) {
            CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                                        messageAsString:@"NFC not available on this device"];
            [self.commandDelegate sendPluginResult:result callbackId:callbackId];
            return;
        }
        
        self.currentCallbackId = callbackId;
        
        // Create NFC session
        self.nfcSession = [[NFCTagReaderSession alloc] initWithPollingOption:NFCPollingISO14443
                                                                     delegate:self
                                                                        queue:dispatch_get_main_queue()];
        
        self.nfcSession.alertMessage = [NSString stringWithFormat:@"Hold your iPhone near a MIFARE Classic card to read sector %ld", (long)self.requestedSector];
        [self.nfcSession beginSession];
        
        NSLog(@"NFC session started for reading sector %ld", (long)self.requestedSector);
        
    } else {
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                                    messageAsString:@"NFC requires iOS 13.0 or later"];
        [self.commandDelegate sendPluginResult:result callbackId:callbackId];
    }
}

#pragma mark - NFCTagReaderSessionDelegate

/**
 * Called when NFC session becomes invalid
 */
- (void)tagReaderSession:(NFCTagReaderSession *)session didInvalidateWithError:(NSError *)error API_AVAILABLE(ios(13.0)) {
    NSLog(@"NFC session invalidated with error: %@", error.localizedDescription);
    
    if (self.currentCallbackId) {
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                                    messageAsString:error.localizedDescription];
        [self.commandDelegate sendPluginResult:result callbackId:self.currentCallbackId];
        self.currentCallbackId = nil;
    }
    
    self.nfcSession = nil;
}

/**
 * Called when NFC tags are detected
 */
- (void)tagReaderSession:(NFCTagReaderSession *)session didDetectTags:(NSArray<__kindof id<NFCTag>> *)tags API_AVAILABLE(ios(13.0)) {
    NSLog(@"NFC tags detected: %lu", (unsigned long)tags.count);

    if (tags.count > 0) {
        id<NFCTag> tag = tags.firstObject;

        [session connectToTag:tag completionHandler:^(NSError * _Nullable error) {
            if (error) {
                NSLog(@"Error connecting to tag: %@", error.localizedDescription);
                [session invalidateSessionWithErrorMessage:@"Failed to connect to card"];
                return;
            }

            NSLog(@"Connected to NFC tag");
            [self readMifareClassicData:tag session:session];
        }];
    }
}

#pragma mark - MIFARE Classic Reading

/**
 * Read MIFARE Classic data from the connected tag
 */
- (void)readMifareClassicData:(id<NFCTag>)tag session:(NFCTagReaderSession *)session API_AVAILABLE(ios(13.0)) {
    // Check if tag supports MIFARE Classic (ISO14443 Type A)
    if (tag.type != NFCTagTypeISO14443TypeA) {
        NSLog(@"Tag is not ISO14443 Type A (MIFARE Classic)");
        [session invalidateSessionWithErrorMessage:@"Card is not MIFARE Classic compatible"];
        return;
    }

    id<NFCISO14443TypeATag> iso14443Tag = [tag asNFCISO14443TypeATag];

    // Note: iOS Core NFC has limited MIFARE Classic support
    // Full MIFARE Classic authentication and sector reading requires additional implementation
    // This is a simplified version that demonstrates the structure

    NSLog(@"Attempting to read MIFARE Classic sector %ld", (long)self.requestedSector);

    // For demonstration, we'll try to read some basic data
    // In a full implementation, you would need to implement MIFARE Classic authentication
    [self performBasicMifareRead:iso14443Tag session:session];
}

/**
 * Perform basic MIFARE read operation
 * Note: This is a simplified implementation due to iOS Core NFC limitations
 */
- (void)performBasicMifareRead:(id<NFCISO14443TypeATag>)tag session:(NFCTagReaderSession *)session API_AVAILABLE(ios(13.0)) {
    // iOS Core NFC doesn't provide direct MIFARE Classic sector authentication
    // This would require implementing the MIFARE Classic protocol manually

    // For now, we'll return a message indicating the limitation
    NSString *message = [NSString stringWithFormat:@"iOS Core NFC has limited MIFARE Classic support. Detected card but cannot perform full sector authentication for sector %ld", (long)self.requestedSector];

    NSLog(@"%@", message);

    [session invalidateSessionWithErrorMessage:@"MIFARE Classic reading completed"];

    if (self.currentCallbackId) {
        // In a real implementation, this would return the actual ASCII data
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                                    messageAsString:message];
        [self.commandDelegate sendPluginResult:result callbackId:self.currentCallbackId];
        self.currentCallbackId = nil;
    }
}

/**
 * Convert NSData to ASCII string
 */
- (NSString *)dataToAsciiString:(NSData *)data {
    NSMutableString *asciiString = [[NSMutableString alloc] init];
    const unsigned char *bytes = [data bytes];

    for (NSUInteger i = 0; i < [data length]; i++) {
        unsigned char byte = bytes[i];
        // Convert byte to ASCII character, handle non-printable characters
        if (byte >= 32 && byte <= 126) {
            [asciiString appendFormat:@"%c", byte];
        } else {
            [asciiString appendString:@"."];  // Replace non-printable characters with dot
        }
    }

    return [asciiString copy];
}

@end
