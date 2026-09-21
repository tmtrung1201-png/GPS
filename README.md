# Fake GPS

Android Mock Location Provider for testing.

## Build with GitHub Actions

1. Push this project to a GitHub repository.
2. Open **Actions**.
3. Run **Build APK** (or push to `main`).
4. After a successful run, download artifact **FakeGPS-debug-apk**.
5. Extract `app-debug.apk` and install it on an Android test device.

## Enable mock location

On the Android device:

1. Enable Developer options.
2. Open **Select mock location app**.
3. Choose **Fake GPS**.
4. Open Fake GPS, select a point on the map or enter coordinates.
5. Press **Bắt đầu Mock**.

This app is intended for testing and development. Android requires the user to explicitly select a mock-location app in Developer options.
