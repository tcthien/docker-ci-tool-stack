/**
*	User define parameters
*/
def gitCodelabConfig = 'https://github.com/tcthien/codelab-config-service'
def gitCodelabRegistry = 'https://github.com/tcthien/codelab-registry-service'
def gitCodelabGateway = 'https://github.com/tcthien/codelab-gateway-service'
def gitCodelabMonitoring = 'https://github.com/tcthien/codelab-monitoring-service'
def gitCodelabAuth = 'https://github.com/tcthien/codelab-auth-service'
def gitCodelabAccount = 'https://github.com/tcthien/codelab-account-service'
def gitCodelabArticle = 'https://github.com/tcthien/codelab-article-service'

// Docker Registry Authentication
def registryUrl = 'registry:5000'
def registryUser = 'admin'
def registryPass = 'admin123'

// Create Job for codelab-config-service
createCiJob("codelab-config-service", gitCodelabConfig, "pom.xml")
createSonarJob("codelab-config-service", "pom.xml")
createDockerBuildJob("codelab-config-service", ".", "codelab-config-service", registryUrl, admin, admin123)
createDockerStartJob("codelab-config-service", ".", "codelab-config-service", "20088:20088")
createDockerStopJob("codelab-config-service", ".", "codelab-config-service")

createCiJob("codelab-registry-service", gitCodelabRegistry, "pom.xml")
createSonarJob("codelab-registry-service", "pom.xml")
createDockerBuildJob("codelab-registry-service", ".", "codelab-registry-service", registryUrl, admin, admin123)
createDockerStartJob("codelab-registry-service", ".", "codelab-registry-service", "20087:20087")
createDockerStopJob("codelab-registry-service", ".", "codelab-registry-service")

createCiJob("codelab-gateway-service", gitCodelabGateway, "pom.xml")
createSonarJob("codelab-gateway-service", "pom.xml")
createDockerBuildJob("codelab-gateway-service", ".", "codelab-gateway-service", registryUrl, admin, admin123)
createDockerStartJob("codelab-gateway-service", ".", "codelab-gateway-service", "20080:20080")
createDockerStopJob("codelab-gateway-service", ".", "codelab-gateway-service")

createCiJob("codelab-monitoring-service", gitCodelabMonitoring, "pom.xml")
createSonarJob("codelab-monitoring-service", "pom.xml")
createDockerBuildJob("codelab-monitoring-service", ".", "codelab-monitoring-service", registryUrl, admin, admin123)
createDockerStartJob("codelab-monitoring-service", ".", "codelab-monitoring-service", "20086:20086 -p 20085:20085")
createDockerStopJob("codelab-monitoring-service", ".", "codelab-monitoring-service")

createCiJob("codelab-auth-service", gitCodelabAuth, "pom.xml")
createSonarJob("codelab-auth-service", "pom.xml")
createDockerBuildWithDbJob("codelab-auth-service", ".", "codelab-auth-service", "codelab-auth-mongodb", registryUrl, admin, admin123)
createDockerStartWithDbJob("codelab-auth-service", ".", "codelab-auth-service", "20084:20084", "codelab-auth-mongodb", "20184:27017")
createDockerStopWithDbJob("codelab-auth-service", ".", "codelab-auth-service", "codelab-auth-mongodb")

createCiJob("codelab-account-service", gitCodelabAccount, "pom.xml")
createSonarJob("codelab-account-service", "pom.xml")
createDockerBuildWithDbJob("codelab-account-service", ".", "codelab-account-service", "codelab-account-mongodb", registryUrl, admin, admin123)
createDockerStartWithDbJob("codelab-account-service", ".", "codelab-account-service", "20082:20082", "codelab-account-mongodb", "20182:27017")
createDockerStopWithDbJob("codelab-account-service", ".", "codelab-account-service", "codelab-account-mongodb")

createCiJob("codelab-article-service", gitCodelabArticle, "pom.xml")
createSonarJob("codelab-article-service", "pom.xml")
createDockerBuildWithDbJob("codelab-article-service", ".", "codelab-article-service", "codelab-article-mongodb", registryUrl, admin, admin123)
createDockerStartWithDbJob("codelab-article-service", ".", "codelab-article-service", "20083:20083", "codelab-article-mongodb", "20183:27017")
createDockerStopWithDbJob("codelab-article-service", ".", "codelab-article-service", "codelab-article-mongodb")

