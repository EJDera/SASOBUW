package edu.eur.absa.OntBuilding;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.apache.commons.lang3.time.StopWatch;

import edu.eur.absa.Framework;

/***
 * A class to perform parameter optimization
 * 
 * @author Ewelina Dera
 *
 */
public class GridSearchParametersMain 
{

	public static void main(String[] args) throws IOException 
	{
		Framework.suppressJenaMessages();
		
		//All the variables below are the parameters that have to be set/chosen
		String domain = "Restaurant";
		String pathToAspectsCSV = Framework.DATA_PATH + "Possible_aspects.csv";
		int nrOfCategories = 6;
		int nrOfAttributes = 5;
		ArrayList<String> omissions = new ArrayList<>();	//Categories/attributes to be omitted - i.e. the ones that do not create parent classes in the ontology
		omissions.add("General");
		omissions.add("Miscellaneous");
		
		String pathToEmbeddingsFile = Framework.DATA_PATH + "GloVe.txt";
		int dimensions = 300;
		
		boolean WordNet = true;		
		boolean basicApproach = false;
		boolean useTriples = true;
		
		//Also lines 752-753 in OntHeper.java need to be changed for fastText or Glove embeddings
		
				
		String ontName = domain;
		
		if (WordNet)
		{
			ontName = ontName + "-Synsets";
		}
		
		if (basicApproach)
		{
			ontName = ontName + "-BasicApproach";
		}
		else if (!basicApproach)
		{
			ontName = ontName + "-ExtendedApproach";
		}
		
		if (useTriples)
		{
			ontName = ontName + "-Triples";
		}
		
		//File where the results of parameter optimization for term extraction will be saved
		PrintWriter parameters_extraction = new PrintWriter(Framework.DATA_PATH + "ParametersTermExtraction.txt");
		
		//Range of the grid search for term extraction
		for (double i = 0.6; i >= 0.4; i = i - 0.02)
		{
			System.out.println("Current extraction threshold: " + i);
			
			ArrayList<Double> thresholds_extraction = new ArrayList<>();
			thresholds_extraction.add(i);	//Verbs
			thresholds_extraction.add(i);	//Nouns
			thresholds_extraction.add(i);	//Adjectives
			thresholds_extraction.add(i);	//Generic verbs
			thresholds_extraction.add(i);	//Generic nouns
			thresholds_extraction.add(i);	//Generic adjectives
			
			Ontology base = new Ontology(Framework.EXTERNALDATA_PATH + domain + "OntologyBase.owl");		
			OntHelper ontology = new OntHelper(base);
			ontology.createAspectClasses(pathToAspectsCSV, nrOfCategories + 1, nrOfAttributes + 1, omissions, pathToEmbeddingsFile, dimensions);			
			
			int[] result_extraction = ontology.extractTerms(pathToEmbeddingsFile, thresholds_extraction, WordNet);
			
			double ratio_verbs = ((double) result_extraction[0]) / (result_extraction[0] + result_extraction[3]);
			double harmonic_verbs = 2.0 / ((1 / result_extraction[0]) + (1 / ratio_verbs));
			parameters_extraction.println("Verbs with threshold: " + i + " --> Accepted terms: " + result_extraction[0] + " Rejected terms: " + result_extraction[3] + " Acceptance ratio: " + ratio_verbs + " Harmonic mean: " + harmonic_verbs);
		
			double ratio_nouns = ((double) result_extraction[1]) / (result_extraction[1] + result_extraction[4]);
			double harmonic_nouns = 2.0 / ((1 / result_extraction[1]) + (1 / ratio_nouns));
			parameters_extraction.println("Nouns with threshold: " + i + " --> Accepted terms: " + result_extraction[1] + " Rejected terms: " + result_extraction[4] + " Acceptance ratio: " + ratio_nouns + " Harmonic mean: " + harmonic_nouns);
		
			double ratio_adjs = ((double) result_extraction[2]) / (result_extraction[2] + result_extraction[5]);
			double harmonic_adjs = 2.0 / ((1 / result_extraction[2]) + (1 / ratio_adjs));
			parameters_extraction.println("Adjectives with threshold: " + i + " --> Accepted terms: " + result_extraction[2] + " Rejected terms: " + result_extraction[5] + " Acceptance ratio: " + ratio_adjs + " Harmonic mean: " + harmonic_adjs);		
		
			double ratio_verbsGen = ((double) result_extraction[6]) / (result_extraction[6] + result_extraction[9]);
			double harmonic_verbsGen = 2.0 / ((1 / result_extraction[6]) + (1 / ratio_verbsGen));
			parameters_extraction.println("Generic verbs with threshold: " + (i - 1) + " --> Accepted terms: " + result_extraction[6] + " Rejected terms: " + result_extraction[9] + " Acceptance ratio: " + ratio_verbsGen + " Harmonic mean: " + harmonic_verbsGen);
			
			double ratio_nounsGen = ((double) result_extraction[7]) / (result_extraction[7] + result_extraction[10]);
			double harmonic_nounsGen = 2.0 / ((1 / result_extraction[7]) + (1 / ratio_nounsGen));
			parameters_extraction.println("Generic nouns with threshold: " + i + " --> Accepted terms: " + result_extraction[7] + " Rejected terms: " + result_extraction[10] + " Acceptance ratio: " + ratio_nounsGen + " Harmonic mean: " + harmonic_nounsGen);
		
			double ratio_adjsGen = ((double) result_extraction[8]) / (result_extraction[8] + result_extraction[11]);
			double harmonic_adjsGen = 2.0 / ((1 / result_extraction[8]) + (1 / ratio_adjsGen));
			parameters_extraction.println("Generic adjectives with threshold: " + i + " --> Accepted terms: " + result_extraction[8] + " Rejected terms: " + result_extraction[11] + " Acceptance ratio: " + ratio_adjsGen + " Harmonic mean: " + harmonic_adjsGen);		
		}
		
		parameters_extraction.close();
		//End of parameter optimization for term extraction
				
		//Best parameters found for term extraction		
		ArrayList<Double> thresholds_extraction = new ArrayList<>();
		thresholds_extraction.add(0.46);	//Verbs
		thresholds_extraction.add(0.54);	//Nouns
		thresholds_extraction.add(0.48);	//Adjectives
		thresholds_extraction.add(0.56);	//Generic verbs
		thresholds_extraction.add(0.54);	//Generic nouns
		thresholds_extraction.add(0.56);	//Generic adjectives
	
		//File where the results of parameter optimization for hierarchy building of sentiment concepts will be saved
		PrintWriter parameters_hierarchySent = new PrintWriter(Framework.DATA_PATH + "ParametersHierarchySentiments.txt");
		//File where the results of parameter optimization for hierarchy building of aspect concepts will be saved
		PrintWriter parameters_hierarchyAsp = new PrintWriter(Framework.DATA_PATH + "ParametersHierarchyAspects.txt");
		
		//Range of the grid search for hierarchy building
		for (double j = 0.85; j >= 0.70; j = j - 0.02)
		{				
//			final StopWatch stopwatch = new StopWatch();
//			stopwatch.start();
			
			System.out.println("Current hierarchy threshold: " + j);
			
			Ontology base = new Ontology(Framework.EXTERNALDATA_PATH + domain + "OntologyBase.owl");		
			OntHelper ontology = new OntHelper(base);
			ontology.createAspectClasses(pathToAspectsCSV, nrOfCategories + 1, nrOfAttributes + 1, omissions, pathToEmbeddingsFile, dimensions);			
			
			int[] result_extraction = ontology.extractTerms(pathToEmbeddingsFile, thresholds_extraction, WordNet);
			
			ontology.conceptualization();
			base.save(ontName + "-AfterConceptualization-" + j + ".owl");
			
			ArrayList<Double> thresholds_hierarchy = new ArrayList<>();
			thresholds_hierarchy.add(j);	//Aspect verbs
			thresholds_hierarchy.add(j);	//Aspect nouns
			thresholds_hierarchy.add(j);	//Aspect adjectives
			thresholds_hierarchy.add(j - 0.70);	//Type-3 verbs
			thresholds_hierarchy.add(j - 0.70);	//Type-3 nouns
			thresholds_hierarchy.add(j - 0.70);	//Type-3 adjectives
				
			ArrayList<int[]> result_hierarchy = ontology.buildHierarchy(thresholds_hierarchy, useTriples, basicApproach);
		
			//Save the built ontology
			base.save(ontName + "-OntologyFull-" + j + ".owl");
			
			int[] sent = result_hierarchy.get(0);
			
			double ratio_verbsSentType2 = ((double) sent[0]) / (sent[0] + sent[3]);
			double harmonic_verbsSentType2 = 2.0 / ((1 / sent[0]) + (1 / ratio_verbsSentType2));
			parameters_hierarchySent.println("Type-2 verbs with threshold: " + j + " --> Accepted terms: " + sent[0] + " Rejected terms: " + sent[3] + " Acceptance ratio: " + ratio_verbsSentType2 + " Harmonic mean: " + harmonic_verbsSentType2);
			
			double ratio_nounsSentType2 = ((double) sent[1]) / (sent[1] + sent[4]);
			double harmonic_nounsSentType2 = 2.0 / ((1 / sent[1]) + (1 / ratio_nounsSentType2));
			parameters_hierarchySent.println("Type-2 nouns with threshold: " + j + " --> Accepted terms: " + sent[1] + " Rejected terms: " + sent[4] + " Acceptance ratio: " + ratio_nounsSentType2 + " Harmonic mean: " + harmonic_nounsSentType2);
		
			double ratio_adjsSentType2 = ((double) sent[2]) / (sent[2] + sent[5]);
			double harmonic_adjsSentType2 = 2.0 / ((1 / sent[2]) + (1 / ratio_adjsSentType2));
			parameters_hierarchySent.println("Type-2 adjectives with threshold: " + j + " --> Accepted terms: " + sent[2] + " Rejected terms: " + sent[5] + " Acceptance ratio: " + ratio_adjsSentType2 + " Harmonic mean: " + harmonic_adjsSentType2);					
		
			double ratio_verbsSentType3 = ((double) sent[6]) / (sent[6] + sent[9]);			
			double harmonic_verbsSentType3 = 2.0 / ((1 / sent[6]) + (1 / ratio_verbsSentType3));
			parameters_hierarchySent.println("Type-3 verbs with threshold: " + j + " --> Accepted terms: " + sent[6] + " Rejected terms: " + sent[9] + " Acceptance ratio: " + ratio_verbsSentType3 + " Harmonic mean: " + harmonic_verbsSentType3);
			
			double ratio_nounsSentType3 = ((double) sent[7]) / (sent[7] + sent[10]);
			double harmonic_nounsSentType3 = 2.0 / ((1 / sent[7]) + (1 / ratio_nounsSentType3));
			parameters_hierarchySent.println("Type-3 nouns with threshold: " + j + " --> Accepted terms: " + sent[7] + " Rejected terms: " + sent[10] + " Acceptance ratio: " + ratio_nounsSentType3 + " Harmonic mean: " + harmonic_nounsSentType3);
			
			double ratio_adjsSentType3 = ((double) sent[8]) / (sent[8] + sent[11]);
			double harmonic_adjsSentType3 = 2.0 / ((1 / sent[8]) + (1 / ratio_adjsSentType3));
			parameters_hierarchySent.println("Type-3 adjectives with threshold: " + j + " --> Accepted terms: " + sent[8] + " Rejected terms: " + sent[11] + " Acceptance ratio: " + ratio_adjsSentType3 + " Harmonic mean: " + harmonic_adjsSentType3);					
			
			int[] asp = result_hierarchy.get(1);
			
			double ratio_verbsAsp = ((double) asp[0]) / (asp[0] + asp[3]);
			double harmonic_verbsAsp = 2.0 / ((1 / asp[0]) + (1 / ratio_verbsAsp));
			parameters_hierarchyAsp.println("Aspect verbs with threshold: " + j + " --> Accepted terms: " + asp[0] + " Rejected terms: " + asp[3] + " Acceptance ratio: " + ratio_verbsAsp + " Harmonic mean: " + harmonic_verbsAsp);
			
			double ratio_nounsAsp = ((double) asp[1]) / (asp[1] + asp[4]);
			double harmonic_nounsAsp = 2.0 / ((1 / asp[1]) + (1 / ratio_nounsAsp));
			parameters_hierarchyAsp.println("Aspect nouns with threshold: " + j + " --> Accepted terms: " + asp[1] + " Rejected terms: " + asp[4] + " Acceptance ratio: " + ratio_nounsAsp + " Harmonic mean: " + harmonic_nounsAsp);
		
			double ratio_adjsAsp = ((double) asp[2]) / (asp[2] + asp[5]);
			double harmonic_adjsAsp = 2.0 / ((1 / asp[2]) + (1 / ratio_adjsAsp));
			parameters_hierarchyAsp.println("Aspect adjectives with threshold: " + j + " --> Accepted terms: " + asp[2] + " Rejected terms: " + asp[5] + " Acceptance ratio: " + ratio_adjsAsp + " Harmonic mean: " + harmonic_adjsAsp);					
			
//			parameters_hierarchyAsp.println("Total ontology building time " + stopwatch);
		}
		
		parameters_hierarchySent.close();
		parameters_hierarchyAsp.close();
				
	}

}
