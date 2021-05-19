package fr.visionitgroup.groovylib.jboss7
import java.security.SecureRandom;



import groovy.lang.Closure;

import org.jboss.as.cli.scriptsupport.*

public class Jboss7Shell {
	
	def  String controllerHost="127.0.0.1"
	def int controllerPort=9999
	def String userMgmt=null
	def char[] passwordMgmt=null
	
	def CLI cli=null
	
	public Jboss7Shell(def String userMgmt,def char[] passwordMgmt,def String controllerHost, def int controllerPort=9999) {
		this.controllerHost=controllerHost
		this.controllerPort=controllerPort
		this.userMgmt=userMgmt
		this.passwordMgmt=passwordMgmt
	}

	public Jboss7Shell(def String userMgmt,def char[] passwordMgmt) {
		this.userMgmt=userMgmt
		this.passwordMgmt=passwordMgmt
	}
	
	public Jboss7Shell() {
		
	}
	
	String getControllerHost(){
		return controllerHost
	}
	int getControllerPort(){
		return controllerPort
	}
	String getPasswordMgmt(){
		return passwordMgmt
	}
	String getUserMgmt(){
		return userMgmt
	}
	public Boolean testConnexion(){
		if(!isConnected()){
			println "[I] Test Connexion: KO"
			return false
			
		}
		else{
			println "[I] Test Connexion: OK"
			return true
		}
		
		
	}
	 def Boolean isControllerListen(){
		def Boolean gotConnection=false
		def s = new Socket()
		
		try {
			
			def addr = new InetSocketAddress(this.getControllerHost(), this.getControllerPort())
			s.connect(addr)
			gotConnection=true
		}
		catch (IOException e) {
			gotConnection=false
			
		}
		
		if (s != null) {
				s.close()
		}
		
		return gotConnection
		
	}
	 
	def int waitForControllerListen(def long timeout_sec){
		println "[I] waitFor a valid ping of Jboss controller :controllerHost=$controllerHost,controllerPort=$controllerPort,userMgmt=$userMgmt,passwordMgmt=**** "
		
		if(timeout_sec <=0){
			System.out.print("[W] Wait for Connection! (Retry in each 5 sec...):")
			while (!isControllerListen()  )   {
				
				System.out.print "."
				sleep(5000)
				
					
			}
			println "[S] Ping of Jboss controller [OK]" 
			return 0
			
		}
		else{
			def long timeout=timeout_sec*1000 // convert in millisec		
			System.out.print("[W] Wait for Connection! (Retry in each 5 sec...):")
			def startTime = System.currentTimeMillis()
			while (!isControllerListen()  )   {
				
				System.out.print "."
				sleep(5000)
					
					
				
				//println "[D]"+System.currentTimeMillis() - startTime +"/"+ timeout
				if(  System.currentTimeMillis() - startTime >= timeout){
					break;
				}
				
			}
			System.out.println ""
			if (!isControllerListen()){
				println ("[E] Timeout ($timeout millisec) : Ping of Jboss controller [KO] ")
				return 1
			}
			else{
				println "[S] Ping of Jboss controller [OK]"
				return 0
			}
		}
	}
	
	def int pingController(){
		if(this.isControllerListen()){
			println "[I] Ping Controller"+this.getControllerHost()+":"+this.getControllerPort() +": OK"
			return 0
		}
		else{
			println "[E] Ping Controller"+this.getControllerHost()+":"+this.getControllerPort() +": KO"
			return 1
		}
	}
	public Boolean isConnected(){
		if(this.cli==null){
				return false
			
		}
		
		try{
			def commandline="cd /"
			def result=runCLICommand(commandline)
			if (result.isSuccess()){
				return true
			}
			else{
				
				return false
			}
		}
		catch(Exception e){
			return false
		}
		
	}
	public Boolean connect(){
		if(! isConnected()){
			if(!_connect()){
				println ("[E] Impossible to connect on Jboss controller :controllerHost=$controllerHost,controllerPort=$controllerPort,userMgmt=$userMgmt,passwordMgmt=**** ")
				this.cli=null
				return false
			}
			else{
				println "[I] Connected on Jboss controller :controllerHost=$controllerHost,controllerPort=$controllerPort,userMgmt=$userMgmt,passwordMgmt=**** "
				return true
			}
		
		}
		else{
			// already connected
			println "[W] already connected"
			return true
		}
	}
	private Boolean _connect(){
		if(! isConnected()){
			PrintStream nps = new PrintStream(new FileOutputStream("NUL:"));
			PrintStream sav=System.err
			System.err.flush()
			System.setErr(nps);
			this.cli = CLI.newInstance()
			try{
				if (userMgmt!= ""){
					if (controllerHost!= null){
						this.cli.connect( controllerHost,  controllerPort,  userMgmt,  passwordMgmt)
					}
					else{
						this.cli.connect(   userMgmt,  passwordMgmt)
					}
				}
				else{
					this.cli.connect(  )
				}
				System.setErr(sav);
				return true
			}
			catch (Exception e){
				System.setErr(sav);
				return false
			}
			System.setErr(sav);
			this.cli=null
			return false
			
		}
		else{
			// already connected
			return true
		}
	}
	
