#!groovy
import groovy.json.JsonSlurperClassic

node ('agent'){
    //VeraCompany
    // def SF_CONSUMER_KEY="3MVG9PwZx9R6_UreUz3oqKCmsmGhfLU2Ae6z8FE4ZDQwvkfOdF.KJxOPJK.2MeFHIoCAK1aSYou51NXj8MTeI"
    //JoseAdam
    def SF_CONSUMER_KEY="3MVG9k02hQhyUgQDJCnSUUKcwU_kRQ4HwnwVKlslkIOs8dKY4kt7kQQXM2aX7ZoR3tvlZD13z_FGojLGUBU1O"
    def SF_USERNAME= params.SF_USERNAME
    def ORG_ALIAS = params.ORG_ALIAS
    def snapshotId

    def SNAPSHOT_NAME = params.SNAPSHOT_NAME
    def SOURCE_SCRATCH_ID = params.SOURCE_SCRATCH_ID

    def NEW_SCRATCH_NAME = params.NEW_SCRATCH_NAME
    
        withEnv(["HOME=${env.WORKSPACE}"]) {
            stage("Authorizing DevHub Access") {
                sh '''
                    echo "
-----BEGIN PRIVATE KEY-----
MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQC7U9hqr4RZlOAC
GH0DRNETw6rmL40dN1kXR4jf74oIeD/BPMVw9vqnkEBP4jtU9RCdocMqVSwSRg6o
zRRiJpRQncgVoA8tGNLnNOWb5Z4e1PyBms2UILp9cymRownLyD856nD/PnpQgf5p
8d8kDMv5MoxWy21liY6UnyWQHeTV/F/r0pZUSezIh4NzdT7SHw+SFLWxutVYJL67
ZcnNHu9HAgMBAAECggEADrRMgZtYv+YQFg9cCMdRw25m2ii+DUlh7zzd6/i+lLRJ
CjRoqdmz3+FShbK+xjLHuh06LPbV6HKisJy1zo/esTFMGmkfp9eR/oodbTj/Shud
MoM3QzsKcycjX9uKEk/xU+zThTBJ+PXV8DhOROU80AExQ3IOk6023I5adxlr8O0b
Fy0XMW4S/AxT8b08k2naXaKMwbTr9uIPF7l8R8cOuPRHuiTtkB7iTg1TJMcLUBkL
z5iwujMPoS+LciHZX+HZwkjf3wH/GqS4iWZiM7N/4P7c+Cx3wfEkfnn7Z8Z8C+sZ
6M8ZWXnreeVP91NAmAJBQPN0EkAaQPstFXABdFvFSQKBgQDjGzPzNaesFn61MKOC
oIgaEgga09igMFqxf/lIQg5xJTtpsOm5CDJ1WBeRDBgROhSOgUBdcgf99EYDkfw9
0ODGKVaNsKqcjdC/mLD1xTkJpkuH7fzOPUf/4AGMVlyhqyAz6nXSxOX0+UwBIUfj
dERIFfUop7uxapROvzWpXIhK4wKBgQDTKQ7tMNP9NtA8rv/BEB8IPOVWrfLdGGRX
YwtG95wguPGDRnZ3uPa4LYUYV28sG0f6L3C/wJb3kTPl/97iqLlqiLVZkDTtg3om
6vH5LKWy8Fmwzg3XYt2pEOL5h7CmdW58MgOYr2RAmyg9qljjLL7djZJWbhh2IhOT
3gKhtQ1DTQKBgAhpCnx4QTRbROf63CNLRJ8KiehMbhpIkCNApP+rCpPgGBoq5gzn
I886quzBMOb6aqZ4XOTZAzjBYhJhxqCXaquL/luS7k6L5e4ifnMrkZgs42EuTA1r
MY5lQklrLVUDXFbv2NssVYPD86+bBumCdcDhp0mRJJ9GXiWoaRlThuyXAoGAHYu3
LPUSFHIg/Py3iHoK/YoXxOMeAlWxZJDVOR7YV7PJB0dKuEHHM8oav2cshQ1kKtuM
eSgnd4h3rIz60Ngu6XsrdNa6sSkeZGenZ+gZX0pGInkScrrArTFRsPyNeR0hUntl
CW4xJuEO5mH5gf/2zOVMPo+1GIJ4Hs6Hq7y7mFkCgYAlB+eXUC3ZZipcb1LgTVsT
HHTq8+u+l5oaET8Oz/S8Ga1YRDao+VmdMC385zX4uubfEAS7ncrO6QgcSEn6Yx1y
JZsQw4R2mdFifLbjNNMSC0TRLcFQGw0aYaRHQCnI7LWMr2rm/lIuPAgjBn5LyTxh
JbmHp76mj2srOJf7Pu34Lg==
-----END PRIVATE KEY-----" > /tmp/server.key 
                    '''
                def rc = sh(script: "sf org login jwt --client-id ${SF_CONSUMER_KEY} --username ${SF_USERNAME} --alias ${ORG_ALIAS} --jwt-key-file /tmp/server.key", returnStatus: true)
                println(rc)
                if (rc != 0) {
                    error "Failed to Authorize DevHub"
                }
            }
            stage("Creating Snapshot from Scratch Org") {
                def rc = sh(script: "sf org create snapshot --name ${SNAPSHOT_NAME} --source-org ${SOURCE_SCRATCH_ID} --target-dev-hub ${ORG_ALIAS}", returnStdout: true).trim()
                
                env.snapshotId = sh(script: "echo '${rc}' | awk '/^ Id/ {print \$2}'", returnStdout: true).trim()

                println("Snapshot ID: ${env.snapshotId}")
                
                if (env.snapshotId == " ") {
                    error "Failed to Create Snapshot"
                } 

            }
            stage('Check if Snapshot is Ready') {
                    def snapshotReady = false
                    while (!snapshotReady) {
                        // Ejecutar el comando para obtener el estado del snapshot
                        def rc = sh(script: "sf org get snapshot --snapshot ${env.snapshotId} --target-dev-hub ${ORG_ALIAS}", returnStdout: true).trim()
                        echo "Snapshot Command Output:\n${rc}"

                        // Extraer el estado usando awk
                        def status = sh(script: "echo '${rc}' | awk '/^ Status/ {print \$2}'", returnStdout: true).trim()
                        echo "Current Status: ${status}"

                        // Comprobar si el estado es 'Active'
                        if (status == 'Active') {
                            echo "Snapshot is Active"
                            snapshotReady = true
                        } else {
                            echo "Snapshot is not Active yet. Waiting 120 seconds..."
                            sleep(time: 120, unit: 'SECONDS')
                        }
                    }
        }
            stage("Creating new Scratch Org") {
                sh """
                    echo "{
                    \\"orgName\\": \\"${NEW_SCRATCH_NAME}\\",
                    \\"snapshot\\": \\"${SNAPSHOT_NAME}\\"
                    }" > scratchSnapshot.json
                """

                def rc = sh(script: "sf org create scratch --definition-file scratchSnapshot.json --wait 10 --target-dev-hub ${ORG_ALIAS}", returnStdout: true).trim()
                println(rc)
                
            }
            stage("Deleting Snapshot") {
                def rc = sh(script: "echo yes | sf org delete snapshot --snapshot ${SNAPSHOT_NAME} --target-dev-hub ${ORG_ALIAS}", returnStdout: true).trim()
                println(rc)
                
            }
    }
}