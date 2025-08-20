package com.dawitf.akahidegn.domain.model

enum class AchievementCategory {
    TRIP,
    SOCIAL,
    SAFETY,
    ENVIRONMENTAL,
    MILESTONE,
    SPECIAL,
    STREAK
}

enum class AchievementRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY
}

enum class Gender {
    MALE,
    FEMALE,
    OTHER,
    PREFER_NOT_TO_SAY
}

enum class VerificationLevel {
    BASIC,
    VERIFIED,
    PREMIUM,
    TRUSTED
}

enum class PaymentMethod {
    CASH,
    MOBILE_MONEY,
    CREDIT_CARD,
    BANK_TRANSFER,
    WALLET
}

enum class RideStatus {
    PENDING,
    ACCEPTED,
    STARTED,
    COMPLETED,
    CANCELLED,
    EXPIRED
}

enum class RideType {
    REGULAR,
    POOL,
    PREMIUM,
    EXPRESS,
    SPECIAL
}
