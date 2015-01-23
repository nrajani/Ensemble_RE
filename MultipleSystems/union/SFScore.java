package MultipleSystems.union;

// Scorer for TAC KBP 2012 slot-filling task
// author:  Ralph Grishman

// version 1.4.1
// November 16, 2013
// modified by: Mihai Surdeanu (surdeanu@gmail.com)
//
// Implemented policy for handling multiple, conflicting filler value
// judgments (column 11 in assessment file) under lenient matching
// (with the ignoreoffsets or anydocs options): Keep only the "best"
// set of judgments found under leniently matched key, where filler
// value judgments are ordered by CORRECT, REDUNDANT, IGNORE, INEXACT,
// WRONG.  If multiple conflicting equivalance classes for two CORRECT
// or REDUNDANT fillers are both matched under lenient matching,
// collapse the two equivalence classes. The side effect is that the
// scorer no longer distinguishes between titles such as "President of
// Harvard" and "President of Yale"


// version 1.4
// October 24, 2013
// modified by: Hoa Dang (hoa.dang@nist.gov)
//
// Updated to handle revised slot names (including sentiment slots)
// and response file and key file formats for 2013. KBP 2013
// provenance includes justification offsets and corresponding
// justification judgments for 1) entire predicate, 2) filler mention,
// and 3) query entity mention in the document.  Judgments are now
// character strings rather than integers. The per:member_of and
// per:employee_of slots have also been merged into one
// per:employee_or_member_of slot
//
// Revised treatment of slot fillers that are judged as Redundant with the
// reference KB ("R" in Column 11 of the key file), by computing two scores:
//   1) Consider Redundant slot fillers as Correct, and require systems to return
//   these Redundant slot fillers (official score).
//   2) Ignore Redundant responses in both the key file and system
//   responses (approximates the task from KBP 2012 and earlier, where
//   systems are not supposed to return fillers that are already in
//   the reference KB)
//
// TO DO:
// 1. Add option to require that all 3 provenance judgments be Correct
// in order for the response to be Correct; i.e., the response is
// Correct only if the judgements in Columns 8-11 in the key file are
// all "C".  This option cannot be combined with either of the lenient
// matching options: ignoreoffsets, anydoc
// 2. Implement policy for handling conflicting judgments in lenient matching
//
//
// Each line in the response file has the following 4 or 10 tab-separated columns:
// Column 1: query id
// Column 2: slot name
// Column 3: a unique run id for the submission
// Column 4: NIL, if the system believes no information is learnable
//           for this slot; or a single docid that justifies the
//           relation between the query entity and the slot filler
// Column 5: a slot filler
// Column 6: start-end offsets for representative mentions used to
//           extract/normalize filler
// Column 7: start-end offsets for representative mentions used to
//           extract/normalize query entity
// Column 8: start-end offsets of clause(s)/sentence(s) in justification
// Column 10: confidence score
//
// If Column 4 is NIL, then Columns 5-10 must be empty.
// The slot filler (Column 5) must not contain any embeded tab characters

// The slot key file is simply a concatenation of the assessment
// result files from LDC.  Each assessment result files contain 12
// tab-separated fields. The field definitions are as follows:

// Column 1: Response number [generated by NIST]

// Column 2: SF query ID + slot name

// Column 3: A single doc ID for a document in the source corpus
//           (LDC2013E45) that was identified as supporting the
//           relation between the query entity and the slot filler
//           [from submission Column 4]

// Column 4: A slot filler (possibly normalized, e.g., for dates;
//           otherwise, should appear in the provenance document)
//           [from submission Column 5]

// Column 5: Start-end character offsets for representative mentions
//           used to extract/normalize filler. If two strings were
//           used, they are represented as two, comma-separated
//           offset pairs [from submission Column 6]

// Column 6: Start-end character offsets for representative mentions
//           used to extract/normalize query entity. If two strings
//           were used, they are represented as two, comma-separated
//           offset pairs [from submission Column 7]

// Column 7: Start-end character offsets of clause(s)/sentence(s) in
//           justification. If two strings were used, they are
//           represented as two, comma-separated offset pairs [from
//           submission Column 8]

// Column 8: LDC Assessment of Col 5 (filler offsets):
//             C - Correct 
//             W - Wrong
//             X - Inexact 
//             I - Ignore

// Column 9: LDC Assessment of Col 6 (query entity offsets):
//             C - Correct 
//             W - Wrong
//             X - Inexact 
//             I - Ignore
//             0 - No Response (for {per,org}:alternate_names only)

