plugins {
    id("me.qoomon.git-versioning") version "5.0.0"
}

version = "0.0.0-SNAPSHOT"
gitVersioning.apply {

    refs {
        considerTagsOnBranches = false
        branch("release/.*") {
            version = "\${ref}-SNAPSHOT"
            properties.put("foo", "foo@gradle")
        }
    }

    rev {
        version = "\${commit}-TEST"
    }
}

println(project.version)
println(project.property("foo"))
println(project.property("bar"))
