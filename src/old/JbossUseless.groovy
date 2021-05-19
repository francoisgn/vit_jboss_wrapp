package old

import java.util.List;
import java.util.Map;

import fr.visionitgroup.groovylib.jboss7.*

class Jboss7ApplicationDomainMgrOld {

	// base functions
	static def List getAllServersList(def Jboss7Shell cli){
		def List instanceList=[]
		Jboss7CommonTools.getListofObjects(cli,"","host",false).each{ slave->
			instanceList.addAll( Jboss7CommonTools.getListofObjects(cli,slave,"server-config",false))
		}
		return instanceList
	}	
	static def List getGroupsFromAppli(def Jboss7Shell cli, def String deployName){
		// get servergroup from appli : getGroupFromapplis -> List de servergroup
		// -> filtre inclure uniquement serverGroupfilter
		
		def List groupsList=[]
		Jboss7CommonTools.getListofObjects(cli,"","server-group",false).each{servergroup->
			Jboss7CommonTools.getListofObjects(cli,servergroup,"deployment",false,["name":deployName]).each{
				if(it){
					groupsList.add(servergroup)
				} else {
					println "[E] no group for $deployName "
				}
			}
		}
		return groupsList
	}
	static def List getServersFromGroups(def Jboss7Shell cli, def List groupsList){
		// servergroup -> instance (fullpath) : getInstanceFromgroups -> List D'instance (host/server)
		// Filtre 1) host / 2) slave
		
		def instanceFromgroup=[]
		def instanceList=[]
		instanceList.addAll( getAllServersList(cli))
		if (instanceList){
			instanceList.each{ instance ->
				groupsList.each{ def String groupscope ->
					def String groupshortName=groupscope.replaceAll("^/server-group=","")
					def instGroup=Jboss7CommonTools.getAttributeValue(cli,instance,"group")
					if (instGroup==groupshortName){
						instanceFromgroup.add(instance)
					}
				}
			}
		} else {
		println "[E] no servers found"
		}
		return instanceFromgroup
	}
	static def List getServersFromApplication(def Jboss7Shell cli, def String deployName){
		def GroupsFromApp=Jboss7ApplicationDomainMgrOld.getGroupsFromAppli(cli,deployName)
		def ServersFromGroup=Jboss7ApplicationDomainMgrOld.getServersFromGroups(cli,GroupsFromApp)
			return ServersFromGroup
	}
	// useless
	
