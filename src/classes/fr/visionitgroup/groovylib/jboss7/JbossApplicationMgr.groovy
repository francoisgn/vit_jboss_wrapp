package fr.visionitgroup.groovylib.jboss7

import java.util.List;

import fr.visionitgroup.groovylib.jboss7.Jboss7Shell
import org.jboss.as.cli.scriptsupport.*

class JbossApplicationMgr {



                static def Boolean isApplicationExist(def Jboss7Shell cli,def String deployName){
                               def commandline
                               commandline="/deployment=$deployName:read-attribute(name=content)"
                               def CLI.Result result=cli.runCLICommand(commandline)
                               if (result.isSuccess()       ){
                                               return true
                               }else{
                                               return false
                               }
                }
                static def Boolean isApplicationExistOnScope(def Jboss7Shell cli,def String deployName,def scope){
                               def commandline
                               commandline="/server-group=$scope/deployment=$deployName:read-attribute(name=name)"
                               def CLI.Result result=cli.runCLICommand(commandline)
                               if (result.isSuccess()       ){

                                               return true
                               }
                               else{
                                               return false
                               }
                }
                static def Boolean isScopeExist(def Jboss7Shell cli,def scopeStr){
                               // scopeStr can be equal to : ""| server-group-name,server-group-name2|--all-server-group
                               println ("[I] isScopeExist check if  deployScope: $scopeStr  exist...")
                               if (!cli.isDomainMode()){
                                               // for standalone
                                               if(scopeStr != ""){
                                                               println "[E] isScopeExist : For standalone Server, scope must be empty"
                                                               return false
                                               }
                                               else{
                                                               return true
                                               }
                               }
                               else{
                                               // for domain
                                  if(scopeStr == "--all-server-group"){
                                                               return true;
                                  }
                                  else{
                                                  try{
                                                                  scopeStr.split(",").each{
                                                                                 def String scope=it
                                                                                 scope.replaceAll(/^ */, "")
                                                                                 scope.replaceAll(/ *$/, "")
                                                                                 def commandline="/server-group=$scope:read-attribute(name=profile)"
                                                                                 def CLI.Result result=cli.runCLICommand(commandline)
                                                                                 if (result.isSuccess()    ){
                                                                                                 println ("[I] isScopeExist deploy scope : $scope  exist")
                                                                                                 return true
                                                                                 }
                                                                                 else{
                                                                                                 throw new Exception ("[E] isScopeExist deploy scope : $scope doesn't exist" )
                                                                                 }
                                                                  }
                                                  }
                                                  catch(Exception e){
                                                                  println(e.getMessage())
                                                                  return false
                                                  }
                                                  return true
                                  }

                               }


                               return false
                }


                static def int deployOneApplication(def Jboss7Shell cli,def String deployName,def deployPathFile,def String deployScope, def String deployRunTimeName="", def String deployOptions){
                               def cr

                               // controle mandatory input argument

                               if (deployPathFile == null || (deployPathFile != null && deployPathFile == "")){ println "[E] deployOneApplication invalid argument: deployPathFile can't be empty"; return 1}
                               if (!cli.isDomainMode()){
                                               if (deployScope == null || (deployScope != null && deployScope == "")){ println "[E] deployOneApplication invalid argument: deployScope can't be empty"; return 1}
                               }

                               println "[I] Start deployOneApplication of $deployPathFile"

                               // control no mandatory argument
                               if (deployName == null || (deployName != null && deployName == "")){
                                               def java.io.File deployPathFileObj=new java.io.File(deployPathFile)
                                               deployName=deployPathFileObj.getName()
                                               println "[W] deployOneApplication: deployName is no defined, by default $deployName is used"
                               }

                               // test if deployment already exist, undeploy it
                               if (isApplicationExist(cli,deployName)){
                                               // undeploy the application on all scope if application already exist
                                               cr=undeployOneApplicationIfExist(cli,deployName)
                                               if(cr!= 0){
                                                               print "[E] Impossible to continue the deployment of /deployment=$deployName ($deployPathFile)"
                                                               return 1
                                               }
                               }

                               // start deployment

                               def commandline
                               if (!cli.isDomainMode()){
                                               // for standalone
                                               commandline="deploy $deployPathFile"
                                               if (deployRunTimeName != null && deployRunTimeName != "" ) { commandline+=" --runtime-name=$deployRunTimeName"}
                                               if (deployOptions != null && deployOptions != "" ) { commandline+=" $deployOptions"}
                               }
                               else{
                                               // for domain
                                               commandline="deploy $deployPathFile"


                                  if(deployScope == "--all-server-groups" ){
                                                               commandline+=" --all-server-groups"
                                  }
                                  else{
                                                  commandline+=" --server-groups=$deployScope"
                                  }


                                  if (deployRunTimeName != null && deployRunTimeName != "" ) { commandline+=" --runtime-name=$deployRunTimeName"}
                                  if (deployOptions != null && deployOptions != "" ) { commandline+=" $deployOptions"}
                               }

                               // execute  commandline
                               println "[I] Excute $commandline"
                               def CLI.Result result=cli.runCLICommand(commandline)
                               if (result.isSuccess()       ){
                                               println "[S] deploy of /deployment=$deployName ($deployPathFile) on  successfully"
                                               return 0
                               }
                               else{

                                               def response = result.getResponse()
                                               println("[E]: deploy /deployment=$deployName ($deployPathFile) FAILED:"+response.asString())
                                               return 1
                               }
                               return 1

                }



