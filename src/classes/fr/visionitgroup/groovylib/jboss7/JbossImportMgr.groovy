package fr.visionitgroup.groovylib.jboss7



import org.jboss.as.cli.scriptsupport.*

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern

import org.jboss.dmr.ModelNode;

import java.util.regex.Matcher
import java.util.regex.Pattern

class JbossImportMgr {
	
	def static Boolean isExistResource(def Jboss7Shell js,def resourceScope) {
		// test if ressource existe
		def command="$resourceScope:read-resource"
		
		def result=js.runCLICommand(command)
		if (result.isSuccess()){
		   return true
		}
		else{
			return false
		}
		
		
		return 1
	}
	
	def private static generateImportJbossCLIStringFromImportConfigurationString(def Jboss7Shell js,def mode="insertOrUpdate",def String importStr){
		def String jbossCliStr=""
		// generation de la liste line => objets a importer (cad a creer ou mettre a jour
		def listOfObjToCreate=[:]
		def orderedListOfObjToCreate=[]
		def listOfObjToUpdate=[:]
		def orderedListOfObjToUpdate=[]
		def long cptLine=1
		importStr.eachLine {line->
			line=line.trim()
			
			if(!(line ==~ /^ *$/ || line ==~ /^ *#.*$/)){
				if(line =~ /.*:constructor\(.*\)/ ){
					
					def matcher = "$line" =~ /(.*):constructor\((.*)\)/
					if (matcher.matches() && matcher.getCount() ==1) {
						def String objName= matcher[0][1].toString().trim()
						def String constructorvalue=matcher[0][2].toString().trim()
						if(!listOfObjToCreate.containsKey(objName)){
							listOfObjToCreate.putAt(objName, constructorvalue)
							orderedListOfObjToCreate.add(objName)
						}
					}
					else{
						throw new Exception( "[E] matcher `.*:constructor(.*)` error in  line=$cptLine:$line")
					}
				}
				else if(line =~ /.*:property\(.*\)/ ){
					def matcher = "$line" =~ /(.*):property\((.*)\)/
					if (matcher.matches() && matcher.getCount() ==1) {
						def String objName= matcher[0][1].toString().trim()
						def String propertyvalue=matcher[0][2].toString().trim()
						if(!listOfObjToUpdate.containsKey(objName)){
							listOfObjToUpdate.putAt(objName, propertyvalue)
							orderedListOfObjToUpdate.add(objName)
						}
					}
					else{
						throw new Exception( "[E] matcher `.*:property(.*)` error in  line=$cptLine:$line")
					}
				}
				else{
					throw new Exception( "[E] Syntax Error line=$cptLine:$line")
				}
			}
			
			cptLine++
			
		}
		println "[D]orderedListOfObjToCreate="+orderedListOfObjToCreate
		println "[D]orderedListOfObjToUpdate="+orderedListOfObjToUpdate
		
		
				
		// generation de la liste des objets existant deja
		def listOfObjAlreadyExist=[:]
		orderedListOfObjToCreate.each {objScope->
			if(isExistResource(js,objScope)){listOfObjAlreadyExist.put(objScope, "")}
		}
		orderedListOfObjToUpdate.each {objScope->
			if(isExistResource(js,objScope)){listOfObjAlreadyExist.put(objScope, "")}
		}
		println "[D]listOfObjAlreadyExist="+listOfObjAlreadyExist
		
		// Controle de l'existance ou de la future existance des objets a mettre a jour et control de l'existence de l'objet dans les listes alreadyExist ou toBeCreated
		try{
			orderedListOfObjToUpdate.each{ objScope->
				if(!listOfObjAlreadyExist.containsKey(objScope)  ){
					if(!listOfObjToCreate.containsKey(objScope)  ){
						throw new Exception ("[E] Error the object $objScope mus be updated but it doesn't already exist and it won't be create by import")
						return 1
					}
				}
			}
		}
		catch(Exception e){
			println e.getMessage()
			return 1
		}
		
		if(mode=="insertOrUpdate" ){
			
			
			
			// generation de la liste des objets manquant devant etre crees 
			//def listOfObjToCreate=[:]
			def orderedListOfObjMissing=[]
			orderedListOfObjToCreate.each{ objScope->
				if(!listOfObjAlreadyExist.containsKey(objScope)){
					orderedListOfObjMissing.add(objScope)
				}
			}
			println "[D]orderedListOfObjMissing="+ orderedListOfObjMissing
			
			
			
			
			// get String de creation des nouveaux objets
			def String createNewObjectStr=""
			orderedListOfObjMissing.each{objScope->
				createNewObjectStr+="${objScope}:add("+listOfObjToCreate.get(objScope)+")\n"
			}
			// get String de mise a jour
			
			// Erreur: il faut repartir de la liste complet des 
			def String updateObjectStr=""
			
			cptLine=1
			importStr.eachLine {line->
				line=line.trim()
				
				if(!(line ==~ /^ *$/ || line ==~ /^ *#.*$/)){
					 if(line =~ /.*:property\(.*\)/ ){
						def matcher = "$line" =~ /(.*):property\((.*)\)/
						if (matcher.matches() && matcher.getCount() ==1) {
							def String objName= matcher[0][1].toString().trim()
							def String propertyvalue=matcher[0][2].toString().trim()
							
							updateObjectStr+="${objName}:write-attribute("+propertyvalue+")\n"
						}
						else{
							throw new Exception( "[E] matcher `.*:property(.*)` error in  line=$cptLine:$line")
						}
					}
					
				}
				
				
			}
			
			jbossCliStr=createNewObjectStr+updateObjectStr
			
			
			
			
		}
		else{
						
			// generation de la liste des objets existant deja devant donc etre supprimer en premier
			// rq: la suppression doit être inverse par rapport a l'ordre de creation
			
			def orderedListOfObjMustBeDeleteFirst=[]
			orderedListOfObjToCreate.reverse().each{ objScope->
				if(listOfObjAlreadyExist.containsKey(objScope)){
					orderedListOfObjMustBeDeleteFirst.add(objScope)
				}
			}
			println "[D]orderedListOfObjMustBeDeleteFirst="+ orderedListOfObjMustBeDeleteFirst
			
			
			
			// get String de suppression des objets existant
			def String deleteFirstExistedObjectStr=""
			orderedListOfObjMustBeDeleteFirst.each{objScope->
				deleteFirstExistedObjectStr+="${objScope}:remove()\n"
			}
			println "[D]deleteFirstExistedObjectStr="+ deleteFirstExistedObjectStr
			
			// get String de creation and update des objets
			def String createOrUpdateObjectStr=""
			cptLine=1
			importStr.eachLine {line->
				line=line.trim()
				
				if(!(line ==~ /^ *$/ || line ==~ /^ *#.*$/)){
					if(line =~ /.*:constructor\(.*\)/ ){
						def matcher = "$line" =~ /(.*):constructor\((.*)\)/
						if (matcher.matches() && matcher.getCount() ==1) {
							def String objName= matcher[0][1].toString().trim()
							def String propertyvalue=matcher[0][2].toString().trim()
							
							createOrUpdateObjectStr+="${objName}:add("+propertyvalue+")\n"
						}
						else{
							throw new Exception( "[E] matcher `.*:add(.*)` error in  line=$cptLine:$line")
						}
					}
					else if(line =~ /.*:property\(.*\)/ ){
						def matcher = "$line" =~ /(.*):property\((.*)\)/
						if (matcher.matches() && matcher.getCount() ==1) {
							def String objName= matcher[0][1].toString().trim()
							def String propertyvalue=matcher[0][2].toString().trim()
							
							createOrUpdateObjectStr+="${objName}:write-attribute("+propertyvalue+")\n"
						}
						else{
							throw new Exception( "[E] matcher `.*:property(.*)` error in  line=$cptLine:$line")
						}
					}
					
				}
				
				
			}
			
			
			jbossCliStr=deleteFirstExistedObjectStr+createOrUpdateObjectStr
		}
		println "[D]jbossCliStr="+ jbossCliStr
		return jbossCliStr
	}
	
	
	def static public  int importConfigurationFromString(def Jboss7Shell js,def mode,def String importStr){
		if(mode!="insertOrUpdate" && mode!="deleteAndInsert"){ println "[E] invalid mode : $mode"; return 1}
		
		def String importFormatedStr=generateImportJbossCLIStringFromImportConfigurationString(js,mode,importStr)
		
		

		return js.runBacthCLIScript(false, importFormatedStr)
	}
	
	def static public  int importConfigurationFromFile(def Jboss7Shell js,def mode,def String importFilePath){
		if(mode!="insertOrUpdate" && mode!="deleteAndInsert"){ println "[E] invalid mode : $mode"; return 1}
		
		def importFormatedStr=""
		try{
			importFormatedStr=generateImportJbossCLIStringFromImportConfigurationString(js,mode,new File(importFilePath).text)
		}
		catch(Exception e){
			println "[E] Technical error due to :"+e.getMessage()
			return 1
		}
		
		
		
		

		return js.runBacthCLIScript(false, importFormatedStr)
	}
	def static public  int checkImportConfigurationFromString(def Jboss7Shell js,def mode,def String importStr){
		if(mode!="insertOrUpdate" && mode!="deleteAndInsert"){ println "[E] invalid mode : $mode"; return 1}
		
		def String importFormatedStr=generateImportJbossCLIStringFromImportConfigurationString(js,mode,importStr)

		return js.runBacthCLIScript(true, importFormatedStr)
	}
	def static public  int checkImportConfigurationFromFile(def Jboss7Shell js,def mode,def String importFilePath){
		if(mode!="insertOrUpdate" && mode!="deleteAndInsert"){ println "[E] invalid mode : $mode"; return 1}
		
		def importFormatedStr=""
		try{
			importFormatedStr=generateImportJbossCLIStringFromImportConfigurationString(js,mode,new File(importFilePath).text)
		}
		catch(Exception e){
			println "[E] Technical error due to :"+e.getMessage()
			return 1
		}

		def cr=js.runBacthCLIScript(true, importFormatedStr)
		def File f=new File("g:/nico.debug")
		if(f.isFile()){f.delete()}
		f<< importFormatedStr
		return cr

	}
}
