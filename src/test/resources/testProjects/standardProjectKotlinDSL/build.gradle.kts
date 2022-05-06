import kotlin.reflect.full.declaredMemberExtensionProperties
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.declaredMembers

plugins {
    id("me.qoomon.git-versioning") version "6.0.0"
}

gitVersioning.apply {
    describeTagPattern = "v.*"
    refs {
        considerTagsOnBranches = false
        branch("master") {
            describeTagPattern =  "v.*"
            version = "\${describe}-SNAPSHOT"
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
