plugins {
    idea
}

// Find checksums here: https://gradle.org/release-checksums/
// Run gradlew :wrapper a couple of times to update.
tasks.wrapper {
    gradleVersion = "8.13"
    distributionSha256Sum = "20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78"
    distributionType = Wrapper.DistributionType.BIN
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
