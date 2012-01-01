@license{
  Copyright (c) 2009-2011 CWI
  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
}
@contributor{Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI}

@bootstrapParser
module experiments::RascalTutor::CourseModel

import Graph;
import List;
import Map;
import IO;
import ValueIO;
import String;
import Exception;
import ParseTree;
import experiments::RascalTutor::RascalUtils;

public loc courseDir    = |std:///experiments/RascalTutor/Courses/|;
public loc courseDirSVN = |std:///experiments/RascalTutor/Courses/.svn|;
public str remoteLoc = "remote-loc.value";
public str remoteConcepts = "remote-concepts.value";

// A ConceptName is the "pathname" of a concept in the concept hierarchy, e.g., "Rascal/Datastructure/Set"

alias ConceptName = str;

// A QuestionName is similar to a ConceptName, but extended with a suffix, e.g., "Rascal/Datastructure/Set.1"
alias QuestionName = str;

alias Questions = list[Question];

// A Course captures all the information for the run-time execution of a course by the Tutor.

data Course = 
     course(
			ConceptName root,                         // Name of the root concept
			list[str] warnings,                       // List of course compiler warnings
			map[ConceptName,Concept] concepts,        // Mapping ConceptNames to their description
			//rel[ConceptName,ConceptName] refinements, // Tree structure of concept refinements
			list[str]  baseConcepts                  // List of baseConcepts (e.g. names that occur on path of
			                                          // of some ConceptName)
//			map[str,ConceptName] related              // Mapping abbreviated concept names to full ConceptNames
     );

data Concept = 
	 concept(ConceptName fullName,                  // Full name of the concept
			list[str] warnings,                     // Explicit warnings in concept text
			list[ConceptName] details,              // Optional (ordered!) list of details
			set[str] searchTerms,    				// Set of search terms
			Questions questions                 	// List of Questions 
	);
        		
data Question = choiceQuestion(ConceptName fullName, QuestionName name, str descr, list[Choice] choices)
              | textQuestion(ConceptName fullName, QuestionName name, str descr, set[str] replies)
              | tvQuestion(ConceptName fullName, QuestionName name, TVkind kind, TVdetails details)
              | useQuestion(ConceptName fullName, QuestionName name, ConceptName useFullName, QuestionName useName)
              
 // Some future possibilities:
              | commandQuestion(QuestionName name, str descr, list[str] setup, str expr, str validate)
              | funQuestion(QuestionName name, str descr, str fname, RascalType resultType, list[RascalType] argTypes, str reference)
              | moduleQuestion(QuestionName name, str descr, str mname, str fname, RascalType resultType, list[RascalType] argTypes, str reference)
			  ;
data TVdetails = details(str descr,         // Optional descriptive text
                         list[str] setup,   // setup code
                         str lstBefore,     //  listing before hole
                         str lstAfter,      // listing after hole
                         str cndBefore,     // condition before hole
                         str cndAfter,      // condition after hole
                         bool holeInLst,     // is there a hole in the listing?
                         bool holeInCnd,    // is there a hole included in the condition?
                         list[tuple[str,RascalType]] vars, 
                         list[tuple[str,str]] auxVars, 
                         RascalType rtype,
                         str hint)
              ;
data TVkind   = valueOfExpr()
              | typeOfExpr()
              ;
// TODO:
// - labels in tuples and relations are not yet handled

data RascalType =
       \bool()
     | \int(int from, int to)
     | \real(int from, int to)
     | \num(int from, int to)
     | \str()
     | \loc()
     | \datetime()
     | \list(RascalType tp)
     | \set(RascalType tp)
     | \map(RascalType key, RascalType val)
     | \tuple(list[RascalType] tps)
     | \rel(list[RascalType] tps)
     | \value()
     | \void()
     | \arb(int depth, list[RascalType] tps)	// arbitrary type of max depth and preference for leaf types
     | \same(str name)             				// a previously generated type
     ;

data Choice = good(str description)
            | bad(str description)
            ;
            
data Exception = ConceptError(ConceptName cname, str cause);

data examResult = examResult(str studentName, str studentMail, str StudentNumber, str timestamp, 
                             map[str,str] answers, map[str,str] expectedAnswers,
                             map[str, str] evaluation, num score);
            
alias VarEnv = map[str, tuple[RascalType rtype, str rval]];
            
// Common utilities

public str conceptExtension = "concept";
public str htmlExtension = "html";
public str questExtension = "quest";
public str rascalExtension = "rsc";

// Get the basename from a ConceptName, eg 
// - basename("A/B/C") => "C"

