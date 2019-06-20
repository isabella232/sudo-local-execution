package com.rundeck.plugin

import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.plugins.PluginLogger
import com.dtolabs.rundeck.plugins.step.PluginStepContext

class LocalUtil {

    static String getFromKeyStorage(String path, PluginStepContext context){
        return LocalUtil.getFromKeyStorage(path,context.getExecutionContext())
    }

    static String getFromKeyStorage(String path, ExecutionContext context){
        ResourceMeta contents = context.getStorageTree().getResource(path).getContents()
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()
        contents.writeContent(byteArrayOutputStream)
        String password = new String(byteArrayOutputStream.toByteArray())

        return password

    }


    static Map<String, Object> getRenderOpt(  boolean password = false, boolean storagePassword = false, boolean storageKey = false) {
        Map<String, Object> ret = new HashMap<>();
        if(password){
            ret.put("displayType",StringRenderingConstants.DisplayType.PASSWORD)
        }
        if(storagePassword){
            ret.put(StringRenderingConstants.SELECTION_ACCESSOR_KEY,StringRenderingConstants.SelectionAccessor.STORAGE_PATH)
            ret.put(StringRenderingConstants.STORAGE_PATH_ROOT_KEY,"keys")
            ret.put(StringRenderingConstants.STORAGE_FILE_META_FILTER_KEY, "Rundeck-data-type=password")
        }
        if(storageKey){
            ret.put(StringRenderingConstants.SELECTION_ACCESSOR_KEY,StringRenderingConstants.SelectionAccessor.STORAGE_PATH)
            ret.put(StringRenderingConstants.STORAGE_PATH_ROOT_KEY,"keys")
            ret.put(StringRenderingConstants.STORAGE_FILE_META_FILTER_KEY, "Rundeck-key-type=private")
        }

        return ret;
    }

    static def buildSudoCommand(String shell, String username, String command, Boolean login){
        def sudoCommand = "/usr/bin/sudo ${login?"-i":""} -k -S -u ${username} ${command}".toString()
        return [shell, "-c", sudoCommand]

    }

    static def setUserPermissionCommand(String shell, String username, String path){
        return [shell,"-c","/usr/bin/sudo -k  -S chown " + username +  " " +  path]

    }

    static int setUserPermission(String shell, String username, File file, String sudoPassword, boolean sudoIsRequested, ExecutionListener logger){
        //se permissions to username
        def array_permissions = LocalUtil.setUserPermissionCommand(shell,username, file.absolutePath)
        def commander = new CommandBuilder().sudoPassword(sudoPassword)
                                            .sudoIsRequested(sudoIsRequested)
                                            .logger(logger)

        return commander.runCommand(array_permissions)

    }

    static File createTempScriptFile(String rdeckBase, String command){
        String tempPath=rdeckBase + "/var/tmp"
        File file = File.createTempFile("temp",".sh", new File(tempPath))
        file.setExecutable(true)
        file.write command

        return file
    }

}
