package com.yshalsager.suntimes.prayertimesaddon

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.os.ParcelFileDescriptor
import com.yshalsager.suntimes.prayertimesaddon.widget.WidgetPrefs
import java.io.IOException

class PrayerTimesBackupAgent : BackupAgent() {
    override fun onBackup(oldState: ParcelFileDescriptor?, data: BackupDataOutput?, newState: ParcelFileDescriptor?) = Unit

    override fun onRestore(data: BackupDataInput?, appVersionCode: Int, newState: ParcelFileDescriptor?) = Unit

    override fun onFullBackup(data: android.app.backup.FullBackupDataOutput) {
        migrate_alarm_token()
        super.onFullBackup(data)
    }

    private fun migrate_alarm_token() {
        if (!WidgetPrefs.migrate_alarm_token(this)) throw IOException("Unable to exclude widget alarm token from backup")
    }
}
