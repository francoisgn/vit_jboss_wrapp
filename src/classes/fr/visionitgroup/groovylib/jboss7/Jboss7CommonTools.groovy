package fr.visionitgroup.groovylib.jboss7

import org.jboss.as.cli.scriptsupport.*

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern

import org.jboss.dmr.ModelNode;

class Jboss7CommonTools {
	
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
	
	def static private List getAttributesListOfObject(def Jboss7Shell js,def String objScope){
		def myList=[]
		def command="$objScope:read-resource-description()"
		def resultCmd=js.runCLICommand(command)
		if (resultCmd.isSuccess()){
			
			def ModelNode result=resultCmd.getResponse().get("result")
			 result.get("attributes").keys().each{
				 //println "key=$it"
				 myList.add(it)
			 }
			 
			
		}
		else{
			myList=[]
		}
		return myList
	}
	def static private Map getAttributeInformations(def Jboss7Shell js,def String objScope,def String attributeName){
		def myMap=[:]
		def command="$objScope:read-resource-description()"
		def resultCmd=js.runCLICommand(command)
		if (resultCmd.isSuccess()){
			
			def ModelNode result=resultCmd.getResponse().get("result")
			result.get("attributes").get(attributeName).keys().each{def String key->
				myMap.put(key, result.get("attributes").get(attributeName).get(key).asString())
			}
			
			 
			
		}
		else{
			myMap=[]
		}
		return myMap
	}
	def static private List getChildrenTypeListOfObject(def Jboss7Shell js,def String objScope){
		def myList=[]
		def command="$objScope:read-resource-description()"
		def resultCmd=js.runCLICommand(command)
		if (resultCmd.isSuccess()){
			def ModelNode result=resultCmd.getResponse().get("result")
	
			 result.get("children").keys().each{
				 
				 myList.add(it.trim())
			 }
			 
			
		}
		else{
			myList=[]
		}
		return myList
	}
	
	def static private List getChildrenListOfObject(def Jboss7Shell js,def String objScope,def childTypes=[]){
		def childObject=[]
		childTypes.each{ String type->
			def String command="$objScope/:read-children-resources(child-type=${type})"
			def resultCmd=js.runCLICommand(command)
			if (resultCmd.isSuccess()){
				
				def response=resultCmd.getResponse()

				def ModelNode result=response.get("result")
				result.keys().each { it->
					def String name=it.trim()
					
					name=formatNameObject(name)
					
					childObject.add("$objScope/${type}="+name)
				}
			}
			else{
				return []
			}
		}
		return childObject
	}
	def static private String formatNameObject(def String value){
		return value.replaceAll("/", "\\\\/").replaceAll(":", "\\\\:").replaceAll(" ", "\\\\ ").replaceAll('"', '\\\\"')
	}
	
	def static private String getAttributeValue_old(def Jboss7Shell js,def String objScope,def String attributename,def String attributeType){
		
		def String value=""
		def command="$objScope:read-attribute(name=${attributename},include-defaults=true"
		def resultCmd=js.runCLICommand(command)
		if (resultCmd.isSuccess()){
			value=resultCmd.getResponse().get("result").asString()
			if(value!="undefined"){
				//format value in depending on attributeType
				if( attributeType=="INT"){
					value=formatValueAsINT(value)
				}
				else if ( attributeType=="LONG"){
					value=formatValueAsLONG(value)
				}
				else if ( attributeType=="BOOLEAN" ){
					value=formatValueAsBOOLEAN(value)
				}
				else if ( attributeType=="STRING"){
					value=formatValueAsSTRING(value)
				}
				else if( attributeType=="BOOLEAN" ) {
					value=formatValueAsSTRING(value)
				}
				else if( attributeType=="BIG_DECIMAL" ) {
					value=formatValueAsBIGDECIMAL(value)
				}
				else if( attributeType=="OBJECT" ) {
					value=formatValueAsOBJECT(value)
				}
				else if( attributeType=="LIST" ) {
					value=formatValueAsLIST(value)
				}
				else if( attributeType=="BOOLEAN" ) {
					value=formatValueAsBOOLEAN(value)
				}
				else{
					
					value=formatValueAsSTRING(value)
				}
			
			
				
			}
			
			
			//
			
			
		}
		else{
			return null
		}
		return value
	}
	
