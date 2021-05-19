package fr.visionitgroup.groovylib.jboss7

import java.io.File;
import java.util.List;
import java.util.Map;


import fr.visionitgroup.groovylib.jboss7.Jboss7Shell

class Jboss7InstanceDomainMgr {
	
	
	
	
	static public String getPidInstance(def Jboss7Shell cli, def String path_instance,def String host="", def String  instance=""){
		
		
		if (path_instance == ""){
			path_instance="/host=$host/server-config=$instance"
		}
		
		// calcul du serverName et slave name

		
		
		// /host=slave2/server=server2/core-service=platform-mbean/type=runtime/
		
		if(!Jboss7CommonTools.isExistResource(cli,path_instance)){
			println "[E] instance $path_instance doesn't exist"
			return "$path_instance::"
		}
		
		// calcul du path server_runtime
		def String path_server_runtime=path_instance.replaceAll("server-config", "server")
		
		// calcul du path_runtime
		def String path_runtime="${path_server_runtime}/core-service=platform-mbean/type=runtime"
		
		
		
		if(!Jboss7CommonTools.isExistResource(cli,path_runtime)){
			println "[E] the instance runtime seem to be down"
			return "$path_instance::"
		}
		
		def String pidStr=Jboss7CommonTools.getAttributeValue(cli, path_runtime, "name")
		if(pidStr != null){
			def pid=""
			def hostname=""
			if(pidStr.split("@")[0]!= null){pid=pidStr.split("@")[0]}
			if(pidStr.split("@")[1]!= null){hostname=pidStr.split("@")[1]}
			
			return "$path_instance:$hostname:$pid"
		}
		else{
			return "$path_instance::"
		}
	}
	
	static public String getPidHostController(def Jboss7Shell cli, def String path_controller,def String host=""){
		
		
		if (path_controller == ""){
			path_controller="/host=$host"
		}
		
		// calcul du serverName et slave name

		
		
		// /host=slave2/server=server2/core-service=platform-mbean/type=runtime/
		
		if(!Jboss7CommonTools.isExistResource(cli,path_controller)){
			println "[E] controller $path_controller doesn't exist"
			return "$path_controller::"
		}
		

		
		// calcul du path_runtime
		def String path_runtime="${path_controller}/core-service=platform-mbean/type=runtime"
		
		
		
		if(!Jboss7CommonTools.isExistResource(cli,path_runtime)){
			println "[E] the instance runtime seem to be down"
			return "$path_controller::"
		}
		
		def String pidStr=Jboss7CommonTools.getAttributeValue(cli, path_runtime, "name")
		if(pidStr != null){
			def pid=""
			def hostname=""
			if(pidStr.split("@")[0]!= null){pid=pidStr.split("@")[0]}
			if(pidStr.split("@")[1]!= null){hostname=pidStr.split("@")[1]}
			
			return "$path_controller:$hostname:$pid"
		}
		else{
			return "$path_controller::"
		}
	}
	
	static public String getPidDomainController(def Jboss7Shell cli){
		

		def path_domain=Jboss7CommonTools.getListofObjects(cli,"","host",false,["master": "true"])[0]
				
		// calcul du path_runtime
		def String path_runtime="${path_domain}/core-service=platform-mbean/type=runtime"
		
		
		
		if(!Jboss7CommonTools.isExistResource(cli,path_runtime)){
			println "[E] the instance runtime seem to be down"
			return "$path_domain::"
		}
		
		def String pidStr=Jboss7CommonTools.getAttributeValue(cli, path_runtime, "name")
		if(pidStr != null){
			def pid=""
			def hostname=""
			if(pidStr.split("@")[0]!= null){pid=pidStr.split("@")[0]}
			if(pidStr.split("@")[1]!= null){hostname=pidStr.split("@")[1]}
			
			return "$path_domain:$hostname:$pid"
		}
		else{
			return "$path_domain::"
		}
	}
	
	
	
	static public String getStatusInstance(def Jboss7Shell cli, def String path_instance){
		/********
		 * Liste des status possibles pour une instance Jboss7
			  "DISABLED",
		"STARTING",
		"STARTED",
		"STOPPING",
		"STOPPED",
		"FAILED",
		"UNKNOWN",
		"DOES_NOT_EXIST"
		 */
		def commandline="$path_instance:read-attribute(name=status)"
		def result=cli.runCLICommand(commandline)
		if (result.isSuccess()){
			return result.getResponse().get("result").asString()
		}
		else{
			return null
		}
	}
	
	
	
	
	
	
	
