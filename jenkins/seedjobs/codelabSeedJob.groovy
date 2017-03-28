/**
*	User define parameters
*/
def nexusRepo = 'http://nexus:8081/content/repositories/releases/'
def gitCodelabConfig = 'https://github.com/tcthien/codelab-config-service'
def gitCodelabRegistry = 'https://github.com/tcthien/codelab-registry-service'
def gitCodelabGateway = 'https://github.com/tcthien/codelab-gateway-service'
def gitCodelabMonitoring = 'https://github.com/tcthien/codelab-monitoring-service'
def gitCodelabAuth = 'https://github.com/tcthien/codelab-auth-service'
def gitCodelabAccount = 'https://github.com/tcthien/codelab-account-service'
def gitCodelabArticle = 'https://github.com/tcthien/codelab-article-service'


// Create Job for codelab-config-service
createCiJob("codelab-config-service", gitCodelabConfig, "pom.xml", nexusRepo)
createSonarJob("codelab-config-service", "pom.xml")
createDockerBuildJob("codelab-config-service", ".", "codelab-config-service")
createDockerStartJob("codelab-config-service", ".", "codelab-config-service", "20088:20088")
createDockerStopJob("codelab-config-service", ".", "codelab-config-service")

createCiJob("codelab-registry-service", gitCodelabRegistry, "pom.xml", nexusRepo)
createSonarJob("codelab-registry-service", "pom.xml")
createDockerBuildJob("codelab-registry-service", ".", "codelab-registry-service")
createDockerStartJob("codelab-registry-service", ".", "codelab-registry-service", "20087:20087")
createDockerStopJob("codelab-registry-service", ".", "codelab-registry-service")

createCiJob("codelab-gateway-service", gitCodelabGateway, "pom.xml", nexusRepo)
createSonarJob("codelab-gateway-service", "pom.xml")
createDockerBuildJob("codelab-gateway-service", ".", "codelab-gateway-service")
createDockerStartJob("codelab-gateway-service", ".", "codelab-gateway-service", "20080:20080")
createDockerStopJob("codelab-gateway-service", ".", "codelab-gateway-service")

createCiJob("codelab-monitoring-service", gitCodelabMonitoring, "pom.xml", nexusRepo)
createSonarJob("codelab-monitoring-service", "pom.xml")
createDockerBuildJob("codelab-monitoring-service", ".", "codelab-monitoring-service")
createDockerStartJob("codelab-monitoring-service", ".", "codelab-monitoring-service", "20086:20086 -p 20085:20085")
createDockerStopJob("codelab-monitoring-service", ".", "codelab-monitoring-service")

createCiJob("codelab-auth-service", gitCodelabAuth, "pom.xml", nexusRepo)
createSonarJob("codelab-auth-service", "pom.xml")
createDockerBuildWithDbJob("codelab-auth-service", ".", "codelab-auth-service", "codelab-auth-mongodb")
createDockerStartWithDbJob("codelab-auth-service", ".", "codelab-auth-service", "20084:20084", "codelab-auth-mongodb", "20184:27017")
createDockerStopWithDbJob("codelab-auth-service", ".", "codelab-auth-service", "codelab-auth-mongodb")

createCiJob("codelab-account-service", gitCodelabAccount, "pom.xml", nexusRepo)
createSonarJob("codelab-account-service", "pom.xml")
createDockerBuildWithDbJob("codelab-account-service", ".", "codelab-account-service", "codelab-account-mongodb")
createDockerStartWithDbJob("codelab-account-service", ".", "codelab-account-service", "20082:20082", "codelab-account-mongodb", "20182:27017")
createDockerStopWithDbJob("codelab-account-service", ".", "codelab-account-service", "codelab-account-mongodb")

createCiJob("codelab-article-service", gitCodelabArticle, "pom.xml", nexusRepo)
createSonarJob("codelab-article-service", "pom.xml")
createDockerBuildWithDbJob("codelab-article-service", ".", "codelab-article-service", "codelab-article-mongodb")
createDockerStartWithDbJob("codelab-article-service", ".", "codelab-article-service", "20083:20083", "codelab-article-mongodb", "20183:27017")
createDockerStopWithDbJob("codelab-article-service", ".", "codelab-article-service", "codelab-article-mongodb")

def createCiJob(def jobName, def gitUrl, def pomFile, def nexusRepo) {
  job("${jobName}-1-ci") {
    parameters {
      stringParam("BRANCH", "master", "Define TAG or BRANCH to build from")
      stringParam("REPOSITORY_URL", nexusRepo, "Nexus Release Repository URL")
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
        goals('clean deploy')
        mavenInstallation('Maven 3.3.3')
        rootPOM(pomFile)
        mavenOpts('-Xms512m -Xmx1024m')
        providedGlobalSettings('MyGlobalSettings')
      }
    }
    publishers {
      chucknorris()
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
      chucknorris()
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

def createDockerBuildWithDbJob(def jobName, def folder, def dockerImageName, def dockerImageNameOfDb) {

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
		shell("cd ${folder} && sudo /usr/bin/docker build -t ${dockerImageNameOfDb} .")
        shell("cd ${folder} && sudo /usr/bin/docker build -t ${dockerImageName} .")
      }
    }
    publishers {
      chucknorris()
      downstreamParameterized {
        trigger("${jobName}-4-docker-start-container") {
          parameters {
            currentBuild()
          }
        }
      }
    }
  }
}

def createDockerBuildJob(def jobName, def folder, def dockerImageName) {

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
      }
    }
    publishers {
      chucknorris()
      downstreamParameterized {
        trigger("${jobName}-4-docker-start-container") {
          parameters {
            currentBuild()
          }
        }
      }
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
      chucknorris()
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
      chucknorris()
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
      chucknorris()
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
      chucknorris()
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