	def static private String getAttributeValue(def Jboss7Shell js,def String objScope,def String attributename,def String attributeType=null){
		
		def String value=""
		def command="$objScope:read-attribute(name=${attributename},include-defaults=true"
		def resultCmd=js.runCLICommand(command)
		if (resultCmd.isSuccess()){
			   value=resultCmd.getResponse().get("result").asString()
			   if(value!="undefined"){
					  //format value in depending on attributeType
					  if( attributeType==null){
							 
							 attributeType=getAttributeInformations( js,objScope,attributename)["type"]
							 
					  }
					  if( attributeType=="INT"){
							 value=formatValueAsINT(value)
					  }
					  else if ( attributeType=="LONG"){
							 value=formatValueAsLONG(value)
					  }
					  else if ( attributeType=="BOOLEAN" ){
							 value=formatValueAsBOOLEAN(value)
					  }
					  else if ( attributeType=="STRING"){
							 value=formatValueAsSTRING(value)
					  }
					  else if( attributeType=="BOOLEAN" ) {
							 value=formatValueAsSTRING(value)
					  }
					  else if( attributeType=="BIG_DECIMAL" ) {
							 value=formatValueAsBIGDECIMAL(value)
					  }
					  else if( attributeType=="OBJECT" ) {
							 value=formatValueAsOBJECT(value)
					  }
					  else if( attributeType=="LIST" ) {
							 value=formatValueAsLIST(value)
					  }
					  else if( attributeType=="BOOLEAN" ) {
							 value=formatValueAsBOOLEAN(value)
					  }
					  else{
							 
							 value=formatValueAsSTRING(value)
					  }
			   
			   
					  
			   }
		}
	}

	def static private String protectedValueIfneeded(def String value){
		def newValue=value
		def needToBeProtected=false
		if(newValue=~ / /){needToBeProtected=true}
		if(newValue=~ /=/){needToBeProtected=true}
		if(newValue=~ /\$\{[^ ]*\}/){needToBeProtected=true}
		if(needToBeProtected){newValue='"'+newValue+'"'}
		return newValue
	}
	def static private String formatValueAsSTRING(def String value){
		
		def newValue=value
		def needToBeProtected=false
		// detect if '"' protection is needed
		if(newValue=~ / /){needToBeProtected=true}
		if(newValue=~ /=/){needToBeProtected=true}
		if(newValue=~ /\$\{[^ ]*\}}/){needToBeProtected=true}
		
		// transform data
		if(value=~ / /){newValue=newValue.replaceAll(" ","\\\\ ") }
		