	static int statusInstance(def Jboss7Shell cli, def String path_instance, def String host="", def String  instance=""){
		def cr=0
		if (path_instance == ""){
			path_instance="/host=$host/server-config=$instance"
		}
		
		if(!Jboss7CommonTools.isExistResource(cli,path_instance)){
			println "[E] Status of instance $path_instance : DOES_NOT_EXIST"
			return 1
		}
		
		def status=getStatusInstance(cli,path_instance)
		if (status==null){
			println "[E] Status of instance $path_instance : UNKNOWN"
			return 1
		}
		println "[I] Status of instance $path_instance : $status"
		return 0
	}
	
	static int waitForstatusOfInstance(def Jboss7Shell cli, def String statusToWait, def long timeout,def String path_instance, def String host="", def String  instance=""){
		/********
		 * Liste des status possibles pour une instance Jboss7
			  "DISABLED",
		"STARTING",
		"STARTED",
		"STOPPING",
		"STOPPED",
		"FAILED",
		"UNKNOWN",
		"DOES_NOT_EXIST"
		 */
		
		def cr=0
		
		switch(statusToWait){
			case "DISABLED":
			case "STARTING":
			case "STARTED":
			case "STOPPING":
			case "STOPPED":
			case "FAILED":
			case "UNKNOWN":
			case "DOES_NOT_EXIST":
				break;
			default:
				println("[E] Invalid status : $statusToWait ");
				return 1;
				break;
		}
		
		if (path_instance == ""){
			path_instance="/host=$host/server-config=$instance"
		}
		
		if(!Jboss7CommonTools.isExistResource(cli,path_instance)){
			println "[E] Status of instance $path_instance : DOES_NOT_EXIST"
			return 1
		}
		
		// by default wait for terminate state or status=statusToWait
		
		def status=getStatusInstance(cli,path_instance)
		if (status==null){status="UNKNOWN"}
		if(status==statusToWait){
			println "";println "[S] Instance $path_instance is already  $status."
			return 0
		}
		
		def laststatus=status
		
		println "[I] Instance $path_instance is currently  $status."
		print "[I] Wait for status $statusToWait:."
		def startTime = System.currentTimeMillis()
		while(laststatus=status || status== "STARTING" ||  status== "STOPPING" ||  status== "UNKNOWN"   ){
			// wait 5 secondes
			sleep(5000)
			status=getStatusInstance(cli,path_instance)
			if (status==null){status="UNKNOWN"}
			
			if(status==statusToWait){
				println "";println "[S] Instance $path_instance is now  $status."
				return 0
			}
			
			laststatus=status
			
			// println System.currentTimeMillis() - startTime+"/"+timeout
			if( timeout != 0 && System.currentTimeMillis() - startTime >= timeout){
				println "";
				println("[E] !!! TIMEOUT !!!")
				println "[E] Instance $path_instance is currently  $status. WaitForStatus aborting..."
				return 1
			}
			
			
		}
		
		if(status==statusToWait){
			println "";println "[S] Instance $path_instance is now  $status."
			return 0
		}
		else{
			println "[E] Instance $path_instance is currently  $status."
			return 1
		}
		

	}
	
	
	
	
	
	static int astartInstance(def Jboss7Shell cli, def String path_instance, def String host="", def String  instance=""){

		if (path_instance == ""){
			path_instance="/host=$host/server-config=$instance"
		}
		
		println("[I] Async Start Domain Instance :$path_instance")
		if(!Jboss7CommonTools.isExistResource(cli,path_instance)){
			println "[E] instance $path_instance doesn't exist"
			return 1
		}
		
		def status=getStatusInstance(cli,path_instance)
		if (status==null){
			println("[E]  Impossible to control the status of instance: $path_instance")
			return 1
		}
		
		
		if(status == "STOPPED"  ){
		}
		else {
			   println("[E] Current status of instance $path_instance is =$status. This instance can't be started.")
			   return 1
		}
			
		
		
		// try to start instance
		try{
			def commandline="$path_instance:start"
			def result=cli.runCLICommand(commandline)
			if (result.isSuccess()){
				println("[S] Start of $path_instance")
				return 0
			}
		   else{
				def response = result.getResponse()
				println("[E] Start of $path_instance failed due to :"+response.asString())
				return 1
		   }
		}
		catch(Exception e){
			println "[E] Technical Error due to :"+e.getMessage()
			return 1
		}
	   
	   
	   
	}
	
