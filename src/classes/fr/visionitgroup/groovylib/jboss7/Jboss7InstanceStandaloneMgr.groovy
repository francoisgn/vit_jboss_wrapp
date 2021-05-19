package fr.visionitgroup.groovylib.jboss7

import java.io.File;
import java.util.List;
import java.util.Map;


import fr.visionitgroup.groovylib.jboss7.Jboss7Shell

class Jboss7InstanceStandaloneMgr {
	
	
		
	static public String getPidInstance(def Jboss7Shell cli ){
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
	


	 
	 
	 
	
	 
	 static def int statusInstance(def Jboss7Shell cli){
		 
		 def cr=0
		 // test si l'instance est bien demarrée
		 if(!cli.isControllerListen()){
			 println("[I] Status of Standalone Instance ("+cli.getControllerHost()+":"+cli.getControllerPort()+"): STOPPED")
			 return 0
		 }
		 
		 
		 // connection to Controller
		 if(!cli.connect()){
			 println("[I] Status of Standalone Instance ("+cli.getControllerHost()+":"+cli.getControllerPort()+"): UNKNOW")
			 return 1
		 }
		 
		 def status=getStatusInstance(cli)
		 
		 println("[I] Status of Standalone Instance ("+cli.getControllerHost()+":"+cli.getControllerPort()+"): $status")

		 return cr
	 }
	 static def int startInstance(def Jboss7Shell cli,def String exec_path,def List exec_args, def String exec_workdir, def List exec_env, def long timeout_sec) {
		
		 
		 
		 println("[I] Start Standalone Server "+cli.getControllerHost()+":"+cli.getControllerPort())
		 
		 def status=getStatusInstance(cli)
		 switch (status){
			 case "STARTING":
			 case "RESTART_REQUIRED":
			 case "RUNNING":println "[E] Instance already started or starting"; return 1; break;
			 case "UNKNOWN": println "[E] Current status of Instance is $status "; return 1;
				 break;
			 
			 case "STOPPED": break;

		 }
		 
		 
		 // format la start command line
		 
		 //execute_background_cmdline
		 def List<String> cmdArgs =[]
		cmdArgs.add( exec_path)
		cmdArgs.addAll(exec_args)
		
		
		// execute_background command
		// static def int execute_background_cmdline(def List envp=[],def String workdir,def List<String> command)
		println "[D] "+ cmdArgs
		
		def File exec_workdirFile=null
		if(exec_workdir!=""){ exec_workdirFile=new File(exec_workdir)}
		def cr=Jboss7CommonTools.execute_background_cmdline(exec_env,exec_workdirFile,cmdArgs)
		 if(cr!=0){
			 print "[E] Impossible to start of Instance"
			 return 1
		 }
		 
		 // wait for Ping of Controller
		 if(cli.waitForControllerListen(timeout_sec)!=0){
			 print "[E] Start of Instance failed: Impossible to ping Jboss Controller"
			 return 1
		 }
		 
		 // wait for Cli Connexion
		 if(!cli.waitForReconnect(timeout_sec)){
			 print "[E] Start of Instance failed : Impossible to connect to Jboss Controller"
			 return 1
		 }
		 
		 
		 
		 // wait for STARTED State
		 status=""
		 if(timeout_sec <=0){
			 System.out.print("[W] Wait for RUNNING status (Retry in each 5 sec...):")
			 while (status!= "RUNNING"  )   {
				 
				 try {
					 status=getStatusInstance(cli)
					 System.out.print "."
					 sleep(5000)

				 }
				 catch (Exception e) {
					 System.out.print "."
					 sleep(5000)
				 }
				 
				 
			 }
			 System.out.println ""
			 println "[S] Start of Instance [OK] "
			 return 0
		 }
		 else{
			 def long timeout=timeout_sec*1000 // convert in millisec
			 
			 
			 System.out.print("[W] Wait for RUNNING status (Retry in each 5 sec...):")
			 def startTime = System.currentTimeMillis()
			 while (status!= "RUNNING"  )   {
				try {
					 status=getStatusInstance(cli)
					 System.out.print "."
					 sleep(5000)

				 }
				 catch (Exception e) {
					 System.out.print "."
					 sleep(5000)
				 }
				 
				 //println "[D]"+System.currentTimeMillis() - startTime +"/"+ timeout
				 if(  System.currentTimeMillis() - startTime >= timeout){
					 break;
				 }
				 
			 }
			 System.out.println ""
			 if (status!= "RUNNING"){
				 println ("[E] Timeout ($timeout millisec) : Impossible to know if instance has finished to start ")
				 return 1
			 }
			 else{
				 println "[S] Start of Instance [OK] "
				 return 0
			 }
		 }
					  
		
	}
	 
	 static def int stopInstance(def Jboss7Shell cli,def long timeout_sec){
		 println("[I] Stop Standalone Server"+cli.getControllerHost()+":"+cli.getControllerPort())
		 def status=getStatusInstance(cli)
		 switch (status){
			 case "STARTING":
			 case "RESTART_REQUIRED":
			 case "RUNNING":break;
			 case "UNKNOWN": println "[E] Current status of Instance is $status "; return 1;
				 break;
			 
			 case "STOPPED":println "[E] Instance already stopped"; return 1; break;

		 }
		 
		 
		 // Print PID of instance
		 println "Instance PID:"+getPidInstance(cli)
		 // shutdown   the instance
		 def commandline="/:shutdown"
		 try {

			 
			 
			 
			 def result=cli.runCLICommand(commandline)
			 if (result.isSuccess()){
				 println("[I] a Shutdown request has been send to server")
			 }
			 else{
				 println("[E] a Shutdown request can't be send to server succesfully. Abort...")
				 return 1
			 }
		 }
		 catch (Exception e) {
			 println("[E]  Impossible to send a Shutdown Request to then Standalone Server. Command $commandline Fialed ...Abort...")
			 return 1
		 }
		 
		 // wait the completed stop
		 println("[I] Wait for the completed shutdown of the server")
		 def startTime = System.currentTimeMillis()
		 
		 while (cli.isControllerListen() && ( System.currentTimeMillis() - startTime < timeout_sec)) {
				 sleep(5000)
		 }
		 if (!cli.isControllerListen()){
			 println "[S] Standalone Server has finished to shutdown"
			 cli._disconnect()
			 return 0
		 }
		 else{
			 println "[E] Shutdown Request has been send to the server. But Standalone Server seem to be stil alive after timeout (listen on "+cli.getControllerHost()+":"+cli.getControllerPort() +" ) "
			 return 1
		 }
			 
		 
		 
	 }
	 
	 
	 static def int reloadInstance(def Jboss7Shell cli,def long timeout_sec){
		 println("[I] Reload Standalone Server"+cli.getControllerHost()+":"+cli.getControllerPort())
		 // test si l'instance est bien demarrée
		 if(!cli.isControllerListen()){
			 println "[E] Instance "+cli.getControllerHost()+":"+cli.getControllerPort() +"  seem to be down"
			 return 1
		 }
		 
		 
		 // connection to Controller
		 if(!cli.connect()){
			 println "[E] Impossible to connect to Instance "+cli.getControllerHost()+":"+cli.getControllerPort()
			 return 1
		 }
		 
		 
		 // reload   the instance
		 def commandline="/:reload"
		 try {
			 def result=cli.runCLICommand(commandline)
			 if (result.isSuccess()){
				 println("[I] a reload request has been send to server")
			 }
			 else{
				 println("[E] a reload request can't be send to server succesfully. Abort...")
				 return 1
			 }
		 }
		 catch (Exception e) {
			 println("[E]  Impossible to send a reload Request to then Standalone Server. Command $commandline Fialed ...Abort...")
			 return 1
		 }
		 
		 
		 if(!cli.waitForReconnect(timeout_sec)){
			 println("[E]  Impossible to reconnect to Jboss Controller...")
			 println "[E] Reload of Instance [KO]"
			 return 1
		 }
		 else{
			 println "[S] Reload of Instance [OK]"
			 return 0
		 }
		 
	 }
	 
	 static def restartInstance(def Jboss7Shell cli,def String exec_path,def List exec_args, def String exec_workdir, def List exec_env, def long timeout_sec) {
		 println("[I] Restart Standalone Server"+cli.getControllerHost()+":"+cli.getControllerPort())
		 
		 
		 
		 def status=getStatusInstance(cli)
		 switch (status){
			 case "STARTING":
			 case "RESTART_REQUIRED":
			 case "RUNNING":
			 case "UNKNOWN":
				 if(stopInstance(cli,timeout_sec)!=0){
					 println "[E] Impossible to restart Instance "+cli.getControllerHost()+":"+cli.getControllerPort()
					 return 1
				 }
				 break;
			 
			 case "STOPPED":break;
			 
		 
		 }
		 
		 if(startInstance( cli,exec_path,exec_args,exec_workdir, exec_env,timeout_sec)!= 0 ){
			 println "[E] Impossible to restart Instance "+cli.getControllerHost()+":"+cli.getControllerPort()
			 return 1
		 }
		 println "[S] Restart of Instance "+cli.getControllerHost()+":"+cli.getControllerPort()
		 return 0
		 

		 
	 }
	 
	 
	 static def int astartInstance(def Jboss7Shell cli,def String exec_path,def List exec_args, def String exec_workdir, def List exec_env) {
		 
		  
		  
		  println("[I] Async Start Standalone Server "+cli.getControllerHost()+":"+cli.getControllerPort())
		  
		  def status=getStatusInstance(cli)
		  switch (status){
			  case "STARTING":
			  case "RESTART_REQUIRED":
			  case "RUNNING":println "[E] Instance already started or starting"; return 1; break;
			  case "UNKNOWN": println "[E] Current status of Instance is $status "; return 1;
				  break;
			  
			  case "STOPPED": break;
 
		  }
		  
		  
		  // format la start command line
		  
		  //execute_background_cmdline
		  def List<String> cmdArgs =[]
		 cmdArgs.add( exec_path)
		 cmdArgs.addAll(exec_args)
		 
		 
		 // execute_background command
		 // static def int execute_background_cmdline(def List envp=[],def String workdir,def List<String> command)
		 println "[D] "+ cmdArgs
		 
		 def File exec_workdirFile=null
		 if(exec_workdir!=""){ exec_workdirFile=new File(exec_workdir)}
		 def cr=Jboss7CommonTools.execute_background_cmdline(exec_env,exec_workdirFile,cmdArgs)
		  if(cr!=0){
			  print "[E] Impossible to start of Instance"
			  return 1
		  }
		  else{
			  println "[S] Async start of Instance [OK]"
			  return 0
		  }
		  
		  

					   
		 
	 }
	  
	  static def int astopInstance(def Jboss7Shell cli){
		  println("[I] Async Stop Standalone Server"+cli.getControllerHost()+":"+cli.getControllerPort())
		  def status=getStatusInstance(cli)
		  switch (status){
			  case "STARTING":
			  case "RESTART_REQUIRED":
			  case "RUNNING":break;
			  case "UNKNOWN": println "[E] Current status of Instance is $status "; return 1;
				  break;
			  
			  case "STOPPED":println "[E] Instance already stopped"; return 1; break;
 
		  }
		  
		  
		  // Print PID of instance
		  println "Instance PID:"+getPidInstance(cli)
		  // shutdown   the instance
		  def commandline="/:shutdown"
		  try {
			  
			  def result=cli.runCLICommand(commandline)
			  if (result.isSuccess()){
				  println("[I] a Shutdown request has been send to server")
			  }
			  else{
				  println("[E] a Shutdown request can't be send to server succesfully. Abort...")
				  return 1
			  }
		  }
		  catch (Exception e) {
			  println("[E]  Impossible to send a Shutdown Request to then Standalone Server. Command $commandline Fialed ...Abort...")
			  return 1
		  }
		  
		  println "[S] Async stop of Instance [OK]"
		  cli._disconnect()
		  return 0
			  
		  
		  
	  }
	  
	  
	  static def int areloadInstance(def Jboss7Shell cli){
		  println("[I] Async Reload Standalone Server"+cli.getControllerHost()+":"+cli.getControllerPort())
		  // test si l'instance est bien demarrée
		  if(!cli.isControllerListen()){
			  println "[E] Instance "+cli.getControllerHost()+":"+cli.getControllerPort() +"  seem to be down"
			  return 1
		  }
		  
		  
		  // connection to Controller
		  if(!cli.connect()){
			  println "[E] Impossible to connect to Instance "+cli.getControllerHost()+":"+cli.getControllerPort()
			  return 1
		  }
		  
		  
		  // reload   the instance
		  def commandline="/:reload"
		  try {
			  def result=cli.runCLICommand(commandline)
			  if (result.isSuccess()){
				  println("[I] a reload request has been send to server")
			  }
			  else{
				  println("[E] a reload request can't be send to server succesfully. Abort...")
				  return 1
			  }
		  }
		  catch (Exception e) {
			  println("[E]  Impossible to send a reload Request to then Standalone Server. Command $commandline Fialed ...Abort...")
			  return 1
		  }
		  
		  println "[S] Async reload of Instance [OK]"
		  cli._disconnect()
		  return 0
		  
		  
		  
	  }
	  
	  static def arestartInstance(def Jboss7Shell cli,def String exec_path,def List exec_args, def String exec_workdir, def List exec_env, def long timeout_sec) {
		  println("[I] Restart Standalone Server"+cli.getControllerHost()+":"+cli.getControllerPort())
		  
		  
		  
		  def status=getStatusInstance(cli)
		  switch (status){
			  case "STARTING":
			  case "RESTART_REQUIRED":
			  case "RUNNING":
			  case "UNKNOWN":
				  if(stopInstance(cli,timeout_sec)!=0){
					  println "[E] Impossible to restart Instance "+cli.getControllerHost()+":"+cli.getControllerPort()
					  return 1
				  }
				  break;
			  
			  case "STOPPED":break;
			  
		  
		  }
		  
		  if(astartInstance( cli,exec_path,exec_args,exec_workdir, exec_env)!= 0 ){
			  println "[E] Impossible to restart Instance "+cli.getControllerHost()+":"+cli.getControllerPort()
			  return 1
		  }
		  println "[S] Async Restart of Instance "+cli.getControllerHost()+":"+cli.getControllerPort()+ " [OK]"
		  return 0
		  
 
		  
	  }
	 
	  static public String getStatusInstance(def Jboss7Shell cli){
		  /********
		   * Liste des status possibles pour une instance Jboss7
		  STARTING,
		  RUNNING
		  RESTART_REQUIRED,
		  STOPPED
		  UNKNOWN,
		  
		   */
		  
		  
		   
		   
		   // connection to Controller
		  if(!cli.isControllerListen()){
			  return "STOPPED"
		  }
		  else{
			  if(!cli.isConnected()){
				   if(!cli._connect()){
					   if(!cli.isControllerListen()){
						   return "STOPPED"
					   }
					   else{
						   return "UNKNOWN"
					   }
				   }
			  }
		  }
		  
		  def commandline="/:read-attribute(name=server-state)"
		  def result=cli.runCLICommand(commandline)
		  if (result.isSuccess()){
			  return result.getResponse().get("result").asString().toUpperCase()
		  }
		  else{
			  return "UNKNOWN"
		  }
	  }
	   static int waitForstatusOfInstance(def Jboss7Shell cli, def String statusToWait, def long timeout){
		  /********
		   * Liste des status possibles pour une instance Jboss7
		  STARTING,
		  RUNNING
		  RESTART_REQUIRED,
		  STOPPED
		  UNKNOWN,
		  
		   */
		  
		  def cr=0
		  
		  switch(statusToWait){
			  case "STARTING":
			  case "RUNNING":
			  case "RESTART_REQUIRED":
			  case "STOPPED":
			  case "UNKNOWN":
			  
				  break;
			  default:
				  println("[E] Invalid status : $statusToWait ");
				  return 1;
				  break;
		  }
		  
		  
		  
		  // by default wait for terminate state or status=statusToWait
		  
		  def status=getStatusInstance(cli)
		  if (status==null){status="UNKNOWN"}
		  
		  if(status==statusToWait){
			  println "";println "[S] Instance is already  $status."
			  return 0
		  }
		  
		  def laststatus=status
		  
		  println "[I] Instance is currently  $status."
		  print "[I] Wait for status $statusToWait:."
		  def startTime = System.currentTimeMillis()
		  while(laststatus==status || status== "STARTING"  ||  status== "UNKNOWN"   ){
			  // wait 5 secondes
			  sleep(5000)
			  status=getStatusInstance(cli)
			  if (status==null){status="UNKNOWN"}
			  laststatus=status
			  
			  
			  if(status==statusToWait){
				  println "";println "[S] Instance is now  $status."
				  return 0
			  }
			  // println System.currentTimeMillis() - startTime+"/"+timeout
			  if( timeout != 0 && System.currentTimeMillis() - startTime >= timeout){
				  println "";
				  println("[E] !!! TIMEOUT !!!")
				  println "[E] Instance  is currently  $status. WaitForStatus aborting..."
				  return 1
			  }
			  
			  
		  }
		  
		  if(status==statusToWait){
			  println "";println "[S] Instance is now  $status."
			  return 0
		  }
		  else{
			  println "[E] Instance is currently  $status."
			  return 1
		  }
		  
  
	  }
	 
	   
}
