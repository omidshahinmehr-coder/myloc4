package ir.example.locationsms

/**
 * Simple in-memory lock flag. It resets to false whenever the process restarts
 * (device reboot, app killed, etc.) and is also reset when MainActivity goes
 * to the background, so re-opening the app from Recents asks for the
 * password again.
 */
object AppLockState {
    @Volatile
    var unlocked: Boolean = false
}
