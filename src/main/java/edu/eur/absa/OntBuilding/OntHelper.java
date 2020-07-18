package edu.eur.absa.OntBuilding;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.util.Pair;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;

import edu.eur.absa.Framework;
import edu.smu.tspell.wordnet.NounSynset;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.VerbSynset;
import edu.smu.tspell.wordnet.WordNetDatabase;

/***
 * A class with all the relevant methods for building an ontology
 * 
 * @author Ewelina Dera
 *
 */
public class OntHelper 
{
	private Ontology base;
	
	private String mention;
	private String sentiment;
	
	private String actionMention;
	private String entityMention;
	private String propertyMention;
	
	private String positiveSentiment;
	private String neutralSentiment;
	private String negativeSentiment;
	
	private ArrayList<String> aspectParentClasses = new ArrayList<>();
	private ArrayList<String> sentimentParentClasses = new ArrayList<>();
	private ArrayList<String> genericParentClasses = new ArrayList<>();
	
	private int acceptedVerbs = 0;
	private int acceptedNouns = 0;
	private int acceptedAdjectives = 0;
	
	private int acceptedGenericVerbs = 0;
	private int acceptedGenericNouns = 0;
	private int acceptedGenericAdjectives = 0;
	
	private int rejectedVerbs = 0;
	private int rejectedNouns = 0;
	private int rejectedAdjectives = 0;
	
	private int rejectedGenericVerbs = 0;
	private int rejectedGenericNouns = 0;
	private int rejectedGenericAdjectives = 0;
	
	private int embeddingsDimensions = 0;
	
	private HashMap<String, double[]> verbs = new HashMap<>();
	private HashMap<String, double[]> nouns = new HashMap<>();
	private HashMap<String, double[]> adjectives = new HashMap<>();
	
	private int acceptedHierarchiesVerbsSentType2 = 0;
	private int rejectedHierarchiesVerbsSentType2 = 0;	
	private int acceptedHierarchiesNounsSentType2 = 0;
	private int rejectedHierarchiesNounsSentType2 = 0;
	private int acceptedHierarchiesAdjsSentType2 = 0;
	private int rejectedHierarchiesAdjsSentType2 = 0;
	
	private int acceptedHierarchiesVerbsSentType3 = 0;
	private int rejectedHierarchiesVerbsSentType3 = 0;	
	private int acceptedHierarchiesNounsSentType3 = 0;
	private int rejectedHierarchiesNounsSentType3 = 0;
	private int acceptedHierarchiesAdjsSentType3 = 0;
	private int rejectedHierarchiesAdjsSentType3 = 0;
	
	private int acceptedHierarchiesVerbsAspects = 0;
	private int rejectedHierarchiesVerbsAspects = 0;	
	private int acceptedHierarchiesNounsAspects = 0;
	private int rejectedHierarchiesNounsAspects = 0;
	private int acceptedHierarchiesAdjsAspects = 0;
	private int rejectedHierarchiesAdjsAspects = 0;

	public OntHelper(Ontology baseOnt) 
	{
		//Create the skeletal classes
		base = baseOnt;
		
		mention = base.addClass("Mention");
		sentiment = base.addClass("Sentiment");
		
		actionMention = base.addClass("ActionMention", mention);
		entityMention = base.addClass("EntityMention", mention);
		propertyMention = base.addClass("PropertyMention", mention);
		positiveSentiment = base.addClass("Positive", sentiment);
		neutralSentiment = base.addClass("Neutral", sentiment);
		negativeSentiment = base.addClass("Negative", sentiment);
		
		//Create the classes for Type-1 sentiments
		String genericPositiveAction = base.addClass("GenericPositiveAction", actionMention, positiveSentiment);
		genericParentClasses.add(genericPositiveAction);
		String genericNegativeAction = base.addClass("GenericNegativeAction", actionMention, negativeSentiment);
		genericParentClasses.add(genericNegativeAction);
		String genericPositiveEntity = base.addClass("GenericPositiveEntity", entityMention, positiveSentiment);
		genericParentClasses.add(genericPositiveEntity);
		String genericNegativeEntity = base.addClass("GenericNegativeEntity", entityMention, negativeSentiment);
		genericParentClasses.add(genericNegativeEntity);
		String genericPositiveProperty = base.addClass("GenericPositiveProperty", propertyMention, positiveSentiment);
		genericParentClasses.add(genericPositiveProperty);
		String genericNegativeProperty = base.addClass("GenericNegativeProperty", propertyMention, negativeSentiment);		
		genericParentClasses.add(genericNegativeProperty);
	}
	
	/***
	 * A method to merge multiple hash maps into one
	 * 
	 * @param input list of hash maps to be merged
	 * @return merged hash map
	 */
	public HashMap<String, double[]> mergeHashMaps(ArrayList<HashMap<String, double[]>> input)
	{
		HashMap<String, double[]> output = new HashMap<>();
		
		for (HashMap<String, double[]> hashMap : input)
		{
			for (String key : hashMap.keySet())
			{
				output.put(key, hashMap.get(key));
			}
		}
		
		return output;
	}
	
	/***
	 * A method to create the aspect (category/attribute) parent classes
	 * 
	 * @param path path to a CSV file with the relevant aspects
	 * @param rows number of rows in the CSV file
	 * @param columns number of columns in the CSV file
	 * @param omissions categories/attributes which should not be separate classes
	 * @param pathToEmbeddingsFile path to the file with the word embeddings
	 * @param dimensions dimensions of the word embeddings
	 * @throws IOException
	 */
	public void createAspectClasses(String path, int rows, int columns, ArrayList<String> omissions, String pathToEmbeddingsFile, int dimensions) throws IOException
	{
		embeddingsDimensions = dimensions;
		
		//File where the vectors representing seeds of different classes will be saved
		PrintWriter lexicalizationsVectors = new PrintWriter(Framework.DATA_PATH + "VectorsForClassNames.txt");
		
		//Hash map with all of the vectors
		ArrayList<HashMap<String, double[]>> embeddingsAll = readEmbeddingsFromFile(pathToEmbeddingsFile);						
		HashMap<String, double[]> embeddings = mergeHashMaps(embeddingsAll);
		
		//2D array with the possible category-attribute combinations
		String[][] aspects = readCSV(path, rows, columns);
		
		ArrayList<String> categories = new ArrayList<>();
		for (int i = 1; i <= rows - 1; i++)
		{
			categories.add(aspects[i][0]);
//			System.out.println("Adding category: " + aspects[i][0]);
		}
		
		ArrayList<String> attributes = new ArrayList<>();
		for (int j = 1; j <= columns - 1; j++)
		{
			attributes.add(aspects[0][j]);
//			System.out.println("Adding attribute: " + aspects[0][j]);
		}
			
		for (String category : categories)
		{
			//Hash set with all of the aspects with the given category
			HashSet<String> aspectsWithCategory = new HashSet<>();
			
			for (int k = 1; k <= rows - 1; k++)
			{
				if (aspects[k][0].equals(category))
				{
					for (int l = 1; l <= columns - 1; l++)
					{
						if (aspects[k][l].equals("1"))
						{
							String aspectName = category + "#" + aspects[0][l];
							aspectsWithCategory.add(aspectName);
						}
					}
				}
			}
			
			if (omissions.contains(category))
			{
//				System.out.println("Omitted: " + category);
			}
			else
			{
				
				String categoryAction = "";
				String categoryEntity = "";
				String categoryProperty = "";
				
				System.out.println("Creating aspect classes for: " + category);
				
				//If category consists of multiple seeds/lexicalizations
				if (category.contains("&"))
				{
//					System.out.println("Category contains & sign");
					
					String[] multipleLex = category.split("&");
	
					//For each seed/lexicalization
					for (int m = 0; m <= multipleLex.length - 1; m++)
					{						
						if (m == 0)
						{
							String lex = multipleLex[m];
							
							if (lex.substring(lex.length() - 1, lex.length()).equals("s"))
							{
								lex = lex.substring(0, lex.length() - 1);
							}
							
							System.out.println("Adding first lexicalisation: " + lex);
							
							categoryAction = base.addClass(category + "ActionMention", true, lex.toLowerCase(), true, aspectsWithCategory, actionMention);
							aspectParentClasses.add(categoryAction);
							categoryEntity = base.addClass(category + "EntityMention", true, lex.toLowerCase(), true, aspectsWithCategory, entityMention);
							aspectParentClasses.add(categoryEntity);
							categoryProperty = base.addClass(category + "PropertyMention", true, lex.toLowerCase(), true, aspectsWithCategory, propertyMention);
							aspectParentClasses.add(categoryProperty);
													
							for (String key : embeddings.keySet())
							{								
								if (key.equals(lex.toLowerCase() + "-noun"))
								{
									lexicalizationsVectors.println(lex.toLowerCase() + "-noun " + printVectorArray(embeddings.get(key)));
								}
							}
						}
						else
						{
							if (multipleLex[m].substring(multipleLex[m].length() - 1, multipleLex[m].length()).equals("s"))
							{
								multipleLex[m] = multipleLex[m].substring(0, multipleLex[m].length() - 1);
							}
							
//							System.out.println("Adding another lexicalisation: " + multipleLex[m]);
							
							base.addLexicalization(categoryAction, multipleLex[m].toLowerCase());
							base.addLexicalization(categoryEntity, multipleLex[m].toLowerCase());
							base.addLexicalization(categoryProperty, multipleLex[m].toLowerCase());
							
							for (String key : embeddings.keySet())
							{								
								if (key.equals(multipleLex[m].toLowerCase() + "-noun"))
								{
									lexicalizationsVectors.println(multipleLex[m].toLowerCase() + "-noun " + printVectorArray(embeddings.get(key)));
								}
							}
						}	
					}
				}
				//If category contains only one seed/lexicalization
				else
				{					
					String categorySingular = category;
					
					if (categorySingular.substring(categorySingular.length() - 1, categorySingular.length()).equals("s"))
					{
						categorySingular = categorySingular.substring(0, categorySingular.length() - 1);
					}					
					
					categoryAction = base.addClass(category + "ActionMention", true, categorySingular.toLowerCase(), true, aspectsWithCategory, actionMention);
					aspectParentClasses.add(categoryAction);
					categoryEntity = base.addClass(category + "EntityMention", true, categorySingular.toLowerCase(), true, aspectsWithCategory, entityMention);
					aspectParentClasses.add(categoryEntity);
					categoryProperty = base.addClass(category + "PropertyMention", true, categorySingular.toLowerCase(), true, aspectsWithCategory, propertyMention);
					aspectParentClasses.add(categoryProperty);
					
					for (String key : embeddings.keySet())
					{						
						if (key.equals(categorySingular.toLowerCase() + "-noun"))
						{
							lexicalizationsVectors.println(categorySingular.toLowerCase() + "-noun " + printVectorArray(embeddings.get(key)));
						}
					}
				}
				
				System.out.println("Creating sentiment classes for: " + category);
				
				String categoryPositiveAction = base.addClass(category + "PositiveAction", false, category.toLowerCase(), false, aspectsWithCategory, categoryAction, positiveSentiment);
				sentimentParentClasses.add(categoryPositiveAction);
				String categoryNegativeAction = base.addClass(category + "NegativeAction", false, category.toLowerCase(), false, aspectsWithCategory, categoryAction, negativeSentiment);
				sentimentParentClasses.add(categoryNegativeAction);
				
				String categoryPositiveEntity = base.addClass(category + "PositiveEntity", false, category.toLowerCase(), false, aspectsWithCategory, categoryEntity, positiveSentiment);
				sentimentParentClasses.add(categoryPositiveEntity);
				String categoryNegativeEntity = base.addClass(category + "NegativeEntity", false, category.toLowerCase(), false, aspectsWithCategory, categoryEntity, negativeSentiment);
				sentimentParentClasses.add(categoryNegativeEntity);
				
				String categoryPositiveProperty = base.addClass(category + "PositiveProperty", false, category.toLowerCase(), false, aspectsWithCategory, categoryProperty, positiveSentiment);
				sentimentParentClasses.add(categoryPositiveProperty);
				String categoryNegativeProperty = base.addClass(category + "NegativeProperty", false, category.toLowerCase(), false, aspectsWithCategory, categoryProperty, negativeSentiment);		
				sentimentParentClasses.add(categoryNegativeProperty);
			}
		}
		
		
		for (String attribute : attributes)
		{
			//Hash set with all of the aspects with the given attribute
			HashSet<String> aspectsWithAttribute = new HashSet<>();
			
			for (int k = 1; k <= columns - 1; k++)
			{
				if (aspects[0][k].equals(attribute))
				{
					for (int l = 1; l <= rows - 1; l++)
					{
						if (aspects[l][k].equals("1"))
						{
							String aspectName = aspects[l][0] + "#" + attribute;
							aspectsWithAttribute.add(aspectName);
						}
					}
				}
			}
			
			if (omissions.contains(attribute))
			{
//				System.out.println("Omitted: " + attribute);
			}
			else
			{
				
				String attributeAction = "";
				String attributeEntity = "";
				String attributeProperty = "";
				
				System.out.println("Creating aspect classes for: " + attribute);
				
				//If attribute consists of multiple seeds/lexicalizations
				if (attribute.contains("&"))
				{
					String[] multipleLex =attribute.split("&");
					
//					System.out.println("Attribute contains & sign");
	
					//For each seed/lexicalization
					for (int m = 0; m <= multipleLex.length - 1; m++)
					{
						if (m == 0)
						{
							String lex = multipleLex[m];
							
							if (lex.substring(lex.length() - 1, lex.length()).equals("s"))
							{
								lex = lex.substring(0, lex.length() - 1);
							}
							
//							System.out.println("Adding first lexicalisation: " + lex);
							
							attributeAction = base.addClass(attribute + "ActionMention", true, lex.toLowerCase(), true, aspectsWithAttribute, actionMention);
							aspectParentClasses.add(attributeAction);
							attributeEntity = base.addClass(attribute + "EntityMention", true, lex.toLowerCase(), true, aspectsWithAttribute, entityMention);
							aspectParentClasses.add(attributeEntity);
							attributeProperty = base.addClass(attribute + "PropertyMention", true, lex.toLowerCase(), true, aspectsWithAttribute, propertyMention);
							aspectParentClasses.add(attributeProperty);
							
							for (String key : embeddings.keySet())
							{								
								if (key.equals(lex.toLowerCase() + "-noun"))
								{
									lexicalizationsVectors.println(lex.toLowerCase() + "-noun " + printVectorArray(embeddings.get(key)));
								}
							}
						}
						else
						{
							if (multipleLex[m].substring(multipleLex[m].length() - 1, multipleLex[m].length()).equals("s"))
							{
								multipleLex[m] = multipleLex[m].substring(0, multipleLex[m].length() - 1);
							}
							
//							System.out.println("Adding another lexicalisation: " + multipleLex[m]);
							
							base.addLexicalization(attributeAction, multipleLex[m].toLowerCase());
							base.addLexicalization(attributeEntity, multipleLex[m].toLowerCase());
							base.addLexicalization(attributeProperty, multipleLex[m].toLowerCase());
							
							for (String key : embeddings.keySet())
							{								
								if (key.equals(multipleLex[m].toLowerCase() + "-noun"))
								{
									lexicalizationsVectors.println(multipleLex[m].toLowerCase() + "-noun " + printVectorArray(embeddings.get(key)));
								}
							}
						}	
					}
				}
				//If attribute contains only one seed/lexicalization
				else
				{					
					String attributeSingular = attribute;
					
					if (attributeSingular.substring(attributeSingular.length() - 1, attributeSingular.length()).equals("s"))
					{
						attributeSingular = attributeSingular.substring(0, attributeSingular.length() - 1);
					}
					
					attributeAction = base.addClass(attribute + "ActionMention", true, attributeSingular.toLowerCase(), true, aspectsWithAttribute, actionMention);
					aspectParentClasses.add(attributeAction);
					attributeEntity = base.addClass(attribute + "EntityMention", true, attributeSingular.toLowerCase(), true, aspectsWithAttribute, entityMention);
					aspectParentClasses.add(attributeEntity);
					attributeProperty = base.addClass(attribute + "PropertyMention", true, attributeSingular.toLowerCase(), true, aspectsWithAttribute, propertyMention);
					aspectParentClasses.add(attributeProperty);
					
					for (String key : embeddings.keySet())
					{						
						if (key.equals(attributeSingular.toLowerCase() + "-noun"))
						{
							lexicalizationsVectors.println(attributeSingular.toLowerCase() + "-noun " + printVectorArray(embeddings.get(key)));
						}
					}
				}
				
				System.out.println("Creating sentiment classes for: " + attribute);
				
				String attributePositiveAction = base.addClass(attribute + "PositiveAction", false, attribute.toLowerCase(), false, aspectsWithAttribute, attributeAction, positiveSentiment);
				sentimentParentClasses.add(attributePositiveAction);
				String attributeNegativeAction = base.addClass(attribute + "NegativeAction", false, attribute.toLowerCase(), false, aspectsWithAttribute, attributeAction, negativeSentiment);
				sentimentParentClasses.add(attributeNegativeAction);
				
				String attributePositiveEntity = base.addClass(attribute + "PositiveEntity", false, attribute.toLowerCase(), false, aspectsWithAttribute, attributeEntity, positiveSentiment);
				sentimentParentClasses.add(attributePositiveEntity);
				String attributeNegativeEntity = base.addClass(attribute + "NegativeEntity", false, attribute.toLowerCase(), false, aspectsWithAttribute, attributeEntity, negativeSentiment);
				sentimentParentClasses.add(attributeNegativeEntity);
				
				String attributePositiveProperty = base.addClass(attribute + "PositiveProperty", false, attribute.toLowerCase(), false, aspectsWithAttribute, attributeProperty, positiveSentiment);
				sentimentParentClasses.add(attributePositiveProperty);
				String attributeNegativeProperty = base.addClass(attribute + "NegativeProperty", false, attribute.toLowerCase(), false, aspectsWithAttribute, attributeProperty, negativeSentiment);		
				sentimentParentClasses.add(attributeNegativeProperty);
			}
		}
		
		//Adding vectors 'positive' and 'negative' to the txt file with the seed vectors
		for (String key : embeddings.keySet())
		{
			if (key.equals("positive-adj"))
			{
				lexicalizationsVectors.println("positive-adj " + printVectorArray(embeddings.get(key)));
			}
			else if (key.equals("negative-adj"))
			{
				lexicalizationsVectors.println("negative-adj " + printVectorArray(embeddings.get(key)));
			}
		}
		
		lexicalizationsVectors.close();	
	}
	
