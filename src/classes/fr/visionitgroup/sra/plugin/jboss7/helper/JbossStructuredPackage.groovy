package fr.visionitgroup.sra.plugin.jboss7.helper

import fr.visionitgroup.sra.plugin.jboss7.helper.Jboss7CLIConnection
import org.jboss.as.cli.scriptsupport.*
import fr.nauzikaa.utils.zip.ZipTools;
import fr.nauzikaa.templateEngine.TemplateEngineTools;


/************************
 -----------------------
 package GroupOfApplications
 /path/env/jboss-socle-env.cli
 /path/env/jboss-socle-env.properties
	 jboss-group.env=DEV
	 jboss-group.name=Group1
	 jboss-group.type=standalone|domain
	 jboss-group.deployscope=""| server-group-name,server-group-name2|--all-server-group
	 
	 jboss-controler.user
	 jboss-controler.password
	 jboss-controler.host
	 jboss-controler.port
	 
	 jboss-inst.waitForStarting.timeout
	 jboss-inst.waitForStopping.timeout
	 
	 jboss-tplengine.begin-separator=@@
	 jboss-tplengine.end-separator=@@
	 jboss-tplengine.scan-zip-file=true
	 jboss-tplengine.zip-file-extension-regexp=".zip$|ear$|war$|jar$"
	 
	 jboss-inst.instance-1.name=/host:rhel6Node1
	 jboss-inst.instance1.connector-protocole=ssl
	jboss-inst.instance1.connector-ssl-user
	jboss-inst.instance1.connector-ssl-password
	jboss-inst.instance1.connector-ssl-host
	jboss-inst.instance1.connector-ssl-port
	jboss-inst.instance1.connector-ssl-keyfile
	jboss-inst.instance1.connector-ssl-passphrase
	jboss-inst.instance1.connector-ssl-verbose
	jboss-inst.instance1.connector-ssl-preserveLastModified
	 jboss-inst.instance-1.external-configdir.mapConfName1=/tmp/titi
	 jboss-inst.instance-1.external-configdir.mapConfName2=/tmp/titi2
	 jboss-inst.instance-1.external-libdir.mapLibName1=/tmp/titi
	 jboss-inst.instance-1.external-libdir.mapLibName2=/tmp/titi2
	 
	 jboss-inst.instance-2.name=/host:rhel6Node2
	 jboss-inst.instance-2.connector-protocole=local
	 jboss-inst.instance-2.connector-host=local
	 jboss-inst.instance-2.external-configdir.mapConfName1=/tmp/titi
	 jboss-inst.instance-2.external-configdir.mapConfName2=/tmp/titi2
	 jboss-inst.instance-2.external-libdir.mapLibName1=/tmp/titi
	 jboss-inst.instance-2.external-libdir.mapLibName2=/tmp/titi2
	 
	 
	 jboss-inst.var.token1=value1
	 jboss-inst.instance-1.var.token1=value1
	 jboss-inst.instance-2.var.token1=value2
	 
	 token1=value
	 tokenSRAApp=value
	 tokenSRAEnv=value
	 tokenSRAResource=value
	 
  *

 /package-name  = basedir
  * 		package.properties
  				group-name=$GroupName
  				application-deployMode=Full|Incr
  				config-deployMode=Full|Incr
  				lib-deployMode=Full|Incr
  				pkg-type=jboss7
				pkg-version=1.0
  				
			/app
			   /$GroupName-SubAppliName/
								 /versioned-file.(war|jar|ear)
								 /jboss-deployment.properties
							  		runtime-name=
							  		deployOptions=
								 /$GroupName-SubAppliName/jboss-deployment-env.cli
				  					/profile=full/subsystem=naming/binding=java\:global\/env\/flag2:add(binding-type=simple, type=boolean, value=true)
				  					/profile=full/subsystem=naming/binding=java\:global\/env\/flag:write-attribute(name=value,value=false)
			/env
				/DEV_application.properties.properties
				/...
				/PROD_application.properties.properties
				 
			/external-config
				$config_dir-map1/
				$config_dir-map2/
			/external-lib
				$lib_dir_map1/
				$lib_dir_map1/
 
 workspace/
		  /bck
		  /TE
 ------------------------
 **************/


public class JbossStructuredPackage {
	def String CST_JBOSSPACKAGE_PROPERTIES="jboss-package.properties"
	def String CST_JBOSSDEPLOYMENT_PROPERTIES="jboss-deployment.properties"
	def String CST_JBOSSDEPLOYMENT_ENV_CLI="jboss-deployment-env.cli"
	def String CST_JBOSSAPP_PROPERTIES="_application.properties"
	def Jboss7CLIConnection cli
	
	// workspace directories
	def File workdir=null
	def File myworkdir=null
	def String backupBaseName="bck"
	def File backupWorkdir=null
	def String templateEngineBaseName="TE"
	def File templateEngineWorkdir=null
	
	// package directories information
	def File basedir=null
	def String appBaseName="app"
	def File appBasedir=null
	def String externalConfigBaseName="external-config"
	def File externalConfigBasedir=null
	def String externalLibBaseName="external-lib"
	def File externalLibBasedir=null
	def String envBaseName="env"
	def File envBasedir=null
	
	
	// jboss deployment Informations
	def Properties jbossSocleEnvProp=null
	def Properties jbossAppEnvProp=null
	def String jbossGroupEnv=""  
	def String jbossGroupType=""
	def String jbossGroupDeployScope=""
	
	def jbossControlerUser=""
	def char[] jbossControlerPassword=null
	def jbossControlerHost=""
	def int jbossControlerPort=9999
	
	def int jbossInstanceWaitForStartingTimeout=0
	def int jbossInstanceWaitForStoppingTimeout=0
	 
	def jbossTplengineBeginSeparator=""
	def jbossTplengineEndSeparator=""
	def jbossTplengineScanZipFile=""
	def jbossTplengineZipFileExtensionRegexp=""
	def List jbossInstanceList=[]
	
	// Package Information
	
