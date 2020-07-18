package edu.eur.absa.OntBuilding;

import java.io.IOException;
import java.util.ArrayList;
import org.apache.commons.lang3.time.StopWatch;
import edu.eur.absa.Framework;

/***
 * A class that creates an ontology with the chosen parameters and building approaches
 * 
 * @author Ewelina Dera
 *
 */
public class MainOntBuilder 
{
	public static void main(String[] args) throws IOException 
	{		
		final StopWatch stopwatch = new StopWatch();
		stopwatch.start();
//		System.out.println("Start time " + stopwatch.toString());

		Framework.suppressJenaMessages();
		
		//All the variables below are the parameters that have to be set/chosen
		String domain = "Restaurant";
		String pathToAspectsCSV = Framework.DATA_PATH + "Possible_aspects.csv";
		int nrOfCategories = 6;
		int nrOfAttributes = 5;
		ArrayList<String> omissions = new ArrayList<>();	//Categories/attributes to be omitted - i.e. the ones that do not create parent classes in the ontology
		omissions.add("General");
		omissions.add("Miscellaneous");
		
		String pathToEmbeddingsFile = Framework.DATA_PATH + "fastText.bin";
		int dimensions = 300;
		
		ArrayList<Double> thresholds_extraction = new ArrayList<>();
		thresholds_extraction.add(0.46);	//Verbs
		thresholds_extraction.add(0.54);	//Nouns
		thresholds_extraction.add(0.48);	//Adjectives
		thresholds_extraction.add(0.56);	//Generic verbs
		thresholds_extraction.add(0.54);	//Generic nouns
		thresholds_extraction.add(0.56);	//Generic adjectives
		boolean WordNet = true;
		
		ArrayList<Double> thresholds_hierarchy = new ArrayList<>();
		thresholds_hierarchy.add(0.85);	//Aspect verbs
		thresholds_hierarchy.add(0.85);	//Aspect nouns
		thresholds_hierarchy.add(0.85);	//Aspect adjectives
		thresholds_hierarchy.add(0.85 - 0.70);	//Type-3 verbs
		thresholds_hierarchy.add(0.85 - 0.70);	//Type-3 nouns
		thresholds_hierarchy.add(0.85 - 0.70);	//Type-3 adjectives
		boolean basicApproach = true;
		boolean useTriples = false;
		
		//Also line 752-753 in OntHeper.java need to be changed for fastText or Glove embeddings
		
		
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
		
		Ontology base = new Ontology(Framework.EXTERNALDATA_PATH + domain + "OntologyBase.owl");		
		OntHelper ontology = new OntHelper(base);
		
		//Create the skeleton of the ontology with the parent classes
		ontology.createAspectClasses(pathToAspectsCSV, nrOfCategories + 1, nrOfAttributes + 1, omissions, pathToEmbeddingsFile, dimensions);		
		base.save(ontName + "-SkeletalOntology.owl");
		
		//Extract terms
		ontology.extractTerms(pathToEmbeddingsFile, thresholds_extraction, WordNet);
		
		//Create concepts
		ontology.conceptualization();
		
		base.save(ontName + "-AfterConceptualization.owl");
			
		//Build hierarchy
		ontology.buildHierarchy(thresholds_hierarchy, useTriples, basicApproach);
		
		//Save the final ontology
		base.save(ontName + "-OntologyFull.owl");
		
		System.out.println("Total ontology building time " + stopwatch);
		
	}

}
