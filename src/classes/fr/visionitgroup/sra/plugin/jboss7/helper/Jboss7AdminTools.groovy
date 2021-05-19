package fr.visionitgroup.sra.plugin.jboss7.helper;
import fr.visionitgroup.sra.plugin.jboss7.helper.Jboss7CLIConnection;
import org.jboss.as.cli.scriptsupport.*


public class Jboss7AdminTools {
	
	
	public static List getDataSource(def Jboss7CLIConnection cli, def String profile ){
		def listOfObj=[]
		def path=""
		if (cli.isDomainMode()){
			path="/profile=$profile/subsystem=datasources"
		}
		else{
			path="/subsystem=datasources"
		}
		
		def CLI.Result result=cli.runCLICommand("cd $path")
		result=cli.runCLICommand(":read-resource")
		def response = result.getResponse()
		def responseResult = response.get("result")
		def propertyList= responseResult.get("data-source").asPropertyList()
		propertyList.iterator().each {
			prop->	listOfObj.add(path+"/"+prop.getName())
		}
		
		return listOfObj
	}
	public static List getJdbcDriver(def Jboss7CLIConnection cli, def String profile ){
		def listOfObj=[]
		def path=""
		if (cli.isDomainMode()){
			path="/profile=$profile/subsystem=datasources"
		}
		else{
			path="/subsystem=datasources"
		}
		
		def CLI.Result result=cli.runCLICommand("cd $path")
		result=cli.runCLICommand(":read-resource")
		def response = result.getResponse()
		def responseResult = response.get("result")
		def propertyList= responseResult.get("jdbc-driver").asPropertyList()
		propertyList.iterator().each {
			prop->	listOfObj.add(path+"/"+prop.getName())
		}
		
		return listOfObj
	}
	public static List getXADataSource(def Jboss7CLIConnection cli, def String profile ){
		def listOfObj=[]
		def path=""
		if (cli.isDomainMode()){
			path="/profile=$profile/subsystem=datasources"
		}
		else{
			path="/subsystem=datasources"
		}
		
		
		
		def CLI.Result result=cli.runCLICommand("cd $path")
		result=cli.runCLICommand(":read-resource")
		def response = result.getResponse()
		def responseResult = response.get("result")
		def propertyList= responseResult.get("xa-data-source").asPropertyList()
		propertyList.iterator().each {
			prop->	listOfObj.add(path+"/"+prop.getName())
		}
		
		return listOfObj
	}
	
	def static int isExistResource(def Jboss7CLIConnection cli,def resource) {
		// test if ressource existe
		def command="$resource:read-resource"
		def result=cli.runCLICommand(command)
		if (result.isSuccess()){
		   return 0
		}
		else{
			return 1
		}
		
		return 1
	}
	
	def static int deleteResource(def Jboss7CLIConnection cli,def resource) {
		// test if ressource existe
		def command="$resource:read-resource"
		def result=cli.runCLICommand(command)
		if (result.isSuccess()){
		   command="$resource:remove"
		   result=cli.runCLICommand(command)
		   if (result.isSuccess()){
			   println("[S]:delete resource:$resource")
			   return 0
		   }
		   else{
				def response = result.getResponse()
				println("[E]:delete resource:$resource:"+response.asString())
				return 1
		   }
		}
		else{
			println("[W]:unknown resource:$resource: cancel Deletion")
			return 0
		}
		
		return 1
	}
	
	def static int addResource(def Jboss7CLIConnection cli,def command) {
		// test if ressource existe
		try{
			def result=cli.runCLICommand(command)
			if (result.isSuccess()){
				println("[S]:add resource:$command")
				return 0
			}
		   else{
				def response = result.getResponse()
				println("[E]:add resource:$command:"+response.asString())
				return 1
		   }
		}
		catch (Exception e){
			println("[E]:add resource:$command:"+e.getMessage())
			return 1
		}

		return 1
	}
	
	def static int deleteGroupOfResources(def Jboss7CLIConnection cli,def List resourceLst,def Boolean _reverse) {
		def status=0
		if (_reverse){resourceLst=resourceLst.reverse()}
		//println("resourceLst="+resourceLst)
		resourceLst.each{ 
			//println("it=$it")
			def cr=Jboss7AdminTools.deleteResource (cli,it)
			if (cr != 0 ){ status=1}
		}
		return status
	}
	
	def static int addGroupOfResources(def Jboss7CLIConnection cli,def List commandLst) {
		def status=0
		
		//println("commandLst="+commandLst)
		commandLst.each{
			//println("it=$it")
			def cr=Jboss7AdminTools.addResource (cli,it)
			if (cr != 0 ){ status=1}
		}
		return status
	}
	
	public static int checkDeployEnvironment(def Jboss7CLIConnection cli, def String commandlinesStr){
		def listOfAlreadyExistedRessources=[]
		def listOfCreatedRessources=[]
		def int cptline=0
		try {
			commandlinesStr.eachLine { line ->
				cptline++
				if(line ==~ / *#.*/ ){
					
				}
				else if(line ==~ / *\/.*:.*(.*) */ ){
					def obj=line.replaceAll("^ *", "").replace("\\:", "\\;;!!**;;!!@").split(":")[0].replace( "\\;;!!**;;!!@","\\:")
					//listOfRessources.add(line)
				}
				else{
					throw new Exception ("[E]: Invalid Syntax at line $cptline: $line")
				}
			}
		}
		catch (Exception e){
			println(e.getMessage())
			return 1
		}
		return 0
	}
	public static int deployEnvironment(def Jboss7CLIConnection cli, def String commandlinesStr){
		def commandlineAddLst=[]
		commandlinesStr.eachLine { line ->
			if(line ==~ / *\/.*:.*(.*) */ ){
				commandlineAddLst.add(line)
			}
		}
		//println("deployEnvironment:"+filtredCommandlineAddScript)
		def cr=addGroupOfResources(cli,commandlineAddLst)
		return cr
	}
	
	public static int undeployEnvironment(def Jboss7CLIConnection cli, def String commandlinesStr){
		def commandlineAddLst=[]
		commandlinesStr.eachLine { line ->
			if(line ==~ / *\/.*:add(.*)/ ){
				line=line.replaceAll("^ *", "").replace("\\:", "\\;;!!**;;!!@").split(":")[0].replace( "\\;;!!**;;!!@","\\:")
				commandlineAddLst.add(line)
			}
			else{
				def commande=replaceAll(" *","").replaceAll(" *.*","")
				if(line ==~ / *connection-factory .* *add *.*|^ *data-source .* *add *.*| *deployment-overlay .* *add *.*| *jms-queue .* *add *.*| *jms-topic .* *add *.*| *xa-data-source .* *add *.*/ ){
					def name=line.replaceAll(".* --name=","").replaceAll(" *--.*=.*","")
					def profile=line.replaceAll(".* --profil=","").replaceAll(" *--.*=.*","")
					
					commandlineAddLst.add("")
				}
			}
		}
		def cr=deleteGroupOfResources(cli,commandlineAddLst, true)
		return cr
	}
	
	/*
	 * @deleteGroupOfDataSouces(profile,grpname_regexp)
@deleteAllOfDataSouces(profile,grpname_regexp)
@deleteSpecificDataSouce(profile,datasourcename)

@deleteGroupOfJdbcDriver(profile,grpname_regexp)
@deleteAllOfJdbcDriver(profile,grpname_regexp)
@deleteSpecificJdbcDriver(profile,datasourcename)

@deleteGroupOfConnectionFactory(profile,grpname_regexp)
@deleteAllOfConnectionFactory(profile,grpname_regexp)
@deleteSpecificConnectionFactory(profile,datasourcename)

@deleteGroupOfQueue(profile,grpname_regexp)
@deleteAllOfQueue(profile,grpname_regexp)
@deleteSpecificQueue(profile,datasourcename)

@deleteGroupOfTopic(profile,grpname_regexp)
@deleteAllOfTopic(profile,grpname_regexp)
@deleteSpecificTopic(profile,datasourcename)

@deleteServerGroupAndAllInstance(profile,ServerGroupName)
@addServerGroup(profile,ServerGroupName,add_paramater)
@deleteSpecificResource(profile,datasourcename)


deployGroupOfApplication(group-server,lstOfApplication(name,/path_src,), disable_option )
disableGroupOfApplication
enableGroupOfApplication
undeployGroupOfApplication(,keep-content_option)

stateofGroupofApplication
waitIsReadyForGroupOfApplication

start/stop/restart/AStar/Astop/ArestartInstance
start/stop/restart/AStar/Astop/ArestartGroupInstance
start/stop/restart/AStar/Astop/ArestartServerGroup
*
**/
	static int astartGroupOfDomainInstance(def Jboss7CLIConnection cli, def String[] path_instanceLst){
		def status=0
		path_instanceLst.each{ 
			it -> def cr=astartDomainInstance(  cli, it,"","")
			if (cr!= 0){status=1}
			
		}
		return status
	}
	 static int astartDomainInstance(def Jboss7CLIConnection cli, def String path_instance, def String host="", def String  instance=""){
		 println("[A] Async Start Domain Instance ")
		 def response=""
		 
		 if (path_instance == ""){
			 path_instance="/host=$host/server-config=$instance"
		 }
		 
		 def commandline=""
		 
		 // check that instance 's status is STOPPED
		 commandline="$path_instance:read-attribute(name=status)"
		 def result=cli.runCLICommand(commandline)
		 if (result.isSuccess()){
			 response = result.getResponse().get("result").asString()
			 println("response=$response")
			 if(response != "STOPPED"){
			    println("[E]:Instance $path_instance is not stopped (status=$response). It's must be STOPPED before started")
				return 1
			 }
			 
		 }
		 else{
			 println("[E]: Impossible to control the status of instance: $path_instance")
			 return 1
			 
		 }
		 
		 // try to start instance
		 commandline="$path_instance:start"
		 result=cli.runCLICommand(commandline)
		 if (result.isSuccess()){
			 println("[S]:start $path_instance")
			 return 0
		 }
		else{
			 response = result.getResponse()
			 println("[E]:start $path_instance:"+response.asString())
			 return 1
		}
		
		
		
	 }
	 
