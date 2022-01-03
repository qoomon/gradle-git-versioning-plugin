plugins {
    id("me.qoomon.git-versioning") version "5.1.2"
}

gitVersioning.apply {

    refs {
        considerTagsOnBranches = false
        branch("master") {
            version = "\${ref}-SNAPSHOT"
            properties = mapOf(
                "foo" to "foo@gradle",
            )
        }
    }

    rev {
        version = "\${commit}-TEST"
    }
}

tasks.register("debug") {
    doLast {
        println(project.version)
        println(project.property("foo"))
        println(project.property("bar"))
        println(project.property("git.commit"))
        println(project.property("git.ref"))
    }
}
