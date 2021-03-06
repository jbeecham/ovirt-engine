#!/usr/bin/python

# Copyright 2012 Red Hat
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Start/stop oVirt Engine
#
# chkconfig: - 65 34
# description: oVirt Engine
# pidfile: /var/run/ovirt-engine.pid

import configobj
import errno
import glob
import grp
import optparse
import os
import pwd
import resource
import shutil
import signal
import stat
import string
import sys
import syslog
import time
import traceback

from Cheetah.Template import Template


# The name of the engine:
engineName = "engine-service"

# The engine system configuration variables:
engineConfig = None

# The name of the user and group that should run the service:
engineUser = None
engineGroup = None
engineUid = 0
engineGid = 0

# Java home directory:
javaHomeDir = None

# Java virtual machine launcher:
javaLauncher = None

# JBoss directories:
jbossHomeDir = None

# JBoss files:
jbossModulesJar = None

# Engine directories:
engineEtcDir = None
engineLogDir = None
engineTmpDir = None
engineUsrDir = None
engineVarDir = None
engineLockDir = None
engineContentDir = None
engineDeploymentsDir = None

# Engine files:
enginePidFile = None
engineLoggingFile = None
engineConfigTemplateFile = None
engineConfigFile = None
engineLogFile = None
engineBootLogFile = None
engineConsoleLogFile = None
engineServerLogFile = None


# Helper class to simplify getting values from the configuration, specially
# from the template used to generate the application server configuration
# file:
class Config(configobj.ConfigObj):
    def __init__(self, files):
        # Initialize ourselves as an empty configuration object:
        configobj.ConfigObj.__init__(self)

        # Merge all the given configuration files, in the same order
        # given, so that the values in one file are overriden by values
        # in files appearing later in the list:
        for file in files:
            config = configobj.ConfigObj(file)
            self.merge(config)

    def getString(self, name):
        text = self.get(name)
        if text is None:
            raise Exception("The parameter \"%s\" doesn't have a value." % name)
        return text

    def getBoolean(self, name):
        text = self.getString(name)
        return text.lower() in ["t", "true", "y", "yes", "1"]

    def getInteger(self, name):
        text = self.getString(name)
        try:
            return int(text)
        except:
            raise Exception("The value \"%s\" of parameter \"%s\" is not a valid integer." % (text, name))