	static int astopInstance(def Jboss7Shell cli, def String path_instance, def String host="", def String  instance=""){
		/********
		 * Liste des status possibles pour une instance Jboss7
		"DISABLED",
		"STARTING",
		"STARTED",
		"STOPPING",
		"STOPPED",
		"FAILED",
		"UNKNOWN",
		"DOES_NOT_EXIST"
		 */
		
		
		if (path_instance == ""){
			path_instance="/host=$host/server-config=$instance"
		}
		println("[I] Async Stop Domain Instance $path_instance ")
		
		
		if(!Jboss7CommonTools.isExistResource(cli,path_instance)){
			println "[E] instance $path_instance doesn't exist"
			return 1
		}
		
		def status=getStatusInstance(cli,path_instance)
		if (status==null){
			println("[E]  Impossible to control the status of instance: $path_instance")
			return 1
		}
		
		if(status == "STARTING" ||  status == "STARTED" || status == "STOPPING" || status == "FAILED"){
		}
		else {
			   println("[E] Current status of instance $path_instance is =$status. This instance can't be stopped.")
			   return 1
		}
		
		
		try{
			def commandline="$path_instance:stop"
			def result=cli.runCLICommand(commandline)
			if (result.isSuccess()){
				println("[S] Stop of  $path_instance")
				return 0
			}
		   else{
				def response = result.getResponse()
				println("[E] Stop of $path_instance failed due to :"+response.asString())
				return 1
		   }
		}
		catch(Exception e){
			println "[E] Technical Error due to :"+e.getMessage()
			return 1
		}
		
	}
	