	 static int astopGroupOfDomainInstance(def Jboss7CLIConnection cli, def String[] path_instanceLst){
		 def status=0
		 path_instanceLst.each{
			 it -> def cr=astopDomainInstance(  cli, it, "","")
			 if (cr!= 0){status=1}
			 
		 }
		 return status
	 }
	 static int astopDomainInstance(def Jboss7CLIConnection cli, def String path_instance, def String host="", def String  instance=""){
		 println("[A] Async Stop Domain Instance ")
		 def response=""
		 
		 if (path_instance == ""){
			 path_instance="/host=$host/server-config=$instance"
		 }
		 
		 def commandline=""
		 
		 // check that instance 's status is STOPPED
		 commandline="$path_instance:read-attribute(name=status)"
		 def result=cli.runCLICommand(commandline)
		 if (result.isSuccess()){
			 response = result.getResponse().get("result").asString()
			 println("response=$response")
			 if(response != "STARTING" && response != "STARTED"){
				println("[E]:Instance $path_instance is not starting or started (status=$response). Instance can't be stopped")
				return 1
			 }
			 
		 }
		 else{
			 println("[E]: Impossible to control the status of instance: $path_instance")
			 return 1
		 }
		 
		 // try to start instance
		 commandline="$path_instance:stop"
		 result=cli.runCLICommand(commandline)
		 if (result.isSuccess()){
			 println("[S]:stop $path_instance")
			 return 0
		 }
		else{
			 response = result.getResponse()
			 println("[E]:stop $path_instance:"+response.asString())
			 return 1
		}
	 }
	 
	 static String statusGroupOfDomainInstance(def Jboss7CLIConnection cli, def String[] path_instanceLst){
		 def status=0
		 path_instanceLst.each{
			 it -> def cr=statusDomainInstance(  cli, it, "","")
			 if (cr!= 0){status=1}
			 
		 }
		 return status
	 }
	 static String statusDomainInstance(def Jboss7CLIConnection cli, def String path_instance, def String host="", def String  instance=""){
		 
		 def response="FAILED"
		 
		 if (path_instance == ""){
			 path_instance="/host=$host/server-config=$instance"
		 }
		 
		 def commandline=""
		 
		 // check that instance 's status is STOPPED
		 commandline="$path_instance:read-attribute(name=status)"
		 def result=cli.runCLICommand(commandline)
		 if (result.isSuccess()){
			 response=result.getResponse().get("result").asString()
			 
			 
		 }
		 else{
			 println("[E]: Impossible to control the status of instance: $path_instance")
			 return 1
		 }
		 
		 return response
	 }
	 
	 static int arestartGroupOfDomainInstance(def Jboss7CLIConnection cli,def timeout, def String[] path_instanceLst){
		 def status=0
		 path_instanceLst.each{
			 it -> def cr=arestartDomainInstance(  cli,timeout, it, "","")
			 if (cr!= 0){status=1}
			 
		 }
		 return status
	 }
	 static int arestartDomainInstance(def Jboss7CLIConnection cli, def timeout,def String path_instance, def String host="", def String  instance=""){
		 println("[A] Async ReStart Domain Instance ")
		 def response
		 def result
		 def cr
		 def state="UNKNOW"
		 if (path_instance == ""){
			 path_instance="/host=$host/server-config=$instance"
		 }
		 
		 state=statusDomainInstance( cli, path_instance, host,  instance)
		 switch(state){
			 case "STOPPING":
							 println("[I]:Instance $path_instance is already stopping");
							 cr=stopDomainInstance( cli, timeout,path_instance, host,  instance)
							 break;
			 case "STOPPED":
							 println("[I]:Instance $path_instance is already stopped");
							 cr=0;
							 break;
			 case "STARTING":
							 println("[I]:Instance $path_instance is stopping");
							 cr=stopDomainInstance( cli, timeout,path_instance, host,  instance)
							 break;
			 case "STARTED":
							 println("[I]:Instance $path_instance is started");
							 cr=stopDomainInstance( cli, timeout,path_instance, host,  instance)
							 break;
			 case "FAILED": println("[E]:Instance $path_instance has state:$state"); return 1;break;
			 default: println("[E]:Instance $path_instance has unknown state:$state"); return 1;break;
		 }
		 
		 if(cr!= 0){
			 println("[E]:Impossible to stop $path_instance. Abort Restart operation...");
			 return 1
		 }
		 cr=astartDomainInstance( cli,path_instance, host,  instance)
		 return cr
	 }
	 
	 static int startGroupOfDomainInstance(def Jboss7CLIConnection cli,def timeout, def String[] path_instanceLst){
		 def status=0
		 path_instanceLst.each{
			 it -> def cr=startDomainInstance(  cli,timeout, it,"","")
			 if (cr!= 0){status=1}
			 
		 }
		 return status
	 }
	 static int startDomainInstance(def Jboss7CLIConnection cli, def timeout,def String path_instance, def String host="", def String  instance=""){
		 println("[A] Start Domain Instance ")
		 def response
		 def state
		 def result
		 if (path_instance == ""){
			 path_instance="/host=$host/server-config=$instance"
		 }
		 
		 def commandline=""
		 
		 // check that instance 's status is STOPPED
		 state=statusDomainInstance( cli, path_instance, host,  instance)
		 switch(state){
			 case "STOPPING": println("[W]:Instance $path_instance is still stopping"); break; break;
			 case "STOPPED": break
			 case "STARTING": println("[W]:Instance $path_instance is already starting"); break; break;
			 case "STARTED": println("[E]:Instance $path_instance is already started"); return 1;break;
			 case "FAILED": println("[E]:Instance $path_instance has state:$state"); return 1;break;
			 default: println("[E]:Instance $path_instance has unknown state:$state"); return 1;break;break;
		 }
		 
		 // try to start instance
		 commandline="$path_instance:start"
		 result=cli.runCLICommand(commandline)
		 if (result.isSuccess()){
			 println("[S]:start request 's been send to $path_instance")
		 }
		else{
			 response = result.getResponse()
			 println("[E]:start request can't be send to $path_instance:"+response.asString())
			 return 1
		}
		
		// wait for state instance equal to starting or started
		
		
		def startTime = System.currentTimeMillis()
		
		while (state!= "STARTING" && state!= "STARTED" )   {
			println("[W] Instance $path_instance is still starting (state=$state). Wait for state=STARTED (retry in 5 sec... )")
			sleep(5000)
			state=statusDomainInstance( cli, path_instance, host,  instance)
			
			if (state=="FAILED"  ){
				println("[E] start Instance $path_instance : FAILED ")
				break
			}
			
			println System.currentTimeMillis() - startTime +"/"+ timeout
			if( timeout != 0 && System.currentTimeMillis() - startTime >= timeout){
				println("[E] !!! TIMEOUT !!!")
				break;
			}
		
		}
		if (state == "STARTING"){
			while ( state!= "STARTED" )   {
				println("[W] Instance $path_instance is still starting (state=$state). Wait for state=STARTED (retry in 5 sec... )")
				sleep(5000)
				state=statusDomainInstance( cli, path_instance, host,  instance)
				
				if (state=="FAILED"  ){
					println("[E] start Instance $path_instance : FAILED ")
					break
				}
				
				println System.currentTimeMillis() - startTime +"/"+ timeout
				if( timeout != 0 && System.currentTimeMillis() - startTime >= timeout){
					println("[E] !!! TIMEOUT !!!")
					break;
				}
			
			}
		}
		// END Status
		state=statusDomainInstance( cli, path_instance, host,  instance)
		 switch(state){
			 
			 case "STARTED": println("[S]:Instance $path_instance is  started"); return 0;break;
			 case "STOPPING":
			 case "STARTING":
			 case "STOPPED": println("[W]:Instance $path_instance is $state (Timeout)"); return 1;break;
			 case "FAILED": println("[E]:Instance $path_instance has state:$state"); return 1;break;
			 default: println("[E]:Instance $path_instance has unknown state:$state"); return 1;break;break;
		 }
		 return 1
		
		
	 }
	 static int restartGroupOfDomainInstance(def mode,def Jboss7CLIConnection cli,def timeout, def String[] path_instanceLst){
		 def status=0
		 switch (mode){
			 case "OneByOne":
				 path_instanceLst.each{
					 it -> def cr=restartDomainInstance(  cli,timeout, it, "","")
					 if (cr!= 0){status=1}
					 
				 }
			 	break;
				 
			 case "StopAllAndStartAll":
				 path_instanceLst.each{
					 it -> 
					 def state=statusDomainInstance( cli, it, host,  instance)
					 switch(state){
						 
						 case "STARTED": 
							 def cr=stopDomainInstance(  cli,timeout, it, host, instance)
							 if (cr!= 0){status=1}
							 break
						
						 case "stopping":
						 case "STOPPING":
						 case "starting":
						 case "STARTING":
							 println "[I] Domaine Instance $it is $state. Wait for the stable state (STARTING | STOPPING)..."
							 def startTime = System.currentTimeMillis()
							 def isStable=false
							 while (isStable && System.currentTimeMillis() - startTime < timeout) {
								  state=statusDomainInstance( cli, it, host,  instance)
								  switch (state){
									  case "stopped":
									  case "STOPPED":
									  case "started":
									  case "STARTED":isStable=true; break
									  default:
											  isStable=false;
											  println "[I] Domaine Instance $it is still $state. Retry in 5 sec ..."
											  sleep(5000)
								  }
								 
							 }
							 if (isStable){
								 println "[I] Domaine Instance $it has a stable state (STARTED | STOPPED)"
								 if (state =="started" ||state =="STARTED" ){
									 def cr=stopDomainInstance(  cli,timeout, it, host, instance)
									 if (cr!= 0){status=1}
								 }
								 else{
									 println("[I]:Instance $it is now stopped");
								 }
								 
							 }
							 else{
								 println "[W] Domaine Instance $it hasn't a stable state (running | stopped). Try to shutdown, then restart with no garantee  "
								 def cr=stopDomainInstance(  cli,timeout, it, host, instance)
								 return 1
							 }
						 	break;
						 case "STOPPED": println("[I]:Instance $it is already stopped"); ;break;
						 case "FAILED": println("[W]:Instance $it has state:$state"); break;
						 default: println("[E]:Instance $it has unknown state:$state"); break;
					 }
					 
					 
				 }
				 path_instanceLst.each{
					 it -> def cr=startDomainInstance(  cli,timeout, it, host, instance)
					 if (cr!= 0){status=1}
					 
				 }
			 	break;
				 default:
				 break;
				 
		 }
		 
		 return status
	 }
	 static int restartDomainInstance(def Jboss7CLIConnection cli, def timeout,def String path_instance, def String host="", def String  instance=""){
		 println("[A] ReStart Domain Instance ")
		 def response
		 def result
		 def cr
		 def state="UNKNOW"
		 if (path_instance == ""){
			 path_instance="/host=$host/server-config=$instance"
		 }
		 
		 state=statusDomainInstance( cli, path_instance, host,  instance)
		 switch(state){
			 case "STOPPING": 
			                 println("[I]:Instance $path_instance is already stopping");
							 cr=stopDomainInstance( cli, timeout,path_instance, host,  instance)
			                 break;
			 case "STOPPED": 
			 				println("[I]:Instance $path_instance is already stopped");
							 cr=0;
							 break;
			 case "STARTING": 
							 println("[I]:Instance $path_instance is stopping");
							 cr=stopDomainInstance( cli, timeout,path_instance, host,  instance)
							 break;
			 case "STARTED": 
							 println("[I]:Instance $path_instance is started");
							 cr=stopDomainInstance( cli, timeout,path_instance, host,  instance)
							 break;
			 case "FAILED": println("[E]:Instance $path_instance has state:$state"); return 1;break;
			 default: println("[E]:Instance $path_instance has unknown state:$state"); return 1;break;
		 }
		 
		 if(cr!= 0){
			 println("[E]:Impossible to stop $path_instance. Abort Restart operation...");
			 return 1
		 }
		 cr=startDomainInstance( cli, timeout,path_instance, host,  instance)
		 return cr
	 }
	 static int stopDomainInstance(def Jboss7CLIConnection cli, def timeout,def String path_instance, def String host="", def String  instance=""){
		 println("[A]  Stop Domain Instance ")
		 def response
		 def result
		 def state="UNKNOW"
		 if (path_instance == ""){
			 path_instance="/host=$host/server-config=$instance"
		 }
		 
		 def commandline=""
		 
		 // check that instance 's status is STOPPED
		 state=statusDomainInstance( cli, path_instance, host,  instance)
		 switch(state){
			 case "STOPPING": println("[W]:Instance $path_instance is already stopping"); break;
			 case "STOPPED": println("[E]:Instance $path_instance is already stopped"); return 1;break;
			 case "STARTING": break;
			 case "STARTED": break;
			 case "FAILED": println("[E]:Instance $path_instance has state:$state"); return 1;break;
			 default: println("[E]:Instance $path_instance has unknown state:$state"); return 1;break;break;
		 }
		 
		 
		 // try to stop instance
		 commandline="$path_instance:stop"
		 result=cli.runCLICommand(commandline)
		 if (result.isSuccess()){
			 println("[S]:stop request 's been send to $path_instance")

		 }
		else{
			 response = result.getResponse()
			 println("[E]:stop request can't been send to $path_instance:"+response.asString())
			 return 1
		}
		
		// wait for state instance equal to stopping or Stopped
		def startTime = System.currentTimeMillis()
		while (state!= "STOPPING" && state!= "STOPPED" )   {
			println("[W] Instance $path_instance is still stopping (state=$state). Wait for state=STOPPED (retry in 5 sec... )")
			sleep(5000)
			state=statusDomainInstance( cli, path_instance, host,  instance)
			
			if (state=="FAILED"  ){
				println("[E] stop Instance $path_instance : FAILED ")
				break
			}
			
			println System.currentTimeMillis() - startTime +"/"+ timeout
			if( timeout != 0 && System.currentTimeMillis() - startTime >= timeout){
				println("[E] !!! TIMEOUT !!!")
				break;
			}
		
		}
		if (state == "STOPPING"){
			while ( state!= "STOPPED" )   {
				println("[W] Instance $path_instance is still stopping (state=$state). Wait for state=STOPPED (retry in 5 sec... )")
				sleep(5000)
				state=statusDomainInstance( cli, path_instance, host,  instance)
				
				if (state=="FAILED"  ){
					println("[E] stop Instance $path_instance : FAILED ")
					break
				}
				
				println System.currentTimeMillis() - startTime +"/"+ timeout
				if( timeout != 0 && System.currentTimeMillis() - startTime >= timeout){
					println("[E] !!! TIMEOUT !!!")
					break;
				}
			
			}
		}
		// END Status
		state=statusDomainInstance( cli, path_instance, host,  instance)
		 switch(state){
			 
			 case "STOPPED": println("[S]:Instance $path_instance is  stopped"); return 0;break;
			 case "STOPPING": 
			 case "STARTING": 
			 case "STARTED": println("[W]:Instance $path_instance is $state (Timeout)"); return 1;break;
			 case "FAILED": println("[E]:Instance $path_instance has state:$state"); return 1;break;
			 default: println("[E]:Instance $path_instance has unknown state:$state"); return 1;break;break;
		 }
		 return 1
		
	 }
	 