// Coumn 10: LDC Assessment of Col 7 (relation justification offsets):
//             C - Correct 
//             W - Wrong
//             L - Inexact-Long
//             S - Inexact-Short 
//             I - Ignore
//             0 - No Response (for {per,org}:alternate_names only)

// Column 11: LDC Assessment of Col 4 (slot filler value
//            correctness), with respect to the justification region
//            defined by Cols 5-7:
//             C - Correct 
//             W - Wrong
//             X - Inexact
//             R - Redundant 
//             I - Ignore

// Column 12: LDC Equivalence class of Col 4 (slot filler) if Col 11
//            is Correct or Redundant



// version 1.3
// September 17, 2012
// modified by: Hoa Dang (hoa.dang@nist.gov)
//
// Updated to handle revised slot names and response and key formats for 2012.
//
// Each line in the response file has the following tab-separated columns:
//           
// Column 1: query id
// Column 2: slot name
// Column 3: a unique run id for the submission
// Column 4: NIL, if the system believes no information is learnable for this slot; 
//  or a single docid that justifies the relation between the query entity and the slot filler
// Column 5: a slot filler
// Column 6: start offset of filler
// Column 7: end offset of filler
// Column 8: start offset of justification
// Column 9: end offset of justification
// Column 10: confidence score
//
// If Column 4 is NIL, then Columns 5-10 must be empty.
// The slot filler (Column 5) must not contain any embeded tab characters                                                      
//
//
// The slot key file is simply a concatenation of the assessment
// result files from LDC.  Each assessment result files contain 10
// tab-separated fields. The field definitions are as follows:
//
// * item_id       A file-unique integer in the range of 1 to number of
//                 fillers in original LDC assessment file
//           
// * query_name    The query id and slot for the filler; matches the name
//                 of the assessment file for that (query id, slot) pair
// 
// * docid         The document ID for the filler
// 
// * judgment      The judgment of the filler_norm, which will be one of:
//                   -1  - wrong
//                    1  - correct
//                    2  - redundant
//                    3  - inexact 
// 
// * equiv_class   The unique ID of an equivalence class into which this
//                 response falls; zero for incorrect and inexact fillers,
//                 non-zero for correct and redundant fillers.
// 
// * filler_norm   The possibly normalized system-provided filler that was assessed
//
// * judge_filler_raw    The judgment for filler_raw (all unjudged)
//
// * filler_raw    The start and end offsets for the raw filler in docid
//
// * judge_just    The judgment for justification, wich will be one of:
//                   -1 - wrong
//                    1  - correct
//                    3  - inexact 
//
// * justification The start and end offsets for the justification in docid

// version 1.2
// September 16, 2011
// modified by: Hoa Dang (hoa.dang@nist.gov)
//
// Changed format of slot judgment file to match format in assessment
// files from LDC; the slot judgment file is simply a concatenation of
// the assessment result files from LDC.  Each assessment result files
// contain 6 space-separated fields. Space characters after the first
// 5 fields are assumed to be part of the contents of the 6th
// field. The field definitions are as follows:
//
// * item_id       A file-unique integer in the range of 1 to number of
//                 fillers in file
//           
// * query_name    The query id and slot for the filler; matches the name
//                 of the assessment file for that (query id, slot) pair
// 
// * docid         The document ID for the filler
// 
// * judgment      The judgment of the filler, which will be one of:
//                   -1  - wrong
//                    1  - correct
//                    2  - redundant
//                    3  - inexact 
// 
// * equiv_class   The unique ID of an equivalence class into which this
//                 response falls; zero for incorrect and inexact fillers,
//                 non-zero for correct and redundant fillers.
// 
// * filler        The system-provided filler that was assessed


// version 1.1
// September 20, 2010
// modified by: Hoa Dang (hoa.dang@nist.gov)
//
// In trace: distinguish between responses that are redundant (R) with
// reference KB vs responses that are redundant (r) with other
// responses in the run.
//
// Added surprise slots.


// version 1.0
// July 20, 2010
// updated to penalize responses marked REDUNDANT in key
// if slots=... is specified, counts total slots to be filled based on slots file,
//                            rather than response file

// version 0.90
// May 17, 2010

// updated to handle 2010 format responses and keys
// flags added to command line
// take slot list from system responses if not separatetly provided

import java.io.*;
import java.util.*;

public class SFScore {
	float recall;
	float precision;
	float F;
    // true to print out judgement for each line of response
     boolean trace = false;

    // true to ignore docId ... match only on answerString
     boolean anydoc = false;

    // true to ignore justification offsets ... match only on answerString and docId
     boolean ignoreoffsets = false;
    // true to ignore case in answerString
     boolean nocase = false;

