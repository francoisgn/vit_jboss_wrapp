package fr.visionitgroup.groovylib.jboss7



import org.jboss.as.cli.scriptsupport.*

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern

import org.jboss.dmr.ModelNode;



class Jboss7ExportMgr {
	
	
	def static private String exportRecursiveObject(def Jboss7Shell js,def String objScope,def Boolean recursive=false,def String destExportFilePath="",def String exportMode="jboss-cli"){
		def List attrList=Jboss7CommonTools.getAttributesListOfObject(js,objScope)
		def List childrenTypeList=Jboss7CommonTools.getChildrenTypeListOfObject(js,objScope)
		def List childrenList=Jboss7CommonTools.getChildrenListOfObject(js,objScope,childrenTypeList)
		def String vertical_attriStr=""
		def String horizontal_attriStr=""
		def String exportStr=""
		
		def String constructorStr="add"
		def String propertyStr="write-attribute"
		if(exportMode=="configuration"){
			constructorStr="constructor"
			propertyStr="property"
			
		}
		println "[I] export $objScope"
		attrList.each{def String attributeName->
			
			
			def Map attrInfos=Jboss7CommonTools.getAttributeInformations(js,objScope,attributeName)
			def String value=Jboss7CommonTools.getAttributeValue(js,objScope,attributeName,attrInfos["type"])
			if(value != null){
				// filter only access-type:read-write and
				
				def String attributeIsDeprecated=attrInfos["deprecated"]
				def String attributeAccessType=attrInfos["access-type"]
				def String attributeCanBeNull=attrInfos["nillable"]
				def String attributeDefaultValue=attrInfos["default"]
				
				// Stockage des parameters mandatory pour la construction de l'objet et ceci quelque soit le access-type: read-write|read-only|...  
				if(attributeCanBeNull=="false"){
					if(value!="undefined"){
						if(vertical_attriStr ==""){ vertical_attriStr="$attributeName=$value"}else{vertical_attriStr+=",$attributeName=$value"}
					}
				}
				
				// stockage des parameters optional pouvant etre reconfigure a postiori. filter only on attribute where access-type:read-write
				if(attributeIsDeprecated != null || attributeAccessType!= "read-write"){
					// do nothing: filtered
				}
				else{
		
					if(value!="undefined"){
						if( attributeDefaultValue != null && attributeDefaultValue == value){
						// a la base, il n'est pas necessaire de  faire la mise a jour d'un attribut si sa valeur actuelle est égale à la valeur par defaut. En effet, :
						// --> En cas de reimport en mode insertOrUpdate ou deleteAndInsert cela n'apporte rien
						// --> En  cas de nouveau parametrage avant l'import, l'utilisateur devra decommenter la ligne en meme tps qu'il valorisera la donnee	
						// Ainsi, par defaut le fichier d'export peut donc etre completement restaure de maniere garantie
						// De plus, on augmente les performances de l'import vu qu'il y a moins de donnees a importer
							if(horizontal_attriStr ==""){ horizontal_attriStr="#$objScope:${propertyStr}(name=$attributeName,value=$value)"}else{horizontal_attriStr+="\n"+"#$objScope:${propertyStr}(name=$attributeName,value=$value)"}
						} 
						else{
							if(horizontal_attriStr ==""){ horizontal_attriStr="$objScope:${propertyStr}(name=$attributeName,value=$value)"}else{horizontal_attriStr+="\n"+"$objScope:${propertyStr}(name=$attributeName,value=$value)"}
						}
					}
					else{
						if(attributeCanBeNull=="true"){
							// cas des attributs pouvant etre null et donc undefined
							// => on laisse donc active la ligne car lors du reimport, il fautdra que cet attribut soit force a "undefined"
							if(horizontal_attriStr ==""){ 
								horizontal_attriStr="$objScope:${propertyStr}(name=$attributeName,value=$value)"
							}
							else{
								horizontal_attriStr+="\n"+"$objScope:${propertyStr}(name=$attributeName,value=$value)"
								if(exportMode=="configuration"){
									horizontal_attriStr+="\n"+"#$objScope:property-description(name=$attributeName,description="+attrInfos.toString()+")"
								}
							}
						}
						else{
							// cas particulier d'attribut dit potentiellement "non null", mais qui dans certain cas (fonction notamment d'autres attributs de l'objet) ont le droit d etre null)
							if(horizontal_attriStr ==""){ 
								horizontal_attriStr="#$objScope:${propertyStr}(name=$attributeName,value=Can_not_be_UNDEFINED)"
							}
							else{
								horizontal_attriStr+="\n"+"#$objScope:${propertyStr}(name=$attributeName,value=Can_not_be_UNDEFINED)"
								if(exportMode=="configuration"){
									horizontal_attriStr+="\n"+"#$objScope:property-description(name=$attributeName,description="+attrInfos.toString()+")"
								}
							}
						}
					}
					
				
				}//end else
			}
			else{
				println "[W] unexported attribut case: $objScope:read-attribute(name=$attributeName). This attribut won't be exported"
			}
			
		}//end each
		
		vertical_attriStr="$objScope:${constructorStr}(${vertical_attriStr})"
		
		//format output
		exportStr="\n"+vertical_attriStr
		if(horizontal_attriStr!=""){
			exportStr+="\n"+horizontal_attriStr
		}
		
		
		if(destExportFilePath==""){
			if(recursive){
				
				childrenList.each{it->
					def String tmpStr=exportRecursiveObject(js,it,recursive,destExportFilePath,exportMode)
					if(tmpStr!=""){
						if(exportStr==""){exportStr=tmpStr}else{exportStr+="\n"+tmpStr}
					}
				}
			}
			return exportStr
		}
		else{
			// recriture ds le fichier destExportFilePath
			def File f=new File (destExportFilePath)
			f<<"\n"+exportStr
			childrenList.each{it->
				exportRecursiveObject(js,it,recursive,destExportFilePath,exportMode)
			}
			return ""
			
		}
		
		
		
		
	}//end exportObject
	
