# Work Log

- 2026-04-26 21:36:01 +09:00 - Built the RemoteOn Android WoL project skeleton, added LAN-only security notes, and prepared the repository for GitHub push.
- 2026-04-26 21:56:46 +09:00 - Installed the Android SDK and Gradle in this workspace, then successfully ran `assembleDebug` for the RemoteOn app.
- 2026-04-26 22:18:03 +09:00 - Shortened the Android install APK path by copying the debug build to `app/build/outputs/install/RemoteOn.apk` and updated the build script to match.
- 2026-04-26 22:23:11 +09:00 - Removed the long-lived debug APK and build log artifacts after copying the install APK so only `app/build/outputs/install/RemoteOn.apk` remains.
- 2026-04-26 22:25:21 +09:00 - Removed the leftover `app/build/outputs/apk` folder as part of the build cleanup so the workspace keeps only `app/build/outputs/install/RemoteOn.apk`.
- 2026-04-26 22:37:35 +09:00 - Refreshed the Android UI with a more modern Material layout and verified the updated screen in the emulator.
- 2026-04-26 22:49:45 +09:00 - Added dark/light/system theme selection and automatic broadcast address calculation from IP and subnet mask input.
- 2026-04-26 22:58:39 +09:00 - Reworked the dark theme toward a Samsung One UI style and moved theme switching into a visible toggle on the home screen.
- 2026-04-26 23:07:12 +09:00 - Moved the theme control into a small top-right switch in the app bar and removed the large hero toggle from the home card.
- 2026-04-26 23:15:41 +09:00 - Added small sun and moon indicators beside the top-right theme switch so the light and dark modes are easier to recognize at a glance.
- 2026-04-26 23:24:10 +09:00 - Added MAC address input formatting so hyphens are inserted automatically and the dialog now shows the example format directly.
- 2026-04-26 23:35:52 +09:00 - Created a custom adaptive launcher icon for RemoteOn and linked the app manifest to the new icon assets.
- 2026-04-26 23:39:25 +09:00 - Expanded online status checks to try multiple common Windows ports and ICMP ping so PCs that block RDP no longer look offline too easily.
- 2026-04-26 23:49:48 +09:00 - Made ping results report which probe succeeded or failed so online status now shows a clearer LAN reachability signal instead of a plain offline label.
