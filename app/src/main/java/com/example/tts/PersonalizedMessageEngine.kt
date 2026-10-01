package com.example.tts

object PersonalizedMessageEngine {

    val CATEGORIES = listOf(
        "Breakfast",
        "Lunch",
        "Dinner",
        "Snacks",
        "Tea",
        "Water",
        "Medicine",
        "Exercise",
        "Study",
        "Work",
        "Sleep",
        "Wake Up",
        "Custom"
    )

    val categories = CATEGORIES

    /**
     * Generates a voice message based on person's name, category, language and optional custom message.
     */
    fun generateMessage(
        personName: String,
        category: String,
        customMessage: String,
        language: String
    ): String {
        // If the user provided a custom message, respect and use it!
        if (customMessage.isNotBlank()) {
            return customMessage.trim()
        }

        val name = if (personName.isBlank()) "Dost" else personName.trim()
        val isHindi = language.lowercase().startsWith("hi") || language == "default"

        return if (isHindi) {
            when (category.lowercase()) {
                "breakfast" -> "$name, aapka breakfast time ho gaya hai. Kripya breakfast kar lijiye."
                "lunch" -> "$name, aapka lunch time ho gaya hai. Kripya lunch kar lijiye."
                "dinner" -> "$name, aapka dinner time ho gaya hai. Kripya dinner kar lijiye."
                "snacks" -> "$name, snacks time ho gaya hai. Kripya snacks le lijiye."
                "tea" -> "$name, chai ka time ho gaya hai. Kripya chai pi lijiye."
                "water" -> "$name, paani peene ka time ho gaya hai. Kripya paani pi lijiye."
                "medicine" -> "$name, dawai lene ka time ho gaya hai. Kripya apni medicine le lijiye."
                "exercise" -> "$name, exercise ka time ho gaya hai. Kripya apni exercise shuru kijiye."
                "study" -> "$name, padhai ka time ho gaya hai. Kripya padhai shuru kijiye."
                "work" -> "$name, kaam ka time ho gaya hai. Kripya work start kijiye."
                "sleep" -> "$name, sone ka time ho gaya hai. Kripya ab rest kijiye."
                "wake up" -> "$name, uthne ka time ho gaya hai. Good morning!"
                else -> "$name, aapka $category reminder time ho gaya hai."
            }
        } else {
            when (category.lowercase()) {
                "breakfast" -> "$name, it is breakfast time. Please have your breakfast."
                "lunch" -> "$name, it is lunch time. Please have your lunch."
                "dinner" -> "$name, it is dinner time. Please have your dinner."
                "snacks" -> "$name, it is snack time. Enjoy your snacks."
                "tea" -> "$name, it is tea time. Have a nice cup of tea."
                "water" -> "$name, it is time to drink water. Please stay hydrated."
                "medicine" -> "$name, it is medicine time. Please take your medicine on time."
                "exercise" -> "$name, it is exercise time. Let's start your workout."
                "study" -> "$name, study time has arrived. Please focus on your studies."
                "work" -> "$name, work time has arrived. Let's begin."
                "sleep" -> "$name, it is bedtime. Please get some rest. Good night."
                "wake up" -> "$name, time to wake up. Good morning and have a wonderful day!"
                else -> "$name, this is your $category reminder."
            }
        }
    }
}