def loadConfig():
    # Locate the defaults file:
    engineDefaultsFile = os.getenv("ENGINE_DEFAULTS", "/usr/share/ovirt-engine/conf/engine.conf.defaults")
    if not os.path.exists(engineDefaultsFile):
        raise Exception("The engine configuration defaults file \"%s\" doesn't exist." % engineDefaultsFile)

    # Locate the configuration file:
    engineConfigFile = os.getenv("ENGINE_VARS", "/etc/sysconfig/ovirt-engine")
    if not os.path.exists(engineConfigFile):
        raise Exception("The engine configuration file \"%s\" doesn't exist." % engineConfigFile)

    # Merge all the configuration files:
    global engineConfig
    engineConfig = Config([
        engineDefaultsFile,
        engineConfigFile,
    ])

    # Get the id of the engine user:
    global engineUser
    global engineUid
    engineUser = engineConfig.getString("ENGINE_USER")
    try:
        engineUid = pwd.getpwnam(engineUser).pw_uid
    except:
        raise Exception("The engine user \"%s\" doesn't exist." % engineUser)

    # Get id of the engine group:
    global engineGroup
    global engineGid
    engineGroup = engineConfig.getString("ENGINE_GROUP")
    try:
        engineGid = grp.getgrnam(engineGroup).gr_gid
    except:
        raise Exception("The engine group \"%s\" doesn't exist." % engineGroup)

    # Java home directory:
    global javaHomeDir
    javaHomeDir = engineConfig.getString("JAVA_HOME")

    # Java launcher:
    global javaLauncher
    javaLauncher = os.path.join(javaHomeDir, "bin/java")

    # JBoss directories:
    global jbossHomeDir
    jbossHomeDir = engineConfig.getString("JBOSS_HOME")

    # JBoss files:
    global jbossModulesJar
    jbossModulesJar = os.path.join(jbossHomeDir, "jboss-modules.jar")

    # Engine directories:
    global engineEtcDir
    global engineLogDir
    global engineTmpDir
    global engineUsrDir
    global engineVarDir
    global engineCacheDir
    global engineLockDir
    global engineServiceDir
    global engineContentDir
    global engineDeploymentsDir
    engineEtcDir = engineConfig.getString("ENGINE_ETC")
    engineLogDir = engineConfig.getString("ENGINE_LOG")
    engineTmpDir = engineConfig.getString("ENGINE_TMP")
    engineUsrDir = engineConfig.getString("ENGINE_USR")
    engineVarDir = engineConfig.getString("ENGINE_VAR")
    engineLockDir = engineConfig.getString("ENGINE_LOCK")
    engineCacheDir = engineConfig.getString("ENGINE_CACHE")
    engineServiceDir = os.path.join(engineUsrDir, "service")
    engineContentDir = os.path.join(engineVarDir, "content")
    engineDeploymentsDir = os.path.join(engineVarDir, "deployments")

    # Engine files:
    global enginePidFile
    global engineLoggingFile
    global engineLogFile
    global jbossConfigTemplateFile
    global jbossConfigFile
    global engineBootLogFile
    global engineConsoleLogFile
    global engineServerLogFile
    enginePidFile = engineConfig.getString("ENGINE_PID")
    engineLoggingFile = os.path.join(engineServiceDir, "engine-service-logging.properties")
    engineLogFile = os.path.join(engineLogDir, "engine.log")
    jbossConfigTemplateFile = os.path.join(engineServiceDir, "engine-service.xml.in")
    jbossConfigFile = os.path.join(engineTmpDir, "engine-service.xml")
    engineBootLogFile = os.path.join(engineLogDir, "boot.log")
    engineConsoleLogFile = os.path.join(engineLogDir, "console.log")
    engineServerLogFile = os.path.join(engineLogDir, "server.log")


def checkIdentity():
    if os.getuid() != 0:
        raise Exception("This script should run with the root user.")


def checkOwnership(name, uid=None, gid=None):
    # Get the metadata of the file:
    st = os.stat(name)

    # Check that the file is owned by the given user:
    if uid and st[stat.ST_UID] != uid:
        user = pwd.getpwuid(uid).pw_name
        owner = pwd.getpwuid(st[stat.ST_UID]).pw_name
        if os.path.isdir(name):
            raise Exception("The directory \"%s\" is not owned by user \"%s\", but by \"%s\"." % (name, user, owner))
        else:
            raise Exception("The file \"%s\" is not owned by user \"%s\", but by \"%s\"." % (name, user, owner))

    # Check that the file is owned by the given group:
    if gid and st[stat.ST_GID] != gid:
        group = grp.getgrgid(gid).gr_name
        owner = grp.getgrgid(st[stat.ST_GID]).gr_name
        if os.path.isdir(name):
            raise Exception("The directory \"%s\" is not owned by group \"%s\", but by \"%s\"." % (name, group, owner))
        else:
            raise Exception("The file \"%s\" is not owned by group \"%s\", but by \"%s\"." % (name, group, owner))


def checkDirectory(name, uid=None, gid=None):
    if not os.path.isdir(name):
        raise Exception("The directory \"%s\" doesn't exist." % name)
    checkOwnership(name, uid, gid)


def checkFile(name, uid=None, gid=None):
    if not os.path.isfile(name):
        raise Exception("The file \"%s\" doesn't exist." % name)
    checkOwnership(name, uid, gid)