	 static def int startStandaloneServer(def Jboss7CLIConnection cli,def String exec_path,def List exec_args, def File exec_workdir, def Map exec_env, def timeout) {
		
		 println("[A] Start Standalone Server")
		 def cmdArgs =[exec_path]
		cmdArgs.addAll(exec_args)
		
		println "cmdArgs"+cmdArgs
		println "[I] Builting new process;  "+cmdArgs
     
	 
		
		def builder = new com.urbancode.process.jdk7.ProcessBuilder(cmdArgs)
        
		//def outputFile = System.out
		if (exec_env!= null && exec_env.size()> 0){
			Map<String, String> env = builder.environment();
			println "[I] Add System Environment"+exec_env 
			env.putAll(exec_env)
		}
		
		if (exec_workdir == null){
			exec_workdir=new File(".")
		}
		println "[I] Set Working Dir to "+exec_workdir.absolutePath
		builder.directory(exec_workdir);
		
		def outputFile = new File(exec_workdir, "outputStartFile.txt")
		println "[D] Set outputStartFile to "+outputFile.absolutePath
		builder.redirectOutput(outputFile)
		builder.redirectErrorStream(true)
		/*builder.redirectInput(outputFile)
		builder.redirectError(outputFile)
		*/
		
		
		
		def host=cli.getControllerHost()
		def port=cli.getControllerPort()
		def user=cli.getUserMgmt()
		def controller_infos
		if (user== null){
			controller_infos=host+":"+port+",no user / no passwd"
		}
		else{
			controller_infos=host+":"+port+","+user+" / *****"
		}
		
		println "[I] Check If another instance already started (meaning that controller is listen on $host:$port )  "
        def s = new Socket()
        def addr = new InetSocketAddress(host, port)
        
        try {
            s.connect(addr)
        }
        catch (IOException e) {
            println("[I] Standalone Server seem to be down")
        }
        finally {
            if (s != null) {
                if (s.isConnected()) {
                   println "[I] The server is already running, exiting with success"
                   s.close()
                   return 1
                }
                else {
                    s.close()
                }
            }
        }
        
		println "[I] Start  process;  "
		Process process=builder.start()
		if (process.errorStream != null ){process.errorStream.close()}
		//process.inputStream.close()
		if (process.outputStream != null ){process.outputStream.close()}
		
		println "[I] Wait until obtain a CLI Connection"
        def startTime = System.currentTimeMillis()
        def gotConnection = false        
        
        while (!gotConnection && System.currentTimeMillis() - startTime < timeout) {
            try {
                cli.connect()
                gotConnection = true
				break
            }
            catch (Exception e) {
                println("[I] Could not establish a CLI Connection!:$controller_infos . (Retry in 5 sec)")
				sleep(5000)
            }
            
        }
        
        if (!gotConnection) {
            throw new Exception("[E] Server not started in a timely manner.")
			return 1
        }
		
		
		// /:read-attribute(name=server-state)
		def response="unknown"
		def isRunning=false
		while (!isRunning && System.currentTimeMillis() - startTime < timeout) {
			def commandline="/:read-attribute(name=server-state)"
			
			def result=cli.runCLICommand(commandline)
			if (result.isSuccess()){
				 response=result.getResponse().get("result").asString()
				 println("[I]: state of server:$response")
				 if (response == "running"){
					 isRunning=true
					 break
				 }
				 
			}
			else{
				 println("[W]: Impossible to control the state of server")
			}
			
			sleep(5000)
		}
		
		
		
        if(isRunning){
            println "[S] Achieved a connection to the server. Server has finished to start"
			return 0
        }
		else{
			println "[E] Achieved a connection to the server. But Server hasn't finished to start after timeout (state still : $response ) "
			return 1
		}
        
        
        return 1
    }
	 
	 static def int stopStandaloneServer(def Jboss7CLIConnection cli,def int timeout){
		 println("[A] Stop Standalone Server")
		 def host=cli.getControllerHost()
		 def port=cli.getControllerPort()
		 def user=cli.getUserMgmt()
		 def controller_infos
		 if (user== null){
			 controller_infos=host+":"+port+",no user / no passwd"
		 }
		 else{
			 controller_infos=host+":"+port+","+user+" / *****"
		 }
		 
		 println "[I] Check If Standalone server is alived (listen on $controller_infos)  "
		 def gotConnection=false
		 def s = new Socket()
		 def addr = new InetSocketAddress(host, port)
		 
		 try {
			 s.connect(addr)
			 gotConnection=true
		 }
		 catch (IOException e) {
			 println("[E] Standalone Server seem to be already down")
			 
		 }
		 finally {
			 if (s != null) {
					 s.close()
			 }
		 }
		 if(!gotConnection){
			 return 1
		 }
		 
		 
		 try {
			 cli.connect()
			 //println("[I] Connected to controller's server : $controller_infos ")
		     
			 def commandline="/:shutdown"
			 
			 def result=cli.runCLICommand(commandline)
			 if (result.isSuccess()){
				 println("[I] a Shutdown request has been send to server")
			 }
			 else{
				 println("[E] a Shutdown request can't be send to server succesfully. Abort...")
				 return 1
			 }
			 
			 // wait the completed stop
			 println("[I] Wait for the completed shutdown of the server")
			 def startTime = System.currentTimeMillis()
			 gotConnection=true
			 while (gotConnection && System.currentTimeMillis() - startTime < timeout) {
				  s = new Socket()
				  addr = new InetSocketAddress(host, port)
				 
				 try {
					 s.connect(addr)
					 println("[I] Standalone Server is still alive (listen on $host:$port). Retry in 5 sec")
					 s.close()
					 sleep(5000)
				 }
				 catch (IOException e) {
					 println("[I] Standalone Server seem to be down (no listen on $host:$port)")
					 gotConnection=false
				 }
				 
			 }
			 if (!gotConnection){
				 println "[S] Standalone Server has finished to shutdown"
				 return 0
			 }
			 else{
				 println "[E] Shutdown Request has been send to the server. But Standalone Server seem to be stil alive after timeout (listen on $host:$port ) "
				 return 1
			 }
			 
		 }
		 catch (Exception e) {
			 println("[I] Could not establish a CLI Connection!:$controller_infos . Impossible to send a Shutdown Request to then Standalone Server...Abort...")
		 }
		 return 1
		 
	 }
	 /********************************************************
	 static test(def Jboss7CLIConnection cli){
		 def response="unknow"
		 def host=cli.getControllerHost()
		 def port=cli.getControllerPort()
		 def user=cli.getUserMgmt()
		 def controller_infos
		 if (user== null){
			 controller_infos=host+":"+port+",no user / no passwd"
		 }
		 else{
			 controller_infos=host+":"+port+","+user+" / *****"
		 }
		 
		 try {
			 cli.connect()
			 //println("[I] Connected to controller's server : $controller_infos ")
			 println("[I] Start Loop")
			 while (1){
				 
				 cli.disconnect()
				 cli.connect()
				 def response2
				 def commandline="/:read-attribute(name=server-state)"
				 
				 def result=cli.runCLICommand(commandline)
				 if (result.isSuccess()){
					   response2=result.getResponse().get("result").asString()
					  
								
				 }
				 else{
					  println("[W]: Impossible to control the state of server")
					  response2="unknow"
				 }
				 if (response2!=response){
					 response=response2
					 println "response=$response"
				 }
				 println "response2=$response2"
				
			 }
			 println("[I] End Loop")
			 
		 }
		 catch (Exception e) {
			 println("[I] Could not establish a CLI Connection!:$controller_infos . Impossible to get status of Standalone Server...Abort...")
			 response="unknow"
		 }
		 
		 return response
	 
	 }
	 *****************************************/
	 
