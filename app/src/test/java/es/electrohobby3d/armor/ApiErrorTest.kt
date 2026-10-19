package es.electrohobby3d.armor

import es.electrohobby3d.armor.network.ApiError
import es.electrohobby3d.armor.network.ArmorApi
import es.electrohobby3d.armor.network.ArmorApiClient
import es.electrohobby3d.armor.network.ArmorApiException
import es.electrohobby3d.armor.network.ElevationRequiredException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiErrorTest {
    @Test fun theCodeOfARefusalIsReadOnceFromTheServerAnswer() {
        val error = ArmorApiException("Server operation failed (HTTP 403): {\"error\":\"wrong pin\",\"code\":\"wrong_pin\"}", 403)
        assertEquals(ApiError(403, "wrong_pin", "wrong pin"), error.apiError)
    }

    @Test fun anAnswerWithoutACodeHasAnEmptyCodeAndKeepsTheStatus() {
        val error = ArmorApiException("Server operation failed (HTTP 500)", 500)
        assertEquals(ApiError(500, "", ""), error.apiError)
        assertEquals(ApiError(null, "", ""), ArmorApiException("no answer").apiError)
    }

    @Test fun anElevationRefusalCarriesItsCode() {
        assertEquals("elevation_required", ElevationRequiredException().apiError.code)
    }

    @Test fun theHttpClientIsOneImplementationOfTheInterfaceThatScreensDependOn() {
        val api: ArmorApi = ArmorApiClient()
        assertTrue(api is ArmorApiClient)
    }
}
