package com.cpen321.usermanagement.ui.screens.profile

import com.cpen321.usermanagement.ui.components.ExperienceLevel

data class ProfileFormState(
    val name: String = "",
    val email: String = "",
    val ageText: String = "",
    val cityQuery: String = "",
    val selectedCity: String? = null,
    val selectedCityPlaceId: String? = null,
    val experience: ExperienceLevel? = null,
    val bioText: String = "",
    val showErrors: Boolean = false,
    val originalName: String = "",
    val originalAge: Int? = null,
    val originalCityDisplay: String? = null,
    val originalExperience: ExperienceLevel? = null,
    val originalBio: String = ""
) {
    private fun String?.norm() = this?.trim().orEmpty()
    private fun equalsNorm(a: String?, b: String?) = a.norm() == b.norm()

    private fun isNameValid() = name.isNotBlank()

    private fun isAgeValid(): Boolean {
        if (ageText.isBlank()) return true
        val a = ageText.toIntOrNull() ?: return false
        return a in 13..120
    }

    private fun isEditingCity(): Boolean =
        cityQuery.isNotBlank() || !equalsNorm(selectedCity, originalCityDisplay)

    private fun isCityValid(): Boolean =
        if (!isEditingCity()) true else !selectedCityPlaceId.isNullOrBlank()

    private fun isExpValid() = true

    private fun isBioValid() = bioText.length <= 500

    fun canSave(): Boolean =
        isNameValid() && isAgeValid() && isCityValid() && isExpValid() && isBioValid()

    val ageOrNull: Int? get() = ageText.toIntOrNull()

    val nameError: String? get() = if (!showErrors) null else if (!isNameValid()) "Required" else null
    val ageError: String?  get() = if (!showErrors) null else if (!isAgeValid()) "Enter a valid age (13–120)" else null
    val cityError: String?
        get() = if (!showErrors) null
        else if (isEditingCity() && selectedCityPlaceId.isNullOrBlank()) "Pick a city from suggestions" else null
    val expError: String?  get() = null
    val bioError: String?  get() = if (!showErrors) null else if (!isBioValid()) "Max 500 characters" else null

    fun hasChanges(): Boolean {
        val nameChanged = !equalsNorm(name, originalName)
        val ageChanged  = ageOrNull != originalAge
        val bioChanged  = !equalsNorm(bioText, originalBio)
        val expChanged  = experience != originalExperience
        val cityChanged = isEditingCity()
        return nameChanged || ageChanged || bioChanged || expChanged || cityChanged
    }
}