	 static def String statusStandaloneServer(def Jboss7CLIConnection cli){
		 def response="unknow"
		 def host=cli.getControllerHost()
		 def port=cli.getControllerPort()
		 def user=cli.getUserMgmt()
		 def controller_infos
		 if (user== null){
			 controller_infos=host+":"+port+",no user / no passwd"
		 }
		 else{
			 controller_infos=host+":"+port+","+user+" / *****"
		 }
		 
		 
		 // Server is UP
		 def gotConnection=false
		 def s = new Socket()
		 def addr = new InetSocketAddress(host, port)
		 
		 try {
			 s.connect(addr)
			 gotConnection=true
		 }
		 catch (IOException e) {
			 gotConnection=false
		 }
		 finally {
			 if (s != null) {
					 s.close()
			 }
		 }
		 if(!gotConnection){
			 response="stopped"
		 }
		 else{

			 def isConnected=false
			 try {
				 cli.connect()
				 isConnected=true
				 //println("[I] Connected to controller's server : $controller_infos ")
				 
				 def commandline="/:read-attribute(name=server-state)"
				 
				 def result=cli.runCLICommand(commandline)
				 if (result.isSuccess()){
					  response=result.getResponse().get("result").asString()
					  // response can be : starting, stopping, running		  
				 }
				 else{
					  
					 println("[W]: Impossible to control the state of server")
					 response="unknow"
				 }
				 
			 }
			 catch (Exception e) {
				 println("[I] Could not establish a CLI Connection!:$controller_infos . Impossible to get status of Standalone Server...Abort...")
				 
				 response="listen-only"
			 }
		 
		 }
		 return response
	 }
	 
	 static def int areloadStandaloneServer(def Jboss7CLIConnection cli){
		 println("[A] Async Reload Standalone Server")
		 def cr=1
		 def host=cli.getControllerHost()
		 def port=cli.getControllerPort()
		 def user=cli.getUserMgmt()
		 def controller_infos
		 if (user== null){
			 controller_infos=host+":"+port+",no user / no passwd"
		 }
		 else{
			 controller_infos=host+":"+port+","+user+" / *****"
		 }
		 
		 try {
			 cli.connect()
			 //println("[I] Connected to controller's server : $controller_infos ")
			 
			 def commandline="/:reload"
			 
			 def result=cli.runCLICommand(commandline)
			 if (result.isSuccess()){
				  cr=0
							
			 }
			 else{
				  println("[E]: Impossible to send Reload request to the standalone server")
				  cr=1
			 }
			 
			 
			 
			 
		 }
		 catch (Exception e) {
			 println("[I] Could not establish a CLI Connection!:$controller_infos . Impossible to send Reload request to the standalone server...Abort...")
			 cr=1
		 }
		 
		 return cr
	 }
	 
	 static def int reloadStandaloneServer(def Jboss7CLIConnection cli,timeout){
		 println("[A] Reload Standalone Server")
		 def cr=1
		 def host=cli.getControllerHost()
		 def port=cli.getControllerPort()
		 def user=cli.getUserMgmt()
		 def controller_infos
		 if (user== null){
			 controller_infos=host+":"+port+",no user / no passwd"
		 }
		 else{
			 controller_infos=host+":"+port+","+user+" / *****"
		 }
		 
		 try {
			 cli.connect()
			 //println("[I] Connected to controller's server : $controller_infos ")
			 
			 def commandline="/:reload"
			 
			 def result=cli.runCLICommand(commandline)
			 if (!result.isSuccess()){
				  println("[E]: Impossible to send Reload request to the standalone server")
				  cr=1
			 }
			 else{
				 println("[I] Reload request has been send to the server")
				 try{
				    cli.waitForConnect(timeout)
				 }
				 catch(Exception e){
					 
				 }
				 
				println "[I] Wait until obtain a CLI Connection"
		        def startTime = System.currentTimeMillis()
		        def gotConnection = false        
		        
		        while (!gotConnection && System.currentTimeMillis() - startTime < timeout) {
		            try {
		                cli.connect()
		                gotConnection = true
						break
		            }
		            catch (Exception e) {
		                println("[I] Could not establish a CLI Connection!:$controller_infos . (Retry in 5 sec)")
						sleep(5000)
		            }
		            
		        }
		        
		        if (!gotConnection) {
		            throw new Exception("[E] Server not started in a timely manner.")
					return 1
		        }
				
				
				// /:read-attribute(name=server-state)
				def response="unknown"
				def isRunning=false
				while (!isRunning && System.currentTimeMillis() - startTime < timeout) {
					commandline="/:read-attribute(name=server-state)"
					
					result=cli.runCLICommand(commandline)
					if (result.isSuccess()){
						 response=result.getResponse().get("result").asString()
						 println("[I]: state of server:$response")
						 if (response == "running"){
							 isRunning=true
							 break
						 }
						 
					}
					else{
						 println("[W]: Impossible to control the state of server")
					}
					
					sleep(5000)
				}
				
				
				
		        if(isRunning){
		            println "[S] Achieved a connection to the server. Server has finished to start"
					return 0
		        }
				else{
					println "[E] Achieved a connection to the server. But Server hasn't finished to start after timeout (state still : $response ) "
					return 1
				}
			 }
			 
		 }
		 catch (Exception e) {
			 println("[I] Could not establish a CLI Connection!:$controller_infos . Impossible to send Reload request to the standalone server...Abort...")
			 cr=1
		 }
		 
		 return cr
	 }
	 
