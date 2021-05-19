package fr.visionitgroup.sra.plugin.jboss7.helper;


import groovy.lang.Closure;

import org.jboss.as.cli.scriptsupport.*

public class Jboss7CLIConnection {
	
	def  String controllerHost="127.0.0.1"
	def int controllerPort=9999
	def String userMgmt=null
	def char[] passwordMgmt=null
	
	def CLI cli=null
	
	public Jboss7CLIConnection(def String userMgmt,def char[] passwordMgmt,def String controllerHost, def int controllerPort=9999) {
		this.controllerHost=controllerHost
		this.controllerPort=controllerPort
		this.userMgmt=userMgmt
		this.passwordMgmt=passwordMgmt
	}

	public Jboss7CLIConnection(def String userMgmt,def char[] passwordMgmt) {
		this.userMgmt=userMgmt
		this.passwordMgmt=passwordMgmt
	}
	
	public Jboss7CLIConnection() {
		
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
	public void connect(){
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
		}
		catch (e){
			throw new Exception("[E] Impossible to connect on Jboss controller :controllerHost=$controllerHost,controllerPort=$controllerPort,userMgmt=$userMgmt,passwordMgmt=**** ")
		}
		
		println "[D] Connected on Jboss controller :controllerHost=$controllerHost,controllerPort=$controllerPort,userMgmt=$userMgmt,passwordMgmt=**** "
	}
	public void disconnect(){
		this.cli.disconnect(  )
	}
	public void waitForConnect(def timeout){
		def startTime = System.currentTimeMillis()
		def gotConnection = false
		if (this.cli != null){
			disconnect()
		}
		while (!gotConnection  )   {
			
			try {
				
				connect()
				gotConnection=true
			}
			catch (Exception e) {
				System.out.println("[W] Wait for Connection! (Retry in 5 sec...)")
				sleep(5000)
			}
			println System.currentTimeMillis() - startTime +"/"+ timeout
			if( timeout != 0 && System.currentTimeMillis() - startTime >= timeout){
				break;
			}
			
		}
		if (!gotConnection){
			throw new Exception("[E] Timeout ($timeout millisec) : Impossible to connect on Jboss controller :controllerHost=$controllerHost,controllerPort=$controllerPort,userMgmt=$userMgmt,passwordMgmt=**** ")
		}
		
	}
	
	def Boolean isDomainMode(){
		return cli.getCommandContext().isDomainMode()
		
	}
	
	def public CLI.Result runCLICommand(def command) {
		
		return this.cli.cmd(command)
	}
	
	def public int runCLIScript(def commandLines,def keepgoing=0) {
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
	
	
}
	