	static int arestartInstance(def Jboss7Shell cli, def long timeout,def String path_instance, def String host="", def String  instance=""){
		/********
		 * Liste des status possibles pour une instance Jboss7
		"DISABLED",
		"STARTING",
		"STARTED",
		"STOPPING",
		"STOPPED",
		"FAILED",
		"UNKNOWN",
		"DOES_NOT_EXIST"
		 */
		
		
		if (path_instance == ""){
			path_instance="/host=$host/server-config=$instance"
		}
		println("[I] Async Restart Domain Instance $path_instance ")
		
		
		if(!Jboss7CommonTools.isExistResource(cli,path_instance)){
			println "[E] instance $path_instance doesn't exist"
			return 1
		}
		
		def state=getStatusInstance(cli,path_instance)
		if (state==null){
			println("[E]  Impossible to control the status of instance: $path_instance")
			return 1
		}
		
		def cr=1
		switch(state){
			case "STOPPING":
							println("[I] Instance $path_instance is already stopping");
							cr=stopInstance( cli, timeout,path_instance)
							break;
			case "STOPPED":
							println("[I] Instance $path_instance is already stopped");
							cr=0;
							break;
			case "STARTING":
							
			case "STARTED":
							
			case "FAILED": println("[I] Instance $path_instance is $state");
							cr=stopInstance( cli, timeout,path_instance)
							break;
							
			default: println("[E] Instance $path_instance has unknown state:$state"); return 1;break;
		}
		
		if(cr!= 0){
			println("[E] Impossible to stop $path_instance. Abort Restart operation...");
			return 1
		}
		cr=astartInstance( cli,path_instance)
		if(cr==0){
			println("[S] RetStart of $path_instance");
		}
		else{
			println("[E] Impossible to restart $path_instance...");
		}
		return cr
	}
	
	
	static int startInstance(def Jboss7Shell cli, def long timeout,def String path_instance, def String host="", def String  instance=""){
		/********
		 * Liste des status possibles pour une instance Jboss7
		"DISABLED",
		"STARTING",
		"STARTED",
		"STOPPING",
		"STOPPED",
		"FAILED",
		"UNKNOWN",
		"DOES_NOT_EXIST"
		 */
		if (path_instance == ""){
			path_instance="/host=$host/server-config=$instance"
		}
		
		println("[I] Start Domain Instance :$path_instance")
		if(!Jboss7CommonTools.isExistResource(cli,path_instance)){
			println "[E] instance $path_instance doesn't exist"
			return 1
		}
		
		
		
		
		
		// check that instance 's status is STOPPED
		def state=getStatusInstance(cli,path_instance)
		switch(state){
			case "STOPPED": break
			case "STARTING": println("[W] Instance $path_instance is already starting"); return 1; break;
			case "STOPPING": println("[E] Instance $path_instance is still stopping"); return 1;break;
			case "STARTED": println("[E] Instance $path_instance is already started"); return 1;break;
			case "FAILED" : println("[E] Instance $path_instance has state:$state"); return 1;break;
			case "DISABLED": println("[E] Instance $path_instance has state:$state"); return 1;break;
			case "UNKNOWN": println("[E] Instance $path_instance has state:$state"); return 1;break;
			case "DOES_NOT_EXIST": println("[E] Instance $path_instance has state:$state"); return 1;break;
			default: println("[E] Instance $path_instance has unknown state:$state"); return 1;break;break;
		}
		
		
		
		try{
			def response
			def result
			def commandline=""
			// try to start instance
			commandline="$path_instance:start"
			result=cli.runCLICommand(commandline)
			if (result.isSuccess()){
				println("[I] Start request 's been send to $path_instance")
			}
		   else{
				response = result.getResponse()
				println("[E] start request can't be send to $path_instance:"+response.asString())
				return 1
		   }
		}
		catch(Exception e){
			println "[E] Technical Error due to :"+e.getMessage()
			return 1
		}
	   
	   // wait for state instance equal to starting
	   
	   print "[I] Wait for Starting of $path_instance:."
	   def startTime = System.currentTimeMillis()
	   
	   while (state!= "STARTING" )   {
		   sleep(5000)
		   state=getStatusInstance(cli,path_instance)
		   
		   switch(state){
			   case "STOPPED": print "."; break// continue to loop
							   
			   case "STARTING": println "";println("[I] Instance $path_instance is $state"); break;// stop to loop
			   case "STARTED": println "";println("[S] Instance $path_instance is $state"); return 0;break;// stop to loop
			   case "FAILED": println "";println("[E] Start of Instance $path_instance is $state"); return 1;break;// stop to loop
							  break;
			   
			   case "STOPPING":
			   case "FAILED" :
			   case "DISABLED":
			   case "UNKNOWN":
			   case "DOES_NOT_EXIST":
			   default: println "";println("[E] Status of Instance $path_instance is now $state. Start of Instance failed"); return 1;break;
		   }
		   
		   
		   // println System.currentTimeMillis() - startTime +"/"+ timeout
		   if( timeout != 0 && System.currentTimeMillis() - startTime >= timeout){
			   println "";println("[E] !!! TIMEOUT !!!")
			   return 1
			   break;
		   }
	   
	   }
	   
	   // here state=STARTING
	   // wait for stateSTARTED
	   print "Wait for the end of starting of $path_instance:."
	   while ( state!= "STARTED" )   {
		   sleep(5000)
		   state=getStatusInstance(cli,path_instance)
		   
		   switch(state){
							   
			   case "STARTING": print ".";  break;// continue to loop
			   case "STARTED": println "";println("[S] Instance $path_instance is $state"); return 0;break;// stop to loop
			   case "FAILED": println "";println("[E] Start of Instance $path_instance is $state"); return 1;break;// stop to loop
							  break;
			   case "STOPPED":
			   case "STOPPING":
			   case "DISABLED":
			   case "UNKNOWN":
			   case "DOES_NOT_EXIST":
			   default: println "";println("[E] Status of Instance $path_instance is now $state.  Start of Instance failed"); return 1;break;
		   }
		   
		   // println System.currentTimeMillis() - startTime +"/"+ timeout
		   if( timeout != 0 && System.currentTimeMillis() - startTime >= timeout){
			   println "";println("[E] !!! TIMEOUT !!!")
			   return 1
		   }
	   
	   }
   
	   return 1
	   
	   
	}
	
