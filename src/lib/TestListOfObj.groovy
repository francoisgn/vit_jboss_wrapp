
import java.util.List;
import java.util.Map;

import fr.visionitgroup.groovylib.jboss7.*

class Jboss7ApplicationDomainMgr {

	// base functions
	static def List getAllServersList(def Jboss7Shell cli){
		def List instanceList=[]
		Jboss7CommonTools.getListofObjects(cli,"","host",false).each{ slave->
			instanceList.addAll( Jboss7CommonTools.getListofObjects(cli,slave,"server-config",false))
		}
		return instanceList
	}	
	static def List getGroupsFromAppli(def Jboss7Shell cli, def String deployName,def List<String> serverGroupsFilter=[]){
		def List groupsList=[]
		// get all groups
		Jboss7CommonTools.getListofObjects(cli,"","server-group",false).each{def String servergroup->
			// check all group if it has deployment
			Jboss7CommonTools.getListofObjects(cli,servergroup,"deployment",false,["name":deployName]).each{ list ->
				// if theres a list of group with deployment
				if(list){
					//if filter exist
					if(serverGroupsFilter){
						serverGroupsFilter.each{ filter ->
								//check if servergroup correspond with filter then add
								def String groupshortName=servergroup.replaceAll("^/server-group=","")
								if (filter==groupshortName){
									groupsList.add(servergroup)
								}
							}
					} else { 
						// if there no filter add servergroup to list
						groupsList.add(servergroup)
					}
				} else {
					println "[E] no group for $deployName "
				}
			}
		}
		return groupsList
	}
	static def List getServersFromGroups(def Jboss7Shell cli, def List groupsList, def String slave_Regexpfilter="", def String instanceNameRegexpfilter=""){
		// servergroup -> instance (fullpath) : getInstanceFromgroups -> List D'instance (host/server)
		// Filtre 1) host / 2) slave
		def instanceFromgroup=[]
		def FilteredInst=[]
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
		
		if (slave_Regexpfilter!="" && slave_Regexpfilter!=null){
			instanceFromgroup.each{ def String instance ->
				def String instancehost=instance.split("/")[1].replaceAll("^/host=","")
				if (slave_Regexpfilter==instancehost){
					FilteredInst.add(instance)
				}
			}
		} else {
			if (instanceNameRegexpfilter!="" && instanceNameRegexpfilter!=null){
				instanceFromgroup.each{ def String instance ->
					def String instanceshortname=instance.split("/")[2].replaceAll("^/server-config=","")
					if (instanceNameRegexpfilter==instanceshortname){
						FilteredInst.add(instance)
					}
				}
			} else {
			FilteredInst.addAll(instanceFromgroup)
			}
		}
		return FilteredInst
	}
	static def List getServersFromApplication(def Jboss7Shell cli, def String deployName,def List<String> serverGroupsFilter=[],def String slave_Regexpfilter="", def String instanceNameRegexpfilter=""){
		def GroupsFromApp=Jboss7ApplicationDomainMgr.getGroupsFromAppli(cli,deployName,serverGroupsFilter)
		def ServersFromGroup=Jboss7ApplicationDomainMgr.getServersFromGroups(cli,GroupsFromApp,slave_Regexpfilter,instanceNameRegexpfilter)
			return ServersFromGroup
	}


	
	// ok
	static def int doOnObject(def Jboss7Shell cli,def List<String> path_objectList,def String action,def String slave_Regexpfilter="", def String instanceNameRegexpfilter="",def long timeout=0,def Boolean failOnError=true){
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
					InstanceList.addAll( getServersFromGroups(cli,["$path_object"],slave_Regexpfilter,instanceNameRegexpfilter))
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
	static def String doOnInstanceOfApplications(def Jboss7Shell cli,def String[] deployNameList,def String action,def long timeout=0,def List<String> serverGroupsFilter=[],def String slave_Regexpfilter="", def String instanceNameRegexpfilter="",def Boolean failOnError=false){
		def List groupList=[]
		// recupérer la liste des groups pour chaque deployName
		deployNameList.each{ deployName ->
			groupList.addAll( getGroupsFromAppli(cli,deployName,serverGroupsFilter))
		}
		// doOnObject sur les groupes récupéré
		doOnObject(cli,groupList,action,slave_Regexpfilter,instanceNameRegexpfilter,timeout,failOnError,)
	}
	// To Be Done
	static def List getApplicationsListFromProjectName(){}
	static def List pidOfObjectList(def Jboss7Shell cli,def List<String> path_objectList,def String action,def String slave_Regexpfilter="", def String instanceNameRegexpfilter="",def long timeout=0,def Boolean failOnError=true){
		def pidList=[]
		// object can be server-group or server-config (full path)
		def List InstanceList=[]
		//check if ressource exist
		path_objectList.each{ path_object ->
			if(!Jboss7CommonTools.isExistResource(cli,path_object)){
				if (failOnError){
					println "[E] instance $path_object doesn't exist"
					return pidList
				} else {
					println "[W] instance $path_object doesn't exist"
				}
			}
			else {
				//if path is servergroup then get the list of instance from the group
				if (path_object.matches("^/server-group=.*")){
					InstanceList.addAll( getServersFromGroups(cli,["$path_object"],slave_Regexpfilter,instanceNameRegexpfilter))
				} else {
					if(path_object.matches("^/host=.*/server-config=.*")){
						InstanceList.add(path_object)
					} else {
	
						if (failOnError){
							println "[E] $path_object doesn't match attended pattern (must be servergroup or instance scope - ex : /server-group=<groupname> or /host=slaveName/server-config=InstanceName"
							return pidList
						} else {
							println "[W] $path_object doesn't match attended pattern (must be servergroup or instance scope - ex : /server-group=<groupname> or /host=slaveName/server-config=InstanceName"
						}
					}
				}
			}
		}
		InstanceList=InstanceList.unique()
		InstanceList.each{
				pidList.addAll(it+":"+Jboss7InstanceDomainMgr.getPidInstance(cli,it))
		}
		return pidList
	}
	static def int deployActionOnObject(def Jboss7Shell cli,def List<String> path_objectList,def String action,def String slave_Regexpfilter="", def String instanceNameRegexpfilter="",def long timeout=0,def Boolean failOnError=true){
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
					InstanceList.addAll( getServersFromGroups(cli,["$path_object"],slave_Regexpfilter,instanceNameRegexpfilter))
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
			case "deploy" :
								Jboss7InstanceDomainMgr.arestartListOfInstances(cli,timeout,InstanceList,failOnError);break;
								//return int (0:ok/1:failed/2:warn)
			case "undeploy" :
								Jboss7InstanceDomainMgr.astartListOfInstances(cli,InstanceList,failOnError);break;
								//return int (0:ok/1:failed/2:warn)
			case "remove" :
								Jboss7InstanceDomainMgr.astopListOfInstances(cli,InstanceList,failOnError);break;
								//return int (0:ok/1:failed/2:warn)
			case "redeploy" :
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


Jboss7ApplicationDomainMgr.doOnInstanceOfApplications(js,appliList,"restart")


/**
println "------------------getAllServersList"
println Jboss7ApplicationDomainMgr.getAllServersList(js)
println "------------------getGroupsFromAppli"
println Jboss7ApplicationDomainMgr.getGroupsFromAppli(js,appli)
println "------------------getServersFromGroups"
def groupfromapplilist=Jboss7ApplicationDomainMgr.getGroupsFromAppli(js,appli)
println Jboss7ApplicationDomainMgr.getServersFromGroups(js,groupfromapplilist)
println "------------------getStatusDomainInstance"
println Jboss7ApplicationDomainMgr.getStatusDomainInstance(js,"/host=slave1/server-config=server1")
println "------------------statusOfInstanceOfApplication"
Jboss7ApplicationDomainMgr.statusOfInstanceOfApplication(js,appli)
println "------------------statusOfInstanceOfApplicationsList"
Jboss7ApplicationDomainMgr.statusOfInstanceOfApplicationsList(js,appliList)
println "------------------getStatusOfApplicationOnOneServer"
println Jboss7ApplicationDomainMgr.getStatusOfApplicationOnOneServer(js,appli,Server)
println "------------------getServersFromApplication"
println Jboss7ApplicationDomainMgr.getServersFromApplication(js,appli)
println "------------------statusOfApplication"
Jboss7ApplicationDomainMgr.statusOfApplication(js,appli)
//println "------------------doOnInstance"
//Jboss7ApplicationDomainMgr.doOnInstance(js,Server2,"start")
//println "------------------doOnInstanceOfApp"
//println Jboss7ApplicationDomainMgr.doOnInstanceOfApp(js,appli,"stop")
println "------------------doOnInstanceOfAppList"
Jboss7ApplicationDomainMgr.doOnInstanceOfAppList(js, appliList, "stop")
**/





js.disconnect()
System.exit(0)

