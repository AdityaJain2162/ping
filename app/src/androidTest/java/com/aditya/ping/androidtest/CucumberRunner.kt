package com.aditya.ping.androidtest

import io.cucumber.junit.CucumberOptions

@CucumberOptions(
    features = ["features"],
    glue = ["com.aditya.ping.androidtest.steps"],
    plugin = ["pretty"],
    monochrome = true,
)
class CucumberRunner