	static int stopInstance(def Jboss7Shell cli, def long timeout,def String path_instance, def String host="", def String  instance=""){
		/********
		 * Liste des status possibles pour une instance Jboss7
		"DISABLED",
		"STARTING",
		"STARTED",
		"STOPPING",
		"STOPPED",
		"FAILED",
		"UNKNOWN",
		"DOES_NOT_EXIST"
		 */
		if (path_instance == ""){
			path_instance="/host=$host/server-config=$instance"
		}
		
		println("[I] Stop Domain Instance :$path_instance")
		if(!Jboss7CommonTools.isExistResource(cli,path_instance)){
			println "[E] instance $path_instance doesn't exist"
			return 1
		}
		
		
		// check that instance 's status is STOPPED
		def state=getStatusInstance(cli,path_instance)
		switch(state){
			case "STARTING":
			case "STARTED":
			case "FAILED" : break; // continue
			case "STOPPING": println("[W] Instance $path_instance is already stopping");break;// continue
			
			case "STOPPED": println("[E] Instance $path_instance is already stopped"); return 1;break;
			
			
			case "DISABLED": println("[E] Instance $path_instance has state:$state"); return 1;break;
			case "UNKNOWN": println("[E] Instance $path_instance has state:$state"); return 1;break;
			case "DOES_NOT_EXIST": println("[E] Instance $path_instance has state:$state"); return 1;break;
			default: println("[E] Instance $path_instance has unknown state:$state"); return 1;break;break;
		}
		
		
		
		if(state!= "STOPPING" )   {
			// state=STARTING,STARTED or FAILED
			// try to stop instance
			try{
				def commandline="$path_instance:stop"
				def result=cli.runCLICommand(commandline)
				if (result.isSuccess()){
					println("[I] Stop request 's been send to $path_instance")
				}
			   else{
					def response = result.getResponse()
					println("[E] Stop request can't be send to $path_instance:"+response.asString())
					return 1
			   }
			}
			catch(Exception e){
				println "[E] Technical Error due to :"+e.getMessage()
				return 1
			}
		}
	   
	   // wait for state instance equal to stopping
		// println System.currentTimeMillis() - startTime +"/"+ timeout of $path_instance:."
		def startTime = System.currentTimeMillis()
		
		while (state!= "STOPPING" )   {
			sleep(5000)
			state=getStatusInstance(cli,path_instance)
			
			switch(state){
				case "STARTED": print "."; break// continue to loop
				case "STOPPING": println "";println("[I] Instance $path_instance is $state"); break;// stop to loop
				
				case "STOPPED": println "";println("[S] Instance $path_instance is $state"); return 0;break;// stop to loop
				case "FAILED": println "";println("[E] Stop of Instance $path_instance is $state"); return 1;break;// stop to loop


				default: println "";println("[E] Status of Instance $path_instance is now $state. Stop of Instance failed"); return 1;break;
			}
			
			
			// println System.currentTimeMillis() - startTime +"/"+ timeout
			if( timeout != 0 && System.currentTimeMillis() - startTime >= timeout){
				println "";println("[E] !!! TIMEOUT !!!")
				return 1
				break;
			}
		
		}
		
		print "Wait for then end of Stop of $path_instance:."
		
		while (state!= "STOPPED" )   {
			sleep(1000)
			state=getStatusInstance(cli,path_instance)
			
			switch(state){
				case "STOPPING": print "."; break// continue to loop
				case "STOPPED": println "";println("[S] Instance $path_instance is $state"); return 0;break;// stop to loop
				case "FAILED": println "";println("[E] Stop of Instance $path_instance is $state"); return 1;break;// stop to loop

				default: println "";println("[E] Status of Instance $path_instance is now $state. Stop of Instance failed"); return 1;break;
			}
			
			
			// println System.currentTimeMillis() - startTime +"/"+ timeout
			if( timeout != 0 && System.currentTimeMillis() - startTime >= timeout){
				println "";println("[E] !!! TIMEOUT !!!")
				return 1
				break;
			}
		
		}
		

		return 1
	   
	}
	