		if (newValue==""){newValue='""'}
		
		
		return protectedValueIfneeded(newValue)
	}
	def static private String formatValueAsINT(value){
		return protectedValueIfneeded(value)
	}
	def static private String formatValueAsLONG(value){
		return protectedValueIfneeded(value)
	}
	
	def static private String formatValueAsBOOLEAN(value){
		return protectedValueIfneeded(value)
	}
	def static private String formatValueAsBIGDECIMAL(value){
		return protectedValueIfneeded(value)
	}
	def static private String formatValueAsOBJECT(value){
		// Attention ne pas applique la protection par '"' pour ce type de valeur
		return value
	}
	def static private String formatValueAsLIST(value){
		// Attention ne pas applique la protection par '"' pour ce type de valeur
		return value
	}
	
	static List  getListofObjects(def Jboss7Shell js, def String  searchBaseScope,def String typeOfObject,def Boolean recursiveSearch=false, def Map CoupleAttrValueFilter=[:],def String  regexpFilter=""){
		def List listOfObjects=[]
		
		searchBaseScope=searchBaseScope.replaceAll("/*\$","")
		def List childrenTypeList=Jboss7CommonTools.getChildrenTypeListOfObject(js,searchBaseScope)
		
		childrenTypeList.each{ def String type->
			if(type==typeOfObject){
				//println "type:$type"
				def List typeOfList=[]
				typeOfList.add(type)
				//println "typeOfList:"+typeOfList
				//println "searchBaseScope:"+searchBaseScope
				def List childrenList=Jboss7CommonTools.getChildrenListOfObject(js,searchBaseScope,typeOfList)
				//println "childrenList:"+childrenList
				childrenList.each{ def String childScope->
					def allAttrMacthed=true
					if(CoupleAttrValueFilter.size() > 0){
						def List attrList=Jboss7CommonTools.getAttributesListOfObject(js,childScope)
						try{
							CoupleAttrValueFilter.each{ def ctrlAttr,def String crtlValue->
								//println "ctrlAttr=$ctrlAttr crtlValue=$crtlValue"
								
								def attrMatched=false
								try{
									attrList.each{def String attributeName->
										//println "attributeName=$attributeName"
										if (attributeName== ctrlAttr){
											def Map attrInfos=Jboss7CommonTools.getAttributeInformations(js,childScope,attributeName)
											def String value=Jboss7CommonTools.getAttributeValue(js,childScope,attributeName,attrInfos["type"])
											//println "attributeName=$attributeName value=$value"
											def regexpAttributValue=CoupleAttrValueFilter[attributeName]
											//println "attributeName=$attributeName value=$value regexpAttributValue=$regexpAttributValue"
											if((value=~ /${regexpAttributValue}/)){
												attrMatched=true
												throw new Exception ("attr match")
											}
										}
									}
								}
								catch(Exception e){
									// on sort de la closure car l'attribut match
									attrMatched=true
								}
								
								if(!attrMatched){
									throw new Exception ("one attr no match")
								}
								
							}
						}
						catch(Exception e){
							allAttrMacthed=false
						}
					}
					if(allAttrMacthed){
						if(regexpFilter!="" && childScope =~ /${regexpFilter}/){
							listOfObjects.add(childScope)
								
						}
						else{
							listOfObjects.add(childScope)
						}
					}
						
				}
			}
			else{
				if(recursiveSearch){
					def List typeOfList=[]
					typeOfList.add(type)
					def List childrenList=Jboss7CommonTools.getChildrenListOfObject(js,searchBaseScope,childrenTypeList)
					childrenList.each{def String childScope->
						listOfObjects.addAll(getListofObjects( js,childScope,typeOfObject,recursiveSearch,CoupleAttrValueFilter,regexpFilter))
					}
					
				}
			}
		}
		
		
		return listOfObjects
	}
	
	static def int execute_background_cmdline(def List envp=[],def File workdir=null,def List<String> command){
		
		// Calcul des var environnement
		def Map env=System.getenv()
		
		def Map newEnv=[:]
		envp.each{ def String keyVal->
			def String newKey=keyVal.split("=")[0]
			def String newVal=keyVal.replaceAll("^ *${keyVal} *=","")
			newEnv.put(newKey.trim(), newVal.trim())
		}
		
		
		def  newEnvp=[]
		env.each{ key,val->
			if(!newEnv.containsKey(key)){
				envp.add("$key=${val}")
			}
			
		}
		newEnvp.each{ key,val->
				envp.add("$key=${val}")
		}
		//force special env var necessary for windows plateforme
		envp.add("NOPAUSE=Y")

		def cr=0
		try{
			  
			  println "[D] command:$command"
			  //command="""cmd /c set"""
			 def process
			 
			 process = command.execute(envp,workdir)
			 
             process.consumeProcessOutput()
             
       }
       catch(Exception e){
             cr=1
             println "[E] Fail to execute : cmd=$command\n Exception occured :\n"+e.getMessage()
       
       }
       return cr
	}
	
}