	/***
	 * A method to read a CSV file with possible aspects into a 2D array
	 * 
	 * @param path to the CSV file with the possible aspects
	 * @param rows number of rows in the CSV file
	 * @param columns number of columns in the CSV file
	 * @return 2D array with the possible aspects
	 * @throws IOException
	 */
	public String[][] readCSV(String path, int rows, int columns) throws IOException
	{
		String[][] aspects = new String[rows][columns];
		
		BufferedReader br = new BufferedReader(new FileReader(path)); 
		String line;
	    int rowCounter = -1;
	    
	    //Read each line/row
	    while ((line = br.readLine()) != null) 		    
	    {
		    rowCounter++;
		    
		    //Split line based on ;
		    String[] lineItems = line.split(";");
	        int columnCounter = -1;
		      
	        //For each element/column in the given row
	        for (String item : lineItems)
	        {
		        columnCounter++;
		       	aspects[rowCounter][columnCounter] = item;
		    }
		 }
	    
		 br.close();
		 
		 return aspects;
	}
	
	/***
	 * A method to extract the relevant terms
	 * 
	 * @param path to the file with the word embeddings
	 * @param thresholds list of thresholds above which terms are suggested
	 * @param WordNet boolean for synset extraction
	 * @return array with the number of accepted/rejected terms
	 * @throws IOException
	 */
	public int[] extractTerms(String path, ArrayList<Double> thresholds, boolean WordNet) throws IOException
	{	
		System.out.println("Extracting terms");
		
		ArrayList<HashMap<String, double[]>> vectorsPerPOS = readEmbeddingsFromFile(path);
		
		verbs = vectorsPerPOS.get(0);
		nouns = vectorsPerPOS.get(1);
		adjectives = vectorsPerPOS.get(2);
		
		//For each possible parent class (for each possible category/attribute)
		for (String parent : aspectParentClasses)
		{			
			String fileName = getFileNameFromClassName(parent);
			
			String fileNameAspects = fileName + "-AspectConcepts.txt";
			String fileNameSentiments = fileName + "-SentimentConcepts.txt";
			String fileNameRejected = fileName + "-Rejected.txt";
			
			HashSet<String> lexicalizations = base.getLexicalizations(parent);
			
			String[] temp = fileName.split("(?=\\p{Upper})");	//Split on upper case letters			
			String type = temp[temp.length - 1];
			
			double threshold = 1.0;
			
			for (String lex : lexicalizations)
			{			
				String pos = "";
				
				if (type.equals("Action"))
				{
					pos = "verb";
					threshold = thresholds.get(0);
					suggestTerms(lex, verbs, pos, threshold, fileNameAspects, fileNameSentiments, fileNameRejected, WordNet);
				}
				else if (type.equals("Entity"))
				{
					pos = "noun";
					threshold = thresholds.get(1);
					suggestTerms(lex, nouns, pos, threshold, fileNameAspects, fileNameSentiments, fileNameRejected, WordNet);
				}
				if (type.equals("Property"))
				{
					pos = "adj";
					threshold = thresholds.get(2);
					suggestTerms(lex, adjectives, pos, threshold, fileNameAspects, fileNameSentiments, fileNameRejected, WordNet);
				}
			}
		}
		
		System.out.println("Looking for Type-1 sentiment concepts");
		
		String pathToLexVectors = Framework.DATA_PATH + "VectorsForClassNames.txt";
		ArrayList<HashMap<String, double[]>> vectorsAll = readEmbeddingsFromFile(pathToLexVectors);
		
		HashMap<String, double[]> vectorsLex = mergeHashMaps(vectorsAll);
		
		double[] vectorPos = vectorsLex.remove("positive-adj");
		double[] vectorNeg = vectorsLex.remove("negative-adj");
		
		//For each Type-1 sentiment parent
		for (String genericParent : genericParentClasses)
		{
			String fileName  = genericParent.replace(Ontology.namespace + "#", "");
			
			String fileNameAccepted = Framework.ACCEPTEDTERMS_PATH + fileName + "-AcceptedConcepts.txt";
			String fileNameRejected = Framework.ACCEPTEDTERMS_PATH + fileName + "-RejectedConcepts.txt";
						
			String[] temp = fileName.split("(?=\\p{Upper})");			
			String type = temp[temp.length - 1];
			String pos = "";
			String sentiment = temp[temp.length - 2].toLowerCase();
			
			double threshold = 1.0;
			
			if (type.equals("Action"))
			{
				pos = "verb";
				threshold = thresholds.get(3);
				
				if (sentiment.equals("positive") && vectorPos != null)
				{
					suggestGenericTerms(WordNet, sentiment, vectorPos, verbs, vectorsLex, pos, threshold, fileNameAccepted, fileNameRejected);
				}
				else if (sentiment.equals("negative") && vectorPos != null)
				{
					suggestGenericTerms(WordNet, sentiment, vectorNeg, verbs, vectorsLex, pos, threshold, fileNameAccepted, fileNameRejected);
				}
			}
			else if (type.equals("Entity"))
			{
				pos = "noun";
				threshold = thresholds.get(4);
				
				if (sentiment.equals("positive") && vectorPos != null)
				{
					suggestGenericTerms(WordNet, sentiment, vectorPos, nouns, vectorsLex, pos, threshold, fileNameAccepted, fileNameRejected);
				}
				else if (sentiment.equals("negative") && vectorPos != null)
				{
					suggestGenericTerms(WordNet, sentiment, vectorNeg, nouns, vectorsLex, pos, threshold, fileNameAccepted, fileNameRejected);
				}
			}
			if (type.equals("Property"))
			{
				pos = "adj";
				threshold = thresholds.get(5);
				
				if (sentiment.equals("positive") && vectorPos != null)
				{
					suggestGenericTerms(WordNet, sentiment, vectorPos, adjectives, vectorsLex, pos, threshold, fileNameAccepted, fileNameRejected);
				}
				else if (sentiment.equals("negative") && vectorPos != null)
				{
					suggestGenericTerms(WordNet, sentiment, vectorNeg, adjectives, vectorsLex, pos, threshold, fileNameAccepted, fileNameRejected);
				}
			}			
		}
		
		int[] result = new int[12];
		
		result[0] = acceptedVerbs;
		result[1] = acceptedNouns;
		result[2] = acceptedAdjectives;
		result[3] = rejectedVerbs;
		result[4] = rejectedNouns;
		result[5] = rejectedAdjectives;
		result[6] = acceptedGenericVerbs;
		result[7] = acceptedGenericNouns;
		result[8] = acceptedGenericAdjectives;
		result[9] = rejectedGenericVerbs;
		result[10] = rejectedGenericNouns;
		result[11] = rejectedGenericAdjectives;
		
		System.out.println("Accepted verbs: " + acceptedVerbs);
		System.out.println("Accepted nouns: " + acceptedNouns);
		System.out.println("Accepted adjectives: " + acceptedAdjectives);
		
		double ratioVerbs = ((double) acceptedVerbs)/(acceptedVerbs + rejectedVerbs);
		double ratioNouns = ((double) acceptedNouns)/(acceptedNouns + rejectedNouns);
		double ratioAdjectives = ((double) acceptedAdjectives)/(acceptedAdjectives + rejectedAdjectives);
		
		System.out.println("Acceptance ratio of verbs: " + ratioVerbs);
		System.out.println("Acceptance ratio of nouns: " + ratioNouns);
		System.out.println("Acceptance ratio of adjectives: " + ratioAdjectives);
	
		System.out.println("Accepted generic verbs: " + acceptedGenericVerbs);
		System.out.println("Accepted generic nouns: " + acceptedGenericNouns);
		System.out.println("Accepted generic adjectives: " + acceptedGenericAdjectives);
		
		double ratioGenericVerbs = ((double) acceptedGenericVerbs)/(acceptedGenericVerbs + rejectedGenericVerbs);
		double ratioGenericNouns = ((double) acceptedGenericNouns)/(acceptedGenericNouns + rejectedGenericNouns);
		double ratioGenericAdjectives = ((double) acceptedGenericAdjectives)/(acceptedGenericAdjectives + rejectedGenericAdjectives);
		
		System.out.println("Acceptance ratio of generic verbs: " + ratioGenericVerbs);
		System.out.println("Acceptance ratio of generic nouns: " + ratioGenericNouns);
		System.out.println("Acceptance ratio of generic adjectives: " + ratioGenericAdjectives);
		
		return result;
	}
	
	/***
	 * A method to suggest the Type-1 sentiment concepts
	 * 
	 * @param WordNet boolean for synset extraction
	 * @param parentSent the sentiment for which children are being searched for
	 * @param vectorSent vector for parentSent
	 * @param vectors vectors for all the possible terms that will be investigated
	 * @param vectorsLex vectors for the seeds/lexicalizations of all the parent classes
	 * @param pos part-of-speech tag
	 * @param threshold the threshold above which terms are suggested
	 * @param fileNameAccepted path to the file with the accepted terms
	 * @param fileNameRejected path to the file with the rejected terms
	 * @throws IOException
	 */
	public void suggestGenericTerms(boolean WordNet, String parentSent, double[] vectorSent, HashMap<String, double[]> vectors, HashMap<String, double[]> vectorsLex, String pos, double threshold, String fileNameAccepted, String fileNameRejected) throws IOException
	{				
//		if (parentVector != null)
//		{	
			System.out.println("Looking for children of generic " + parentSent + " class with POS: " + pos);
			System.out.println("--> To accept a term as Type-1 sentiment type 'a'");
			System.out.println("--> To reject a term type 'r'");
			
			Scanner in = new Scanner(System.in);
			
			ArrayList<String> synsetsRemoved = new ArrayList<>();
			
			for (String key : vectors.keySet())
			{
				if (key.contains("."))
				{
					continue;
				}
				
				//Skip terms that were already accepted/rejected with synset extraction
				if (WordNet && synsetsRemoved.contains(key))
				{
					continue;
				}
				
				//Similarity between the term and the given sentiment
				double scoreSent = cosineSimilarity(vectorSent, vectors.get(key));				
				double scoreLex = 0.0;
				
				int counter = 0;
				for (String keyLex : vectorsLex.keySet())
				{
					counter++;
					scoreLex = scoreLex + cosineSimilarity(vectorsLex.get(keyLex), vectors.get(key));
				}
				
				//Harmonic mean of cosine similarities between the term and the aspect parent classes
				double scoreHarmonic = (counter + 1) / ((1 / scoreSent) + scoreLex);

				//Changing the range of the threshold from 0-1 to 3.5-... or 2.0-...
				double whichIter = 0.6 - threshold;
				double thresholdGeneric = 1.0;
				
//				thresholdGeneric = 3.5 - (whichIter * 5);		//fastText
				thresholdGeneric = 2.0 - (whichIter * 5);		//GloVe
								
				if (scoreHarmonic >= thresholdGeneric)
				{	
					System.out.println("Suggested term: " + key + " with a score of " + scoreHarmonic);

					//Lists with the previously accepted/rejected terms
					List<String> acceptedVectors = loadVectorsFile(fileNameAccepted);
					List<String> rejectedVectors = loadVectorsFile(fileNameRejected);
					
					String input = "";
					
					boolean conditionToPrint = true;
					
					if (acceptedVectors.contains(key))
					{
						input = "a";
						System.out.println("Term was already accepted as a sentiment concept");
						conditionToPrint = false;
					}
					else if (rejectedVectors.contains(key))
					{
						input = "r";
						System.out.println("Term was already rejected");
						conditionToPrint = false;
					}
					else
					{
						input = in.next();
					}
					
					FileWriter fw_aspects = new FileWriter(fileNameAccepted, true);
				    BufferedWriter bw_aspects = new BufferedWriter(fw_aspects);
				    PrintWriter accepted = new PrintWriter(bw_aspects);
				    
					FileWriter fw_rejected = new FileWriter(fileNameRejected, true);
				    BufferedWriter bw_rejected = new BufferedWriter(fw_rejected);
				    PrintWriter rejected = new PrintWriter(bw_rejected);
					
					if (input.equals("a"))
					{
						if (pos.equals("verb"))
						{
							acceptedGenericVerbs++;
						}
						else if (pos.equals("noun"))
						{
							acceptedGenericNouns++;
						}
						else if (pos.equals("adj"))
						{
							acceptedGenericAdjectives++;
						}
						
						if (conditionToPrint)
						{
							accepted.println(key + " " + printVectorArray(vectors.get(key)));
						}
						
						//If synset extraction is to be used
						if (WordNet)
						{						
							System.out.println("Looking for synsets with " + key);
						
							Pair<Integer, ArrayList<String>> pairResult = getSynsetTerms(key, in, pos, fileNameAccepted, fileNameRejected);
							ArrayList<String> synsetWords = pairResult.getValue();
							int numRejected = pairResult.getKey();
							
							for (String synsetWord : synsetWords)
							{
								if (vectors.keySet().contains(synsetWord))
								{
									accepted.println(key + " " + printVectorArray(vectors.get(synsetWord)));
									synsetsRemoved.add(synsetWord);
								}
								
								if (pos.equals("verb"))
								{
									acceptedGenericVerbs++;
								}
								else if (pos.equals("noun"))
								{
									acceptedGenericNouns++;
								}
								else if (pos.equals("adj"))
								{
									acceptedGenericAdjectives++;
								}
							}
							
							if (pos.equals("verb"))
							{
								rejectedGenericVerbs = rejectedGenericVerbs + numRejected;
							}
							else if (pos.equals("noun"))
							{
								rejectedGenericNouns = rejectedGenericNouns + numRejected;
							}
							else if (pos.equals("adj"))
							{
								rejectedGenericAdjectives = rejectedGenericAdjectives + numRejected;
							}
						}
					}
					else if (input.equals("r"))
					{
						if (pos.equals("verb"))
						{
							rejectedGenericVerbs++;
						}
						else if (pos.equals("noun"))
						{
							rejectedGenericNouns++;
						}
						else if (pos.equals("adj"))
						{
							rejectedGenericAdjectives++;
						}
						
						if (conditionToPrint)
						{
							rejected.println(key + " " + printVectorArray(vectors.get(key)));
						}
					}
					
					accepted.close();
					rejected.close();
				}
			}
	}
	