def createCiJob(def jobName, def gitUrl, def pomFile) {
  job("${jobName}-1-ci") {
    parameters {
      stringParam("BRANCH", "master", "Define TAG or BRANCH to build from")
    }
    scm {
      git {
        remote {
          url(gitUrl)
        }
        extensions {
          cleanAfterCheckout()
        }
      }
    }
    wrappers {
      colorizeOutput()
      preBuildCleanup()
    }
    triggers {
      scm('30/H * * * *')
      githubPush()
    }
    steps {
      maven {
          goals('clean versions:set -DnewVersion=DEV-\${BUILD_NUMBER}')
          mavenInstallation('Maven 3.3.3')
          rootPOM( pomFile )
          mavenOpts('-Xms512m -Xmx1024m')
          providedGlobalSettings('MyGlobalSettings')
      }
      maven {
        goals('clean install')
        mavenInstallation('Maven 3.3.3')
        rootPOM(pomFile)
        mavenOpts('-Xms512m -Xmx1024m')
        providedGlobalSettings('MyGlobalSettings')
      }
    }
    publishers {
      archiveXUnit {
        jUnit {
          pattern('**/target/surefire-reports/*.xml')
          skipNoTestFiles(true)
          stopProcessingIfError(true)
        }
      }
      publishCloneWorkspace('**', '', 'Any', 'TAR', true, null)
      downstreamParameterized {
        trigger("${jobName}-2-sonar") {
          parameters {
            currentBuild()
          }
        }
      }
    }
  }
}

def createSonarJob(def jobName, def pomFile) {
  job("${jobName}-2-sonar") {
    parameters {
      stringParam("BRANCH", "master", "Define TAG or BRANCH to build from")
    }
    scm {
      cloneWorkspace("${jobName}-1-ci")
    }
    wrappers {
      colorizeOutput()
      preBuildCleanup()
    }
    steps {
      maven {
        goals('org.jacoco:jacoco-maven-plugin:0.7.4.201502262128:prepare-agent install -Psonar')
        mavenInstallation('Maven 3.3.3')
        rootPOM(pomFile)
        mavenOpts('-Xms512m -Xmx1024m')
        providedGlobalSettings('MyGlobalSettings')
      }
      maven {
        goals('sonar:sonar -Psonar')
        mavenInstallation('Maven 3.3.3')
        rootPOM(pomFile)
        mavenOpts('-Xms512m -Xmx1024m')
        providedGlobalSettings('MyGlobalSettings')
      }
    }
    publishers {
      downstreamParameterized {
        trigger("${jobName}-3-docker-build") {
          parameters {
            currentBuild()
          }
        }
      }
    }
  }
}

def createDockerBuildWithDbJob(def jobName, def folder, def dockerImageName, def dockerImageNameOfDb, def registryUrl, def registryUser, def registryPass) {

  println "############################################################################################################"
  println "Creating Docker Build Job for ${jobName} "
  println "############################################################################################################"

  job("${jobName}-3-docker-build") {
    logRotator {
        numToKeep(10)
    }
    scm {
      cloneWorkspace("${jobName}-1-ci")
    }
    steps {
      steps {
        shell("cd ${folder}/mongodb && sudo /usr/bin/docker build -t ${dockerImageNameOfDb} .")
        shell("cd ${folder}/mongodb && sudo /usr/bin/docker tag ${dockerImageNameOfDb} ${registryUrl}/codelab/${dockerImageNameOfDb}")
        shell("cd ${folder}/mongodb && sudo /usr/bin/docker login ${registryUrl} -u ${registryUser} -p ${registryPass} && sudo /usr/bin/docker push ${registryUrl}/codelab/${dockerImageNameOfDb}")
        shell("cd ${folder} && sudo /usr/bin/docker build -t ${dockerImageName} .")
        shell("cd ${folder} && sudo /usr/bin/docker tag ${dockerImageName} ${registryUrl}/codelab/${dockerImageName}")
        shell("cd ${folder} && sudo /usr/bin/docker login ${registryUrl} -u ${registryUser} -p ${registryPass} && sudo /usr/bin/docker push ${registryUrl}/codelab/${dockerImageName}")
      }
    }
    publishers {
      /*
      downstreamParameterized {
        trigger("${jobName}-4-docker-start-container") {
          parameters {
            currentBuild()
          }
        }
      }
      */
    }
  }
}

