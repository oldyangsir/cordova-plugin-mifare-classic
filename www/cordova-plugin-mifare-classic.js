/**
 * MifareClassicPlugin JavaScript Interface
 * Provides NFC MIFARE Classic card reading functionality for Ionic/Cordova applications
 */

var exec = require('cordova/exec');

/**
 * MifareClassicPlugin constructor
 */
function MifareClassicPlugin() {}

/**
 * Read a specified sector from a MIFARE Classic card and return ASCII data
 * @param {number} sectorNumber - The sector number to read (0-based, max 39 for MIFARE Classic 4K)
 * @param {function} successCallback - Success callback function that receives ASCII data
 * @param {function} errorCallback - Error callback function that receives error message
 */
MifareClassicPlugin.prototype.readMifareClassicSector = function(sectorNumber, successCallback, errorCallback) {
    // Input validation
    if (typeof sectorNumber !== 'number' || isNaN(sectorNumber) || sectorNumber < 0 || sectorNumber > 39) {
        var error = 'Invalid sector number. Must be a number between 0 and 39.';
        if (typeof errorCallback === 'function') {
            setTimeout(function() { errorCallback(error); }, 0);
        } else {
            throw new Error(error);
        }
        return;
    }

    if (typeof successCallback !== 'function') {
        throw new Error('Success callback must be a function');
    }

    if (typeof errorCallback !== 'function') {
        throw new Error('Error callback must be a function');
    }

    // Execute native plugin method with error wrapping
    exec(
        function(result) {
            try {
                successCallback(result);
            } catch (e) {
                console.error('Error in success callback:', e);
            }
        },
        function(error) {
            try {
                errorCallback(error);
            } catch (e) {
                console.error('Error in error callback:', e);
            }
        },
        'MifareClassicPlugin',
        'readMifareClassicSector',
        [sectorNumber]
    );
};

/**
 * Check if NFC is available on the device
 * @param {function} successCallback - Success callback function that receives boolean result
 * @param {function} errorCallback - Error callback function that receives error message
 */
MifareClassicPlugin.prototype.isNfcAvailable = function(successCallback, errorCallback) {
    if (typeof successCallback !== 'function') {
        throw new Error('Success callback must be a function');
    }

    if (typeof errorCallback !== 'function') {
        throw new Error('Error callback must be a function');
    }

    exec(
        successCallback,
        errorCallback,
        'MifareClassicPlugin',
        'isNfcAvailable',
        []
    );
};

/**
 * Check if NFC is enabled on the device
 * @param {function} successCallback - Success callback function that receives boolean result
 * @param {function} errorCallback - Error callback function that receives error message
 */
MifareClassicPlugin.prototype.isNfcEnabled = function(successCallback, errorCallback) {
    if (typeof successCallback !== 'function') {
        throw new Error('Success callback must be a function');
    }

    if (typeof errorCallback !== 'function') {
        throw new Error('Error callback must be a function');
    }

    exec(
        successCallback,
        errorCallback,
        'MifareClassicPlugin',
        'isNfcEnabled',
        []
    );
};

/**
 * Start NFC listening for MIFARE Classic cards
 * @param {function} successCallback - Success callback function
 * @param {function} errorCallback - Error callback function that receives error message
 */
MifareClassicPlugin.prototype.startNfcListener = function(successCallback, errorCallback) {
    if (typeof successCallback !== 'function') {
        throw new Error('Success callback must be a function');
    }

    if (typeof errorCallback !== 'function') {
        throw new Error('Error callback must be a function');
    }

    exec(
        successCallback,
        errorCallback,
        'MifareClassicPlugin',
        'startNfcListener',
        []
    );
};

/**
 * Stop NFC listening
 * @param {function} successCallback - Success callback function
 * @param {function} errorCallback - Error callback function that receives error message
 */
MifareClassicPlugin.prototype.stopNfcListener = function(successCallback, errorCallback) {
    if (typeof successCallback !== 'function') {
        throw new Error('Success callback must be a function');
    }

    if (typeof errorCallback !== 'function') {
        throw new Error('Error callback must be a function');
    }

    exec(
        successCallback,
        errorCallback,
        'MifareClassicPlugin',
        'stopNfcListener',
        []
    );
};

// Create and export the plugin instance
var mifareClassicPlugin = new MifareClassicPlugin();
module.exports = mifareClassicPlugin;