	/***
	 * A method to suggest (aspect, Type-2 and Type-3 sentiment) terms
	 * 
	 * @param parent for which children are being currently searched for
	 * @param vectorsAll vectors (with the relevant pos) with all the possible terms
	 * @param pos part-of-speech tag
	 * @param threshold threshold above which terms are suggested
	 * @param fileNameAspects path to a file with the already accepted aspect concepts
	 * @param fileNameSentiments path to a file with the already accepted sentiment concepts
	 * @param fileNameRejected path to a file with the already rejected terms
	 * @param WordNet boolean for synset extraction
	 * @throws IOException
	 */
	public void suggestTerms(String parent, HashMap<String, double[]> vectorsAll, String pos, double threshold, String fileNameAspects, String fileNameSentiments, String fileNameRejected, boolean WordNet) throws IOException
	{		
		HashMap<String, double[]> vectors = vectorsAll;
		
		String parentLex = parent.toLowerCase() + "-noun";
		double[] parentVector = getVector(parentLex);
		
//		System.out.println(parentLex);
		
		if (parentVector != null)
		{	
			System.out.println("Looking for children of " + parent + " with POS: " + pos);
			System.out.println("--> To accept a term as an aspect concept type 'a'");
			System.out.println("--> To accept a term as a sentiment concept type 's'");
			System.out.println("--> To reject a term type 'r'");
			
			Scanner in = new Scanner(System.in);
			
			ArrayList<String> synsetsRemoved = new ArrayList<>();
			
			for (String key : vectors.keySet())
			{				
				if (key.equals(parentLex))
				{
					continue;
				}
				
				//Skip terms that were already accepted/rejected with synset extraction
				if (WordNet && synsetsRemoved.contains(key))
				{
					continue;
				}
				
				if (key.contains("."))
				{
					continue;
				}
				
				//Cosine similarity between the term and the seed
				double score = cosineSimilarity(parentVector, vectors.get(key));
				
				if (score >= threshold)
				{					
					System.out.println("Suggested term: " + key + " with a score of " + score);
							
					//Lists with the previously accepted/rejected terms
					List<String> acceptedAspects = loadVectorsFile(fileNameAspects);
					List<String> acceptedSentiments = loadVectorsFile(fileNameSentiments);
					List<String> rejectedVectors = loadVectorsFile(fileNameRejected);
					
					String input = "";
					
					boolean conditionToPrint = true;
					
					if (acceptedAspects.contains(key))
					{
						input = "a";
						System.out.println("Term was already accepted as an aspect concept");
						conditionToPrint = false;
					}
					else if (acceptedSentiments.contains(key))
					{
						input = "s";
						System.out.println("Term was already accepted as a sentiment concept");
						conditionToPrint = false;
					}
					else if (rejectedVectors.contains(key))
					{
						input = "r";
						System.out.println("Term was already rejected");
					}
					else
					{
						input = in.next();
					}
					
					FileWriter fw_aspects = new FileWriter(fileNameAspects, true);
				    BufferedWriter bw_aspects = new BufferedWriter(fw_aspects);
				    PrintWriter aspects = new PrintWriter(bw_aspects);
				    
					FileWriter fw_sentiments = new FileWriter(fileNameSentiments, true);
				    BufferedWriter bw_sentiments = new BufferedWriter(fw_sentiments);
				    PrintWriter sentiments = new PrintWriter(bw_sentiments);
				    
					FileWriter fw_rejected = new FileWriter(fileNameRejected, true);
				    BufferedWriter bw_rejected = new BufferedWriter(fw_rejected);
				    PrintWriter rejected = new PrintWriter(bw_rejected);
							
				    //Aspect concept
					if (input.equals("a"))
					{
						if (pos.equals("verb"))
						{
							acceptedVerbs++;
						}
						else if (pos.equals("noun"))
						{
							acceptedNouns++;
						}
						else if (pos.equals("adj"))
						{
							acceptedAdjectives++;
						}
						
						
						if (conditionToPrint)
						{
							aspects.println(key + " " + printVectorArray(vectors.get(key)));
						}
						
						//If synset extraction is to be used
						if (WordNet)
						{						
							System.out.println("Looking for synsets with " + key);
						
							Pair<Integer, ArrayList<String>> pairResult = getSynsetTerms(key, in, pos, fileNameAspects, fileNameRejected);
							ArrayList<String> synsetWords = pairResult.getValue();
							int numRejected = pairResult.getKey();
							
							for (String synsetWord : synsetWords)
							{
								if (vectors.keySet().contains(synsetWord))
								{
									aspects.println(key + " " + printVectorArray(vectors.get(synsetWord)));
									synsetsRemoved.add(synsetWord);
								}
								
								if (pos.equals("verb"))
								{
									acceptedVerbs++;
								}
								else if (pos.equals("noun"))
								{
									acceptedNouns++;
								}
								else if (pos.equals("adj"))
								{
									acceptedAdjectives++;
								}
							}
							
							if (pos.equals("verb"))
							{
								rejectedVerbs = rejectedVerbs + numRejected;
							}
							else if (pos.equals("noun"))
							{
								rejectedNouns = rejectedNouns + numRejected;
							}
							else if (pos.equals("adj"))
							{
								rejectedAdjectives = rejectedAdjectives + numRejected;
							}
						}
					}
					//Sentiment concept
					else if (input.equals("s"))
					{
						if (pos.equals("verb"))
						{
							acceptedVerbs++;
						}
						else if (pos.equals("noun"))
						{
							acceptedNouns++;
						}
						else if (pos.equals("adj"))
						{
							acceptedAdjectives++;
						}
						
						
						if (conditionToPrint)
						{
							sentiments.println(key + " " + printVectorArray(vectors.get(key)));
						}
						
						//If synset extraction is to be used
						if (WordNet)
						{						
							System.out.println("Looking for synsets with " + key);
						
							Pair<Integer, ArrayList<String>> pairResult = getSynsetTerms(key, in, pos, fileNameSentiments, fileNameRejected);
							ArrayList<String> synsetWords = pairResult.getValue();
							int numRejected = pairResult.getKey();							
							
							for (String synsetWord : synsetWords)
							{
								if (vectors.keySet().contains(synsetWord))
								{
									sentiments.println(key + " " + printVectorArray(vectors.get(synsetWord)));
									synsetsRemoved.add(synsetWord);
								}
								
								if (pos.equals("verb"))
								{
									acceptedVerbs++;
								}
								else if (pos.equals("noun"))
								{
									acceptedNouns++;
								}
								else if (pos.equals("adj"))
								{
									acceptedAdjectives++;
								}
							}
							
							if (pos.equals("verb"))
							{
								rejectedVerbs = rejectedVerbs + numRejected;
							}
							else if (pos.equals("noun"))
							{
								rejectedNouns = rejectedNouns + numRejected;
							}
							else if (pos.equals("adj"))
							{
								rejectedAdjectives = rejectedAdjectives + numRejected;
							}
						}
					}
					else if (input.equals("r"))
					{
						if (pos.equals("verb"))
						{
							rejectedVerbs++;
						}
						else if (pos.equals("noun"))
						{
							rejectedNouns++;
						}
						else if (pos.equals("adj"))
						{
							rejectedAdjectives++;
						}
						
						
						if (conditionToPrint)
						{
							rejected.println(key + " " + printVectorArray(vectors.get(key)));
						}
					}
					
					aspects.close();
					sentiments.close();
					rejected.close();
				}
			}
		}
		else
		{
			System.out.println("Parent does not have a vector representation --> " + parentLex);
		}
	}
	
	/***
	 * A method to load a list of vector names from a file
	 * 
	 * @param filePath path to the file with vectors
	 * @return list of vector names
	 * @throws FileNotFoundException
	 */
	public List<String> loadVectorsFile(String filePath) throws FileNotFoundException
	{
	    Scanner scanner = new Scanner(new File(filePath));
	    List<String> vectors = new ArrayList<>();
	    
	    while(scanner.hasNextLine())
	    {
	    	String temp = scanner.nextLine();
	    	String[] vec = temp.split(" ");
	    	
	        vectors.add(vec[0]);
	    }
	    
	    scanner.close();
	    return vectors;
	}
	
	/***
	 * A method suggesting synsets for a given term
	 * 
	 * @param term for which synsets are to be found and suggested
	 * @param in scanner
	 * @param pos part-of-speech tag
	 * @param fileAccepted path to a file with the accepted terms
	 * @param fileRejected path to a file with the rejected terms
	 * @return a pair with the number of rejected terms and a list of the accepted terms
	 * @throws IOException
	 */
	public Pair<Integer, ArrayList<String>> getSynsetTerms(String term, Scanner in, String pos, String fileAccepted, String fileRejected) throws IOException
	{
		String tempAccepted = fileAccepted.replace(".txt", "-Synsets.txt");
		String tempRejected = fileRejected.replace(".txt", "-Synsets.txt");
		
		String[] temp = term.split("-");
		String word = "";
		
		for (int l = 0; l <= temp.length - 2; l++)
		{
			word = word + temp[l];
		}
		
		ArrayList<String> acceptedTerms = new ArrayList<>();
		
		File WordNet = new File(Framework.EXTERNALDATA_PATH + "/WordNet-3.0/dict");
		System.setProperty("wordnet.database.dir", WordNet.toString());

		WordNetDatabase database = WordNetDatabase.getFileInstance(); 
		
		Synset[] synsets = null;
		
		//Get synsets with the given POS
		if (pos.equals("verb"))
		{
			synsets = database.getSynsets(word, SynsetType.VERB); 		
		}
		else if (pos.equals("noun"))
		{
			synsets = database.getSynsets(word, SynsetType.NOUN); 
		}
		else if (pos.equals("adj"))
		{
			Synset[] synsets1 = database.getSynsets(word, SynsetType.ADJECTIVE); 
			Synset[] synsets2 = database.getSynsets(word, SynsetType.ADJECTIVE_SATELLITE); 
		
			synsets = (Synset[]) ArrayUtils.addAll(synsets1, synsets2);
		}
		
		if (synsets.length > 0)
		{
			System.out.println("--> To accept a synset type 'a'");
			System.out.println("--> To reject a synset type 'r'");
			System.out.println("Synsets of " + term + " are:" );
		}
		else
		{
			System.out.println("No synsets found");
		}
		
		//Lists with the already accepted/rejected synsets
		List<String> acceptedSynsets = loadSynsetsFile(tempAccepted);
		List<String> rejectedSynsets = loadSynsetsFile(tempRejected);
		
		int counter = 0;
				
		for (int i = 0; i <= synsets.length - 1; i++)
		{
			//Print the definition of the given synset
			System.out.println(synsets[i].getDefinition());
			
			String input = "";
			
			boolean conditionToPrint = true;
			
			if (acceptedSynsets.contains(synsets[i].getDefinition()))
			{
				input = "a";
				System.out.println("Synset was already accepted");
				conditionToPrint = false;
			}
			else if (rejectedSynsets.contains(synsets[i].getDefinition()))
			{
				input = "r";
				System.out.println("Synset was already rejected");
				conditionToPrint = false;
			}
			else
			{
				input = in.next();
			}
			
//			String input = in.next();
			
			FileWriter fw_accepted = new FileWriter(tempAccepted, true);
		    BufferedWriter bw_accepted = new BufferedWriter(fw_accepted);
		    PrintWriter accepted = new PrintWriter(bw_accepted);
		    
			FileWriter fw_rejected = new FileWriter(tempRejected, true);
		    BufferedWriter bw_rejected = new BufferedWriter(fw_rejected);
		    PrintWriter rejected = new PrintWriter(bw_rejected);
			
			if (input.equals("a"))
			{
				//Get the words/terms in the given synset
				String[] synsetWords = synsets[i].getWordForms();
				
				for (int j = 0; j <= synsetWords.length - 1; j++)
				{
					acceptedTerms.add(synsetWords[j].toLowerCase() + "-" + pos);
				}
				
				if (conditionToPrint)
				{
					accepted.println(synsets[i].getDefinition());
				}
			}
			else if (input.equals("r"))
			{
				String[] synsetWords = synsets[i].getWordForms();
				
				for (int j = 0; j <= synsetWords.length - 1; j++)
				{
					counter++;
				}
				
				if (conditionToPrint)
				{
					rejected.println(synsets[i].getDefinition());
				}
			}
			
			accepted.close();
			rejected.close();
		}
		
		return new Pair<Integer, ArrayList<String>>(counter, acceptedTerms);
	}
	
	/***
	 * A method to read a file with the list of already accepted/rejected synsets
	 * 
	 * @param filePath path to the file that is supposed to be read
	 * @return list of synset definitions read from the file
	 * @throws FileNotFoundException
	 */
	public List<String> loadSynsetsFile(String filePath) throws FileNotFoundException
	{
	    Scanner scanner = new Scanner(new File(filePath));
	    List<String> synsets = new ArrayList<>();
	    while(scanner.hasNextLine())
	    {
	    	String temp = scanner.nextLine();	    	
	        synsets.add(temp);
	    }    
	    scanner.close();
	    return synsets;
	}
	
	/***
	 * A method to print a vector in one line with different elements separated by space
	 * 
	 * @param vector to be printed
	 * @return string with the vector's elements separated by space
	 */
	public String printVectorArray(double[] vector)
	{
		String output = "";
		
		for (double vec : vector)
		{
			output += vec + " ";
		}
		
		return output.substring(0, output.length());
	}
	