def createDockerBuildJob(def jobName, def folder, def dockerImageName, def registryUrl, def registryUser, def registryPass) {

  println "############################################################################################################"
  println "Creating Docker Build Job for ${jobName} "
  println "############################################################################################################"

  job("${jobName}-3-docker-build") {
    logRotator {
        numToKeep(10)
    }
    scm {
      cloneWorkspace("${jobName}-1-ci")
    }
    steps {
      steps {
        shell("cd ${folder} && sudo /usr/bin/docker build -t ${dockerImageName} .")
        shell("cd ${folder} && sudo /usr/bin/docker tag ${dockerImageName} ${registryUrl}/codelab/${dockerImageName}")
        shell("cd ${folder} && sudo /usr/bin/docker login ${registryUrl} -u ${registryUser} -p ${registryPass} && sudo /usr/bin/docker push ${registryUrl}/codelab/${dockerImageName}")
      }
    }
    publishers {
      /*
      downstreamParameterized {
        trigger("${jobName}-4-docker-start-container") {
          parameters {
            currentBuild()
          }
        }
      }
      */
    }
  }
}

def createDockerStartWithDbJob(def jobName, def folder, def dockerImageName, def dockerPortMapping, def dockerImageNameOfDb, def dockerPortMappingOfDb) {

  println "############################################################################################################"
  println "Creating Docker Start Job for ${jobName} "
  println "############################################################################################################"

  job("${jobName}-4-docker-start-container") {
    logRotator {
        numToKeep(10)
    }
    scm {
      cloneWorkspace("${jobName}-1-ci")
    }
    steps {
      steps {
        shell('echo "Stopping Docker Container ${dockerImageNameOfDb} with port ${dockerPortMappingOfDb}"')
        shell("sudo /usr/bin/docker stop \$(sudo /usr/bin/docker ps -a -q --filter=\"name=${dockerImageNameOfDb}\") | true ")
        shell("sudo /usr/bin/docker rm \$(sudo /usr/bin/docker ps -a -q --filter=\"name=${dockerImageNameOfDb}\") | true ")
        shell('echo "Starting Docker Container ${dockerImageNameOfDb} with port ${dockerPortMappingOfDb}"')
        shell("sudo /usr/bin/docker run --network=host --restart always --env-file ./env -d --name ${dockerImageNameOfDb} -p=${dockerPortMappingOfDb} ${dockerImageNameOfDb}")
		
        shell('echo "Stopping Docker Container ${dockerImageName} with port ${dockerPortMapping}"')
        shell("sudo /usr/bin/docker stop \$(sudo /usr/bin/docker ps -a -q --filter=\"name=${dockerImageName}\") | true ")
        shell("sudo /usr/bin/docker rm \$(sudo /usr/bin/docker ps -a -q --filter=\"name=${dockerImageName}\") | true ")
        shell('echo "Starting Docker Container ${dockerImageName} with port ${dockerPortMapping}"')
        shell("sudo /usr/bin/docker run --network=host --restart always --env-file ./env -d --name ${dockerImageName} -p=${dockerPortMapping} ${dockerImageName}")
      }
    }
    publishers {
    }
  }
}

def createDockerStartJob(def jobName, def folder, def dockerImageName, def dockerPortMapping) {

  println "############################################################################################################"
  println "Creating Docker Start Job for ${jobName} "
  println "############################################################################################################"

  job("${jobName}-4-docker-start-container") {
    logRotator {
        numToKeep(10)
    }
    scm {
      cloneWorkspace("${jobName}-1-ci")
    }
    steps {
      steps {
        shell('echo "Stopping Docker Container ${dockerImageName} with port ${dockerPortMapping}"')
        shell("sudo /usr/bin/docker stop \$(sudo /usr/bin/docker ps -a -q --filter=\"name=${dockerImageName}\") | true ")
        shell("sudo /usr/bin/docker rm \$(sudo /usr/bin/docker ps -a -q --filter=\"name=${dockerImageName}\") | true ")
        shell('echo "Starting Docker Container ${dockerImageName} with port ${dockerPortMapping}"')
        shell("sudo /usr/bin/docker run --network=host --restart always --env-file ./env -d --name ${dockerImageName} -p=${dockerPortMapping} ${dockerImageName}")
      }
    }
    publishers {
    }
  }
}