    // tables built from judgement file
    // provenance is docid:predoffsets:entityoffsets:filleroffsets

    //  mapping from entity_id:slot_name:provenance:response_string --> judgement for filler (response_string and provenance)
     Map<String, String> judgement = new HashMap<String, String> ();

    //  mapping from entity_id:slot_name:provenance:response_string --> judgement for predicate justification offsets
     Map<String, String> predoffjudgement = new HashMap<String, String> ();

    //  mapping from entity_id:slot_name:provenance:response_string --> judgement for filler mention offsets
     Map<String, String> filleroffjudgement = new HashMap<String, String> ();

    //  mapping from entity_id:slot_name:provenance:response_string --> judgement for entity mention offsets
     Map<String, String> entityoffjudgement = new HashMap<String, String> ();

    //  mapping from entity_id:slot_name:provenance:response_string --> equivalence class of Correct or Redundant answers
     Map<String, Integer> equivalenceClass = new HashMap<String, Integer> ();

    // //  mapping from entity_id:slot_name --> {true, false}
    // //  look at size of query_eclasses.get(query) to determine if query has answer
    // static Map<String, Boolean> query_has_answer = new HashMap<String, Boolean> ();

    //  mapping from entity_id:slot_name --> set of equivalence classes for Correct answers not in the reference KB
     Map<String, Set<Integer>> query_eclasses = new HashMap<String, Set<Integer>> ();

    //  mapping from entity_id:slot_name --> set of equivalence classes for Correct answers already in the reference KB
     Map<String, Set<Integer>> query_kb_eclasses = new HashMap<String, Set<Integer>> ();

    // table built from responses file

    //  mapping from entity_id:slot_name --> list[provenance:response_string]
     Map<String, List<String>> response = new HashMap <String, List<String>> ();

    // keeps track of equivalent eclasses (necessary due to lenient matching)
     Map<String, Set<Integer>> equivEclassesByKey = new HashMap<String, Set<Integer>> ();
    
    
    //book keeping for prior estimation for extractors
     Map<String, Double> mpSlotConfidence_precision = new HashMap<String, Double> (); //this is same as probability
     Map<String, Double> mpSlotConfidence_recall = new HashMap<String, Double> ();
     Map<String, Double> mpSlotConfidence_f1 = new HashMap<String, Double> ();
     Map<String, Integer> mpSlotJudgementEntriesCount = new HashMap<String, Integer> ();
     Map<String, Integer> mpSlotResponseEntriesCount = new HashMap<String, Integer> ();
     Map<String, Integer> mpSlotCorrectResponseEntriesCount = new HashMap<String, Integer> ();
    
    // codes in judgement file
    // static final String CORRECT = "C";
    // static final String REDUNDANT = "R";
    // static final String INEXACT = "X";
    // static final String WRONG = "W";
    // static final String IGNORE = "I";
    // static final String NORESPONSE = "0";
    // static final String INEXACT_LONG = "L";
    // static final String INEXACT_SHORT = "S";

     String CORRECT = "C";
     String REDUNDANT = "R";
     String INEXACT = "X";
     String WRONG = "W";
     String IGNORE = "I";
     String NORESPONSE = "0";
     String INEXACT_LONG = "L";
     String INEXACT_SHORT = "S";

    // next unique equivalence class
     int eclass_generator = 1000000;

     String slotFile = null;

     Set<String> slots = new TreeSet<String>();

    /**
     *  SFScorer <responses file> <key file>
     *  scores responses file against key file
     */

