# Changelog

## 5.1.4

##### Fixes
* fix worktree handling


## 5.1.3

##### Fixes
*  fix `rootDirectory` determination for sub working trees


## 5.1.2

##### Fixes
* `groovy` add special `properties_` field to work around groovy naming conflict of `properties` filed from `groovy.lang.MetaClass`  


## 5.1.1

##### Fixes
* if a tag is provided (and no branch) the extension behaves like in detached head state
* if a branch is provided (and no tag) the extension behaves like in attached head state with no tags pointing to head


## 5.1.0

##### Features
* New Placeholder `${commit.timestamp.year.2digit}`


## 5.0.0

##### Features
* Add GitHub Actions, GitLab CI and Jenkins environment variable support
    * GitHub Actions: if `$GITHUB_ACTIONS == true`, `GITHUB_REF` is considered
    * GitLab CI: if `$GITLAB_CI == true`, `CI_COMMIT_BRANCH` and `CI_COMMIT_TAG` are considered
    * Circle CI: if `$CIRCLECI == true`, `CIRCLE_BRANCH` and `CIRCLE_TAG` are considered
    * Jenkins: if `JENKINS_HOME` is set, `BRANCH_NAME` and `TAG_NAME` are considered
* Simplify configuration (also see BREAKING CHANGES)

    **Groovy DSL Example:** `build.gradle`
    ```groovy
    version = '0.0.0-SNAPSHOT'
    gitVersioning.apply {
        refs {
            branch('.+') {
                version = '${ref}-SNAPSHOT'
            }
            tag('v(?<version>.*)') {
                version = '${ref.version}'
            }
        }
        
        // optional fallback configuration in case of no matching ref configuration
        rev {
            version = '${commit}'
        }
    }
    ```
    
    **Kotlin DSL Example:** `build.gradle.kts`
    ```kotlin
    version = "0.0.0-SNAPSHOT"
    gitVersioning.apply {
        refs {
            branch(".+") {
                version = "\${ref}-SNAPSHOT"
            }
            tag('v(?<version>.*)') {
                version = "\${ref.version}"
            }
        }
        
        // optional fallback configuration in case of no matching ref configuration
        rev {
            version = "\${commit}"
        }
    }
    ```
* New option to consider tag configs on branches (attached HEAD), enabled by `refs { considerTagsOnBranches = true }`
    * If enabled, first matching branch or tag config will be used for versioning

##### BREAKING CHANGES
* There is no default config anymore, if no `ref` configuration is matching current git situation and no `rev` configuration has been
  defined a warning message will be logged and plugin execution will be skipped.
* Placeholder Changes (old -> new)
    * `${branch}` -> `${ref}`
    * `${tag}` -> `${ref}`
    * `${REF_PATTERN_GROUP}` -> `${ref.REF_PATTERN_GROUP}`
    * `${describe.TAG_PATTERN_GROUP}` -> `${describe.tag.TAG_PATTERN_GROUP}`
* `preferTags` option was removed
    * use `refs { considerTagsOnBranches = true }` instead


## 4.3.0

* **Features**
    * add git describe version placeholders
        * new placeholders
            * `${describe}`
            * `${describe.tag}`
                * `${describe.<TAG_PATTERN_GROUP_NAME or TAG_PATTERN_GROUP_INDEX>}` e.g. pattern `v(?<version>.*)` will create placeholder `${describe.version}`
            * `${describe.distance}`

* **BREAKING CHANGES**
    * no longer provide project property `git.dirty` due to performance issues on larger projects,
      version format placeholder `${dirty}` is still available
    

## 4.1.0

* **Features**
  * add ability to define default or overwrite values for version and property format.
    * default value if parameter value is not set `${paramter:-<DEFAULT_VALUE>}` e.g. `${buildNumber:-0}`
    * overwrite value if parameter has a value `${paramter:+<OVERWRITE_VALUE>}` e.g. `${dirty:+-SNAPSHOT}`
  

## 4.0.0 - **Major refactoring**

* **Features** 
    * Add option to disable plugin by default and enable on demand.
    * Add option to modify project `gradle.properties` file accordingly to plugin related changes.
        * commandline property `gradle ... -Pversioning.updateGradleProperties`
        * Environment variable `OPTION_UPDATE_GRADLE_PROPERTIES=true`
        * Plugin Config `updateGradleProperties=true` global and branch/tag specific.
    * Project `<Dependency>` and `<Plugin>` versions will be updated accordingly to git versions
    * Add config option `<disable>true</disable>` to disable extension by default.
    * Add format placeholder:
        * `${dirty.snapshot}`
        * `${commit.timestamp.year}`
        * `${commit.timestamp.month}`
        * `${commit.timestamp.day}`
        * `${commit.timestamp.hour}`
        * `${commit.timestamp.minute}`
        * `${commit.timestamp.second}`
        * Gradle CLI properties e.g. `gradle ... -Pfoo=bar` will be accessible by `${foo}` placeholder
* **BREAKING CHANGES**
  * default version format on a branch changed to `${branch}-SNAPSHOT` was `${commit}`
  * Replace property regex pattern match with simple name match
    * old regex pattern config `branch|tag|commit { property { pattern = 'abc' } }` 
    * new property name config `branch|tag|commit { property { name = 'abc' } }` 
  * Remove property value pattern `branch|tag|commit { property { valuePattern = 'xyz' } }`
  * Remove format placeholder `${property.name}`
  * Rename format placeholder `${property.value}` to just `${value}`
  * Remove `CommitVersionDescription`, use `VersionDescription` instead.


## 3.0.0

#### Features
* simplify `property` replacement configuration

#### Breaking Changes
* simplify `property` replacement configuration
    
    new config
    ```groovy
    gitVersioning.apply {
      branch {
        pattern = 'master'
        versionFormat = '${version}'
        property {
          pattern = 'revision'
          valueFormat = '${branch-SNAPSHOT}'
        }
      }
    }
    ```
    old config
    ```groovy
    gitVersioning.apply {
      branch {
        pattern = 'master'
        versionFormat = '${version}'
        property {
          pattern ='revision'
          value {
            format = '${branch-SNAPSHOT}'
          }
        }
      }
    }
    ```
  

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
      // see configuration documentatiomn above
    }
    ```
