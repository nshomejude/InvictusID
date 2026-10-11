package com.opesware.childtrace.shared.model

/**
 * The three age-tiered monitoring postures from the spec's policy model.
 *
 * The same capture + risk engine runs on every device; the child's age picks
 * a single band that sets the sensitivity dial. Raw visibility goes *down* as
 * the child gets older — restrict the little ones, flag for the tweens, surface
 * only real danger for the teens.
 *
 * There is a hard ceiling at 16: [fromAge] returns null for 16+, and no profile
 * may be provisioned for anyone that age or older.
 */
enum class AgeBand(val minAge: Int, val maxAge: Int, val narrowedSurfacing: Boolean = false) {
    /** 5–8 · Restrict. Open messaging mostly blocked; backstop risk only. */
    RESTRICT(5, 8),

    /** 9–12 · Flag (high). Messaging allowed and fully analyzed; broad categories. */
    FLAG_HIGH(9, 12),

    /** 13–15 · Flag (narrowed). Only real danger surfaces; everyday chatter never does. */
    FLAG_NARROWED(13, 15, narrowedSurfacing = true),

    /**
     * 16–17 · Senior teen. Narrowest posture, same real-danger-only surfacing as
     * 13–15. Provisioned ONLY when the senior-teen gate is explicitly enabled
     * (counsel sign-off); refused by default. 18+ is never provisioned.
     */
    SENIOR_TEEN(16, 17, narrowedSurfacing = true);

    companion object {
        /** Absolute ceiling: no profile is ever provisioned for anyone this age or older. */
        const val ADULT_AGE = 18

        /** The default (counsel-not-required) ceiling: 16+ refused unless the senior-teen gate is on. */
        const val DEFAULT_CEILING = 16

        /**
         * Maps an age to its band. By default the supported range is 5–15 (16+
         * refused). With [allowSeniorTeen] = true (counsel sign-off), 16–17 maps
         * to [SENIOR_TEEN]; 18+ is always refused; under 5 has no product.
         */
        fun fromAge(age: Int, allowSeniorTeen: Boolean = false): AgeBand? = when {
            age >= ADULT_AGE -> null
            age >= DEFAULT_CEILING -> if (allowSeniorTeen) SENIOR_TEEN else null
            else -> entries.firstOrNull { age in it.minAge..it.maxAge && it != SENIOR_TEEN }
        }
    }
}
