package fr.visionitgroup.sra.plugin.jboss7.helper;


import groovy.lang.Closure;

import org.jboss.as.cli.scriptsupport.*

public class RemoteInstanceDirectory {
	
	public def String instName=null
	public def String remoteDir=null
	
	public def String instConnectorProtocole="local"
	public def String instConnectorSSLUser=null 
	public def String instConnectorSSLPassword=null 
	public def String instConnectorSSLHost=null 
	public def Integer instConnectorSSLPort=22
	public def Boolean instConnectorSSLTrust=false
	public def String instConnectorSSLKnownhosts=null 
	public def String instConnectorSSLKeyfile=null 
	public def String instConnectorSSLPassphrase="" 
	public def Boolean instConnectorSSLVerbose=false
	public def Integer instConnectorSSLTimeout=0
	
	
	public String getInstName(){return this.instName}
	public String getRemoteDir(){return this.remoteDir}
	public String getInstConnectorProtocole(){return this.instConnectorProtocole}
	public String getInstConnectorSSLUser(){return this.instConnectorSSLUser}
	public String getInstConnectorSSLPassword(){return this.instConnectorSSLPassword}
	public String getInstConnectorSSLHost(){return this.instConnectorSSLHost}
	public Integer getInstConnectorSSLPort(){return this.instConnectorSSLPort}
	public Boolean getInstConnectorSSLTrust(){return this.instConnectorSSLTrust}
	public String getInstConnectorSSLKnownhosts(){return this.instConnectorSSLKnownhosts}
	public String getInstConnectorSSLKeyfile(){return this.instConnectorSSLKeyfile}
	public String getInstConnectorSSLPassphrase(){return this.instConnectorSSLPassphrase}
	public Boolean getInstConnectorSSLVerbose(){return this.instConnectorSSLVerbose}
	public Integer getInstConnectorSSLTimeout(){return this.instConnectorSSLTimeout}
	
	public void setInstName(def instName) {this.instName=instName}
	public void setRemoteDir(def String remoteDir) {this.remoteDir=remoteDir.replace("\\","/");}
	public void setInstConnectorProtocole(def instConnectorProtocole) {this.instConnectorProtocole=instConnectorProtocole}
	public void setInstConnectorSSLUser(def instConnectorSSLUser) {this.instConnectorSSLUser=instConnectorSSLUser}
	public void setInstConnectorSSLPassword(def instConnectorSSLPassword) {this.instConnectorSSLPassword=instConnectorSSLPassword}
	public void setInstConnectorSSLHost(def instConnectorSSLHost) {this.instConnectorSSLHost=instConnectorSSLHost}
	public void setInstConnectorSSLPort(def instConnectorSSLPort) {this.instConnectorSSLPort=instConnectorSSLPort}
	public void setInstConnectorSSLTrust(def instConnectorSSLTrust) {this.instConnectorSSLTrust=instConnectorSSLTrust}
	public void setInstConnectorSSLKnownhosts(def String instConnectorSSLKnownhosts) {this.instConnectorSSLKnownhosts=instConnectorSSLKnownhosts.replace("\\","/");}
	public void setInstConnectorSSLKeyfile(def String instConnectorSSLKeyfile) {this.instConnectorSSLKeyfile=instConnectorSSLKeyfile.replace("\\","/");}
	public void setInstConnectorSSLPassphrase(def instConnectorSSLPassphrase) {this.instConnectorSSLPassphrase=instConnectorSSLPassphrase}
	public void setInstConnectorSSLVerbose(def instConnectorSSLVerbose) {this.instConnectorSSLVerbose=instConnectorSSLVerbose}
	public void setInstConnectorSSLTimeout(def instConnectorSSLTimeout) {this.instConnectorSSLVerbose=instConnectorSSLTimeout}
	
	public RemoteInstanceDirectory(def instName){
		this.instName=instName
	}
	public RemoteInstanceDirectory(def instName,def String remoteDir,def instConnectorProtocole){
		this.instName=instName
		this.remoteDir=remoteDir.replace("\\","/");
		this.instConnectorProtocole=instConnectorProtocole
		if (this.instConnectorProtocole != "local" && this.instConnectorProtocole != "ssl"){
			throw new Exception("[E] Impossible to create a RemoteInstanceDirectory : managed protocole are 'local' or 'ssl' only  ")
		}
		
		
	}
	