	def File pkgApplicationpropFile=null
	def String pkgGroupName=null
	def String pkgApplicationDeployMode="" // Full | Incr
	def String pkgConfigDeployMode="" // Full | Incr
	def String pkgLibDeployMode="" // Full | Incr
	def String pkgType=null
	def String pkgVersion=null
		
	def File pkgApplicationEnvFile =null
	def Properties pkgApplicationEnvProp =null
	
	def List pkgApplicationList=[]
	def List pkgApplicationStructuredPropertiesLst=[]
	
	def List pkgExternalConfigList=[]
	def List pkgExternalConfigRemoteInstanceList=[]
	def List pkgExternalLibList=[]
	def List pkgExternalLibRemoteInstanceList=[]
	def Map pkgExternalLibRemoteInstanceMap=[:]
	def Map pkgExternalConfigRemoteInstanceMap=[:]
	
	def private Boolean init=false
	def private Boolean workspaceCreated=false
	
	
	
	
	
	public JbossStructuredPackage(def String pkgDeployMode,def Properties jbossSocleEnvProp, def File pkgBasedir,def Properties jbossAppEnvProp=null, def File workdir){
		
		this.pkgApplicationpropFile=new File(pkgBasedir,CST_JBOSSPACKAGE_PROPERTIES)
		if(! pkgApplicationpropFile.isFile()){ throw new Exception("[E] JbossStructuredPackage : package doesn't contain :"+CST_JBOSSPACKAGE_PROPERTIES)}
		def java.util.Properties ppkg=new java.util.Properties()
		pkgApplicationpropFile.withInputStream {
		  stream -> ppkg.load(stream)
		}
		
		/*
		 * package.properties
  				pkg-group-name=$GroupName
  				pkg-application-deployMode=Full|Incr
  				pkg-config-deployMode=Full|Incr
  				pkg-lib-deployMode=Full|Incr
  				pkg-type=jboss7
				pkg-version=1.0
		 */
		if(ppkg.getProperty("pkg-group-name", "") != ""){this.pkgGroupName=ppkg.getProperty("pkg-group-name","")}
		if(ppkg.getProperty("pkg-application-deployMode", "") != ""){this.pkgApplicationDeployMode=ppkg.getProperty("pkg-application-deployMode","")}
		if(ppkg.getProperty("pkg-config-deployMode", "") != ""){this.pkgConfigDeployMode=ppkg.getProperty("pkg-config-deployMode","")}
		if(ppkg.getProperty("pkg-lib-deployMode", "") != ""){this.pkgLibDeployMode=ppkg.getProperty("pkg-lib-deployMode","")}
		if(ppkg.getProperty("pkg-type", "") != ""){this.pkgType=ppkg.getProperty("pkg-type","")}
		if(ppkg.getProperty("pkg-version", "") != ""){this.pkgVersion=ppkg.getProperty("pkg-version","")}
		
		if (pkgGroupName == ""){ throw new Exception ("[E] JbossStructuredPackage : Syntax error on "+CST_JBOSSPACKAGE_PROPERTIES+": pkg-group-name is mandatory")}
		
		if (this.pkgApplicationDeployMode!= "Full" && this.pkgApplicationDeployMode!= "Incr"){throw new Exception("[E] JbossStructuredPackage : Syntax error on "+CST_JBOSSPACKAGE_PROPERTIES+": pkg-application-deployMode must be  Full or Incr ")	}
		if (this.pkgConfigDeployMode!= "Full" && this.pkgConfigDeployMode!= "Incr"){throw new Exception("[E] JbossStructuredPackage : Syntax error on "+CST_JBOSSPACKAGE_PROPERTIES+": pkg-config-deployMode must be  Full or Incr ")	}
		if (this.pkgLibDeployMode!= "Full" && this.pkgLibDeployMode!= "Incr"){throw new Exception("[E] JbossStructuredPackage : Syntax error on "+CST_JBOSSPACKAGE_PROPERTIES+": pkg-lib-deployMode must be  Full or Incr ")	}
		if (this.pkgType!= "jboss7" && this.pkgType!= "jboss-eap6.1"){throw new Exception("[E] JbossStructuredPackage : Syntax error on "+CST_JBOSSPACKAGE_PROPERTIES+": pkg-type must be  jboss7 or jboss-eap6.1 ")	}
		if (this.pkgVersion!= "v1.0" ){throw new Exception("[E] JbossStructuredPackage : Syntax error on "+CST_JBOSSPACKAGE_PROPERTIES+":pkg-version must be  v1.0  ")	}
		
		
		
		this.jbossSocleEnvProp = jbossSocleEnvProp
		this.jbossGroupEnv=jbossSocleEnvProp.getProperty("jboss-group.env", "")
		this.jbossGroupType=jbossSocleEnvProp.getProperty("jboss-group.type", "")
		this.jbossGroupDeployScope=jbossSocleEnvProp.getProperty("jboss-group.deployscope", "")
		 
		this.jbossControlerUser=jbossSocleEnvProp.getProperty("jboss-controler.user", "")
		this.jbossControlerPassword=jbossSocleEnvProp.getProperty("jboss-controler.password", "")
		this.jbossControlerHost=jbossSocleEnvProp.getProperty("jboss-controler.host", "")
		
		if(jbossSocleEnvProp.getProperty("jboss-controler.port", "") != ""){this.jbossControlerPort=Integer.parseInt(jbossSocleEnvProp.getProperty("jboss-controler.port"))}
		if(jbossSocleEnvProp.getProperty("jboss-inst.waitForStarting.timeout", "")!= ""){this.jbossInstanceWaitForStartingTimeout=Integer.parseInt(jbossSocleEnvProp.getProperty("jboss-inst.waitForStarting.timeout"))}
		if(jbossSocleEnvProp.getProperty("jboss-inst.waitForStopping.timeout", "")!= ""){this.jbossInstanceWaitForStoppingTimeout=Integer.parseInt(jbossSocleEnvProp.getProperty("jboss-inst.waitForStopping.timeout"))}
		
		 
		this.jbossTplengineBeginSeparator=jbossSocleEnvProp.getProperty("jboss-tplengine.begin-separator", "")
		this.jbossTplengineEndSeparator=jbossSocleEnvProp.getProperty("jboss-tplengine.end-separator", "")
		this.jbossTplengineScanZipFile=jbossSocleEnvProp.getProperty("jboss-tplengine.scan-zip-file", "")
		this.jbossTplengineZipFileExtensionRegexp=jbossSocleEnvProp.getProperty("jboss-tplengine.zip-file-extension-regexp", "")
		
		
		
		if (jbossGroupEnv == ""){ throw new Exception ("[E] JbossStructuredPackage jbossSocleEnvProp must contain property : jboss-group.env")}
		if (this.jbossGroupType!= "standalone" && this.jbossGroupType!= "domain"){throw new Exception("[E] JbossStructuredPackage : type of Jboss architecture must be : standalone or domain ")	}
		if (this.jbossGroupType!= "domain" && this.jbossGroupDeployScope == ""){throw new Exception("[E] JbossStructuredPackage : deploy scope can't be null for domain architecture ")	}

		if (jbossControlerUser == ""){ throw new Exception ("[E] JbossStructuredPackage jbossSocleEnvProp must contain property : jboss-controler.user")}
		if (jbossControlerPassword == ""){ throw new Exception ("[E] JbossStructuredPackage jbossSocleEnvProp must contain property : jboss-controler.password")}
		if (jbossControlerHost == ""){ throw new Exception ("[E] JbossStructuredPackage jbossSocleEnvProp must contain property : jboss-controler.host")}
		if (jbossControlerPort == ""){ throw new Exception ("[E] JbossStructuredPackage jbossSocleEnvProp must contain property : jboss-controler.port")}

		
		if (jbossInstanceWaitForStartingTimeout == ""){ throw new Exception ("[E] JbossStructuredPackage jbossSocleEnvProp must contain property : jboss-inst.waitForStarting.timeout")}
		if (jbossInstanceWaitForStoppingTimeout == ""){ throw new Exception ("[E] JbossStructuredPackage jbossSocleEnvProp must contain property : jboss-inst.waitForStopping.timeout")}

		if (jbossTplengineBeginSeparator == ""){ throw new Exception ("[E] JbossStructuredPackage jbossSocleEnvProp must contain property : jboss-tplengine.begin-separator")}
		if (jbossTplengineEndSeparator == ""){ throw new Exception ("[E] JbossStructuredPackage jbossSocleEnvProp must contain property : jboss-tplengine.end-separator")}
		if (jbossTplengineScanZipFile == ""){ throw new Exception ("[E] JbossStructuredPackage jbossSocleEnvProp must contain property : jboss-tplengine.scan-zip-file")}
		if (this.jbossTplengineScanZipFile!= "true" && this.jbossTplengineScanZipFile!= "false"){throw new Exception("[E] JbossStructuredPackage : jboss-tplengine.scan-zip-file  must be : true or false ")	}
		if (jbossTplengineZipFileExtensionRegexp == ""){ throw new Exception ("[E] JbossStructuredPackage jbossSocleEnvProp must contain property : jbossTplengineZipFileExtensionRegexp")}
		
		//jbossInstanceList.add(cr)
		//jboss-inst.instance-1.name=
		jbossSocleEnvProp.each{ name,value ->
			if(name =~ "^jboss-inst\\..*\\.name\$"){
				println "[D] $name=$value"
				def nameInst=name.split(/\./)[1]
				println "[D] nameInst=$nameInst"
				if(!jbossInstanceList.contains(nameInst)){
					jbossInstanceList.add(nameInst)
				}
				else{
					throw new Exception("[E] JbossStructuredPackage jbossSocleEnvProp can't contain more than one entry : jboss-inst."+nameInst+".name ")  
				}
			}
		}
		println "[D] jbossInstanceList="+jbossInstanceList
		this.cli = new Jboss7CLIConnection(jbossControlerUser,jbossControlerPassword,jbossControlerHost,jbossControlerPort)
		
		this.jbossAppEnvProp=jbossAppEnvProp
		
		
		this.envBasedir=new File(pkgBasedir,envBaseName)
		
		this.pkgApplicationEnvFile=new File(envBasedir,jbossGroupEnv+CST_JBOSSAPP_PROPERTIES)
		if (pkgApplicationEnvFile.isFile()){
			this.pkgApplicationEnvProp=new Properties()
			pkgApplicationEnvFile.withInputStream {
				stream -> pkgApplicationEnvProp.load(stream)
			  }
		}
		
		this.basedir=pkgBasedir
		this.appBasedir=new File(basedir,appBaseName)
		this.externalConfigBasedir=new File(basedir,externalConfigBaseName)
		this.externalLibBasedir=new File(basedir,externalLibBaseName)
		this.workdir=workdir
		this.myworkdir=new File(workdir,"wk_"+new Date().format("yyMMddHHmmssSSS"))
		this.backupWorkdir=new File(myworkdir,backupBaseName)
		this.templateEngineWorkdir=new File(myworkdir,templateEngineBaseName)
		
		if (workdir==null || (workdir!=null && !workdir.isDirectory() )){throw new Exception("[E] JbossStructuredPackage wordir can't be null and must be a directory")	}
		if (basedir==null || (basedir!=null && !basedir.isDirectory() )){throw new Exception("[E] JbossStructuredPackage basedir of package can't be null and must be a directory")		}
		
		
		
		
		
		
		
		
		
		
		
	}
	
