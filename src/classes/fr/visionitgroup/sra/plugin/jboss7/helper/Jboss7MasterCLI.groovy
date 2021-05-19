package fr.visionitgroup.sra.plugin.jboss7.helper;

/****
 

@deleteResourcesByName=profile,scope,type,name1,...,nameN
@deleteResourcesByGroup=profile,scope,type,groupname
@deleteResourcesByGroup=full,/subsystem=datasources,jdbc-driver,MyGroupDatasource
@deleteResourcesByRegExp=profile,scope,type,regexp
@deleteResourcesByRegExp=full,/subsystem=datasources,jdbc-driver,mysql-.*

@addResource=profile,scope,type,name,mandatory_attr1=value,...,mandatory_attrN=value,optional_attr1=value,...,optional_attrM=value

@writeResourceAttribute=profile,scope,type,name,attr-name,attr-value
@undefineResourceAttribute=profile,scope,type,name,attr-name,attr-value

@enableResourcesByName=profile,scope,type,name1,...,nameN
@enableResourcesByGroup=profile,scope,type,groupname
@enableResourcesRegExp=profile,scope,type,regexp
@disableResourcesByName=profile,scope,type,name1,...,nameN
@disableResourcesByGroup=profile,scope,type,groupname
@disableResourcesRegExp=profile,scope,type,regexp

@IfOneOrMoreExistResourcesByName=profile,scope,type,name1,...,nameN
@IfOneOrMoreExistResourcesByGroup=profile,scope,type,groupname
@IfAllExistResourcesByName=profile,scope,type,name1,...,nameN
@IfAllExistResourcesByGroup=profile,scope,type,groupname
@IfExistParentResource=profile,scope,type,name

@echo=messages
@native=jboss-cli_native_cmdline

# comment

/profile=full/subsystem=datasources/jdbc-driver=mysql2:add(driver-name="mysql",driver-module-name="com.mysql")
/profile=full/subsystem=datasources/data-source=cluster1_DS:add( connection-url="jdbc:oracle:thin:@databaseserver:port:database",jndi-name="java:jboss/cluster1_DS",driver-name="oracle",user-name="cluster1_appl",password="password",use-java-context="true",min-pool-size="0",max-pool-size="64",idle-timeout-minutes="30",background-validation="false",background-validation-millis="1",validate-on-match="true",allocation-retry="0",share-prepared-statements="false",set-tx-query-timeout="false",query-timeout="0",use-try-lock="0",url-delimiter="|")
/profile=full/subsystem=messaging/hornetq-server=default/:write-attribute(name=persistence-enabled,value=false)
/profile=full/subsystem=messaging/hornetq-server=default/:undefine-attribute(name=journal-file-size)
connection-factory add|read-resource|remove --profile=full --name=aaa --entries=  [--PROP=VALUE]
data-source add|disable|enable|read-resource|remove  --profile=full --name=aaa --connection-url --jndi-name --driver-name  [--PROP=VALUE]
xa-data-source add|disable|enable|read-resource|remove  --profile=full --name=aaa  --jndi-name --driver-name [--PROP=VALUE]
jms-queue add|read-resource|remove --profile=full --queue-address=ddd --entries=
jms-topic add|count-messages-for-subscription|drop-all-subscriptions|drop-durable-subscription|list-all-subscriptions|list-all-subscriptions-as-json|list-durable-subscriptions|list-durable-subscriptions-as-json|list-messages-for-subscription|list-messages-for-subscription-as-json|list-non-durable-subscriptions|list-non-durable-subscriptions-as-json|read-resource|remove|remove-messages  --topic-address= --entries= --profile=full

****/

import groovy.lang.Closure;

import org.jboss.as.cli.scriptsupport.*

public class Jboss7MasterCLI {
	
	def List  scriptLinesArray=[]	
	def Jboss7CLIConnection cli=null
	
	public Jboss7MasterCLI(def Jboss7CLIConnection cli,def String scriptLines) {
		this.cli=cli
		//this.script=script
		scriptLines.eachLine{
			scriptLinesArray.add(it)
		}
		

	}
	
	public int checkMasterScript(){
		return 0
	}
	