	/***
	 * A method to calculate cosine similarity between two given vectors
	 * 
	 * @param vectorA
	 * @param vectorB
	 * @return cosine similarity value
	 */
	public static double cosineSimilarity(double[] vectorA, double[] vectorB) 
	{
	    double dotProduct = 0.0;
	    double normA = 0.0;
	    double normB = 0.0;
	    
	    for (int i = 0; i < vectorA.length; i++) 
	    {
	        dotProduct += vectorA[i] * vectorB[i];
	        normA += Math.pow(vectorA[i], 2);
	        normB += Math.pow(vectorB[i], 2);
	    } 
	    
	    return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
	}
	
	/***
	 * A method to read embeddings (vectors and their names) from a file
	 * 
	 * @param path to file with the embeddings
	 * @return a list with hash maps for embeddings per POS
	 * @throws IOException
	 */
	public ArrayList<HashMap<String, double[]>> readEmbeddingsFromFile(String path) throws IOException
	{
		ArrayList<HashMap<String, double[]>> vectorsPerPOS = new ArrayList<>();
		
		HashMap<String, double[]> verbs = new HashMap<>();
		HashMap<String, double[]> nouns = new HashMap<>();
		HashMap<String, double[]> adjectives = new HashMap<>();
		
		BufferedReader br = new BufferedReader(new FileReader(path)); 
		String line;
		
	    while ((line = br.readLine()) != null) 		    
	    {		    
		    String[] lineItems = line.split(" ");
		    
		    //If first line in the file contains the dimensions and not a vector
		    if (lineItems.length != embeddingsDimensions + 1)
		    {
		    	continue;
		    }
		    
		    String word = lineItems[0];
		    double[] vector = new double[embeddingsDimensions];

		    for (int i = 1; i <= embeddingsDimensions; i++)
		    {
		    	vector[i - 1] = Double.parseDouble(lineItems[i]);
		    }
		    
		    if (word.contains("-verb"))
		    {
		    	verbs.put(word, vector);
		    }
		    else if (word.contains("-noun"))
		    {
		    	nouns.put(word, vector);
		    }
		    if (word.contains("-adj"))
		    {
		    	adjectives.put(word, vector);
		    }		    
	    }
		vectorsPerPOS.add(verbs);
		vectorsPerPOS.add(nouns);
		vectorsPerPOS.add(adjectives);
		
		br.close();
		
		return vectorsPerPOS;
	}
	
	/***
	 * A method to build a hierarchy between concepts
	 * 
	 * @param thresholds list with thresholds for different POS
	 * @param triples boolean if the triples-based approach is to be used
	 * @param basicApproach boolean if hierarchy between aspect concepts is to be made
	 * @return a list with arrays with the number of accepted/rejected hierarchies
	 * @throws IOException
	 */
	public ArrayList<int[]> buildHierarchy(ArrayList<Double> thresholds, boolean triples, boolean basicApproach) throws IOException
	{	
		//Hierarchy between aspect concepts is supposed to be built
		if (!basicApproach)
		{	
			System.out.println("Building hierarchy for aspect concepts");
			
			for (String parent : aspectParentClasses)
			{									
				String pathToAcceptedTerms = getFileNameFromClassName(parent) + ("-AspectConcepts.txt");

				String pathToFileAccepted = pathToAcceptedTerms.replace("AcceptedTerms", "AcceptedHierarchies");
				pathToFileAccepted = pathToFileAccepted.substring(0, pathToFileAccepted.length() - 4) + "-AcceptedHierarchies.txt";
				
				String pathToFileRejected = pathToAcceptedTerms.replace("AcceptedTerms", "RejectedHierarchies");
				pathToFileRejected = pathToFileRejected.substring(0, pathToFileRejected.length() - 4) + "-RejectedHierarchies.txt";
				
				String pathToFile = pathToAcceptedTerms.replace("AcceptedTerms", "AcceptedHierarchies");
				pathToFile = pathToFile.substring(0, pathToFile.length() - 4) + "-AcceptedHierarchies.txt";
				
				if (!triples)
				{
					hierarchyAspectConcepts(pathToAcceptedTerms, parent, thresholds, pathToFileAccepted, pathToFileRejected);
				}
				else
				{
					String tempAccepted = pathToFileAccepted.replace(".txt", "-Triples.txt");
					String tempRejected = pathToFileRejected.replace(".txt", "-Triples.txt");
					
					hierarchyAspectConceptsTriples(pathToAcceptedTerms, parent, thresholds, tempAccepted, tempRejected);
				
					pathToFile = pathToFile.replace(".txt", "-Triples.txt");
				}
				
				System.out.println("Adding the accepted hierarchies to the ontology");
				
				HashSet<String> lexicalizations = base.getLexicalizations(parent);
				
				BufferedReader br = new BufferedReader(new FileReader(pathToFile));
				String line;
						
				//For each accepted hierarchy
				while ((line = br.readLine()) != null) 
				{					
					String[] concepts = line.split(" ");
					
					if (!triples)
					{						
						String parentConcept = concepts[0];
//						System.out.println("parentConcept: " + parentConcept);
						String childConcept = concepts[1];
//						System.out.println("childConcept: " + childConcept);
						String grandChildConcept = concepts[2];
//						System.out.println("grandChildConcept " + grandChildConcept);
						
						String childConceptTemp = childConcept;
						String parentConceptTemp = parentConcept;
						String grandChildConceptTemp = grandChildConcept;
						
						if (childConceptTemp.contains("-verb"))
						{
							childConceptTemp = childConceptTemp.replace("-verb", "");
						}
						else if (childConceptTemp.contains("-noun"))
						{
							childConceptTemp = childConceptTemp.replace("-noun", "");
						}
						else if (childConceptTemp.contains("-adj"))
						{
							childConceptTemp = childConceptTemp.replace("-adj", "");
						}
						
						if (grandChildConceptTemp.contains("-verb"))
						{
							grandChildConceptTemp = grandChildConceptTemp.replace("-verb", "");
						}
						else if (grandChildConceptTemp.contains("-noun"))
						{
							grandChildConceptTemp = grandChildConceptTemp.replace("-noun", "");
						}
						else if (grandChildConceptTemp.contains("-adj"))
						{
							grandChildConceptTemp = grandChildConceptTemp.replace("-adj", "");
						}
						
						if (parentConceptTemp.contains("-verb"))
						{
							parentConceptTemp = parentConceptTemp.replace("-verb", "");
						}
						else if (parentConceptTemp.contains("-noun"))
						{
							parentConceptTemp = parentConceptTemp.replace("-noun", "");
						}
						else if (parentConceptTemp.contains("-adj"))
						{
							parentConceptTemp = parentConceptTemp.replace("-adj", "");
						}
												
						ArrayList<HashMap<String, double[]>> embeddingsAll = readEmbeddingsFromFile(pathToAcceptedTerms);
						HashMap<String, double[]> embeddings = new HashMap<String, double[]>();
						
						if (pathToAcceptedTerms.contains("Action"))
						{
							embeddings = embeddingsAll.get(0);
						}
						if (pathToAcceptedTerms.contains("Entity"))
						{
							embeddings = embeddingsAll.get(1);
						}
						if (pathToAcceptedTerms.contains("Property"))
						{
							embeddings = embeddingsAll.get(2);
						}
						
						String pathToLexVectors = Framework.DATA_PATH + "VectorsForClassNames.txt";
						ArrayList<HashMap<String, double[]>> vectorsAll = readEmbeddingsFromFile(pathToLexVectors);
						HashMap<String, double[]> vectorsLex = mergeHashMaps(vectorsAll);
						
						if (embeddings.keySet().contains(parentConcept) || vectorsLex.keySet().contains(parentConcept + "-noun"))
						{
							if (embeddings.keySet().contains(childConcept) && embeddings.keySet().contains(grandChildConcept))
							{
								base.setSuperClassAspects(parentConceptTemp, childConceptTemp, parent);
								base.setSuperClassAspects(childConceptTemp, grandChildConceptTemp, parent);
							}
						}						
					}
					else					
					{						
						String parentConcept = concepts[0];
						String childConcept1 = concepts[1];
						String childConcept2 = concepts[2];
						
						String childConceptTemp1 = childConcept1;
						String parentConceptTemp = parentConcept;
						String childConceptTemp2 = childConcept2;
						
						if (childConceptTemp1.contains("-verb"))
						{
							childConceptTemp1 = childConceptTemp1.replace("-verb", "");
						}
						else if (childConceptTemp1.contains("-noun"))
						{
							childConceptTemp1 = childConceptTemp1.replace("-noun", "");
						}
						else if (childConceptTemp1.contains("-adj"))
						{
							childConceptTemp1 = childConceptTemp1.replace("-adj", "");
						}
						
						if (childConceptTemp2.contains("-verb"))
						{
							childConceptTemp2 = childConceptTemp2.replace("-verb", "");
						}
						else if (childConceptTemp2.contains("-noun"))
						{
							childConceptTemp2 = childConceptTemp2.replace("-noun", "");
						}
						else if (childConceptTemp2.contains("-adj"))
						{
							childConceptTemp2 = childConceptTemp2.replace("-adj", "");
						}
						
						if (parentConceptTemp.contains("-verb"))
						{
							parentConceptTemp = parentConceptTemp.replace("-verb", "");
						}
						else if (parentConceptTemp.contains("-noun"))
						{
							parentConceptTemp = parentConceptTemp.replace("-noun", "");
						}
						else if (parentConceptTemp.contains("-adj"))
						{
							parentConceptTemp = parentConceptTemp.replace("-adj", "");
						}
						
						ArrayList<HashMap<String, double[]>> embeddingsAll = readEmbeddingsFromFile(pathToAcceptedTerms);
						HashMap<String, double[]> embeddings = new HashMap<String, double[]>();
						
						if (pathToAcceptedTerms.contains("Action"))
						{
							embeddings = embeddingsAll.get(0);
						}
						if (pathToAcceptedTerms.contains("Entity"))
						{
							embeddings = embeddingsAll.get(1);
						}
						if (pathToAcceptedTerms.contains("Property"))
						{
							embeddings = embeddingsAll.get(2);
						}
						
						String pathToLexVectors = Framework.DATA_PATH + "VectorsForClassNames.txt";
						ArrayList<HashMap<String, double[]>> vectorsAll = readEmbeddingsFromFile(pathToLexVectors);
						HashMap<String, double[]> vectorsLex = mergeHashMaps(vectorsAll);
						
						if (embeddings.keySet().contains(parentConcept) || vectorsLex.keySet().contains(parentConcept + "-noun"))
						{
							if (embeddings.keySet().contains(childConcept1) && embeddings.keySet().contains(childConcept2))
							{
								base.setSuperClassAspectsTriples(parentConceptTemp, childConceptTemp1, parent);
								base.setSuperClassAspectsTriples(parentConceptTemp, childConceptTemp2, parent);
							}
						}						
					}
				}
				
				br.close();
			}
		}
		
		System.out.println("Building hierarchy for sentiment concepts");
		
		for (String parent : aspectParentClasses)
		{
			String pathToAcceptedTerms = getFileNameFromClassName(parent) + ("-SentimentConcepts.txt");
			
			String pathToAcceptedHierarchies = pathToAcceptedTerms.replace("AcceptedTerms", "AcceptedHierarchies");
			pathToAcceptedHierarchies = pathToAcceptedHierarchies.substring(0, pathToAcceptedHierarchies.length() - 4) + "-AcceptedHierarchies.txt";
			
			String pathToRejectedHierarchies = pathToAcceptedTerms.replace("AcceptedTerms", "RejectedHierarchies");
			pathToRejectedHierarchies = pathToRejectedHierarchies.substring(0, pathToRejectedHierarchies.length() - 4) + "-RejectedHierarchies.txt";
			
			hierarchySentimentConcepts(pathToAcceptedTerms, parent, thresholds, pathToAcceptedHierarchies, pathToRejectedHierarchies);					
		}
		
		int[] resultSent = new int[12];
		
		resultSent[0] = acceptedHierarchiesVerbsSentType2;
		resultSent[1] = acceptedHierarchiesNounsSentType2;
		resultSent[2] = acceptedHierarchiesAdjsSentType2;
		resultSent[3] = rejectedHierarchiesVerbsSentType2;
		resultSent[4] = rejectedHierarchiesNounsSentType2;
		resultSent[5] = rejectedHierarchiesAdjsSentType2;
		resultSent[6] = acceptedHierarchiesVerbsSentType2;
		resultSent[7] = acceptedHierarchiesNounsSentType3;
		resultSent[8] = acceptedHierarchiesAdjsSentType3;
		resultSent[9] = rejectedHierarchiesVerbsSentType3;
		resultSent[10] = rejectedHierarchiesNounsSentType3;
		resultSent[11] = rejectedHierarchiesAdjsSentType3;
		
		System.out.println("Accepted Type-2 sentiment hierarchies for verbs: " + acceptedHierarchiesVerbsSentType2);
		System.out.println("Accepted Type-2 sentiment hierarchies for nouns: " + acceptedHierarchiesNounsSentType2);
		System.out.println("Accepted Type-2 sentiment hierarchies for adjectives: " + acceptedHierarchiesAdjsSentType2);
				
		double ratioSentimentVerbsType2 = ((double) acceptedHierarchiesVerbsSentType2)/(acceptedHierarchiesVerbsSentType2 + rejectedHierarchiesVerbsSentType2);
		double ratioSentimentNounsType2 = ((double) acceptedHierarchiesNounsSentType2)/(acceptedHierarchiesNounsSentType2 + rejectedHierarchiesNounsSentType2);
		double ratioSentimentAdjectivesType2 = ((double) acceptedHierarchiesAdjsSentType2)/(acceptedHierarchiesAdjsSentType2 + rejectedHierarchiesAdjsSentType2);
		
		System.out.println("Acceptance ratio of Type-2 sentiment hierarchies for verbs: " + ratioSentimentVerbsType2);	
		System.out.println("Acceptance ratio of Type-2 sentiment hierarchies for nouns: " + ratioSentimentNounsType2);	
		System.out.println("Acceptance ratio of Type-2 sentiment hierarchies for adjectives: " + ratioSentimentAdjectivesType2);		
	
		System.out.println("Accepted Type-3 sentiment hierarchies for verbs: " + acceptedHierarchiesVerbsSentType3);
		System.out.println("Accepted Type-3 sentiment hierarchies for nouns: " + acceptedHierarchiesNounsSentType3);
		System.out.println("Accepted Type-3 sentiment hierarchies for adjectives: " + acceptedHierarchiesAdjsSentType3);
				
		double ratioSentimentVerbsType3 = ((double) acceptedHierarchiesVerbsSentType3)/(acceptedHierarchiesVerbsSentType3 + rejectedHierarchiesVerbsSentType3);
		double ratioSentimentNounsType3 = ((double) acceptedHierarchiesNounsSentType3)/(acceptedHierarchiesNounsSentType3 + rejectedHierarchiesNounsSentType3);
		double ratioSentimentAdjectivesType3 = ((double) acceptedHierarchiesAdjsSentType3)/(acceptedHierarchiesAdjsSentType3 + rejectedHierarchiesAdjsSentType3);
		
		System.out.println("Acceptance ratio of Type-3 sentiment hierarchies for verbs: " + ratioSentimentVerbsType3);	
		System.out.println("Acceptance ratio of Type-3 sentiment hierarchies for nouns: " + ratioSentimentNounsType3);	
		System.out.println("Acceptance ratio of Type-3 sentiment hierarchies for adjectives: " + ratioSentimentAdjectivesType3);		
		
		int[] resultAspects = new int[6];
		
		resultAspects[0] = acceptedHierarchiesVerbsAspects;
		resultAspects[1] = acceptedHierarchiesNounsAspects;
		resultAspects[2] = acceptedHierarchiesAdjsAspects;
		resultAspects[3] = rejectedHierarchiesVerbsAspects;
		resultAspects[4] = rejectedHierarchiesNounsAspects;
		resultAspects[5] = rejectedHierarchiesAdjsAspects;
		
		System.out.println("Accepted aspect hierarchies for verbs: " + acceptedHierarchiesVerbsAspects);
		System.out.println("Accepted aspect hierarchies for nouns: " + acceptedHierarchiesNounsAspects);
		System.out.println("Accepted aspect hierarchies for adjectives: " + acceptedHierarchiesAdjsAspects);
				
		double ratioAspectVerbs = ((double) acceptedHierarchiesVerbsAspects)/(acceptedHierarchiesVerbsAspects + rejectedHierarchiesVerbsAspects);
		double ratioAspectNouns = ((double) acceptedHierarchiesNounsAspects)/(acceptedHierarchiesNounsAspects + rejectedHierarchiesNounsAspects);
		double ratioAspectAdjectives = ((double) acceptedHierarchiesAdjsAspects)/(acceptedHierarchiesAdjsAspects + rejectedHierarchiesAdjsAspects);
		
		System.out.println("Acceptance ratio of aspect hierarchies for verbs: " + ratioAspectVerbs);	
		System.out.println("Acceptance ratio of aspect hierarchies for nouns: " + ratioAspectNouns);	
		System.out.println("Acceptance ratio of aspect hierarchies for adjectives: " + ratioAspectAdjectives);		
	
		ArrayList<int[]> result = new ArrayList<>();
		result.add(resultSent);
		result.add(resultAspects);
		
		return result;
	}
	
