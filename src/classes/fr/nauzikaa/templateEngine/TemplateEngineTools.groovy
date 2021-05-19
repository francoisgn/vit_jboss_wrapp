package fr.nauzikaa.templateEngine;
import fr.nauzikaa.utils.zip.ZipTools;

public class TemplateEngineTools {
	
	def dirOffsetStr=""
	def destDirStr = ""
	def workDirStr= ""
	
	def startDelimiter = '@@'
	def endDelimiter = '@@'
	def listZipFileRegExp=""
	
	
	def propFileName = "replace_tokens.properties"
	
	def includesLst = "**/*.properties"
	def excludesLst = ""
	
	def propertyPrefix = ""
	def envPropValues = ""
	def explicitTokens = ""
	def inputPropertiesFilesList =""
	
	def File propFile 
	def bootstrap
	def tempUnZipdir
	def tempSrcdir
	def workDirInstanceStr
	def Properties properties = new Properties()
	def valided=false
	

	
    TemplateEngineTools(srcDirStr,destDirStr,workDirStr){
    	this.bootstrap = new ZipTools()
    	this.dirOffsetStr=srcDirStr
    	this.destDirStr=destDirStr
		this.workDirStr=workDirStr
		this.workDirInstanceStr=workDirStr+File.separator+"TE_"+new Date().format("yyyy-MM-dd_HH-mm-ss-SSS")
    }
	
	void setIncludesLst(includes){this.includesLst=includes}
	void setExcludesLst(excludes){this.excludesLst=excludes}
	void setPropertyPrefix(propertyPrefix){this.propertyPrefix=propertyPrefix}
	void setInputPropertiesFilesList(inputPropertiesFilesList){this.inputPropertiesFilesList=inputPropertiesFilesList}
	void setEnvPropValues(envPropValues){this.envPropValues=envPropValues}
	void setExplicitTokens(explicitTokens){this.explicitTokens=explicitTokens}
	void setStartDelimiter(startDelimiter){this.startDelimiter=startDelimiter}
	void setEndDelimiter(endDelimiter){this.endDelimiter=endDelimiter}
	void setListZipFileRegExp(listZipFileRegExp){this.listZipFileRegExp=listZipFileRegExp}
	void setPropFileName(propFileName){this.propFileName=propFileName}
	void setValided(valided){this.valided=valided}

	
	private createWorkdir(){
		println "[Infos] [createWorkdir] create  workdir "+this.workDirInstanceStr+" and its subdirectories"
		def d=new File(this.workDirStr)
		if (!d.isDirectory()){
			throw new Exception("["+this.class.canonicalName+"][createWorkdir] [Error]  The directory "+this.workDirStr+" doesn't exist\n")
			
		}
		else{
			
			try{
				def workDirInstance= new File(this.workDirInstanceStr)
				workDirInstance.mkdirs()
				this.tempUnZipdir=new File(this.workDirInstanceStr+File.separator+"tempUnzip")
				this.tempSrcdir=new File(this.workDirInstanceStr+File.separator+"tempSrc")
				this.tempUnZipdir.mkdirs()
				this.tempSrcdir.mkdirs()
				this.propFile = new File(this.workDirInstanceStr+File.separator+propFileName)
				//this.propFile = new File(propFileName)
			}
		
			catch (Exception e) {
				e.printStackTrace()
				throw new Exception("["+this.class.canonicalName+"][createWorkdir] [Error] Impossible to create "+this.workDirInstanceStr+" and its subdirectories\n"+e.getMessage())

			}
		}

	}

