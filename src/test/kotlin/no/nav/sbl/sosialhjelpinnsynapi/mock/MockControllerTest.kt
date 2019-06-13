package no.nav.sbl.sosialhjelpinnsynapi.mock

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*

@ExtendWith(SpringExtension::class)
@WebMvcTest(MockController::class)
@ActiveProfiles(profiles = ["mock"])
class MockControllerTest(@Autowired val mvc: MockMvc) {

    @MockBean
    lateinit var fiksClientMock: FiksClientMock

    private val nedlastingPath = "/api/v1/mock/nedlasting"
    private val mockUuid = UUID.fromString("00000000-0000-0000-0000-000000000000")

    @Test
    fun getNedlasting_withMockUuid_isNotFound() {
        mvc.perform(get("$nedlastingPath/$mockUuid"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun getNedlasting_withJpegUuid_contentIsJpeg() {
        mvc.perform(get("$nedlastingPath/$JPG_UUID"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.IMAGE_JPEG))
    }

    @Test
    fun getNedlasting_withPdfUuid_contentIsPdf() {
        mvc.perform(get("$nedlastingPath/$PDF_UUID"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
    }

    @Test
    fun getNedlasting_withPngUuid_contentIsPng() {
        mvc.perform(get("$nedlastingPath/$PNG_UUID"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.IMAGE_PNG))
    }
}
