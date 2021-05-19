package fr.visionitgroup.sra.plugin.jboss7.helper;


import java.io.File;
import java.util.List;

import groovy.lang.Closure;

import org.jboss.as.cli.scriptsupport.*

public class Test {
	
	def String CST_JBOSSDEPLOYMENT_PROPERTIES="jboss-deployment.properties"
	def String CST_JBOSSDEPLOYMENT_ENV_CLI="jboss-deployment-env.cli"
	def Jboss7CLIConnection cli
	
	def File workdir=null
	def String backupBaseName="bck"
	def File backupWorkdir=null
	def String templateEngineBaseName="TE"
	def File templateEngineWorkdir=null
	
	def String groupName
	def List applicationList=[]
	def List applicationStructuredPropertiesLst=[]
	def File jbossSocleEnvFile=null
	def String deployScope
	
	
	def List externalConfigList=[]
	def List externalLibList=[]
	
	
	def File basedir=null
	def File groupeBaseDir=null
	def String appBaseName="app"
	def File appBasedir=null
	def String externalConfigBaseName="external-config"
	def File externalConfigBasedir=null
	def String externalLibBaseName="external-lib"
	def File externalLibBasedir=null
	
	def String deployMode="Full" // full | incre
	
	public Test(Jboss7CLIConnection cli,def File basedir,def File jbossSocleEnvFile=null,def File workdir){
		if (workdir==null || (workdir!=null && !workdir.isDirectory() )){
			println("[E] JbossStructuredPackage wordir($workdir) can't be null and must be a directory")
		}
		
		if (basedir==null || (basedir!=null && !basedir.isDirectory() )){
			println("[E] JbossStructuredPackage basedir of package(checkStructureOfPackage) can't be null and mnust be a directory")
		}
		
		
		this.cli = cli
		this.jbossSocleEnvFile = jbossSocleEnvFile
		this.workdir=workdir
		
		this.basedir=basedir
		if (this.basedir.list().size() != 1 ){
			throw new Exception("[E] JbossStructuredPackage structure of package(checkStructureOfPackage) isn't wrong: The directory $basedir must only contain one directory taht the name is equal to the group-name of the package ")
		}
		this.basedir.eachDir {
			this.groupName=it
			println "[I] JbossStructuredPackage Auto Detect Group Name: $groupName"
		}
		
		this.groupeBaseDir=new File(basedir,groupName)
		this.appBasedir=new File(groupeBaseDir,appBaseName)
		this.externalConfigBasedir=new File(groupeBaseDir,appBaseName)
		this.externalLibBasedir=new File(groupeBaseDir,appBaseName)
		
		this.backupWorkdir=new File(workdir,backupBaseName)
		this.templateEngineWorkdir=new File(workdir,templateEngineBaseName)
		
		this.groupName="dir"
		
		
		
	}

}
	

