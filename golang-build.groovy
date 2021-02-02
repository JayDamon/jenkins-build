node {

    def VERSION
    def IMAGE_NAME_TAG
    def REPOSITORY = 'repository.factotumsoftware.com'

    try {

        stage ("Checkout") {
            checkout scm: [
                $class: 'GitSCM', 
                userRemoteConfigs: [
                    [
                        url: "https://github.com/JayDamon/${PROJECT_NAME}.git", 
                        credentialsId: 'GitHub'
                    ]
                ],
                branches: [
                    [
                        name: "${BRANCH_SELECTOR}"
                    ]
                ],
            ], poll: false


            VERSION = "${BRANCH_SELECTOR}"
            IMAGE_NAME_TAG = "${REPOSITORY}/${ENVIRONMENT}-${PROJECT_NAME}:${VERSION}"
            currentBuild.displayName = "${ENVIRONMENT}-${VERSION}"
        }

        stage ("Build Image") {

            sh "docker build --build-arg configuration=${ENVIRONMENT} -t ${IMAGE_NAME_TAG} ."

        }

        stage ("Push Container to Registry") {

            withCredentials([usernamePassword(credentialsId: 'DockerRegistry', passwordVariable: 'psw', usernameVariable: 'usr')]) {
                sh 'docker login -u ${usr} -p ${psw} https://repository.factotumsoftware.com'
                sh "docker push ${IMAGE_NAME_TAG}"
            }
        }

    } finally {
        cleanWs()
    }

}