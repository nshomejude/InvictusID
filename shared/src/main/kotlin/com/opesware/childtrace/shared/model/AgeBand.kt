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
enum class AgeBand(val minAge: Int, val maxAge: Int) {
    /** 5–8 · Restrict. Open messaging mostly blocked; backstop risk only. */
    RESTRICT(5, 8),

    /** 9–12 · Flag (high). Messaging allowed and fully analyzed; broad categories. */
    FLAG_HIGH(9, 12),

    /** 13–15 · Flag (narrowed). Only real danger surfaces; everyday chatter never does. */
    FLAG_NARROWED(13, 15);

    companion object {
        /** Hard age ceiling. No profile is provisioned for anyone this age or older. */
        const val AGE_CEILING = 16

        /**
         * Maps an age to its band, or null if the age is outside the supported
         * 5–15 range. 16+ is refused by design (the stalkerware/illegal grey zone);
         * under 5 has no product.
         */
        fun fromAge(age: Int): AgeBand? = when {
            age >= AGE_CEILING -> null
            else -> entries.firstOrNull { age in it.minAge..it.maxAge }
        }
    }
}