	static def String getStatusDomainInstance(def Jboss7Shell cli, def String path_instance){
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
//			def statusInstance=result.getResponse().get("result").asString()
//			return statusInstance
			return result.getResponse().get("result").asString()
		}else{
			println "[E] unable to get status from $path_instance"
			return null
		}
  }	
	static def String statusOfServersList(def Jboss7Shell cli, def List  path_instanceLst){
		// compteur d'etat des applications
		def Integer NbServers=0
		def Integer Started=0
		def Integer Starting=0
		def Integer Stopped=0
		def Integer Stopping=0
		def Integer Failed=0
		def String returnString=""
		// definition liste des instances failed pour avoir un output
		def List FailedInst=[]
		// pour chaque entree de la liste on split on recupere host et server et on recupere le status
		path_instanceLst.each{def String instanceScope ->
			NbServers=NbServers+1
		   def statusInstance=getStatusDomainInstance(  cli, instanceScope)
		   if(statusInstance=="STARTED") {
			   Started=Started+1
		   } else {
			   if(statusInstance=="STARTING") {
				   Starting=Starting+1
			   } else {
				   if(statusInstance=="STOPPED") {
					   Stopped=Stopped+1
				   } else {
					   if(statusInstance=="STOPPING") {
						   Stopping=Stopping+1
						} else {
							if(statusInstance=="FAILED") {
								Failed=Failed+1
								FailedInst.add(instanceScope)
							} else {
								println "[E] unable to retrieve status of server $instanceScope"
							}
						}
				   }
			   }
		   }					
		}
		// case detat : RUNNING / STOPPED / DEGRADED / TRANSITION / FAILED
		if (NbServers==Started){
			returnString="[I]  running(${Started}/${NbServers})"
		} else {
			if (NbServers==Stopped){
				returnString="[I]  stopped(${Stopped}/${NbServers})"
			} else {
				if(Started>0 && Started !=NbServers){
				returnString="[W]  degraded - running(${Started}/${NbServers})"
				} else {
					if(Starting+Stopping==NbServers){
						returnString="[W]  transitional state - starting(${Starting}/${NbServers}) stopping(${Stopping}/${NbServers})"
					} else {
						if(Failed==NbServers){
							returnString="[E] failed(${Failed}/${NbServers})"
						} else {
							returnString="[W]  state : running(${Started}/${NbServers}) - starting(${Starting}/${NbServers}) - stopping((${Stopping}/${NbServers}) - stopped(${Stopped}/${NbServers}) - failed(${Failed}/${NbServers})"
						}
					}
				}
			}
		}
		if (FailedInst){
			println "[E] There's failed servers : ${FailedInst}"
		}
	//retourne le cr 0(running ou pas d'instances) 1(stopped) 2(degraded) 3(transition) 4(all failed) 5(autre etat)
		println returnString
		return returnString
	}

	static def List getServersFromApplicationWithFiltre(def Jboss7Shell cli, def String deployName){
		def GroupsFromApp=Jboss7ApplicationDomainMgrOld.getGroupsFromAppli(cli,deployName)
		def ServersFromGroup=Jboss7ApplicationDomainMgrOld.getServersFromGroups(cli,GroupsFromApp)
			return ServersFromGroup
	}
	static def String statusOfInstanceOfApplication(def Jboss7Shell cli,def String deployName){
		def groupList=[]
		if ( JbossApplicationMgr.isApplicationExist(cli,deployName)==true){
			groupList.addAll( getGroupsFromAppli(cli,deployName))
			def instanceFromgroup=[]
			instanceFromgroup.addAll( getServersFromGroups(cli,groupList))
			statusOfServersList(cli,instanceFromgroup)
		} else {
		println "[E] application $deployName doesnt exist"
		}
	}
	static def String statusOfInstanceOfApplicationsList(def Jboss7Shell cli,def String[] deployNameList){
		def String returnString=""
		deployNameList.each{ deployName ->
			println "[I] -- Checking Status of "+deployName
			def String resultString=statusOfInstanceOfApplication(cli,deployName)
			if(resultString!=null){
				returnString="[I] "+deployName+" status message : "+resultString
			} else {
				returnString="[E] "+deployName+" have status : "+resultString
			}
		}
		println returnString
		return returnString
	}
	static def String getStatusOfApplicationOnOneServer(def Jboss7Shell cli,def String deployName, def String Object){
		// server = full path (ex. /host=slave2/server-config=server2)
		def String Server=Object.replaceAll("/server-config=","/server=")
		//mise en place regex pour passer de server-config à server=
		// is server exist
		// attention if not exist stacktrace
		def String cmdLine="${Server}:read-attribute(name=launch-type)"
		def result=cli.runCLICommand(cmdLine)
		if (result.isSuccess()){
			def returnstring=result.getResponse().get("result").asString()
			if (returnstring!="DOMAIN"){
				return "[E] ${Server} isn't in domain mode"
			}
		} else {
			def String errorString=result.getResponse().get("failure-description").asString()
			return "[E] unable to read ${Server} attributes : ${errorString}"
		}
		// is app exist
		if( JbossApplicationMgr.isApplicationExist(cli,deployName) != true) {
			return "[E] ${deployName} doesnt exist"
		}
			
		//getStatus
		/** Status can be
		 OK
		 STOPPED
		 FAILED
		 no metrics available //???
		 */
		cmdLine="${Server}/deployment=${deployName}:read-attribute(name=status)"
		result=cli.runCLICommand(cmdLine)
		if (result.isSuccess()){
			return result.getResponse().get("result").asString()
		} else {
			def String errorstring=result.getResponse().get("failure-description").asString()
			return "[E] unsuccessful execution : ${errorstring}"
		}
	}
	static def List statusOfApplication(def Jboss7Shell cli,def String deployName){
		def List AppServers=Jboss7ApplicationDomainMgrOld.getServersFromApplication(cli,deployName)
		AppServers.each{
			//check syntax of iteration in AppServers
			def String Server=it.replaceAll("/server-config=","/server=")
			println getStatusOfApplicationOnOneServer(cli,deployName,Server)
			//mettre en place le calcul pour etat global
			/** each Status can be
			 OK
			 STOPPED
			 FAILED
			 no metrics available //???
			 */
		}
	}
	static def String doOnInstance(def Jboss7Shell cli,def String Server,def String action){
		// action = stop - start - restart
		if (action !="start" && action !="stop" && action !="restart"){
			println "[E] action doesnt match intended pattern"
			println "[I] possible action : stop - start - restart"
			return "[E] NO ACTION PERFORMED"
		}
		// check syntax of server
		if (Server == "unattended string"){
			println "[E] Server doesnt match intended pattern"
			println "[I] Server pattern exemple : /host=slave1/server-config=server11"
			return "[E] NO ACTION PERFORMED"
		}
		def cmdLine="${Server}:${action}"
		def result=cli.runCLICommand(cmdLine)
		if (result.isSuccess()){
			 def CmdOut=result.getResponse().get("result").asString()
			 return "${Server}:"+CmdOut
		} else {
			def String errorstring=result.getResponse().get("failure-description").asString()
			return "[E] unsuccessful execution : ${errorstring}"
		}
	}
	static def String[] doOnInstanceOfApp(def Jboss7Shell cli,def String deployName,def String action){
		def resultList=[]
		def List ServerFromAppli=getServersFromApplication(cli,deployName)
		ServerFromAppli.each{ Server ->
			resultList.add( doOnInstance(cli,Server,action))
		}
		return resultList
	}
	static def String[] doOnInstanceOfAppList(def Jboss7Shell cli,def String[] deployNameList,def String action){
		def resultList=[]
		deployNameList.each{ deployName ->
			resultList.add( doOnInstanceOfApp(cli,deployName,action))

		}
		return resultList
	}
	static def String[] doOnInstanceOfServergroups(def Jboss7Shell cli,def List groupsList,def String action){
		def resultList=[]
		def List ServerFromGroups=getServersFromGroups(cli,groupsList)
		ServerFromGroups.each{ Server ->
			resultList.add( doOnInstance(cli,Server,action))
		}
		return resultList
	}
	static def List getServersFromScope (){}

	
	// ok
	static public def int startAllInstancesOfDomaineApplication(def String appliname,def List<String> serverGroupsFilter=[],def String slave_Regexpfilter="", def String instanceNameRegexpfilter=""){

		
		
	}
	//ex . doOneObject(cli,/host=slave2/server-config=server11,restart)
	static def int doOnObject(def Jboss7Shell cli,def List<String> path_objectList,def String action,def long timeout=0,def Boolean failOnError=true){
		def cr=0
		// object can be server-group or server-config (full path)
		def List InstanceList=[]
		//check if ressource exist
		path_objectList.each{ path_object ->
			if(!Jboss7CommonTools.isExistResource(cli,path_object)){
				if (failOnError){
					println "[E] instance $path_object doesn't exist"
					return 1
				} else {
					println "[W] instance $path_object doesn't exist"
				}
			}
			else {
				//if path is servergroup then get the list of instance from the group
				if (path_object.matches("^/server-group=.*")){
					InstanceList.addAll( getServersFromGroups(cli,["$path_object"]))
				} else {
					if(path_object.matches("^/host=.*/server-config=.*")){
						InstanceList.add(path_object)
					} else {
	
						if (failOnError){
							println "[E] $path_object doesn't match attended pattern (must be servergroup or instance scope - ex : /server-group=<groupname> or /host=slaveName/server-config=InstanceName"
							return 1
						} else {
							println "[W] $path_object doesn't match attended pattern (must be servergroup or instance scope - ex : /server-group=<groupname> or /host=slaveName/server-config=InstanceName"
						}
					}
				}
			}
		}
		InstanceList=InstanceList.unique()
		switch(action){
			case "arestart" : 
								Jboss7InstanceDomainMgr.arestartListOfInstances(cli,timeout,InstanceList,failOnError);break;
								//return int (0:ok/1:failed/2:warn)
			case "astart" :
								Jboss7InstanceDomainMgr.astartListOfInstances(cli,InstanceList,failOnError);break;
								//return int (0:ok/1:failed/2:warn)
			case "astop" :
								Jboss7InstanceDomainMgr.astopListOfInstances(cli,InstanceList,failOnError);break;
								//return int (0:ok/1:failed/2:warn)
			case "restart" :
								Jboss7InstanceDomainMgr.restartListOfInstances(cli,timeout,"OneByOne",InstanceList,failOnError);break;
								//return int (0:ok/1:failed/2:warn)
			case "start" :
								Jboss7InstanceDomainMgr.startListOfInstances(cli,timeout,InstanceList,failOnError);break;
								//return int (0:ok/1:failed/2:warn)
			case "stop" : 
								Jboss7InstanceDomainMgr.stopListOfInstances(cli,timeout,InstanceList,failOnError);break;
								//return int (0:ok/1:failed/2:warn)
			case "status" :
								Jboss7InstanceDomainMgr.statusListOfInstances(cli,InstanceList);break;
								//return int (0:status/1:unknown) but print current status
			default : println "[E] action case aint take into account";return "failed";break;

		}
	}
	static def String doOnInstanceOfApplications(def Jboss7Shell cli,def String[] deployNameList,def String action,def long timeout=0,def Boolean failOnError=true){
		def List groupList=[]
		// recupérer la liste des groups pour chaque deployName
		deployNameList.each{ deployName ->
			groupList.addAll( getGroupsFromAppli(cli,deployName))
		}
		// doOnObject sur les groupes récupéré
		doOnObject(cli,groupList,action,timeout,failOnError)
	}
	// To Be Done
	static def List getApplicationsListFromProjectName(){}
}	
	
	
	