def checkLog(name):
    log = os.path.join(engineLogDir, name)
    if os.path.exists(log):
        checkOwnership(log, engineUid, engineGid)


def checkInstallation():
    # Check that the Java home directory exists and that it contais at least
    # the java executable:
    checkDirectory(javaHomeDir)
    checkFile(javaLauncher)

    # Check the required JBoss directories and files:
    checkDirectory(jbossHomeDir)
    checkFile(jbossModulesJar)

    # Check the required engine directories and files:
    checkDirectory(engineEtcDir, uid=engineUid, gid=engineGid)
    checkDirectory(engineLogDir, uid=engineUid, gid=engineGid)
    checkDirectory(engineUsrDir, uid=0, gid=0)
    checkDirectory(engineVarDir, uid=engineUid, gid=engineGid)
    checkDirectory(engineLockDir, uid=engineUid, gid=engineGid)
    checkDirectory(engineServiceDir, uid=0, gid=0)
    checkDirectory(engineContentDir, uid=engineUid, gid=engineGid)
    checkDirectory(engineDeploymentsDir, uid=engineUid, gid=engineGid)
    checkFile(engineLoggingFile)
    checkFile(jbossConfigTemplateFile)

    # Check that log files are owned by the engine user, if they exist:
    checkLog(engineLogFile)
    checkLog(engineBootLogFile)
    checkLog(engineConsoleLogFile)
    checkLog(engineServerLogFile)

    # XXX: Add more checks here!


def loadEnginePid():
    if not os.path.exists(enginePidFile):
        return None
    with open(enginePidFile, "r") as enginePidFd:
        return int(enginePidFd.read())


def saveEnginePid(pid):
    with open(enginePidFile, "w") as enginePidFd:
        enginePidFd.write(str(pid) + "\n")


def removeEnginePid():
    if os.path.exists(enginePidFile):
        os.remove(enginePidFile)


