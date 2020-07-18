package edu.eur.absa.Yelp;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import edu.eur.absa.Framework;
import edu.eur.absa.model.exceptions.IllegalSpanException;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

/***
 * A class which performs the necessary NLP steps for the Yelp reviews and tips
 * 
 * @author Ewelina Dera
 *
 */
public class TxtForEmbeddings 
{

	public static void main(String[] args) throws IOException, ParseException, ClassNotFoundException, JSONException, IllegalSpanException 
	{		
		//Output with the processed reviews and tips
    	PrintWriter output = new PrintWriter(Framework.DATA_PATH + "Yelp_processed_reviews_tips_ALL.txt");
    	
    	JSONParser parser = new JSONParser();
    	
    	Framework.log("Loading reviews part 1");
    	JSONArray jsonArrayReview1 = (JSONArray) parser.parse(new FileReader(Framework.DATA_PATH+"Yelp_restaurant_reviews_part1.json"));
    	Framework.log("Processing reviews part 1");
    	writeTxtForReviews(jsonArrayReview1, output);
    	
    	Framework.log("Loading reviews part 2");
    	JSONArray jsonArrayReview2 = (JSONArray) parser.parse(new FileReader(Framework.DATA_PATH+"Yelp_restaurant_reviews_part2.json"));
    	Framework.log("Processing reviews part 2");
    	writeTxtForReviews(jsonArrayReview2, output);
    	
    	Framework.log("Loading reviews part 3");
    	JSONArray jsonArrayReview3 = (JSONArray) parser.parse(new FileReader(Framework.DATA_PATH+"Yelp_restaurant_reviews_part3.json"));
    	Framework.log("Processing reviews part 3");
    	writeTxtForReviews(jsonArrayReview3, output);
    	
    	Framework.log("Loading reviews part 4");
    	JSONArray jsonArrayReview4 = (JSONArray) parser.parse(new FileReader(Framework.DATA_PATH+"Yelp_restaurant_reviews_part4.json"));
    	Framework.log("Processing reviews part 4");
    	writeTxtForReviews(jsonArrayReview4, output);
    	
    	Framework.log("Loading tips");
    	JSONArray jsonArrayTip = (JSONArray) parser.parse(new FileReader(Framework.DATA_PATH+"Yelp_restaurant_tips.json"));
    	Framework.log("Processing tips");
    	writeTxtForTips(jsonArrayTip, output);
    	
    	output.close();
	}
	
	/***
	 * A method which performs NLP (sentence splitting, tokenization, pos-tagging and lemmatization)
	 * for the Yelp reviews and writes the output to a txt file
	 * 
	 * @param jsonArrayReview array with all the reviews to be processed
	 * @param output where the processed reviews will be written
	 * @throws ClassNotFoundException
	 * @throws JSONException
	 * @throws IllegalSpanException
	 * @throws IOException
	 * @throws ParseException
	 */
	public static void writeTxtForReviews(JSONArray jsonArrayReview, PrintWriter output) throws ClassNotFoundException, JSONException, IllegalSpanException, IOException, ParseException
	{    	
		//Creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution 
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		
		int counter = 0;
    	
		//For each review
    	for (Object o : jsonArrayReview)
    	{	   		
    		counter++;
    		
    		Framework.log("Processing review " + counter);
    		
    		JSONObject rev = (JSONObject) o;

    		String text = (String) rev.get("text");
    		text = text.replaceAll("\\r\\n|\\r|\\n", " ");	//Delete all the new lines etc.
    		text = text.replaceAll("/", " ");  

    		//Create an empty Annotation just with the given text
    		Annotation document = new Annotation(text);

    		//Run all Annotators on this text
    		pipeline.annotate(document);

    		//All the sentences in this document
    		List<CoreMap> sentences = document.get(SentencesAnnotation.class);

    		for(CoreMap sentence: sentences) 
    		{
//    			System.out.println(sentence.toString());
    			
    		  //Traversing the words in the current sentence
    		  for (CoreLabel token: sentence.get(TokensAnnotation.class)) 
    		  {
    		    //This is the text of the token
//    		    String word = token.get(TextAnnotation.class);
    			  
//    		    //This is the POS tag of the token
    		    String pos = token.get(PartOfSpeechAnnotation.class);
    		    
//    		    //This is the NER label of the token
//    		    String ne = token.get(NamedEntityTagAnnotation.class);
    		    
    		    //This is the lemma of the token
    		    String lemma = token.getString(LemmaAnnotation.class);
//    		    System.out.println(lemma);
    		    
    		    //If lemma contains any special signs etc.
    		    if (!Pattern.matches("[.,\\/#?!@$%\\^&\\*;:{}=\\-_`'~()]+", lemma) && !lemma.equals("-lrb-") && !lemma.equals("-rrb-") && !lemma.equals("-lsb-") && !lemma.equals("-rsb-") && !lemma.equals("-lcb-") && !lemma.equals("-rcb-"))
    		    {
    		    	lemma = lemma.toLowerCase() + "-" + pos;
    		    	output.print(lemma + " ");
    		    }
    		  }
    		  
    		  output.print(";");	//End each sentence with ;
    		}    
    		
    		output.println();	//Start each review in a new line
    	}
	}
	
