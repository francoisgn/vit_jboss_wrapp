package fr.visionitgroup.groovylib.jboss7

import java.io.File;
import java.util.List;
import java.util.Map;



import fr.visionitgroup.groovylib.jboss7.Jboss7Shell

class Jboss7DomainInstanceMgr {
	
	
	
	
	static public String getPidDomainInstance(def Jboss7Shell cli, def String path_instance,def String host="", def String  instance=""){
		
		
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
  
  static public String getPidStandaloneInstance(def Jboss7Shell cli ){
		// calcul du path_runtime
		def String path_runtime="/core-service=platform-mbean/type=runtime"
		
		
		def String pidStr=Jboss7CommonTools.getAttributeValue(cli, path_runtime, "name")
		if(pidStr != null){
			   def pid=""
			   def hostname=""
			   if(pidStr.split("@")[0]!= null){pid=pidStr.split("@")[0]}
			   if(pidStr.split("@")[1]!= null){hostname=pidStr.split("@")[1]}
			   
			   return "standalone:$hostname:$pid"
		}
		else{
			   return "standalone::"
		}
  }
  
  static public String getStatusDomainInstance(def Jboss7Shell cli, def String path_instance){
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
  
  
  
  
  
  
  
  static int statusDomainInstance(def Jboss7Shell cli, def String path_instance, def String host="", def String  instance=""){
		def cr=0
		if (path_instance == ""){
			   path_instance="/host=$host/server-config=$instance"
		}
		
		if(!Jboss7CommonTools.isExistResource(cli,path_instance)){
			   println "[E] Status of instance $path_instance : DOES_NOT_EXIST"
			   return 1
		}
		
		def status=getStatusDomainInstance(cli,path_instance)
		if (status==null){
			   println "[E] Status of instance $path_instance : UNKNOWN"
			   return 1
		}
		println "[I] Status of instance $path_instance : $status"
		return 0
  }
  
  static int waitForstatusOfDomainInstance(def Jboss7Shell cli, def String statusToWait, def long timeout,def String path_instance, def String host="", def String  instance=""){
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
		
		def status=getStatusDomainInstance(cli,path_instance)
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
			   status=getStatusDomainInstance(cli,path_instance)
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
  
  
  
  
  
  static int astartDomainInstance(def Jboss7Shell cli, def String path_instance, def String host="", def String  instance=""){

		if (path_instance == ""){
			   path_instance="/host=$host/server-config=$instance"
		}
		
		println("[I] Async Start Domain Instance :$path_instance")
		if(!Jboss7CommonTools.isExistResource(cli,path_instance)){
			   println "[E] instance $path_instance doesn't exist"
			   return 1
		}
		
		def status=getStatusDomainInstance(cli,path_instance)
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
  
  static int astopDomainInstance(def Jboss7Shell cli, def String path_instance, def String host="", def String  instance=""){
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
		
		def status=getStatusDomainInstance(cli,path_instance)
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
  
  static int arestartDomainInstance(def Jboss7Shell cli, def long timeout,def String path_instance, def String host="", def String  instance=""){
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
		
		def state=getStatusDomainInstance(cli,path_instance)
		if (state==null){
			   println("[E]  Impossible to control the status of instance: $path_instance")
			   return 1
		}
		
		def cr=1
		switch(state){
			   case "STOPPING":
										  println("[I] Instance $path_instance is already stopping");
										  cr=stopDomainInstance( cli, timeout,path_instance)
										  break;
			   case "STOPPED":
										  println("[I] Instance $path_instance is already stopped");
										  cr=0;
										  break;
			   case "STARTING":
										  
			   case "STARTED":
										  
			   case "FAILED": println("[I] Instance $path_instance is $state");
										  cr=stopDomainInstance( cli, timeout,path_instance)
										  break;
										  
			   default: println("[E] Instance $path_instance has unknown state:$state"); return 1;break;
		}
		
		if(cr!= 0){
			   println("[E] Impossible to stop $path_instance. Abort Restart operation...");
			   return 1
		}
		cr=astartDomainInstance( cli,path_instance)
		if(cr==0){
			   println("[S] RetStart of $path_instance");
		}
		else{
			   println("[E] Impossible to restart $path_instance...");
		}
		return cr
  }
  
  
  static int startDomainInstance(def Jboss7Shell cli, def long timeout,def String path_instance, def String host="", def String  instance=""){
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
		def state=getStatusDomainInstance(cli,path_instance)
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
		   state=getStatusDomainInstance(cli,path_instance)
		   
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
		   state=getStatusDomainInstance(cli,path_instance)
		   
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
  
  static int stopDomainInstance(def Jboss7Shell cli, def long timeout,def String path_instance, def String host="", def String  instance=""){
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
		def state=getStatusDomainInstance(cli,path_instance)
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
			   state=getStatusDomainInstance(cli,path_instance)
			   
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
			   state=getStatusDomainInstance(cli,path_instance)
			   
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
  
  static int restartDomainInstance(def Jboss7Shell cli, def long timeout,def String path_instance, def String host="", def String  instance=""){
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
		def state=getStatusDomainInstance(cli,path_instance)
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
										  cr=stopDomainInstance( cli, timeout,path_instance, host,  instance)
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
		cr=startDomainInstance( cli, timeout,path_instance, host,  instance)
		if(cr==0){
			   println("[S] Instance $path_instance is RESTARTED")
		}
		else{
			   println("[E] Retsart of instance $path_instance FAILED")
		}
		return cr
  }
  
  
  static int waitForstatusForListOfDomainInstance(def Jboss7Shell cli, def String statusToWait, def long timeout,def List<String>  path_instanceLst,def Boolean failOnError=true){
		def status=0
		try{

			   path_instanceLst.each{
					  it ->
					  def cr=waitForstatusOfDomainInstance(cli, statusToWait, timeout, it)
					  if (cr!= 0){
							 //println "failOnError=$failOnError"
							 if(failOnError) { // break
								   status=1
								   throw new Exception ("waitForstatusOfDomainInstance of instance $it failed. Abort waitForstatusForListOfDomainInstance action...")
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
  
  
  
  
  static int statusListOfDomainInstance(def Jboss7Shell cli, def List<String>  path_instanceLst){
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
			   def String statusInstance=statusDomainInstance(  cli, instanceScope, "","")
			   if(statusInstance==null){
					  cr=1
			   }

		}
		return cr
  }
  
  static int astartListOfDomainInstance(def Jboss7Shell cli, def List<String> path_instanceLst,def Boolean failOnError=true){
		def status=0
		try{

			   path_instanceLst.each{
					  it ->
					  def cr=astartDomainInstance(  cli, it,"","")
					  if (cr!= 0){
							 //println "failOnError=$failOnError"
							 if(failOnError) { // break
								   status=1
								   throw new Exception ("astart of instance $it failed. Abort astartListOfDomainInstance action...")
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
  
  
  
  static int astopListOfDomainInstance(def Jboss7Shell cli, def List<String> path_instanceLst,def Boolean failOnError=true){
		def status=0
		try{
			   path_instanceLst.each{
					  it ->
					  def cr=astopDomainInstance(  cli, it,"","")
					  if (cr!= 0){
							 //println "failOnError=$failOnError"
							 if(failOnError) { // break
								   status=1
								   throw new Exception ("astop of instance $it failed. Abort astopListOfDomainInstance action...")
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
  
   
   

  static int startListOfDomainInstance(def Jboss7Shell cli,def long timeout, def List<String> path_instanceLst,def Boolean failOnError=true){
		def status=0
		try{
			   path_instanceLst.each{
					  it ->
					  def cr=startDomainInstance(  cli,timeout, it)
					  if (cr!= 0){
							 if(failOnError) { // break
								   status=1
								   throw new Exception ("start of instance $it failed. Abort startListOfDomainInstance action...")
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
  
   static int stopListOfDomainInstance(def Jboss7Shell cli,def long timeout, def List<String> path_instanceLst,def Boolean failOnError=true){
		def status=0
		try{
			   path_instanceLst.each{
					  it ->
					  def cr=stopDomainInstance(  cli,timeout, it)
					  if (cr!= 0){
							 if(failOnError) { // break
								   status=1
								   throw new Exception ("stop of instance $it failed. Abort stopListOfDomainInstance action...")
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
  
   
   
   
   static int arestartListOfDomainInstance(def Jboss7Shell cli,def long timeout, def List<String> path_instanceLst,def Boolean failOnError=true){
		def status=0
		try{
			   path_instanceLst.each{
					  it ->
					  def cr=arestartDomainInstance(  cli,timeout, it)
					  if (cr!= 0){
							 if(failOnError) { // break
								   status=1
								   throw new Exception ("stop of instance $it failed. Abort stopListOfDomainInstance action...")
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
  
   

  

  
  static int restartListOfDomainInstance(def Jboss7Shell cli,def long timeout,def mode, def List<String> path_instanceLst,def Boolean failOnError=true){
		def status=0
		switch (mode){
			   case "OneByOne": // mode cascade
					  try{
							 path_instanceLst.each{
								   it ->
									def cr=restartDomainInstance(  cli,timeout, it)
								   if (cr!= 0){
										  if(failOnError) { // break
												 status=1
												 throw new Exception ("restart of instance $it failed. Abort restartListOfDomainInstance action...")
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
										  def state=statusDomainInstance( cli, it)
										  switch(state){
												 
												  case "STOPPED": println("[I] Instance $it is already stopped"); ;break;
												 case "STARTED":
												 case "STOPPING":
												 case "STARTING":
												 case "FAILED":
													   def cr=stopDomainInstance(  cli,timeout, it)
													   if (cr!= 0){
														 if(failOnError) { // break
																	 status=1
																	 throw new Exception ("stop of instance $it failed. Abort restartListOfDomainInstance action...")
															  }
															  else{
																	 status=2 // status de  type WARNING
															  }
													   }
													   break
												 
												 
														
													   
												  
												  default: println("[E] Instance $it has unknown state:$state"); break;
													   if(failOnError) { // break
															  status=1
															  throw new Exception (" Instance $it has unknown state:$state Abort restartListOfDomainInstance action...")
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
							 def cr=startListOfDomainInstance(cli,0,path_instanceLst ,failOnError)
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
  
   
   
// split
	
	
	
	
	static int astartListOfDomainInstance(def Jboss7Shell cli, def String[] path_instanceLst){
		def status=0
		path_instanceLst.each{
			it -> def cr=astartDomainInstance(  cli, it,"","")
			if (cr!= 0){status=1}
			
		}
		return status
	}

	 
	 static int astopListOfDomainInstance(def Jboss7Shell cli, def String[] path_instanceLst){
		 def status=0
		 path_instanceLst.each{
			 it -> def cr=astopDomainInstance(  cli, it, "","")
			 if (cr!= 0){status=1}
			 
		 }
		 return status
	 }
	 
	 
	 static String statusListOfDomainInstance(def Jboss7Shell cli, def String[] path_instanceLst){
		 def status=0
		 path_instanceLst.each{
			 it -> def cr=statusDomainInstance(  cli, it, "","")
			 if (cr!= 0){status=1}
			 
		 }
		 return status
	 }
	 
	 
	 static int arestartListOfDomainInstance(def Jboss7Shell cli,def timeout, def String[] path_instanceLst){
		 def status=0
		 path_instanceLst.each{
			 it -> def cr=arestartDomainInstance(  cli,timeout, it, "","")
			 if (cr!= 0){status=1}
			 
		 }
		 return status
	 }
	 static int arestartDomainInstance(def Jboss7Shell cli, def timeout,def String path_instance, def String host="", def String  instance=""){
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
	 
	 static int startListOfDomainInstance(def Jboss7Shell cli,def timeout, def String[] path_instanceLst){
		 def status=0
		 path_instanceLst.each{
			 it -> def cr=startDomainInstance(  cli,timeout, it,"","")
			 if (cr!= 0){status=1}
			 
		 }
		 return status
	 }
	 static int startDomainInstance(def Jboss7Shell cli, def timeout,def String path_instance, def String host="", def String  instance=""){
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
	 static int restartListOfDomainInstance(def mode,def Jboss7Shell cli,def timeout, def String[] path_instanceLst){
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
	 static int restartDomainInstance(def Jboss7Shell cli, def timeout,def String path_instance, def String host="", def String  instance=""){
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
	 static int stopDomainInstance(def Jboss7Shell cli, def timeout,def String path_instance, def String host="", def String  instance=""){
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
	 
	 static def int startStandaloneServer(def Jboss7Shell cli,def String exec_path,def List exec_args, def File exec_workdir, def Map exec_env, def timeout) {
		
		 println("[A] Start Standalone Server")
		 def cmdArgs =[exec_path]
		cmdArgs.addAll(exec_args)
		
		println "cmdArgs"+cmdArgs
		println "[I] Builting new process;  "+cmdArgs
	 
	 
		/***********
		 * A revoir
		 
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
		***********/
	}
	 
	 static def int stopStandaloneServer(def Jboss7Shell cli,def int timeout){
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
	 
	 
	 static def String statusStandaloneServer(def Jboss7Shell cli){
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
	 
	 static def int areloadStandaloneServer(def Jboss7Shell cli){
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
	 
	 static def int reloadStandaloneServer(def Jboss7Shell cli,timeout){
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
	 
	 static def restartStandaloneServer(def Jboss7Shell cli,def String exec_path,def List exec_args, def File exec_workdir, def Map exec_env, def timeout) {
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
	 static def astartStandaloneServer(def Jboss7Shell cli,def String exec_path,def List exec_args, def File exec_workdir, def Map exec_env) {
		 println("[A] Start Standalone Server")
		 def cmdArgs =[exec_path]
		cmdArgs.addAll(exec_args)
		
		println "cmdArgs"+cmdArgs
		println "[I] Builting new process;  "+cmdArgs
	 
	 
		/*****
		 * Arevoir
		 
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
		*****/
		 
	 }
	 static def arestartStandaloneServer(def Jboss7Shell cli,def String exec_path,def List exec_args, def File exec_workdir, def Map exec_env,def int timeout) {
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
	 static def astopStandaloneServer(def Jboss7Shell cli,def String exec_path,def List exec_args, def File exec_workdir, def Map exec_env, def timeout) {
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
	 static def startStopRestartHostController(){
	 
	 }
	 static def startStopRestartDomainController(){
	 
	 }
	 
	 static def startStopRestartDomainServerGroup(){
	 
	 }

}