def startEngine():
    # Load the configuration and perform checks:
    loadConfig()
    checkIdentity()
    checkInstallation()

    # Get the PID:
    enginePid = loadEnginePid()

    # If the PID already exists then we need to check if the
    # process is running and tell the user that the service needs
    # to be restarted:
    if enginePid:
        if not os.path.exists("/proc/%d" % enginePid):
            raise Exception(
                "The engine PID file \"%s\" contains %d but "
                "that process doesn't exist. This means that "
                "the engine crashed or was killed. You need "
                "to explicitly run 'service ovirt-engine stop' "
                "and then 'service ovirt-engine start' to "
                "enable it again." %
                (enginePidFile, enginePid))
        else:
            syslog.syslog(syslog.LOG_WARNING,
                "The engine PID file \"%s\" exists and the "
                "process %d is running." %
                (enginePidFile, enginePid))
            return

    # The list of applications to be deployed:
    engineApps = engineConfig.getString("ENGINE_APPS").split()

    for engineApp in engineApps:
        # Do nothing if the application is not available:
        engineAppDir = os.path.join(engineUsrDir, engineApp)
        if not os.path.exists(engineAppDir):
            syslog.syslog(syslog.LOG_WARNING, "The application \"%s\" doesn't exist, it will be ignored." % engineAppDir)
            continue

        # Make sure the application is linked in the deployments directory, if not
        # link it now:
        engineAppLink = os.path.join(engineDeploymentsDir, engineApp)
        if not os.path.islink(engineAppLink):
            syslog.syslog(syslog.LOG_INFO, "The symbolic link \"%s\" doesn't exist, will create it now." % engineAppLink)
            try:
                os.symlink(engineAppDir, engineAppLink)
            except:
                raise Exception("Can't create symbolic link from \"%s\" to \"%s\"." % (engineAppLink, engineAppDir))

        # Remove all existing deployment markers:
        for markerFile in glob.glob("%s.*" % engineAppLink):
            try:
                os.remove(markerFile)
            except:
                raise Exception("Can't remove deployment marker file \"%s\"." % markerFile)

        # Create the new marker file to trigger deployment of the application:
        markerFile = "%s.dodeploy" % engineAppLink
        try:
            markerFd = open(markerFile, "w")
            markerFd.close()
        except:
            raise Exception("Can't create deployment marker file \"%s\"." % markerFile)

    # Clean and recreate the temporary directory:
    if os.path.exists(engineTmpDir):
        shutil.rmtree(engineTmpDir)
    os.mkdir(engineTmpDir)

    # Create cache directory if does not exist
    os.chown(engineTmpDir, engineUid, engineGid)
    if not os.path.exists(engineCacheDir):
        os.mkdir(engineCacheDir)
    os.chown(engineCacheDir, engineUid, engineGid)

    # Generate the main configuration from the template and copy it to the
    # configuration directory making sure that the application server will be
    # able to write to it:
    jbossConfigTemplate = Template(file=jbossConfigTemplateFile, searchList=[engineConfig])
    jbossConfigText = str(jbossConfigTemplate)
    with open(jbossConfigFile, "w") as jbossConfigFd:
        jbossConfigFd.write(jbossConfigText)
    os.chown(jbossConfigFile, engineUid, engineGid)

    # Get heap configuration parameters from the environment or use defaults if
    # they are not provided:
    engineHeapMin = engineConfig.getString("ENGINE_HEAP_MIN")
    engineHeapMax = engineConfig.getString("ENGINE_HEAP_MAX")
    enginePermMin = engineConfig.getString("ENGINE_PERM_MIN")
    enginePermMax = engineConfig.getString("ENGINE_PERM_MAX")

    # Module path should include first the engine modules so that they can override
    # those provided by the application server if needed:
    jbossModulesDir = os.path.join(jbossHomeDir, "modules")
    engineModulesDir = os.path.join(engineUsrDir, "modules")
    engineModulePath = "%s:%s" % (engineModulesDir, jbossModulesDir)

    # We start with an empty list of arguments:
    engineArgs = []

    # Add arguments for the java virtual machine:
    engineArgs.extend([
        # The name or the process, as displayed by ps:
        engineName,

        # Virtual machine options:
        "-server",
        "-XX:+TieredCompilation",
        "-Xms%s" % engineHeapMin,
        "-Xmx%s" % engineHeapMax,
        "-XX:PermSize=%s" % enginePermMin,
        "-XX:MaxPermSize=%s" % enginePermMax,
        "-Djava.net.preferIPv4Stack=true",
        "-Dsun.rmi.dgc.client.gcInterval=3600000",
        "-Dsun.rmi.dgc.server.gcInterval=3600000",
        "-Djava.awt.headless=true",
    ])

    # Add extra system properties provided in the configuration:
    engineProperties = engineConfig.getString("ENGINE_PROPERTIES")
    if engineProperties:
        for engineProperty in engineProperties.split():
            if not engineProperty.startswith("-D"):
                engineProperty = "-D" + engineProperty
            engineArgs.append(engineProperty)

    # Add arguments for remote debugging of the java virtual machine:
    engineDebugAddress = engineConfig.getString("ENGINE_DEBUG_ADDRESS")
    if engineDebugAddress:
        engineArgs.append("-Xrunjdwp:transport=dt_socket,address=%s,server=y,suspend=n" % engineDebugAddress)

    # Enable verbose garbage collection if required:
    engineVerboseGC = engineConfig.getBoolean("ENGINE_VERBOSE_GC")
    if engineVerboseGC:
        engineArgs.extend([
            "-verbose:gc",
            "-XX:+PrintGCTimeStamps",
            "-XX:+PrintGCDetails",
        ])

    # Add arguments for JBoss:
    engineArgs.extend([
        "-Djava.util.logging.manager=org.jboss.logmanager",
        "-Dlogging.configuration=file://%s" % engineLoggingFile,
        "-Dorg.jboss.resolver.warning=true",
        "-Djboss.modules.system.pkgs=org.jboss.byteman",
        "-Djboss.server.default.config=engine-service",
        "-Djboss.home.dir=%s" % jbossHomeDir,
        "-Djboss.server.base.dir=%s" % engineUsrDir,
        "-Djboss.server.config.dir=%s" % engineTmpDir,
        "-Djboss.server.data.dir=%s" % engineVarDir,
        "-Djboss.server.log.dir=%s" % engineLogDir,
        "-Djboss.server.temp.dir=%s" % engineTmpDir,
        "-Djboss.controller.temp.dir=%s" % engineTmpDir,
        "-jar", jbossModulesJar,
        "-mp", engineModulePath,
        "-jaxpmodule", "javax.xml.jaxp-provider",
        "org.jboss.as.standalone", "-c", os.path.basename(jbossConfigFile),
    ])

    # Fork a new process:
    enginePid = os.fork()

    # If this is the parent process then the last thing we have to do is
    # saving the child process PID to the file:
    if enginePid != 0:
        syslog.syslog(syslog.LOG_INFO, "Started engine process %d." % enginePid)
        saveEnginePid(enginePid)
        return

    # Change the resource limits while we are root as we won't be
    # able to change them once we assume the engine identity:
    engineNofile = engineConfig.getInteger("ENGINE_NOFILE")
    resource.setrlimit(resource.RLIMIT_NOFILE, (engineNofile, engineNofile))

    # This is the child process, first thing we do is assume the engine
    # identity:
    os.setgid(engineGid)
    os.setuid(engineUid)

    # Then close standard input and some other security measures:
    os.close(0)
    os.setsid()
    os.chdir("/")

    # Then open the console log and redirect standard output and errors to it:
    engineConsoleFd = os.open(engineConsoleLogFile, os.O_CREAT | os.O_WRONLY | os.O_APPEND, 0660)
    os.dup2(engineConsoleFd, 1)
    os.dup2(engineConsoleFd, 2)
    os.close(engineConsoleFd)

    # Prepare a clean environment:
    engineEnv = {
        "PATH": "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin",
        "LANG": "en_US.UTF-8",
        "ENGINE_ETC": engineEtcDir,
        "ENGINE_LOG": engineLogDir,
        "ENGINE_TMP": engineTmpDir,
        "ENGINE_USR": engineUsrDir,
        "ENGINE_VAR": engineVarDir,
        "ENGINE_CACHE": engineCacheDir,
        "ENGINE_LOCK": engineLockDir,
    }

    # Finally execute the java virtual machine:
    os.execvpe(javaLauncher, engineArgs, engineEnv)


