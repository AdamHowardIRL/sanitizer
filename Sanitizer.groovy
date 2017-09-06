package adam.howard

import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.sql.Timestamp
import java.util.ArrayList
import java.util.Collection
import java.util.HashMap
import java.util.Iterator
import java.util.Scanner
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * @author Adam Howard
 * @since 4/6/2017
 * @version 1.0.0.0
 *
 */
public class Sanitizer {
	//Locals
	public static String jenkinsFile = ""
	public static String whiteList = ""
	public static String blackList = ""
	private static def linesMand = new ArrayList<Integer>()
	private static def linesBlack = new ArrayList<Integer>()
	private static boolean verboseSwitch = false
	private static def mandMap = new HashMap<String, Boolean>()
	private static def blackMap = new HashMap<String, Boolean>()
	private static def mandLinesFound = new ArrayList<Integer>()
	private static def blackLinesFound = new ArrayList<Integer>()
	private static def blackNameFound = new ArrayList<String>()
	private static def mandNameFound = new ArrayList<String>()
	private static final String helpSwitch = "-h"
	private static final String mandSwitch = "-m"
	private static final String blackSwitch = "-b"
	private static final String jenkSwitch = "-j"
	private static final String verbSwitch = "-v"
	private static boolean wantHelp = false
	private static final String whitelistedChars = "[^a-z0-9A-Z-.\\[\\]]"
	private static char[] acceptedLeft = [':', '(', '[', ' ', '{', '@','/','\\',';','#','$','*','\"','\'',']','&','!','+','?','|',';',')','.','=','=','='];
	private static char[] acceptedRight = [':', ')', ']', ' ', '}', '@','/','\\',';','#','$','*','\"','\'','[','&','!','+','?','|',';','(','.','=','=','='];
	private static File jenkFile = null
	private static File mand = null
	private static File black = null
	private static String[] lineNumber = ""
	private static Scanner whiteScan = null
	private static Scanner blackScan = null
	private static Scanner jenkScan = null
	private static def white = new ArrayList<String>()
	private static def blackArray = new ArrayList<String>()
	private static int exitCode = 0
	private static boolean isMandatoryThere = false
	private static boolean isBlacklistedThere = false
	private static int foundLineCounter = 0
	private static ArrayList<Integer> commentedLines = new ArrayList<Integer>();
	private static boolean whitelistedEmpty = false
	private static boolean blacklistedEmpty = false
	/**The bulk of the program
	 * @param args Command line arguments
	 */
	//@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		try{
			//Parse arguments and fill variables.
			for(int i = 0; i < args.length; i++){
				//Input Verification. Removes any characters not in whitelistedChars.
				args[i] = args[i].replaceAll(whitelistedChars, "");

				if (args[i].contains(jenkSwitch))
					jenkinsFile = args[i+1]
				else if (args[i].equalsIgnoreCase(blackSwitch))
					blackList = args[i+1]
				else if	(args[i].equalsIgnoreCase(mandSwitch) || args[i].equalsIgnoreCase("-w"))
					whiteList = args[i+1]
				else if (args[i].equalsIgnoreCase(helpSwitch) || args[i].equalsIgnoreCase("/?"))
					wantHelp = true
				else if (args[i].equalsIgnoreCase(verbSwitch))
					verboseSwitch = true
				else if (args.length == 0)
					printUsage()
			}
		} catch (Exception ex){
			println("Problem reading arguments")
			//printUsage()
		}
		if(wantHelp){
			println("This program requires three files as arguments. \n ")
			printUsage()
		}
		//When verbose, print files that have been inputted.
		if(verboseSwitch){
			println(getTimeStampNow() + " INFO: File to test: " + jenkinsFile)
			println(getTimeStampNow() + " INFO: File whitelist: " + whiteList)
			println(getTimeStampNow() + " INFO: File blacklist: " + blackList)
		}
		//Print that file are about to be loaded.
		if(verboseSwitch)
			println("\n" + getTimeStampNow() + " Loading files\n")
		//Load the files
		try{
			jenkFile = new File(jenkinsFile)
			mand = new File(whiteList)
			black = new File(blackList)
		} catch (Exception e){
			printUsage()
			return
		}
		//Mandatory and blacklisted converted to ArrayList		
		//Scanners
		try {
			jenkScan = new Scanner(jenkFile)
			if(verboseSwitch)
				println(getTimeStampNow().toString() + " File to test read")
		} catch (Exception e) {
			if(!wantHelp){
				printUsage()
				println("ERROR: Could not load the file to test")
			}
			//return
		}
		try{
			whiteScan = new Scanner(mand)
			if(verboseSwitch)
				println(getTimeStampNow().toString() + " Whitelisted file read")
		} catch (Exception ex) {
			//println("ERROR: Could not load whitelisted file")
			//return
		}
		try{
			blackScan = new Scanner(black)
			if(verboseSwitch)
				println(getTimeStampNow().toString() + " Blacklisted file read")
		} catch (Exception ex) {
			//println("ERROR: Could not load blacklisted file")
			//return
		}
		//Load files
		try{
			//Fill Maps
			while(whiteScan.hasNextLine()){
				String line = whiteScan.nextLine()
				String place = null
				white.add(line)
				mandMap.put(line, false)
			}
		} catch(Exception e){
			//println("Problem loading whitelisted file to program")
		}
		
		try{
			while(blackScan.hasNextLine()){
				String line = blackScan.nextLine()
				String place = null
				blackArray.add(line)
				blackMap.put(line, false)
			}
		} catch (Exception e){
			//println("Problem loading blacklisted file to program");
		}
		
		
		try{
			if(isFileEmpty(jenkFile)){
				println("File to test is empty")
			}
			if(isFileEmpty(mand)){
				println("Whitelisted file is empty")
				whitelistedEmpty = true
			}
			if(isFileEmpty(black)){
				println("Blacklisted file is empty")
				blacklistedEmpty = true
			}
		} catch (Exception e){
		}
		
		
		removeDuplicates()
		findCommentedLines()
		if(verboseSwitch){
			println("\n\t\tBEFORE\n")
			println(getTimeStampNow() + " INFO: Whitelisted entries\n ")
			printMandMap()
			println("\n" + getTimeStampNow() + " INFO: Blacklisted entries\n ")
			printBlackMap()
			println("\nBeginning search")
			println("\n")
		}
		//Start of search. Whitelisted. For each mandatory word it makes 5 different checks between spaces, between single quotes, between (),
		//between [], between {}
		
		if(white.size > 0 && !whitelistedEmpty){
			for(String cur : white){
				if(checkWordSides(cur, true)){
					mandMap.replace(cur,  true)
					for(String s: lineNumber){
						mandNameFound.add(cur)
						mandLinesFound.add(s)
					}
				}
				if(findWordInFile(cur, jenkinsFile)){
					mandMap.replace(cur,  true)
					for(String s: lineNumber){
						mandNameFound.add(cur)
						mandLinesFound.add(s)
					}
				}
			}
		}
		//Blacklisted search. REGEX and space delimiter test per line.
		foundLineCounter = 0
		
		if(blackArray.size > 0 && !blacklistedEmpty){
			for(String cur : blackArray){
				if(checkWordSides(cur, false)){
					blackMap.replace(cur,  true)
					for(String s: lineNumber){
						blackNameFound.add(cur)
						blackLinesFound.add(s)
					}
				}
				if(findWordInFile(cur, jenkinsFile)){
					blackMap.replace(cur,  true)
					for(String s: lineNumber){
						blackNameFound.add(cur)
						blackLinesFound.add(s)
					}
				}
			}
		}
		//After the file has been searched, output results.
		if(verboseSwitch){
			println("\n\t\tAFTER\n")
			println(getTimeStampNow() + " INFO: Whitelisted entries\n ")
			printMandMap()
			println("\n" + getTimeStampNow() + " INFO: Blacklisted entries\n ")
			printBlackMap()
		}
		//Print results.
		if(testMandatory() && whitelistedEmpty){
			isMandatoryThere = true
			println("\nINFO: All mandatory values present. SUCCESS.\n")
			if(!verboseSwitch)
			printMandLines()
		} else {
			println("\nERROR: " + howManyMandsMissing() + " mandatory values missing. \nFAILURE. Results below.\n")
			printMandMap()
			
			if(howManyMandsMissing() == 0)
				isMandatoryThere = true
		}
		if(testBlack()){
			isBlacklistedThere = true
			println("\nERROR: " + howManyBlacksMissing() + " Blacklisted entries have been found.\nFAILURE. Results below.\n")
			if(!verboseSwitch)
			printBlackMap()
			if(!verboseSwitch)
			printBlackLines()
		} else {
			println("INFO: All blacklisted entries have not been found. SUCCESS.")
		}
		if(verboseSwitch){
			printBlackLines()
			printMandLines()
		}
		//Depending on results, assign exit code.
		exitCode = 0

		if(!isBlacklistedThere && isMandatoryThere)
			exitCode = 1

		if(!isMandatoryThere)
			exitCode = 2

		if(isBlacklistedThere)
			exitCode = 3

		if(isBlacklistedThere && !isMandatoryThere)
			exitCode = 4

		//Close
		try{
			whiteScan.close()
			blackScan.close()
			jenkScan.close()
		} catch (Exception e){
			
		}
		//Exit.
		System.exit(exitCode)
	}
	/**
	 * After test of whitelist values, output where they were found
	 */
	public static void outputMandatoryLines(){
		println("Hit mandatory value at line: ")
		for(Integer ing : linesMand){
			println(ing.toString())
		}
	}
	/**
	 * Checks if all whitelisted values are there.
	 * @return boolean
	 */
	public static boolean testMandatory(){
		boolean answer = true
		def mandVals = mandMap.values()
		def it = mandVals.iterator()
		boolean fal = false

		while(it.hasNext()){
			if(it.next().equals(fal))
				answer = false
		}
		return answer
	}
	/**
	 * Prints to console the lines whitelisted entries were found.
	 * @author adamhowa
	 * @since 20/04/2017
	 *
	 * {@code}
	 *
	 */
	public static void printMandLines(){
		if(mandLinesFound.size() > 0){
			//println(mandLinesFound.size())
			println("\nMandatory values found at line:")
			int cnt = 0;
			for(int i = 0; i < mandLinesFound.size(); i++){
				printf("%-30.30s  %-30.30s\n", mandNameFound.get(i), mandLinesFound.get(i))
			}
		}
	}
	public static boolean isFileEmpty(File fl){
		boolean isEmpty = false
		int numLines = 0
		Scanner emptyChecker = new Scanner(fl)
		while(emptyChecker.hasNextLine()){
			emptyChecker.nextLine()
			numLines++
		}
		
		if(numLines == 0){
			isEmpty = true
		} else {
			isEmpty = false
		}
		
		return isEmpty
	}
	
	
	/**
	 * Prints to console the lines blacklisted entries were found.
	 * @author adamhowa
	 * @since 20/04/2017
	 *
	 * {@code}
	 *
	 */
	
	public static void printBlackLines(){
		if(blackLinesFound.size() > 0){
			println("\nBlacklisted values found at line:");
			int cnt = 0;
			for(int i = 0; i < blackLinesFound.size(); i++){
				printf("%-30.30s  %-30.30s\n", blackNameFound.get(i), blackLinesFound.get(i))
				cnt++
			}
		}
	}
	/**
	 * Prints to the console the usage of this program.
	 * @author adamhowa
	 *
	 */
	public static void printUsage(){
		println("The correct usage of this program is\n")
		println("sanitizer -j <jenkinsFile> -m <mandatory> -b <blacklisted> [-v] [-h]\n")
		println("Where -v is verbose. Giving more detailed output. -h is help.\n")
		println("EXIT CODES\n0: Nothing\n1: Pass (All Whitelisted are there AND All blacklisted are not there)\n2: FAIL A whitelisted does not exist\n3: FAIL A blacklisted exists\n4: FAIL A whitelisted does not exist AND A blacklisted exists")
		println("--------------------------------------------------------------------\n\n")
	}
	/**
	 * Prints blacklisted entries status as columns.
	 * @author adamhowa
	 * 
	 */
	public static void printBlackMap(){
		def keys = blackMap.keySet()
		def it = keys.iterator()
		def vals = blackMap.values()
		def itt = vals.iterator()
		println("\nBlacklisted entry 	      In File?")
		while(it.hasNext()){
			String next
			if(itt.next()){
				next = "Yes"
			} else {
				next = "No"
			}
			printf("%-30.30s  %-30.30s\n", it.next(), next)
		}
	}
	/**
	 * Prints whitelisted entries status as columns.
	 * @author adamhowa
	 *
	 */
	public static void printMandMap(){
		def keys = mandMap.keySet()
		def it = keys.iterator()
		def vals = mandMap.values()
		def itt = vals.iterator()
		println("\nWhitelisted entry 	      In File?")
		while(it.hasNext()){
			String next
			if(itt.next()){
				next = "Yes"
			} else {
				next = "No"
			}
			printf("%-30.30s  %-30.30s\n", it.next(), next)
		}
	}
	/**
	 * Are all of the blacklisted entries there?
	 * @author adamhowa
	 * @return boolean
	 *
	 */
	public static boolean testBlack(){
		boolean answer = false
		def mandVals = blackMap.values()
		def it = mandVals.iterator()
		boolean fal = false

		while(it.hasNext()){
			if(it.next().equals(true)){
				answer = true
			}
		}
		return answer
	}
	/**
	 * Gives the number of mandatory or whitelisted values that are not there.
	 * @author adamhowa
	 * @return int
	 *
	 */
	public static int howManyMandsMissing(){
		int howMany = 0

		def mandVals = mandMap.values()
		def it = mandVals.iterator()
		boolean fal = false

		while(it.hasNext()){
			if(it.next().equals(fal)){
				howMany++
			}
		}
		return howMany
	}
	/**
	 * Gives the number of blacklisted values that are not there.
	 * @author adamhowa
	 * @return int 
	 *
	 */
	public static int howManyBlacksMissing(){
		int howMany = 0

		def mandVals = blackMap.values()
		def it = mandVals.iterator()
		boolean fal = false

		while(it.hasNext()){
			if(it.next().equals(true)){
				howMany++
			}
		}
		return howMany
	}
	/**
	 * After test of blacklist values, output where they were found
	 */
	public static void outputBlacklistedLines(){
		println("Hit blacklisted value at line: ")
		for(Integer ing : linesBlack){
			println(ing.toString())
		}
	}
	/**
	 * Gets the time(as a String).
	 * @author adamhowa
	 * @return Timestamp
	 *
	 */
	public static String getTimeStampNow(){
		def d  = new Date()
		String res = d.toString()
		return res
	}
	/**
	 * Searches both files to see if there are duplicate strings in any line. If there are duplicates, delete.
	 * @author adamhowa
	 *
	 */
	public static void removeDuplicates(){
		int indexFirst, indexSecond = 0
		def keysMand = mandMap.keySet()
		String[] whiteLst = keysMand.toArray()

		def keysBlack = blackMap.keySet()
		String[] blackLst = keysBlack.toArray()

		for(String w : whiteLst){
			for(String b : blackLst){
				if(w == b){	
					white.remove(w)
					blackArray.remove(b)
					mandMap.remove(w)
					blackMap.remove(b)
					println("\nERROR: Duplicates found in whitelisted and blacklisted files.")
					println(w + " was found in both files")
					println(w + " removed")
				}
			}
		}
	}
	/**
	 * Searches file splitting on space. Ignores lines that are commented.
	 * @param inputSearch
	 * @param filePath
	 * @return boolean true/false
	 */
	public static boolean findWordInFile(String inputSearch, String filePath){
		lineNumber = ""
		boolean found = false
		double count = 0,countBuffer=0,countLine=0
		BufferedReader br
		String line = ""
		countLine = 0
		boolean lineCommented = false
		try {
			br = new BufferedReader(new FileReader(filePath))
			try {
				while((line = br.readLine()) != null)
				{
					for(Integer i : commentedLines){
						if(i == countLine){
							lineCommented = true
						}
					}
					if(!lineCommented){
						String[] words = line.split(" ")
						for (String word : words) {
							if (word.equals(inputSearch)) {
								found  =true
								count++
								countBuffer++
								lineNumber += (int)countLine
							}
						}
					}
					if(lineCommented)
						lineCommented = false
					
					countLine++
				}
				br.close()
			} catch (IOException e) {
				e.printStackTrace()
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace()
		}
		return found
	}
	/**
	 * Sifts through the file to test and identifies the commented lines. Both multi line and single.
	 * Stores line numbers in programs memory for later use.
	 */
	public static void findCommentedLines(){
		boolean isLineCommented = false
		String line
		
		int lineCount = 0
		
		boolean hitEnd = false
		boolean startOfMulti = false
		
		while(jenkScan.hasNextLine()){
			line = jenkScan.nextLine()
			
			if(line.contains("//")){
				commentedLines.add(lineCount)
			}
			
			if(line.contains("/*") || startOfMulti){
				if(!startOfMulti)
				startOfMulti = true
				
				if(startOfMulti)
					commentedLines.add(lineCount)
				
				if(line.contains("*/")){
					hitEnd = true
					startOfMulti = false
				}
			}
			
			lineCount++
		}
	}
	/**
	 * Iterates through each possible combination of characters to the left and right of the word to search for.
	 * This is the main SEARCH method that tries to match given values. It uses a huge combination of regex tests per each line in the test file to try and find all
	 * matches, including complex ones. It ignores commented lines both single and multi line.
	 * @param inputSearch
	 * @param filePath
	 * @return true/false
	 */
	public static boolean checkWordSides(String inputSearch, boolean whiteBlack){
		lineNumber = ""
		boolean found = false
		double count = 0,countBuffer=0,countLine=0
		BufferedReader br
		String line = ""
		String REGEX_PATTERN = "\\{(" + inputSearch+ ")\\}"
		countLine = 0
		int leftAcceptedLimit = 3
		int acceptedCounter = 0;
		int limit = 24;
		println("Searching for " + inputSearch + ".")
		
		boolean lineCommented = false
		try {
			br = new BufferedReader(new FileReader(jenkinsFile))
			try {
				while((line = br.readLine()) != null)
				{
					
					int local = 0;
					String left = null;
					String right = null;
					
					for(Integer i : commentedLines){
						if(i == countLine){
							lineCommented = true
							
						}
					}	
						while(acceptedCounter < limit){
							if(acceptedCounter == limit-1){
								local++;
								acceptedCounter = 0;
							}
							left = acceptedLeft[local];
							right = acceptedRight[acceptedCounter]
							
							acceptedCounter++;
							REGEX_PATTERN = "\\" + left + "(\\Q" + inputSearch + "\\E)" + "\\" + right;
							String s  = line
							Pattern p = Pattern.compile(REGEX_PATTERN)
							Matcher m = p.matcher(s)
							//println(REGEX_PATTERN)
							while (m.find()) {
								found = true
								countBuffer++
								if(!lineCommented){
									lineNumber += (int)countLine
								}
								if(lineCommented){
									String lister = ""
									if(whiteBlack){
										lister = "Whitelisted"
									} else {
										lister = "Blacklisted"
									}
									print("WARNING: line " + (int)countLine + " contains " + lister + " value " + inputSearch + "\n")
								}
							}
							if(acceptedCounter == limit || local == limit){
								break;
							}
						}
					
					
					if(lineCommented)
						lineCommented = false
					countLine++;
				}
				br.close()
			} catch (Exception e) {
				//e.printStackTrace()
			}
		} catch (FileNotFoundException e) {
			//e.printStackTrace()
		}
		return found
	}
}