                static def int undeployOneApplication(def Jboss7Shell cli,def String deployName){
                               def commandline
                               if (deployName == null || (deployName != null && deployName == "")){ println "[E] undeployOneApplication invalid argument: deployName can't be empty"; return 1}

                               println "[I] Start undeployOneApplication of $deployName"
                               if (isApplicationExist(cli,deployName)){
                                               if (! cli.isDomainMode()){
                                                               // for standalone
                                                               commandline="undeploy --name=$deployName "
                                               }
                                               else{
                                                               // for domain
                                                               commandline="undeploy --name=$deployName --all-relevant-server-groups"
                                               }
                                               def CLI.Result result=cli.runCLICommand(commandline)
                                               if (result.isSuccess()       ){
                                                               println "[S] Undeploy /deployment=$deployName successfully"
                                                               return 0
                                               }
                                               else{

                                                               def response = result.getResponse()
                                                               println("[E]: Undeploy of /deployment=$deployName FAILED:"+response.asString())
                                                               return 1
                                               }
                               }
                               else{
                                               println "[E] /deployment=$deployName isn't deployed. No Undeploy action needed"
                                               return 1
                               }
                }


                static def int undeployOneApplicationIfExist(def Jboss7Shell cli,def String deployName){
                               def cr=0
                               if (deployName == null || (deployName != null && deployName == "")){ println "[E] undeployOneApplicationIfExist invalid argument: deployName can't be empty"; return 1}

                               println "[I] Start undeployOneApplicationIfExist of $deployName"
                               if (isApplicationExist(deployName)){
                                               cr=undeployOneApplication(cli,deployName)
                               }
                               else{
                                               println "[0] /deployment=$deployName isn't deploy. No Undeploy action needed"
                                               cr=0
                               }
                               return cr
                }

                static def int disableOneApplicationIfExist(def Jboss7Shell cli,def String deployName,def String[] deployScope){
                               def cr=0
                               if (deployName == null || (deployName != null && deployName == "")){ println "[E] disableOneApplicationIfExist invalid argument: deployName can't be empty"; return 1}
                               if (!cli.isDomainMode()){
                                               if (deployScope == null || (deployScope != null && deployScope == "")){ println "[E] disableOneApplicationIfExist invalid argument: deployScope can't be empty"; return 1}
                               }

                               println "[I] Start disableOneApplicationIfExist of $deployName"




                               // test if deployment already exist,
                               if (isApplicationExist(cli,deployName)){
                                               cr=disableOneApplication(cli,deployName,deployScope)
                               }
                               else{
                                               print "[E] /deployment=$deployName doesn't exist. Abort operation"
                                                               return 0
                               }
                               return cr
                }