	private Boolean _disconnect(){
		if(isConnected()){
			PrintStream nps = new PrintStream(new FileOutputStream("NUL:"));
			PrintStream sav=System.err
			System.err.flush()
			try{
				this.cli.disconnect()
				this.cli=null
				System.setErr(sav);
				return true
			}
			catch(Exception e){
				System.setErr(sav);
				return false
			}
		}
		else{
			// already connected
			this.cli=null
			return true
		}
		
	}
	public Boolean disconnect(){
		if(isConnected()){
			if(!_disconnect()){
				println ("[E] Impossible to disconnect from Jboss controller :controllerHost=$controllerHost,controllerPort=$controllerPort,userMgmt=$userMgmt,passwordMgmt=**** ")
				println e.getMessage()
				this.cli=null
				return false
			}
			else{
				println ("[I] Disconnect from Jboss controller :controllerHost=$controllerHost,controllerPort=$controllerPort,userMgmt=$userMgmt,passwordMgmt=**** ")
				return true
			}
		}
		else{
			// already connected
			println "[W] Already disconnected"
			this.cli=null
			return true
		}
		
	}
	public Boolean waitForReconnect(def long timeout_sec=0){
		
		if(!_disconnect()){
			println ("[E] Impossible to disconnect from Jboss controller :controllerHost=$controllerHost,controllerPort=$controllerPort,userMgmt=$userMgmt,passwordMgmt=**** ")
			return false
		}
		println "[I] try to re-connect to Jboss controller :controllerHost=$controllerHost,controllerPort=$controllerPort,userMgmt=$userMgmt,passwordMgmt=**** "
		
		if(timeout_sec <=0){
			System.out.print("[W] Wait for Connection! (Retry in each 5 sec...):")
			while (!isConnected()  )   {
				
				try {
					
					if(! _connect()){
						System.out.print "."
						sleep(5000)
					}
					
				}
				catch (Exception e) {
					System.out.print "."
					sleep(5000)
				}
				
				
			}
			System.out.println ""
			
			println ("[I] Re-connect on Jboss controller :controllerHost=$controllerHost,controllerPort=$controllerPort,userMgmt=$userMgmt,passwordMgmt=**** ")
			return true

		}
		else{
			def long timeout=timeout_sec*1000 // convert in millisec
			
			
			System.out.print("[W] Wait for Connection! (Retry in each 5 sec...):")
			def startTime = System.currentTimeMillis()
			while (!isConnected()  )   {
				
				try {
					
					if(! _connect()){
						System.out.print "."
						sleep(5000)
					}
					
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
			if (!isConnected()){
				println ("[E] Timeout ($timeout millisec) : Impossible to connect on Jboss controller :controllerHost=$controllerHost,controllerPort=$controllerPort,userMgmt=$userMgmt,passwordMgmt=**** ")
				return false
			}
			else{
				println ("[I] Re-connect on Jboss controller :controllerHost=$controllerHost,controllerPort=$controllerPort,userMgmt=$userMgmt,passwordMgmt=**** ")
				return true
			}
		}
		
	}
	
	def Boolean isDomainMode(){
		return cli.getCommandContext().isDomainMode()
		
	}
	
	def public CLI.Result runCLICommand(def command) {
		
		
			return this.cli.cmd(command)
			
		
		
	}
	def public int runCLIScripFromFile(def String filePath,def keepgoing=0){
		if(!new File(filePath).isFile()){
			println "[E] Impossible to access to $filePath"
			return 1
		}
		else{
			try{
				def String fileContent=new File(filePath).text
				return runCLIScript(fileContent,keepgoing)
			}
			catch(Exception e){
				
				return 1
			}
			return 1
		}
	}
	def public int runCLIScript(def String commandLines,def keepgoing=0) {
		def cr=0
		try{
			
			commandLines=commandLines.replaceAll(/\\\r\n/,"\\\n").replaceAll(/\\\n/,"")
			
			commandLines.eachLine { line ->
				if(line ==~ /^ *$/ || line ==~ /^ *#.*$/){	
					println("[C]:$line")
				}
				else{
					
					//println("line=$line")
					def result = cli.cmd(line)
					    
					if (result.isSuccess()){
						println("[S]:$line")
					}
					else{
						def response = result.getResponse()
						println("[E]:$line:"+response.asString())
						cr=1
						if (keepgoing==0){ 
							throw new Exception("return from closure")
						}
					}
				}
			}
		}
		catch (e){
			cr=1
		}
		finally{
			println("Exit with cr=$cr")
			return cr
		}
	}
	
	def private String randowString(){
		  return new BigInteger(130, new SecureRandom()).toString(32)
	}
	def public int runBacthCLIScriptFromFile(def Boolean  simulate, def String filePath,def String workdirPath=""){
		if(!new File(filePath).isFile()){
			println "[E] Impossible to access to $filePath"
			return 1
		}
		else{
			try{
				def String fileContent=new File(filePath).text
				return runBacthCLIScript(simulate,fileContent,workdirPath)
			}
			catch(Exception e){
				
				return 1
			}
			return 1
		}
	}
	def public int runBacthCLIScript(def Boolean  simulate, def String commandLines,def String workdirPath="") {
		
		def String random="AAA"+randowString()
		def String wrongQuery="/system-property=${random}:read-attribute(name=${random})"
		def File tempFile
		try{
			if (workdirPath!=""){
				tempFile=File.createTempFile( "WsadminShell", ".py", new File (workdirPath))
			}
			else{
				tempFile=File.createTempFile( "WsadminShell", ".py")
			}
		}
		catch(Exception e){
			println "[E] Impossible to create a temporary file:"+e.getMessage()
			return 1
		}
		
		try{
			def tempFileName=tempFile.getAbsolutePath()
			
			
			def cr=0
			try{
				// convert dos2unix
				commandLines=commandLines.replaceAll(/\\\r\n/,"\\\n").replaceAll(/\\\n/,"")
				
				// load line in jboss cli shell
				def commandLinesNew=""
				commandLines.eachLine { line ->
					if(!(line ==~ /^ *$/ || line ==~ /^ *#.*$/)){
						commandLinesNew+=line+"\n"
					}
				}
				
				
				
				tempFile<<commandLinesNew
				
				if(simulate){
					// add at the end of script an invalid jboss-cli query
					tempFile<<wrongQuery
					
				}
				
				//println "[D] TempFileContent=\n"+tempFile.text
				//println "[D] cli.cmd="+"batch --file="+tempFile.getAbsolutePath()
				
				
				def result = cli.cmd("run-batch --file="+tempFile.getAbsolutePath())
				if (result.isSuccess()){
					
					
					println("[S]:Execution script with batch mode : OK")
				}
				else{
					
					if(simulate){
						// simulate mode
						// Check if fail due to wrong_query or before this query
						def response=result.getResponse()
						def failureDescriptionMessage=response.get("failure-description").asString()
						//println "failureDescriptionMessage=$failureDescriptionMessage"
						if(failureDescriptionMessage =~ /${random}/ ){
							println("[S]:Simulation of script with batch mode : OK")
							cr=0
						}
						else{
							println("[E]:Simulation of script with batch mode :  failed\n"+result.getResponse())
							cr=1
						}
					}
					else{
						// execute mode
					
						println("[E]:Execution script with batch mode: failed\n"+result.getResponse())
						cr=1
					}
				}
				
			}
			catch (e){
				
				println("[E]:Execution script with batch mode: failed due to :\n"+e.getMessage())
				cr=1
			}
			finally{
				tempFile.delete()
				println("Exit with cr=$cr")
				return cr
			}
		}
		catch(Exception e){
			tempFile.delete()
			println("runBacthCLIScript technical error to due :\n"+e.getMessage()+"\nExit with cr=1")
			return 1
		}
		tempFile.delete()
		return 1
	}// end function
	
}
	