	/***
	 * A method to find the appropriate sentiment parents for sentiment concepts
	 * 
	 * @param pathToTerms path to a file with the accepted sentiment concepts
	 * @param parentURI aspect parent
	 * @param thresholds list with thresholds for different POS
	 * @param pathToFileAccepted path to a file with the accepted hierarchies
	 * @param pathToFileRejected path to a file with the rejected hierarchies
	 * @throws IOException
	 */
	public void hierarchySentimentConcepts(String pathToTerms, String parentURI, ArrayList<Double> thresholds, String pathToFileAccepted, String pathToFileRejected) throws IOException
	{
		ArrayList<HashMap<String, double[]>> embeddingsAll = readEmbeddingsFromFile(pathToTerms);
		HashMap<String, double[]> embeddings = new HashMap<String, double[]>();
		
		String pos = "";
		double threshold = 1.0;
		
		if (parentURI.contains("Action"))
		{
			embeddings = embeddingsAll.get(0);
			pos = "verb";
//			System.out.println("Parent contains Action");
			threshold = thresholds.get(3);
		}
		else if (parentURI.contains("Entity"))
		{
			embeddings = embeddingsAll.get(1);
			pos = "noun";
//			System.out.println("Parent contains Entity");
			threshold = thresholds.get(4);
		}
		else if (parentURI.contains("Property"))
		{
			embeddings = embeddingsAll.get(2);
			pos = "adj";
//			System.out.println("Parent contains Property");
			threshold = thresholds.get(5);
		}
		
		HashSet<String> lexicalizations = base.getLexicalizations(parentURI);
		
		ArrayList<String> possibleParents = new ArrayList<>();
		HashSet<String> lexicalizationsTemp = new HashSet<>();
		
		for (String lex : lexicalizations)
		{
			possibleParents.add(lex + "-noun");
			lexicalizationsTemp.add(lex + "-noun");
		}
		
		ArrayList<String> possibleParentsCopy = possibleParents;
		
		ArrayList<ArrayList<String>> acceptedSentimentParents = new ArrayList<>();
		
		Scanner in = new Scanner(System.in);
		
		String tempParent = parentURI.replace(Ontology.namespace + "#", "");
		String tempPar = tempParent.split("(?=\\p{Upper})")[0].toLowerCase();
		
		if (tempPar.contains("&"))
		{
			tempPar = tempPar.replace("&", "");
		}
		
		System.out.println("Determining sentiments for sentiment concepts of " + tempPar + " with POS: " + pos);
		System.out.println("--> To accept a parent type 'a'");
		System.out.println("--> To reject a parent type 'r'");
		
		for (String vec : embeddings.keySet())
		{
			System.out.println("Looking for parents of " + vec);
			
			possibleParents = new ArrayList<>();
			possibleParents.addAll(possibleParentsCopy);
			
			boolean conditionType2 = false;
			
			boolean conditionSkipType3 = false;
			
			for (int i = 0; i <= possibleParents.size() - 1; i++)
			{	
				String possibleParent = possibleParents.get(i);
				
				double[] parentVec = getVector(possibleParent);
					
				double[] positiveVec = getVector("positive-adj");
				double[] negativeVec = getVector("negative-adj");
				
				double sim = cosineSimilarity(embeddings.get(vec), parentVec);
				double simPositive = cosineSimilarity(embeddings.get(vec), positiveVec);
				double simNegative = cosineSimilarity(embeddings.get(vec), negativeVec);
				
//				if ()
//				{					
					if (simPositive > simNegative)
					{
						System.out.println("Suggested parent: " + possibleParent + " with positive sentiment" );
						
						List<String> acceptedHierarchies = loadHierarchiesFile(pathToFileAccepted);
						List<String> rejectedHierarchies = loadHierarchiesFile(pathToFileRejected);
						
						String input = "";
						
						boolean conditionToPrint = true;
						
						if ((!conditionType2) && acceptedHierarchies.contains(vec + " " + possibleParent + " Positive"))
						{
							input = "a";
							System.out.println("Hierarchy was already accepted");
							conditionToPrint = false;
						}
						else if ((!conditionType2) && rejectedHierarchies.contains(vec + " " + possibleParent + " Positive"))
						{
							input = "r";
							System.out.println("Hierarchy was already rejected");
							conditionToPrint = false;
						}
						else
						{
							input = in.next();
						}
						
						FileWriter fw_accepted = new FileWriter(pathToFileAccepted, true);
					    BufferedWriter bw_accepted = new BufferedWriter(fw_accepted);
					    PrintWriter accepted = new PrintWriter(bw_accepted);
					    
						FileWriter fw_rejected = new FileWriter(pathToFileRejected, true);
					    BufferedWriter bw_rejected = new BufferedWriter(fw_rejected);
					    PrintWriter rejected = new PrintWriter(bw_rejected);
						
						if (input.equals("a"))
						{							
							ArrayList<String> accept = new ArrayList<>();
							accept.add(vec);
							accept.add(possibleParent);
							accept.add("positive");
							
							acceptedSentimentParents.add(accept);
														
							if (pos.equals("verb"))
							{
								acceptedHierarchiesVerbsSentType2++;
							}
							else if (pos.equals("noun"))
							{
								acceptedHierarchiesNounsSentType2++;
							}
							else if (pos.equals("adj"))
							{
								acceptedHierarchiesAdjsSentType2++;
							}
							
							if (conditionToPrint && (!conditionType2))
							{
								accepted.println(vec +  " " + possibleParent + " Positive");
							}
							
							System.out.println("Sentiment concept is Type-2 sentiment");
						}
						else if (input.equals("r"))
						{
							//Suggest opposite sentiment
							System.out.println("Suggested parent: " + possibleParent + " with negative sentiment" );
							
							if (pos.equals("verb"))
							{
								rejectedHierarchiesVerbsSentType2++;
							}
							else if (pos.equals("noun"))
							{
								rejectedHierarchiesNounsSentType2++;
							}
							else if (pos.equals("adj"))
							{
								rejectedHierarchiesAdjsSentType2++;
							}
							
							if (conditionToPrint && (!conditionType2))
							{
								rejected.println(vec +  " " + possibleParent + " Positive");
							}
							
							accepted.close();
							bw_accepted.close();
							fw_accepted.close();
							rejected.close();
							bw_rejected.close();
							fw_rejected.close();
							
							List<String> acceptedHierarchies2 = loadHierarchiesFile(pathToFileAccepted);
							List<String> rejectedHierarchies2 = loadHierarchiesFile(pathToFileRejected);
							
							String input2 = "";
							
							boolean conditionToPrint2 = true;
							
							if ((!conditionType2) && acceptedHierarchies2.contains(vec + " " + possibleParent + " Negative"))
							{
								input2 = "a";
								System.out.println("Hierarchy was already accepted");
								conditionToPrint2 = false;
							}
							else if ((!conditionType2) && rejectedHierarchies2.contains(vec + " " + possibleParent + " Negative"))
							{
								input2 = "r";
								System.out.println("Hierarchy was already rejected");
								conditionToPrint2 = false;
							}
							else
							{
								input2 = in.next();
							}
							
							FileWriter fw_accepted2 = new FileWriter(pathToFileAccepted, true);
						    BufferedWriter bw_accepted2 = new BufferedWriter(fw_accepted2);
						    PrintWriter accepted2 = new PrintWriter(bw_accepted2);
						    
							FileWriter fw_rejected2 = new FileWriter(pathToFileRejected, true);
						    BufferedWriter bw_rejected2 = new BufferedWriter(fw_rejected2);
						    PrintWriter rejected2 = new PrintWriter(bw_rejected2);
							
							if (input2.equals("a"))
							{								
								ArrayList<String> accept = new ArrayList<>();
								accept.add(vec);
								accept.add(possibleParent);
								accept.add("negative");
								
								acceptedSentimentParents.add(accept);
								
								if (pos.equals("verb"))
								{
									acceptedHierarchiesVerbsSentType2++;
								}
								else if (pos.equals("noun"))
								{
									acceptedHierarchiesNounsSentType2++;
								}
								else if (pos.equals("adj"))
								{
									acceptedHierarchiesAdjsSentType2++;
								}
								
								if (conditionToPrint2 && (!conditionType2))
								{
									accepted2.println(vec +  " " + possibleParent + " Negative");
								}
								
								System.out.println("Sentiment concept is Type-2 sentiment");
							}
							else if (input2.equals("r"))
							{								
								if (pos.equals("verb"))
								{
									rejectedHierarchiesVerbsSentType2++;
								}
								else if (pos.equals("noun"))
								{
									rejectedHierarchiesNounsSentType2++;
								}
								else if (pos.equals("adj"))
								{
									rejectedHierarchiesAdjsSentType2++;
								}
								
								if (conditionToPrint2 && (!conditionType2))
								{
									rejected2.println(vec +  " " + possibleParent + " Negative");
								}
								
								accepted2.close();
								bw_accepted2.close();
								fw_accepted2.close();
								rejected2.close();
								bw_rejected2.close();
								fw_rejected2.close();
								
								//Both Type-2 parents were rejected so concept is changed into Type-3
								if (!conditionSkipType3)
								{
									Pair<ArrayList<ArrayList<String>>, ArrayList<String>> outputType3 = hierarchyType3Sentiments(pathToTerms, parentURI, threshold, pos, in, vec, embeddings.get(vec), pathToFileAccepted, pathToFileRejected);
									ArrayList<String> outputType3Parent = outputType3.getValue();
									ArrayList<ArrayList<String>> outputType3Accepted = outputType3.getKey();
									
									conditionSkipType3 = true;
									
									if (outputType3Parent.size() > 0)
									{
										for (String tempParType3 : outputType3Parent)
										{
											possibleParents.add(tempParType3);
											conditionType2 = true;
										}
									}
									
									if (outputType3Accepted.size() > 0)
									{
										for (ArrayList<String> tempAccepted : outputType3Accepted)
										{
											acceptedSentimentParents.add(tempAccepted);
										}
									}
								}
							}
							
							accepted2.close();
							bw_accepted2.close();
							fw_accepted2.close();
							rejected2.close();
							bw_rejected2.close();
							fw_rejected2.close();
						}																			
						
						accepted.close();
						bw_accepted.close();
						fw_accepted.close();
						rejected.close();
						bw_rejected.close();
						fw_rejected.close();

					}
					else
					{
						System.out.println("Suggested parent: " + possibleParent + " with negative sentiment" );
						
						List<String> acceptedHierarchies = loadHierarchiesFile(pathToFileAccepted);
						List<String> rejectedHierarchies = loadHierarchiesFile(pathToFileRejected);
						
						String input = "";
						
						boolean conditionToPrint = true;
						
						if ((!conditionType2) && acceptedHierarchies.contains(vec + " " + possibleParent + " Negative"))
						{
							input = "a";
							System.out.println("Hierarchy was already accepted");
							conditionToPrint = false;
						}
						else if ((!conditionType2) && rejectedHierarchies.contains(vec + " " + possibleParent + " Negative"))
						{
							input = "r";
							System.out.println("Hierarchy was already rejected");
							conditionToPrint = false;
						}
						else
						{
							input = in.next();
						}
						
						FileWriter fw_accepted = new FileWriter(pathToFileAccepted, true);
					    BufferedWriter bw_accepted = new BufferedWriter(fw_accepted);
					    PrintWriter accepted = new PrintWriter(bw_accepted);
					    
						FileWriter fw_rejected = new FileWriter(pathToFileRejected, true);
					    BufferedWriter bw_rejected = new BufferedWriter(fw_rejected);
					    PrintWriter rejected = new PrintWriter(bw_rejected);
						
						if (input.equals("a"))
						{						
							ArrayList<String> accept = new ArrayList<>();
							accept.add(vec);
							accept.add(possibleParent);
							accept.add("negative");
							
							acceptedSentimentParents.add(accept);
							
							if (pos.equals("verb"))
							{
								acceptedHierarchiesVerbsSentType2++;
							}
							else if (pos.equals("noun"))
							{
								acceptedHierarchiesNounsSentType2++;
							}
							else if (pos.equals("adj"))
							{
								acceptedHierarchiesAdjsSentType2++;
							}
							
							if (conditionToPrint && (!conditionType2))
							{
								accepted.println(vec +  " " + possibleParent + " Negative");
							}
							
							System.out.println("Sentiment concept is Type-2 sentiment");
						}
						else if (input.equals("r"))
						{
							//Suggest opposite sentiment
							System.out.println("Suggested parent: " + possibleParent + " with positive sentiment" );
							
							if (pos.equals("verb"))
							{
								rejectedHierarchiesVerbsSentType2++;
							}
							else if (pos.equals("noun"))
							{
								rejectedHierarchiesNounsSentType2++;
							}
							else if (pos.equals("adj"))
							{
								rejectedHierarchiesAdjsSentType2++;
							}
							
							if (conditionToPrint && (!conditionType2))
							{
								rejected.println(vec +  " " + possibleParent + " Negative");
							}
							
							accepted.close();
							bw_accepted.close();
							fw_accepted.close();
							rejected.close();
							bw_rejected.close();
							fw_rejected.close();
							
							List<String> acceptedHierarchies2 = loadHierarchiesFile(pathToFileAccepted);
							List<String> rejectedHierarchies2 = loadHierarchiesFile(pathToFileRejected);
							
							String input2 = "";
							
							boolean conditionToPrint2 = true;
							
							if ((!conditionType2) && acceptedHierarchies2.contains(vec + " " + possibleParent + " Positive"))
							{
								input2 = "a";
								System.out.println("Hierarchy was already accepted");
								conditionToPrint2 = false;
							}
							else if ((!conditionType2) && rejectedHierarchies2.contains(vec + " " + possibleParent + " Positive"))
							{
								input2 = "r";
								System.out.println("Hierarchy was already rejected");
								conditionToPrint2 = false;
							}
							else
							{
								input2 = in.next();
							}
							
							FileWriter fw_accepted2 = new FileWriter(pathToFileAccepted, true);
						    BufferedWriter bw_accepted2 = new BufferedWriter(fw_accepted2);
						    PrintWriter accepted2 = new PrintWriter(bw_accepted2);
						    
							FileWriter fw_rejected2 = new FileWriter(pathToFileRejected, true);
						    BufferedWriter bw_rejected2 = new BufferedWriter(fw_rejected2);
						    PrintWriter rejected2 = new PrintWriter(bw_rejected2);
							
							if (input2.equals("a"))
							{								
								ArrayList<String> accept = new ArrayList<>();
								accept.add(vec);
								accept.add(possibleParent);
								accept.add("positive");
								
								acceptedSentimentParents.add(accept);
								
								if (pos.equals("verb"))
								{
									acceptedHierarchiesVerbsSentType2++;
								}
								else if (pos.equals("noun"))
								{
									acceptedHierarchiesNounsSentType2++;
								}
								else if (pos.equals("adj"))
								{
									acceptedHierarchiesAdjsSentType2++;
								}
								
								if (conditionToPrint2 && (!conditionType2))
								{
									accepted2.println(vec +  " " + possibleParent + " Positive");
								}
								
								System.out.println("Sentiment concept is Type-2 sentiment");								
							}
							else if (input2.equals("r"))
							{								
								if (pos.equals("verb"))
								{
									rejectedHierarchiesVerbsSentType2++;
								}
								else if (pos.equals("noun"))
								{
									rejectedHierarchiesNounsSentType2++;
								}
								else if (pos.equals("adj"))
								{
									rejectedHierarchiesAdjsSentType2++;
								}
								
								if (conditionToPrint2 && (!conditionType2))
								{
									rejected2.println(vec +  " " + possibleParent + " Positive");
								}
								
								accepted2.close();
								bw_accepted2.close();
								fw_accepted2.close();
								rejected2.close();
								bw_rejected2.close();
								fw_rejected2.close();
								
								//Both Type-2 parents were rejected so concept is changed into Type-3
								if (!conditionSkipType3)
								{
									Pair<ArrayList<ArrayList<String>>, ArrayList<String>> outputType3 = hierarchyType3Sentiments(pathToTerms, parentURI, threshold, pos, in, vec, embeddings.get(vec), pathToFileAccepted, pathToFileRejected);							
									ArrayList<String> outputType3Parent = outputType3.getValue();
									ArrayList<ArrayList<String>> outputType3Accepted = outputType3.getKey();
									
									conditionSkipType3 = true;
									
									if (outputType3Parent.size() > 0)
									{
										for (String tempParType3 : outputType3Parent)
										{
											possibleParents.add(tempParType3);
											conditionType2 = true;
										}
									}
									
									if (outputType3Accepted.size() > 0)
									{
										for (ArrayList<String> tempAccepted : outputType3Accepted)
										{
											acceptedSentimentParents.add(tempAccepted);
										}
									}
								}
							}
							
							accepted2.close();
							bw_accepted2.close();
							fw_accepted2.close();
							rejected2.close();
							bw_rejected2.close();
							fw_rejected2.close();
						}			
						
						accepted.close();
						bw_accepted.close();
						fw_accepted.close();
						rejected.close();
						bw_rejected.close();
						fw_rejected.close();
					}
					
				}
			}
//		}
		
		System.out.println("Adding the accepted hierarchies to the ontology");
		
		for (ArrayList<String> accepted : acceptedSentimentParents)
		{
			String vec = accepted.get(0);
			String parent = accepted.get(1);
			String sent = accepted.get(2);
			
			if(lexicalizationsTemp.contains(parent))
			{
//				System.out.println("Type-2 sentiment is created");
				
				if (sent.equals("positive"))
				{					
					base.setSuperClassSentiments(vec, true, pos, parentURI);
				}
				else
				{
					base.setSuperClassSentiments(vec, false, pos, parentURI);
				}
			}
			else
			{
//				System.out.println("Type-3 sentiment is created");
				
				if (sent.equals("positive"))
				{
					base.setType3SuperClassSentiments(parent, vec, true, pos);
				}
				else
				{
					base.setType3SuperClassSentiments(parent, vec, false, pos);
				}				
			}			
		}		
	}
	