	 static def restartStandaloneServer(def Jboss7CLIConnection cli,def String exec_path,def List exec_args, def File exec_workdir, def Map exec_env, def timeout) {
		 println("[A] Restart Standalone Server")
		 def state=statusStandaloneServer(cli)
		 def cr=1
		 switch (state){
			 case "running": 
			 case "RUNNING": println "[I] Standalone Server is $state. Try to Shutdown the standalone Server..."
			 				cr=stopStandaloneServer(cli,timeout);
							if (cr==0){
								
								cr=startStandaloneServer(cli,exec_path,exec_args,exec_workdir,exec_env,timeout)
							}
							else{
								println "[E] Shutdown of Standalone Server FAILED. Abort Restart operation..."
								cr=1
							}
							break;
			 				
			 
			 case "stopping":
			 case "STOPPING": 
			 				
			 
			 case "starting":
			 case "STARTING": 
			 				
							 // wait the stable state 
			 				 println "[I] Standalone Server is $state. Wait for the stable state (running | stopped)..."
							 def startTime = System.currentTimeMillis()
							 def isStable=false
							 while (isStable && System.currentTimeMillis() - startTime < timeout) {
								  state=statusStandaloneServer(cli)
								  switch (state){
									  case "running":
									  case "RUNNING":
									  case "stopped":
									  case "STOPPED":isStable=true; break
									  default:
									  		isStable=false; 
											  println "[I] Standalone Server is still $state. Retry in 5 sec ..."
											  sleep(5000)
								  }
								 
							 }
							 if (isStable){
								 println "[I] Standalone Server has a stable state (running | stopped)"
								 if (state =="stopped" ||state =="STOPPED" ){
									 cr=startStandaloneServer(cli,exec_path,exec_args,exec_workdir,exec_env,timeout)
								 }
								 else{
									 cr=stopStandaloneServer(cli,timeout);
									 if (cr==0){
										 
										 cr=startStandaloneServer(cli,exec_path,exec_args,exec_workdir,exec_env,timeout)
									 }
									 else{
										 println "[E] Shutdown of Standalone Server FAILED. Abort Restart operation..."
										 cr=1
									 }
								 }
							 }
							 else{
								 println "[W] Standalone Server hasn't a stable state (running | stopped). Try to shutdown, then restart with no garantee  "
								 stopStandaloneServer(cli,timeout)
								 cr=startStandaloneServer(cli,exec_path,exec_args,exec_workdir,exec_env,timeout)
								 return 1
							 }
			 				
			 
			 case "stopped":
			 case "STOPPED": 
			 				println "[I] Standalone Server is still down. Start the standalone Server..."
							 cr=startStandaloneServer(cli,exec_path,exec_args,exec_workdir,exec_env,timeout)
							 break;
			 
			case "listen-only":
			 case "unknow":
			 case "UNKNOWN": println "[I] Standalone Server has a $state state. Try to Shutdown then start the standalone Server..."
			 				stopStandaloneServer(cli,timeout)
							 // don't take care about cr of stop stopStandaloneServer
				 			cr=startStandaloneServer(cli,exec_path,exec_args,exec_workdir,exec_env,timeout)
							 break;
			 default: 	println "[I] Standalone Server has a undefined state. Abort restart operation..."
			 			cr=1
						break;
		 }
		 
		 return cr
		 
	 }
	 static def astartStandaloneServer(def Jboss7CLIConnection cli,def String exec_path,def List exec_args, def File exec_workdir, def Map exec_env) {
		 println("[A] Start Standalone Server")
		 def cmdArgs =[exec_path]
		cmdArgs.addAll(exec_args)
		
		println "cmdArgs"+cmdArgs
		println "[I] Builting new process;  "+cmdArgs
	 
	 
		
		def builder = new com.urbancode.process.jdk7.ProcessBuilder(cmdArgs)
		
		//def outputFile = System.out
		if (exec_env!= null && exec_env.size()> 0){
			Map<String, String> env = builder.environment();
			println "[I] Add System Environment"+exec_env
			env.putAll(exec_env)
		}
		
		if (exec_workdir == null){
			exec_workdir=new File(".")
		}
		println "[I] Set Working Dir to "+exec_workdir.absolutePath
		builder.directory(exec_workdir);
		
		def outputFile = new File(exec_workdir, "outputStartFile.txt")
		println "[D] Set outputStartFile to "+outputFile.absolutePath
		builder.redirectOutput(outputFile)
		builder.redirectErrorStream(true)
		/*builder.redirectInput(outputFile)
		builder.redirectError(outputFile)
		*/
		
		
		
		def host=cli.getControllerHost()
		def port=cli.getControllerPort()
		def user=cli.getUserMgmt()
		def controller_infos
		if (user== null){
			controller_infos=host+":"+port+",no user / no passwd"
		}
		else{
			controller_infos=host+":"+port+","+user+" / *****"
		}
		
		println "[I] Check If another instance already started (meaning that controller is listen on $host:$port )  "
		def s = new Socket()
		def addr = new InetSocketAddress(host, port)
		
		try {
			s.connect(addr)
		}
		catch (IOException e) {
			println("[I] Standalone Server seem to be down")
		}
		finally {
			if (s != null) {
				if (s.isConnected()) {
				   println "[I] The server is already running, exiting with success"
				   s.close()
				   return 1
				}
				else {
					s.close()
				}
			}
		}
		
		println "[I] Starting  process;  "
		Process process=builder.start()
		if (process.errorStream != null ){process.errorStream.close()}
		//process.inputStream.close()
		if (process.outputStream != null ){process.outputStream.close()}
		
		
		println "[S] Started  process;  "
		return 0
		 
	 }
	 static def arestartStandaloneServer(def Jboss7CLIConnection cli,def String exec_path,def List exec_args, def File exec_workdir, def Map exec_env,def int timeout) {
		 println("[A] Restart Standalone Server")
		 def state=statusStandaloneServer(cli)
		 def cr=1
		 switch (state){
			 case "running":
			 case "RUNNING": println "[I] Standalone Server is $state. Try to Shutdown the standalone Server..."
							 cr=stopStandaloneServer(cli,timeout);
							if (cr==0){
								
								cr=astartStandaloneServer(cli,exec_path,exec_args,exec_workdir,exec_env)
							}
							else{
								println "[E] Shutdown of Standalone Server FAILED. Abort Restart operation..."
								cr=1
							}
							break;
							 
			 
			 case "stopping":
			 case "STOPPING":
							 
			 
			 case "starting":
			 case "STARTING":
							 
							 // wait the stable state
							  println "[I] Standalone Server is $state. Wait for the stable state (running | stopped)..."
							 def startTime = System.currentTimeMillis()
							 def isStable=false
							 while (isStable && System.currentTimeMillis() - startTime < timeout) {
								  state=statusStandaloneServer(cli)
								  switch (state){
									  case "running":
									  case "RUNNING":
									  case "stopped":
									  case "STOPPED":isStable=true; break
									  default:
											  isStable=false;
											  println "[I] Standalone Server is still $state. Retry in 5 sec ..."
											  sleep(5000)
								  }
								 
							 }
							 if (isStable){
								 println "[I] Standalone Server has a stable state (running | stopped)"
								 if (state =="stopped" ||state =="STOPPED" ){
									 cr=astartStandaloneServer(cli,exec_path,exec_args,exec_workdir,exec_env,timeout)
								 }
								 else{
									 cr=stopStandaloneServer(cli,timeout);
									 if (cr==0){
										 
										 cr=astartStandaloneServer(cli,exec_path,exec_args,exec_workdir,exec_env,timeout)
									 }
									 else{
										 println "[E] Shutdown of Standalone Server FAILED. Abort Restart operation..."
										 cr=1
									 }
								 }
							 }
							 else{
								 println "[W] Standalone Server hasn't a stable state (running | stopped). Try to shutdown, then restart with no garantee  "
								 stopStandaloneServer(cli,timeout)
								 cr=astartStandaloneServer(cli,exec_path,exec_args,exec_workdir,exec_env,timeout)
								 return 1
							 }
							 
			 
			 case "stopped":
			 case "STOPPED":
							 println "[I] Standalone Server is still down. Start the standalone Server..."
							 cr=astartStandaloneServer(cli,exec_path,exec_args,exec_workdir,exec_env,timeout)
							 break;
			 
			case "listen-only":
			 case "unknow":
			 case "UNKNOWN": println "[I] Standalone Server has a $state state. Try to Shutdown then start the standalone Server..."
							 stopStandaloneServer(cli,timeout)
							 // don't take care about cr of stop stopStandaloneServer
							 cr=astartStandaloneServer(cli,exec_path,exec_args,exec_workdir,exec_env,timeout)
							 break;
			 default: 	println "[I] Standalone Server has a undefined state. Abort restart operation..."
						 cr=1
						break;
		 }
		 
		 return cr
		 
	 }
	 static def astopStandaloneServer(def Jboss7CLIConnection cli,def String exec_path,def List exec_args, def File exec_workdir, def Map exec_env, def timeout) {
		 println("[A] Stop Standalone Server")
		 def host=cli.getControllerHost()
		 def port=cli.getControllerPort()
		 def user=cli.getUserMgmt()
		 def controller_infos
		 if (user== null){
			 controller_infos=host+":"+port+",no user / no passwd"
		 }
		 else{
			 controller_infos=host+":"+port+","+user+" / *****"
		 }
		 
		 println "[I] Check If Standalone server is alived (listen on $controller_infos)  "
		 def gotConnection=false
		 def s = new Socket()
		 def addr = new InetSocketAddress(host, port)
		 
		 try {
			 s.connect(addr)
			 gotConnection=true
		 }
		 catch (IOException e) {
			 println("[E] Standalone Server seem to be already down")
			 
		 }
		 finally {
			 if (s != null) {
					 s.close()
			 }
		 }
		 if(!gotConnection){
			 return 1
		 }
		 
		 
		 try {
			 cli.connect()
			 //println("[I] Connected to controller's server : $controller_infos ")
			 
			 def commandline="/:shutdown"
			 
			 def result=cli.runCLICommand(commandline)
			 if (result.isSuccess()){
				 println("[I] a Shutdown request has been send to server")
				 return 0
			 }
			 else{
				 println("[E] a Shutdown request can't be send to server succesfully. Abort...")
				 return 1
			 }
			 
			 
			 
		 }
		 catch (Exception e) {
			 println("[I] Could not establish a CLI Connection!:$controller_infos . Impossible to send a Shutdown Request to then Standalone Server...Abort...")
		 }
		 return 1
		 
	 }
	 
	 
	 static def checkAllHostOfDomainConnection(){
	 
	 }
	 static def startStopRestartHostDomain(){
	 
	 }
	 static def startStopRestartDomain(){
	 
	 }
	 