def stopEngine():
    # Load the configuration and perform checks:
    loadConfig()
    checkIdentity()
    checkInstallation()

    # Load the PID:
    enginePid = loadEnginePid()
    if not enginePid:
        syslog.syslog(syslog.LOG_INFO, "The engine PID file \"%s\" doesn't exist." % enginePidFile)
        return

    # First check that the process exists:
    if not os.path.exists("/proc/%d" % enginePid):
        syslog.syslog(syslog.LOG_WARNING, "The engine PID file \"%s\" contains %d, but that process doesn't exist, will just remove the file." % (enginePidFile, enginePid))
        removeEnginePid()
        return

    # Get the time to wait for the engine to stop from the configuration:
    stopTime = engineConfig.getInteger("ENGINE_STOP_TIME")
    stopInterval = engineConfig.getInteger("ENGINE_STOP_INTERVAL")

    # Kill the process softly and wait for it to dissapear or for the timeout
    # to expire:
    os.kill(enginePid, signal.SIGTERM)
    initialTime = time.time()
    timeElapsed = 0
    while os.path.exists("/proc/%d" % enginePid):
        syslog.syslog(syslog.LOG_INFO, "Waiting up to %d seconds for engine process %d to finish." % ((stopTime - timeElapsed), enginePid))
        timeElapsed = time.time() - initialTime
        if timeElapsed > stopTime:
            break
        time.sleep(stopInterval)

    # If the process didn't dissapear after the allowed time then we forcibly
    # kill it:
    if os.path.exists("/proc/%d" % enginePid):
        syslog.syslog(syslog.LOG_WARNING, "The engine process %d didn't finish after waiting %d seconds, killing it." % (enginePid, timeElapsed))
        os.kill(enginePid, signal.SIGKILL)
        syslog.syslog(syslog.LOG_WARNING, "Killed engine process %d." % enginePid)
    else:
        syslog.syslog(syslog.LOG_INFO, "Stopped engine process %d." % enginePid)

    # Remove the PID file:
    removeEnginePid()

    # Clean the temporary directory:
    if os.path.exists(engineTmpDir):
        shutil.rmtree(engineTmpDir)


