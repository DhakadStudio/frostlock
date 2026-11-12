package com.frostlock.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey
    val id: Long = 1L,
    val goals: String,
    val triggers: String,
    val reason: String
)