	private String getRemoteDirFullStr(){
		def String remoteDirFullStr
		if (instConnectorProtocole=="local"){
				return instConnectorProtocole+"//"+remoteDir
		}
		else{
			return instConnectorProtocole+"//"+instConnectorSSLUser+"@"+instConnectorSSLHost+"("+instConnectorSSLPort+")"+":"+remoteDir
		}
	}
	public Integer syncFromLocalDirectoryToRemote(def String localSrcDir,def syncMode="deleteAndInsert"){
		println "[I] syncFromLocalDirectoryToRemote $instName : process starting "
		println "[I] syncFromLocalDirectoryToRemote $instName :    source=$localSrcDir "
		println "[I] syncFromLocalDirectoryToRemote $instName :         ===>>  destination="+getRemoteDirFullStr()
		if(syncMode!="deleteAndInsert" && syncMode!="insertOrUpdate"){
			println "[E] syncFromLocalDirectoryToRemote $instName :Invalid value for syncMode ($syncMode): it must be deleteAndInsert or insertOrUpdate only. Abort..."
			return 1
		}
		if (this.instConnectorProtocole == "local" ){
			// Control existence local directory
			def File dirLocal=new File(localSrcDir)
			if(! dirLocal.isDirectory()){
				println "[E] syncFromLocalDirectoryToRemote $instName : Impossible to control acces to local directory  $localSrcDir. Abort..."
				return 1
			}
			// Control existence remote directory
			def cr=control()
			if (cr!=0){
				println "[E] syncFromLocalDirectoryToRemote $instName : Impossible to control acces to RemoteInstanceDirectory $instName. Abort..."
				return 1
			}
			
			if(syncMode=="deleteAndInsert"){
				try{
				// Delete Content (file and directory inside
					def File d=new File(remoteDir)
					d.eachFile {
						def name=it.absolutePath
						if(it.isDirectory()){
							println "[D] delete directory local:$it"
							
							if(!it.deleteDir()){
								println "[E] syncFromLocalDirectoryToRemote $instName : Impossible to delete directory local:$name. Abort...\n"
								throw new Exception("return from closure")
							}
						}
						else{
							println "[D] delete file local:$it"
							if(!it.delete()){
								println "[E] syncFromLocalDirectoryToRemote $instName : Impossible to delete file local:$name. Abort...\n"
								cr=1
								throw new Exception("return from closure")
							}
						}
					}
					
				}
				catch(Exception e){
					println (e.getMessage())
					return 1
				}
			} 
			
			// Copy src to Dest directory
			try{
				def ant = new AntBuilder()
				ant.copy(verbose: "true", todir: remoteDir, overwrite: "true") {
					fileset(dir: localSrcDir) {
						include(name:"**/*")
					}
				}
			}
	
			catch(Exception e){
				println "[E] syncFromLocalDirectoryToRemote $instName : Impossible to copy $localSrcDir to local:$remoteDir. Abort...\n"+e.getMessage()
				return 1
			}
			
		}
		
		// SSL : UNIX case or windows over cygwin
		if ( this.instConnectorProtocole == "ssl"){
			// Control existence local directory
			def File dirLocal=new File(localSrcDir)
			if(! dirLocal.isDirectory()){
				println "[E] syncFromLocalDirectoryToRemote $instName : Impossible to control acces to local directory  $localSrcDir. Abort..."
				return 1
			}
			// Control existence remote directory
			def cr=control()
			if (cr!=0){
				println "[E] syncFromLocalDirectoryToRemote $instName : Impossible to control acces to RemoteInstanceDirectory $instName. Abort..."
				return 1
			}
			
			if(syncMode=="deleteAndInsert"){
				
					// Delete Content (file and directory inside
					def cmdline='if [  -d "'+remoteDir+'" ]; then rm -rf "'+remoteDir+'"/*; cr2=$?;echo cr=$cr2;exit $cr2; fi; '
					def ssh_cr=0
					def ant = new AntBuilder()
					try{
						
					
						ant.sshexec(
							command:cmdline,
							
							host:instConnectorSSLHost,
							port:instConnectorSSLPort,
							username:instConnectorSSLUser,
							keyfile:instConnectorSSLKeyfile,
							passphrase:instConnectorSSLPassphrase,
							trust:instConnectorSSLTrust,
							knownhosts:instConnectorSSLKnownhosts,
			
							timeout:instConnectorSSLTimeout,
							
							verbose:instConnectorSSLVerbose,
							failonerror:true,
							suppresssystemout:false,
							usepty:false,
							outputproperty: 'result',
							
							)
					}
					catch (Exception e){
						ssh_cr=1
					
					}
					finally{
						
							def result=ant.project.properties.'result'.toString();
							def String[] lines=result.split("\n")
							def lastline=lines[lines.size()-1]
							if (lastline =~ "cr=0" && ssh_cr == 0){
								println "[I]  RemoteInstanceDirectory $instName : content of  directory $instConnectorSSLUser@$instConnectorSSLHost($instConnectorSSLPort):$remoteDir has been deleted"
							}
							else{
								if (lastline =~ /^cr=/ && ssh_cr != 0){
									println "[E] RemoteInstanceDirectory $instName : content of  directory $instConnectorSSLUser@$instConnectorSSLHost($instConnectorSSLPort):$remoteDir can't be deleted"
									return 1
								}
								else{
									println "[E] syncFromLocalDirectoryToRemote $instName :Impossible to delete content of directory $instConnectorSSLUser@$instConnectorSSLHost($instConnectorSSLPort):$remoteDir : SSH connexion failed:"+result
									return 1
								}
							}					
					}
				
			}
		
			// Copy src to Dest directory
			try{
				def ant = new AntBuilder()
				def String todirStr=""
				
				ant.scp(
					
					todir:"$instConnectorSSLUser@$instConnectorSSLHost:$remoteDir",
					password:instConnectorSSLPassword,
					port:instConnectorSSLPort,
					
					keyfile:instConnectorSSLKeyfile,
					passphrase:instConnectorSSLPassphrase,
					trust:instConnectorSSLTrust,
					knownhosts:instConnectorSSLKnownhosts,
						
					verbose:instConnectorSSLVerbose,
					failonerror:true,
					
					
					
					){
						fileset(dir: localSrcDir) {
							include(name:"**/*")
						}
					}
				
				
			}
			catch(Exception e){
				println "[E] syncFromLocalDirectoryToRemote $instName : Impossible to copy $localSrcDir to local:$remoteDir. Abort...\n"+e.getMessage()
				return 1
			}
		}
		println "[I] syncFromLocalDirectoryToRemote $instName : process ending succesfully"
		return 0
		
	}
	public Integer syncFromRemoteToLocalDirectory(def String localSrcDir){
		println "[I] syncFromLocalDirectoryToRemote $instName : process starting "
		println "[I] syncFromLocalDirectoryToRemote $instName :    source="+getRemoteDirFullStr()
		println "[I] syncFromLocalDirectoryToRemote $instName :         ===>>  destination=$localSrcDir"
		
		if (this.instConnectorProtocole == "local" ){
			// Control existence local directory
			def File dirLocal=new File(localSrcDir)
			if(! dirLocal.isDirectory()){
				println "[E] syncFromRemoteToLocalDirectory $instName : Impossible to control acces to local directory  $localSrcDir. Abort..."
				return 1
			}
			// Control existence remote directory
			def cr=control()
			if (cr!=0){
				println "[E] syncFromRemoteToLocalDirectory $instName : Impossible to control acces to RemoteInstanceDirectory $instName. Abort..."
				return 1
			}
			
			
			
			// Copy src to Dest directory
			try{
				def ant = new AntBuilder()
				ant.copy(verbose: "true", todir: localSrcDir, overwrite: "true") {
					fileset(dir: remoteDir ) {
						include(name:"**/*")
					}
				}
			}
	
			catch(Exception e){
				println "[E] syncFromRemoteToLocalDirectory $instName : Impossible to copy $localSrcDir to local:$remoteDir. Abort...\n"+e.getMessage()
				return 1
			}
		}
		if ( this.instConnectorProtocole == "ssl"){
			// Control existence local directory
			def File dirLocal=new File(localSrcDir)
			if(! dirLocal.isDirectory()){
				println "[E] syncFromRemoteToLocalDirectory $instName : Impossible to control acces to local directory  $localSrcDir. Abort..."
				return 1
			}
			// Control existence remote directory
			def cr=control()
			if (cr!=0){
				println "[E] syncFromRemoteToLocalDirectory $instName : Impossible to control acces to RemoteInstanceDirectory $instName. Abort..."
				return 1
			}
			
			
		
			// Copy src to Dest directory
			try{
				def ant = new AntBuilder()
				def String todirStr=""
				
				ant.scp(
					
					todir: localSrcDir ,
					password:instConnectorSSLPassword,
					port:instConnectorSSLPort,
					
					keyfile:instConnectorSSLKeyfile,
					passphrase:instConnectorSSLPassphrase,
					trust:instConnectorSSLTrust,
					knownhosts:instConnectorSSLKnownhosts,
						
					verbose:instConnectorSSLVerbose,
					failonerror:true,
					
					
					
					){
						fileset(dir: "$instConnectorSSLUser@$instConnectorSSLHost:$remoteDir") {
							include(name:"**/*")
						}
					}
				
				
			}
			catch(Exception e){
				println "[E] syncFromRemoteToLocalDirectory $instName : Impossible to copy $localSrcDir to local:$remoteDir. Abort...\n"+e.getMessage()
				return 1
			}
		}
		println "[I] syncFromRemoteToLocalDirectory $instName : process ending succesfully"
		return 0
	}
	public void setSSLConxionByPasswordAuth(def instConnectorSSLHost,def instConnectorSSLUser, def instConnectorSSLPassword, def instConnectorSSLTrust=false, def String instConnectorSSLKnownhosts ){
		this.instConnectorProtocole="ssl"
		this.instConnectorSSLHost=instConnectorSSLHost
		this.instConnectorSSLUser=instConnectorSSLUser
		this.instConnectorSSLPassword=instConnectorSSLPassword
		this.instConnectorSSLTrust=instConnectorSSLTrust
		this.instConnectorSSLKnownhosts=instConnectorSSLKnownhosts.replace("\\","/");
		
	}
	public void setSSLConxionBykeyAuth(def instConnectorSSLHost,def instConnectorSSLUser, def String instConnectorSSLKeyfile, def instConnectorSSLPassphrase=null , def instConnectorSSLTrust=false, def String  instConnectorSSLKnownhosts ){
		this.instConnectorProtocole="ssl"
		this.instConnectorSSLHost=instConnectorSSLHost
		this.instConnectorSSLUser=instConnectorSSLUser
		this.instConnectorSSLKeyfile=instConnectorSSLKeyfile.replace("\\","/");
		this.instConnectorSSLPassphrase=instConnectorSSLPassphrase
		this.instConnectorSSLTrust=instConnectorSSLTrust
		this.instConnectorSSLKnownhosts=instConnectorSSLKnownhosts.replace("\\","/");
	
	}
	
	
	private String formatSSHEXECCommandLine(){
		
	}
	