def checkEngine():
    # Load the configuration:
    loadConfig()

    # First check that the engine PID file exists, if it doesn't
    # then we assume that the engine is not running:
    enginePid = loadEnginePid()
    if not enginePid:
        print("The engine is not running.")
        sys.exit(3)

    # Now check that the process exists:
    if not os.path.exists("/proc/%d" % enginePid):
        print("The engine process %d is not running." % enginePid)
        sys.exit(1)

    # XXX: Here we could check deeper the status of the engine sending a
    # request to the health status servlet.
    print("The engine process %d is running." % enginePid)
    sys.exit(0)


def showUsage():
    print("Usage: %s {start|stop|restart|status}" % engineName)


def prettyAction(label, action):
    # Determine the colors to use according to the type of terminal:
    colorNormal = ""
    colorSuccess = ""
    colorFailure = ""
    moveColumn = ""
    if os.getenv("TERM") in ["linux", "xterm"]:
        colorNormal = "\033[0;39m"
        colorSuccess = "\033[0;32m"
        colorFailure  = "\033[0;31m"
        moveColumn = "\033[60G"

    # Inform that we are doing the job:
    sys.stdout.write(label + " " + engineName + ":")
    sys.stdout.flush()

    # Do the real action:
    try:
        action()
        sys.stdout.write(moveColumn + " [  " + colorSuccess + "OK" + colorNormal + "  ]\n")
    except Exception as exception:
        sys.stdout.write(moveColumn + " [" + colorFailure + "FAILED" + colorNormal + "]\n")
        raise


def performAction(action):
    # The status action is a bit special, as it has some very
    # specific requirements regarding the exit codes, so they are
    # managed inside the function:
    if action == "status":
        try:
            checkEngine()
        except SystemExit:
            raise
        except Exception as exception:
            print(str(exception))
            sys.exit(4)

    # The rest of the actions all work in the same way, if
    # everything goes well finish with exit code zero, otherwise
    # finish with non zero exit code:
    try:
        if action == "start":
            prettyAction("Starting", startEngine)
        elif action == "stop":
            prettyAction("Stopping", stopEngine)
        elif action == "restart":
            prettyAction("Stopping", stopEngine)
            prettyAction("Starting", startEngine)
    except Exception as exception:
        syslog.syslog(syslog.LOG_ERR, str(exception))
        sys.exit(1)
    else:
        sys.exit(0)


def main():
    # Check the arguments:
    args = sys.argv[1:]
    if len(args) != 1:
        showUsage()
        sys.exit(1)
    action = args[0].lower()
    if not action in ["start", "stop", "restart", "status"]:
        showUsage()
        sys.exit(1)

    # Run the action with syslog open and remember to close it
    # regardless of what happens in the middle:
    syslog.openlog(engineName, syslog.LOG_PID)
    try:
        performAction(action)
    finally:
        syslog.closelog()


if __name__ == "__main__":
    main()
