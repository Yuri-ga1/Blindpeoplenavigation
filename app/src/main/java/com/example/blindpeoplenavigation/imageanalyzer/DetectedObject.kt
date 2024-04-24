package com.example.blindpeoplenavigation.imageanalyzer

data class DetectedObject(val locations: ObjectLocation, val objectClass: String, val scores: Float)
