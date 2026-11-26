package es.electrohobby3d.armor

import android.os.Looper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections

/**
 * The state holders publish from a worker thread (a sign-in answers on Dispatchers.IO) and the screen must still receive and draw it on the main thread, as on a phone. With a test
 * rule that resumes a collector on the thread that publishes, the views can be changed from that thread: Compose captures "Only the original thread that created a view hierarchy
 * can touch its views" and the activity later dies while it is destroyed, with an unrelated error of the slot table.
 */
@RunWith(AndroidJUnit4::class)
class PublishThreadTest {
    @get:Rule val compose = createComposeRule()

    @Test fun aStatePublishedFromAWorkerThreadIsReceivedOnTheMainThread() {
        val published = MutableStateFlow(0)
        val threads = Collections.synchronizedSet(mutableSetOf<Thread>())
        val received = published.onEach { threads.add(Thread.currentThread()) }
        compose.setContent {
            val value by received.collectAsStateWithLifecycle(initialValue = 0)
            // nodes come and go with each value, as the screen after a sign-in does
            Column { Column { if (value % 2 == 0) Text("even $value") else Row { Text("odd $value") } } }
        }
        for (next in 1..20) {
            compose.waitForIdle()
            runBlocking { withContext(Dispatchers.IO) { published.value = next } }
        }
        compose.onNodeWithText("even 20").assertExists()
        assertEquals(setOf(Looper.getMainLooper().thread), threads.toSet())
    }
}
