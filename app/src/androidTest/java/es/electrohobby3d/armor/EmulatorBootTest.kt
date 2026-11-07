package es.electrohobby3d.armor

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The smallest instrumented test: it shows that the emulator of the workflow `android-emulator.yml` starts, installs this app and runs code inside it. It touches no server, no network and
 * no device of the installation. The tests of the screens against a simulated server are added to this folder one at a time, with this one as the proof that the emulator itself works.
 */
@RunWith(AndroidJUnit4::class)
class EmulatorBootTest {
    @Test
    fun theAppRunsInsideTheEmulatorWithItsOwnIdentity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("es.electrohobby3d.armor", context.packageName)
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        assertTrue("the app has a version", !info.versionName.isNullOrEmpty())
        assertTrue("the emulator is the API level the workflow asks for or newer", Build.VERSION.SDK_INT >= 34)
    }
}