def createDockerStopWithDbJob(def jobName, def folder, def dockerImageName, def dockerImageNameOfDb) {

  println "############################################################################################################"
  println "Creating Docker Stop Job for ${jobName} "
  println "############################################################################################################"

  job("${jobName}-5-docker-stop-container") {
    logRotator {
        numToKeep(10)
    }
    steps {
      steps {
        shell("sudo /usr/bin/docker stop \$(sudo /usr/bin/docker ps -a -q --filter=\"name=${dockerImageName}\")")
        shell("sudo /usr/bin/docker rm \$(sudo /usr/bin/docker ps -a -q --filter=\"name=${dockerImageName}\")")
		
		shell("sudo /usr/bin/docker stop \$(sudo /usr/bin/docker ps -a -q --filter=\"name=${dockerImageNameOfDb}\")")
        shell("sudo /usr/bin/docker rm \$(sudo /usr/bin/docker ps -a -q --filter=\"name=${dockerImageNameOfDb}\")")
      }
    }
    publishers {
    }
  }
}

def createDockerStopJob(def jobName, def folder, def dockerImageName) {

  println "############################################################################################################"
  println "Creating Docker Stop Job for ${jobName} "
  println "############################################################################################################"

  job("${jobName}-5-docker-stop-container") {
    logRotator {
        numToKeep(10)
    }
    steps {
      steps {
        shell("sudo /usr/bin/docker stop \$(sudo /usr/bin/docker ps -a -q --filter=\"name=${dockerImageName}\")")
        shell("sudo /usr/bin/docker rm \$(sudo /usr/bin/docker ps -a -q --filter=\"name=${dockerImageName}\")")
      }
    }
    publishers {
    }
  }
}

buildPipelineView('cl-config') {
    filterBuildQueue()
    filterExecutors()
    title('Codelab Config Service')
    displayedBuilds(5)
    selectedJob("codelab-config-service-1-ci")
    alwaysAllowManualTrigger()
    refreshFrequency(60)
}

buildPipelineView('cl-registry') {
    filterBuildQueue()
    filterExecutors()
    title('Codelab Registry Service')
    displayedBuilds(5)
    selectedJob("codelab-registry-service-1-ci")
    alwaysAllowManualTrigger()
    refreshFrequency(60)
}
buildPipelineView('cl-monitoring') {
    filterBuildQueue()
    filterExecutors()
    title('Codelab Monitoring Service')
    displayedBuilds(5)
    selectedJob("codelab-monitoring-service-1-ci")
    alwaysAllowManualTrigger()
    refreshFrequency(60)
}
buildPipelineView('cl-gateway') {
    filterBuildQueue()
    filterExecutors()
    title('Codelab Gateway Service')
    displayedBuilds(5)
    selectedJob("codelab-gateway-service-1-ci")
    alwaysAllowManualTrigger()
    refreshFrequency(60)
}
buildPipelineView('cl-auth') {
    filterBuildQueue()
    filterExecutors()
    title('Codelab Auth Service')
    displayedBuilds(5)
    selectedJob("codelab-auth-service-1-ci")
    alwaysAllowManualTrigger()
    refreshFrequency(60)
}
buildPipelineView('cl-account') {
    filterBuildQueue()
    filterExecutors()
    title('Codelab Account Service')
    displayedBuilds(5)
    selectedJob("codelab-account-service-1-ci")
    alwaysAllowManualTrigger()
    refreshFrequency(60)
}
buildPipelineView('cl-article') {
    filterBuildQueue()
    filterExecutors()
    title('Codelab Article Service')
    displayedBuilds(5)
    selectedJob("codelab-article-service-1-ci")
    alwaysAllowManualTrigger()
    refreshFrequency(60)
}

listView('Codelab') {
    description('')
    filterBuildQueue()
    filterExecutors()
    jobs {
        regex(/codelab-.*/)
    }
    columns {
        status()
        buildButton()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
    }
}

listView('Codelab-ci') {
    description('')
    filterBuildQueue()
    filterExecutors()
    jobs {
        regex(/codelab-.*-ci/)
    }
    columns {
        status()
        buildButton()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
    }
}