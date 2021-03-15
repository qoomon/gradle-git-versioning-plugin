# Gradle Git Versioning Plugin

[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/me/qoomon/gradle-git-versioning-plugin/maven-metadata.xml.svg?colorB=007ec6&label=gradle-plugin)](https://plugins.gradle.org/plugin/me.qoomon.git-versioning)
[![Changelog](https://badgen.net/badge/changelog/%E2%98%85/blue)](CHANGELOG.md)

[![Build Workflow](https://github.com/qoomon/gradle-git-versioning-plugin/workflows/Build/badge.svg)](https://github.com/qoomon/gradle-git-versioning-plugin/actions)
[![LGTM Grade](https://img.shields.io/lgtm/grade/java/github/qoomon/gradle-git-versioning-plugin)](https://lgtm.com/projects/g/qoomon/gradle-git-versioning-plugin)


**ℹ Also available as [Maven Extension](https://github.com/qoomon/maven-git-versioning-extension)**


This extension will set project versions, based on current **Git branch** or **Git tag**.

ℹ **No files will be modified, versions are modified in memory only.**
* Get rid of...
    * editing `build.gradle`
    * managing project versions with Git tags and within files
    * Git merge conflicts

![Example](doc/GradleGitVersioningPlugin.png)

## Usage

### Add Plugin to Gradle Project

⚠️ You should apply git versioning (`gitVersioning.apply{...}`) directly after version declaration.

#### Groovy DSL `build.gradle`
```groovy
plugins {
    id 'me.qoomon.git-versioning' version '4.2.0'
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
    id("me.qoomon.git-versioning") version "4.2.0"
}


// ...

version = "0.0.0-SNAPSHOT"
gitVersioning.apply {
  // see configuration documentation below
}
```

ℹ Consider [CI/CD](#cicd-setup) section when running this plugin in a CI/CD environment 

## Configure Plugin

You can configure the final version format for specific branches and tags separately.

### Example Configuration

##### Groovy DSL `build.gradle` 
```groovy
version = '0.0.0-SNAPSHOT'
gitVersioning.apply {
  branch {
    pattern = 'main'
    versionFormat = '${version}'
  }
  branch {
    pattern = 'feature/(?<feature>.+)'
    versionFormat = '${feature}-SNAPSHOT'
  }
  branch {
    pattern = 'pull/.+'
    versionFormat = '${branch}-SNAPSHOT'
  }
  tag {
    pattern = 'v(?<tagVersion>[0-9].*)'
    versionFormat = '${tagVersion}'
  }
}
```

#### Kotlin DSL `build.gradle.kts`
```kotlin
import me.qoomon.gradle.gitversioning.GitVersioningPluginConfig
import me.qoomon.gradle.gitversioning.GitVersioningPluginConfig.*

//...

version = "0.0.0-SNAPSHOT"
gitVersioning.apply(closureOf<GitVersioningPluginConfig> {
        branch(closureOf<VersionDescription>{
            pattern = "main"
            versionFormat = "\${version}"
        })
        branch(closureOf<VersionDescription>{
            pattern = "feature/(?<feature>.+)"
            versionFormat = "\${feature}-SNAPSHOT"
        })
        branch(closureOf<VersionDescription>{
            pattern = "pull/.+)"
            versionFormat = "\${branch}-SNAPSHOT"
        })
        tag(closureOf<VersionDescription>{
            pattern = "v(?<tagVersion>[0-9].*)"
            versionFormat = "\${tagVersion}"
        })
})
```
- *optional* `<disable>` global disable(`true`)/enable(`false`) extension.
    - Can be overridden by command option, see (Parameters & Environment Variables)[#parameters-&-environment-variables].

- *optional* `updateGradleProperties` global enable(`true`)/disable(`false`) version and properties update in `gradle.properties` file.
    - Can be overridden by command option, see (Parameters & Environment Variables)[#parameters-&-environment-variables].

- *optional* `preferTags` global enable(`true`)/disable(`false`) prefer tag rules over branch rules if both match.

- `branch` specific version format definition.
    - `pattern` An arbitrary regex to match branch names (has to be a **full match pattern** e.g. `feature/.+` )
    - `versionFormat` An arbitrary string, see [Version Format & Placeholders](#version-format--placeholders)
    - `property` A property definition to update the value of a property
        - `name` The property name
        - `valueFormat` The new value format of the property, see [Version Format & Placeholders](#version-format--placeholders)
    - *optional* `updateGradleProperties` Enable(`true`) or disable(`false`) version and properties update in `gradle.properties` file. (will override global `updateGradleProperties` value)
    - ⚠ **considered if...**
        * HEAD attached to a branch `git checkout <BRANCH>`<br>
        * Or branch name is provided by environment variable or command line parameter

- `tag` specific version format definition.
    - `pattern` An arbitrary regex to match tag names (has to be a **full match pattern** e.g. `v[0-9].*` )
    - `versionFormat` An arbitrary string, see [Version Format & Placeholders](#version-format--placeholders)
    - `property` A property definition to update the value of a property
        - `name` The property name
        - `valueFormat` The new value format of the property, see [Version Format & Placeholders](#version-format--placeholders)
    - *optional* `updateGradleProperties` Enable(`true`) or disable(`false`) version and properties update in `gradle.properties` file. (will override global `updateGradleProperties` value)
    - ⚠ **considered if...**
        * HEAD is detached `git checkout <TAG>`<br>
        * Or tag name is provided by environment variable or command line parameter
  
- `commit` specific version format definition.
    - `versionFormat` An arbitrary string, see [Version Format & Placeholders](#version-format--placeholders)
    - `property` A property definition to update the value of a property
        - `name` The property name
        - `valueFormat` The new value format of the property, see [Version Format & Placeholders](#version-format--placeholders)
    - ⚠ **considered if...**
        * HEAD is detached `git checkout <COMMIT>` and no matching version tag is pointing to HEAD<br>

#### Format Placeholders

ℹ whole `versionFormat` will be slugified automatically, that means all `/` characters replaced by `-`

ℹ define placeholder default value (placeholder is not defined) like this `${name:-default_value}`<br>
  e.g `${buildNumber:-0}` or `${buildNumber:-local}` 

ℹ define placeholder overwrite value (placeholder is defined) like this `${name:+overwrite_value}`<br>
  e.g `${dirty:-SNAPSHOT}` resolves to `-SNAPSHOT` instead of `-DIRTY`
  
- `${ref}`
    - current ref name (branch name, tag name or commit hash)
- `${ref.slug}`
    - like `${ref}` with all `/` replaced by `-`

- `${branch}` (only available within branch configuration)
    - The branch name of `HEAD`
    - e.g. 'master', 'feature/next-big-thing', ...
- `${branch.slug}`
    - like `${branch}` with all `/` replaced by `-`    
 
- `${tag}` (only available within tag configuration)
    - The tag name that points at `HEAD`, if multiple tags point at `HEAD` latest version is selected
    - e.g. 'version/1.0.1', 'v1.2.3', ...
- `${tag.slug}`
    - like `${tag}` with all `/` replaced by `-`    
    
- `${commit}`
    - The `HEAD` commit hash
    - e.g. '0fc20459a8eceb2c4abb9bf0af45a6e8af17b94b'

- `${commit.short}`
    - The short `HEAD` commit hash (7 characters)
    - e.g. '0fc2045'

- `${commit.timestamp}`
    - The `HEAD` commit timestamp (epoch seconds)
    - e.g. '1560694278'
- `${commit.timestamp.year}`
    - The `HEAD` commit year
    - e.g. '2021'
- `${commit.timestamp.month}`
    - The `HEAD` commit month of year
    - e.g. '01'
- `${commit.timestamp.day}`
    - The `HEAD` commit day of month
    - e.g. '01'
- `${commit.timestamp.hour}`
    - The `HEAD` commit hour of day (24h)
    - e.g. '01'
- `${commit.timestamp.minutes}`
    - The `HEAD` commit minutes of hour
    - e.g. '01'
- `${commit.timestamp.seconds}`
    - The `HEAD` commit seconds of minute
    - e.g. '01'
- `${commit.timestamp.datetime}`
    - The `HEAD` commit timestamp formatted as `yyyyMMdd.HHmmss`
    - e.g. '20190616.161442'

- `Pattern Groups`
    - Contents of group in the regex pattern can be addressed `${GROUP_NAME}` or `${GROUP_INDEX}`
    - `${GROUP_NAME.slug}` or `${GROUP_INDEX.slug}`
        - like `${GROUP_NAME}` or `${GROUP_INDEX}` with all `/` replaced by `-`  
    - Examples
        - Named Group
            ```groovy
            pattern = 'feature/(?<feature>.+)'
            versionFormat = '${feature}-SNAPSHOT'    
            ```
        - Group Index
            ```groovy
            pattern = 'v([0-9].*)'
            versionFormat = '${1}'
            ```
          
- `${version}`
    - `version` set in `pom.xml`
    - e.g. '1.0.0-SNAPSHOT'
- `${version.release}`
    - like `${version}` without `-SNAPSHOT` postfix
    - e.g. '1.0.0'

- `${dirty}`
    - if repository has untracked files or uncommited changes this placeholder will resolve to `-DIRTY`, otherwise it will resolve to an empty string.  
- `${dirty.snapshot}`
    - if repository has untracked files or uncommited changes this placeholder will resolve to `-SNAPSHOT`, otherwise it will resolve to an empty string.
  
- `${value}` - Only available within property format
    - value of matching property

- `${env.VARIABLE}`
    - value of environment variable `VARIABLE`   

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

- Update `gradle.properties`
    - **Environment Variables**
        - `export VERSIONING_UPDATE_GRADLE_PROPERTIES=true`
    - **Command Line Parameters**
        - `gradle ... -Dversioning.updateGradleProperties=true`

- **Prefer Tags** for Versioning instead of Branches
    - **Environment Variables**
        - `export VERSIONING_PREFER_TAGS=true`
    - **Command Line Parameters**
        - `gradle ... -Pversioning.preferTags=true`

## Provided Project Properties

- `git.commit` e.g. '0fc20459a8eceb2c4abb9bf0af45a6e8af17b94b'
- `git.ref` value of branch or tag name or commit hash
    - `git.ref.slug` like `git.ref` with all `/` replaced by `-`
- `git.branch` e.g. 'feature/next-big-thing', only set for branch versioning
    - `git.branch.slug` like `git.branch` with all `/` replaced by `-`
- `git.tag` e.g. 'v1.2.3', only set for tag versioning
    - `git.tag.slug` like `git.tag` with all `/` replaced by `-`
- `git.commit.timestamp` e.g. '1560694278'
- `git.commit.timestamp.datetime` e.g. '2019-11-16T14:37:10Z'
- `git.dirty` repository's dirty state indicator `true` or `false`


## Gradle Tasks

* **version**
  * Print project version e.g. `gradle :version -q`

## Miscellaneous Hints

### CI/CD Setup
Most CI/CD systems do checkouts in a detached HEAD state so no branch information is available, however they provide environment variables with this information. You can provide those, by using [Parameters & Environment Variables](#parameters--environment-variables). Below you'll find some setup example for common CI/CD systems.

#### GitHub Actions Setup
execute this snippet before running your `gradle` command
```shell
if  [[ "$GITHUB_REF" = refs/tags/* ]]; then
    export VERSIONING_GIT_TAG=${GITHUB_REF#refs/tags/};
elif [[ "$GITHUB_REF" = refs/heads/* ]]; then
    export VERSIONING_GIT_BRANCH=${GITHUB_REF#refs/heads/};
elif [[ "$GITHUB_REF" = refs/pull/*/merge ]]; then
    export VERSIONING_GIT_BRANCH=${GITHUB_REF#refs/pull/};
fi
```

#### GitLab CI Setup
execute this snippet before running your `gradle` command
```shell
before_script:
  - export VERSIONING_GIT_TAG=$CI_COMMIT_TAG;
    export VERSIONING_GIT_BRANCH=$CI_COMMIT_BRANCH;
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