	def static public  String exportConfigurationtoString(def Jboss7Shell js,def Boolean recursive=false,def  objScopeList=[]){
		if(objScopeList.size()==0){ throw new Exception ( "[E] No object to export");}
		def String destExportFilePath=""
		def String exportMode="configuration"
		def String exportStr=""
		objScopeList.each{objScope->
			if(isExistResource(js,objScope)){
				try{
					println "[I] exportConfigurationtoString for ${objScope}"
					def String tmpStr=exportRecursiveObject(js,objScope,recursive,destExportFilePath,exportMode)
					if(tmpStr!=""){
						if(exportStr==""){exportStr=tmpStr}else{exportStr="\n"+tmpStr}
					}
				}
				catch(Exception e){
					throw new Exception( "[E] exportConfigurationtoString for ${objScope}: Failed due to \n"+e.getMessage())
				}
			}
			else{
				println "[W] exportConfigurationtoString for ${objScope}: Resource doesn't exist"
			}
		}
		return exportStr
	}
	def static public  int exportConfigurationToFile(def Jboss7Shell js,def Boolean recursive=false,def String destExportFilePath ,def objScopeList=[]){
		if(objScopeList.size()==0){ println "[E] No object to export"; return 1;}
		def String exportMode="configuration"
		
		def File f=new File(destExportFilePath)
		if(f.isFile()){
			if(!f.delete()){ println "[E] Impossible to delete $destExportFilePath"; return 1}
		}
		
		if(!f.getParentFile().isDirectory()){ println "[E] Impossible to locate "+ f.getParentFile().getAbsolutePath(); return 1}
			
		
		objScopeList.each{objScope->
			if(isExistResource(js,objScope)){
				try{
					println "[I] exportConfigurationToFile for ${objScope}"
					exportRecursiveObject(js,objScope,recursive,destExportFilePath,exportMode)
				}
				catch(Exception e){
					println ( "[E] exportConfigurationToFile for ${objScope}: Failed due to \n"+e.getMessage())
					return 1
				}
			}
			else{
				println "[W] exportConfigurationToFile for ${objScope}: Resource doesn't exist"
			}
		}
		return 0
	}
	
	
	def static public  String exportJbossClitoString(def Jboss7Shell js,def Boolean recursive=false,def  objScopeList=[]){
		if(objScopeList.size()==0){ throw new Exception ( "[E] No object to export");}
		def String destExportFilePath=""
		def String exportMode="jboss-cli"
		def String exportStr=""
		objScopeList.each{objScope->
			if(isExistResource(js,objScope)){
				try{
					println "[I] exportJbossClitoString for ${objScope}"
					def String tmpStr=exportRecursiveObject(js,objScope,recursive,destExportFilePath,exportMode)
					if(tmpStr!=""){
						tmpStr="$objScope:remove()"+"\n"
						if(exportStr==""){
							exportStr=tmpStr}else{exportStr="\n"+tmpStr}
					}
				}
				catch(Exception e){
					throw new Exception( "[E] exportJbossClitoString for ${objScope}: Failed due to \n"+e.getMessage())
				}
			}
			else{
				println "[W] exportJbossClitoString for ${objScope}: Resource doesn't exist"
			}
		}
		return exportStr
	}
	def static public  int exportJbossCliToFile(def Jboss7Shell js,def Boolean recursive=false,def String destExportFilePath ,def objScopeList=[]){
		if(objScopeList.size()==0){ println "[E] No object to export"; return 1;}
		def File f=new File(destExportFilePath)
		if(f.isFile()){
			if(!f.delete()){ println "[E] Impossible to delete $destExportFilePath"; return 1}
		}
		
		if(!f.getParentFile().isDirectory()){ println "[E] Impossible to locate "+ f.getParentFile().getAbsolutePath(); return 1}
		
		def String exportMode="jboss-cli"
		objScopeList.each{objScope->
			if(isExistResource(js,objScope)){
				try{
					println "[I] exportJbossCliToFile for ${objScope}"
					
					f<<"$objScope:remove()"
					exportRecursiveObject(js,objScope,recursive,destExportFilePath,exportMode)
					f<<"\n\n"
				}
				catch(Exception e){
					println ( "[E] exportJbossCliToFile for ${objScope}: Failed due to \n"+e.getMessage())
					return 1
				}
			}
			else{
				println "[W] exportJbossCliToFile for ${objScope}: Resource doesn't exist"
			}
		}
		return 0
	}
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
	
}//end class
