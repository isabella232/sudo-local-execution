package com.rundeck.plugin

import com.dtolabs.rundeck.core.execution.ExecutionLogger

class ThreadedStreamHandler extends Thread{
    InputStream inputStream
    String adminPassword
    OutputStream outputStream
    OutputStreamWriter printWriter
    boolean sudoIsRequested = false
    ExecutionLogger logger
    boolean isErrorStream=false

    ThreadedStreamHandler(InputStream inputStream,
                          OutputStream outputStream,
                          String adminPassword,
                          boolean isErrorStream,
                          boolean sudoIsRequested,
                          ExecutionLogger logger)
    {
        this.inputStream = inputStream
        this.outputStream = outputStream
        this.printWriter = new OutputStreamWriter(outputStream)
        this.adminPassword = adminPassword
        this.logger = logger
        this.sudoIsRequested = sudoIsRequested
        this.isErrorStream=isErrorStream
    }

    void run()
    {
        if (sudoIsRequested)
        {
            printWriter.println(adminPassword)
            printWriter.flush()
        }

        BufferedReader bufferedReader = null
        try
        {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream))
            bufferedReader.eachLine {line->

                if(isErrorStream){
                    if(line.contains("Password:") || line.contains("[sudo] password")){
                        logger.log(5, line)
                    }else{
                        logger.log(0, line)
                    }

                }else{
                    logger.log(2, line)
                }

                //handling sudo password error
                if (line.contains("Sorry, try again")) {
                    logger.log(0, line)
                    printWriter.println(adminPassword)
                    printWriter.flush()
                }

            }
        }
        catch (Exception ioe){
            logger.log(0, ioe.getMessage())
        }
        finally
        {
            try{
                bufferedReader.close()
            }catch (IOException e)
            {
                logger.log(0, e.getMessage())
            }
        }
    }



}