                static def int disableOneApplication(def Jboss7Shell cli,def String deployName,def String[] deployScope){
                               def cr

                               // controle mandatory input argument

                               if (deployName == null || (deployName != null && deployName == "")){ println "[E] disableOneApplication invalid argument: deployName can't be empty"; return 1}
                               if (!cli.isDomainMode()){
                                               if (deployScope == null || (deployScope != null && deployScope == "")){ println "[E] disableOneApplication invalid argument: deployScope can't be empty"; return 1}
                               }

                               println "[I] Start disableOneApplication of $deployName"




                               // test if deployment already exist, undeploy it
                               if (!isApplicationExist(cli,deployName)){

                                                               print "[E] /deployment=$deployName doesn't exist. Abort operation"
                                                               return 1

                               }

                               // start deployment

                               def commandline
                               if (!cli.isDomainMode()){
                                               // for standalone
                                               commandline="undeploy --name=$deployName  --keep-content"
                               }
                               else{
                                               // for domain
                                               commandline="undeploy --name=$deployName  --keep-content"


                                                  if(deployScope == "--all-server-groups" ){
                                                                              commandline+=" --all-server-groups"
                                                  }
                                                  else{
                                                                  commandline+=" --server-groups=$deployScope"
                                                  }



                               }

                               // execute  commandline
                               println "[I] Excute $commandline"
                               def CLI.Result result=cli.runCLICommand(commandline)
                               if (result.isSuccess()       ){
                                               println "[S] disable of /deployment=$deployName   successfully"
                                               return 0
                               }
                               else{

                                               def response = result.getResponse()
                                               println("[E]: disable /deployment=$deployName  FAILED:"+response.asString())
                                               return 1
                               }
                               return 1


                }

                static def int enableOneApplication(def Jboss7Shell cli,def String deployName,def String[] deployScope){
                               // controle mandatory input argument

                               if (deployName == null || (deployName != null && deployName == "")){ println "[E] enableOneApplication invalid argument: deployName can't be empty"; return 1}
                               if (!cli.isDomainMode()){
                                               if (deployScope == null || (deployScope != null && deployScope == "")){ println "[E] enableOneApplication invalid argument: deployScope can't be empty"; return 1}
                               }

                               println "[I] Start enableOneApplication of $deployName"




                               // test if deployment already exist, undeploy it
                               if (!isApplicationExist(cli,deployName)){
                                                               print "[E] /deployment=$deployName doesn't exist. Abort operation"
                                                               return 1
                               }

                                // start deployment

                               def commandline
                               if (!cli.isDomainMode()){
                                               // for standalone
                                               commandline="deploy --name=$deployName"
                               }
                               else{
                                               // for domain
                                               commandline="deploy --name=$deployName"


                                                  if(deployScope == "--all-server-groups" ){
                                                                              commandline+=" --all-server-groups"
                                                  }
                                                  else{
                                                                  commandline+=" --server-groups=$deployScope"
                                                  }



                               }

                               // execute  commandline
                               println "[I] Excute $commandline"
                               def CLI.Result result=cli.runCLICommand(commandline)
                               if (result.isSuccess()       ){
                                               println "[S] enable of /deployment=$deployName   successfully"
                                               return 0
                               }
                               else{

                                               def response = result.getResponse()
                                               println("[E]: enable of /deployment=$deployName  FAILED:"+response.asString())
                                               return 1
                               }
                               return 1

                }
                static def int enableOneApplicationIfExist(def Jboss7Shell cli,def String deployName,def String[] deployScope){
                               def cr=0
                               if (deployName == null || (deployName != null && deployName == "")){ println "[E] enableOneApplicationIfExist invalid argument: deployName can't be empty"; return 1}
                               if (!cli.isDomainMode()){
                                               if (deployScope == null || (deployScope != null && deployScope == "")){ println "[E] enableOneApplicationIfExist invalid argument: deployScope can't be empty"; return 1}
                               }

                               println "[I] Start enableOneApplicationIfExist If exist of $deployName"

                               // test if deployment already exist,
                               if (isApplicationExist(cli,deployName)){
                                               cr=enableOneApplication(cli,deployName,deployScope)
                               }
                               else{
                                               print "[E] /deployment=$deployName doesn't exist. Abort operation"
                                                               return 0
                               }
                               return cr
                }