public str basename(ConceptName cn){
  return (/^.*\/<base:[A-Za-z0-9\-\_]+>$/ := cn) ? base : cn;
}

// Get the parentname from a ConceptName, eg 
// - basename("A/B/C") => "A/B"

public str parentname(ConceptName cn){
  return (/<parent:^.*>\/<base:[A-Za-z0-9\-\_]+>$/ := cn) ? parent : cn;
}

//test basename("A/B/C") == "C";

// Get the root name from a ConeptName, e.g.
// - rootname("A/B/C") => "A"

public str rootname(ConceptName cn){
  return (/<root:[A-Za-z0-9\-\_]+>.*$/ := cn) ? root : cn;
}

public list[str] getPathNames(str path){
  return [ name | /<name:[A-Za-z]+>(\/|$)/ := path ];
}

// Get all the names in a ConceptName

public list[str] basenames(ConceptName cn){
  names = [base | /<base:[A-Za-z0-9\-\_]+>/ := cn];
  nn = size(names);
  // remove duplication due to main concept in directory e.g. C/C.concept
  if(nn >= 2 && names[nn-1] == names[nn-2])
     	names = head(names, nn-1);
  return names;
}

//test basenames("A") == ["A"];
//test basenames("A/B/C") == ["A", "B", "C"];

// Compose a sublist of a list of names to a ConceptName
public str compose(list[str] names, int from, int to){
   str res = "";
   for(int i <- [from .. to])
   	res += (res == "") ? names[i] : ("/" + names[i]);
   return res;
}

public str compose(list[str] names){
  return compose(names, 0, size(names)-1);
}

public loc conceptFile(str cn){
  return courseDir + cn + (basename(cn) + ".concept");
}

public loc htmlFile(ConceptName cn){
  return courseDir + cn + (basename(cn) + ".html");
}

public loc questFile(ConceptName cn){
  return courseDir + cn + (basename(cn) + ".quest");
}

// Escape concept name for use as HTML id.

public str escapeConcept(ConceptName cn) = replaceAll(cn, "/", "_");

// Unescape concept name from use as HTML id.

public str unescapeConcept(ConceptName cn) = replaceAll(cn, "_", "/");


public str mkConceptTemplate(ConceptName cn){
return "Name: <cn>
       '
       'Synopsis:
       '
       'Syntax:
       '
       'Types:
       '
       'Function:
       '       
       'Usage:
       '
       'Details:
       '
       'Description:
       '
       'Examples:
       '
       'Benefits:
       '
       'Pitfalls:
       '
       'Questions:
       '
       ";
}

// Get a section from the concept description. Each starts with a capitalized keyword,e,g, "Description".
// Questions is the last section and is treated special: it contains questions that are analyzed later
//
// Options is a "meta-section" and may contain directives for the course compiler.

public list[str] sectionKeywords = ["Name",  "Options", "Synopsis", "Syntax", "Types", "Function", "Usage", "Details", "Description",
                                   "Examples", "Benefits", "Pitfalls", "Questions"];

public str logo = "\<img id=\"leftIcon\" height=\"40\" width=\"40\" src=\"/Courses/images/rascal-tutor-small.png\"\>";

// Read a concept file cn
// - if a remote location is defined AND the extracted doc is non-empty, return it.
// - otherwise return "<cn>.concept".

