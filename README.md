# Gradle Git Versioning Plugin

[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/me/qoomon/gradle-git-versioning-plugin/maven-metadata.xml.svg?colorB=007ec6&label=gradle-plugin)](https://plugins.gradle.org/plugin/me.qoomon.git-versioning)
[![Changelog](https://badgen.net/badge/changelog/%E2%98%85/blue)](#changelog)

[![Build Workflow](https://github.com/qoomon/gradle-git-versioning-plugin/workflows/Build/badge.svg)](https://github.com/qoomon/gradle-git-versioning-plugin/actions)
[![LGTM Grade](https://img.shields.io/lgtm/grade/java/github/qoomon/gradle-git-versioning-plugin)](https://lgtm.com/projects/g/qoomon/gradle-git-versioning-plugin)

**ℹ Also available as [Maven Extension](https://github.com/qoomon/maven-git-versioning-extension)**


This extension will set project versions, based on current **Git branch** or **Git tag**.

* Get rid of...
    * editing `build.gradle`
    * managing version by git and within files
    * Git merge conflicts

![Example](doc/GradleGitVersioningPlugin.png)

## Install

### Add Plugin

#### Groovy DSL `build.gradle`
```groovy
plugins {
    id 'me.qoomon.git-versioning' version '2.0.0'
}

// ...

version = '0.0.0-SNAPSHOT'
gitVersioning.apply {
  // see configuration documentation below
}
```

#### Kotlin DSL `build.gradle.kts`
```kotlin
plugins {
    id("me.qoomon.git-versioning") version "2.0.0"
}


// ...

version = "0.0.0-SNAPSHOT"
gitVersioning.apply {
  // see configuration documentation below
}
```

ℹ Consider [CI/CD](#cicd) section when running this plugin in a CI/CD environment 

## Configure Extension

You can configure the final version format for specific branches and tags separately.

### Example Configuration

##### Groovy DSL `build.gradle` 
```groovy
gitVersioning.apply {
  branch {
    pattern = 'master'
    versionFormat = '${version}'
  }
  branch {
    pattern = 'feature/(?<feature>.+)'
    versionFormat = '${feature}-SNAPSHOT'
  }
  tag {
    pattern = 'v(?<tagVersion>[0-9].*)'
    versionFormat = '${tagVersion}'
  }
  commit {
    versionFormat = '${commit.short}'
  }
}
```

#### Kotlin DSL `build.gradle.kts`
```kotlin
import me.qoomon.gradle.gitversioning.GitVersioningPluginExtension.VersionDescription
import me.qoomon.gradle.gitversioning.GitVersioningPluginExtension.CommitVersionDescription
gitVersioning.apply(closureOf<GitVersioningPluginConfig> {
        branch(closureOf<VersionDescription>{
            pattern = "master"
            versionFormat = "\${version}"
        })
        branch(closureOf<VersionDescription>{
            pattern = "feature/(?<feature>.+)"
            versionFormat = "\${feature}-SNAPSHOT"
        })
        tag(closureOf<VersionDescription>{
            pattern = "v(?<tagVersion>[0-9].*)"
            versionFormat = "\${tagVersion}"
        })
        commit(closureOf<CommitVersionDescription>{
          versionFormat = "\${commit.short}"
        })
})
```

- *optional* `preferTags` global enable(`true`)/disable(`false`) prefer tag rules over branch rules if both match.

- `branch` specific version format definition.
    - `pattern` An arbitrary regex to match branch names (has to be a **full match pattern** e.g. `feature/.+` )
    - `versionFormat` An arbitrary string, see [Version Format & Placeholders](#version-format--placeholders)
    - `property` A property definition to update the value of a property
        - `pattern` An arbitrary regex to match property names
        - `value` The definition of the new property value
            - `pattern` An arbitrary regex to match property values
            - `format` The new value format of the property, see [Version Format & Placeholders](#version-format--placeholders)
    - ⚠ **considered if...**
        * HEAD attached to a branch `git checkout <BRANCH>`<br>
        * Or branch name is provided by environment variable or command line parameter

- `tag` specific version format definition.
    - `pattern` An arbitrary regex to match tag names (has to be a **full match pattern** e.g. `v[0-9].*` )
    - `versionFormat` An arbitrary string, see [Version Format & Placeholders](#version-format--placeholders)
    - `property` A property definition to update the value of a property
        - `pattern` An arbitrary regex to match property names
        - `value` The definition of the new property value
            - `pattern` An arbitrary regex to match property values
            - `format` The new value format of the property, see [Version Format & Placeholders](#version-format--placeholders)
    - ⚠ **considered if...**
        * HEAD is detached `git checkout <TAG>`<br>
        * Or tag name is provided by environment variable or command line parameter
  
- `commit` specific version format definition.
    - `versionFormat` An arbitrary string, see [Version Format & Placeholders](#version-format--placeholders)
    - `property` A property definition to update the value of a property
        - `pattern` An arbitrary regex to match property names
        - `value` The definition of the new property value
            - `pattern` An arbitrary regex to match property values
            - `format` The new value format of the property, see [Version Format & Placeholders](#version-format--placeholders)
    - ⚠ **considered if...**
        * HEAD is detached `git checkout <COMMIT>` and no matching version tag is pointing to HEAD<br>


#### Version Format & Placeholders

ℹ `/` characters within final version will be replaced by `-`**

- `${ref}`
    - current ref name (branch name, tag name or commit hash)

- `${branch}` (only available within branch configuration)
    - The branch name of `HEAD`
    - e.g. 'master', 'feature/next-big-thing', ...

- `${tag}` (only available within tag configuration)
    - The tag name that points at `HEAD`, if multiple tags point at `HEAD` latest version is selected
    - e.g. 'version/1.0.1', 'v1.2.3', ...

- `${commit}`
    - The `HEAD` commit hash
    - e.g. '0fc20459a8eceb2c4abb9bf0af45a6e8af17b94b'

- `${commit.short}`
    - The short `HEAD` commit hash (7 characters)
    - e.g. '0fc2045'

- `${commit.timestamp}`
    - The `HEAD` commit timestamp (epoch seconds)
    - e.g. '1560694278'
    
- `${commit.timestamp.datetime}`
    - The `HEAD` commit timestamp formatted as `yyyyMMdd.HHmmss`
    - e.g. '20190616.161442'

- `${dirty}`
    - A dirty flag indicator
    - resolves to '-DIRTY' if repo is in dirty state, empty string otherwise

- `Pattern Groups`
    - Contents of group in the regex pattern can be addressed by `group name` or `group index` e.g.
    - Named Group Example
        ```groovy
        pattern = 'feature/(?<feature>.+)'
        versionFormat = '${feature}-SNAPSHOT'    
        ```
    - Group Index Example
        ```groovy
        pattern = 'v([0-9].*)'
        versionFormat = '${1}'
        ```
        
- `${version}`
    - `version` set in `build.gradle`
    - e.g. '1.0.0-SNAPSHOT'
    
- `${version.release}`
    - `version` set in `build.gradle` without `-SNAPSHOT` postfix
    - e.g. '1.0.0'
    
### Parameters & Environment Variables

- Disable Plugin
    - **Environment Variables**
        - `export VERSIONING_DISABLE=true`
    - **Command Line Parameters**
        - `gradle ... -Pversioning.disable=true`

- Provide **branch** or **tag** name
    - **Environment Variables**
        - `export VERSIONING_GIT_BRANCH=$PROVIDED_BRANCH_NAME`
        - `export VERSIONING_GIT_TAG=$PROVIDED_TAG_NAME`
    - **Command Line Parameters**
        - `gradle ... -Pgit.branch=$PROVIDED_BRANCH_NAME`
        - `gradle ... -Pgit.tag=$PROVIDED_TAG_NAME`
  
  ℹ Especially useful for **CI builds** see [Miscellaneous Hints](#miscellaneous-hints)

- **Prefer Tags** for Versioning instead of Branches
    - **Environment Variables**
        - `export VERSIONING_PREFER_TAGS=true`
    - **Command Line Parameters**
        - `gradle ... -Pversioning.preferTags=true`

## Provided Project Properties

- `git.commit` e.g. '0fc20459a8eceb2c4abb9bf0af45a6e8af17b94b'
- `git.ref` value of branch of tag name, always set
  - `git.branch` e.g. 'feature/next-big-thing', only set for branch versioning
  - `git.tag` e.g. 'v1.2.3', only set for tag versioning
- `git.commit.timestamp` e.g. '1560694278'
- `git.commit.timestamp.datetime` e.g. '2019-11-16T14:37:10Z'


## Gradle Tasks

* **version**
  * Print project version e.g. `gradle :version -q`

## Miscellaneous Hints

### CI/CD
Most CI/CD systems do checkouts in a detached HEAD state so no branch information is available, however they provide environment variables with this information. You can provide those, by using [Parameters & Environment Variables](#parameters--environment-variables). Below you'll find some setup example for common CI/CD systems.

#### GitLab CI Setup
execute this snippet before running your `gradle` command
```shell
before_script:
  - if [ -n "$CI_COMMIT_TAG" ]; then
       export VERSIONING_GIT_TAG=$CI_COMMIT_TAG;
    else
       export VERSIONING_GIT_BRANCH=$CI_COMMIT_REF_NAME;
    fi
```

#### Jenkins Setup
execute this snippet before running your `gradle` command
```shell
if [[ "$GIT_BRANCH" = origin/tags/* ]]; then e
    export VERSIONING_GIT_TAG=${GIT_BRANCH#origin/tags/};
else 
    export VERSIONING_GIT_BRANCH=${GIT_BRANCH#origin/};
fi
```

## Build
```shell
  - ./gradlew build
```

# Changelog
### 2.1.0
* add `${dirty}` flag version format placeholder
* add `git.dirty` property

### 2.0.0

#### Breaking Changes
* New way of applying git versioning
  * You need to call `apply` method with config, after version was set. 
    ```
    version = '0.0.0-SNAPSHOT'
    gitVersioning.apply {
      // see configuration documentatiomn below
    }
    ```