/*****
 * --------------------------- Input
 * --------------------------- Connect
 * --------------------------- Test connection
 * --------------------------- Call method
 *  --------------------------- Disconnect
 */
def controllerHost="10.10.10.1"
def int controllerPort=9999
def String userMgmt="admin"
def char[] passwordMgmt="azerty01;"


def String appli="GroupName-SubAppliName1-v1.1.war"
def String[] appliList=[ "SubAppliName1-v1.1.war","SubAppliName1-v1.2.war", "GroupName-SubAppliName1-v1.1.war", "GroupName-SubAppliName1-v1.2.war" ]
def String Server="/host=slave2/server-config=server21"
def String Server2="/host=slave2/server-config=server21"

def Jboss7Shell js = new Jboss7Shell(userMgmt,passwordMgmt,controllerHost,controllerPort)
js.connect()
js.testConnexion()


Jboss7ApplicationDomainMgrOld.doOnInstanceOfApplications(js,appliList,"status")

/**
println "------------------getAllServersList"
println Jboss7ApplicationDomainMgrOld.getAllServersList(js)
println "------------------getGroupsFromAppli"
println Jboss7ApplicationDomainMgrOld.getGroupsFromAppli(js,appli)
println "------------------getServersFromGroups"
def groupfromapplilist=Jboss7ApplicationDomainMgrOld.getGroupsFromAppli(js,appli)
println Jboss7ApplicationDomainMgrOld.getServersFromGroups(js,groupfromapplilist)
println "------------------getStatusDomainInstance"
println Jboss7ApplicationDomainMgrOld.getStatusDomainInstance(js,"/host=slave1/server-config=server1")
println "------------------statusOfInstanceOfApplication"
Jboss7ApplicationDomainMgrOld.statusOfInstanceOfApplication(js,appli)
println "------------------statusOfInstanceOfApplicationsList"
Jboss7ApplicationDomainMgrOld.statusOfInstanceOfApplicationsList(js,appliList)
println "------------------getStatusOfApplicationOnOneServer"
println Jboss7ApplicationDomainMgrOld.getStatusOfApplicationOnOneServer(js,appli,Server)
println "------------------getServersFromApplication"
println Jboss7ApplicationDomainMgrOld.getServersFromApplication(js,appli)
println "------------------statusOfApplication"
Jboss7ApplicationDomainMgrOld.statusOfApplication(js,appli)
//println "------------------doOnInstance"
//Jboss7ApplicationDomainMgrOld.doOnInstance(js,Server2,"start")
//println "------------------doOnInstanceOfApp"
//println Jboss7ApplicationDomainMgrOld.doOnInstanceOfApp(js,appli,"stop")
println "------------------doOnInstanceOfAppList"
Jboss7ApplicationDomainMgrOld.doOnInstanceOfAppList(js, appliList, "stop")
**/





js.disconnect()
System.exit(0)