	static int restartInstance(def Jboss7Shell cli, def long timeout,def String path_instance, def String host="", def String  instance=""){
		/********
		 * Liste des status possibles pour une instance Jboss7
		"DISABLED",
		"STARTING",
		"STARTED",
		"STOPPING",
		"STOPPED",
		"FAILED",
		"UNKNOWN",
		"DOES_NOT_EXIST"
		 */
		if (path_instance == ""){
			path_instance="/host=$host/server-config=$instance"
		}
		
		println("[I] ReStart Domain Instance :$path_instance")
		if(!Jboss7CommonTools.isExistResource(cli,path_instance)){
			println "[E] instance $path_instance doesn't exist"
			return 1
		}
		
		
		// check that instance 's status is STOPPED
		def state=getStatusInstance(cli,path_instance)
		switch(state){
			case "STARTING":
			case "STARTED":
			case "FAILED" : break; // continue
			case "STOPPING": println("[W] Instance $path_instance is already stopping");break;// continue
			
			case "STOPPED": println("[E] Instance $path_instance is already stopped"); return 1;break;
			
			
			case "DISABLED": println("[E] Instance $path_instance has state:$state"); return 1;break;
			case "UNKNOWN": println("[E] Instance $path_instance has state:$state"); return 1;break;
			case "DOES_NOT_EXIST": println("[E] Instance $path_instance has state:$state"); return 1;break;
			default: println("[E] Instance $path_instance has unknown state:$state"); return 1;break;break;
		}
		
		def cr=0
		switch(state){
			case "STARTING":
			case "STARTED":
			case "FAILED" :
							println("[I] Instance $path_instance is currently $state");
							cr=stopInstance( cli, timeout,path_instance, host,  instance)
							break;
			case "STOPPED":
							println("[I] Instance $path_instance is already stopped");
							cr=0;
							break;
			
			case "DISABLED":
			case "UNKNOWN":
			case "DOES_NOT_EXIST":
			default: println("[E] Instance $path_instance has unknown state:$state"); return 1;break;
		}
		
		if(cr!= 0){
			println("[E] Impossible to stop $path_instance. Abort Restart operation...");
			return 1
		}
		cr=startInstance( cli, timeout,path_instance, host,  instance)
		if(cr==0){
			println("[S] Instance $path_instance is RESTARTED")
		}
		else{
			println("[E] Retsart of instance $path_instance FAILED")
		}
		return cr
	}
	
	
	static int waitForstatusForListOfInstances(def Jboss7Shell cli, def String statusToWait, def long timeout,def List<String>  path_instanceLst,def Boolean failOnError=true){
		def status=0
		try{

			path_instanceLst.each{
				it ->
				def cr=waitForstatusOfInstance(cli, statusToWait, timeout, it)
				if (cr!= 0){
					//println "failOnError=$failOnError"
					if(failOnError) { // break
						status=1
						throw new Exception ("waitForstatusOfInstance of instance $it failed. Abort waitForstatusForListOfInstances action...")
					}
					else{
						status=2 // status de  type WARNING
					}
				}
				
			}
		}
		catch(Exception e){
			println "[E] "+e.getMessage()
			return 1
		}
		
		return status
		

	}
	
	static int getPidOfListOfInstances(def Jboss7Shell cli, def List<String>  path_instanceLst,def Boolean failOnError=true){
		def status=0
		try{

			path_instanceLst.each{
				it ->
				def cr=getPidInstance(cli, it)
				if (cr!= 0){
					//println "failOnError=$failOnError"
					if(failOnError) { // break
						status=1
						throw new Exception ("getPidOfListOfInstances of instance $it failed. Abort getPidOfListOfInstances action...")
					}
					else{
						status=2 // status de  type WARNING
					}
				}
				
			}
		}
		catch(Exception e){
			println "[E] "+e.getMessage()
			return 1
		}
		return status
	}


	
	
	
	
	
	static int statusListOfInstances(def Jboss7Shell cli, def List<String>  path_instanceLst){
		/********
		 * Liste des status possibles pour une instance Jboss7
		"DISABLED",
		"STARTING",
		"STARTED",
		"STOPPING",
		"STOPPED",
		"FAILED",
		"UNKNOWN",
		"DOES_NOT_EXIST"
		 */
		def cr=0
		path_instanceLst.each{def String instanceScope ->
			def String statusInstance=statusInstance(  cli, instanceScope, "","")
			if(statusInstance==null){
				cr=1
			}

		}
		return cr
	}
	
	static int astartListOfInstances(def Jboss7Shell cli, def List<String> path_instanceLst,def Boolean failOnError=true){
		def status=0
		try{

			path_instanceLst.each{
				it ->
				def cr=astartInstance(  cli, it,"","")
				if (cr!= 0){
					//println "failOnError=$failOnError"
					if(failOnError) { // break
						status=1
						throw new Exception ("astart of instance $it failed. Abort astartListOfInstances action...")
					}
					else{
						status=2 // status de  type WARNING
					}
				}
				
			}
		}
		catch(Exception e){
			println "[E] "+e.getMessage()
			return 1
		}
		
		return status
	}
	
	
	