	// init
	private void init(){
		println "[I] Initialisation starting"
		try{
			createWorkSpace()
			checkStructureOfPackage()
			checkRemoteInstanceDirectory()
			checkCLIConnexion()
			checkJbossGroupDeployScopeExistence()
			this.init=true
		}
		catch(Exception e){
			throw new Exception (e.getMessage())
		}
		println "[I] End of Initialisation "
		
		
	}
	
	private void createWorkSpace(){
		//test existentce of workdir
		if (workdir==null || (workdir!=null && !workdir.isDirectory() )){
			throw new Exception("[E] JbossStructuredPackage.createWorkSpace wordir($workdir) can't be null and must be a directory")
		}
		
		if(! myworkdir.mkdirs()){
			throw new Exception("[E] JbossStructuredPackage.createWorkSpace Impossible to create :"+myworkdir.absolutePath)
		}
		if(! backupWorkdir.mkdirs()){
			throw new Exception("[E] JbossStructuredPackage.createWorkSpace Impossible to create :"+backupWorkdir.absolutePath)
		}
		if(! templateEngineWorkdir.mkdirs()){
			throw new Exception("[E] JbossStructuredPackage.createWorkSpace Impossible to create :"+templateEngineWorkdir.absolutePath)
		}
		
		
		
		workspaceCreated=true
		
		
	}
	
