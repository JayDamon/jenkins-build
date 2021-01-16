node {

    def VERSION
    def IMAGE_NAME_TAG
    def PROJECT_NAME = 'bond'
    def REPOSITORY = 'repository.factotumsoftware.com'
    def ENVIRONMENT = 'dev'
    try {
        stage ("Checkout") {
       
            // git branch: '${BRANCH_SELECTOR}', url: 'https://github.com/JayDamon/bond.git', credentialsId: 'GitHub'   
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
                        name: '${BRANCH_SELECTOR}'
                    ]
                ],
            ], poll: false
            
            packageJSON = readJSON file: 'package.json'   
            VERSION = packageJSON.version
            IMAGE_NAME_TAG = "${REPOSITORY}/${PROJECT_NAME}:${VERSION}"
            echo "${IMAGE_NAME_TAG}"
        }
        
        stage ("Build Docker Image") {
            
            sh "docker build --build-arg configuration=dev -t ${IMAGE_NAME_TAG} ."
            withCredentials([usernamePassword(credentialsId: 'DockerRegistry', passwordVariable: 'psswd', usernameVariable: 'username')]) {
                sh 'docker login -u ${username} -p ${psswd} https://repository.factotumsoftware.com'
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