	 static int astopListOfInstances(def Jboss7Shell cli, def List<String> path_instanceLst,def Boolean failOnError=true){
		def status=0
		try{
			path_instanceLst.each{
				it ->
				def cr=astopInstance(  cli, it,"","")
				if (cr!= 0){
					//println "failOnError=$failOnError"
					if(failOnError) { // break
						status=1
						throw new Exception ("astop of instance $it failed. Abort astopListOfInstances action...")
					}
					else{
						status=2 // status de  type WARNING
					}
				}
			}
		}
		catch(Exception e){
			println "[E] "+e.getMessage()
			return 1
		}
		return status

	 }
	 
	 
	 

	 static int startListOfInstances(def Jboss7Shell cli,def long timeout, def List<String> path_instanceLst,def Boolean failOnError=true){
		def status=0
		try{
			path_instanceLst.each{
				it ->
				def cr=startInstance(  cli,timeout, it)
				if (cr!= 0){
					if(failOnError) { // break
						status=1
						throw new Exception ("start of instance $it failed. Abort startListOfInstances action...")
					}
					else{
						status=2 // status de  type WARNING
					}
				}
			}
		}
		catch(Exception e){
			println "[E] "+e.getMessage()
			return 1
		}
		return status
	 }
	 
	 static int stopListOfInstances(def Jboss7Shell cli,def long timeout, def List<String> path_instanceLst,def Boolean failOnError=true){
		 def status=0
		 try{
			 path_instanceLst.each{
				 it ->
				 def cr=stopInstance(  cli,timeout, it)
				 if (cr!= 0){
					 if(failOnError) { // break
						 status=1
						 throw new Exception ("stop of instance $it failed. Abort stopListOfInstances action...")
					 }
					 else{
						 status=2 // status de  type WARNING
					 }
				 }
			 }
		 }
		 catch(Exception e){
			 println "[E] "+e.getMessage()
			 return 1
		 }
		 return status
	  }
	 
	 
	 
	 
	 static int arestartListOfInstances(def Jboss7Shell cli,def long timeout, def List<String> path_instanceLst,def Boolean failOnError=true){
		 def status=0
		 try{
			 path_instanceLst.each{
				 it ->
				 def cr=arestartInstance(  cli,timeout, it)
				 if (cr!= 0){
					 if(failOnError) { // break
						 status=1
						 throw new Exception ("stop of instance $it failed. Abort stopListOfInstances action...")
					 }
					 else{
						 status=2 // status de  type WARNING
					 }
				 }
			 }
		 }
		 catch(Exception e){
			 println "[E] "+e.getMessage()
			 return 1
		 }
		 return status
	  }
	 
	 

	 

	
	 static int restartListOfInstances(def Jboss7Shell cli,def long timeout,def String mode, def List<String> path_instanceLst,def Boolean failOnError=true){
		 def status=0
		 switch (mode){
			 case "OneByOne": // mode cascade
				 try{
					 path_instanceLst.each{
						 it ->
						 def cr=restartInstance(  cli,timeout, it)
						 if (cr!= 0){
							 if(failOnError) { // break
								 status=1
								 throw new Exception ("restart of instance $it failed. Abort restartListOfInstances action...")
							 }
							 else{
								 status=2 // status de  type WARNING
							 }
						 }
					 }
				 }
				 catch(Exception e){
					 println "[E] "+e.getMessage()
					 return 1
				 }
					 
				 
				 break;
				 
			 case "StopAllAndStartAll":
				 try{
					 path_instanceLst.each{
							 it ->
							 def state=statusInstance( cli, it)
							 switch(state){
								 
								 case "STOPPED": println("[I] Instance $it is already stopped"); ;break;
								 case "STARTED":
								 case "STOPPING":
								 case "STARTING":
								 case "FAILED":
									 def cr=stopInstance(  cli,timeout, it)
									 if (cr!= 0){
									  if(failOnError) { // break
											 status=1
											 throw new Exception ("stop of instance $it failed. Abort restartListOfInstances action...")
										 }
										 else{
											 status=2 // status de  type WARNING
										 }
									 }
									 break
								
								 
									 
									
								 
								 default: println("[E] Instance $it has unknown state:$state"); break;
									 if(failOnError) { // break
										 status=1
										 throw new Exception (" Instance $it has unknown state:$state Abort restartListOfInstances action...")
									 }
									 else{
										 status=2 // status de  type WARNING
									 }
							 }
							 
							 
						 }
					 }
					 catch(Exception e){
						 println "[E] "+e.getMessage()
						 return 1
					 }
					 
					 // start all instance
					 def cr=startListOfInstances(cli,0,path_instanceLst ,failOnError)
					 if (cr!= 0){
						 if(failOnError) { // break
							 status=1
						 }
						 else{
							 status=2 // status de  type WARNING
						 }
					 }
				 
				 
					 break;
				 default:
					 break;
				 
		 }
		 
		 return status
	 }
	 
	 
	 
	
	 	 
