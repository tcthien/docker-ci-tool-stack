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
def registryUrl = 'codelab.local:5000'
def registryUser = 'admin'
def registryPass = 'admin123'

// Create Job for codelab-config-service
createDockerBuildJob("codelab-prod-config-service", "codelab-config-service", registryUrl, registryUser, registryPass)
createDockerStartJob("codelab-prod-config-service", "codelab-config-service", "20088:20088", gitCodelabConfig)
createDockerStopJob("codelab-prod-config-service", "codelab-config-service")

createDockerBuildJob("codelab-prod-registry-service", "codelab-registry-service", registryUrl, registryUser, registryPass)
createDockerStartJob("codelab-prod-registry-service", "codelab-registry-service", "20087:20087", gitCodelabRegistry)
createDockerStopJob("codelab-prod-registry-service", "codelab-registry-service")

createDockerBuildJob("codelab-prod-gateway-service", "codelab-gateway-service", registryUrl, registryUser, registryPass)
createDockerStartJob("codelab-prod-gateway-service", "codelab-gateway-service", "20080:20080", gitCodelabGateway)
createDockerStopJob("codelab-prod-gateway-service", "codelab-gateway-service")

createDockerBuildJob("codelab-prod-monitoring-service", "codelab-monitoring-service", registryUrl, registryUser, registryPass)
createDockerStartJob("codelab-prod-monitoring-service", "codelab-monitoring-service", "20086:20086 -p 20085:20085", gitCodelabMonitoring)
createDockerStopJob("codelab-prod-monitoring-service", "codelab-monitoring-service")

createDockerBuildWithDbJob("codelab-prod-auth-service", "codelab-auth-service", "codelab-auth-mongodb", registryUrl, registryUser, registryPass)
createDockerStartWithDbJob("codelab-prod-auth-service", "codelab-auth-service", "20084:20084", "codelab-auth-mongodb", "20184:27017", gitCodelabAuth)
createDockerStopWithDbJob("codelab-prod-auth-service", "codelab-auth-service", "codelab-auth-mongodb")

createDockerBuildWithDbJob("codelab-prod-account-service", "codelab-account-service", "codelab-account-mongodb", registryUrl, registryUser, registryPass)
createDockerStartWithDbJob("codelab-prod-account-service", "codelab-account-service", "20082:20082", "codelab-account-mongodb", "20182:27017", gitCodelabAccount)
createDockerStopWithDbJob("codelab-prod-account-service", "codelab-account-service", "codelab-account-mongodb")

createDockerBuildWithDbJob("codelab-prod-article-service", "codelab-article-service", "codelab-article-mongodb", registryUrl, registryUser, registryPass)
createDockerStartWithDbJob("codelab-prod-article-service", "codelab-article-service", "20083:20083", "codelab-article-mongodb", "20183:27017", gitCodelabArticle)
createDockerStopWithDbJob("codelab-prod-article-service", "codelab-article-service", "codelab-article-mongodb")


def createDockerBuildWithDbJob(def jobName, def dockerImageName, def dockerImageNameOfDb, def registryUrl, def registryUser, def registryPass) {

  println "############################################################################################################"
  println "Creating Docker Build Job for ${jobName} "
  println "############################################################################################################"

  job("${jobName}-1-docker-pull") {
    logRotator {
        numToKeep(10)
    }
    steps {
      steps {
        shell("sudo /usr/bin/docker login ${registryUrl} -u ${registryUser} -p ${registryPass} && sudo /usr/bin/docker pull ${registryUrl}/codelab/${dockerImageNameOfDb}")
        shell("sudo /usr/bin/docker login ${registryUrl} -u ${registryUser} -p ${registryPass} && sudo /usr/bin/docker pull ${registryUrl}/codelab/${dockerImageName}")
      }
    }
    publishers {
      downstreamParameterized {
        trigger("${jobName}-2-docker-start") {
          parameters {
            StringParameterValue('WORK_AROUND', "WORK_AROUND")
          }
        }
      }
    }
  }
}

def createDockerBuildJob(def jobName, def dockerImageName, def registryUrl, def registryUser, def registryPass) {

  println "############################################################################################################"
  println "Creating Docker Build Job for ${jobName} "
  println "############################################################################################################"

  job("${jobName}-1-docker-pull") {
    logRotator {
        numToKeep(10)
    }
    steps {
      steps {
        shell("sudo /usr/bin/docker login ${registryUrl} -u ${registryUser} -p ${registryPass} && sudo /usr/bin/docker pull ${registryUrl}/codelab/${dockerImageName}")
      }
    }
    publishers {
      downstreamParameterized {
        trigger("${jobName}-2-docker-start") {
          parameters {
            StringParameterValue('WORK_AROUND', "WORK_AROUND")
          }
        }
      }
    }
  }
}

