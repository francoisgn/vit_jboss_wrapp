package fr.nauzikaa.utils.zip;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream

public class ZipTools {
	static{
		bootstrapPluginZipTools()
	  }
	
	private static void bootstrapPluginZipTools(){
		//zipping methods here
		
	 
		//define zip closure first, then allocate it
		//it is needed to make the recursion work
		def zip
		zip = { ZipOutputStream zipOutStream,File f , String path->
		   def name = (path.equals(""))?f.name:path + File.separator + f.name
		   if(!f.isDirectory() ){
			 def entry = new ZipEntry(name)
			 zipOutStream.putNextEntry(entry)
			 new FileInputStream(f).withStream { inStream ->
			   def buffer = new byte[1024]
			   def count
			   while((count = inStream.read(buffer, 0, 1024)) != -1) {
				 zipOutStream.write(buffer,0,count)
			   }
			 }
			 zipOutStream.closeEntry()
		   }
		   else {
			 //write the directory first, in order to allow empty directories
			 def entry = new ZipEntry(name +"/")
			 zipOutStream.putNextEntry(entry)
			 zipOutStream.closeEntry()
			 f.eachFile{
			   //recurse
			   zip(zipOutStream,it,name)
			 }
		   }
		}
		def zipdir
		zipdir = { ZipOutputStream zipOutStream,File f , String path->
			def name = (path.equals(""))?f.name:path + File.separator + f.name
			if(!f.isDirectory() ){
			  def entry = new ZipEntry(name)
			  zipOutStream.putNextEntry(entry)
			  new FileInputStream(f).withStream { inStream ->
				def buffer = new byte[1024]
				def count
				while((count = inStream.read(buffer, 0, 1024)) != -1) {
				  zipOutStream.write(buffer,0,count)
				}
			  }
			  zipOutStream.closeEntry()
			}
			else {
			  ////write the directory first, in order to allow empty directories
			  //def entry = new ZipEntry(name +"/")
			  //zipOutStream.putNextEntry(entry)
			  //zipOutStream.closeEntry()
			  f.eachFile{
				//recurse
				 // println "name2="+it.name
				zip(zipOutStream,it,"")
			  }
			}
		 }
	File.metaClass.zipdir = { String destination ->
	   //cache the delegate (the File Object) as it will be modified
	   //in the withStream closure
	   def input = delegate
	   def result = new ZipOutputStream(new FileOutputStream(destination))
	   result.withStream {zipOutStream->
		 //recursively zip files
		 zipdir(zipOutStream,input,"")
	   }
	}
	File.metaClass.zip = { String destination ->
		//cache the delegate (the File Object) as it will be modified
		//in the withStream closure
		def input = delegate
		def result = new ZipOutputStream(new FileOutputStream(destination))
		result.withStream {zipOutStream->
		  //recursively zip files
		  zip(zipOutStream,input,"")
		}
	 }
	
	//unzipping methods here
	
	
	File.metaClass.unzip = { String dest ->
		//in metaclass added methods, 'delegate' is the object on which
		//the method is called. Here it's the file to unzip
		def result = new ZipInputStream(new FileInputStream(delegate))
		def destFile = new File(dest)
		if(!destFile.exists()){
		  destFile.mkdir();
		}
		result.withStream{
		  def entry
		  while(entry = result.nextEntry){
			  if (!entry.isDirectory()){
				  new File(dest + File.separator + entry.name).parentFile?.mkdirs()
				  def output = new FileOutputStream(dest + File.separator
													+ entry.name)
				  output.withStream{
					int len = 0;
					byte[] buffer = new byte[4096]
					while ((len = result.read(buffer)) > 0){
					  output.write(buffer, 0, len);
					}
				  }
			   }
			   else {
				 println dest + File.separator + entry.name
				   new File(dest + File.separator + entry.name).canonicalFile.mkdirs()
			   }
		  } //while
		}//result.withStream
	  }//File.metaClass.unzip
	}
}