	private void deleteWorkSpace(){
		if(workspaceCreated){
			if(myworkdir.isDirectory()){
				if(!myworkdir.deleteDir()){
					throw new Exception("[E] JbossStructuredPackage.createWorkSpace Impossible to delete :"+templateEngineWorkdir.absolutePath)
				}
			}
		}
	}
	
	private void checkStructureOfPackage(){
		// Control $basedir/app
		if (appBasedir==null || (appBasedir!=null && !appBasedir.isDirectory() )){
			println "[W] checkStructureOfPackage The package doesn't contain "+appBasedir.absolutePath
			appBasedir=null
		}
		else{
			// generate List of subapplication (all directories into $basedir/$pkgGroupName/app/*)
			appBasedir.eachFile{it ->
				if(it.isDirectory()){
					if(it.getName() =~ "^$pkgGroupName-"){
						pkgApplicationList.add(it.getName())
					}
					else{
						throw new Exception("[E] checkStructureOfPackage Invalid name for application :"+it.getName()+" : must begin by $pkgGroupName-")
					}
				}
				else{
					// Control non file into $basedir/app/*
					println("[W] checkStructureOfPackage Package contain invalide file :"+it.absolutePath+" ... Ignored!!!")
				}
			}
			// Check size of List of subapplication
			if (pkgApplicationList.size() == 0 ){
				println("[W] checkStructureOfPackage Package contain no application to deploy")
			}
			println "[D] applicationList="+pkgApplicationList
			pkgApplicationList.each { it ->
				// FYI
				//$basedir/app/$subappli
				//$basedir/app/$subappli/appli-version.*.(jar|war|ear)
				//$basedir/app/$subappli/jboss-deployment.properties
				//$basedir/app/$subappli/jboss-deployment-env.cli
				//List of Control
				// Control name 's syntax
				// Control each file that compose a sub application
				// Control that one andonly one source file is present
				// Other actions:
				// generate a properties to store all information of sub application
				// store properties inot applicationStructuredPropertiesLst
				// this properties will be useful for Jboss7AdminTools.deployOnePackagedApplication
				
				def java.util.Properties p=new java.util.Properties()
				
				
				
				/****
				def String deployRunTimeName=deploymentProperties.getProperty("deployOption_runtime-name", "")
				def String deployOptions=deploymentProperties.getProperty("deployOption_others", "")
				
				def String deployScope=deploymentProperties.getProperty("deployOption_scope", "")
				def deployPathSrcFile=deploymentProperties.getProperty("deployOption_src-path", "")
				def deployPathEnvFile=deploymentProperties.getProperty("deployOption_env-path", "")
				def deployName=deploymentProperties.getProperty("deployOption_name", "")
				
				*****/
				p.put("deployOption_name", it)
				p.put("deployOption_scope", this.jbossGroupDeployScope)
				
				
				def subappliBaseDir=new File(appBasedir,it)
				
				
				subappliBaseDir.eachFile { file->
					
					if(file.getName() == this.CST_JBOSSDEPLOYMENT_PROPERTIES ){
						// case where file is general application descriptor
						println "[I] checkStructureOfPackage AutoDetect: Jboss Deployment properties : "+file.getAbsolutePath()
						//load properties
						def java.util.Properties penv=new java.util.Properties()
						file.withInputStream {
						  stream -> penv.load(stream)
						}
						def deployOption_runtimename=penv.getProperty("deployOption_runtime-name", "")
						def deployOption_others=penv.getProperty("deployOption_others", "")
						if(deployOption_runtimename!=""){
							p.put("deployOption_runtime-name", deployOption_runtimename)
						}
						if(deployOption_others!=""){
							p.put("deployOption_others", deployOption_others)
						}
						
					}
					else if(file.getName() == this.CST_JBOSSDEPLOYMENT_ENV_CLI ){
						println "[I] checkStructureOfPackage AutoDetect: Jboss Deployment Env Script : "+file.getAbsolutePath()
						p.put("deployOption_env-path", file.absolutePath)
					}
					else{
						if (!file.isDirectory()){
							if (p.containsKey("deployOption_src-path")){
								throw new Exception ("[E] Sub application 's name ( $it) must contain only one source file (like jar, war or ear)")
							}
							else{
								 // case where file is the source war,ear or jar , ...
								// Control that each source started by GroupName-
								if(file.getName() =~ "^$it-"){
									println "[I] checkStructureOfPackage AutoDetect: Jboss Deployment Src Binary : "+file.getAbsolutePath()
									p.put("deployOption_src-path", file.absolutePath)
								}
								else{
									throw new Exception("[E] checkStructureOfPackage Invalid name for source file :"+file.absolutePath+" : must begin by $it-")
								}
								
								
							}
						}
						else{
							println "[W] checkStructureOfPackage Unknown directory : "+file.absolutePath+" . Ignored ..."
						}
					}
					
				}
				
				if (! p.containsKey("deployOption_src-path") ){
					throw new Exception ("[E] checkStructureOfPackage Sub application 's name ( $it) must contain minimum of one source file (like jar, war or ear)")
				}
				
				
				// store p into applicationStructuredPropertiesLst
				this.pkgApplicationStructuredPropertiesLst.add(p)
			}
		}
		
		// Control $basedir/external-config
		if (externalConfigBasedir==null || (externalConfigBasedir!=null && !externalConfigBasedir.isDirectory() )){
			println "[W] checkStructureOfPackage The package doesn't contain external Config directory "
			externalConfigBasedir=null
		}
		else{
			try{
				externalConfigBasedir.eachFile{it ->
					if (!it.isDirectory()){
						println("[W] checkStructureOfPackage Package contain invalide file :"+it.absolutePath+" ... Ignored!!!")
					}
					else{
						println "[I] checkStructureOfPackage AutoDetect: External Config directory : "+it.absolutePath
						//pkgExternalConfigRemoteInstanceList
						def map=it.getName()
						//check taht $map is define for each jboss instance
						//  ex: jboss-inst.instance-1.external-configdir.mapConfName1=/tmp/titi
						//      jboss-inst.instance-2.external-configdir.mapConfName2=/tmp/titi2
						this.jbossInstanceList.each{
							try{
								def String remotedir=jbossSocleEnvProp.getProperty("jboss-inst."+it+".external-configdir."+map, "")
								if( remotedir == ""){
									throw new Exception("[E] checkStructureOfPackage Impossible to locate jbossSocleEnvProp jboss-inst."+it+".external-configdir.$map")
								}
								else{
									def RemoteInstanceDirectory rid=getRemoteInstanceDirectory(it,map,remotedir)
									if(rid != null){
										pkgExternalConfigRemoteInstanceList.add(rid)
										pkgExternalConfigRemoteInstanceMap[it+"-"+map]=rid
									}
									else{
										throw new Exception ("[E] checkStructureOfPackage Impossible to create RemoteInstanceDirectory for $it-$map")
									}
								}
							}
							catch (Exception e){
								throw new Exception (e.getMessage())
							}
						}
						pkgExternalConfigList.add(map)
					}
				}
			}
			catch (Exception e){
				throw new Exception (e.getMessage())
				
			}
			if (pkgExternalConfigList.size() == 0 ){
				println("[W] checkStructureOfPackage Package contain no external-conf directories to deploy")
			}
		}
		
		// Control $basedir/external-lib
		if (externalLibBasedir==null || (externalLibBasedir!=null && !externalLibBasedir.isDirectory() )){
			println "[W] The package doesn't contain "+externalLibBasedir.absolutePath
			externalLibBasedir=null
		}
		else{
			try{
				externalLibBasedir.eachFile{it ->
					if (!it.isDirectory()){
						println("[W] checkStructureOfPackage Package contain invalide file :"+it.absolutePath+" ... Ignored!!!")
					}
					else{
						println "[I] checkStructureOfPackage AutoDetect: External Lib directory : "+it.absolutePath
						//pkgExternalLibRemoteInstanceList
						def map=it.getName()
						//check taht $map is define for each jboss instance
						//  ex: jboss-inst.instance-1.external-libdir.mapConfName1=/tmp/titi
						//      jboss-inst.instance-2.external-libdir.mapConfName2=/tmp/titi2
						this.jbossInstanceList.each{
							try{
								def remotedir=jbossSocleEnvProp.getProperty("jboss-inst."+it+".external-libdir.$map", "")
								if( remotedir == ""){
									throw new Exception("[E] checkStructureOfPackage Impossible to locate jbossSocleEnvProp jboss-inst."+it+".external-libdir.$map")
								}
								else{
									def RemoteInstanceDirectory rid=getRemoteInstanceDirectory(it,map,remotedir)
									if(rid != null){
										pkgExternalLibRemoteInstanceList.add(rid)
										pkgExternalLibRemoteInstanceMap[it+"-"+map]=rid
									}
									else{
										throw new Exception ("[E] checkStructureOfPackage Impossible to create RemoteInstanceDirectory for $it-$map")
									}
								}
							}
							catch (Exception e){
								throw new Exception (e.getMessage())
							}
						}
						pkgExternalLibList.add(map)
						
					}
				}
			}
			catch (Exception e){
				throw new Exception (e.getMessage())
				
			}
			if (pkgExternalLibList.size() == 0 ){
				println("[W] Package contain no external-lib directories to deploy")
			}
		}
		println "[D] pkgExternalConfigList"+pkgExternalConfigList
		println "[D] pkgExternalConfigRemoteInstanceMap"+pkgExternalConfigRemoteInstanceMap
		println "[D] pkgExternalLibList"+pkgExternalLibList
		println "[D] pkgExternalLibRemoteInstanceMap"+pkgExternalLibRemoteInstanceMap
		
	}
	private RemoteInstanceDirectory getRemoteInstanceDirectory(def String instanceName,def String mapName,def String remotedir){
		 def RemoteInstanceDirectory rid=new RemoteInstanceDirectory("$instanceName-$mapName")
		 rid.setRemoteDir(remotedir)
		 
		 jbossSocleEnvProp.each { token,value ->
			 if(token =~ "jboss-inst.${instanceName}.connector-protocole")		{ rid.setInstConnectorProtocole(value)}
			if(token =~ "jboss-inst.${instanceName}.connector-ssl-user")		{ rid.setInstConnectorSSLUser(value)}
			if(token =~ "jboss-inst.${instanceName}.connector-ssl-password")	{ rid.setInstConnectorSSLPassword(value)}
			if(token =~ "jboss-inst.${instanceName}.connector-ssl-host")		{ rid.setInstConnectorSSLHost(value)}
			if(token =~ "jboss-inst.${instanceName}.connector-ssl-port")		{ if(value != ""){rid.setInstConnectorSSLPort(Integer.parseInt(value))}}
			if(token =~ "jboss-inst.${instanceName}.connector-ssl-trust")		{ if(value == "true"){rid.setInstConnectorSSLTrust(true)} ; if(value == "false"){rid.setInstConnectorSSLTrust(false)} }
			if(token =~ "jboss-inst.${instanceName}.connector-ssl-knownhosts")	{ rid.setInstConnectorSSLKnownhosts(value)}
			if(token =~ "jboss-inst.${instanceName}.connector-ssl-keyfile")		{ rid.setInstConnectorSSLKeyfile(value)}
			if(token =~ "jboss-inst.${instanceName}.connector-ssl-passphrase")	{ rid.setInstConnectorSSLPassphrase(value)}
			if(token =~ "jboss-inst.${instanceName}.connector-ssl-verbose")   	{ if(value == "true"){rid.setInstConnectorSSLVerbose(true)}; if(value == "true"){rid.setInstConnectorSSLVerbose(false)}; }
			if(token =~ "jboss-inst.${instanceName}.connector-ssl-timeout")		{ if(value != ""){rid.setInstConnectorSSLTimeout(Integer.parseInt(value))}}	 
		 }
		 
			
		 return rid
	 }
	 