                static def List getListOfApplicationByRegexp(def Jboss7Shell cli, def String regexp){

                               def List applicationLst=[]
                               def regexpStr=regexp
                               def commandline="/deployment=*:read-attribute(name=name)"
                               def CLI.Result result
                               result=cli.runCLICommand(commandline)
                               if (result.isSuccess()       ){
                                               def response = result.getResponse()
                                               def List ApplicationTab=response.get("result").asList()

                                               ApplicationTab.each{
                                                               def applicationName=it.get("result").asString()

                                                               if(applicationName =~ regexpStr){
                                                                              applicationLst.add( applicationName)
                                                               }
                                               }

                               }
                               else{

                                               def response = result.getResponse()
                                               println("[E]: Impossible to generate the list of all Application"+response.asString())
                                               return null
                               }
                               return applicationLst

                }
                /***********
                //deployOneApplication(cli,def String deployName,def deployPathFile,def String deployScope, def String deployRunTimeName="", def String deployOptions)
                static def deployMultipleApplications(def Jboss7Shell cli,def List deployNameLst,def deployPathDir,def String deployScope, def String deployOptions){
                               def cr=0

                               if (deployNameLst == null || (deployNameLst != null && deployNameLst=="" ) ){ println "[E] deployGroupOfApplications invalid argument: deployNameLst can't be empty"; return 1}
                               if (deployPathDir == null || (deployPathDir != null && deployPathDir=="" ) ){ println "[E] deployGroupOfApplications invalid argument: deployPathDir can't be empty"; return 1}
                               def File deployPathDirFile=new File(deployPathDir)
                               if(!deployPathDirFile.isDirectory()){
                                               println "[E] deployGroupOfApplications: The directory $deployPathDir doesnt exist or isn't a directory"
                                               return 1
                               }

                                println "[I] Start exportGroupOfApplications "

                                deployNameLst.each{ deployName ->
                                               def deployPathFile
                                               def deployRunTimeName
                                               cr=deployOneApplication(cli,deployName,deployPathFile,deployScope, deployRunTimeName, deployOptions)

                                }
                               println "[S] End  deployGroupOfApplications  "
                               return 0
                }
                ****************/
                static def int undeployMultipleApplications(def Jboss7Shell cli,def List deployNameLst){
                               def cr=0

                               if (deployNameLst == null ){ println "[E] undeployMultipleApplications invalid argument: deployNameLst can't be empty"; return 1}

                                println "[I] Start undeployMultipleApplications "

                                deployNameLst.each{ deployName ->
                                               if (!isApplicationExist(cli,deployName)){
                                                               print "[E] /deployment=$deployName doesn't exist. Abort operation"
                                                               println "[E] End  undeployMultipleApplications : FAILED "
                                                               return 1
                                               }
                                               else{
                                                               cr=undeployOneApplication(cli,deployName)
                                                               if (cr!= 0){
                                                                              println "[E] End  undeployMultipleApplications : FAILED "
                                                                              return 1
                                                               }
                                               }
                               }
                               println "[S] End  undeployMultipleApplications  "
                               return  0
                }
                static  def int undeployGroupOfApplications(def Jboss7Shell cli,def String groupName){
                               def cr=0
                               def regexpGroupName="^$groupName-.*\$"
                               println "[I] Start undeployGroupOfApplications "
                               // make a list of application that name match with regexpGroupName
                               def List ApplicationOfGroup=getListOfApplicationByRegexp(cli,regexpGroupName)
                               if (ApplicationOfGroup.size() ==0){
                                               println "[E] undeployGroupOfApplications :No application exist for group:$regexpGroupName"
                                               cr=1
                               }
                               else{
                                               cr=undeployMultipleApplications(cli,ApplicationOfGroup)
                               }
                               if(cr==0){
                                               println "[S] End undeployGroupOfApplications "
                               }
                               else{
                                               println "[E] End undeployGroupOfApplications "
                               }
                               return cr
                }
                static  def Boolean isExistGroupOfApplications(def Jboss7Shell cli,def String groupName){
                               def regexpGroupName="^$groupName-.*\$"
                               def List ApplicationOfGroup=getListOfApplicationByRegexp(cli,regexpGroupName)
                               if (ApplicationOfGroup.size() ==0){
                                               return false
                               }
                               else{
                                               return true
                               }

                }
                static def int enableMultipleApplications(def Jboss7Shell cli,def List deployNameLst){
                               def cr=0

                               if (deployNameLst == null ){ println "[E] enableMultipleApplications invalid argument: deployNameLst can't be empty"; return 1}

                                println "[I] Start enableMultipleApplications "

                                deployNameLst.each{ deployName ->
                                               if (!isApplicationExist(cli,deployName)){
                                                               print "[E] /deployment=$deployName doesn't exist. Abort operation"
                                                               println "[E] End  enableMultipleApplications : FAILED "
                                                               return 1
                                               }
                                               else{
                                                               cr=enableOneApplication(cli,deployName)
                                                               if (cr!= 0){
                                                                              println "[E] End  enableMultipleApplications : FAILED "
                                                                              return 1
                                                               }
                                               }
                               }
                               println "[S] End  enableMultipleApplications  "
                               return  0
                }
                static def int enableGroupOfApplications(def Jboss7Shell cli,def String groupName){
                               def cr=0
                               def regexpGroupName="^$groupName-.*\$"
                               println "[I] Start enableGroupOfApplications "
                               // make a list of application that name match with regexpGroupName
                               def List ApplicationOfGroup=getListOfApplicationByRegexp(cli,regexpGroupName)
                               if (ApplicationOfGroup.size() ==0){
                                               println "[E] enableGroupOfApplications :No application exist for group:$regexpGroupName"
                                               cr=1
                               }
                               else{
                                               cr=enableMultipleApplications(cli,ApplicationOfGroup)
                               }
                               if(cr==0){
                                               println "[S] End enableGroupOfApplications "
                               }
                               else{
                                               println "[E] End enableGroupOfApplications "
                               }
                               return cr
                }