    public  void run (String[] args) throws IOException {

	if (args.length < 2 || args.length > 7) {
	    System.out.println ("SlotScorer must be invoked with 2 to 8 arguments:");
	    System.out.println ("\t<responses file>  <key file> [flag ...]");
	    System.out.println ("flags:");
	    System.out.println ("\ttrace  -- print a line with assessment of each system response");
	    System.out.println ("\tanydoc -- judge response based only on answer string, ignoring doc id and justification offsets");
	    System.out.println ("\tignoreoffsets -- judge response based on answer string and docid, ignoring justification offsets");
	    System.out.println ("\tnocase -- ignore case in matching answer string");
	    System.out.println ("\tslots=<slotfile> -- take list of entityId:slot pairs from slotfile");
	    System.out.println ("\t                    (otherwise list of pairs is taken from system responses)");
	    System.exit(1);
	}
	String responseFile = args[0];
	String keyFile = args[1];
	for (int i=2; i<args.length; i++) {
	    String flag = args[i];
	    if (flag.equals("trace")) {
		trace = true;
	    } else if (flag.equals("anydoc")) {
		anydoc = true;
		ignoreoffsets = true;
	    } else if (flag.equals("ignoreoffsets")) {
		ignoreoffsets = true;
	    } else if (flag.equals("nocase")) {
		nocase = true;
	    } else if (flag.startsWith("slots=")) {
		slotFile = flag.substring(6);
	    } else {
		System.out.println ("Unknown flag: " + flag);
		System.exit(1);
	    }
	}

	// ----------- read in slot judgements ------------

	BufferedReader keyReader = null;
	try {
	    keyReader = new BufferedReader (new FileReader(keyFile));
	} catch (FileNotFoundException e) {
	    System.out.println ("Unable to open judgement file " + keyFile);
	    System.exit (1);
	}
	String line;
	while ((line = keyReader.readLine()) != null) {
	    String[] fields = line.trim().split("\t", 12);
	    if (fields.length != 12) {
		System.out.println ("Warning: Invalid line in judgement file:");
		System.out.println (line);
		continue;
	    }

	    String query_id = fields[1];  //  entity_id + ":" + slot_name
	    query_id = query_id.replace(",","/");

	    String doc_id = fields[2];
	    // 2010 participant annotations may include NILs, but these need not be recorded
	    if (doc_id.equals("NIL"))
		continue;
	    if (anydoc)
		doc_id = "*";
	    String answerString = fields[3];
	    answerString = answerString.trim();
	    if (nocase)
		answerString = answerString.toLowerCase();
	    String filleroff = fields[4];
	    //	    filleroff = filleroff.trim();
	    String entityoff = fields[5];
	    //	    entityoff = entityoff.trim();
	    String predoff = fields[6];
	    //	    predoff = predoff.trim();

	    if (ignoreoffsets) {
		filleroff = "*";
		entityoff = "*";
		predoff = "*";
	    }

	    String filleroffjment = fields[7];
	    String entityoffjment = fields[8];
	    String predoffjment = fields[9];
	    String jment = fields[10];
	    int eclass = 0;
	    try {
		eclass = Integer.parseInt(fields[11]);
	    } catch (NumberFormatException e) {
		System.out.println ("Warning: Invalid line in judgement file -- invalid equivalence class:");
		System.out.println (line);
		continue;
	    }
	    if (eclass == 0)
		eclass = eclass_generator++;

	    String key = query_id + ":" + doc_id + ":" + predoff + ":" + entityoff + ":" + filleroff + ":" + answerString;
	    String J = judgement.get(key);
	    if (J != null) {  // this may happen under lenient matching: nocase, ignoreoffsets, or anydoc

	    // manage different judgments: keep the strongest
		if (! jment.equals(J)) {  
		    String strongerJ = solveDisagreement(J, jment); // pick the stronger judgment, e.g., C is preferred over W
		   // System.out.println("Warning: Multiple conflicting judgments for response " + key + ": " + jment + " vs. " + J + " => we kept " + strongerJ);
		    if(! strongerJ.equals(J)) {
		    	// update judgment
		    	judgement.put(key, strongerJ);
		    	// we might need to move the old eclass in query_eclasses and query_kb_eclasses because of new jment
		    	// remove old eclass from query_eclasses and query_kb_eclasses, if necessary
		    	int oldEclass = equivalenceClass.get(key);
		    	if(J.equals(CORRECT)) query_eclasses.get(query_id).remove(oldEclass);  // should never true, because CORRECT is strongest judgment
		    	else if(J.equals(REDUNDANT)) query_kb_eclasses.get(query_id).remove(oldEclass);  // if, e.g., "Harvard President" is in KB and "Yale President" is not in KB.... remove "Harvard President"
		    	// add it to query_eclasses and query_kb_eclasses, if necessary
		    	if(strongerJ.equals(CORRECT)) {  
		    		if (query_eclasses.get(query_id) == null)
						query_eclasses.put(query_id, new HashSet<Integer>());
				query_eclasses.get(query_id).add(oldEclass); // we'll end up collapsing oldEclass and eclass later
		    	}
		    	else if(strongerJ.equals(REDUNDANT)) {
		    		if(query_kb_eclasses.get(query_id) == null)
		    			query_kb_eclasses.put(query_id, new HashSet<Integer>());
		    		query_kb_eclasses.get(query_id).add(oldEclass); // we'll end up collapsing oldEclass and eclass later
		    	}
		    }
		}

		// we do NOT update equivalenceClass, query_eclasses, and query_kb_eclasses here
		// for now, we just keep track of equivalent eclasses 
		// after reading all keys, we replace all equivalent eclasses with a single value
		if(equivEclassesByKey.get(key) == null) 
			equivEclassesByKey.put(key, new HashSet<Integer>());
		equivEclassesByKey.get(key).add(eclass);  
		equivEclassesByKey.get(key).add(equivalenceClass.get(key)); 

	    } else { // new key, this is easy: we shouldn't have any conflicts
	    	
	    	//this is tracking the number of entries in judgement per slot type
	    	
	    String slot_type=fields[1].split(":",2)[1];
	    if(mpSlotJudgementEntriesCount.containsKey(slot_type)){
	    	mpSlotJudgementEntriesCount.put(slot_type,mpSlotJudgementEntriesCount.get(slot_type)+1);
	    }
	    else{
	    	mpSlotJudgementEntriesCount.put(slot_type, 1);
	    }
	    
		judgement.put(key, jment);
		equivalenceClass.put(key, eclass);
		predoffjudgement.put(key, predoffjment);
		entityoffjudgement.put(key, entityoffjment);
		filleroffjudgement.put(key, filleroffjment);
		if (jment.equals(CORRECT)) {
		    if (query_eclasses.get(query_id) == null)
			query_eclasses.put(query_id, new HashSet<Integer>());
		    query_eclasses.get(query_id).add(eclass);
		}
		if (jment.equals(REDUNDANT)) {
		    if (query_kb_eclasses.get(query_id) == null)
			query_kb_eclasses.put(query_id, new HashSet<Integer>());
		    query_kb_eclasses.get(query_id).add(eclass);
		}
		
	    }
	}
	System.out.println ("Read " + judgement.size() + " judgements.");

	// normalize eclasses; necessary for the lenient scoring
	for(String key: equivEclassesByKey.keySet()) {
	        int normEclass = findSmallest(equivEclassesByKey.get(key)); 
		equivalenceClass.put(key, normEclass);
		String qid = qidFromKey(key);
		normalizeTo(query_eclasses.get(qid), equivEclassesByKey.get(key), normEclass);
		normalizeTo(query_kb_eclasses.get(qid), equivEclassesByKey.get(key), normEclass);
	}

	// --------- read in system responses -------------

	BufferedReader responseReader = null;
	try {
	    responseReader = new BufferedReader (new FileReader(responseFile));
	} catch (FileNotFoundException e) {
	    System.out.println ("Unable to open responses file " + responseFile);
	    System.exit (1);
	}
	// String line;
	while ((line = responseReader.readLine()) != null) {
	    String[] fields = line.trim().split("\t", 10);
	    if (fields.length < 4 | fields.length > 10) {
		System.out.println ("Warning: Invalid line in responses file:  " + fields.length + " fields");
		System.out.println (line);
		continue;
	    }
	    String entity = fields[0];
	    String slot = fields[1];
	    String query_id = entity + ":" + slot;
	    String doc_id = fields[3];
	    if (anydoc && !doc_id.equals("NIL"))
		doc_id = "*";
	    String answer_string = "";
	    String filleroff = "*";
	    String entityoff = "*";
	    String predoff = "*";
	    String provenance = doc_id;
	    
	    if (!doc_id.equals("NIL")) {
		answer_string = ":" + fields[4].trim();
		if (!ignoreoffsets) {
		    filleroff = fields[5].trim();
		    entityoff = fields[6].trim();
		    predoff = fields[7].trim();
		}
		provenance = doc_id + ":" + predoff + ":" + entityoff + ":" + filleroff;
	    }
	    if (nocase)
		answer_string = answer_string.toLowerCase();


	    if (response.get(query_id) == null)
		response.put(query_id, new ArrayList<String>());
	    response.get(query_id).add(provenance + answer_string);
	    slots.add(query_id);
	}
	System.out.println ("Read responses for " + response.size() + " slots.");

	// -------------- read list of slots ----------
	//   separate into single and list valued slots

	if (slotFile != null)
	     slots = new TreeSet<String>(readLines(slotFile));
	List<String> svSlots = new ArrayList<String> ();
	List<String> lSlots = new ArrayList<String> ();
	for (String slot : slots) {
	    String type = slotType(slot);
	    if (type  == "single")
		svSlots.add(slot);
	    else if (type == "list")
		lSlots.add(slot);
	}

	// ------------- score responses ------------
	//          for both single-valued and list-valued slots

	// number of non-NIL responses, including those Redundant with reference KB
	int num_responses = 0;
	// number of correct non-NIL responses, excluding those Redundant with reference KB
	int num_correct = 0;

	// counts for different error types for responses
	int num_wrong = 0;  // includes spurious and incorrect
	int num_inexact = 0;
	int num_redundant = 0;  // redundant with another returned response
	int num_kb_redundant = 0;  // redundant with reference KB

	// number of Correct answers in key (that aren't in reference KB)
	//   (correct single-value answers + list-value equivalence classes)
	int num_answers = 0;

	// number of Redundant answers in key (that are in reference KB)
	//   (list-value kb equivalence classes)
	int num_kb_answers = 0;
	
	for (String query : slots) {
	    String type = slotType(query);
	    int num_answers_to_query = 0;
	    if (query_eclasses.get(query) != null) {
		if (type == "list")
		    num_answers_to_query = query_eclasses.get(query).size();
		else if (type == "single")
		    num_answers_to_query = 1;
		else {
		    System.out.println ("Warning: unrecognizable slot type " + query);
		    continue;
		}
	    }
	    num_answers += num_answers_to_query;
	    

	    int num_kb_answers_to_query = 0;
	    if (query_kb_eclasses.get(query) != null) {
		if (type == "list")
		    num_kb_answers_to_query = query_kb_eclasses.get(query).size();
		else if (type == "single")  // this shouldn't happen if SF query entities enumerate single-valued slots to ignore because they're already filled in the reference KB
		    num_kb_answers_to_query = 1;
		else {
		    System.out.println ("Warning: unrecognizable slot type " + query);
		    continue;
		}
	    }
	    num_kb_answers += num_kb_answers_to_query;
	    
	    
	    String slot_type=query.split(":",2)[1];
	    if(mpSlotResponseEntriesCount.containsKey(slot_type)){
	    	mpSlotResponseEntriesCount.put(slot_type,mpSlotResponseEntriesCount.get(slot_type)+num_answers_to_query+num_kb_answers_to_query);
	    }
	    else{
	    	mpSlotResponseEntriesCount.put(slot_type, num_answers_to_query+num_kb_answers_to_query);
	    }
	    

	    List<String> responseList = response.get(query);
	    if (responseList == null) {
		System.out.println ("Warning: No system response for slot " + query);
		continue;
	    }
	    int num_responses_to_query = responseList.size();  // used only for issuing warnings, not for computing scores
	    if (type == "single") {
		if (num_responses_to_query > 1) {
		    System.out.println ("Warning: Ignoring all but first response among multiple responses for single-valued slot " + query);
		    responseList = responseList.subList(0,0);
		    num_responses_to_query = responseList.size();
		    if (num_responses_to_query != 1) {
			System.out.println ("Error: unable to take first of multiple responses for single-valued slot for query " + query);
			System.exit (1);
		    }
		}
	    }
	    Set<Integer> distincts = new HashSet<Integer>();
	    for (String responseString : responseList) {
		String fields[] = responseString.split(":",5);
		String doc_id = fields[0];
		//		String answer_string = "";
		//		if (fields.length == 5)
		//		    answer_string = fields[4];
		String symbol = "?";
		if (doc_id.equals("NIL")) {
		    if (num_responses_to_query > 1)
			System.out.println ("Warning: More than one response, including NIL, for " + query);
		    num_responses_to_query = 0; //issue warning only once for query; don't warn again when we encounter other, possibly non-NIL, responses to query
		    if (num_answers_to_query > 0) {
			// missing filler in system response
			symbol = "M";
		    } else if (num_kb_answers_to_query > 0) {
			symbol = "m"; // missing a filler that is already in the reference KB
		    } else {
			symbol = "c"; // "correctly" discovers that there are no known fillers in the corpus
		    }
		} else /* non-NIL system response */ {
		    num_responses++;
		    String key = query + ":" + responseString;
		    String J = judgement.get(key);
		    if (J == null) {
			System.out.println ("Warning: No judgement for " + key);
			J = WRONG;
		    }
		    symbol = J;
		    if (J.equals(IGNORE) || J.equals(WRONG)) {
			num_wrong++;
		    } else if (J.equals(INEXACT)) {
			num_inexact++;
		    } else if (J.equals(REDUNDANT)) {
			Integer E = equivalenceClass.get(key);
			if (distincts.contains(E)) {
			    num_redundant++;
			    if(mpSlotCorrectResponseEntriesCount.containsKey(slot_type)){
			    	mpSlotCorrectResponseEntriesCount.put(slot_type, mpSlotCorrectResponseEntriesCount.get(slot_type)+1);
			    }
			    else{
			    	mpSlotCorrectResponseEntriesCount.put(slot_type, 1);
			    }
			    symbol = "r";   // redundant with other returned response
			} else {
			    num_kb_redundant++;
			    distincts.add(E);
			}
		    } else if (J.equals(CORRECT)) {
			Integer E = equivalenceClass.get(key);
			if (distincts.contains(E)) {
			    num_redundant++;
			    symbol = "r";   // redundant with other returned response
			} else {
			    num_correct++;
			    distincts.add(E);
			    if(mpSlotCorrectResponseEntriesCount.containsKey(slot_type)){
			    	mpSlotCorrectResponseEntriesCount.put(slot_type, mpSlotCorrectResponseEntriesCount.get(slot_type)+1);
			    }
			    else{
			    	mpSlotCorrectResponseEntriesCount.put(slot_type, 1);
			    }
			}
		    } else {
			System.out.println ("ERROR: Invalid judgement " + J);
			System.exit (1);
		    }
		}
	    if (trace)
		System.out.println (symbol + " " + query + " " + responseString);
	    }
	}

	System.out.println ("\n======== Summary Statistics ===========");
	if (slotFile != null)
	    System.out.println ("Slot lists taken from file " + slotFile);
	else
	    System.out.println ("Slot lists taken from system responses");
	System.out.println ("Slot lists include " + svSlots.size() + " single valued slots");
	System.out.println ("               and " +  lSlots.size() + " list-valued slots");
	System.out.println ("\nNumber of filled slots in key that are not in reference KB: " + num_answers);
	System.out.println ("Number of filled slots in key that are in reference KB: " + num_kb_answers);
	System.out.println ("\nNumber of filled slots in responses: " + num_responses);
	System.out.println ("\tNumber Correct (not in reference KB): " + num_correct);
	System.out.println ("\tNumber Redundant with reference KB: " + num_kb_redundant);
	System.out.println ("\tNumber redundant with another response: " + num_redundant);
	System.out.println ("\tNumber inexact: " + num_inexact);
	System.out.println ("\tNumber incorrect / spurious: " + num_wrong);



	recall = ((float) num_correct) / num_answers;
	precision = ((float) num_correct) / (num_responses - num_kb_redundant);  // don't penalize for fillers already in reference KB
	F = (2 * recall * precision) / (recall + precision);
	System.out.println ("\nDiagnostic scores (ignoring slot fillers in key and responses that are already in reference KB):");
	System.out.println ("\tDiagnostic Recall: " + num_correct + " / " + num_answers + " = " + recall);
	System.out.println ("\tDiagnostic Precision: " + num_correct + " / (" + num_responses + "-" + num_kb_redundant + ") = " + precision);
	System.out.println ("\tDiagnostic F1: " + F);

	recall = ((float) (num_correct + num_kb_redundant)) / (num_answers + num_kb_answers);
	precision = ((float) (num_correct + num_kb_redundant)) / (num_responses);  
	F = (2 * recall * precision) / (recall + precision);
	System.out.println ("\nOfficial Scores (requiring slot fillers that are already in reference KB):");
	System.out.println ("\tRecall: (" + num_correct + "+" + num_kb_redundant + ") / (" + num_answers + "+" + num_kb_answers + ") = " + recall);
	System.out.println ("\tPrecision: (" + num_correct + "+" + num_kb_redundant + ") / " + num_responses + " = " + precision);
	System.out.println ("\tF1: " + F);
	
	
	for(String key : mpSlotCorrectResponseEntriesCount.keySet()){
		double ncorrect =  mpSlotCorrectResponseEntriesCount.get(key);
		double nresponses = mpSlotResponseEntriesCount.get(key);
		double njudgement = mpSlotJudgementEntriesCount.get(key);
		//System.out.println("key: "+key+ " correct responses: "+ncorrect);
		//System.out.println("key: "+key+ " total responses: "+nresponses);
		//System.out.println("key: "+key+ " judgement responses: "+njudgement);
		
		Double prec = new Double(0.0);
		Double rec =  new Double(0.0);
		Double f1 = new Double(0.0);
		
		prec=ncorrect/nresponses;
		rec=ncorrect/njudgement;
		f1=2*prec*rec/(prec+rec);
		
		mpSlotConfidence_precision.put(key,prec);
		mpSlotConfidence_recall.put(key, rec);
		mpSlotConfidence_f1.put(key,f1);	
		
	}
	
//	for(String k : mpSlotConfidence_precision.keySet() ){
//			System.out.println(k+"\t : \t"+mpSlotConfidence_precision.get(k));
//	}
//	for(String k : mpSlotConfidence_recall.keySet() ){
//		System.out.println(k+"\t : \t"+mpSlotConfidence_recall.get(k));
//	}
//	for(String k : mpSlotConfidence_f1.keySet() ){
//		System.out.println(k+"\t : \t"+mpSlotConfidence_f1.get(k));
//	}
	
		
    }

