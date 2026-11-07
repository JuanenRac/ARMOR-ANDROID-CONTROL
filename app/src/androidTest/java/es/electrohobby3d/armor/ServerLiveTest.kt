package es.electrohobby3d.armor

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.electrohobby3d.armor.network.ArmorApiClient
import es.electrohobby3d.armor.network.ArmorApiException
import es.electrohobby3d.armor.network.SignIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The app's own client (the one the screens use) against a REAL ARMOR-SERVER started on the runner of the workflow `android-emulator.yml`: the emulator reaches the host on
 * 10.0.2.2. The address comes from the instrumentation argument `armorServer`; without it every test here is skipped, so a run that has no server never fails for lack of one.
 * The administrator is the one the workflow gives the server. Nothing here touches a real installation, a phone or a camera.
 */
@RunWith(AndroidJUnit4::class)
class ServerLiveTest {
    private val origin: String? = InstrumentationRegistry.getArguments().getString("armorServer")
    private val client = ArmorApiClient()

    private fun server(): String {
        assumeTrue("no armorServer argument: this run has no server", !origin.isNullOrBlank())
        return origin!!
    }

    @Test
    fun aWrongPasswordIsRefusedByTheRealServer() {
        val address = server()
        try {
            client.signIn(address, "admin", "not-the-password")
            fail("the server accepted a wrong password")
        } catch (error: ArmorApiException) {
            assertTrue("the refusal carries a status", error.code == null || error.code!! in 400..429)
        }
    }

    @Test
    fun theAdministratorSignsInAndReadsTheStateOfTheSystem() {
        val address = server()
        val result = client.signIn(address, "admin", "correct-horse-battery")
        assertTrue("a plain password sign-in is complete", result is SignIn.Done)
        val snapshot = client.status(address)
        assertNotNull(snapshot)
        assertEquals("a fresh server is disarmed", "disarmed", snapshot.mode)
        assertEquals("the administrator is read back as one", "admin", client.studioRole(address))
        assertTrue("the session is active", client.studioSessionActive(address))
    }

    @Test
    fun theScreensThatListThingsGetEmptyListsFromAFreshServerNotErrors() {
        val address = server()
        client.signIn(address, "admin", "correct-horse-battery")
        val (active, history) = client.alarms(address)
        assertTrue(active.isEmpty())
        assertTrue(history.isEmpty())
        assertNotNull(client.preflight(address))
    }
}