	/***
	 * A method to find parents of a Type-3 sentiment concept
	 * 
	 * @param pathToTerms path to terms that can be parents of a Type-3 concept
	 * @param parentURI aspect parent
	 * @param threshold above which hierarchies are suggested
	 * @param pos part-of-speech tag
	 * @param in scanner
	 * @param vecName concept for which parents are being searched for
	 * @param vec vector of vecName
	 * @param pathToFileAccepted path to a file with the already accepted hierarchies
	 * @param pathToFileRejected path to a file with the already rejected hierarchies
	 * @return a pair with the list of accepted sentiment parents and a list of concepts that need to be changed back into Type-2
	 * @throws IOException
	 */
	public Pair<ArrayList<ArrayList<String>>, ArrayList<String>> hierarchyType3Sentiments(String pathToTerms, String parentURI, double threshold, String pos, Scanner in, String vecName, double[] vec, String pathToFileAccepted, String pathToFileRejected) throws IOException
	{
		System.out.println("Sentiment concept will be Type-3 sentiment");
		
		String pathToAspects = pathToTerms.replace("SentimentConcepts", "AspectConcepts");
		
		if (pathToAspects.contains("Action"))
		{
			pathToAspects = pathToAspects.replace("Action", "Entity");
		}
		else if (pathToAspects.contains("Property"))
		{
			pathToAspects = pathToAspects.replace("Property", "Entity");
		}
		
		ArrayList<HashMap<String, double[]>> embeddingsAll = readEmbeddingsFromFile(pathToAspects);
		HashMap<String, double[]> embeddings = embeddingsAll.get(1);
		
		ArrayList<ArrayList<String>> acceptedSentimentParents = new ArrayList<>();
		
		boolean conditionType2 = true;
		
		for (String embedding : embeddings.keySet())
		{
			double[] parentVec = embeddings.get(embedding);
			
			double[] positiveVec = getVector("positive-adj");
			double[] negativeVec = getVector("negative-adj");
			
			double sim = cosineSimilarity(vec, parentVec);
			double simPositive = cosineSimilarity(vec, positiveVec);
			double simNegative = cosineSimilarity(vec, negativeVec);
			
			if (sim >= threshold)
			{
				if (simPositive > simNegative)
				{
					System.out.println("Suggested parent: " + embedding + " with positive sentiment" );
					
					List<String> acceptedHierarchies = loadHierarchiesFile(pathToFileAccepted);
					List<String> rejectedHierarchies = loadHierarchiesFile(pathToFileRejected);
					
					boolean conditionToPrint = true;
					
					String input = "";
					
					if (acceptedHierarchies.contains(vecName + " " + embedding + " Positive"))
					{
						input = "a";
						System.out.println("Hierarchy was already accepted");
						conditionToPrint = false;
					}
					else if (rejectedHierarchies.contains(vecName + " " + embedding + " Positive"))
					{
						input = "r";
						System.out.println("Hierarchy was already rejected");
						conditionToPrint = false;
					}
					else
					{
						input = in.next();
					}
					
					FileWriter fw_accepted = new FileWriter(pathToFileAccepted, true);
				    BufferedWriter bw_accepted = new BufferedWriter(fw_accepted);
				    PrintWriter accepted = new PrintWriter(bw_accepted);
				    
					FileWriter fw_rejected = new FileWriter(pathToFileRejected, true);
				    BufferedWriter bw_rejected = new BufferedWriter(fw_rejected);
				    PrintWriter rejected = new PrintWriter(bw_rejected);
				    
					if (input.equals("a"))
					{							
						ArrayList<String> accept = new ArrayList<>();
						accept.add(vecName);
						accept.add(embedding);
						accept.add("positive");
						
						acceptedSentimentParents.add(accept);
													
						if (pos.equals("verb"))
						{
							acceptedHierarchiesVerbsSentType3++;
						}
						else if (pos.equals("noun"))
						{
							acceptedHierarchiesNounsSentType3++;
						}
						else if (pos.equals("adj"))
						{
							acceptedHierarchiesAdjsSentType3++;
						}
						
						conditionType2 = false;
						
						if (conditionToPrint)
						{
							accepted.println(vecName +  " " + embedding + " Positive");
						}
					}
					else if (input.equals("r"))
					{
						//Suggest opposite sentiment
						System.out.println("Suggested parent: " + embedding + " with negative sentiment" );
						
						if (pos.equals("verb"))
						{
							rejectedHierarchiesVerbsSentType3++;
						}
						else if (pos.equals("noun"))
						{
							rejectedHierarchiesNounsSentType3++;
						}
						else if (pos.equals("adj"))
						{
							rejectedHierarchiesAdjsSentType3++;
						}
						
						if (conditionToPrint)
						{
							rejected.println(vecName +  " " + embedding + " Positive");
						}
						
						accepted.close();
						bw_accepted.close();
						fw_accepted.close();
						rejected.close();
						bw_rejected.close();
						fw_rejected.close();
						
						List<String> acceptedHierarchies2 = loadHierarchiesFile(pathToFileAccepted);
						List<String> rejectedHierarchies2 = loadHierarchiesFile(pathToFileRejected);
						
						String input2 = "";
						
						boolean conditionToPrint2 = true;
						
						if (acceptedHierarchies2.contains(vecName + " " + embedding + " Negative"))
						{
							input2 = "a";
							System.out.println("Hierarchy was already accepted");
							conditionToPrint2 = false;
						}
						else if (rejectedHierarchies2.contains(vecName + " " + embedding + " Negative"))
						{
							input2 = "r";
							System.out.println("Hierarchy was already rejected");
							conditionToPrint2 = false;
						}
						else
						{
							input2 = in.next();
						}
						
						FileWriter fw_accepted2 = new FileWriter(pathToFileAccepted, true);
					    BufferedWriter bw_accepted2 = new BufferedWriter(fw_accepted2);
					    PrintWriter accepted2 = new PrintWriter(bw_accepted2);
					    
						FileWriter fw_rejected2 = new FileWriter(pathToFileRejected, true);
					    BufferedWriter bw_rejected2 = new BufferedWriter(fw_rejected2);
					    PrintWriter rejected2 = new PrintWriter(bw_rejected2);
						
						if (input2.equals("a"))
						{								
							ArrayList<String> accept = new ArrayList<>();
							accept.add(vecName);
							accept.add(embedding);
							accept.add("negative");
							
							acceptedSentimentParents.add(accept);
							
							if (pos.equals("verb"))
							{
								acceptedHierarchiesVerbsSentType3++;
							}
							else if (pos.equals("noun"))
							{
								acceptedHierarchiesNounsSentType3++;
							}
							else if (pos.equals("adj"))
							{
								acceptedHierarchiesAdjsSentType3++;
							}
							
							conditionType2 = false;
							
							if (conditionToPrint2)
							{
								accepted2.println(vecName +  " " + embedding + " Negative");
							}
						}
						else if (input2.equals("r"))
						{								
							if (pos.equals("verb"))
							{
								rejectedHierarchiesVerbsSentType3++;
							}
							else if (pos.equals("noun"))
							{
								rejectedHierarchiesNounsSentType3++;
							}
							else if (pos.equals("adj"))
							{
								rejectedHierarchiesAdjsSentType3++;
							}
							
							if (conditionToPrint2)
							{
								rejected2.println(vecName +  " " + embedding + " Negative");
							}
						}
						
						accepted2.close();
						bw_accepted2.close();
						fw_accepted2.close();
						rejected2.close();
						bw_rejected2.close();
						fw_rejected2.close();
					}
					
					accepted.close();
					bw_accepted.close();
					fw_accepted.close();
					rejected.close();
					bw_rejected.close();
					fw_rejected.close();					
				}
				else
				{
					System.out.println("Suggested parent: " + embedding + " with negative sentiment" );
					
					List<String> acceptedHierarchies = loadHierarchiesFile(pathToFileAccepted);
					List<String> rejectedHierarchies = loadHierarchiesFile(pathToFileRejected);
					
					String input = "";
					
					boolean conditionToPrint = true;
					
					if (acceptedHierarchies.contains(vecName + " " + embedding + " Negative"))
					{
						input = "a";
						System.out.println("Hierarchy was already accepted");
						conditionToPrint = false;
					}
					else if (rejectedHierarchies.contains(vecName + " " + embedding + " Negative"))
					{
						input = "r";
						System.out.println("Hierarchy was already rejected");
						conditionToPrint = false;
					}
					else
					{
						input = in.next();
					}
					
					FileWriter fw_accepted = new FileWriter(pathToFileAccepted, true);
				    BufferedWriter bw_accepted = new BufferedWriter(fw_accepted);
				    PrintWriter accepted = new PrintWriter(bw_accepted);
				    
					FileWriter fw_rejected = new FileWriter(pathToFileRejected, true);
				    BufferedWriter bw_rejected = new BufferedWriter(fw_rejected);
				    PrintWriter rejected = new PrintWriter(bw_rejected);
					
					if (input.equals("a"))
					{						
						ArrayList<String> accept = new ArrayList<>();
						accept.add(vecName);
						accept.add(embedding);
						accept.add("negative");
						
						acceptedSentimentParents.add(accept);
						
						if (pos.equals("verb"))
						{
							acceptedHierarchiesVerbsSentType3++;
						}
						else if (pos.equals("noun"))
						{
							acceptedHierarchiesNounsSentType3++;
						}
						else if (pos.equals("adj"))
						{
							acceptedHierarchiesAdjsSentType3++;
						}
						
						conditionType2 = false;
						
						if (conditionToPrint)
						{
							accepted.println(vecName +  " " + embedding + " Negative");
						}
					}
					else if (input.equals("r"))
					{
						//Suggest opposite sentiment
						System.out.println("Suggested parent: " + embedding + " with positive sentiment" );
						
						if (pos.equals("verb"))
						{
							rejectedHierarchiesVerbsSentType3++;
						}
						else if (pos.equals("noun"))
						{
							rejectedHierarchiesNounsSentType3++;
						}
						else if (pos.equals("adj"))
						{
							rejectedHierarchiesAdjsSentType3++;
						}
						
						if (conditionToPrint)
						{
							rejected.println(vecName +  " " + embedding + " Negative");
						}
						
						accepted.close();
						bw_accepted.close();
						fw_accepted.close();
						rejected.close();
						bw_rejected.close();
						fw_rejected.close();
						
						List<String> acceptedHierarchies2 = loadHierarchiesFile(pathToFileAccepted);
						List<String> rejectedHierarchies2 = loadHierarchiesFile(pathToFileRejected);
						
						String input2 = "";
						
						boolean conditionToPrint2 = true;
						
						if (acceptedHierarchies2.contains(vecName + " " + embedding + " Positive"))
						{
							input2 = "a";
							System.out.println("Hierarchy was already accepted");
							conditionToPrint2 = false;
						}
						else if (rejectedHierarchies2.contains(vecName + " " + embedding + " Positive"))
						{
							input2 = "r";
							System.out.println("Hierarchy was already rejected");
							conditionToPrint2 = false;
						}
						else
						{
							input2 = in.next();
						}
						
						FileWriter fw_accepted2 = new FileWriter(pathToFileAccepted, true);
					    BufferedWriter bw_accepted2 = new BufferedWriter(fw_accepted2);
					    PrintWriter accepted2 = new PrintWriter(bw_accepted2);
					    
						FileWriter fw_rejected2 = new FileWriter(pathToFileRejected, true);
					    BufferedWriter bw_rejected2 = new BufferedWriter(fw_rejected2);
					    PrintWriter rejected2 = new PrintWriter(bw_rejected2);
						
						if (input2.equals("a"))
						{								
							ArrayList<String> accept = new ArrayList<>();
							accept.add(vecName);
							accept.add(embedding);
							accept.add("positive");
							
							acceptedSentimentParents.add(accept);
							
							if (pos.equals("verb"))
							{
								acceptedHierarchiesVerbsSentType3++;
							}
							else if (pos.equals("noun"))
							{
								acceptedHierarchiesNounsSentType3++;
							}
							else if (pos.equals("adj"))
							{
								acceptedHierarchiesAdjsSentType3++;
							}
							
							conditionType2 = false;
							
							if (conditionToPrint2)
							{
								accepted2.println(vecName +  " " + embedding + " Positive");
							}
							
						}
						else if (input2.equals("r"))
						{								
							if (pos.equals("verb"))
							{
								rejectedHierarchiesVerbsSentType3++;
							}
							else if (pos.equals("noun"))
							{
								rejectedHierarchiesNounsSentType3++;
							}
							else if (pos.equals("adj"))
							{
								rejectedHierarchiesAdjsSentType3++;
							}
							
							if (conditionToPrint2)
							{
								rejected2.println(vecName +  " " + embedding + " Positive");
							}
							
							accepted2.close();
							bw_accepted2.close();
							fw_accepted2.close();
							rejected2.close();
							bw_rejected2.close();
							fw_rejected2.close();							
						}
						
						accepted2.close();
						bw_accepted2.close();
						fw_accepted2.close();
						rejected2.close();
						bw_rejected2.close();
						fw_rejected2.close();
					}			
					
					accepted.close();
					bw_accepted.close();
					fw_accepted.close();
					rejected.close();
					bw_rejected.close();
					fw_rejected.close();
				}
			}
			else
			{
//				System.out.println(embedding + " with a score of " + sim + " is below the threshold of " + threshold);
			}
			
		}
		
		ArrayList<String> parentsType2 = new ArrayList<>();
		
		if (conditionType2)
		{
			System.out.println("No parents were accepted/suggested for the Type-3 sentiment: "  + vecName);
			System.out.println("Concept is changed back into Type-2. Please accept the right sentiment");
//			System.out.println("Otherwise, the concept will be deleted from the ontology");
			
			HashSet<String> lexicalizations = base.getLexicalizations(parentURI);
			
			for (String lex : lexicalizations)
			{
				parentsType2.add(lex + "-noun");
			}
		}
		
		return new Pair<ArrayList<ArrayList<String>>, ArrayList<String>>(acceptedSentimentParents, parentsType2);		
	}
	
