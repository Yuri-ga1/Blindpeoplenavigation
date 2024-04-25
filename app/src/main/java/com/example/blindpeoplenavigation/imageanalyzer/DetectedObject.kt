package com.example.blindpeoplenavigation.imageanalyzer

data class DetectedObject(val location: ObjectLocation, val objectClass: String, val scores: Float)