    /**
     *  reads a series of lines from 'fileName' and returns them as a list of Strings
     */

    static List<String> readLines (String fileName) {
	BufferedReader reader = null;
	List<String> lines = new ArrayList<String>();
	try {
	    reader = new BufferedReader (new FileReader(fileName));
	} catch (FileNotFoundException e) {
	    System.out.println ("Unable to open file " + fileName);
	    System.exit (1);
	}
	String line;
	try {
	    while ((line = reader.readLine()) != null) {
		lines.add(line.trim());
	    }
	} catch (IOException e) {
	    System.out.println ("Error readng from file " + fileName);
	    System.exit (1);
	}
	System.out.println ("Read " + lines.size() + " lines from " + fileName);
	return lines;
    }

    static List<String> singleValuedSlots = Arrays.asList(
	"per:date_of_birth",
	"per:age",
	"per:country_of_birth",
	"per:stateorprovince_of_birth",
	"per:city_of_birth",
	"per:date_of_death",
	"per:country_of_death",
	"per:stateorprovince_of_death",
	"per:city_of_death",
	"per:cause_of_death",
	"per:religion",
	"org:number_of_employees_members",
	"org:date_founded",
	"org:date_dissolved",
	"org:country_of_headquarters",
	"org:stateorprovince_of_headquarters",
	"org:city_of_headquarters",
	"org:website");

