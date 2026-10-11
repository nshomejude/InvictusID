package com.opesware.childtrace.shared.engine

import com.opesware.childtrace.shared.model.AgeBand

/**
 * The software half of the provisioning gate. The app cannot verify guardianship
 * or the buyer's age — those are human checks backed by the signed certificate —
 * but it *can* and must enforce the hard age ceiling and the age→band mapping.
 *
 * This is the single programmatic guard that keeps the product provably clear of
 * the 16–17 grey zone and the 18+ illegal zone.
 */
object Provisioning {

    sealed interface Result {
        /** Provisioning allowed; the child falls into [band]. */
        data class Allowed(val band: AgeBand) : Result

        /** Provisioning refused, with a human-readable [reason]. */
        data class Refused(val reason: String) : Result
    }

    /**
     * Validates a child's age at provisioning. 16+ is refused by design; under 5
     * has no product. On success the resolved [AgeBand] is returned so the caller
     * can bind it to the consent record.
     */
    fun validateChildAge(age: Int): Result {
        if (age >= AgeBand.AGE_CEILING) {
            return Result.Refused(
                "Age $age is at or above the hard ceiling of ${AgeBand.AGE_CEILING}. " +
                    "ChildTrace does not provision profiles for anyone 16 or older."
            )
        }
        val band = AgeBand.fromAge(age)
            ?: return Result.Refused("Age $age is outside the supported 5–15 range.")
        return Result.Allowed(band)
    }
}
