package org.spendoo.statistics.model

import java.util.Locale

enum class Language(val locale: Locale) {
    EN(Locale.ENGLISH),
    AR(Locale.forLanguageTag("ar"))
}