                static def int disableMultipleApplications(def Jboss7Shell cli,def List deployNameLst){
                               def cr=0

                               if (deployNameLst == null ){ println "[E] disableMultipleApplications invalid argument: deployNameLst can't be empty"; return 1}

                                println "[I] Start disableMultipleApplications "

                                deployNameLst.each{ deployName ->
                                               if (!isApplicationExist(cli,deployName)){
                                                               print "[E] /deployment=$deployName doesn't exist. Abort operation"
                                                               println "[E] End  disableMultipleApplications : FAILED "
                                                               return 1
                                               }
                                               else{
                                                               cr=disableOneApplication(cli,deployName)
                                                               if (cr!= 0){
                                                                              println "[E] End  disableMultipleApplications : FAILED "
                                                                              return 1
                                                               }
                                               }
                               }
                               println "[S] End  disableMultipleApplications  "
                               return  0
                }
                static def int disableGroupOfApplications(def Jboss7Shell cli,def String groupName){
                               def cr=0
                               def regexpGroupName="^$groupName-.*\$"
                               println "[I] Start disableGroupOfApplications "
                               // make a list of application that name match with regexpGroupName
                               def List ApplicationOfGroup=getListOfApplicationByRegexp(cli,regexpGroupName)
                               if (ApplicationOfGroup.size() ==0){
                                               println "[E] disableGroupOfApplications :No application exist for group:$regexpGroupName"
                                               cr=1
                               }
                               else{
                                               cr=disableMultipleApplications(cli,ApplicationOfGroup)
                               }
                               if(cr==0){
                                               println "[S] End disableGroupOfApplications "
                               }
                               else{
                                               println "[E] End disableGroupOfApplications "
                               }
                               return cr
                }
                static def disableGroupOfApplications(){

                }
                static def int exportOneApplication(def Jboss7Shell cli,def String deployName, def String path_exportdir_destination){
                               def cr=0
                               def commandlineForHash
                               def commandlineForJbossContentPath

                               if (deployName == null || (deployName != null && deployName == "")){ println "[E] exportOneApplication invalid argument: deployName can't be empty"; return 1}

                               def File exportDirFile=new File(path_exportdir_destination)
                               if(!exportDirFile.isDirectory()){
                                               println "[E] exportOneApplication invalid argument: $path_exportdir_destination doesn't exist or isn't a directory";
                                               return 1
                               }

                               println "[I] Start exportOneApplication If exist of $deployName"

                               if (!isApplicationExist(cli,deployName)){
                                               print "[E] /deployment=$deployName doesn't exist. Abort operation"
                   return 1
                               }

                               // catch run-time name
                               def commandlineForRuntimeName="/deployment=$deployName:read-attribute(name=runtime-name)"
                               def runtimeName
                               def CLI.Result result=cli.runCLICommand(commandlineForJbossContentPath)
                               if (result.isSuccess()       ){
                                               def response = result.getResponse()
                                               commandlineForRuntimeName=response.get("result").asString()
                                               println "[I] RuntimeName: $commandlineForRuntimeName   "

                               }
                               else{

                                               def response = result.getResponse()
                                               println("[E]: Impossible to get Jboss Context Dir path:"+response.asString())
                                               return 1
                               }

                               // catch the JbossContentPath
                               def path_jboss_content_dir=""
                               if (!cli.isDomainMode()){
                                               // for standalone
                                               commandlineForJbossContentPath="/core-service=server-environment:read-attribute(name=content-dir)"
                               }
                               else{
                                               // for domain
                                               commandlineForJbossContentPath="/host=master/core-service=host-environment/:read-attribute(name=domain-content-dir)"
                               }

                               result=cli.runCLICommand(commandlineForJbossContentPath)
                               if (result.isSuccess()       ){
                                               def response = result.getResponse()
                                               path_jboss_content_dir=response.get("result").asString()
                                               println "[I] JbossContentPath: $path_jboss_content_dir   "

                               }
                               else{

                                               def response = result.getResponse()
                                               println("[E]: Impossible to get Jboss Context Dir path:"+response.asString())
                                               return 1
                               }


                               // catch the application hash number store inside Jboss Repository
                               def hashCode=""

                               // same for standalone and domaine
                               commandlineForHash="/deployment=$deployName:read-attribute(name=content)"
                               def path_application_content=""

                               result=cli.runCLICommand(commandlineForHash)
                               if (result.isSuccess()       ){
                                               def response = result.getResponse()


                                               def resultInfos=response.get("result")
                                               def Byte[] hashByteInfos=resultInfos.get(0).get("hash").asBytes()
                                               def sizeHash=hashByteInfos.size()
                                               println "sizeHash=$sizeHash"
                                               def parentDir=String.format("%02x", hashByteInfos[0]&0xff)

                                               def strHash=""
                                               for(def i=1;i<sizeHash;i++){
                                                               strHash+=String.format("%02x", hashByteInfos[i]&0xff)
                                               }

                                               path_application_content=path_jboss_content_dir+System.getProperty("line.separator")+parentDir+System.getProperty("line.separator")+strHash
                                               println "[I] Application Content path: $path_application_content   "


                                               path_jboss_content_dir=response.get("result").asString()
                                               println "[I] JbossContentPath: $path_jboss_content_dir   "

                               }
                               else{

                                               def response = result.getResponse()
                                               println("[E]: Impossible to get Hash value associted to /deployment=$deployName:"+response.asString())
                                               return 1
                               }

                   // copy  to dest file
                   def copy = { File src,File dest->

                                                  def input = src.newDataInputStream()
                                                  def output = dest.newDataOutputStream()

                                                  output << input

                                                  input.close()
                                                  output.close()
                   }

                   def File path_export_runtime_dir = new File (path_exportdir_destination,runtimeName)
                   def File srcFile  = new File(path_application_content)
                   def File destFile = new File(path_export_runtime_dir,deployName)
                   try{




                                  if (! path_export_runtime_dir.isDirectory()){
                                                  path_export_runtime_dir.mkdir()
                                  }

                                  copy(srcFile,destFile)
                   }
                   catch(Exception e){
                                  println("[E]: Impossible to copy  :"+srcFile.absolutePath+" to "+destFile.absolutePath+" :" +e.getMessage()+":\n"+e.printStackTrace())
                                  return 1
                   }

                   return 0
                }


