plugins {
    id("me.qoomon.git-versioning") version "5.1.1"
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
println(project.property("git.commit"))
println(project.property("git.ref"))