	private void checkJbossGroupDeployScopeExistence() {
		
		if(!Jboss7AdminTools.isScopeExist(this.cli,this.jbossGroupDeployScope)){
			throw new Exception("[E] JbossStructuredPackage.checkJbossGroupDeployScopeExistence JbossGroupDeployScope doesn't exist: ")
		}
	}

	private void checkCLIConnexion() {
		
		try{
			this.cli.connect()
			println "[I] checkCLIConnexion cli connexion to  Jboss Controller is available "
			//=new Jboss7CLIConnection(username,password,controllerHost, controllerPort=9999)
		}
		catch (Exception e){
			throw new Exception("[E] JbossStructuredPackage.checkCLIConnexion Impossible to connect to Jboss Controller "+e.getMessage())
		}
	}
	
	
	
	private void checkRemoteInstanceDirectory(){
		pkgExternalConfigRemoteInstanceMap.each{ key,val->
			def RemoteInstanceDirectory rid=val
			def name=rid.getInstName()
			if(rid.control()!= 0){
				throw new Exception("[E] checkRemoteInstanceDirectory the RemoteInstanceDirectory $name isn't available ")
			}
			else{
				println "[I] checkRemoteInstanceDirectory the RemoteInstanceDirectory $name is available "
			}
		}
		pkgExternalLibRemoteInstanceMap.each{ key,val->
			def RemoteInstanceDirectory rid=val
			def name=rid.getInstName()
			if(rid.control()!= 0){
				throw new Exception("[E] checkRemoteInstanceDirectory the RemoteInstanceDirectory $name isn't available ")
			}
			else{
				println "[I] checkRemoteInstanceDirectory the RemoteInstanceDirectory $name is available "
			}
		}
	}
	
	
	private void generateMapDirectoryForEachInstance(){
		// creation d'un repertoire MAP pour chaque instance
		// ex: 
		//     templateEngineWorkdir/ConfMapping/instance1/MapConf1
		//     templateEngineWorkdir/ConfMapping/instance1/MapConf2
		//     templateEngineWorkdir/LibMapping/instance1/Maplib1
		//     templateEngineWorkdir/LibMapping/instance1/Maplib2
		// et
		//     templateEngineWorkdir/ConfMapping/instance2/MapConf1
		//     templateEngineWorkdirConfMapping/instance2/MapConf2
		//     templateEngineWorkdir/LibMapping/instance2/Maplib1
		//     templateEngineWorkdir/LibMapping/instance2/Maplib2
		println "[I] prepare Lib Mapping"
		def dl=new File(templateEngineWorkdir,"LibMapping")
		// mkdir templateEngineWorkdir/LibMapping/instance1/
		if(!dl.isDirectory()){
			if(! dl.mkdirs()){	throw new Exception("[E] generateMapDirectoryForEachInstance : Impossible to create directory "+dl.absolutePath)	}
		}
		jbossInstanceList.each{
			def di=new File(dl,it)
			// mkdir templateEngineWorkdir/instance1/
			if(!di.isDirectory()){
				if(! di.mkdirs()){	throw new Exception("[E] generateMapDirectoryForEachInstance : Impossible to create directory "+di.absolutePath)	}
			}
			pkgExternalLibList.each{ map->
				def dm=new File(di,map)
				// mkdir templateEngineWorkdir/instance1/Maplib1
				if(!dm.isDirectory()){
					if(! dm.mkdir()){	throw new Exception("[E] generateMapDirectoryForEachInstance : Impossible to create directory "+dm.absolutePath)	}
				}
				// ex: copy -r basedir/external-lib/$lib_dir_map1/ to templateEngineWorkdir/instance2/Maplib1
				// basedir/external-lib/$lib_dir_map1/ externalConfigBasedir externalLibBasedir pkgExternalConfigList  pkgExternalConfigRemoteInstanceList  pkgExternalLibList  pkgExternalLibRemoteInstanceList
				def ant = new AntBuilder()
				try {
					    
					def fromDirFile=new File(externalLibBasedir,map)
					def fromDir=fromDirFile.absolutePath
					def String toDir=dm.absolutePath
					println "[D] Preparing LibMap $map for instance $it by  synchronizing $fromDir to $toDir"
					ant.sync(verbose: "false", todir: toDir, overwrite:true) {fileset(dir: fromDir) {	"**/*"}}
				}
				
				catch (Exception e) {
					throw new Exception("Error synchronizing directories: ${e.message}")
				}
			}
			
		}
		
		println "[I] prepare Config Mapping"
		def dc=new File(templateEngineWorkdir,"ConfMapping")
		// mkdir templateEngineWorkdir/LibMapping/instance1/
		if(!dc.isDirectory()){
			if(! dc.mkdirs()){	throw new Exception("[E] generateMapDirectoryForEachInstance : Impossible to create directory "+dc.absolutePath)	}
		}
		jbossInstanceList.each{
			def di=new File(dc,it)
			// mkdir templateEngineWorkdir/instance1/
			if(!di.isDirectory()){
				if(! di.mkdirs()){	throw new Exception("[E] generateMapDirectoryForEachInstance : Impossible to create directory "+di.absolutePath)	}
			}
			// touch  templateEngineWorkdir/instance1/global_instance.properties
			def tempGlobalPropFile=new File(di,"global_instance.properties")
			
			// recup of list of token contained by jbossSocleEnvProp
			// ex: 
			//    
			//    token1=valueA
			//    jboss-inst.instance1.var.token1=valueB => token1=valueB (it's specific value for instance1)
			def jbossSocleEnvPropNew=new Properties()
			jbossSocleEnvProp.each{ key,val -> 
				if (key =~ "^jboss-.*"){
				}
				else{
					jbossSocleEnvPropNew.put(key, val)
				}
			}
			jbossSocleEnvProp.each{ String key, String val ->
				if (key =~ "^jboss-.*\\.$it\\.var\\."){
					// purge de la key=jboss-inst.instance1.var.token1 => keynew=token1
					def keynew=key.replaceFirst("^jboss-.*\\.$it\\.var\\.", "")
					jbossSocleEnvPropNew.put(keynew, val)
				}
				
			}
			
			// fusion of different Prop : pkgApplicationEnvProp jbossAppEnvProp  jbossSocleEnvProp
			def tempGlobalProp=new Properties()
			pkgApplicationEnvProp.each{ key,val -> tempGlobalProp.put(key, val)}
			jbossAppEnvProp.each{ key,val -> tempGlobalProp.put(key, val)}
			jbossSocleEnvPropNew.each{ key,val -> tempGlobalProp.put(key, val)}
			tempGlobalProp.store(tempGlobalPropFile.newWriter(), null)
			
			pkgExternalConfigList.each{ map->
				def dm=new File(di,map)
				// mkdir templateEngineWorkdir/instance1/MapConf1
				if(!dm.isDirectory()){
					if(! dm.mkdir()){	throw new Exception("[E] generateMapDirectoryForEachInstance : Impossible to create directory "+dm.absolutePath)	}
				}
				// ex: copy -r basedir/external-lib/$conf_dir_map1/ to templateEngineWorkdir/instance2/Mapconf1
				// basedir/external-lib/$lib_dir_map1/ externalConfigBasedir externalLibBasedir pkgExternalConfigList  pkgExternalConfigRemoteInstanceList  pkgExternalLibList  pkgExternalLibRemoteInstanceList
				
						
					def fromDirFile=new File(externalConfigBasedir,map)
					def fromDir=fromDirFile.absolutePath
					def String toDir=dm.absolutePath
					println "[D] Preparing ConfMap $map for instance $it by  synchronizing $fromDir to $toDir"
					
					def File TEWorkdir=new File(templateEngineWorkdir,"TEI")
					if(TEWorkdir.isDirectory()){
						if(!TEWorkdir.deleteDir()){ throw new Exception("[E] generateMapDirectoryForEachInstance : Impossible to delete directory "+TEWorkdir.absolutePath)	}
					}
					if(! TEWorkdir.mkdir()){	throw new Exception("[E] generateMapDirectoryForEachInstance : Impossible to create directory "+TEWorkdir.absolutePath)	}
					
					
				try{	
					
					def TemplateEngineTools t=new TemplateEngineTools(fromDir,toDir,TEWorkdir.absolutePath)
					t.setInputPropertiesFilesList(tempGlobalPropFile.absolutePath)
					t.setIncludesLst("**/*")
					t.setStartDelimiter(jbossTplengineBeginSeparator)
					t.setEndDelimiter(jbossTplengineEndSeparator)
					if(jbossTplengineScanZipFile){
						t.setListZipFileRegExp(jbossTplengineZipFileExtensionRegexp)
					}
					t.setValided(true)
					t.run()
					
					
				}
				
				catch (Exception e) {
					throw new Exception("[E] Error during Template Engine Operation between $fromDir and $toDir ")
				}
			}
			
		}
		
		// copy des Maplib dans les Maplib associés de chaque instance
		//  loop for jbossInstanceList [instance1,instance2]
		//     => loop for pkgExternalLibList [Maplib1,Maplib2]
		//          => cp externalLibBasedir/$map  templateEngineWorkdir/$instanceName/$map
		
		// Genere pour chaque instance un fichier  instance_parameter.prop
		// pkgApplicationEnvProp jbossAppEnvProp  jbossSocleEnvProp
		
		// paramaters.properties = 
		// copy des MapConf dans les Maplib associés de chaque instance
		//  loop for jbossInstanceList [instance1,instance2]
		//     => loop for pkgExternalConfigList [MapConf1,MapConf2]
		//          => Template :  externalConfigBasedir/$map  templateEngineWorkdir/$instanceName/$map fonction instance_parameter.prop
		//
		
		
		
		// 
		/***
		 
		 def TemplateEngine t=new TemplateEngine(dirOffset,destDir,"d:\\SRA\\templateEngine\\temp")
   t.setExcludesLst(excludes)
   t.setIncludesLst(includes)
   t.setStartDelimiter(startDelimiter)
   t.setEndDelimiter(endDelimiter)
   t.setListZipFileRegExp(listZipFileRegExp)
   t.setPropFileName(propFileName)
   t.setPropertyPrefix(propertyPrefix)
   t.setEnvPropValues(envPropValues)
   t.setExplicitTokens( explicitTokens)
   t.setInputPropertiesFilesList(inputPropertiesFilesList)
   t.setPropFileName(propFileName)
   t.setValided(valided)
   
   def cr=t.run()
   
		 ****/
	}
	