	/***
	 * A method which performs NLP (sentence splitting, tokenization, pos-tagging and lemmatization)
	 * for the Yelp tips and writes the output to a txt file
	 * 
	 * @param jsonArrayReview array with all the tips to be processed
	 * @param output where the processed tips will be written
	 * @throws ClassNotFoundException
	 * @throws JSONException
	 * @throws IllegalSpanException
	 * @throws IOException
	 * @throws ParseException
	 */
	public static void writeTxtForTips(JSONArray jsonArrayTip, PrintWriter output) throws ClassNotFoundException, JSONException, IllegalSpanException, IOException, ParseException
	{   	
		//Creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution 
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
    	
		//For each tip
    	for (Object o : jsonArrayTip) 
    	{
    		JSONObject rev = (JSONObject) o;
    		String newText = "";

    		String text = (String) rev.get("text");
    		text = text.replaceAll("\\r\\n|\\r|\\n", " "); 	//Delete all the new lines etc. 
    		text = text.replaceAll("/", " ");  

    		//Create an empty Annotation just with the given text
    		Annotation document = new Annotation(text);

    		//Run all Annotators on this text
    		pipeline.annotate(document);

    		//All the sentences in this document
    		List<CoreMap> sentences = document.get(SentencesAnnotation.class);

    		for(CoreMap sentence: sentences) 
    		{
    		  //Traversing the words in the current sentence
    		  for (CoreLabel token: sentence.get(TokensAnnotation.class)) 
    		  {
//    		    //This is the text of the token
//    		    String word = token.get(TextAnnotation.class);
    			  
//    		    //This is the POS tag of the token
//    		    String pos = token.get(PartOfSpeechAnnotation.class);
    			  
//    		    //This is the NER label of the token
//    		    String ne = token.get(NamedEntityTagAnnotation.class);
    			  
    			//This is the lemma of the token  
    		    String lemma = token.getString(LemmaAnnotation.class);
    		    
    		    //If lemma contains any special signs etc.
    		    if (!Pattern.matches("[.,\\/#?!$%\\^&\\*;:{}=\\-_`'~()]+", lemma) && !lemma.equals("-lrb-") && !lemma.equals("-rrb-") && !lemma.equals("-lsb-") && !lemma.equals("-rsb-") && !lemma.equals("-lcb-") && !lemma.equals("-rcb-"))
    		    {
    		    		newText = newText + lemma.toLowerCase() + " ";
    		    }
    		  }

    		}
    		output.println(newText);
		}
	}
		
}
