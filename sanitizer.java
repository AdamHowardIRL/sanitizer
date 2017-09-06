//package sanitizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.Executors;

/**
 * @author Adam Howard
 * @since 4/6/2017
 * @version 1.0.0.0
 *
 */
public class sanitizer {
	//Locals
	public static String jenkinsFile = "";
	public static String whiteList = "";
	public static String blackList = "";
	private static ArrayList<Integer> linesMand = new ArrayList<Integer>();
	private static ArrayList<Integer> linesBlack = new ArrayList<Integer>();
	private static boolean verboseSwitch = false;
	private static HashMap<String, Boolean> mandMap = new HashMap<String, Boolean>();
	private static HashMap<String, Boolean> blackMap = new HashMap<String, Boolean>();
	private static ArrayList<Integer> mandLinesFound = new ArrayList<Integer>();
	private static ArrayList<Integer> blackLinesFound = new ArrayList<Integer>();
	private static ArrayList<String> blackNameFound = new ArrayList<String>();
	private static ArrayList<String> mandNameFound = new ArrayList<String>();
	private static final String helpSwitch = "-h";
	private static final String mandSwitch = "-m";
	private static final String blackSwitch = "-b";
	private static final String jenkSwitch = "-j";
	private static final String verbSwitch = "-v";
	private static boolean wantHelp = false;
	private static final String whitelistedChars = "[^a-z0-9A-Z-.\\[\\]]";
	private static char[] acceptedLeft = {':', '(', '[', ' ', '{', '@','/','\\',';','#','$','*','\"','\'',']','&','!','+','?','|',';',')','\u0000','\u0000','=','='};
	private static char[] acceptedRight = {':', ')', ']', ' ', '}', '@','/','\\',';','#','$','*','\"','\'','[','&','!','+','?','|',';','(','\u0000','\u0000','=','='};
	private static File jenkFile = null;
	private static File mand = null;
	private static File black = null;
	private static ArrayList<String> lineNumber = new ArrayList<String>();
	private static Scanner whiteScan = null;
	private static Scanner blackScan = null;
	private static Scanner jenkScan = null;
	private static ArrayList<String> white = new ArrayList<String>();
	private static ArrayList<String> blackArray = new ArrayList<String>();
	private static int exitCode = 0;
	private static boolean isMandatoryThere = false;
	private static boolean isBlacklistedThere = false;
	private static int foundLineCounter = 0;
	private static ArrayList<Integer> commentedLines = new ArrayList<Integer>();
	private static boolean whitelistedEmpty = false;
	private static boolean blacklistedEmpty = false;
	private static ExecutorService exer = Executors.newSingleThreadExecutor();
	private static boolean prodRDEV = true;
	private static String prod = "prod";
	private static String dev = "dev";
	private static ArrayList<String> wordsToKill = new ArrayList<String>();
    private static ArrayList<String> toRemove = new ArrayList<String>();
    private static ArrayList<String> noStar = new ArrayList<String>();
    private static ArrayList<String> noStarB = new ArrayList<String>();
    private static ArrayList<String> removeWith = new ArrayList<String>();
    private static ArrayList<String> blackRM = new ArrayList<String>();
    private static ArrayList<String> toAdd = new ArrayList<String>();
    private static ArrayList<String> presentHigh = new ArrayList<String>();
    private static boolean isReady = true;
    private static float here = 231;

