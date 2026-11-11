package com.hasanege.materialtv.panel

/**
 * Category ordering/hiding controlled from the panel.
 *
 * Patterns are case-insensitive. "UK" matches any name containing "uk";
 * "^UK" matches names that start with "uk" (so "UKRAINE" isn't caught by "^UK |").
 *
 * Final order: Top patterns (in the order listed) → everything else → Bottom patterns (in order).
 * Within each group the provider's own order (or A–Z, if set in the panel) is kept.
 */
object CategoryRules {
    private const val MIDDLE = 50_000
    private const val BOTTOM = 100_000

    private val cfg get() = PanelClient.config.value

    private fun matches(name: String, pattern: String): Boolean {
        val p = pattern.trim().lowercase()
        if (p.isEmpty()) return false
        return if (p.startsWith("^")) name.startsWith(p.substring(1)) else name.contains(p)
    }

    private fun indexIn(name: String, patterns: List<String>): Int =
        patterns.indexOfFirst { matches(name, it) }

    fun isHidden(rawName: String?): Boolean {
        val c = cfg
        val name = rawName?.lowercase() ?: return false
        if (indexIn(name, c.catHide) >= 0) return true
        if (c.catHideUnlisted && indexIn(name, c.catTop) < 0 && indexIn(name, c.catBottom) < 0) return true
        return false
    }

    fun rank(rawName: String?): Int {
        val c = cfg
        val name = rawName?.lowercase() ?: return MIDDLE
        indexIn(name, c.catTop).let { if (it >= 0) return it }
        indexIn(name, c.catBottom).let { if (it >= 0) return BOTTOM + it }
        return MIDDLE
    }

    /** True when the panel wants A–Z inside each group instead of the provider's order. */
    val alphabetical: Boolean get() = cfg.catSortAz

    /** Tie-break used when a device has no custom order of its own. */
    fun fallbackCompare(aName: String?, bName: String?, aProviderIndex: Int?, bProviderIndex: Int?): Int {
        if (alphabetical) return (aName ?: "").compareTo(bName ?: "", ignoreCase = true)
        val ai = aProviderIndex ?: Int.MAX_VALUE
        val bi = bProviderIndex ?: Int.MAX_VALUE
        return if (ai != bi) ai.compareTo(bi) else (aName ?: "").compareTo(bName ?: "", ignoreCase = true)
    }
}
