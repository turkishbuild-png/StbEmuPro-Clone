package com.mclone.stbemu.core.stb

/**
 * Beschreibung eines emulierten Infomir-MAG-Receivers.
 *
 * Der Originalaufbau (jadx: `p000.chb`, erzeugt ueber `p000.chd.m6930a(...)`)
 * besteht aus genau vier Identifikations-Feldern. Diese Klasse bildet sie 1:1 ab.
 *
 * @param type   Geraete-Familie, z. B. "mag" oder "AuraHD"
 * @param group  Marketing-Gruppe innerhalb der Familie, z. B. "MAG 25x"
 * @param name   Anzeigename, z. B. "MAG 250"
 * @param id     Stabile Kennung fuer Profile/DB, z. B. "mag-250"
 */
data class StbModel(
    val type: String,
    val group: String,
    val name: String,
    val id: String,
) {
    override fun toString(): String = name
}

/**
 * Anmerkung, mit der ein Firmware-Profil als Dagger-/ServiceLocator-Schluessel
 * registriert wird. Ersetzt im Klon das Multibinding aus dem Original
 * (`p000.chc` / `p000.chb`), ohne die Dagger-Abhaengigkeit.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class StbModelAnnotation(
    val type: String,
    val group: String,
    val name: String,
    val id: String,
)
