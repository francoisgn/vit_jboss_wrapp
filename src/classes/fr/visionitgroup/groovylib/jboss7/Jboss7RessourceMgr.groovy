package fr.visionitgroup.groovylib.jboss7

import java.util.List;

import fr.visionitgroup.groovylib.jboss7.Jboss7Shell

class Jboss7RessourceMgr {
	def static int deleteResource(def Jboss7Shell cli,def resource) {
		// test if ressource existe
		def command="$resource:read-resource"
		def result=cli.runCLICommand(command)
		if (result.isSuccess()){
		   command="$resource:remove"
		   result=cli.runCLICommand(command)
		   if (result.isSuccess()){
			   println("[S]:delete resource:$resource")
			   return 0
		   }
		   else{
				def response = result.getResponse()
				println("[E]:delete resource:$resource:"+response.asString())
				return 1
		   }
		}
		else{
			println("[W]:unknown resource:$resource: cancel Deletion")
			return 0
		}
		
		return 1
	}
	
	def static int addResource(def Jboss7Shell cli,def command) {
		// test if ressource existe
		try{
			def result=cli.runCLICommand(command)
			if (result.isSuccess()){
				println("[S]:add resource:$command")
				return 0
			}
		   else{
				def response = result.getResponse()
				println("[E]:add resource:$command:"+response.asString())
				return 1
		   }
		}
		catch (Exception e){
			println("[E]:add resource:$command:"+e.getMessage())
			return 1
		}

		return 1
	}
	
	def static int deleteListOfResources(def Jboss7Shell cli,def List resourceLst,def Boolean _reverse) {
		def status=0
		if (_reverse){resourceLst=resourceLst.reverse()}
		//println("resourceLst="+resourceLst)
		resourceLst.each{
			//println("it=$it")
			def cr=deleteResource (cli,it)
			if (cr != 0 ){ status=1}
		}
		return status
	}
	
	def static int addGroupOfResources(def Jboss7Shell cli,def List commandLst) {
		def status=0
		
		//println("commandLst="+commandLst)
		commandLst.each{
			//println("it=$it")
			def cr=addResource (cli,it)
			if (cr != 0 ){ status=1}
		}
		return status
	}
	
	
	
	
	
	
}