	private void deployGroupofApplicationsForEachInstance(def Boolean AutoRollbackMode=false){
		
		println "[I] deployGroupofApplications on Each Instance"
		println "[I] deployGroupofApplications deploy mode :$pkgApplicationDeployMode"
		// pkgApplicationDeployMode pkgConfigDeployMode pkgLibDeployMode
		def syncMode=""
		if (pkgApplicationDeployMode== "Full"){ 
			if(Jboss7AdminTools.isExistGroupOfApplications( cli,pkgGroupName)){
				println "[I] deployGroupofApplications uninstall all of application of group :$pkgGroupName"
				if( Jboss7AdminTools.undeployGroupOfApplications( cli,pkgGroupName)){
					throw new Exception ("[E] deployGroupofApplications Impossible to uninstall all  applications of group :$pkgGroupName. Abort...")
				}
			}
			else{
				println "[I] group :$pkgGroupName doesn't exist yet: no need to undeploy applications of group"
			}
			
		}
		def cr
		println "pkgApplicationStructuredPropertiesLst:"+pkgApplicationStructuredPropertiesLst
		cr=Jboss7AdminTools.deployMultiplePackagedApplications( cli,pkgApplicationStructuredPropertiesLst)
		
		
		if (cr != 0 ){
			if( AutoRollbackMode){
				def cr_restore=JbossStructuredPackage.restorePackage()
				if (cr_restore!=0){
					throw new Exception ("[E] deployGroupofApplications Error during installation of application(s) of group:$pkgGroupName. AutoRoll FAILED !!!...")
				}
				else{
					throw new Exception ("[E] deployGroupofApplications Error during installation of application(s) of group:$pkgGroupName. AutoRoll SUCCES !!!...")
				}
			}
		}
		if (cr != 0){
			throw new Exception ("[E] deployGroupofApplications Error during installation of application(s) of group:$pkgGroupName. Abort...")
		}
		
		
		// deployMultiplePackagedApplications( cli,pkgApplicationStructuredPropertiesLst)
		// exportGroupOfApplications(def Jboss7CLIConnection cli,def String regexpGroupName,def String path_exportdir_destination)
		// checkAllHostOfDomainConnection()
		
	}
		
