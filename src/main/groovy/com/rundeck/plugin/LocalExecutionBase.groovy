package com.rundeck.plugin

import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.plugins.step.PluginStepContext

class LocalExecutionBase {

    Boolean sudoIsRequested = false
    String sudoPassword
    String username
    String shell
    ExecutionListener logger
    Boolean login
    String rdeckBase


    def process(String[] command){
        return this.process(command.join(' '))
    }

    def process(String command){
        File file = LocalUtil.createTempScriptFile(rdeckBase, command)

        def commandArray = this.setExecutionPermissions(file,
                                                        username,
                                                        shell,
                                                        sudoIsRequested,
                                                        sudoPassword)

        def commander = new CommandBuilder().sudoPassword(sudoPassword)
                                            .sudoIsRequested(sudoIsRequested)
                                            .logger(logger)

        int exitValue = commander.runCommand(commandArray)

        logger.log(5,"[debug] Exit Code: "+ exitValue)

        file?.delete()

        return exitValue
    }

    def setExecutionPermissions(File file, String username, String shell, boolean sudoIsRequested, String sudoPassword){
        //run commands as rundeckuser
        def commandArray = [shell,"-c",file.absolutePath]
        //if username is defined on the local node, the command will run as another user
        if(sudoIsRequested){
            int exitValue = LocalUtil.setUserPermission(shell,
                                                        username,
                                                        file,
                                                        sudoPassword,
                                                        sudoIsRequested,
                                                        logger)

            logger.log(5,"[debug] Exit Code Permissions: "+ exitValue)

            if (exitValue!=0) {
                logger.log(0, "Error creating temporary file")
            }

            //running file as username
            commandArray = []
            commandArray.addAll(LocalUtil.buildSudoCommand(shell, username, file.absolutePath, login))
        }

        logger.log(5,"[debug] Running command : "+ commandArray.toString())
        return commandArray
    }

}
