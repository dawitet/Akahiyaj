package com.dawitf.akahidegn.data.repository.exceptions

/**
 * Exception thrown when user is not authorized to access profile data
 */
class Unauthorized(message: String = "User is not authorized") : Exception(message)

/**
 * Exception thrown for unknown or unexpected errors
 */
class Unknown(message: String = "An unknown error occurred") : Exception(message)
