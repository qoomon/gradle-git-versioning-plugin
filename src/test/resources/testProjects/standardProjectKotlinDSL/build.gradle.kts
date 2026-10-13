plugins {
    id("me.qoomon.git-versioning") version "6.4.4"
}

gitVersioning.apply {
    describeTagPattern = "v.*"
    refs {
        considerTagsOnBranches = false
        branch(".*") {
            describeTagPattern =  "v.*"
            version = "\${ref}-\${describe}-SNAPSHOT"
            properties["foo"] = "foo@gradle"
        }
    }

    rev {
        version = "\${commit}-TEST"
    }
}

tasks.register("debug") {
    // capture values at configuration time, the project is not available at execution time with the configuration cache
    val values = listOf(
        project.version,
        project.property("foo"),
        project.property("bar"),
        project.property("git.commit"),
        project.property("git.ref"),
    )
    doLast {
        values.forEach { println(it) }
    }
}
