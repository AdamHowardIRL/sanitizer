node(){
    
    //Remember to setup String paramaters: GITURL and TOPFOLDER
    deleteDir()
    git credentialsId: 'adamhowaid', url: GITURL
   
    def allJobs
    
    //Specify the folder containing the folders with jobs. E.g. project_code for pipeline-common.
    String topLevelFolder =  TOPFOLDER
    dir(topLevelFolder){
        allJobs = bat(returnStdout: true, script: 'dir /s /b *Jenkinsfile*')
    }
    
    String str = allJobs;
    String findStr = "Jenkinsfile";
    int lastIndex = 0;
    int countJ = 0;

    //Count number of jobs to be created.
    while(lastIndex != -1){
        lastIndex = str.indexOf(findStr,lastIndex);

        if(lastIndex != -1){
             countJ++;
             lastIndex += findStr.length();
        }
    }
    int countFolders = 0
    def allFolders
    
    //Count folders containing jobs.
    dir(topLevelFolder){
        allFolders = bat(returnStdout: true, script: 'dir /b /o:n /a:d').trim().split("\\n")
    }
    
    allFolders[0] = ""
        
    
    for(String s : allFolders){
        println s
    }
    
    println allFolders.length
    
    println allJobs
    
    if(countJ > 0)
    countJ--
    
    println("NUM C: "  + countJ);

    String[] gitJenks = new String[countJ]
    String[] createFolders = new String[allFolders.length]
   
   
    dir (topLevelFolder){
        def jenks = bat(returnStdout: true, script: 'dir /s /b *Jenkinsfile*').trim().split("\n")
        createFolders = bat(returnStdout: true, script: 'dir /b /o:n /a:d').trim().split("\n")
 
        createFolders[0] = ""
        
        //Creates folders under top level
        for(int i = 1; i < createFolders.length; i++){
            println createFolders[i]
            jobDsl scriptText: "folder('" + createFolders[i].trim() + "')"
        }
        
        for(int i = 1; i < jenks.length; i++){
            println jenks[i]
        }

        int counter = 0;
        
        for(int j = 0; j < (jenks.length - 1) ; j++){
            for(int i = 0; i <= jenks[j + 1].length(); i++){
 
            if(jenks[j + 1].charAt(i) == '\\')
                counter++
                
            if(counter == 5){
                gitJenks[j] = jenks[j + 1].substring(i + 1)
                counter = 0 
                break;
            }
            }
        }
    }

    println "gitJenks.length " + gitJenks.length
    int numJobs = countJ;
    
    String[] jobNames = new String[numJobs]
    
    int counter = 0;
    
    //Extract jobs names. Folder name over Jenkinsfile.
    for(int j = 0; j < countJ; j++){
        String[] seperated = gitJenks[j].split("\\\\")    
         for(int i = 0; i < seperated.length; i++){
                for(String s : createFolders){
                    if(seperated[i] == s.trim()){
                        jobNames[j] = seperated[i + 1]
                     }
            }
        }
    }
    int cnt = 0

    //Change \ -> / and remove whitespace.
    try{
         for(int i = 0; i < gitJenks.length; i++){
            gitJenks[i] = gitJenks[i].replace('\\', '/')
            gitJenks[i] = gitJenks[i].trim()
         }
    } catch(Exception e){
    }
    
    for(String gj : gitJenks){
        println gj
    }
    
    //Change \ -> / and remove whitespace.
    try{
        for(int i = 0; i < jobNames.length; i++){
        jobNames[i] = jobNames[i].replace('\\', '/')
        jobNames[i] = jobNames[i].trim()
    }
    } catch (Exception e){
        
    }
    
    for(String c : createFolders){
        c = c.trim()
    }

    for(String j : jobNames){
        println "jobName[" + cnt+ "]" + j
        cnt++
    }
    
    int folderCount = 0;
    for(String s : createFolders){
        if(s.length() > 0)
            folderCount++
    }

    int numFolders = 0
    
    String jobs = allJobs;
    String search = "Jenkinsfile";
    int lstIndex = 0;
    int arInd = 0
    
    int[] countD = new int[folderCount + 1];
    countD[0] = 0
    countD[1] = 0
    
    String[] seper = allJobs.split("\\n")
    
    //Count number of jobs per folder
    for(int i = 1; i < createFolders.length; i++) {
        for(String s : seper){
            if(s.contains(createFolders[i].trim())){
                countD[arInd]++
            }
        }
        arInd++
    }                        
    println "Creating jobs now. Job DSL."
    int balancer = 0
    
    try{
        for(int i = 0; i < jobNames.length; i++){
        jobDsl scriptText: "pipelineJob('" + createFolders[(balancer + 1)].trim() + "/" + jobNames[i] +  "'){definition { cps { script(readFileFromWorkspace('" + gitJenks[i] + "'))}}}"
        if(i == (countD[balancer] - 1)){
            balancer++
        }
    }
    } catch (Exception e){
        println "Error creating jobs."
    }  
}  