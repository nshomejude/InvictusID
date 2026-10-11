package com.opesware.childtrace.shared.engine

import com.opesware.childtrace.shared.model.AgeBand

/**
 * The software half of the provisioning gate. The app cannot verify guardianship
 * or the buyer's age — those are human checks backed by the signed certificate —
 * but it *can* and must enforce the age rules and the age→band mapping.
 *
 * By default the supported range is 5–15 and anyone 16+ is refused. The 16–17
 * band is opened ONLY by [ProvisioningConfig.allowSeniorTeen], which stands in
 * for Cameroon counsel sign-off (CLAUDE.md invariant 5/7). 18+ is always
 * refused, regardless of configuration.
 */
object Provisioning {

    /** Provisioning configuration. [allowSeniorTeen] gates the 16–17 band. */
    data class Config(val allowSeniorTeen: Boolean = false)

    sealed interface Result {
        /** Provisioning allowed; the child falls into [band]. */
        data class Allowed(val band: AgeBand) : Result

        /** Provisioning refused, with a human-readable [reason]. */
        data class Refused(val reason: String) : Result
    }

    /**
     * Validates a child's age at provisioning. 18+ is always refused; 16–17 is
     * refused unless [config] enables the senior-teen band; under 5 has no
     * product. On success the resolved [AgeBand] is returned to bind to consent.
     */
    fun validateChildAge(age: Int, config: Config = Config()): Result {
        if (age >= AgeBand.ADULT_AGE) {
            return Result.Refused(
                "Age $age is an adult (18+). ChildTrace never provisions profiles for adults."
            )
        }
        if (age >= AgeBand.DEFAULT_CEILING && !config.allowSeniorTeen) {
            return Result.Refused(
                "Age $age (16–17) is refused by default. This band requires Cameroon counsel " +
                    "sign-off and must be explicitly enabled before it can be provisioned."
            )
        }
        val band = AgeBand.fromAge(age, allowSeniorTeen = config.allowSeniorTeen)
            ?: return Result.Refused("Age $age is outside the supported range.")
        return Result.Allowed(band)
    }
}