    static List<String> listValuedSlots = Arrays.asList(
	"per:alternate_names",
	"per:origin",
	"per:countries_of_residence",
	"per:statesorprovinces_of_residence",
	"per:cities_of_residence",
	"per:schools_attended",
	"per:title",
	//	"per:member_of",
	//	"per:employee_of",
	"per:employee_or_member_of",
	"per:spouse",
	"per:children",
	"per:parents",
	"per:siblings",
	"per:other_family",
	"per:charges",
	"org:alternate_names",
	"org:political_religious_affiliation",
	"org:top_members_employees",
	"org:members",
	"org:member_of",
	"org:subsidiaries",
	"org:parents",
	"org:founded_by",
	"org:shareholders",
	"per:awards_won",
	"per:charities_supported",
	"per:diseases",
	"org:products",
	"per:pos-from",
	"per:neg-from",
	"per:pos-towards",
	"per:neg-towards",
	"org:pos-from",
	"org:neg-from",
	"org:pos-towards",
	"org:neg-towards",
	"gpe:pos-from",
	"gpe:neg-from",
	"gpe:pos-towards",
	"gpe:neg-towards");
    /*
     * given entityId:slot, classify slot as "single" or "list" valued
     */

    static String slotType (String slot) {
	String[] slotFields = slot.split(":", 2);
	if (slotFields.length != 2) {
	    System.out.println("Invalid slot " + slot);
	    return "error";
	}
	if (singleValuedSlots.contains(slotFields[1]))
	    return "single";
	if (listValuedSlots.contains(slotFields[1]))
	    return "list";
	System.out.println("Invalid slot " + slot);
        // return "list" if you want 2009 slots to be scored too
	return "error"; 
    }

