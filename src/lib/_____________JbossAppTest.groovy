import fr.visionitgroup.groovylib.jboss7.Jboss7Shell;

	static def int waitForStatusOfOneApplication(def Jboss7Shell cli,def String deployName,def String[] deployScope){
		
	}
	

	
	static def int waitForStatusOfGroupOfApplications(def Jboss7Shell cli,def String deployName,def String[] deployScope){
		
	}
	
	static List getAllServersList(def Jboss7Shell cli){
		def List instanceList=[]
		Jboss7CommonTools.getListofObjects(cli,"","host",false).each{ slave->
			instanceList.addAll( Jboss7CommonTools.getListofObjects(cli,slave,"server-config",false))
		}
		println "[I] AllInstance:"+instanceList
		return instanceList
	}
	
	static List getGroupsFromAppli(def Jboss7Shell cli, def String deployName){
		def List groupsList=[]
		Jboss7CommonTools.getListofObjects(cli,"","server-group",false).each{servergroup->
			Jboss7CommonTools.getListofObjects(cli,servergroup,"deployment",false,["name":deployName]).each{
				if(it){
					groupsList.add(servergroup)
				}
			}
		}
		println "[I] AllGroup From $deployName : $groupsList"
		return groupsList
	}
	static List getServersFromGroups(def Jboss7Shell cli, def List groupsList){
		def instanceFromgroup=[]
		def instanceList=[]
		instanceList.addAll( Jboss7CommonTools.getAllServersList(cli))
		instanceList.each{ instance ->
			groupsList.each{ def String groupscope ->
				def String groupshortName=groupscope.replaceAll("^/server-group=","")
				def instGroup=Jboss7CommonTools.getAttributeValue(cli,instance,"group")
				if (instGroup==groupshortName){
					instanceFromgroup.add(instance)
				}
			}
		}
		println "[I] Instances from $groupsList : $instanceFromgroup"
		return instanceFromgroup
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
		// compteur d'etat des applications
		def Integer NbServers=0
		def Integer Started=0
		def Integer Starting=0
		def Integer Stopped=0
		def Integer Stopping=0
		def Integer Failed=0
		// definition liste des instances failed pour avoir un output
		def List FailedInst=[]
		// pour chaque entree de la liste on split on recupere host et server et on recupere le status
		path_instanceLst.each{def String instanceScope ->
			   def String statusInstance=statusDomainInstance(  cli, instanceScope, "","")
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
								FailedInst.add(instance)
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
			println "[I]  running(${Started}/${NbServers})"
			cr=0
		} else {
			if (NbServers==Stopped){
				println "[I]  stopped(${Stopped}/${NbServers})"
				cr=1
			} else {
				if(Started>0 && Started !=NbServers){
				println "[W]  degraded - running(${Started}/${NbServers})"
				cr=2
				} else {
					if(Starting+Stopping==NbServers){
						println "[W]  transitional state - starting(${Starting}/${NbServers}) stopping(${Stopping}/${NbServers})"
						cr=3
					} else {
						if(Failed==NbServers){
							println "[E] s failed(${Failed}/${NbServers})"
							cr=4
						} else {
							println "[W]  state : running(${Started}/${NbServers}) - starting(${Starting}/${NbServers}) - stopping((${Stopping}/${NbServers}) - stopped(${Stopped}/${NbServers}) - failed(${Failed}/${NbServers})"
							cr=5
						}
					}
				}
			}
		}
		if (FailedInst){
			println "[E] There's failed servers : ${FailedInst}"
		}
	//retourne le cr 0(running ou pas d'instances) 1(stopped) 2(degraded) 3(transition) 4(all failed) 5(autre etat)
	return cr
	}
	
	static def int getstatusFromApplication(def Jboss7Shell cli,def String deployName){
		def instanceList=[]
		instanceList.addAll( Jboss7CommonTools.getAllServersList(cli))
		def groupList=[]
		groupList.addAll( Jboss7CommonTools.getGroupsFromAppli(cli,deployName))
		def instanceFromgroup=[]
		instanceFromgroup.addAll( Jboss7CommonTools.getServersFromGroups(cli,groupList))
		Jboss7CommonTools.statusListOfDomainInstance(cli,instanceFromgroup)
		
	
	}
 }