                static def int exportMultipleApplications(def Jboss7Shell cli,def List deployNameLst,def String path_exportdir_destination){
                               def cr=0

                               if (deployNameLst == null ){ println "[E] exportMultipleApplications invalid argument: deployNameLst can't be empty"; return 1}

                               println "[I] Start exportMultipleApplications "

                               deployNameLst.each{ deployName ->
                                               if (!isApplicationExist(cli,deployName)){
                                                               print "[E] /deployment=$deployName doesn't exist. Abort operation"
                                                               println "[E] End  exportMultipleApplications : FAILED "
                                                               return 1
                                               }
                                               else{
                                                               cr=undeployOneApplication(cli,deployName)
                                                               if (cr!= 0){
                                                                              println "[E] End  exportMultipleApplications : FAILED "
                                                                              return 1
                                                               }
                                               }
                               }
                               println "[S] End  exportMultipleApplications  "
                               return 0

                }
                static def int exportGroupOfApplications(def Jboss7Shell cli,def String groupName,def String path_exportdir_destination){
                               def cr=0
                               def regexpGroupName="^$groupName-.*\$"
                               println "[I] Start exportGroupOfApplications "
                               // make a list of application that name match with regexpGroupName
                               def List ApplicationOfGroup=getListOfApplicationByRegexp(cli,regexpGroupName)
                               if (ApplicationOfGroup.size() ==0){
                                               println "[E] exportGroupOfApplications :No application exist for group:$regexpGroupName"
                                               cr=1
                               }
                               else{
                                               cr=exportMultipleApplications(cli,ApplicationOfGroup,path_exportdir_destination)
                               }
                               if(cr==0){
                                               println "[S] End exportGroupOfApplications "
                               }
                               else{
                                               println "[E] End exportGroupOfApplications "
                               }
                               return cr
                }