public str readConceptFile(ConceptName cn){
   println("readConceptFile: <cn>");
   cfile = conceptFile(cn);
   if(exists(cfile))
      return readFile(cfile);
   remoteloc = courseDir + cn + remoteLoc;
   if(exists(remoteloc)){
      rmap = remoteContentMap[rootname(cn)] ? ();
      if(!(rmap[cn]?)){
         remote = readTextValueFile(#loc,  remoteloc);
         extractAndCacheRemoteConcepts(remote, getLocalRoot(cn));
         rmap = remoteContentMap[rootname(cn)] ? ();
      }
      if(rmap[cn]?){
         rdoc = rmap[cn];
         if(rdoc != ""){
            println("readConceptFile, found in cache: <cn>");
         	return rdoc;
         }
      } 
   }
   throw "readConceptFile: <cn> not found";
}
/*
public list[str] readConceptFileLines(ConceptName cn){
   println("readConceptFileLines: <cn>");
   cfile = conceptFile(cn);
   if(exists(cfile))
      return readFileLines(cfile);
   remoteloc = courseDir + cn + remoteLoc;
   if(exists(remoteloc)){
      rmap = remoteContentMap[rootname(cn)] ? ();
      if(rmap[cn]?){
         println("readConceptFileLines: \"<cn>\" returns <rmap[cn]>");
         rdoc = rmap[cn];
         if(rdoc != ""){
            println("readConceptFileLines, found in cache: <cn>");
            return splitLines(rdoc);
         }
      } else {
        println("readConceptFileLines: rmap[\"<cn>\"] is undefined!");
      }
      
      remote = readTextValueFile(#loc,  remoteloc);
      rdoc = extractDoc(remote, basename(cn));
      if(rdoc != ""){
         rmap[cn] = rdoc;
         remoteContentMap[rootname(cn)] = rmap;
      	 return splitLines(rdoc);
      }
   }
   throw "readConceptFileLines: <cn> not found";
}
*/

public void saveConceptFile(ConceptName cn, str text){
   remoteloc = courseDir + cn + remoteLoc;
   if(exists(remoteloc)){
      remote = readTextValueFile(#loc,  remoteloc);
      if(replaceDoc(remote, basename(cn), text)){
         rmap = remoteContentMap[rootname(cn)] ? ();
         rmap[cn] = text;
         remoteContentMap[rootname(cn)] = rmap;
         return;
         }
   }
   file = conceptFile(cn);
   println("Saving to <file> modified concept file.");
   writeFile(file, text);
}     

public map[str,list[str]] getSections(ConceptName cn){
  return getSections(splitLines(readConceptFile(cn)));
}


public map[str,list[str]] getSections(list[str] script){
  sections = ();
  StartLine = 0;
  currentSection = "";
  for(int i <- index(script)){
    if(/^<section:[A-Z][A-Za-z]*>:\s*<text:.*>/ := script[i] && section in sectionKeywords){
      if(currentSection != ""){
      	sections[currentSection] = trimLines(script, StartLine, i);
      	//println("<currentSection> = <sections[currentSection]>");
      }
      if(/^\s*$/ := text)
         StartLine = i + 1;       // no info following section header
      else {
        script[i] = text;    // remove the section header
        StartLine = i;
      } 
      currentSection = section;
    }
  }
  if(currentSection != ""){
     sections[currentSection] = trimLines(script, StartLine, size(script));
  }
  return sections;
}

public list[str] trimLines(list[str] lines, int StartLine, int end){
  //println("trimlines(<size(lines)>,StartLine=<StartLine>,end=<end>");
  while(StartLine < end && /^\s*$/ := lines[StartLine])
    StartLine += 1;
  while(end > StartLine && /^\s*$/ := lines[end - 1])
    end -= 1;
  //println("slice(<StartLine>,<end-StartLine>)");
  if(StartLine != end)
  	return slice(lines, StartLine, end - StartLine);
  return [];
}

public list[str] splitLines(str text){
 text = visit(text) { case /\r/ => "" };
 if(!endsWith(text, "\n"))
 	text += "\n";
   return for(/<line:.*>\n/ := text)
 	          append line;
}

public str combine(list[str] lines){
  return "<for(str s <- lines){><s>\n<}>";
}


public list[ConceptName] children(Concept c){
  dir = courseDir + c.fullName;
  entries = [ entry | entry <- listEntries(dir), /^[A-Za-z]/ := entry, isDirectory(dir + entry)];
  res =  [ c.fullName + "/" + entry | entry <- c.details + (entries - c.details)];
  //println("children(<c.fullName>) =\> <res>");
  return res;
}

public list[ConceptName] children(ConceptName cn){
  cdetails = getDetails(cn);
  //("<cn>, getDetails: <cdetails>");
  dir = courseDir + cn;
  entries = [ entry | entry <- listEntries(dir), /^[A-Za-z]/ := entry, isDirectory(dir + entry)];
  res =  [ cn + "/" + entry | entry <- cdetails + (entries - cdetails)];
  //println("children(<cn>) =\> <res>");
  return res;
}


public str getSynopsis(ConceptName cn){
   try {
     sections = getSections(cn);
     return intercalate(" ", sections["Synopsis"] ? []);
   } catch: return "";
}


// Extract list of names from a section (e.g. Details section)

public list[str] getNames(list[str] lines){
   return [ cat | line <- lines, /<cat:[A-Z][A-Za-z0-9]*>/ := line ];
}

public list[str] getDetails(ConceptName cn){
   try {
     sections = getSections(cn);
     return getNames(sections["Details"] ? []);
   } catch: return [];
}

public list[str] getDetails(loc file){
   try {
     script = readFileLines(file);
     sections = getSections(script);
     return getNames(sections["Details"] ? []);
   } catch: return [];
}

set[str] exclude = {".svn"};

map[str,Course] courseCache = ();

public Course getCourse(str name){
  if(courseCache[name]?)
     return courseCache[name];
  courseFile = courseDir + name + "course.value";
  if(exists(courseFile)){
     theCourse = readTextValueFile(#Course, courseFile);
     courseCache[name] = theCourse;
     return theCourse;
  }
  throw ConceptError(name, "No such course");
}

public void updateCourse(Course c){
  courseCache[c.root] = c;
  courseConcepts[c.root] = [cn | cn <- c.concepts];
}

map[str,list[ConceptName]] courseConcepts = ();

public list[ConceptName] getCourseConcepts(ConceptName rootConcept){
  println("getCourseConcepts: <rootConcept>");
  if(courseConcepts[rootConcept]?)
     return courseConcepts[rootConcept];
  concepts = [];
  try {
   println("getCourseConcepts: try to get Course");
   theCourse = getCourse(rootConcept);
   concepts = [cn | cn <- theCourse.concepts];
  } catch: {
    concepts = getUncachedCourseConcepts(rootConcept);
  }
  
  courseConcepts[rootConcept] = concepts;
  return concepts;
}

// Get the local root for a remote concept, e.i., given a remote concept
// find the root under which it is included in the concept hierarchy.

str getLocalRoot(str cn){
   remote = courseDir + rootname(cn) + remoteConcepts;
   if(exists(remote)){
      remoteMap = readTextValueFile(#list[tuple[ConceptName, loc]], remote);
      for(<root, dir> <- remoteMap){
          dir1 = replaceLast(dir.path, ".rsc", "");
          println("<dir1> -- <cn>");
          if(contains(cn, dir1))
             return root;
      }
   }
   throw "No local root found for <cn>";
}

map[str,map[str,str]] remoteContentMap = ();

void extractAndCacheRemoteConcepts(loc file, str root){
     file = file[offset=-1][length=-1][begin=<-1,-1>][end=<-1,-1>];
     println("extractAndCacheRemoteConcepts: <file>, <root>");
     rmap =  remoteContentMap[rootname(root)] ? ();
     cmap = extractRemoteConcepts(file, root);
     println("Extracted extracted <size(cmap)> concepts from <file>");
     for(cn <- cmap)
         println("-- Add to remoteContentMap, <cn>:\n<cmap[cn]>");
     for(cn <- cmap)
         rmap[cn] = cmap[cn];
     remoteContentMap[rootname(root)] = rmap;
}

public list[ConceptName] getUncachedCourseConcepts(ConceptName rootConcept){
    println("getUncachedCourseConcepts: read all concepts");
    remoteContentMap[rootConcept] = ();
    rmap = ();
    remote = courseDir + rootConcept + remoteConcepts;
    if(exists(remote)){
      remoteMap = readTextValueFile(#list[tuple[ConceptName, loc]], remote);
      for(<root, dir> <- remoteMap){
          println("root = <root>, dir = <dir>");
          remoteFiles =  crawlFiles(dir, rascalExtension);
          for(file <- remoteFiles){
              extractAndCacheRemoteConcepts(file, root);
          /*
              cmap = extractRemoteConcepts(file, root);
              println("Extracted extracted <size(cmap)> concepts from <file>");
              for(cn <- cmap)
                  println("-- Add to remoteContentMap, <cn>:\n<cmap[cn]>");
              for(cn <- cmap)
                  rmap[cn] = cmap[cn];
          */
          }    
      }
    }
    //remoteContentMap[rootConcept] = rmap;
    concepts = crawlConcepts(rootConcept);
    println("concepts = <concepts>");
    courseConcepts[rootConcept] = concepts;
    return concepts;
}

public list[loc] crawlFiles(loc dir, str suffix){
  dotSuffix = "." + suffix;
  println(dir.path);
  if(endsWith(dir.path, dotSuffix))
     return [dir];
println("crawlFiles: <dir>, <listEntries(dir)>");
  list[loc] res = [];
 
  for( str entry <- listEntries(dir) ){
    if(entry notin exclude){
       loc sub = dir + entry;
       if(endsWith(entry, dotSuffix) || exists(dir + remoteLoc)) { 
      	  res += [sub]; 
       }
       if(isDirectory(sub)) {
          res += crawlFiles(sub, suffix);
      }
    }
  };
  return res;
}

public list[ConceptName] crawlConcepts(ConceptName root){ 
  dir = courseDir + root;
  //println("crawlConcepts: <dir>, <listEntries(dir)>");
  list[ConceptName] res = [root];
 
  for( str entry <- listEntries(dir) ){
    if(entry notin exclude){
       if(isDirectory(dir + entry)) {
          res += crawlConcepts("<root>/<entry>");
      }
    }
  };
  return res;
}