	public Integer control(){
		def int cr=0

		if (this.instName == null || (this.instName != null && this.instName == "")) { println "[E] RemoteInstanceDirectory  : instName attribute is mandatory and can't be empty"; cr=1}
		if (this.remoteDir == null || (this.remoteDir != null && this.remoteDir == "")) { println "[E] RemoteInstanceDirectory  : remoteDir attribute is mandatory and can't be empty"; cr=1}
		if (this.instConnectorProtocole == null || (this.instConnectorProtocole != null && this.instConnectorProtocole == "local" && this.instConnectorProtocole == "ssl")) { println "[E] RemoteInstanceDirectory : protocle attribute is mandatory and must be equal to 'local' or 'ssl'"; cr=1}
		
		
		if (this.instConnectorProtocole == "local" ){
			def File d=new File(remoteDir)
			if (d.isDirectory()){
				println "[I]  RemoteInstanceDirectory $instName : directory $remoteDir exist"
			}
			else{
				println "[E]  RemoteInstanceDirectory $instName : directory $remoteDir doesn't exist"
				cr=1
			}
		}
		
		if ( this.instConnectorProtocole == "ssl"){
			def ant = new AntBuilder()
			if (this.instConnectorSSLUser == null || (this.instConnectorSSLUser != null && this.instConnectorSSLUser == "")) { println "[E] RemoteInstanceDirectory SSL Connector : user attribute is mandatory and can't be empty"; cr=1}
			if (this.instConnectorSSLHost == null || (this.instConnectorSSLHost != null && this.instConnectorSSLHost == "")) { println "[E] RemoteInstanceDirectory SSL Connector : host attribute is mandatory "; cr=1}
			if (this.instConnectorSSLKeyfile == null && this.instConnectorSSLPassword == null) { println "[E] RemoteInstanceDirectory SSL Connector : Authentification by Password or KeyFile must be choised "; cr=1} 
			if (this.instConnectorSSLTrust!=null && this.instConnectorSSLTrust==false){
				if (this.instConnectorSSLKnownhosts == null || (this.instConnectorSSLKnownhosts != null && this.instConnectorSSLKnownhosts == "")) { println "[E] RemoteInstanceDirectory SSL Connector : /path/knownhosts file  is mandatory and can't be empty "; cr=1} 
			}
			
			if (this.instConnectorSSLKeyfile != null ){
				def file=new File(this.instConnectorSSLKeyfile)
				if (!file.isFile()){ println "[E] RemoteInstanceDirectory SSL Connector : KeyFile "+this.instConnectorSSLKeyfile+" doesn't exist "; cr=1}
			}
			
			if(cr!=0){
				return 1
			}
			def cmdline="if [ ! -d \"$remoteDir\" ]; then echo cr=77;exit 77;else echo cr=0;exit 0;fi"
			def ssh_cr=0
			def Properties out=new Properties()
			try{
			ant.sshexec(
				command:cmdline,
				
				host:instConnectorSSLHost,
				port:instConnectorSSLPort,
				username:instConnectorSSLUser,
				keyfile:instConnectorSSLKeyfile,
				passphrase:instConnectorSSLPassphrase,
				trust:instConnectorSSLTrust,
				knownhosts:instConnectorSSLKnownhosts,

				timeout:instConnectorSSLTimeout,
				
				verbose:instConnectorSSLVerbose,
				failonerror:true,
				suppresssystemout:true,
				usepty:false,
				outputproperty: 'result',
				
				)
			}
			catch (Exception e){
				ssh_cr=1
			/*ant.project.properties.each {
				println "it:$it"
			}*/
			}
			finally{
				
					def result=ant.project.properties.'result'.toString();
					def String[] lines=result.split("\n")
					def lastline=lines[lines.size()-1]
					if (lastline =~ "cr=0" && ssh_cr == 0){
						println "[I]  RemoteInstanceDirectory $instName : directory $instConnectorSSLUser@$instConnectorSSLHost($instConnectorSSLPort):$remoteDir exist"
					}
					else{
						if (lastline =~ "cr=77" && ssh_cr != 0){
							println "[E] RemoteInstanceDirectory $instName : directory $instConnectorSSLUser@$instConnectorSSLHost($instConnectorSSLPort):$remoteDir doesn't exist"
							cr=1
						}
						else{
							println "[E] Impossible to verify RemoteInstanceDirectory $instName : SSH connexion failed:"+result
							cr=1
						}
					}
				
				
				
			}
		}
		return cr
	}
	
	/*
	 * 
	ant = new AntBuilder()
    ant.sshexec(
          host:"lx0072.ux.his.no",
          username:"meling",
          keyfile:"/home/meling/.ssh/id_dsa",
          command:"echo Response at: \$HOSTNAME", failonerror:false
    )
    
    ant.scp(file:'user@host:/tmp/file.txt',  
    todir:"/home/localuser/",  
    verbose:true,  
    keyfile:"/home/localuser/.ssh/id_dsa",  
    passphrase:"password") 
	 */
	
	
}
	

