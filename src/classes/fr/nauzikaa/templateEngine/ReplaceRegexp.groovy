package fr.nauzikaa.templateEngine;
import fr.nauzikaa.utils.zip.ZipTools;




public class ReplaceRegexp {
	
	def dirOffsetStr=""
	def destDirStr = ""
	def workDirStr= ""
	
	def replaceText=""
	def failWithoutMatch=""
	
	def includes = "**/*.xml"
	def excludes = ""
	def listZipFileRegExp=""
	def sedSeparator="/"
	def replaceByLine="true"

	def bootstrap
	def tempUnZipdir
	def tempSrcdir
	def workDirInstanceStr
	def valided=false

	

	
    ReplaceRegexp(srcDirStr,destDirStr,workDirStr){
    	this.bootstrap = new ZipTools()
    	this.dirOffsetStr=srcDirStr
    	this.destDirStr=destDirStr
		this.workDirStr=workDirStr
		this.workDirInstanceStr=workDirStr+File.separator+"TE_"+new Date().format("yyyy-MM-dd_HH-mm-ss-SSS")
    }
	


	
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

	println "[Infos][recursiveZipAnalyse] Process on dir: "+tempSrcdir
	
		try{

				//replaceText: /aaa/bbbb/g\n/ddd/eee/g
			if (this.replaceText) {
				this.replaceText.split('\n').each {
					if (it && it.trim().length() > 0) {
						
						def tab=it.trim().split(this.sedSeparator)
						//println "tab:"+tab
						//println "len:"+tab.size()
						if (  tab.size() == 4 ) {
							def matchStr=tab[1]
							def replaceStr=tab[2]
							def flagsStr=tab[3]
							//println "matchStr:$matchStr"
							//println "replaceStr:$replaceStr"
							//println "flagsStr:$flagsStr"
						   
							ant.replaceregexp(match:matchStr,replace:replaceStr,byline:this.replaceByLine,flags:flagsStr)	{
								
									fileset(dir:tempSrcdir) {
																this.includes.split('\n').each {
																	if (it && it.trim().length() > 0) {
																		include(name:it.trim())
																		//println "include:"+it.trim()
																	}
																}
																this.excludes.split('\n').each {
																	if (it && it.trim().length() > 0) {
																		exclude(name:it.trim())
																		//println "exclude:"+it.trim()
																	}
																}
															}
														}
							
						}
						else {
							throw new Exception("["+this.class.canonicalName+"][action] Invalid replace text rule found: $it")
						}
					}
				}
			}
				/*ant.replaceregexp(match:"aaa",replace:"bbb",byline:"true",flags:"g")	{
						
							fileset(dir:tempSrcdir) {
														this.includes.split('\n').each {
															if (it && it.trim().length() > 0) {
																include(name:it.trim())
																println "include:"+it.trim()
															}
														}
														this.excludes.split('\n').each {
															if (it && it.trim().length() > 0) {
																exclude(name:it.trim())
																println "exclude:"+it.trim()
															}
														}
													}
												}*/
			
			
		}
				
		
			
		catch(Exception e){throw new Exception("["+this.class.canonicalName+"][action] ant replace fail\n"+e.getMessage())}
	

}

	def run(){
		try{
			def cr=0
			
			/*println dirOffsetStr
			println destDirStr
			println workDirStr
			
			println replaceText
			println setAttr
			println removeList
			println insertList
			println failWithoutMatch
			
			println includes
			println excludes
			println listZipFileRegExp
		
			println tempUnZipdir
			println tempSrcdir
			println workDirInstanceStr*/
			
			createWorkdir();
			
			createDestDir()
			loadSrcDirToWorkdir()

			
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