	/***
	 * A method to read the accepted/rejected hierarchies from a file
	 * 
	 * @param filePath path to a file from which the hierarchies are to be read
	 * @return list of the read hierarchies
	 * @throws FileNotFoundException
	 */
	public List<String> loadHierarchiesFile(String filePath) throws FileNotFoundException
	{
	    Scanner scanner = new Scanner(new File(filePath));
	    List<String> hierarchies = new ArrayList<>();
	    while(scanner.hasNextLine())
	    {
	    	String temp = scanner.nextLine();
	    	
	        hierarchies.add(temp);
	    }
	    
	    scanner.close();
	    return hierarchies;
	}
	
	/***
	 * A method to build a hierarchy between aspect concepts
	 * 
	 * @param pathToTerms path to a file with the accepted aspect concepts
	 * @param parentURI aspect parent
	 * @param thresholds list with thresholds for different POS
	 * @param pathToAcceptedHierarchies path to a file with already accepted hierarchies
	 * @param pathToRejectedHierarchies path to a file with already rejected hierarchies
	 * @throws IOException
	 */
	public void hierarchyAspectConcepts(String pathToTerms, String parentURI, ArrayList<Double> thresholds, String pathToAcceptedHierarchies, String pathToRejectedHierarchies) throws IOException
	{
		ArrayList<HashMap<String, double[]>> embeddingsAll = readEmbeddingsFromFile(pathToTerms);
		HashMap<String, double[]> embeddings = new HashMap<String, double[]>();
				
		String pos = "";
		double threshold = 1.0;
		
		if (parentURI.contains("Action"))
		{
			embeddings = embeddingsAll.get(0);
			pos = "verb";
//			System.out.println("Parent contains Action");
			threshold = thresholds.get(0);
		}
		else if (parentURI.contains("Entity"))
		{
			embeddings = embeddingsAll.get(1);
			pos = "noun";
//			System.out.println("Parent contains Entity");
			threshold = thresholds.get(1);
		}
		else if (parentURI.contains("Property"))
		{
			embeddings = embeddingsAll.get(2);
			pos = "adj";
//			System.out.println("Parent contains Property");
			threshold = thresholds.get(2);
		}
		
		int rowCount = getWordPairs(embeddings.keySet().toArray()).length;
		Object[][] vectorPairs = new Object[rowCount][6];
		vectorPairs = getWordPairs(embeddings.keySet().toArray());
		
		HashSet<String> lexicalizations = base.getLexicalizations(parentURI);
		
		ArrayList<String> possibleParents = new ArrayList<>();
		for (String lex : lexicalizations)
		{
			possibleParents.add(lex);
		}
		
		ArrayList<ArrayList<String>> acceptedHierarchies = new ArrayList<ArrayList<String>>();
		
		for (int m = 0; m <= possibleParents.size() - 1; m++)
		{
			String parent = possibleParents.get(m);
			
			for (int i = 0; i <= vectorPairs.length - 1; i++)
			{
				String word1 = vectorPairs[i][0].toString();
//				System.out.println("Word1 " + word1);
				String word2 = vectorPairs[i][1].toString();
//				System.out.println("Word2 " + word2);
				
				double[] tempVec = embeddings.get(parent);
//				System.out.println("Parent " + parent);
				
				if (parent.split("-").length == 1)
				{
					String temp = parent + "-noun";
					tempVec = getVector(temp);
				}
				
				double simParentWord1 = cosineSimilarity(tempVec, embeddings.get(word1));
				double simParentWord2 = cosineSimilarity(tempVec, embeddings.get(word2));
				
				double score1 = (simParentWord1 - threshold) + (simParentWord1 - simParentWord2);
				double score2 = (simParentWord2 - threshold) + (simParentWord2 - simParentWord1);
				
				if (score1 > score2)
				{
					vectorPairs[i][2] = score1;
					vectorPairs[i][3] = 1;
					vectorPairs[i][4] = simParentWord1;
					vectorPairs[i][5] = simParentWord2;
				}
				else if (score2 > score1)
				{
					vectorPairs[i][2] = score2;
					vectorPairs[i][3] = 2;
					vectorPairs[i][4] = simParentWord2;
					vectorPairs[i][5] = simParentWord1;
				}				
			}
			
			Arrays.sort(vectorPairs, new ColumnComparator(2, false));
			
			Scanner in = new Scanner(System.in);
			
			System.out.println("Looking for hierarchies for " + parent + " with POS: " + pos);
			System.out.println("--> To accept a hierarchy type 'a'");
			System.out.println("--> To reject a hierarchy type 'r'");
			
			for (int j = 0; j <= vectorPairs.length - 1; j++)
			{
				String word1 = vectorPairs[j][0].toString();
				String word2 = vectorPairs[j][1].toString();
				
				System.out.println("j " + j);
				System.out.println("word1 " + word1);
				System.out.println("word2 " + word2);
				
				double score = (double) vectorPairs[j][2];
				int whichRel = (int) vectorPairs[j][3];
				double simParent1 = (double) vectorPairs[j][4];
				double simParent2 = (double) vectorPairs[j][5];
				
				ArrayList<String> hierarchy = new ArrayList<>();
				
				if (whichRel == 1)
				{
					hierarchy.add(parent);
					hierarchy.add(word1);
					hierarchy.add(word2);
				}
				else if (whichRel == 2)
				{
					hierarchy.add(parent);
					hierarchy.add(word2);
					hierarchy.add(word1);
				}
				
				boolean conditionToSkip = false;
				
				for (ArrayList<String> acc : acceptedHierarchies)
				{
					if (acc.get(0).equals(parent) && acc.get(2).equals(hierarchy.get(1)))
					{
						conditionToSkip = true;
						break;
					}
				}
				
				if (simParent1 > threshold && simParent1 > simParent2 && !conditionToSkip && !word1.equals(parent) && !word2.equals(parent))
				{					
					if (!checkForCycles(acceptedHierarchies, hierarchy))
					{
						System.out.println("Suggested hierarchy: " + hierarchy.get(0) + " --> " + hierarchy.get(1) + " --> " + hierarchy.get(2));

						List<String> acceptedHier = loadHierarchiesFile(pathToAcceptedHierarchies);
						List<String> rejectedHier = loadHierarchiesFile(pathToRejectedHierarchies);
						
						boolean conditionToPrint = true;
						
						String input = "";
												
						if (acceptedHier.contains(hierarchy.get(0) + " " + hierarchy.get(1) + " " + hierarchy.get(2)))
						{
							input = "a";
							System.out.println("Hierarchy was already accepted");
							conditionToPrint = false;
						}
						else if (rejectedHier.contains(hierarchy.get(0) + " " + hierarchy.get(1) + " " + hierarchy.get(2)))
						{
							input = "r";
							System.out.println("Hierarchy was already rejected");
							conditionToPrint = false;
						}
						else
						{
							input = in.next();
						}
						
						FileWriter fw_accepted = new FileWriter(pathToAcceptedHierarchies, true);
					    BufferedWriter bw_accepted = new BufferedWriter(fw_accepted);
					    PrintWriter accepted = new PrintWriter(bw_accepted);
					    
						FileWriter fw_rejected = new FileWriter(pathToRejectedHierarchies, true);
					    BufferedWriter bw_rejected = new BufferedWriter(fw_rejected);
					    PrintWriter rejected = new PrintWriter(bw_rejected);
						
						if (input.equals("a"))
						{
							if (!possibleParents.contains(hierarchy.get(1)))
							{
								possibleParents.add(hierarchy.get(1));
							}
						
							acceptedHierarchies.add(hierarchy);
							
							if (pos.equals("verb"))
							{
								acceptedHierarchiesVerbsAspects++;
							}
							else if (pos.equals("noun"))
							{
								acceptedHierarchiesNounsAspects++;
							}
							else if (pos.equals("adj"))
							{
								acceptedHierarchiesAdjsAspects++;
							}
							
							if (conditionToPrint)
							{
								accepted.println(hierarchy.get(0) + " " + hierarchy.get(1) + " " + hierarchy.get(2));
							}
						}
						else if (input.equals("r"))
						{							
							if (pos.equals("verb"))
							{
								rejectedHierarchiesVerbsAspects++;
							}
							else if (pos.equals("noun"))
							{
								rejectedHierarchiesNounsAspects++;
							}
							else if (pos.equals("adj"))
							{
								rejectedHierarchiesAdjsAspects++;
							}
							
							if (conditionToPrint)
							{
								rejected.println(hierarchy.get(0) + " " + hierarchy.get(1) + " " + hierarchy.get(2));
							}
						}
						
						accepted.close();
						bw_accepted.close();
						fw_accepted.close();
						rejected.close();
						bw_rejected.close();
						fw_rejected.close();
					}
					else
					{
						System.out.println("Suggested hierarchy: " + hierarchy.get(0) + " --> " + hierarchy.get(1) + " --> " + hierarchy.get(2));
						System.out.println("is skipped because of a cycle");
					}
				}
				
			}
		}
	    
	}
    
	/***
	 * A method to calculate a factorial of a certain number
	 * 
	 * @param number for which the factorial is to be calculated
	 * @return factorial result
	 */
    public int factorial(int number) 
    {
        int result = 1;

        for (int factor = 2; factor <= number; factor++) 
        {
            result *= factor;
        }

        return result;
    }
    
    /***
     * A method to get all the possible combinations of objects in triples
     * 
     * @param input objects from which the combinations are to be created
     * @return 2D array with all of the possible combinations
     */
    public Object[][] getCombinationsTriples(Object[] input)
    {
    	int len = input.length;

    	int nrCombinations = (len * (len - 1) * (len - 2)) / (3*2*1);

		Object[][] triples = new Object[nrCombinations][7];
        
        int counter = 0;

        for (int i = 0; i < len - 2; i++)
        {
            for (int j = i + 1; j < len - 1; j++)
            {
                for (int k = j + 1; k < len; k++)
                {
                    triples[counter][0] = input[i];
                    triples[counter][1] = input[j];
                    triples[counter][2] = input[k];
                    
                    counter++;
                }
            }
        }
        
        return triples;
    }
	
