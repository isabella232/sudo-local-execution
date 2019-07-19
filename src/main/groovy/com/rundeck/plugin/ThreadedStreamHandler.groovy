package com.rundeck.plugin

import com.dtolabs.rundeck.core.execution.ExecutionLogger

import java.util.regex.Matcher
import java.util.regex.Pattern

class ThreadedStreamHandler extends Thread{
    InputStream inputStream
    String adminPassword
    OutputStream outputStream
    OutputStreamWriter printWriter
    boolean sudoIsRequested = false
    ExecutionLogger logger
    boolean isErrorStream=false

    List<String> sudoPatterns = ["^Password:(.*)","^\\[sudo\\] password for ([^:]+):\\s(.*)"]

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
        BufferedReader bufferedReader = null
        try
        {

            if (sudoIsRequested)
            {
                printWriter.println(adminPassword)
                printWriter.flush()
            }

            bufferedReader = new BufferedReader(new InputStreamReader(inputStream))
            bufferedReader.eachLine {line->

                if(isErrorStream){

                    if(checkSudoPattern(line)){
                        String message = replacePattern(line)
                        if(message){
                            logger.log(0,  message)
                        }
                    }else{
                        logger.log(0,line)
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

    boolean checkSudoPattern(def line){
        boolean status = sudoPatterns.stream().anyMatch({pattern->
            Pattern.compile(pattern).matcher(line).matches()
        })
        status
    }

    String replacePattern(def line){
        String replace = line
        for(String pattern:sudoPatterns){
            if(Pattern.compile(pattern).matcher(line).matches()){
                Matcher matcher = Pattern.compile(pattern).matcher(line)
                int count = matcher.groupCount()

                if (matcher.find()) {
                    String find = matcher.group(count)
                    if(find.isEmpty()){
                        replace=null
                    }else{
                        replace=find
                    }
                }
                return replace
            }

        }
        replace
    }

}