	 static def startStopRestartDomainServerGroup(){
	 
	 }
	 
	 
	 static def Boolean isApplicationExist(def Jboss7CLIConnection cli,def String deployName){
		 def commandline
		 commandline="/deployment=$deployName:read-attribute(name=content)"
		 def CLI.Result result=cli.runCLICommand(commandline)
		 if (result.isSuccess()	){ 
			 return true
		 }else{
		 	return false
		 }
	 }
	 static def Boolean isApplicationExistOnScope(def Jboss7CLIConnection cli,def String deployName,def scope){
		 def commandline
		 commandline="/server-group=$scope/deployment=$deployName:read-attribute(name=name)"
		 def CLI.Result result=cli.runCLICommand(commandline)
		 if (result.isSuccess()	){
			 
			 return true
		 }
		 else{
			 return false
		 }
	 }
	 static def Boolean isScopeExist(def Jboss7CLIConnection cli,def scopeStr){
		 // scopeStr can be equal to : ""| server-group-name,server-group-name2|--all-server-group
		 println ("[I] isScopeExist check if  deployScope: $scopeStr  exist...")
		  if (!cli.isDomainMode()){
			 // for standalone
			 if(scopeStr != ""){
				 println "[E] isScopeExist : For standalone Server, scope must be empty"
				 return false
			 }
			 else{ 
				 return true
			 }
		 }
		 else{
			 // for domain
			if(scopeStr == "--all-server-group"){ 
				 return true;
			}
			else{
				try{
					scopeStr.split(",").each{
						def String scope=it
						scope.replaceAll(/^ */, "")
						scope.replaceAll(/ *$/, "")
						def commandline="/server-group=$scope:read-attribute(name=profile)"
						def CLI.Result result=cli.runCLICommand(commandline)
						if (result.isSuccess()	){
							println ("[I] isScopeExist deploy scope : $scope  exist")
							return true
						}
						else{
							throw new Exception ("[E] isScopeExist deploy scope : $scope doesn't exist" )
						}
					}
				}
				catch(Exception e){
					println(e.getMessage())
					return false
				}
				return true
			}
			 
		 } 
			
		 
		 return false
	 }
	 
	 
	 static def int deployOneApplication(def Jboss7CLIConnection cli,def String deployName,def deployPathFile,def String deployScope, def String deployRunTimeName="", def String deployOptions){
		 def cr
		 
		 // controle mandatory input argument
		 
		 if (deployPathFile == null || (deployPathFile != null && deployPathFile == "")){ println "[E] deployOneApplication invalid argument: deployPathFile can't be empty"; return 1}
		 if (!cli.isDomainMode()){
			 if (deployScope == null || (deployScope != null && deployScope == "")){ println "[E] deployOneApplication invalid argument: deployScope can't be empty"; return 1}
		 }
		 
		 println "[I] Start deployOneApplication of $deployPathFile"
		 
		 // control no mandatory argument
		 if (deployName == null || (deployName != null && deployName == "")){ 
			 def java.io.File deployPathFileObj=new java.io.File(deployPathFile)
			 deployName=deployPathFileObj.getName()
			 println "[W] deployOneApplication: deployName is no defined, by default $deployName is used"
		 }
		 
		 // test if deployment already exist, undeploy it
		 if (isApplicationExist(cli,deployName)){
			 // undeploy the application on all scope if application already exist
			 cr=undeployOneApplicationIfExist(cli,deployName)
			 if(cr!= 0){
				 print "[E] Impossible to continue the deployment of /deployment=$deployName ($deployPathFile)"
				 return 1
			 }
		 }
		 
		 // start deployment
		 
		 def commandline
		 if (!cli.isDomainMode()){
			 // for standalone
			 commandline="deploy $deployPathFile"
			 if (deployRunTimeName != null && deployRunTimeName != "" ) { commandline+=" --runtime-name=$deployRunTimeName"}
			 if (deployOptions != null && deployOptions != "" ) { commandline+=" $deployOptions"}
		 }
		 else{
			 // for domain
			 commandline="deploy $deployPathFile"
			
		     
			if(deployScope == "--all-server-groups" ){
				 commandline+=" --all-server-groups"
			}
			else{
				commandline+=" --server-groups=$deployScope"
			}
				
		    
			if (deployRunTimeName != null && deployRunTimeName != "" ) { commandline+=" --runtime-name=$deployRunTimeName"}
			if (deployOptions != null && deployOptions != "" ) { commandline+=" $deployOptions"}
		 }
		 
		 // execute  commandline
		 println "[I] Excute $commandline"   
		  def CLI.Result result=cli.runCLICommand(commandline)
		 if (result.isSuccess()	){
			 println "[S] deploy of /deployment=$deployName ($deployPathFile) on  successfully"
			 return 0
		 }
		 else{
			 
			 def response = result.getResponse()
			 println("[E]: deploy /deployment=$deployName ($deployPathFile) FAILED:"+response.asString())
			 return 1
		 }
		 return 1
		 
	 }
	 //
	 
	 
	 static def int undeployOneApplication(def Jboss7CLIConnection cli,def String deployName){
		 def commandline
		 if (deployName == null || (deployName != null && deployName == "")){ println "[E] undeployOneApplication invalid argument: deployName can't be empty"; return 1}
		 
		 println "[I] Start undeployOneApplication of $deployName"
		 if (isApplicationExist(cli,deployName)){
			 if (! cli.isDomainMode()){
				 // for standalone
				 commandline="undeploy --name=$deployName "
			 }
			 else{
				 // for domain
				 commandline="undeploy --name=$deployName --all-relevant-server-groups"
			 }
			 def CLI.Result result=cli.runCLICommand(commandline)
			 if (result.isSuccess()	){
				 println "[S] Undeploy /deployment=$deployName successfully"
				 return 0
			 }
			 else{
				 
				 def response = result.getResponse()
				 println("[E]: Undeploy of /deployment=$deployName FAILED:"+response.asString())
				 return 1
			 }
		 }
		 else{
			 println "[E] /deployment=$deployName isn't deployed. No Undeploy action needed"
			 return 1 
		 } 
	 }
	 
	
	 static def int undeployOneApplicationIfExist(def Jboss7CLIConnection cli,def String deployName){
		 def cr=0
		 if (deployName == null || (deployName != null && deployName == "")){ println "[E] undeployOneApplicationIfExist invalid argument: deployName can't be empty"; return 1}
		 
		 println "[I] Start undeployOneApplicationIfExist of $deployName"
		 if (isApplicationExist(deployName)){
			 cr=undeployOneApplication(cli,deployName)
		 }
		 else{
			 println "[0] /deployment=$deployName isn't deploy. No Undeploy action needed"
			 cr=0
		 }
		 return cr
	 }
	 
	 static def int disableOneApplicationIfExist(def Jboss7CLIConnection cli,def String deployName,def String[] deployScope){
		 def cr=0
		 if (deployName == null || (deployName != null && deployName == "")){ println "[E] disableOneApplicationIfExist invalid argument: deployName can't be empty"; return 1}
		 if (!cli.isDomainMode()){
			 if (deployScope == null || (deployScope != null && deployScope == "")){ println "[E] disableOneApplicationIfExist invalid argument: deployScope can't be empty"; return 1}
		 }
		 
		 println "[I] Start disableOneApplicationIfExist of $deployName"
		 
		 
	
		 
		 // test if deployment already exist, 
		 if (isApplicationExist(cli,deployName)){
			 cr=disableOneApplication(cli,deployName,deployScope)
		 }
		 else{
			  print "[E] /deployment=$deployName doesn't exist. Abort operation"
				 return 0
		 }
		 return cr
	 }
	 
	 static def int disableOneApplication(def Jboss7CLIConnection cli,def String deployName,def String[] deployScope){
		 def cr
		 
		 // controle mandatory input argument
		 
		 if (deployName == null || (deployName != null && deployName == "")){ println "[E] disableOneApplication invalid argument: deployName can't be empty"; return 1}
		 if (!cli.isDomainMode()){
			 if (deployScope == null || (deployScope != null && deployScope == "")){ println "[E] disableOneApplication invalid argument: deployScope can't be empty"; return 1}
		 }
		 
		 println "[I] Start disableOneApplication of $deployName"
		 
		 
	
		 
		 // test if deployment already exist, undeploy it
		 if (!isApplicationExist(cli,deployName)){
			 
				 print "[E] /deployment=$deployName doesn't exist. Abort operation"
				 return 1
			 
		 }
		 
		 // start deployment
		 
		 def commandline
		 if (!cli.isDomainMode()){
			 // for standalone
			 commandline="undeploy --name=$deployName  --keep-content"
		 }
		 else{
			 // for domain
			 commandline="undeploy --name=$deployName  --keep-content"
			
			 
				if(deployScope == "--all-server-groups" ){
					 commandline+=" --all-server-groups"
				}
				else{
					commandline+=" --server-groups=$deployScope"
				}
				
			
			 
		 }
		 
		 // execute  commandline
		 println "[I] Excute $commandline"
		  def CLI.Result result=cli.runCLICommand(commandline)
		 if (result.isSuccess()	){
			 println "[S] disable of /deployment=$deployName   successfully"
			 return 0
		 }
		 else{
			 
			 def response = result.getResponse()
			 println("[E]: disable /deployment=$deployName  FAILED:"+response.asString())
			 return 1
		 }
		 return 1
		 
		 
	 }
	