                static def int backupGroupOfApplications(){

                }
                static def int restoreGroupOfApplications(){

                }

                static def int backupEnv(){

                }
                static def int restoreEnv(){

                }

                static def int statusOfOneApplication(def Jboss7Shell cli,def String deployName,def String[] deployScope){

                }

                static def int waitForStatusOfOneApplication(def Jboss7Shell cli,def String deployName,def String[] deployScope){

                }

                static def int  statusOfGroupOfApplications(def Jboss7Shell cli,def String deployName,def String[] deployScope){

                }

                static def int waitForStatusOfGroupOfApplications(def Jboss7Shell cli,def String deployName,def String[] deployScope){

                }

                /********
                static def int deployGroupPackage(def Jboss7Shell cli,def File path_GroupPackage_src_dir, def File workdir){
                               def cr=0

                               if (deploymentPropertiesLst == null ){ println "[E] deployMultiplePackagedApplications invalid argument: deploymentPropertiesLst can't be empty"; return 1}

                                println "[I] Start deployMultiplePackagedApplications "

                                deploymentPropertiesLst.each{ deploymentProperties ->
                                               cr=deployOnePackagedApplication(cli,deploymentProperties)
                                               if (cr!=0){
                                                               println "[E] End  deployMultiplePackagedApplications : FAILED "
                                                               return 1
                                               }

                                }
                               println "[S] End  deployMultiplePackagedApplications  "
                               return 0
                }
                **************/
                static def int deployMultiplePackagedApplications(def Jboss7Shell cli,def List  deploymentPropertiesLst){
                               def cr=0

                               if (deploymentPropertiesLst == null ){ println "[E] deployMultiplePackagedApplications invalid argument: deploymentPropertiesLst can't be empty"; return 1}

                                println "[I] Start deployMultiplePackagedApplications "
                               try{
                                               deploymentPropertiesLst.each{ deploymentProperties ->
                                                               cr=deployOnePackagedApplication(cli,deploymentProperties)
                                                               if (cr!=0){
                                                                                throw new Exception("[E] End  deployMultiplePackagedApplications : FAILED ")
                                                               }

                                                }
                               }
                               catch(Exception e){

                                                println e.getMessage()
                                               return 1
                               }
                               println "[S] End  deployMultiplePackagedApplications  "
                               return 0
                }
                /* deployOnePackagedApplication (cli,deploymentProperties)
                * deploymentProperties:
                *   *mandatory
                *   *deployOption_name=xxxx
                *   deployOption_runtime-name=xxxx
                *   deployOption_others=xxxx
                *   *deployOption_scope=
                *   *deployOption_src-path="
                *   deployOption_env-path=
                */
                static def int deployOnePackagedApplication(def Jboss7Shell cli,def java.util.Properties deploymentProperties){
                               def cr
                               if (deploymentProperties == null){ println "[E] deployOnePackagedApplication invalid argument: deploymentProperties can't be empty"; return 1}
                               def String deployRunTimeName=deploymentProperties.getProperty("deployOption_runtime-name", "")
                               def String deployOptions=deploymentProperties.getProperty("deployOption_others", "")
                               def String deployScope=deploymentProperties.getProperty("deployOption_scope", "")
                               def deployPathSrcFile=deploymentProperties.getProperty("deployOption_src-path", "")
                               def deployPathEnvFile=deploymentProperties.getProperty("deployOption_env-path", "")
                               def deployName=deploymentProperties.getProperty("deployOption_name", "")

                               // controle mandatory input argument
                               println "[I] Start deployOnePackagedApplication of $deployPathSrcFile"
                               if ( deployName == ""){ println "[E] deployOnePackagedApplication missing property: deployOption_name can't be empty"; return 1}
                               if ( deployPathSrcFile == ""){ println "[E] deployOnePackagedApplication missing property: deployOption_src-path can't be empty"; return 1}
                               if (!cli.isDomainMode()){
                                               if ( deployScope == ""){ println "[E] deployOnePackagedApplication missing property: deployOption_scope can't be empty"; return 1}
                               }


                               println "[D] deployOnePackagedApplication deploymentProperties:"+deploymentProperties
                               // control no mandatory argument
                               /*
                               if (deployName == null || (deployName != null && deployName == "")){
                                               def java.io.File deployPathFileObj=new java.io.File(deployPathSrcFile)
                                               deployName=deployPathFileObj.getName()
                                               println "[W] deployOnePackagedApplication: deployName is no defined, by default $deployName is used"
                               }*/

                               println "[D] deployOnePackagedApplication deployName:"+deployName
                               // test if deployment already exist, undeploy it
                               if (isApplicationExist(cli,deployName)){
                                               // undeploy the application on all scope if application already exist
                                               cr=undeployOneApplicationIfExist(cli,deployName)
                                               if(cr!= 0){
                                                               print "[E] deployOnePackagedApplication Impossible to continue the deployment of /deployment=$deployName ($deployPathSrcFile)"
                                                               return 1
                                               }
                               }


                               println "[I] deployOnePackagedApplication property :deployOption_scope=$deployScope"
                               println "[I] deployOnePackagedApplication property :deployOption_name=$deployName"
                               println "[I] deployOnePackagedApplication property :deployOption_runtime-name=$deployRunTimeName"
                               println "[I] deployOnePackagedApplication property :deployOption_others=$deployOptions"
                               println "[I] deployOnePackagedApplication property :deployOption_src-path=$deployPathSrcFile"
                               println "[I] deployOnePackagedApplication property :deployOption_env-path=$deployPathEnvFile"



                               // manage deployment's environment
                               if(deployPathEnvFile!= ""){
                                               def File f=new File(deployPathEnvFile)
                                               if (f.isFile()){
                                                               def deployPathEnvFileToString=f.text
                                                               cr=undeployEnvironment(cli,deployPathEnvFileToString)
                                                               if(cr!=0){
                                                                              print "[W] deployOnePackagedApplication undeploy deployment's environment failed. Abort..."

                                                               }
                                                               cr=deployEnvironment(cli,deployPathEnvFileToString)
                                                               if(cr!=0){
                                                                              print "[E] deployOnePackagedApplication deploy deployment's environment failed. Abort..."
                                                                              return 1
                                                               }
                                               }
                                               else{
                                                               print "[E] deployOnePackagedApplication deployment's environment($deployPathEnvFile) doesn't exist. Abort..."
                                                               return 1
                                               }
                               }

                               //
                               //deployPathSrcFile=deployPathSrcFile.replace("\\","/");
                               // start deployment
                               def commandline
                               if (!cli.isDomainMode()){
                                               // for standalone
                                               commandline="deploy $deployPathSrcFile"
                                               if ( deployRunTimeName != "" ) { commandline+=" --runtime-name=$deployRunTimeName"}
                                               if ( deployOptions != "" ) { commandline+=" $deployOptions"}
                               }
                               else{
                                               // for domain
                                               commandline="deploy $deployPathSrcFile"


                                  if(deployScope == "--all-server-groups" ){
                                                               commandline+=" --all-server-groups"
                                  }
                                  else{
                                                  commandline+=" --server-groups="+deployScope.replaceAll(/^ */, "").replaceAll(/ *$/, "").replaceAll(/ *, */, ",")
                                  }


                                  if (deployRunTimeName != null && deployRunTimeName != "" ) { commandline+=" --runtime-name=$deployRunTimeName"}
                                  if (deployOptions != null && deployOptions != "" ) { commandline+=" $deployOptions"}
                               }

                               // execute  commandline
                               println "[I] deployOnePackagedApplication Execute $commandline"
                               try{
                                               def CLI.Result result=cli.runCLICommand(commandline)
                                               if (result.isSuccess()       ){
                                                               println "[S] deployOnePackagedApplication deploy of /deployment=$deployName ($deployPathSrcFile) on  successfully"
                                                               return 0
                                               }
                                               else{

                                                               def response = result.getResponse()

                                                               println("[E]: deployOnePackagedApplication deploy /deployment=$deployName ($deployPathSrcFile) FAILED:"+response.asString())

                                                               return 1
                                               }
                               }
                               catch(Exception e){
                                               println("[E]: deployOnePackagedApplication deploy /deployment=$deployName ($deployPathSrcFile) FAILED:"+e.getMessage())
                               }
                               return 1

                }

}