    /***
     * A method to create a hierarchy between aspect concepts with the triples-based approach
     * 
     * @param pathToTerms path to a file with the accepted aspect concepts
	 * @param parentURI aspect parent
	 * @param thresholds list with thresholds for different POS
	 * @param pathToAcceptedHierarchies path to a file with already accepted hierarchies
	 * @param pathToRejectedHierarchies path to a file with already rejected hierarchies
	 * @throws IOException
     */
	public void hierarchyAspectConceptsTriples(String pathToTerms, String parentURI, ArrayList<Double> thresholds, String pathToAcceptedHierarchies, String pathToRejectedHierarchies) throws IOException
	{
		ArrayList<HashMap<String, double[]>> embeddingsAll = readEmbeddingsFromFile(pathToTerms);
		HashMap<String, double[]> embeddings = new HashMap<String, double[]>();
		
		String pos = "";
		double threshold = 1.0;
		
		if (parentURI.contains("Action"))
		{
			embeddings = embeddingsAll.get(0);
			pos = "verb";
//			System.out.println("Parent contains Action");
			threshold = thresholds.get(0);
		}
		else if (parentURI.contains("Entity"))
		{
			embeddings = embeddingsAll.get(1);
			pos = "noun";
//			System.out.println("Parent contains Entity");
			threshold = thresholds.get(1);
		}
		else if (parentURI.contains("Property"))
		{
			embeddings = embeddingsAll.get(2);
			pos = "adj";
//			System.out.println("Parent contains Property");
			threshold = thresholds.get(2);
		}
		
		HashSet<String> lexicalizations = base.getLexicalizations(parentURI);
		
		for (String lex : lexicalizations)
		{
			String tempName = lex + "-noun";
			embeddings.put(tempName, getVector(tempName));
		}
				
		int rowCount = getCombinationsTriples(embeddings.keySet().toArray()).length;
		Object[][] vectorTriples = new Object[rowCount][7];
		vectorTriples = getCombinationsTriples(embeddings.keySet().toArray());
		
		for (int l = 0; l <= vectorTriples.length - 1; l++)
		{
			String term1 = (String) vectorTriples[l][0];
			String term2 = (String) vectorTriples[l][1];
			String term3 = (String) vectorTriples[l][2];
			
			double score1 = cosineSimilarity(embeddings.get(term1), embeddings.get(term2));
			vectorTriples[l][3] = score1;
			double score2 = cosineSimilarity(embeddings.get(term1), embeddings.get(term3));
			vectorTriples[l][4] = score2;
			double score3 = cosineSimilarity(embeddings.get(term2), embeddings.get(term3));
			vectorTriples[l][5] = score3;
			
			double harmonic = 3 / ((1 / score1) + (1 / score2) + (1 / score3));
			vectorTriples[l][6] = harmonic;
		}
		
		Arrays.sort(vectorTriples, new ColumnComparator(6, false));
		
		ArrayList<ArrayList<String>> acceptedHierarchies = new ArrayList<ArrayList<String>>();
		
		Scanner in = new Scanner(System.in);
		
		String temp = parentURI.replace(Ontology.namespace + "#", "");
		String parent = temp.split("(?=\\p{Upper})")[0].toLowerCase();
		
		System.out.println("Looking for hierarchies for " + parent + " with POS: " + pos);
		System.out.println("--> To accept a hierarchy type 'a'");
		System.out.println("--> To reject a hierarchy type 'r'");
		
		for (int i = 0; i <= vectorTriples.length - 1; i++)
		{
			String term1 = (String) vectorTriples[i][0];
			String term2 = (String) vectorTriples[i][1];
			String term3 = (String) vectorTriples[i][2];
			
			double score1 = (double) vectorTriples[i][3];
			double score2 = (double) vectorTriples[i][4];
			double score3 = (double) vectorTriples[i][5];
			
			double harmonic = (double) vectorTriples[i][6];
			
			ArrayList<String> hierarchy = new ArrayList<>();
			
			//3 is the parent
			if (score1 - score2 <= score2 - score3 && score1 - score2 <= score1 - score3)
			{
//				System.out.println("Case 1");
				hierarchy.add(term3);
				hierarchy.add(term1);
				hierarchy.add(term2);
			}
			//2 is the parent
			else if (score1 - score3 <= score2 - score3 && score1 - score3 <= score1 - score2)
			{
//				System.out.println("Case 2");
				hierarchy.add(term2);
				hierarchy.add(term1);
				hierarchy.add(term3);
			}
			//1 is the parent
			else if (score2 - score3 <= score1 - score2 && score2 - score3 <= score1 - score3)
			{
//				System.out.println("Case 3");
				hierarchy.add(term1);
				hierarchy.add(term2);
				hierarchy.add(term3);
			}
						
			if (!checkForCyclesTriples(acceptedHierarchies, hierarchy) && harmonic >= threshold)
			{
				System.out.println("Suggested hierarchy: " + hierarchy.get(0) + " --> " + hierarchy.get(1) + " and " + hierarchy.get(2));

				List<String> acceptedHier = loadHierarchiesFile(pathToAcceptedHierarchies);
				List<String> rejectedHier = loadHierarchiesFile(pathToRejectedHierarchies);
				
				boolean conditionToPrint = true;
				
				String input = "";
				
				if (acceptedHier.contains(hierarchy.get(0) + " " + hierarchy.get(1) + " " + hierarchy.get(2)))
				{
					input = "a";
					System.out.println("Hierarchy was already accepted");
					conditionToPrint = false;
				}
				else if (rejectedHier.contains(hierarchy.get(0) + " " + hierarchy.get(1) + " " + hierarchy.get(2)))
				{
					input = "r";
					System.out.println("Hierarchy was already rejected");
					conditionToPrint = false;
				}
				else
				{
					input = in.next();
				}
				
				FileWriter fw_accepted = new FileWriter(pathToAcceptedHierarchies, true);
			    BufferedWriter bw_accepted = new BufferedWriter(fw_accepted);
			    PrintWriter accepted = new PrintWriter(bw_accepted);
			    
				FileWriter fw_rejected = new FileWriter(pathToRejectedHierarchies, true);
			    BufferedWriter bw_rejected = new BufferedWriter(fw_rejected);
			    PrintWriter rejected = new PrintWriter(bw_rejected);
				
				if (input.equals("a"))
				{						
					acceptedHierarchies.add(hierarchy);
					
					if (pos.equals("verb"))
					{
						acceptedHierarchiesVerbsAspects++;
					}
					else if (pos.equals("noun"))
					{
						acceptedHierarchiesNounsAspects++;
					}
					else if (pos.equals("adj"))
					{
						acceptedHierarchiesAdjsAspects++;
					}
					
					if (conditionToPrint)
					{
						accepted.println(hierarchy.get(0) + " " + hierarchy.get(1) + " " + hierarchy.get(2));
					}
				}
				else if (input.equals("r"))
				{
					if (pos.equals("verb"))
					{
						rejectedHierarchiesVerbsAspects++;
					}
					else if (pos.equals("noun"))
					{
						rejectedHierarchiesNounsAspects++;
					}
					else if (pos.equals("adj"))
					{
						rejectedHierarchiesAdjsAspects++;
					}
					
					if (conditionToPrint)
					{
						rejected.println(hierarchy.get(0) + " " + hierarchy.get(1) + " " + hierarchy.get(2));
					}
				}
				
				accepted.close();
				bw_accepted.close();
				fw_accepted.close();
				rejected.close();
				bw_rejected.close();
				fw_rejected.close();
			}
			else if (checkForCyclesTriples(acceptedHierarchies, hierarchy))
			{
				System.out.println("Suggested hierarchy: " + hierarchy.get(0) + " --> " + hierarchy.get(1) + " and " + hierarchy.get(2));
				System.out.println("is skipped because of a cycle");
			}
			else if (harmonic < threshold)
			{
//				System.out.println("Harmonic is below the threshold");
			}
			
		}	

	}
	
	/***
	 * A method to check whether a certain hierarchy will not create a cycle with other 
	 * already accepted hierarchies (with the triples-based approach)
	 * 
	 * @param acceptedHierarchies list with already accepted hierarchies
	 * @param suggestedHierarchy suggested hierarchy for which it is checked whether the cycle will not appear
	 * @return false if no cycle
	 */
	public boolean checkForCyclesTriples(ArrayList<ArrayList<String>> acceptedHierarchies, ArrayList<String> suggestedHierarchy)
	{
		String parent = suggestedHierarchy.get(0);
		String child1 = suggestedHierarchy.get(1);
		String child2 = suggestedHierarchy.get(2);		
		
		for (ArrayList<String> acceptedHierarchy : acceptedHierarchies)
		{				
			if (acceptedHierarchy.contains(parent) || acceptedHierarchy.contains(child1) || acceptedHierarchy.contains(child2));
			{
					String acceptedParent = acceptedHierarchy.get(0);
					String acceptedChild1 = acceptedHierarchy.get(1);
					String acceptedChild2 = acceptedHierarchy.get(2);
					
					if (parent.equals(acceptedChild1))
					{
						if (child1.equals(acceptedParent))
						{
							return true;
						}
					}
					else if (parent.equals(acceptedChild2))
					{
						if (child2.equals(acceptedParent))
						{
							return true;
						}
					}
			}
		}
		
		return false; 
	}
	
	/***
	 * A method to check whether a certain hierarchy will not create a cycle with other 
	 * already accepted hierarchies
	 * 
	 * @param acceptedHierarchies list with already accepted hierarchies
	 * @param suggestedHierarchy suggested hierarchy for which it is checked whether the cycle will not appear
	 * @return false if no cycle
	 */
	public boolean checkForCycles(ArrayList<ArrayList<String>> acceptedHierarchies, ArrayList<String> suggestedHierarchy)
	{
		String element1 = suggestedHierarchy.get(0);
		String element2 = suggestedHierarchy.get(1);
		String element3 = suggestedHierarchy.get(2);
		
		for (ArrayList<String> acceptedHierarchy : acceptedHierarchies)
		{				
			if (acceptedHierarchy.contains(element1) || acceptedHierarchy.contains(element2) || acceptedHierarchy.contains(element3));
			{
				for (int i = 0; i <= acceptedHierarchy.size() - 2; i++)
				{
					String firstElement = acceptedHierarchy.get(i);
					String secondElement = acceptedHierarchy.get(i + 1);
					
					if (firstElement.equals(element1))
					{
						if (secondElement.equals(element2))
						{
							return false;
						}
						else if (secondElement.equals(element3))
						{
							return false;
						}
					}
					else if (secondElement.equals(element1))
					{
						if (firstElement.equals(element2))
						{
							return true;
						}
						else if (firstElement.equals(element3))
						{
							return true;
						}
					}
					else if (firstElement.equals(element2))
					{
						if (secondElement.equals(element3))
						{
							return false;
						}
						else if (secondElement.equals(element1))
						{
							return true;
						}
					}
					else if (secondElement.equals(element2))
					{
						if (firstElement.equals(element3))
						{
							return true;
						}
					}
				}
			}
		}
		
		return false;
	}
	
	/***
	 * A method to create all the possible combinations (without repetition) of pairs of words
	 * 
	 * @param words for which the pairs are to be created
	 * @return 2D array with all the possible pairs
	 */
	public Object[][] getWordPairs(Object[] words)
	{
		int nrCombinations = (words.length * (words.length - 1)) / 2;
		Object[][] pairs = new Object[nrCombinations][6];
		int counter = -1;
		
		for (int i = 0; i <= words.length - 2; i++)
		{
			for (int j = i + 1; j <= words.length - 1; j++)
			{
				counter++;
				pairs[counter][0] = words[i].toString();
				pairs[counter][1] = words[j].toString();
			}
		}
		
		return pairs;
	}
	
	/***
	 * A method to get the file name (with the accepted/rejected terms/hierarchies) out of a class name
	 * 
	 * @param parentURI class name which will be converted into a file name
	 * @return file name for the given class
	 */
	public String getFileNameFromClassName(String parentURI)
	{			
			String parent = parentURI;
			parent = parent.substring(Ontology.namespace.length() + 1, parent.length());

			String[] temp = parent.split("(?=\\p{Upper})");
			
			String catOrAtt = "";
			String type = "";
						
			if (temp.length == 4)
			{
				catOrAtt = temp[0] + temp[1];
				type = temp[2];
			}
			else
			{
				catOrAtt = temp[0];
				type = temp[1];
			}
			
			return Framework.ACCEPTEDTERMS_PATH + catOrAtt + type;
	}
	
	/***
	 * A method to sort a 2D array by the values in the given column
	 * 
	 * @param arr 2D array which is to be sorted (row-wise)
	 * @param col column based on which the array will be sorted
	 */
	public void sortbyColumn(double arr[][], int col) 
    { 
        //Using built-in sort function Arrays.sort 
        Arrays.sort(arr, new Comparator<double[]>() 
        {       
          @Override              
          //Compare values according to columns 
          public int compare(final double[] entry1,  final double[] entry2) 
          { 
            //To sort in descending order revert  
            //the '>' Operator 
            if (entry1[col] < entry2[col]) 
                return 1; 
            else
                return -1; 
          } 
        });
    }
	
	/***
	 * A method to create concepts out of all the accepted terms
	 * 
	 * @throws IOException
	 */
	public void conceptualization() throws IOException
	{
		System.out.println("Creating aspect concepts");
		conceptualizationAspectConcepts();
		
		System.out.println("Creating Type-1 sentiment concepts");
		conceptualizationGenericConcepts();
		
		System.out.println("Creating Type-2 and Type-3 sentiment concepts");
		conceptualizationSentimentConcepts();
	}
	
	/***
	 * A method to create aspect concepts for the accepted aspect terms
	 * 
	 * @throws IOException
	 */
	public void conceptualizationAspectConcepts() throws IOException
	{
		for (String parent : aspectParentClasses)
		{						
			String pathToAcceptedTerms = getFileNameFromClassName(parent) + ("-AspectConcepts.txt");
			
			BufferedReader br = new BufferedReader(new FileReader(pathToAcceptedTerms));
			String line;
			
			while ((line = br.readLine()) != null) 
			{
				String lexName = line.split(" ")[0];				
				String[] temp = lexName.split("-");
				String lex = temp[0];
				
				for (int l = 1; l <= temp.length - 2; l++)
				{
					if (temp.length > 2)
					{
						lex = lex + "-" + temp[l];
					}
				}
				
				String newConcept = base.addClass(lex, true, lex.toLowerCase(), parent.split("#")[1]);
			}
			
			br.close();
		}
	}
	
	/***
	 * A method to create Type-1 sentiment concepts for the accepted Type-1 sentiment terms
	 * 
	 * @throws IOException
	 */
	public void conceptualizationGenericConcepts() throws IOException
	{
		for (String parent : genericParentClasses)
		{
			String fileName  = parent.replace(Ontology.namespace + "#", "");
			
			BufferedReader br = new BufferedReader(new FileReader(Framework.ACCEPTEDTERMS_PATH + fileName + "-AcceptedConcepts.txt"));
			
			String line;
			
			while ((line = br.readLine()) != null) 
			{
				String lexName = line.split(" ")[0];				
				String[] temp = lexName.split("-");
				String lex = temp[0];
				
				for (int l = 1; l <= temp.length - 2; l++)
				{
					if (temp.length > 2)
					{
						lex = lex + "-" + temp[l];
					}
				}
				
				String newConcept = base.addClass(lex, true, lex.toLowerCase(), parent.split("#")[1]);
			}
			
			br.close();
		}
	}
	
	/***
	 * A method to create sentiment (Type-2 or Type-3) concepts for the accepted sentiment terms
	 * 
	 * @throws IOException
	 */
	public void conceptualizationSentimentConcepts() throws IOException
	{
		for (String parent : aspectParentClasses)
		{						
			String pathToAcceptedTerms = getFileNameFromClassName(parent) + ("-SentimentConcepts.txt");
			
			BufferedReader br = new BufferedReader(new FileReader(pathToAcceptedTerms));
			String line;
			
			while ((line = br.readLine()) != null) 
			{
				String lexName = line.split(" ")[0];
				
				String[] temp = lexName.split("-");
				String lex = temp[0];
				
				for (int l = 1; l <= temp.length - 2; l++)
				{
					if (temp.length > 2)
					{
						lex = lex + "-" + temp[l];
					}
				}
				
				String newConcept = base.addClass(lex, true, lex.toLowerCase());
			}
			
			br.close();
		}
	}
	
	/***
	 * A method to get the given vector
	 * 
	 * @param vecName embedding for which the vector is to be obtained
	 * @return vector of vecName
	 */
	public double[] getVector(String vecName)
	{
		String[] temp = vecName.split("-");
		String pos = temp[temp.length - 1];
		
		HashMap<String, double[]> vectors = new HashMap<>();
		
		if (pos.equals("verb"))
		{
			vectors = verbs;
		}
		else if (pos.equals("noun"))
		{
			vectors = nouns;
		}
		else if (pos.equals("adj"))
		{
			vectors = adjectives;
		}
		
		for (String key : vectors.keySet())
		{
			if (key.equals(vecName))
			{
				return vectors.get(key);
			}
		}
		
		return null;
	}

}

