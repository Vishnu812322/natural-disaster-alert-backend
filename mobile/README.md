# Mobile Phase

The Android client is under mobile/android.

For real FCM delivery:
1. Create/configure a Firebase project.
2. Add the Android application using the same package ID.
3. Put the Firebase configuration file in the Android app as required by Firebase.
4. Add the Firebase Gradle plugin/dependency.
5. Implement FirebaseMessagingService.
6. Register the device token with POST /api/devices.
7. Authenticate the user and associate the device with the account.
8. Implement production emergency notification channels and sound resources.
9. Test only with development/test alerts until the entire authorization and source pipeline is approved.

The included client is intentionally safe: its local button says TEST and does not claim to deliver official alerts.