	private def loadSrcDirToWorkdir(){

		try{
			println "[Infos] [loadSrcDirToWorkdir] Copy  " + this.dirOffsetStr + " to template Engin workdir " + tempSrcdir
			// Copy src dir to template Engine workdir
			def ant = new AntBuilder()
			ant.sync(verbose: "true", todir: tempSrcdir, overwrite: "true") {
				fileset(dir: this.dirOffsetStr) {
					"**/*"
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace()
			throw new Exception("["+this.class.canonicalName+"][loadSrcDirToWorkdir] [Error]  Impossible to copy "+this.dirOffsetStr+ " to template Engin workdir " + tempSrcdir+"\n")
		}

	}
	
	private deleteWorkdir(){
		try{
			println "[Infos] [deleteWorkdir] delete :"+this.workDirInstanceStr
			def workDirInstance= new File(this.workDirInstanceStr)
			workDirInstance.delete()
		
		}
	
		catch (Exception e) {
			e.printStackTrace()
			throw new Exception("["+this.class.canonicalName+"][deleteWorkdir] [Error]  Impossible to delete "+this.workDirInstanceStr+" and its subdirectories\n"+e.getMessage())
		}
	}
	
	private addToProperties(property,propertyPrefix,token,value,startDelimiter,endDelimiter) {
		if (propertyPrefix) {
			println "Looking for properties starting with $propertyPrefix"
		}
		else{
			property.setProperty(startDelimiter+token+endDelimiter, value)
		}
		return 0
	}
	private generateGlobalPropertiesFile(){
		try
		{
			println "[Infos] [generateGlobalPropertiesFile] starting merge all properties"
			
			def outStream  = new FileOutputStream(this.propFile)
			
			if(this.inputPropertiesFilesList) {
				println "[Infos] [generateGlobalPropertiesFile] Merging inputPropertiesFiles"+this.inputPropertiesFilesList
				//this is jeffs magic regex to split on ,'s preceded by even # of \ including 0
				 inputPropertiesFilesList.split("(?<=(^|[^\\\\])(\\\\{2}){0,8}),").each { inputPropertiesFile ->
					 def Properties p = new Properties()
					 def is = new FileInputStream(inputPropertiesFile);
					 p.load(is)
					 is.close()
					 p.each{ propName,propValue ->
						 //properties.setProperty(propName, propValue)
						 addToProperties(properties,propertyPrefix,propName, propValue,startDelimiter,endDelimiter)
						 //println 'added: ' + propName + ':' + propValue
					 }
		
				 }
			 }
			
			println "[Infos] [generateGlobalPropertiesFile] Add envPropValues"
			if(envPropValues) {
				if (propertyPrefix) {
					println "Looking for properties starting with $propertyPrefix"
				}
				//this is jeffs magic regex to split on ,'s preceded by even # of \ including 0
				envPropValues.split("(?<=(^|[^\\\\])(\\\\{2}){0,8}),").each { prop ->
					//split out the name
					def parts = prop.split("(?<=(^|[^\\\\])(\\\\{2}){0,8})=",2);
					def propName = parts[0];
					def propValue = parts.size() == 2 ? parts[1] : "";
					//replace \, with just , and then \\ with \
					propName = propName.replace("\\=", "=").replace("\\,", ",").replace("\\\\", "\\")
					propValue = propValue.replace("\\=", "=").replace("\\,", ",").replace("\\\\", "\\")
		
					if ((!propertyPrefix || propName.startsWith(propertyPrefix))) {
						addToProperties(properties,propertyPrefix,propName, propValue,startDelimiter,endDelimiter)
						//println 'added: ' +  propName  + ':' + propValue
					}
				}
			}
			
			if (explicitTokens) {
				println "[Infos] [generateGlobalPropertiesFile] Add explicitTokens"
				explicitTokens.split("<br/>").each {
					if (it && it.indexOf('->') > 0) {
						def index = it.indexOf('->')
						def propName = it.substring(0, index).trim()
						def propValue = index < (it.length() - 2) ? it.substring(index + 2) : ""
						addToProperties(properties,propertyPrefix,propName, propValue,startDelimiter,endDelimiter)
						//println 'added: ' + propName + ':' + propValue
					}
					else if (it) {
						println "Found invalid explicit token $it - missing -> separator"
					}
				}
			}
			println "[Infos] [generateGlobalPropertiesFile] store collected properties to:"+ this.propFile
			properties.store(outStream, 'Auto generated property file')
			outStream.close()
			
			if (properties.size() == 0) {
				throw new Exception("["+this.class.canonicalName+"][generateGlobalPropertiesFile] [Error]Did not find any properties or explicit tokens for replacement.bytes..")

			}
			println "[Infos] [generateGlobalPropertiesFile] Global properties content:"
			println "--------------- Begin -----------------------"
			println this.propFile.getText()
			println "--------------- End -----------------------"
			println ""
		}
		catch (Exception e) {
			e.printStackTrace()
			throw new Exception("["+this.class.canonicalName+"][generateGlobalPropertiesFile] [Error]  Impossible to generate the global properties file:  "+this.propFile+"\n"+e.getMessage())

		}

	}
	
	private createDestDir(){
		def d=new File(this.destDirStr)
		try {
		
			if (!d.isDirectory()){
		
				if(d.mkdirs()){
					println "[Infos] [createDestDir] Create"+this.destDirStr
				}
				else {
					
					throw new Exception("["+this.class.canonicalName+"][createDestDir] [Error]  Impossible to create "+this.destDirStr)
				}
			}
			else{
				println "[Warning] [createDestDir] "+this.destDirStr+" already exists!"
			}
			
		}
		catch (Exception e) {
			e.printStackTrace()
			throw new Exception("["+this.class.canonicalName+"][createDestDir] [Error]  Impossible to create "+this.destDirStr+"\n"+e.getMessage())

		}

	}

	/*	
	private recursiveZipAnalyse( _dirStr,_tempUnZipdirStr,__index,listZipFileRegExp,includes,excludes,propFileName ){
		
		def _dir=new File(_dirStr)
		def _tempUnZipdir=new File(_tempUnZipdirStr)
		
		def _index=new Integer (__index+1)
		def tab=""
		for( i in 0 .. _index){ tab="\t"+tab}
		
		println "[Infos][recursiveZipAnalyse]"+tab+"> Starting analyse of  : " +_dirStr
		println "[Infos][recursiveZipAnalyse]"+tab+">  UnZipdir : " +_tempUnZipdirStr
		println "[Infos][recursiveZipAnalyse]"+tab+">  index : " +__index
		println "[Infos][recursiveZipAnalyse]"+tab+">  listZipFileRegExp : " +listZipFileRegExp
		try{
			// generation d'une liste de fichier zip a traiter
			def  generatedOfZipFilesList=[]
			 _dir.listFiles().each{
				 if (it.isFile() && it.name.matches(listZipFileRegExp) ){
					 //println "zip:"+ it
					 generatedOfZipFilesList.add(it)
				 }
				 else{
					 //println "no zip:"+ it
				 }
				 
			 }
			 println "[Debug][recursiveZipAnalyse]"+tab+"> generatedOfZipFilesList : " +generatedOfZipFilesList
				
			
			 if (generatedOfZipFilesList.size() > 0 ){
				 // creation d'un repertoire temporaire de unzip
				 def temporaryWorkdir=new File(_tempUnZipdir.canonicalFile.toString()+File.separator+_index.toString())
				 temporaryWorkdir.mkdirs()
			
				 // traitement de chaque fichier zip
			
				generatedOfZipFilesList.each { file ->
					// creation d'un repertoire temporaire pour le unzippage du fichier zip portant le nom du fichier zip
					println "[Infos][recursiveZipAnalyse]"+tab+"> Analyse of zip: "+file
					def loopTemporyDirName=new File(temporaryWorkdir.canonicalFile.toString()+File.separator+file.name)
					loopTemporyDirName.mkdirs()
					
					// decompression du fichier zip ds loopTemporyDirName
					file.unzip(loopTemporyDirName.canonicalFile.toString())
					
					
					
					// lancement recursif sur les fichiers zip contenu dans le zip en cours de traitement
					recursiveZipAnalyse(loopTemporyDirName.canonicalFile.toString(),
						                   temporaryWorkdir.canonicalFile.toString(),
										   _index.toInteger(),
										   listZipFileRegExp,
										   includes,
										   excludes,
										   propFileName)
					
					// Lanceemnt de la valorisation des templates
					println "[Infos][recursiveZipAnalyse]"+tab+"> Replace template in :"+loopTemporyDirName.canonicalFile.toString()
					action(properties,loopTemporyDirName.canonicalFile.toString(),propFileName,includes,excludes)
					
					
					// recreattion du fichier zip
					loopTemporyDirName.zipdir(file.canonicalFile.toString())
					
					// supression d'un repertoire temporaire
					println "[Debug][recursiveZipAnalyse]"+tab+"> delete loopTemporyDirName:"+loopTemporyDirName
					loopTemporyDirName.delete()
					
				}
			}
		}
		catch (Exception e){
			e.printStackTrace()
			throw new Exception("["+this.class.canonicalName+"][recursiveZipAnalyse] Error execution \n"+e.getMessage())
			
		}

	
	}
*/
	
	private recursiveZipAnalyse( _dirStr,_tempUnZipdirStr,__index ){
		
		
		def _dir=new File(_dirStr)
		def _tempUnZipdir=new File(_tempUnZipdirStr)
		
		def _index=new Integer (__index+1)
		def tab=""
		for( i in 0 .. _index){ tab="\t"+tab}
		
		println "[Infos][recursiveZipAnalyse] Starting analyse of  : " +_dirStr
		/*println "[Infos][recursiveZipAnalyse]"+tab+"> Starting analyse of  : " +_dirStr
		println "[Infos][recursiveZipAnalyse]"+tab+">  UnZipdir : " +_tempUnZipdirStr
		println "[Infos][recursiveZipAnalyse]"+tab+">  index : " +__index
		println "[Infos][recursiveZipAnalyse]"+tab+">  listZipFileRegExp : " +this.listZipFileRegExp
		*/
		try{
			// generation d'une liste de fichier zip a traiter
			def  generatedOfZipFilesList=[]
			 _dir.listFiles().each{
				 if (it.isDirectory()){
					 //println "dir:"+ it
					 recursiveZipAnalyse(_dirStr+File.separator+it.name.toString(),_tempUnZipdir.canonicalFile.toString()+File.separator+it.name.toString(),__index)
					 
				 }
				 else{ // cas ou it = isFile()
					 
					 if ( it.name.matches(this.listZipFileRegExp) ){
						 //println "zip:"+ it
						 generatedOfZipFilesList.add(it)
					 }
					 else{
						 //println "no zip:"+ it
					 }
				 }
			 }
			 //println "[Debug][recursiveZipAnalyse]"+tab+"> generatedOfZipFilesList : " +generatedOfZipFilesList
			 println "[Infos][recursiveZipAnalyse] List of ZIPFile found : " +generatedOfZipFilesList
				
			
			 if (generatedOfZipFilesList.size() > 0 ){
				 // creation d'un repertoire temporaire de unzip
				 def temporaryWorkdir=new File(_tempUnZipdir.canonicalFile.toString()+File.separator+_index.toString())
				 temporaryWorkdir.mkdirs()
			
				 // traitement de chaque fichier zip
			
				generatedOfZipFilesList.each { file ->
					// creation d'un repertoire temporaire pour le unzippage du fichier zip portant le nom du fichier zip
					//println "[Infos][recursiveZipAnalyse]"+tab+"> Analyse of zip: "+file
					println "[Infos][recursiveZipAnalyse] Start Analyse of zip: "+file
					def loopTemporyDirName=new File(temporaryWorkdir.canonicalFile.toString()+File.separator+file.name)
					loopTemporyDirName.mkdirs()
					
					// decompression du fichier zip ds loopTemporyDirName
					println "[Infos][recursiveZipAnalyse] Uncompress of zip: "+file
					file.unzip(loopTemporyDirName.canonicalFile.toString())
					
					
					
					// lancement recursif sur les fichiers zip contenu dans le zip en cours de traitement
					recursiveZipAnalyse(loopTemporyDirName.canonicalFile.toString(),
										   temporaryWorkdir.canonicalFile.toString(),
										   _index.toInteger(),
										   )
					
					// Lancemnt de la valorisation des templates
					//println "[Infos][recursiveZipAnalyse]"+tab+"> Replace template in :"+loopTemporyDirName.canonicalFile.toString()
					action(loopTemporyDirName.canonicalFile.toString())
					
					
					
					// recreattion du fichier zip
					println "[Infos][recursiveZipAnalyse] Rebuild of zip: "+file
					loopTemporyDirName.zipdir(file.canonicalFile.toString())
					
					// supression d'un repertoire temporaire
					//println "[Debug][recursiveZipAnalyse]"+tab+"> delete loopTemporyDirName:"+loopTemporyDirName
					loopTemporyDirName.delete()
					println "[Infos][recursiveZipAnalyse] End Analyse of zip: "+file
					
				}
			}
		}
		catch (Exception e){
			e.printStackTrace()
			throw new Exception("["+this.class.canonicalName+"][recursiveZipAnalyse] Error execution \n"+e.getMessage())
			
		}

	
}
	
def action(tempSrcdir){
	def ant = new AntBuilder()
	def listIncludeFile= []
	println "[Infos][recursiveZipAnalyse] Process on dir: "+tempSrcdir
	println "propFileName="+propFileName
	println "this.propFile.absolutePath="+this.propFile.absolutePath
	println "this.propFile.canonicalPath="+this.propFile.canonicalPath.replace('\\', "/")
	if (this.properties.size() > 0) {
		try{
		/*if (excludes) {
			ant.replace(
					dir:tempSrcdir,
					summary: 'true',
					replacefilterfile: propFileName,
					includes: includes,
					excludes: excludes,
					defaultexcludes: 'no')
			

		}
		else {
			ant.replace(
					dir:tempSrcdir,
					summary: 'true',
					replacefilterfile: propFileName,
					includes: includes,
					defaultexcludes: 'no')
		}*/
			if (this.excludesLst) {
				ant.replace(
						summary: 'true',
						replacefilterfile: propFileName ,
						defaultexcludes: 'no')	{
							fileset(dir:tempSrcdir) {
										            	this.includesLst.split(',').each {
															if (it && it.trim().length() > 0) {
																include(name:it.trim())
																listIncludeFile.add(it.trim())
															}
														}
														if(excludesLst) {
															this.excludesLst.split(',').each {
																if (it && it.trim().length() > 0) {
																	exclude(name:it.trim())
																}
												
															}
														}
													}
												}
						
	        }
			else {
				ant.replace(
						summary: 'true',
						replacefilterfile: this.propFile.canonicalPath.replace('\\', "/"),
						defaultexcludes: 'no')	{
							fileset(dir:tempSrcdir) {
										            	this.includesLst.split(',').each {
															if (it && it.trim().length() > 0) {
																include(name:it.trim())
																listIncludeFile.add(it.trim())
																print "add:"+it.trim()
															}
														}
													}
												}
			}
			
        }
				
		
			
		catch(Exception e){throw new Exception("["+this.class.canonicalName+"][action] ant replace fail\n"+e.getMessage())}
	}
	else {
		throw new Exception("["+this.class.canonicalName+"][action] [Error][run] Did not find any properties or explicit tokens for replacement.\n") 

	}

}	
	def run(){
		try{
			def cr=0
			createWorkdir();
			createDestDir()
			loadSrcDirToWorkdir()
			generateGlobalPropertiesFile()
			def index=new Integer(0)
			recursiveZipAnalyse(this.tempSrcdir.canonicalFile.toString(),
				               this.tempUnZipdir.canonicalFile.toString(),
							   index.toInteger()
							   )
			//println "[Infos][recursiveZipAnalyse]> Replace template in :"+tempSrcdir
			action(tempSrcdir)
			
			
			
			//Synchronistaion du templateEngine workdir to destdir
			def ant = new AntBuilder()
			ant.echo("Preparing to synchronize " + tempSrcdir + " with " + this.destDirStr)
			ant.sync(verbose: "true", todir: this.destDirStr, overwrite: "true") {
				fileset(dir: tempSrcdir) {
					"**/*"
					
				}
				
				
			}
			
			deleteWorkdir()
			return 0
		}
		catch (Exception e) {
			e.printStackTrace()
			throw new Exception("["+this.class.canonicalName+"][run] Error exectution . Abort\n"+e.getMessage())
			return 1
		}
		finally{
			deleteWorkdir()
		}
		
	}
}