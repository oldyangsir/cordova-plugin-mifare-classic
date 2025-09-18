package com.stayplease.mifareclassic;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;

/**
 * MifareClassicPlugin for Cordova
 * Provides NFC MIFARE Classic card reading functionality
 */
public class MifareClassicPlugin extends CordovaPlugin {

    private static final String TAG = "MifareClassicPlugin";

    // Action constants
    private static final String ACTION_READ_SECTOR = "readMifareClassicSector";
    private static final String ACTION_IS_NFC_AVAILABLE = "isNfcAvailable";
    private static final String ACTION_IS_NFC_ENABLED = "isNfcEnabled";
    private static final String ACTION_START_LISTENER = "startNfcListener";
    private static final String ACTION_STOP_LISTENER = "stopNfcListener";

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] intentFilters;
    private String[][] techLists;
    private CallbackContext readerCallback;
    private boolean isListening = false;
    private Tag currentTag;
    private int targetSectorNumber = -1;

    @Override
    public void pluginInitialize() {
        super.pluginInitialize();
        Log.d(TAG, "MifareClassicPlugin initialized");

        // Initialize NFC adapter
        NfcManager nfcManager = (NfcManager) cordova.getActivity().getSystemService(Context.NFC_SERVICE);
        nfcAdapter = nfcManager.getDefaultAdapter();

        // Setup pending intent for NFC
        Intent intent = new Intent(cordova.getActivity(), cordova.getActivity().getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(cordova.getActivity(), 0, intent, PendingIntent.FLAG_MUTABLE);

        // Setup intent filters for MIFARE Classic
        IntentFilter mifareFilter = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        intentFilters = new IntentFilter[]{mifareFilter};

        // Setup tech lists for MIFARE Classic
        techLists = new String[][]{new String[]{MifareClassic.class.getName()}};
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "Executing action: " + action);

        try {
            switch (action) {
                case ACTION_READ_SECTOR:
                    Log.d(TAG, "ACTION_READ_SECTOR called with args: " + args.toString());
                    if (args.length() < 1) {
                        Log.e(TAG, "Missing sector number parameter");
                        callbackContext.error("Missing sector number parameter");
                        return false;
                    }
                    int sectorNumber = args.getInt(0);
                    Log.d(TAG, "Parsed sector number: " + sectorNumber);
                    if (sectorNumber < 0 || sectorNumber > 39) { // MIFARE Classic 4K has max 40 sectors (0-39)
                        Log.e(TAG, "Invalid sector number: " + sectorNumber);
                        callbackContext.error("Invalid sector number. Must be between 0 and 39.");
                        return false;
                    }
                    Log.d(TAG, "Calling readMifareClassicSector with sector: " + sectorNumber);
                    readMifareClassicSector(sectorNumber, callbackContext);
                    return true;

                case ACTION_IS_NFC_AVAILABLE:
                    isNfcAvailable(callbackContext);
                    return true;

                case ACTION_IS_NFC_ENABLED:
                    isNfcEnabled(callbackContext);
                    return true;

                case ACTION_START_LISTENER:
                    startNfcListener(callbackContext);
                    return true;

                case ACTION_STOP_LISTENER:
                    stopNfcListener(callbackContext);
                    return true;

                default:
                    Log.e(TAG, "Unknown action: " + action);
                    callbackContext.error("Unknown action: " + action);
                    return false;
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing error: " + e.getMessage());
            callbackContext.error("Invalid parameters: " + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error: " + e.getMessage());
            callbackContext.error("Unexpected error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if NFC is available on the device
     */
    private void isNfcAvailable(CallbackContext callbackContext) {
        boolean available = nfcAdapter != null;
        Log.d(TAG, "NFC available: " + available);
        callbackContext.success(available ? 1 : 0);
    }

    /**
     * Check if NFC is enabled on the device
     */
    private void isNfcEnabled(CallbackContext callbackContext) {
        boolean enabled = nfcAdapter != null && nfcAdapter.isEnabled();
        Log.d(TAG, "NFC enabled: " + enabled);
        callbackContext.success(enabled ? 1 : 0);
    }

    /**
     * Start NFC listener for MIFARE Classic cards
     */
    private void startNfcListener(CallbackContext callbackContext) {
        if (nfcAdapter == null) {
            callbackContext.error("NFC not available on this device");
            return;
        }

        if (!nfcAdapter.isEnabled()) {
            callbackContext.error("NFC is not enabled");
            return;
        }

        readerCallback = callbackContext;
        isListening = true;

        // Enable foreground dispatch
      try {
        nfcAdapter.enableForegroundDispatch(
          cordova.getActivity(),
          pendingIntent,
          intentFilters,
          techLists
        );
        Log.d(TAG, "NFC foreground dispatch enabled successfully");
        Log.d(TAG, "Activity: " + cordova.getActivity());
        Log.d(TAG, "PendingIntent: " + pendingIntent);
      } catch (Exception e) {
        Log.e(TAG, "Failed to enable foreground dispatch: " + e.getMessage());
      }

        // Keep callback for future use
        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
        // 发送成功状态，表示监听已启动
        //callbackContext.success("NFC listener started successfully");
    }

    /**
     * Stop NFC listener
     */
    private void stopNfcListener(CallbackContext callbackContext) {
        if (nfcAdapter != null && isListening) {
            nfcAdapter.disableForegroundDispatch(cordova.getActivity());
            isListening = false;
            readerCallback = null;
            Log.d(TAG, "NFC listener stopped");
        }
        callbackContext.success("NFC listener stopped");
    }

    /**
     * Read MIFARE Classic sector and return ASCII data
     */
    private void readMifareClassicSector(int sectorNumber, CallbackContext callbackContext) {
        Log.d(TAG, "Preparing to read sector: " + sectorNumber);

        // Check if NFC listener is active
        if (!isListening) {
            Log.e(TAG, "NFC listener is not active");
            callbackContext.error("NFC listener is not active. Please start NFC listener first.");
            return;
        }

        // Store the callback and sector number for when a tag is detected
        this.readerCallback = callbackContext;
        this.targetSectorNumber = sectorNumber;

        // Clear any previously stored tag to force fresh detection
        this.currentTag = null;

        Log.d(TAG, "Waiting for NFC tag detection to read sector " + sectorNumber);

        // Start a timeout mechanism to avoid infinite waiting
//        cordova.getThreadPool().execute(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    // Wait for 30 seconds for tag detection
//                    Thread.sleep(30000);
//
//                    // If we still have the same callback after 30 seconds, it means no tag was detected
//                    if (readerCallback == callbackContext && targetSectorNumber == sectorNumber) {
//                        Log.w(TAG, "Timeout waiting for NFC tag detection for sector " + sectorNumber);
//                        callbackContext.error("Timeout: No NFC tag detected within 30 seconds. Please bring a MIFARE Classic card closer to the device.");
//
//                        // Clear the callback to prevent duplicate responses
//                        readerCallback = null;
//                        targetSectorNumber = -1;
//                    }
//                } catch (InterruptedException e) {
//                    Log.d(TAG, "Timeout thread interrupted");
//                }
//            }
//        });

        Log.d(TAG, "Timeout mechanism started for sector " + sectorNumber);
    }

    /**
     * Handle new NFC intent (called when NFC tag is detected)
     */
    @Override
    public void onNewIntent(Intent intent) {
        Log.d(TAG, "New NFC intent received");

        if (readerCallback == null || !isListening) {
            Log.d(TAG, "No active reader callback or not listening");
            return;
        }

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            Log.d(TAG, "NFC tag detected");
            handleNfcTag(tag);
        }
    }

    @Override
    public void onResume(boolean multitasking) {
      super.onResume(multitasking);
      Log.d(TAG, "onResume called, multitasking: " + multitasking);

      if (isListening && nfcAdapter != null) {
        Log.d(TAG, "Re-enabling NFC foreground dispatch");
        try {
          nfcAdapter.enableForegroundDispatch(
            cordova.getActivity(),
            pendingIntent,
            intentFilters,
            techLists
          );
        } catch (Exception e) {
          Log.e(TAG, "Failed to re-enable foreground dispatch in onResume: " + e.getMessage());
        }
      }
    }

    @Override
    public void onPause(boolean multitasking) {
      super.onPause(multitasking);
      Log.d(TAG, "onPause called, multitasking: " + multitasking);

      if (nfcAdapter != null) {
        Log.d(TAG, "Disabling NFC foreground dispatch");
        try {
          nfcAdapter.disableForegroundDispatch(cordova.getActivity());
        } catch (Exception e) {
          Log.e(TAG, "Failed to disable foreground dispatch in onPause: " + e.getMessage());
        }
      }
    }

    @Override
    public void onDestroy() {
      super.onDestroy();
      Log.d(TAG, "onDestroy called");

      if (nfcAdapter != null && isListening) {
        try {
          nfcAdapter.disableForegroundDispatch(cordova.getActivity());
        } catch (Exception e) {
          Log.e(TAG, "Failed to disable foreground dispatch in onDestroy: " + e.getMessage());
        }
      }

      isListening = false;
      readerCallback = null;
    }

    /**
     * Handle detected NFC tag
     */
    private void handleNfcTag(Tag tag) {
        // Store the current tag for future use
        this.currentTag = tag;

        // Only process if we have an active reader callback waiting for a result
        if (readerCallback == null) {
            Log.d(TAG, "NFC tag detected but no active reader callback");
            return;
        }

        // If we have a target sector number set, read that specific sector
        int sectorToRead = (targetSectorNumber >= 0) ? targetSectorNumber : 0;
        Log.d(TAG, "Reading sector " + sectorToRead + " from detected tag");

        String result = readMifareClassicSectorFromTag(tag, sectorToRead);

        if (result != null) {
            Log.d(TAG, "Successfully read sector " + sectorToRead + ", data length: " + result.length());
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
            pluginResult.setKeepCallback(false); // Complete the callback
            readerCallback.sendPluginResult(pluginResult);

            // Clear the callback and target sector after successful read
            readerCallback = null; // 保持监听
            targetSectorNumber = -1;
        } else {
            String errorMsg = "Failed to read MIFARE Classic sector " + sectorToRead + ". The tag may be out of date or moved away. Please keep the card close and try again.";
            Log.e(TAG, errorMsg);
            PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, errorMsg);
            pluginResult.setKeepCallback(false); // Complete the callback
            readerCallback.sendPluginResult(pluginResult);

            // Clear the callback and target sector after failed read
            readerCallback = null;
            targetSectorNumber = -1;
        }
    }

    /**
     * Read MIFARE Classic specified sector and return ASCII data
     * Based on the provided Java reference implementation
     */
    private String readMifareClassicSectorFromTag(Tag tag, int sectorNumber) {
        MifareClassic mifareClassic = MifareClassic.get(tag);
        if (mifareClassic == null) {
            Log.e(TAG, "Tag is not MIFARE Classic compatible");
            return null;
        }

        try {
            // Check if tag is still connected/valid
            if (!mifareClassic.isConnected()) {
                mifareClassic.connect();
                Log.d(TAG, "Connected to MIFARE Classic card");
            } else {
                Log.d(TAG, "MIFARE Classic card already connected");
            }

            // Verify sector key
            boolean auth = mifareClassic.authenticateSectorWithKeyA(sectorNumber, MifareClassic.KEY_DEFAULT);
            if (!auth) {
                Log.d(TAG, "Key A authentication failed, trying Key B");
                auth = mifareClassic.authenticateSectorWithKeyB(sectorNumber, MifareClassic.KEY_DEFAULT);
            }

            if (auth) {
                Log.d(TAG, "Authentication successful for sector " + sectorNumber);

                int blockCount = mifareClassic.getBlockCountInSector(sectorNumber);
                int firstBlock = mifareClassic.sectorToBlock(sectorNumber);

                StringBuilder asciiData = new StringBuilder();

                // Read all blocks in the sector
                for (int j = 0; j < blockCount; j++) {
                    byte[] data = null;
                    int blockNumber = firstBlock + j;
                    try {
                        // Skip blocks that can't be read
                        data = mifareClassic.readBlock(blockNumber);
                        Log.d(TAG, "Successfully read block " + blockNumber);
                    } catch (Exception e) {
                        Log.w(TAG, "Skip_" + sectorNumber + "_" + j + ": " + e.getMessage());
                    }

                    if (data != null) {
                        // Convert to ASCII and append to result
                        String blockAscii = bytesToAscii(data);
                        asciiData.append(blockAscii);
                    }
                }

                return asciiData.toString();
            } else {
                Log.e(TAG, "Authentication failed for sector " + sectorNumber);
                return null;
            }

        } catch (IOException e) {
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("out of date")) {
                Log.e(TAG, "Tag is out of date - please bring the card closer and try again: " + errorMsg);
            } else {
                Log.e(TAG, "IOException while reading MIFARE Classic: " + errorMsg);
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while reading MIFARE Classic: " + e.getMessage());
            return null;
        } finally {
            try {
                mifareClassic.close();
                Log.d(TAG, "MIFARE Classic connection closed");
            } catch (IOException e) {
                Log.e(TAG, "Error closing MIFARE Classic connection: " + e.getMessage());
            }
        }
    }

    /**
     * Convert byte array to ASCII string
     */
    private String bytesToAscii(byte[] bytes) {
        StringBuilder ascii = new StringBuilder();
        for (byte b : bytes) {
            // Convert byte to ASCII character, handle non-printable characters
            if (b >= 32 && b <= 126) {
                ascii.append((char) b);
            } else {
                ascii.append('.');  // Replace non-printable characters with dot
            }
        }
        return ascii.toString();
    }

}