	 static public String getStatusHostController(def Jboss7Shell cli, def String path_slave){
		 
		 def commandline="$path_slave:read-attribute(name=host-state)"
		 def result=cli.runCLICommand(commandline)
		 if (result.isSuccess()){
			 return result.getResponse().get("result").asString().toUpperCase()
		 }
		 else{
			 return null
		 }
	 }
	 static def reloadtHostController(def Jboss7Shell cli, def List<String> scopeSlaveList,def long timeout){
		 def cr=0
		 
		 scopeSlaveList.each{
			 def String scopeSlave->
			 if(!Jboss7CommonTools.isExistResource(cli,scopeSlave)){
				 println "[E] instance $scopeSlave isn't online"
				 cr=1
			 }
			 else{
				 def commandline="$scopeSlave:reload"
				 def result=cli.runCLICommand(commandline)
				 if (result.isSuccess()){
					 println "[I] Reload Request has been send to $scopeSlave:reload"
					 if(!cli.waitForReconnect(timeout)){
						 println "[E] Reload Of $scopeSlave:reload Failed"
						 cr=1
					 }
					 else{
						 if(waitForstatusOfHostController(cli, "RUNNING",  timeout,scopeSlave) != 0){
							 println "[E] Reload Of $scopeSlave:reload Failed"
							 cr=1
						 }
						 else{
							 println "[S] Reload Of $scopeSlave:reload [OK]"
							 
						 }
					 }
				 }
				 else{
					 println "[E] Reload Of $scopeSlave:reload Failed"
					 cr=1
				 }
			 }
		 }
		 return cr
	 }
	 static int waitForstatusOfHostController(def Jboss7Shell cli, def String statusToWait, def long timeout,def String path_slave, def String host="", def String  instance=""){
		 
		 
		 def cr=0
		 
		 switch(statusToWait){
			 case "RUNNING":
			 case "STARTING":
			 case "STARTED":
			 case "STOPPING":
			 case "STOPPED":
			 case "FAILED":
			 case "UNKNOWN":
			 case "DOES_NOT_EXIST":
				 break;
			 default:
				 println("[E] Invalid status : $statusToWait ");
				 return 1;
				 break;
		 }
		 
		 
		 
		 // by default wait for terminate state or status=statusToWait
		 
		 def status=getStatusHostController(cli,path_slave)
		 println "status=$status"
		 if (status==null){status="UNKNOWN"}
		 if(status==statusToWait){
			 println "";println "[S] Host $path_slave is already  $status."
			 return 0
		 }
		 
		 def laststatus=status
		 
		 println "[I] Instance $path_slave is currently  $status."
		 print "[I] Wait for status $statusToWait:."
		 def startTime = System.currentTimeMillis()
		 while(laststatus==status || status== "STARTING" ||  status== "STOPPING" ||  status== "UNKNOWN"   ){
			 // wait 5 secondes
			 sleep(5000)
			 status=getStatusHostController(cli,path_slave)
			 if (status==null){status="UNKNOWN"}
			 laststatus=status
			 
			 if(status==statusToWait){
				 println "";println "[S] Instance $path_slave is now  $status."
				 return 0
			 }
			 // println System.currentTimeMillis() - startTime+"/"+timeout
			 if( timeout != 0 && System.currentTimeMillis() - startTime >= timeout){
				 println "";
				 println("[E] !!! TIMEOUT !!!")
				 println "[E] Instance $path_slave is currently  $status. WaitForStatus aborting..."
				 return 1
			 }
			 
			 
		 }
		 
		 if(status==statusToWait){
			 println "";println "[S] Instance $path_slave is now  $status."
			 return 0
		 }
		 else{
			 println "[E] Instance $path_slave is currently  $status."
			 return 1
		 }
		 
 
	 }
	 

}
