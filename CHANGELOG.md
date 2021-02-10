# Changelog

## 4.0.0
* Major refactoring
* **Features** 
    * Add option to disable extension by default and enable on demand.
    * Project `<Dependency>` and `<Plugin>` versions will be updated accordingly to git versions
      * Add config option `<disable>true</disable>` to disable extension by default.
      * Add format placeholder:
        * `${commit.timestamp.year}`
        * `${commit.timestamp.month}`
        * `${commit.timestamp.day}`
        * `${commit.timestamp.hour}`
        * `${commit.timestamp.minute}`
        * `${commit.timestamp.second}`
        * Gradle CLI properties e.g. `gradle ... -Pfoo=bar` will be accessable by `${foo}` placeholder
    * **BREAKING CHANGES**
      * default version format on a branch changed to `${branch}-SNAPSHOT` was `${commit}`
      * Replace property regex pattern match with simple name match
        * old regex pattern config `branch|tag|commit { property { pattern = 'abc' } }` 
        * new property name config `branch|tag|commit { property { name = 'abc' } }` 
      * Remove property value pattern `branch|tag|commit { property { valuePattern = 'xyz' } }`
      * Remove format placeholder `${property.name}`
      * Rename format placeholder `${property.value}` to just `${value}`


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