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
            
            packageJSON = readJSON file: 'package.json'   
            VERSION = packageJSON.version
            IMAGE_NAME_TAG = "${REPOSITORY}/${PROJECT_NAME}:${VERSION}"
            currentBuild.displayName = "${ENVIRONMENT}-${VERSION}"
        }
        
        stage ("Build Docker Image") {
            
            sh "docker build --build-arg configuration=${ENVIRONMENT} -t ${IMAGE_NAME_TAG} ."
            withCredentials([usernamePassword(credentialsId: 'DockerRegistry', passwordVariable: 'psw', usernameVariable: 'usr')]) {
                sh 'docker login -u ${usr} -p ${psw} https://repository.factotumsoftware.com'
                sh "docker push ${IMAGE_NAME_TAG}"
            }
        }
        
        stage ("Stop Current Container") {

            sh 'docker rename ${PROJECT_NAME}_${ENVIRONMENT} ${PROJECT_NAME}_${ENVIRONMENT}_old || true'
            sh 'docker stop ${PROJECT_NAME}_${ENVIRONMENT}_old || true && docker rm ${PROJECT_NAME}_${ENVIRONMENT}_old || true'

        }
        
        stage ("Run Docker Container") {

            sh "docker run -p 95:80 -d --name ${PROJECT_NAME}_${ENVIRONMENT} ${IMAGE_NAME_TAG}"

        }
    } finally {
        cleanWs()
    }
}