	 static def int enableOneApplication(def Jboss7CLIConnection cli,def String deployName,def String[] deployScope){
		 // controle mandatory input argument
		 
		 if (deployName == null || (deployName != null && deployName == "")){ println "[E] enableOneApplication invalid argument: deployName can't be empty"; return 1}
		 if (!cli.isDomainMode()){
			 if (deployScope == null || (deployScope != null && deployScope == "")){ println "[E] enableOneApplication invalid argument: deployScope can't be empty"; return 1}
		 }
		 
		 println "[I] Start enableOneApplication of $deployName"
		 
		 
	
		 
		 // test if deployment already exist, undeploy it
		 if (!isApplicationExist(cli,deployName)){
				 print "[E] /deployment=$deployName doesn't exist. Abort operation"
				 return 1
		 }
		 
		 // start deployment
		 
		 def commandline
		 if (!cli.isDomainMode()){
			 // for standalone
			 commandline="deploy --name=$deployName"
		 }
		 else{
			 // for domain
			 commandline="deploy --name=$deployName"
			
			 
				if(deployScope == "--all-server-groups" ){
					 commandline+=" --all-server-groups"
				}
				else{
					commandline+=" --server-groups=$deployScope"
				}
				
			
			 
		 }
		 
		 // execute  commandline
		 println "[I] Excute $commandline"
		  def CLI.Result result=cli.runCLICommand(commandline)
		 if (result.isSuccess()	){
			 println "[S] enable of /deployment=$deployName   successfully"
			 return 0
		 }
		 else{
			 
			 def response = result.getResponse()
			 println("[E]: enable of /deployment=$deployName  FAILED:"+response.asString())
			 return 1
		 }
		 return 1

	 }
	 static def int enableOneApplicationIfExist(def Jboss7CLIConnection cli,def String deployName,def String[] deployScope){
		 def cr=0
		 if (deployName == null || (deployName != null && deployName == "")){ println "[E] enableOneApplicationIfExist invalid argument: deployName can't be empty"; return 1}
		 if (!cli.isDomainMode()){
			 if (deployScope == null || (deployScope != null && deployScope == "")){ println "[E] enableOneApplicationIfExist invalid argument: deployScope can't be empty"; return 1}
		 }
		 
		 println "[I] Start enableOneApplicationIfExist If exist of $deployName"

		 // test if deployment already exist,
		 if (isApplicationExist(cli,deployName)){
			 cr=enableOneApplication(cli,deployName,deployScope)
		 }
		 else{
			  print "[E] /deployment=$deployName doesn't exist. Abort operation"
				 return 0
		 }
		 return cr
	 }
	  
	 
	 
	 
	 static def List getListOfApplicationByRegexp(def Jboss7CLIConnection cli, def String regexp){
		 
		 def List applicationLst=[]
		 def regexpStr=regexp
		 def commandline="/deployment=*:read-attribute(name=name)"
		 def CLI.Result result
		 result=cli.runCLICommand(commandline)
		 if (result.isSuccess()	){
			 def response = result.getResponse()			 
			 def List ApplicationTab=response.get("result").asList()
			
			 ApplicationTab.each{ 
				 def applicationName=it.get("result").asString()
				 
				 if(applicationName =~ regexpStr){
					 applicationLst.add( applicationName)
				 }
			 }
			 
		 }
		 else{
			 
			 def response = result.getResponse()
			 println("[E]: Impossible to generate the list of all Application"+response.asString())
			 return null
		 }
		 return applicationLst
		 
	 }
	 /***********
	 //deployOneApplication(cli,def String deployName,def deployPathFile,def String deployScope, def String deployRunTimeName="", def String deployOptions)
	 static def deployMultipleApplications(def Jboss7CLIConnection cli,def List deployNameLst,def deployPathDir,def String deployScope, def String deployOptions){
		 def cr=0
		 
		  if (deployNameLst == null || (deployNameLst != null && deployNameLst=="" ) ){ println "[E] deployGroupOfApplications invalid argument: deployNameLst can't be empty"; return 1}
		  if (deployPathDir == null || (deployPathDir != null && deployPathDir=="" ) ){ println "[E] deployGroupOfApplications invalid argument: deployPathDir can't be empty"; return 1}
		  def File deployPathDirFile=new File(deployPathDir)
		  if(!deployPathDirFile.isDirectory()){
			  println "[E] deployGroupOfApplications: The directory $deployPathDir doesnt exist or isn't a directory"
			  return 1
		  }
		  
		  println "[I] Start exportGroupOfApplications "
		  
		  deployNameLst.each{ deployName ->
			  def deployPathFile
			  def deployRunTimeName
			  cr=deployOneApplication(cli,deployName,deployPathFile,deployScope, deployRunTimeName, deployOptions)
			  
		  }
		  println "[S] End  deployGroupOfApplications  "
		  return 0
	 }
	 ****************/
	 static def int undeployMultipleApplications(def Jboss7CLIConnection cli,def List deployNameLst){
		 def cr=0
		 
		  if (deployNameLst == null ){ println "[E] undeployMultipleApplications invalid argument: deployNameLst can't be empty"; return 1}
		  
		  println "[I] Start undeployMultipleApplications "
		  
		  deployNameLst.each{ deployName ->
			  if (!isApplicationExist(cli,deployName)){
				  print "[E] /deployment=$deployName doesn't exist. Abort operation"
				  println "[E] End  undeployMultipleApplications : FAILED "
				  return 1
			  }
			  else{
				  cr=undeployOneApplication(cli,deployName)
				  if (cr!= 0){
					  println "[E] End  undeployMultipleApplications : FAILED "
					  return 1
				  }
			  }
		  }
		  println "[S] End  undeployMultipleApplications  "
		  return  0
	 }
	 static  def int undeployGroupOfApplications(def Jboss7CLIConnection cli,def String groupName){
		 def cr=0
		 def regexpGroupName="^$groupName-.*\$"
		 println "[I] Start undeployGroupOfApplications "
		 // make a list of application that name match with regexpGroupName
		 def List ApplicationOfGroup=getListOfApplicationByRegexp(cli,regexpGroupName)
		 if (ApplicationOfGroup.size() ==0){
			 println "[E] undeployGroupOfApplications :No application exist for group:$regexpGroupName"
			 cr=1
		 }
		 else{
			 cr=undeployMultipleApplications(cli,ApplicationOfGroup)
		 }
		 if(cr==0){
			 println "[S] End undeployGroupOfApplications "
		 }
		 else{
			 println "[E] End undeployGroupOfApplications "
		 }
		 return cr
	 }
	 static  def Boolean isExistGroupOfApplications(def Jboss7CLIConnection cli,def String groupName){
		 def regexpGroupName="^$groupName-.*\$"
		 def List ApplicationOfGroup=getListOfApplicationByRegexp(cli,regexpGroupName)
		 if (ApplicationOfGroup.size() ==0){
			 return false
		 }
		 else{
			 return true
		 }
		 
	 }
	 static def int enableMultipleApplications(def Jboss7CLIConnection cli,def List deployNameLst){
		 def cr=0
		 
		  if (deployNameLst == null ){ println "[E] enableMultipleApplications invalid argument: deployNameLst can't be empty"; return 1}
		  
		  println "[I] Start enableMultipleApplications "
		  
		  deployNameLst.each{ deployName ->
			  if (!isApplicationExist(cli,deployName)){
				  print "[E] /deployment=$deployName doesn't exist. Abort operation"
				  println "[E] End  enableMultipleApplications : FAILED "
				  return 1
			  }
			  else{
				  cr=enableOneApplication(cli,deployName)
				  if (cr!= 0){
					  println "[E] End  enableMultipleApplications : FAILED "
					  return 1
				  }
			  }
		  }
		  println "[S] End  enableMultipleApplications  "
		  return  0
	 }
	 static def int enableGroupOfApplications(def Jboss7CLIConnection cli,def String groupName){
		 def cr=0
		 def regexpGroupName="^$groupName-.*\$"
		 println "[I] Start enableGroupOfApplications "
		 // make a list of application that name match with regexpGroupName
		 def List ApplicationOfGroup=getListOfApplicationByRegexp(cli,regexpGroupName)
		 if (ApplicationOfGroup.size() ==0){
			 println "[E] enableGroupOfApplications :No application exist for group:$regexpGroupName"
			 cr=1
		 }
		 else{
			 cr=enableMultipleApplications(cli,ApplicationOfGroup)
		 }
		 if(cr==0){
			 println "[S] End enableGroupOfApplications "
		 }
		 else{
			 println "[E] End enableGroupOfApplications "
		 }
		 return cr
	 }
	 
	 static def int disableMultipleApplications(def Jboss7CLIConnection cli,def List deployNameLst){
		 def cr=0
		 
		  if (deployNameLst == null ){ println "[E] disableMultipleApplications invalid argument: deployNameLst can't be empty"; return 1}
		  
		  println "[I] Start disableMultipleApplications "
		  
		  deployNameLst.each{ deployName ->
			  if (!isApplicationExist(cli,deployName)){
				  print "[E] /deployment=$deployName doesn't exist. Abort operation"
				  println "[E] End  disableMultipleApplications : FAILED "
				  return 1
			  }
			  else{
				  cr=disableOneApplication(cli,deployName)
				  if (cr!= 0){
					  println "[E] End  disableMultipleApplications : FAILED "
					  return 1
				  }
			  }
		  }
		  println "[S] End  disableMultipleApplications  "
		  return  0
	 }
	 static def int disableGroupOfApplications(def Jboss7CLIConnection cli,def String groupName){
		 def cr=0
		 def regexpGroupName="^$groupName-.*\$"
		 println "[I] Start disableGroupOfApplications "
		 // make a list of application that name match with regexpGroupName
		 def List ApplicationOfGroup=getListOfApplicationByRegexp(cli,regexpGroupName)
		 if (ApplicationOfGroup.size() ==0){
			 println "[E] disableGroupOfApplications :No application exist for group:$regexpGroupName"
			 cr=1
		 }
		 else{
			 cr=disableMultipleApplications(cli,ApplicationOfGroup)
		 }
		 if(cr==0){
			 println "[S] End disableGroupOfApplications "
		 }
		 else{
			 println "[E] End disableGroupOfApplications "
		 }
		 return cr
	 }
	 static def disableGroupOfApplications(){
	 
	 }
	 static def int exportOneApplication(def Jboss7CLIConnection cli,def String deployName, def String path_exportdir_destination){
		 def cr=0
		 def commandlineForHash
		 def commandlineForJbossContentPath
		 
		 if (deployName == null || (deployName != null && deployName == "")){ println "[E] exportOneApplication invalid argument: deployName can't be empty"; return 1}
		 
		 def File exportDirFile=new File(path_exportdir_destination)
		 if(!exportDirFile.isDirectory()){
			 println "[E] exportOneApplication invalid argument: $path_exportdir_destination doesn't exist or isn't a directory";
			 return 1
		 }
		 
		 println "[I] Start exportOneApplication If exist of $deployName"
		 
		 if (!isApplicationExist(cli,deployName)){
			  print "[E] /deployment=$deployName doesn't exist. Abort operation"
		return 1
		 }
		 
		 // catch run-time name
		 def commandlineForRuntimeName="/deployment=$deployName:read-attribute(name=runtime-name)"
		 def runtimeName
		 def CLI.Result result=cli.runCLICommand(commandlineForJbossContentPath)
		 if (result.isSuccess()	){
			 def response = result.getResponse()
			 commandlineForRuntimeName=response.get("result").asString()
			 println "[I] RuntimeName: $commandlineForRuntimeName   "
			 
		 }
		 else{
			 
			 def response = result.getResponse()
			 println("[E]: Impossible to get Jboss Context Dir path:"+response.asString())
			 return 1
		 }
		 
		 // catch the JbossContentPath
		 def path_jboss_content_dir=""
		 if (!cli.isDomainMode()){
			 // for standalone
			 commandlineForJbossContentPath="/core-service=server-environment:read-attribute(name=content-dir)"
		 }
		 else{
			 // for domain
			 commandlineForJbossContentPath="/host=master/core-service=host-environment/:read-attribute(name=domain-content-dir)"
		 }
		 
		 result=cli.runCLICommand(commandlineForJbossContentPath)
		 if (result.isSuccess()	){
			 def response = result.getResponse()
			 path_jboss_content_dir=response.get("result").asString()
			 println "[I] JbossContentPath: $path_jboss_content_dir   "
			 
		 }
		 else{
			 
			 def response = result.getResponse()
			 println("[E]: Impossible to get Jboss Context Dir path:"+response.asString())
			 return 1
		 }
		 
		 
		 // catch the application hash number store inside Jboss Repository
		 def hashCode=""
		 
		 // same for standalone and domaine
		 commandlineForHash="/deployment=$deployName:read-attribute(name=content)"
		 def path_application_content=""
		 
		 result=cli.runCLICommand(commandlineForHash)
		 if (result.isSuccess()	){
			 def response = result.getResponse()
			 
			 
			 def resultInfos=response.get("result")
			 def Byte[] hashByteInfos=resultInfos.get(0).get("hash").asBytes()
			 def sizeHash=hashByteInfos.size()
			 println "sizeHash=$sizeHash"
			 def parentDir=String.format("%02x", hashByteInfos[0]&0xff)
			 
			 def strHash=""
			 for(def i=1;i<sizeHash;i++){
				 strHash+=String.format("%02x", hashByteInfos[i]&0xff)
			 }
			 
			 path_application_content=path_jboss_content_dir+System.getProperty("line.separator")+parentDir+System.getProperty("line.separator")+strHash
			 println "[I] Application Content path: $path_application_content   "
			 
			 
			 path_jboss_content_dir=response.get("result").asString()
			 println "[I] JbossContentPath: $path_jboss_content_dir   "
			 
		 }
		 else{
			 
			 def response = result.getResponse()
			 println("[E]: Impossible to get Hash value associted to /deployment=$deployName:"+response.asString())
			 return 1
		 }
		 
		// copy  to dest file
		def copy = { File src,File dest->
			 
				def input = src.newDataInputStream()
				def output = dest.newDataOutputStream()
			 
				output << input
			 
				input.close()
				output.close()
		}
		
		def File path_export_runtime_dir = new File (path_exportdir_destination,runtimeName)
		def File srcFile  = new File(path_application_content)
		def File destFile = new File(path_export_runtime_dir,deployName)
		try{
			
			
			
			
			if (! path_export_runtime_dir.isDirectory()){
				path_export_runtime_dir.mkdir()
			}
			
			copy(srcFile,destFile)
		}
		catch(Exception e){
			println("[E]: Impossible to copy  :"+srcFile.absolutePath+" to "+destFile.absolutePath+" :" +e.getMessage()+":\n"+e.printStackTrace())
			return 1
		}
		
		return 0
	 }
	 
	 
	 static def int exportMultipleApplications(def Jboss7CLIConnection cli,def List deployNameLst,def String path_exportdir_destination){
		 def cr=0
		
		 if (deployNameLst == null ){ println "[E] exportMultipleApplications invalid argument: deployNameLst can't be empty"; return 1}
		 
		 println "[I] Start exportMultipleApplications "
		 
		 deployNameLst.each{ deployName ->
			 if (!isApplicationExist(cli,deployName)){
				 print "[E] /deployment=$deployName doesn't exist. Abort operation"
				 println "[E] End  exportMultipleApplications : FAILED "
				 return 1
			 }
			 else{
				 cr=undeployOneApplication(cli,deployName)
				 if (cr!= 0){
					 println "[E] End  exportMultipleApplications : FAILED "
					 return 1
				 }
			 }
		 }
		 println "[S] End  exportMultipleApplications  "
		 return 0
		 
	 }
	 static def int exportGroupOfApplications(def Jboss7CLIConnection cli,def String groupName,def String path_exportdir_destination){
		 def cr=0
		 def regexpGroupName="^$groupName-.*\$"
		 println "[I] Start exportGroupOfApplications "
		 // make a list of application that name match with regexpGroupName
		 def List ApplicationOfGroup=getListOfApplicationByRegexp(cli,regexpGroupName)
		 if (ApplicationOfGroup.size() ==0){
			 println "[E] exportGroupOfApplications :No application exist for group:$regexpGroupName"
			 cr=1
		 }
		 else{
			 cr=exportMultipleApplications(cli,ApplicationOfGroup,path_exportdir_destination)
		 }
		 if(cr==0){
			 println "[S] End exportGroupOfApplications "
		 }
		 else{
			 println "[E] End exportGroupOfApplications "
		 }
		 return cr
	 }
	 