	private void deployLibDirectoryForEachInstance(){
		
		println "[I] deploy Lib Directory on Each Instance : Starting..."
		def syncMode=""
		if (pkgLibDeployMode== "Full"){ syncMode="deleteAndInsert"}else{syncMode="insertOrUpdate"}
		
		def dl=new File(templateEngineWorkdir,"LibMapping")
		
		jbossInstanceList.each{
			def di=new File(dl,it)
			println "[D] pkgExternalLibList.size()="+pkgExternalLibList.size()
			println "[D] lib it=$it"
			for (def i=0;i<pkgExternalLibList.size();i++){
				def String map=pkgExternalLibList.get(i)
				println "[D] lib map=$map"
				def dm=new File(di,map)
				
				def RemoteInstanceDirectory rid=pkgExternalLibRemoteInstanceMap.get(it+"-"+map)
				def ridName=rid.getInstName()
				if(rid.syncFromLocalDirectoryToRemote(dm.absolutePath, syncMode)!= 0){
					throw new Exception ("[E] deployConfigDirectoryForEachInstance sync of Remote Instance Directory $ridName : Failed")
				}
			}
			
			
		}
		println "[I] deploy Lib Directory on Each Instance: End succesfully "
	}
	private void deployConfigDirectoryForEachInstance(){
		println "[I] deploy Config Directory on Each Instance: Starting..."
		
		def syncMode=""
		if (pkgConfigDeployMode== "Full"){ syncMode="deleteAndInsert"}else{syncMode="insertOrUpdate"}
		
		def dc=new File(templateEngineWorkdir,"ConfMapping")
		
		jbossInstanceList.each{
			def di=new File(dc,it)
			println "[D] pkgExternalConfigList.size()="+pkgExternalConfigList.size()
			println "[D] lib it=$it"
			
			for (def i=0;i<pkgExternalConfigList.size();i++){
				def String map=pkgExternalConfigList.get(i)
				println "[D] lib map=$map"
				def dm=new File(di,map)
				
				def RemoteInstanceDirectory rid=pkgExternalConfigRemoteInstanceMap.get(it+"-"+map)
				def ridName=rid.getInstName()
				if(rid.syncFromLocalDirectoryToRemote(dm.absolutePath, syncMode)!= 0){
					throw new Exception ("[E] deployConfigDirectoryForEachInstance sync of Remote Instance Directory $ridName : Failed")
				}
			}
			
			
		}
		println "[I] deploy Config Directory on Each Instance: End succesfully "
	}
	