	/**The bulk of the program
	 * @param args Command line arguments
	 */
	//@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		try{
			if(args.length == 0)
				wantHelp = true;
			//Parse arguments and fill variables.
			for(int i = 0; i < args.length; i++){
				//Input Verification. Removes any characters not in whitelistedChars.
				args[i] = args[i].replaceAll(whitelistedChars, "");
			
				if (args[i].contains(jenkSwitch)){
					if(args[i+1] != null)
						jenkinsFile = args[i+1];
					else
						wantHelp = true;
				}	else if (args[i].equalsIgnoreCase(blackSwitch))
					blackList = args[i+1];
				else if	(args[i].equalsIgnoreCase(mandSwitch) || args[i].equalsIgnoreCase("-w"))
					whiteList = args[i+1];
				else if (args[i].equalsIgnoreCase(helpSwitch) || args[i].equalsIgnoreCase("/?") || args.length < 1)
					wantHelp = true;
				else if (args[i].equalsIgnoreCase(verbSwitch))
					verboseSwitch = true;	
			}
			for(int i = 0; i < args.length; i++){
				if (args[i].equalsIgnoreCase(dev))
					prodRDEV = false;
				
				if (args[i].equalsIgnoreCase(prod))
					prodRDEV = true;
			}
		} catch (Exception ex){
			wantHelp = true;
		}
	
		if(jenkinsFile != "" && whiteList == "" && blackList == "" ){
			wantHelp = true;
		}
		if(jenkinsFile == "" && whiteList != "" && blackList == "" ){
			wantHelp = true;
		}
		if(jenkinsFile == "" && whiteList == "" && blackList != "" ){
			wantHelp = true;
		}
		if(wantHelp){
			System.out.println("This program requires two or three files as arguments. (white and black are optional)\n ");
			printUsage();
		}
		//When verbose, print files that have been inputed.
		if(verboseSwitch){
			System.out.println(getTimeStampNow() + " INFO: File to test: " + jenkinsFile);
			System.out.println(getTimeStampNow() + " INFO: File whitelist: " + whiteList);
			System.out.println(getTimeStampNow() + " INFO: File blacklist: " + blackList);
		}
		//Print that file are about to be loaded.
		if(verboseSwitch)
			System.out.println("\n" + getTimeStampNow() + " Loading files\n");
		//Load the files
		try{
			jenkFile = new File(jenkinsFile);
			mand = new File(whiteList);
			black = new File(blackList);
		} catch (Exception e){
			return;
		}
		if(jenkinsFile != "" && blackList != ""){
			if(!black.exists())
				System.out.println("Blacklisted file does not exist.");
		}
		if(jenkinsFile != "" && whiteList != ""){
			if(!mand.exists())
				System.out.println("Whitelisted file does not exist.");
		}
		//Mandatory and blacklisted converted to ArrayList		
		//Scanners
		try {
			jenkScan = new Scanner(jenkFile);
			if(verboseSwitch)
				System.out.println(getTimeStampNow().toString() + " File to test read");
		} catch (Exception e) {}
		try{
			whiteScan = new Scanner(mand);
			if(verboseSwitch)
				System.out.println(getTimeStampNow().toString() + " Whitelisted file read");
		} catch (Exception ex) {}
		try{
			blackScan = new Scanner(black);
			if(verboseSwitch)
				System.out.println(getTimeStampNow().toString() + " Blacklisted file read");
		} catch (Exception ex) {}
		//Load files
		try{
			//Fill Maps
			while(whiteScan.hasNextLine()){
				String line = whiteScan.nextLine();
				String place = null;
				white.add(line);
				mandMap.put(line, false);
			}
		} catch(Exception e){}
		
		try{
			while(blackScan.hasNextLine()){
				String line = blackScan.nextLine();
				String place = null;
				blackArray.add(line);
				blackMap.put(line, false);
			}
		} catch (Exception e){}
		try{
			Character ch = Character.MIN_VALUE;
			for(String now : white){
				if(now.charAt(0) == '#'){
					toRemove.add(now);
				}
			}
			for(String now : blackArray){
				if(now.charAt(0) == '#'){
					blackRM.add(now);
				}
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		if(!prodRDEV){
			try{
				Character ch = Character.MIN_VALUE;
				for(String now : white){
					if(now.charAt(0) == '*'){
						toRemove.add(now);
					}
				}
				for(String now : blackArray){
					if(now.charAt(0) == '*'){
						blackRM.add(now);
					}
				}
			} catch (Exception e){
			}
		}
		
		if(prodRDEV){
			try{
				Character ch = Character.MIN_VALUE;
				for(String now : white){
					if(now.charAt(0) == '*'){
						String RMStar = now.replace('*', '\u0000');
						String trm = RMStar.trim(); 
						noStar.add(trm);
						toRemove.add(now);
					}
				}
				for(String now : blackArray){
					if(now.charAt(0) == '*'){
						String RMStar = now.replace('*', '\u0000');
						String trm = RMStar.trim(); 
						noStarB.add(trm);
						blackRM.add(now);
					}
				}
			} catch (Exception e){
			}
		}
		
		white.removeAll(toRemove);
		blackArray.removeAll(blackRM);
		
		for(String key : toRemove){
			mandMap.remove(key);
		}
		for(String key : blackRM){
			blackMap.remove(key);
		}
		
		for(String s: noStar){
			white.add(s);
			mandMap.put(s,false);
		}
		
		for(String i : noStarB){
			blackArray.add(i);
			blackMap.put(i,false);
		}
		
		
		try{
			if(!wantHelp){
				if(isFileEmpty(jenkFile) && jenkinsFile != ""){
					System.out.println("File to test is empty");
				}
				if(isFileEmpty(mand) && whiteList != "" && mand.exists()){
					System.out.println("Whitelisted file is empty");
					whitelistedEmpty = true;
				}
				if(isFileEmpty(black) && blackList != "" && black.exists()){
					System.out.println("Blacklisted file is empty");
					blacklistedEmpty = true;
				}
			}
		} catch (Exception e){}
		removeDuplicates();
		findCommentedLines();
		if(verboseSwitch){
			System.out.println("\n\t\tBEFORE\n");
			System.out.println(getTimeStampNow() + " INFO: Whitelisted entries\n ");
			printMandMap();
			System.out.println("\n" + getTimeStampNow() + " INFO: Blacklisted entries\n ");
			printBlackMap();
			System.out.println("\nBeginning search");
			System.out.println("\n");
		}
		//Whitelisted search. Tests each line for many different combination of characters left and right to the word we are looking for.
		if(white.size() > 0 && !whitelistedEmpty && !wantHelp){
			for(String cur : white){
				if(checkWordSides(cur, true)){
					mandMap.replace(cur,  true);
					for(String s: lineNumber){
						mandNameFound.add(cur);
						mandLinesFound.add(Integer.parseInt(s));
					}
				}
			  if(findWordInFile(cur, jenkinsFile)){
					boolean dup = false;
				    mandMap.replace(cur,  true);
					for(String s: lineNumber){
						for(Integer l : mandLinesFound){
							if(s == Integer.toString(l)){
								dup = true;
							}
						}
					}
					if(!dup){
						for(String s : lineNumber){
							mandNameFound.add(cur);
							mandLinesFound.add(Integer.parseInt(s));
						}
					}
				}
			}
		}
		//Blacklisted search. REGEX and space delimiter test per line.
		foundLineCounter = 0;
		if(blackArray.size() > 0 && !blacklistedEmpty && !wantHelp){
			for(String cur : blackArray){
				if(checkWordSides(cur, false)){
					blackMap.replace(cur,  true);
					for(String s: lineNumber){
						blackNameFound.add(cur);
						blackLinesFound.add(Integer.parseInt(s));
					}
				}
				if(findWordInFile(cur, jenkinsFile)){
					boolean dup = false;
				    blackMap.replace(cur,  true);
					for(String s: lineNumber){
						for(Integer l : blackLinesFound){
							if(s == Integer.toString(l)){
								dup = true;
							}
						}
					}
					if(!dup){
						for(String s : lineNumber){
							blackNameFound.add(cur);
							blackLinesFound.add(Integer.parseInt(s));
						}
					}
				}
			}
		}
		//After the file has been searched, output results.
		if(verboseSwitch){
			System.out.println("\n\t\tAFTER\n");
			System.out.println(getTimeStampNow() + " INFO: Whitelisted entries\n ");
			printMandMap();
			System.out.println("\n" + getTimeStampNow() + " INFO: Blacklisted entries\n ");
			printBlackMap();
		}
		//Print results.
		if(testMandatory() && !whitelistedEmpty && mand.exists() && howManyMandsMissing() == 0){
			isMandatoryThere = true;
					System.out.println("\nINFO: All mandatory values present. SUCCESS.\n");
			if(!verboseSwitch)
			printMandLines();
		} else {
			if(!wantHelp && whiteList != "" && mand.exists()){
				System.out.println("\nERROR: " + howManyMandsMissing() + " mandatory values missing. \nFAILURE. Results below.\n");
				printMandMap();
			}
			if(howManyMandsMissing() == 0)
				isMandatoryThere = true;
		}
		if(testBlack() && black.exists()){
			isBlacklistedThere = true;
			System.out.println("\nERROR: " + howManyBlacksMissing() + " Blacklisted entries have been found.\nFAILURE. Results below.\n");
			if(!verboseSwitch)
			printBlackMap();
			if(!verboseSwitch)
			printBlackLines();
		} else {
			if(!wantHelp && !blacklistedEmpty && blackList != "" && black.exists())
			System.out.println("INFO: All blacklisted entries have not been found. SUCCESS.");
		}
		if(verboseSwitch){
			printBlackLines();
			printMandLines();
		}
		//Depending on results, assign exit code.
		//exitCode = 0;
		if(!isBlacklistedThere && isMandatoryThere)
			exitCode = 0;

		if(!isMandatoryThere)
			exitCode = 1;

		if(isBlacklistedThere)
			exitCode = 2;

		if(isBlacklistedThere && !isMandatoryThere)
			exitCode = 3;
		//Close
		try{
			whiteScan.close();
			blackScan.close();
			jenkScan.close();
		} catch (Exception e){}
		//Exit.
		System.exit(exitCode);
	}
	/**
	 * After test of whitelist values, output where they were found
	 */
	public static void outputMandatoryLines(){
		System.out.println("Hit mandatory value at line: ");
		for(Integer ing : linesMand){
			System.out.println(ing.toString());
		}
	}
	/**
	 * Checks if all whitelisted values are there.
	 * @return boolean
	 */
	public static boolean testMandatory(){
		boolean answer = true;
		Collection<Boolean> mandVals = mandMap.values();
		Iterator<Boolean> it = mandVals.iterator();
		boolean fal = false;

		while(it.hasNext()){
			if(it.next().equals(fal))
				answer = false;
		}
		return answer;
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
			System.out.println("\nMandatory values found at line:");
			int cnt = 0;
			for(int i = 0; i < mandLinesFound.size(); i++){
				System.out.printf("%-30.30s  %-30.30s\n", mandNameFound.get(i), mandLinesFound.get(i));
			}
		}
	}
	public static boolean isFileEmpty(File fl){
		boolean isEmpty = false;
		int numLines = 0;
		Scanner emptyChecker = null;
		try {
			emptyChecker = new Scanner(fl);

			while(emptyChecker.hasNextLine()){
				emptyChecker.nextLine();
				numLines++;
			}
		} catch (Exception e) {}
		
		if(numLines == 0){
			isEmpty = true;
		} else {
			isEmpty = false;
		}
		
		return isEmpty;
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
			System.out.println("\nBlacklisted values found at line:");
			int cnt = 0;
			for(int i = 0; i < blackLinesFound.size(); i++){
				System.out.printf("%-30.30s  %-30.30s\n", blackNameFound.get(i), blackLinesFound.get(i));
				cnt++;
			}
		}
	}
	/**
	 * Prints to the console the usage of this program.
	 * @author adamhowa
	 *
	 */
	public static void printUsage(){
		System.out.println("The correct usage of this program is\n");
		System.out.println("sanitizer -j <jenkinsFile> -m <mandatory> -b <blacklisted> [-v] [-h] [prod/dev] \n");
		System.out.println("Where -v is verbose. Giving more detailed output. -h is help.\n");
		System.out.println("Production mode (prod) allows hightlighted entries. Development (dev) does not.\n");
		System.out.println("EXIT CODES\n0: Pass (All Whitelisted are there AND All blacklisted are not there)\n1: FAIL A whitelisted does not exist\n2: FAIL A blacklisted exists\n3: FAIL A whitelisted does not exist AND A blacklisted exists");
		System.out.println("--------------------------------------------------------------------\n\n");
	}
	/**
	 * Prints blacklisted entries status as columns.
	 * @author adamhowa
	 * 
	 */
	public static void printBlackMap(){
		Set<String> keys = blackMap.keySet();
		Iterator<String> it = keys.iterator();
		Collection<Boolean> vals = blackMap.values();
		Iterator<Boolean> itt = vals.iterator();
		System.out.println("\nBlacklisted entry 	      In File?");
		while(it.hasNext()){
			String next;
			if(itt.next()){
				next = "Yes";
			} else {
				next = "No";
			}
			System.out.printf("%-30.30s  %-30.30s\n", it.next(), next);
		}
	}
	/**
	 * Prints whitelisted entries status as columns.
	 * @author adamhowa
	 *
	 */
	public static void printMandMap(){
		Set<String> keys = mandMap.keySet();
		Iterator<String> it = keys.iterator();
		Collection<Boolean> vals = mandMap.values();
		Iterator<Boolean> itt = vals.iterator();
		System.out.println("\nWhitelisted entry 	      In File?");
		while(it.hasNext()){
			String next;
			if(itt.next()){
				next = "Yes";
			} else {
				next = "No";
			}
			System.out.printf("%-30.30s  %-30.30s\n", it.next(), next);
		}
	}
	/**
	 * Are all of the blacklisted entries there?
	 * @author adamhowa
	 * @return boolean
	 *
	 */
	public static boolean testBlack(){
		boolean answer = false;
		Collection<Boolean> mandVals = blackMap.values();
		Iterator<Boolean> it = mandVals.iterator();
		boolean fal = false;

		while(it.hasNext()){
			if(it.next().equals(true)){
				answer = true;
			}
		}
		return answer;
	}
	/**
	 * Gives the number of mandatory or whitelisted values that are not there.
	 * @author adamhowa
	 * @return int
	 *
	 */
	public static int howManyMandsMissing(){
		int howMany = 0;

		Collection<Boolean> mandVals = mandMap.values();
		Iterator<Boolean> it = mandVals.iterator();
		boolean fal = false;

		while(it.hasNext()){
			if(it.next().equals(fal)){
				howMany++;
			}
		}
		return howMany;
	}
	/**
	 * Gives the number of blacklisted values that are not there.
	 * @author adamhowa
	 * @return int 
	 *
	 */
	public static int howManyBlacksMissing(){
		int howMany = 0;

		Collection<Boolean> mandVals = blackMap.values();
		Iterator<Boolean> it = mandVals.iterator();
		boolean fal = false;

		while(it.hasNext()){
			if(it.next().equals(true)){
				howMany++;
			}
		}
		return howMany;
	}
	/**
	 * After test of blacklist values, output where they were found
	 */
	public static void outputBlacklistedLines(){
		System.out.println("Hit blacklisted value at line: ");
		for(Integer ing : linesBlack){
			System.out.println(ing.toString());
		}
	}
	/**
	 * Gets the time(as a String).
	 * @author adamhowa
	 * @return Timestamp
	 *
	 */
	public static String getTimeStampNow(){
		Date d  = new Date();
		String res = d.toString();
		return res;
	}
	/**
	 * Searches both files to see if there are duplicate strings in any line. If there are duplicates, delete.
	 * @author adamhowa
	 *
	 */
	public static void removeDuplicates(){
		int indexFirst, indexSecond = 0;
		Set<String> keysMand = mandMap.keySet();
		Object[] whiteLst = keysMand.toArray();

		Set<String> keysBlack = blackMap.keySet();
		Object[] blackLst = keysBlack.toArray();

		for(Object w : whiteLst){
			for(Object b : blackLst){
				if(w == b){	
					white.remove(w);
					blackArray.remove(b);
					mandMap.remove(w);
					blackMap.remove(b);
					System.out.println("\nERROR: Duplicates found in whitelisted and blacklisted files.");
					System.out.println(w + " was found in both files");
					System.out.println(w + " removed");
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
		lineNumber.clear();
		boolean found = false;
		double count = 0,countBuffer=0,countLine=0;
		BufferedReader br;
		String line = "";
		countLine = 0;
		boolean lineCommented = false;
		try {
			br = new BufferedReader(new FileReader(filePath));
			try {
				while((line = br.readLine()) != null)
				{
					for(Integer i : commentedLines){
						if(i == countLine){
							lineCommented = true;
						}
					}
					if(!lineCommented){
						String[] words = line.split(" ");
						for (String word : words) {
							if (word.equals(inputSearch)) {
								found  =true;
								count++;
								countBuffer++;
								
								lineNumber.add(Integer.toString((int)countLine));
							}
						}
					}
					if(lineCommented)
						lineCommented = false;
					
					countLine++;
				}
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch(Exception e) {} 
		return found;
	}
	/**
	 * Sifts through the file to test and identifies the commented lines. Both multi line and single.
	 * Stores line numbers in programs memory for later use.
	 */
	public static void findCommentedLines(){
		boolean isLineCommented = false;
		String line;
		
		int lineCount = 0;
		
		boolean hitEnd = false;
		boolean startOfMulti = false;
		try{
			while(jenkScan.hasNextLine()){
				line = jenkScan.nextLine();
				
				if(line.contains("//")){
					commentedLines.add(lineCount);
				}
				
				if(line.contains("/*") || startOfMulti){
					if(!startOfMulti)
					startOfMulti = true;
					
					if(startOfMulti)
						commentedLines.add(lineCount);
					
					if(line.contains("*/")){
						hitEnd = true;
						startOfMulti = false;
					}
				}
				
				lineCount++;
			}
		} catch (Exception e){}
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
		lineNumber.clear();
		boolean found = false;
		double count = 0,countBuffer=0,countLine=0;
		BufferedReader br;
		String line = "";
		String REGEX_PATTERN = "\\{(" + inputSearch+ ")\\}";
		countLine = 0;
		int leftAcceptedLimit = 3;
		int acceptedCounter = 0;
		int limit = 24;
		System.out.println("Searching for " + inputSearch + ".");
		
		boolean lineCommented = false;
		try {
			br = new BufferedReader(new FileReader(jenkinsFile));
			try {
				while((line = br.readLine()) != null)
				{
					int local = 0;
					char left = ' ';
					char right = ' ';
					
					for(Integer i : commentedLines){
						if(i == countLine){
							lineCommented = true;
							
						}
					}	
						while(acceptedCounter < limit){
							if(acceptedCounter == limit-1){
								local++;
								acceptedCounter = 0;
							}
							left = acceptedLeft[local];
							right = acceptedRight[acceptedCounter];
							
							acceptedCounter++;
							REGEX_PATTERN = "\\" + left + "(\\Q" + inputSearch + "\\E)" + "\\" + right;
							String s  = line;
							Pattern p = Pattern.compile(REGEX_PATTERN);
							Matcher m = p.matcher(s);
							//println(REGEX_PATTERN)
							while (m.find()) {
								found = true;
								countBuffer++;
								if(!lineCommented){
									lineNumber.add(Integer.toString((int)countLine));
								}
								if(lineCommented){
									String lister = "";
									if(whiteBlack){
										lister = "Whitelisted";
									} else {
										lister = "Blacklisted";
									}
									System.out.print("WARNING: line " + (int)countLine + " contains " + lister + " value " + inputSearch + "\n");
								}
							}
							if(acceptedCounter == limit || local == limit){
								break;
							}
						}
					
					if(lineCommented)
						lineCommented = false;
					countLine++;
				}
				br.close();
			} catch (Exception e) {}
		} catch (FileNotFoundException e) {}
		return found;
	}
	
	public static boolean check4Word(String inputSearch){
		//String inputSearch = "signing";
		lineNumber.clear();
		boolean found = false;
		double count = 0,countBuffer=0,countLine=0;
		BufferedReader br;
		String line = "";
		String REGEX_PATTERN = "\\{(" + inputSearch+ ")\\}";
		countLine = 0;
		int leftAcceptedLimit = 3;
		int acceptedCounter = 0;
		int limit = 24;
		//System.out.println("Searching for " + inputSearch + ".");
		
		boolean lineCommented = false;
		try {
			br = new BufferedReader(new FileReader(jenkinsFile));
			try {
				while((line = br.readLine()) != null)
				{
					int local = 0;
					char left = ' ';
					char right = ' ';
					
					for(Integer i : commentedLines){
						if(i == countLine){
							lineCommented = true;
							
						}
					}	
						while(acceptedCounter < limit){
							if(acceptedCounter == limit-1){
								local++;
								acceptedCounter = 0;
							}
							left = acceptedLeft[local];
							right = acceptedRight[acceptedCounter];
							
							acceptedCounter++;
							REGEX_PATTERN = "\\" + left + "(\\Q" + inputSearch + "\\E)" + "\\" + right;
							String s  = line;
							Pattern p = Pattern.compile(REGEX_PATTERN);
							Matcher m = p.matcher(s);
							//println(REGEX_PATTERN)
							while (m.find()) {
								found = true;
								countBuffer++;
								if(!lineCommented){
									lineNumber.add(Integer.toString((int)countLine));
								}
								
							}
							if(acceptedCounter == limit || local == limit){
								break;
							}
						}
					
					if(lineCommented)
						lineCommented = false;
					countLine++;
				}
				br.close();
			} catch (Exception e) {}
		} catch (FileNotFoundException e) {}
		return found;
	}
	
	
}


	



