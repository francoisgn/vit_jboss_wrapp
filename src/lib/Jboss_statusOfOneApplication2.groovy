package lib
//------- import
import fr.visionitgroup.groovylib.jboss7.*

import org.jboss.as.cli.scriptsupport.*

import java.util.List;
import java.util.regex.Pattern

import org.jboss.dmr.ModelNode;
//------- import






static def Integer statusOfOneApplication(def Jboss7Shell js,def String deployName){
	//recupere la liste des groupes ou l'application a été déployé et remonte le status de l'application
	// definition $cr code retour de la method
	def Integer cr=0
	// definition des listes au plus haut pour pouvoir rappeler les listes si besoin
	def GlobalGroupList=[]
	def GlobalHostList=[]
	def GlobalSrvList=[]
	def AppGroupList=[]
	def AppInstList=[]
	// liste les server-group
	def command="/:read-children-resources(child-type=server-group)"
	def resultCmd=js.runCLICommand(command)
	if (resultCmd.isSuccess()){
		resultCmd.getResponse().get("result").keys().each{
			GlobalGroupList.add(it)
		}
		// liste des deployment présent dans chaque server-group
		GlobalGroupList.each{ group ->
			command="/server-group="+group+":read-children-resources(child-type=deployment)"
			resultCmd=js.runCLICommand(command)
			if (resultCmd.isSuccess()){
				resultCmd.getResponse().get("result").keys().each{ groupapp ->
					//si un deployment match avec le deployName en input alors le server-group est ajouté a la liste
					if(groupapp==deployName){
						AppGroupList.add(group)
					}
				}
			}else{
			AppGroupList=[]
			println "[E] unable to go into group subtree - command : $command"
			}
		}
	}else{
		GlobalGroupList=[]
		println "[E] unable to get group list - command : $command"
		
	}
	if (AppGroupList){
		println "[I] group for application $deployName : $AppGroupList"
		// liste les host du domain
		command="/:read-children-resources(child-type=host)"
		resultCmd=js.runCLICommand(command)
		if (resultCmd.isSuccess()){
			resultCmd.getResponse().get("result").keys().each{
				GlobalHostList.add(it)
			}
		} else {
			println "[E] unable to execute : $command"
		}
		GlobalHostList.each{ host ->
			// WARN verifier si le domain-controler sappelle toujours master - sinon trouvé un tricks pour le filtrer
			if(host!="master"){
				// liste des server-config pour chaque host du domain
				command="/host="+host+":read-children-resources(child-type=server-config)"
				resultCmd=js.runCLICommand(command)
				if (resultCmd.isSuccess()){
					resultCmd.getResponse().get("result").keys().each{inst ->
						// liste les server-group de chaque server-config (inst) pour chaque host
						command="/host="+host+"/server-config="+inst+":read-attribute(name=group)"
						resultCmd=js.runCLICommand(command)
						if (resultCmd.isSuccess()){
							// test de correspondance entre les groupes du server et les groupes de l'application
							def String s=resultCmd.getResponse().get("result").asString()
							// si le server appartient a plusieur server-group alors la string de resultat contient une virgule - on split et on traite chaque cas
							// WARN pas certain du fonctionnement du split
							if(s.indexOf(",")!=-1){
								def InstGroup=s.split(",")
								InstGroup.each{
									AppGroupList.each{
										def String AppGroup=it
										if(AppGroup==InstGroup){
											def AppInst="$host:$inst"
											AppInstList.add(AppInst)
										}
									}
								}
							} else {
								// si le server appartient a un seul groupe alors il n'y a pas de virgule dans la string de resultat
								def InstGroup=s
								AppGroupList.each{
									def String AppGroup=it
									if(AppGroup==InstGroup){
										def AppInst="$host:$inst"
										AppInstList.add(AppInst)
									}
								}
							}
						} else {
							println "[E] unable to retrieve name group for server ${host}/${inst} - command : $command"
						}
					}
				} else {
					println "[E] unable to read server-config child from ${host} - command : $command"
				}
			}
		}
	} else {
		println "[E] no server-group for application : $deployName"
	}
	// si la liste des instances d'une app n'est pas vide on recupere le status des instances
	// opération sur des Integer pour avoir le status global de l'application
	if (AppInstList){
		println "[I] server for application $deployName : $AppInstList"
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
		AppInstList.each{ instance ->
			NbServers=NbServers+1
			def tab1=instance.split(":")
			def host=tab1[0].trim()
			def server=tab1[1].trim()
			command="/host="+host+"/server-config="+server+":read-attribute(name=status)"
			resultCmd=js.runCLICommand(command)
			if (resultCmd.isSuccess()){
				def String state=resultCmd.getResponse().get("result").asString()
				if(state=="STARTED") {
					Started=Started+1
				}
				if(state=="STARTING") {
					Starting=Starting+1
				}
				if(state=="STOPPED") {
					Stopped=Stopped+1
				}
				if(state=="STOPPING") {
					Stopping=Stopping+1
				}
				if(state=="FAILED") {
					Failed=Failed+1
					FailedInst.add(instance)
				}
			} else {
				println "[E] unable to retrieve status of server ${host}/${server} - command : $command"
			}
		}
		
		// case detat : RUNNING / STOPPED / DEGRADED / TRANSITION / FAILED
		if (NbServers==Started){
			println "[I] ${deployName} is running(${Started}/${NbServers})"
			cr=0
		} else {
			if (NbServers==Stopped){
				println "[I] ${deployName} is stopped(${Stopped}/${NbServers})"
				cr=1
			} else {
				if(Started>0 && Started !=NbServers){
				println "[W] ${deployName} is degraded - running(${Started}/${NbServers})"
				cr=2
				} else {
					if(Starting+Stopping==NbServers){
						println "[W] ${deployName} transitional state - starting(${Starting}/${NbServers}) stopping(${Stopping}/${NbServers})"
						cr=3
					} else {
						if(Failed==NbServers){
							println "[E] ${deployName} is failed(${Failed}/${NbServers})"
							cr=4
						} else {
							println "[W] ${deployName} state : running(${Started}/${NbServers}) - starting(${Starting}/${NbServers}) - stopping((${Stopping}/${NbServers}) - stopped(${Stopped}/${NbServers}) - failed(${Failed}/${NbServers})"
							cr=5
						}
					}
				}
			}
		}
		if (FailedInst){
			println "[E] There's failed servers : ${FailedInst}"
		}
	} else {
		println "[E] no instance for application ${deployName}"
	}
	//retourne le cr 0(running ou pas d'instances) 1(stopped) 2(degraded) 3(transition) 4(all failed) 5(autre etat)
	return cr
}










//------- input connexion to domain
def controllerHost="10.10.10.1"
def int controllerPort=9999
def String userMgmt="admin"
def char[] passwordMgmt="azerty01;"
//------- input connexion to domain

//------- def Jboss7Shell & connexion
def Jboss7Shell js = new Jboss7Shell(userMgmt,passwordMgmt,controllerHost,controllerPort)
js.connect()
js.testConnexion()
//------- def Jboss7Shell & connexion


statusOfOneApplication(js,"SubAppliName1-v1.2.war")


js.disconnect()
System.exit(0)

