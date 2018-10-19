package com.rundeck.plugin

import com.dtolabs.rundeck.core.execution.ExecutionListener

class CommandBuilder {

    boolean sudoIsRequested
    String sudoPassword
    ExecutionListener logger

    CommandBuilder() {
    }

    CommandBuilder logger(ExecutionListener logger){
        this.logger=logger
        return this
    }

    CommandBuilder sudoPassword(String sudoPassword){
        this.sudoPassword=sudoPassword
        return this
    }

    CommandBuilder sudoIsRequested(boolean sudoIsRequested){
        this.sudoIsRequested=sudoIsRequested
        return this
    }

    def runCommand(List<String> command){
        //Create the process and start it.
        Process process = new ProcessBuilder().command(command)
                                                        .start()

        ThreadedStreamHandler errorPermHandler = new ThreadedStreamHandler(process.getErrorStream(),
                                                                           process.getOutputStream(),
                                                                           sudoPassword,
                                                                           true,
                                                                           sudoIsRequested,
                                                                           logger)

        ThreadedStreamHandler inputPermHandler = new ThreadedStreamHandler(process.getInputStream(),
                                                                           process.getOutputStream(),
                                                                           sudoPassword,
                                                                           false,
                                                                           sudoIsRequested,
                                                                           logger)

        errorPermHandler.start()
        inputPermHandler.start()

        int exitValue = process.waitFor()

        inputPermHandler.interrupt()
        errorPermHandler.interrupt()

        return exitValue
    }
}
