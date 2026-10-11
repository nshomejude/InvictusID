package com.opesware.childtrace.shared.engine

import com.opesware.childtrace.shared.model.AgeBand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ProvisioningTest {

    @ParameterizedTest
    @CsvSource(
        "5, RESTRICT",
        "8, RESTRICT",
        "9, FLAG_HIGH",
        "12, FLAG_HIGH",
        "13, FLAG_NARROWED",
        "15, FLAG_NARROWED",
    )
    fun `supported ages resolve to the right band`(age: Int, band: AgeBand) {
        val result = Provisioning.validateChildAge(age)
        val allowed = assertInstanceOf(Provisioning.Result.Allowed::class.java, result)
        assertEquals(band, allowed.band)
    }

    @ParameterizedTest
    @CsvSource("16", "17", "18", "25")
    fun `age ceiling refuses 16 and older`(age: Int) {
        assertInstanceOf(Provisioning.Result.Refused::class.java, Provisioning.validateChildAge(age))
    }

    @ParameterizedTest
    @CsvSource("0", "3", "4")
    fun `under-five is refused as out of range`(age: Int) {
        assertInstanceOf(Provisioning.Result.Refused::class.java, Provisioning.validateChildAge(age))
    }

    @Test
    fun `fromAge returns null at the ceiling`() {
        assertEquals(null, AgeBand.fromAge(16))
    }
}