	 static def int backupGroupOfApplications(){
	 
	 }
	 static def int restoreGroupOfApplications(){
	 
	 }
	 
	 static def int backupEnv(){
	 
	 }
	 static def int restoreEnv(){
	 
	 }
	 
	 static def int statusOfOneApplication(def Jboss7CLIConnection cli,def String deployName,def String[] deployScope){
	 
	 }
	 
	 static def int waitForStatusOfOneApplication(def Jboss7CLIConnection cli,def String deployName,def String[] deployScope){
		 
	 }
	 
	 static def int  statusOfGroupOfApplications(def Jboss7CLIConnection cli,def String deployName,def String[] deployScope){
	 
	 }
	 
	 static def int waitForStatusOfGroupOfApplications(def Jboss7CLIConnection cli,def String deployName,def String[] deployScope){
		 
	 }
	 
	 /********
	 static def int deployGroupPackage(def Jboss7CLIConnection cli,def File path_GroupPackage_src_dir, def File workdir){
		 def cr=0
		 
		  if (deploymentPropertiesLst == null ){ println "[E] deployMultiplePackagedApplications invalid argument: deploymentPropertiesLst can't be empty"; return 1}
		  
		  println "[I] Start deployMultiplePackagedApplications "
		  
		  deploymentPropertiesLst.each{ deploymentProperties ->
			  cr=deployOnePackagedApplication(cli,deploymentProperties)
			  if (cr!=0){
				  println "[E] End  deployMultiplePackagedApplications : FAILED "
				  return 1
			  }
			  
		  }
		  println "[S] End  deployMultiplePackagedApplications  "
		  return 0
	 }
	 **************/
	 static def int deployMultiplePackagedApplications(def Jboss7CLIConnection cli,def List  deploymentPropertiesLst){
		 def cr=0
		 
		  if (deploymentPropertiesLst == null ){ println "[E] deployMultiplePackagedApplications invalid argument: deploymentPropertiesLst can't be empty"; return 1}
		  
		  println "[I] Start deployMultiplePackagedApplications "
		  try{
			  deploymentPropertiesLst.each{ deploymentProperties ->
				  cr=deployOnePackagedApplication(cli,deploymentProperties)
				  if (cr!=0){
					   throw new Exception("[E] End  deployMultiplePackagedApplications : FAILED ") 
				  }
				  
			  }
		  }
		  catch(Exception e){
			  
			  println e.getMessage() 
			  return 1
		  }
		  println "[S] End  deployMultiplePackagedApplications  "
		  return 0
	 }
	 /* deployOnePackagedApplication (cli,deploymentProperties)
	  * deploymentProperties:
	  *   *mandatory
	  *   *deployOption_name=xxxx
	  *   deployOption_runtime-name=xxxx
	  *   deployOption_others=xxxx
	  *   *deployOption_scope=
	  *   *deployOption_src-path="
	  *   deployOption_env-path=
	  */
	 static def int deployOnePackagedApplication(def Jboss7CLIConnection cli,def java.util.Properties deploymentProperties){
		 def cr
		 if (deploymentProperties == null){ println "[E] deployOnePackagedApplication invalid argument: deploymentProperties can't be empty"; return 1}
		 def String deployRunTimeName=deploymentProperties.getProperty("deployOption_runtime-name", "")
		 def String deployOptions=deploymentProperties.getProperty("deployOption_others", "")
		 def String deployScope=deploymentProperties.getProperty("deployOption_scope", "")
		 def deployPathSrcFile=deploymentProperties.getProperty("deployOption_src-path", "")
		 def deployPathEnvFile=deploymentProperties.getProperty("deployOption_env-path", "")
		 def deployName=deploymentProperties.getProperty("deployOption_name", "")
		 
		 // controle mandatory input argument
		 println "[I] Start deployOnePackagedApplication of $deployPathSrcFile"
		 if ( deployName == ""){ println "[E] deployOnePackagedApplication missing property: deployOption_name can't be empty"; return 1}
		 if ( deployPathSrcFile == ""){ println "[E] deployOnePackagedApplication missing property: deployOption_src-path can't be empty"; return 1}
		 if (!cli.isDomainMode()){
			 if ( deployScope == ""){ println "[E] deployOnePackagedApplication missing property: deployOption_scope can't be empty"; return 1}
		 }
		 
		 
		 println "[D] deployOnePackagedApplication deploymentProperties:"+deploymentProperties
		 // control no mandatory argument
		 /*
		 if (deployName == null || (deployName != null && deployName == "")){
			 def java.io.File deployPathFileObj=new java.io.File(deployPathSrcFile)
			 deployName=deployPathFileObj.getName()
			 println "[W] deployOnePackagedApplication: deployName is no defined, by default $deployName is used"
		 }*/
		 
		 println "[D] deployOnePackagedApplication deployName:"+deployName
		 // test if deployment already exist, undeploy it
		 if (isApplicationExist(cli,deployName)){
			 // undeploy the application on all scope if application already exist
			 cr=undeployOneApplicationIfExist(cli,deployName)
			 if(cr!= 0){
				 print "[E] deployOnePackagedApplication Impossible to continue the deployment of /deployment=$deployName ($deployPathSrcFile)"
				 return 1
			 }
		 }
		 
		 
		 println "[I] deployOnePackagedApplication property :deployOption_scope=$deployScope"
		 println "[I] deployOnePackagedApplication property :deployOption_name=$deployName"
		 println "[I] deployOnePackagedApplication property :deployOption_runtime-name=$deployRunTimeName"
		 println "[I] deployOnePackagedApplication property :deployOption_others=$deployOptions"
		 println "[I] deployOnePackagedApplication property :deployOption_src-path=$deployPathSrcFile"
		 println "[I] deployOnePackagedApplication property :deployOption_env-path=$deployPathEnvFile"
		 
		 
		 
		 // manage deployment's environment
		 if(deployPathEnvFile!= ""){
			 def File f=new File(deployPathEnvFile)
			 if (f.isFile()){
				 def deployPathEnvFileToString=f.text
				 cr=undeployEnvironment(cli,deployPathEnvFileToString)
				 if(cr!=0){
					 print "[W] deployOnePackagedApplication undeploy deployment's environment failed. Abort..."
					 
				 }
				 cr=deployEnvironment(cli,deployPathEnvFileToString)
				 if(cr!=0){
					 print "[E] deployOnePackagedApplication deploy deployment's environment failed. Abort..."
					 return 1
				 }
			 }
			 else{
				 print "[E] deployOnePackagedApplication deployment's environment($deployPathEnvFile) doesn't exist. Abort..."
				 return 1
			 }
		 }
		 
		 // 
		 //deployPathSrcFile=deployPathSrcFile.replace("\\","/");
		 // start deployment
		 def commandline
		 if (!cli.isDomainMode()){
			 // for standalone
			 commandline="deploy $deployPathSrcFile"
			 if ( deployRunTimeName != "" ) { commandline+=" --runtime-name=$deployRunTimeName"}
			 if ( deployOptions != "" ) { commandline+=" $deployOptions"}
		 }
		 else{
			 // for domain
			 commandline="deploy $deployPathSrcFile"
			
			 
			if(deployScope == "--all-server-groups" ){
				 commandline+=" --all-server-groups"
			}
			else{
				commandline+=" --server-groups="+deployScope.replaceAll(/^ */, "").replaceAll(/ *$/, "").replaceAll(/ *, */, ",")
			}
				
			
			if (deployRunTimeName != null && deployRunTimeName != "" ) { commandline+=" --runtime-name=$deployRunTimeName"}
			if (deployOptions != null && deployOptions != "" ) { commandline+=" $deployOptions"}
		 }
		 
		 // execute  commandline
		 println "[I] deployOnePackagedApplication Execute $commandline"
		 try{
			  def CLI.Result result=cli.runCLICommand(commandline)
			 if (result.isSuccess()	){
				 println "[S] deployOnePackagedApplication deploy of /deployment=$deployName ($deployPathSrcFile) on  successfully"
				 return 0
			 }
			 else{
				 
				 def response = result.getResponse()
				 
				 println("[E]: deployOnePackagedApplication deploy /deployment=$deployName ($deployPathSrcFile) FAILED:"+response.asString())
				 
				 return 1
			 }
		 }
		 catch(Exception e){
			 println("[E]: deployOnePackagedApplication deploy /deployment=$deployName ($deployPathSrcFile) FAILED:"+e.getMessage())
		 }
		 return 1
		 
	 }
}