	static public  Integer  generatePackage(){
		
	}
	static public Integer  restorePackage(){
	
	}
	
	private Integer prepareDeployPackage(){
		try{
	// deploy
		//   init
			init()
		//   prepare
			generateMapDirectoryForEachInstance()
		//   make backup
			JbossStructuredPackage.generatePackage()
		}
		catch(Exception e){
			println "[E] Exception occured during prepareDeployPackage process. Abort!!!:\n"+e.getMessage()
			deleteWorkSpace()
			return 1
		}
		
		return 0
	}
	public Integer deployPackage(){
		def cr=0
		try{
			// deploy
			//   prepareDeployPackage
			if(cr==0){ 
				if(prepareDeployPackage()!=0){ cr=1}
			}
			//   deploy ear/war
			if(cr==0){
				if(deployGroupofApplicationsForEachInstance()!=0){ cr=1}
			}
			//   deploy Config
			if(cr==0){
				if(deployConfigDirectoryForEachInstance()!=0){ cr=1}
			}
			//   deploy Lib
			if(cr==0){
				if(deployLibDirectoryForEachInstance()!=0){ cr=1}
			}
			//     => restore backup
			if(cr==0){
				if(JbossStructuredPackage.restorePackage()!=0){ cr=1}
			}
		}
		catch(Exception e){
			
			println "[E] Exception occured during deployPackage process. Abort!!! : "+e.getMessage()
			cr=1
		}
		finally{
			deleteWorkSpace()
			return cr
		}
	}
	public Integer simuleDeployPackage(){
		try{
			// simulate
			prepareDeployPackage()
		}
		catch(Exception e){
			println "[E] Exception occured during simuleDeployPackage process. Abort!!!"
			deleteWorkSpace()
			return 1
		}
    }
	
	// init
	//  create work space
	//  check package structure
	//  test ressource acces
	
	// Prepare deployment
	//   generate config for each instance
	// 	 generate lib for each instance

	
	
	
	
	// delete Workspace
	
}