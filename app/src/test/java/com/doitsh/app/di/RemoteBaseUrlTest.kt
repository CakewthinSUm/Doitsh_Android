package com.doitsh.app.di

import com.doitsh.app.data.settings.DataMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Regression test for the crash where SELF_HOSTED mode with no server URL selected the
 * remote repositories while DoitshApiClient was never configured, so the first
 * create/update/delete threw from DoitshApiClient.getClient().
 */
class RemoteBaseUrlTest {

    @Test
    fun localModeNeverSelectsRemote() {
        assertNull(remoteBaseUrl(DataMode.LOCAL, null))
        assertNull(remoteBaseUrl(DataMode.LOCAL, ""))
        assertNull(remoteBaseUrl(DataMode.LOCAL, "http://192.168.1.100:8080"))
    }

    @Test
    fun selfHostedWithoutUrlFallsBackToLocal() {
        assertNull(remoteBaseUrl(DataMode.SELF_HOSTED, null))
        assertNull(remoteBaseUrl(DataMode.SELF_HOSTED, ""))
        assertNull(remoteBaseUrl(DataMode.SELF_HOSTED, "   "))
    }

    @Test
    fun selfHostedWithUrlSelectsRemote() {
        assertEquals(
            "http://192.168.1.100:8080",
            remoteBaseUrl(DataMode.SELF_HOSTED, "http://192.168.1.100:8080")
        )
    }
}