	private int checkSyntaxeOfMasterScript(){
		def int cptline=0
		try {
			scriptLinesArray.each { line ->
				cptline++
				def String action=""
				if(line ==~ / *#.*/ ){
					// Ignored 
				}
				else if(line ==~ / *@deleteResourcesByName=.*/ ){
					action="@deleteResourcesByName"
				}
				else if(line ==~ / *\/.*:.*(.*) */ ){
					
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
	
	
	public int simulateMasterScript(){
		return 0
	}
	private String getAction(def String line){
		return line.replaceAll("=.*\$","")
	}
	private String getProfile(def String line){
		return line.replaceAll("^.*=","").split(",")[0]
	}
	private String getScope(def String line){
		return line.replaceAll("^.*=","").split(",")[1]
	}
	private String getType(def String line){
		return line.replaceAll("^.*=","").split(",")[2]
	}
	private String getName(def String line){
		return line.replaceAll("^.*=","").split(",")[3]
	}
	private String getGroupName(def String line){
		return line.replaceAll("^.*=","").split(",")[3]
	}
	private String getRegExp(def String line){
		return line.replaceAll("^.*=","").split(",")[3]
	}
	private String getAttributeName(def String line){
		return line.replaceAll("^.*=","").split(",")[4]
	}
	private String getAttributeValue(def String line){
		return line.replaceAll("^.*=","").split(",")[5]
	}
//@enableResourcesByName=profile,scope,type,name1,...,nameN
	
	private List getNamesList(def String line){
		return line.replaceAll("^.*=[^,]*,[^,]*,[^,]*,","").split(",")
	}
	
	private Map parserLine(def String line){
		def Map m=[:]
		def String action=""
		//m["action"]
		action=getAction(line)
		if(action=="@deleteResourcesByName"){
			m["action"]="deleteResourcesByName";
			m["profile"]=getProfile(line)
			m["scope"]=getScope(line)
			m["type"]=getType(line)
			m["names-list"]=getNamesList(line)
		}
		if(action=="@deleteResourcesByGroup"){m["action"]="deleteResourcesByGroup";}
		if(action=="@deleteResourcesByRegExp"){m["action"]="deleteResourcesByRegExp";}
		if(action=="@addResource"){m["action"]="addResource";}
		if(action=="@writeResourceAttribute"){m["action"]="writeResourceAttribute";}
		if(action=="@undefineResourceAttribute"){m["action"]="undefineResourceAttribute";}
		if(action=="@enableResourcesByName"){m["action"]="enableResourcesByName";}
		if(action=="@enableResourcesByGroup"){m["action"]="enableResourcesByGroup";}
		if(action=="@enableResourcesRegExp"){m["action"]="enableResourcesRegExp";}
		if(action=="@disableResourcesByName"){m["action"]="disableResourcesByName";}
		if(action=="@disableResourcesByGroup"){m["action"]="disableResourcesByGroup";}
		if(action=="@disableResourcesRegExp"){m["action"]="disableResourcesRegExp";}
		if(action=="@IfOneOrMoreExistResourcesByName"){m["action"]="IfOneOrMoreExistResourcesByName";}
		if(action=="@IfOneOrMoreExistResourcesByGroup"){m["action"]="IfOneOrMoreExistResourcesByGroup";}
		if(action=="@IfAllExistResourcesByName"){m["action"]="IfAllExistResourcesByName";}
		if(action=="@IfAllExistResourcesByGroup"){m["action"]="IfAllExistResourcesByGroup";}
		if(action=="@IfExistParentResource"){m["action"]="IfExistParentResource";}
		if(action=="@echo"){m["action"]="echo";}
		if(action=="@native"){m["action"]="native";}
		
		return m
		
	}
	public int executeMasterScript(){
		def int cptline=0
		try {
			scriptLinesArray.each { line ->
				cptline++
				def String action=""
				def String profile=""
				def String scope=""
				def String type=""
				def String groupname=""
				def String name=""
				
				if(line ==~ / *#.*/ ){
					// comment Ignored 
				}
				else if(line ==~ / */ ){
				// empty line Ignored
				}
				else {
					action=getAction(line)
					println "action=$action"
					println parserLine(line)
					profile=""     
					scope=""       
					type=""        
					groupname=""   
					name=""        

				}
				
			}
		}
		catch (Exception e){
			println(e.getMessage())
			return 1
		}
		return 0
	}
	
	
}
	