    private static String solveDisagreement(String j1, String j2) {
    	if(j1.equals("C") || j2.equals("C")) return "C";
    	if(j1.equals("R") || j2.equals("R")) return "R";
    	if(j1.equals("I") || j2.equals("I")) return "I";
    	if(j1.equals("X") || j2.equals("X")) return "X";
    	if(j1.equals("W") || j2.equals("W")) return "W";
    	throw new RuntimeException("Unknown assessment type: " + j1 + " and " + j2);
    }

    private static int findSmallest(Set<Integer> s) {
    	int min = Integer.MAX_VALUE;
    	for(Integer e: s) {
    		if(e < min) {
    			min = e;
    		}
    	}
    	return min;
    }

    private static String qidFromKey(String key) {
    	String [] bits = key.split(":");
    	assert(bits[0].startsWith("SF"));
    	assert(bits[1].equals("per") || bits[1].equals("org"));
    	String qid = bits[0] + ":" + bits[1] + ":" + bits[2];
    	//System.out.println("Using qid " + qid + " from key " + key);
    	return qid;
    }

    private static void normalizeTo(Set<Integer> eclasses, Set<Integer> equivs, int norm) {
    	if(eclasses == null) return;
    	// replace all equivalent eclasses with the normalized one
    	boolean found = false;
    	for(Integer eq: equivs){
    		if(eclasses.contains(eq)) {
    			eclasses.remove(eq);
    			found = true;
    		}
    	}
    	if(found) eclasses.add(norm);
    }
}