def createDockerStartWithDbJob(def jobName, def dockerImageName, def dockerPortMapping, def dockerImageNameOfDb, def dockerPortMappingOfDb, def gitUrl) {

  println "############################################################################################################"
  println "Creating Docker Start Job for ${jobName} "
  println "############################################################################################################"

  job("${jobName}-2-docker-start") {
    logRotator {
        numToKeep(10)
    }
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
      steps {
        shell('echo "Stopping Docker Container ${dockerImageNameOfDb} with port ${dockerPortMappingOfDb}"')
        shell("sudo /usr/bin/docker stop \$(sudo /usr/bin/docker ps -a -q --filter=\"name=${dockerImageNameOfDb}\") | true ")
        shell("sudo /usr/bin/docker rm \$(sudo /usr/bin/docker ps -a -q --filter=\"name=${dockerImageNameOfDb}\") | true ")
        shell('echo "Starting Docker Container ${dockerImageNameOfDb} with port ${dockerPortMappingOfDb}"')
        shell("sudo /usr/bin/docker run --network=host --restart always --env-file ./env -d --name ${dockerImageNameOfDb} -p=${dockerPortMappingOfDb} codelab/${dockerImageNameOfDb}")
		
        shell('echo "Stopping Docker Container ${dockerImageName} with port ${dockerPortMapping}"')
        shell("sudo /usr/bin/docker stop \$(sudo /usr/bin/docker ps -a -q --filter=\"name=${dockerImageName}\") | true ")
        shell("sudo /usr/bin/docker rm \$(sudo /usr/bin/docker ps -a -q --filter=\"name=${dockerImageName}\") | true ")
        shell('echo "Starting Docker Container ${dockerImageName} with port ${dockerPortMapping}"')
        shell("sudo /usr/bin/docker run --network=host --restart always --env-file ./env -d --name ${dockerImageName} -p=${dockerPortMapping} codelab/${dockerImageName}")
      }
    }
    publishers {
    }
  }
}

def createDockerStartJob(def jobName, def dockerImageName, def dockerPortMapping, def gitUrl) {

  println "############################################################################################################"
  println "Creating Docker Start Job for ${jobName} "
  println "############################################################################################################"

  job("${jobName}-2-docker-start") {
    logRotator {
        numToKeep(10)
    }
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
      steps {
        shell('echo "Stopping Docker Container ${dockerImageName} with port ${dockerPortMapping}"')
        shell("sudo /usr/bin/docker stop \$(sudo /usr/bin/docker ps -a -q --filter=\"name=${dockerImageName}\") | true ")
        shell("sudo /usr/bin/docker rm \$(sudo /usr/bin/docker ps -a -q --filter=\"name=${dockerImageName}\") | true ")
        shell('echo "Starting Docker Container ${dockerImageName} with port ${dockerPortMapping}"')
        shell("sudo /usr/bin/docker run --network=host --restart always --env-file ./env -d --name ${dockerImageName} -p=${dockerPortMapping} codelab/${dockerImageName}")
      }
    }
    publishers {
    }
  }
}

def createDockerStopWithDbJob(def jobName, def dockerImageName, def dockerImageNameOfDb) {

  println "############################################################################################################"
  println "Creating Docker Stop Job for ${jobName} "
  println "############################################################################################################"

  job("${jobName}-3-docker-stop") {
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

def createDockerStopJob(def jobName, def dockerImageName) {

  println "############################################################################################################"
  println "Creating Docker Stop Job for ${jobName} "
  println "############################################################################################################"

  job("${jobName}-3-docker-stop") {
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

buildPipelineView('cl-config-prod') {
    filterBuildQueue()
    filterExecutors()
    title('Codelab Production Config Service')
    displayedBuilds(5)
    selectedJob("codelab-prod-config-service-1-docker-pull")
    alwaysAllowManualTrigger()
    refreshFrequency(60)
}

buildPipelineView('cl-registry-prod') {
    filterBuildQueue()
    filterExecutors()
    title('Codelab Production Registry Service')
    displayedBuilds(5)
    selectedJob("codelab-prod-registry-service-1-docker-pull")
    alwaysAllowManualTrigger()
    refreshFrequency(60)
}
buildPipelineView('cl-monitoring-prod') {
    filterBuildQueue()
    filterExecutors()
    title('Codelab Production Monitoring Service')
    displayedBuilds(5)
    selectedJob("codelab-prod-monitoring-service-1-docker-pull")
    alwaysAllowManualTrigger()
    refreshFrequency(60)
}
buildPipelineView('cl-gateway-prod') {
    filterBuildQueue()
    filterExecutors()
    title('Codelab Production Gateway Service')
    displayedBuilds(5)
    selectedJob("codelab-prod-gateway-service-1-docker-pull")
    alwaysAllowManualTrigger()
    refreshFrequency(60)
}
buildPipelineView('cl-auth-prod') {
    filterBuildQueue()
    filterExecutors()
    title('Codelab Production Auth Service')
    displayedBuilds(5)
    selectedJob("codelab-prod-auth-service-1-docker-pull")
    alwaysAllowManualTrigger()
    refreshFrequency(60)
}
buildPipelineView('cl-account-prod') {
    filterBuildQueue()
    filterExecutors()
    title('Codelab Production Account Service')
    displayedBuilds(5)
    selectedJob("codelab-prod-account-service-1-docker-pull")
    alwaysAllowManualTrigger()
    refreshFrequency(60)
}
buildPipelineView('cl-article-prod') {
    filterBuildQueue()
    filterExecutors()
    title('Codelab Production Article Service')
    displayedBuilds(5)
    selectedJob("codelab-prod-article-service-1-docker-pull")
    alwaysAllowManualTrigger()
    refreshFrequency(60)
}
