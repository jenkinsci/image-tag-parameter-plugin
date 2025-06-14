# Image Tag Parameter Plugin
![Logo](img/registry.png)

[Changelog](CHANGELOG-old.md)

[![Licence](https://img.shields.io/github/license/jenkinsci/image-tag-parameter-plugin.svg)](https://github.com/jenkinsci/image-tag-parameter-plugin/blob/master/LICENSE)
[![Build Status](https://ci.jenkins.io/job/Plugins/job/image-tag-parameter-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/image-tag-parameter-plugin/job/master/)
[![Contributors](https://img.shields.io/github/contributors/jenkinsci/image-tag-parameter-plugin.svg)](https://github.com/jenkinsci/image-tag-parameter-plugin/graphs/contributors)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/image-tag-parameter.svg)](https://plugins.jenkins.io/image-tag-parameter)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/image-tag-parameter-plugin.svg?label=changelog)](https://github.com/jenkinsci/image-tag-parameter-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/image-tag-parameter.svg?color=blue)](https://plugins.jenkins.io/image-tag-parameter)

## Introduction
This plugin allows you to define (container) image tags as parameter in your builds.
It uses the Docker **Registry HTTP API V2** to list tags available for an image.

## Screenshots
![Parameter Type Selection](img/screen01.png)

![Configuration](img/screen02.png)

![Image Selection](img/screen03.png)

![Image Selection](img/screen04.png)

![Global Configuration](img/jenkinsConfig.png)

## Usage

### Global Configuration

This Plugin allows a Jenkins admin to set a default registry in the Jenkins config, which will get used for any ImageTag parameter that does not overwrite the `registry` value in the parameter definition.
It is also possible (as of v1.8) to define a default credential, which should get used alongside of that default registry.
The default credential can be overwritten on a per parameter level just like the default registry.

#### JCasC (Jenkins Configuration as Code)

```yaml
unclassified:
# ...
  imageTagParameterConfiguration:
    defaultRegistry: "https://registry-1.docker.io"
    defaultCredentialId: ""
    defaultTagOrdering: DSC_VERSION
    defaultTagPickerType: SELECT
# ...
```

Configuring this plugins global config was available via JCasC since the addition of the global config in v1.2,
but the `defaultCredentialId` value can only be configured with a ImageTagParam version equal or newer to v1.8.

### Definition in Freestyle / Pipeline UI
This is basically showcased in the above [screenshots](#screenshots) :wink:

### Definition in Pipeline
```groovy
pipeline {
  agent any

  parameters {
    imageTag(name: 'DOCKER_IMAGE', description: '',
             image: 'jenkins/jenkins', filter: 'lts.*', defaultTag: 'lts-jdk11',
             registry: 'https://registry-1.docker.io', credentialId: '' /* Use empty String to enforce query without authentication (for example ignore global default authentication) */,
             tagOrder: 'NATURAL', tagPickerType: 'AUTOCOMPLETE_STRICT')
  }

  stages {
    stage('Test') {
      steps {
        echo params.DOCKER_IMAGE
        echo "$DOCKER_IMAGE" // will print selected image name with tag (eg. jenkins/jenkins:lts-jdk11)
        echo "$DOCKER_IMAGE_TAG" // will print selected tag value (eg. lts-jdk11)
        echo "$DOCKER_IMAGE_IMAGE" // will print selected image name value (eg. jenkins/jenkins)
      }
    }
  }
}
```

**Required Values (Minimal Definition)**
```groovy
parameters {
  imageTag(name: 'DOCKER_IMAGE', image: 'jenkins/jenkins')
}
```

### Exposed Environment Variables (and params, since version 1.6)
Based on default Jenkins behaviour you can use `params.imageTagParameterName` to access the value of `imageName:imageTag`,
but since you most of the time only need the image tag by itself the plugin also exports some additional environment variables.

* **$imageTagParameterName_TAG** (or *env.imageTagParameterName_TAG*) contains only the tag value without the image name
* **$imageTagParameterName_IMAGE** (or *env.imageTagParameterName_IMAGE*) contains only the name of the image without the tag

### Set ImageTag Order (1.8+)

The order, of which the ImageTags are listed in the selector box, can be altered by the user via the parameter definition.
There are currently four options available:

* **NATURAL** or *Natural Ordering* ... this is the default setting and offers the same ordering as in the previous versions
* **REV_NATURAL** or *Reverse Natural Ordering* ... the reverted version of **NATURAL**
* **DSC_VERSION** or *Descending Versions* ... This will attempt to parse the gathered queried tags into `hudson.util.VersionNumber`
and order them descending.
* **ASC_VERSION** or *Ascending Versions* ... just like **DSC_VERSION** only in reverse (aka. ascending sorted)

**NOTE** The Version Ordering should be used with care and represents a convenience functionality, it will NOT work with every images tags or every configuration! \
The [`hudson.util.VersionNumber`](https://github.com/jenkinsci/lib-version-number/blob/master/src/main/java/hudson/util/VersionNumber.java)
may try to its best ability to parse a tag to a version, but as ImageTags don't require a format this might not yield an expected result,
so make sure to use a tag order that fits best for the tags you offer via a given job parameter.

## Contributing

Build instructions and requirements can be found in the [CONTRIBUTING](CONTRIBUTING.md) Markdown.
