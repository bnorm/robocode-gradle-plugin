package com.bnorm.robocode

data class RobocodeRobot(
    val name: String
) {
    lateinit var classPath: String
    var version: String? = null
}
