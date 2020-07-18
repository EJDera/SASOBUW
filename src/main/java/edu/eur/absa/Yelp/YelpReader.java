package edu.eur.absa.Yelp;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import edu.eur.absa.Framework;

/***
 * A class that filters all of the Yelp reviews/tips and outputs only the ones
 * which are related to restaurants.
 * 
 * @author Ewelina Dera
 *
 */
public class YelpReader 
{

	public YelpReader() 
	{
	}

    public static void main(String[] args) throws FileNotFoundException, IOException, ParseException
    {
    	Set<String> restaurant_ids = getRestaurantIDs();
    	
    	//Filter all the relevant tips
    	writeJSONRestaurantTips(restaurant_ids);    //985,558
    	
    	//Filter all the relevant reviews
    	//Due to huge amount of reviews and memory issues, the file had to be split into multiple smaller ones
    	Framework.log("Writing the first Yelp JSON file...");
        FileWriter file1 = new FileWriter(Framework.DATA_PATH + "Yelp_restaurant_reviews_part1.json");
           	
        System.out.println("Processing reviews - part 1");
        writeJSONRestaurantReviews(restaurant_ids, Framework.RAWDATA_PATH + "/Yelp - partial data/yelp_academic_dataset_review_part1.json", file1);    //688,712
        System.out.println("Processing reviews - part 2");
        writeJSONRestaurantReviews(restaurant_ids, Framework.RAWDATA_PATH + "/Yelp - partial data/yelp_academic_dataset_review_part2.json", file1);    //691,280
        
        file1.close();
        Framework.log("Writing the second Yelp JSON file...");
        FileWriter file2 = new FileWriter(Framework.DATA_PATH + "Yelp_restaurant_reviews_part2.json");
        
        System.out.println("Processing reviews - part 3");
        writeJSONRestaurantReviews(restaurant_ids, Framework.RAWDATA_PATH + "/Yelp - partial data/yelp_academic_dataset_review_part3.json", file2);    //688,704
        System.out.println("Processing reviews - part 4");
        writeJSONRestaurantReviews(restaurant_ids, Framework.RAWDATA_PATH + "/Yelp - partial data/yelp_academic_dataset_review_part4.json", file2);    //683,847
        
        file2.close();
        Framework.log("Writing the third Yelp JSON file...");
        FileWriter file3 = new FileWriter(Framework.DATA_PATH + "Yelp_restaurant_reviews_part3.json");
        
        System.out.println("Processing reviews - part 5");
        writeJSONRestaurantReviews(restaurant_ids, Framework.RAWDATA_PATH + "/Yelp - partial data/yelp_academic_dataset_review_part5.json", file3);    //679,981
        System.out.println("Processing reviews - part 6");
        writeJSONRestaurantReviews(restaurant_ids, Framework.RAWDATA_PATH + "/Yelp - partial data/yelp_academic_dataset_review_part6.json", file3);    //693,566
        
        file3.close();
        Framework.log("Writing the fourth Yelp JSON file...");
        FileWriter file4 = new FileWriter(Framework.DATA_PATH+"Yelp_restaurant_reviews_part4.json");
        
        System.out.println("Processing reviews - part 7");
        writeJSONRestaurantReviews(restaurant_ids, Framework.RAWDATA_PATH + "/Yelp - partial data/yelp_academic_dataset_review_part7.json", file4);    //693,287
        System.out.println("Processing reviews - part 8");
        writeJSONRestaurantReviews(restaurant_ids, Framework.RAWDATA_PATH + "/Yelp - partial data/yelp_academic_dataset_review_part8.json", file4);    //689,966
    	
    	file4.close();        
    	
    	//In total, 5,509,343 reviews + 985,558 tips = 6,494,901 texts
    }

    /***
     * A method which finds the IDs of businesses that contain "Restaurant" or "Food" in their category description
     * 	
     * @return set with the relevant business IDs
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ParseException
     */
    public static Set<String> getRestaurantIDs() throws FileNotFoundException, IOException, ParseException
    {
    	JSONParser parser = new JSONParser();
    	JSONArray jsonArray = (JSONArray) parser.parse(new FileReader(Framework.RAWDATA_PATH+"yelp_academic_dataset_business.json"));
   	
    	Set<String> restaurant_ids = new HashSet<>();	//List with all the relevant business IDs
    	
    	//For each business
    	for (Object o : jsonArray) 
    	{
    		JSONObject business = (JSONObject) o;

    		String category = (String) business.get("categories");
//    		System.out.println("Category::::" + category);
    		
    		String business_id = (String) business.get("business_id");
//    		System.out.println("Business id::::" + business_id);
    		
    		if(category != null) 
    		{
	    		if(category.contains("Restaurants") || category.contains("Food")) 
	    		{	    			
	    			restaurant_ids.add(business_id);
	    		}
    		}
    	}
    	
//    	System.out.println("Total number of restaurant or food businesses is " + restaurant_ids.size());
    	
    	return restaurant_ids;
    }
    
    /***
     * A method which writes to a file the Yelp reviews related only to the chosen businesses
     * 
     * @param restaurant_ids set of business IDs for which reviews are to be extracted
     * @param input_path path to file where all the reviews (which are to be filtered) are
     * @param file output file where only the relevant reviews will be saved
     * @throws IOException
     * @throws ParseException
     */
    public static void writeJSONRestaurantReviews(Set<String> restaurant_ids, String input_path, FileWriter file) throws IOException, ParseException
    {
    	JSONParser parser = new JSONParser();
    	JSONArray jsonArrayReviews = (JSONArray) parser.parse(new FileReader(input_path));
    	    	
        int counter = 0;
        
        //For each review
    	for (Object o : jsonArrayReviews) 
    	{
    		JSONObject business = (JSONObject) o;

    		String review_id = (String) business.get("review_id");
    		String business_id = (String) business.get("business_id");
    		double stars = (double) business.get("stars");
    		String text = (String) business.get("text");
    		
    		if(restaurant_ids.contains(business_id)) 
    		{
        		counter++;
//        		System.out.println(counter);
    			
                JSONObject obj = new JSONObject();
                obj.put("review_id", review_id);
                obj.put("text", text);
                obj.put("stars", stars);
                obj.put("business_id", business_id);
                
                file.write(obj.toJSONString());

    		} 		
    	}
    	
    	System.out.println("In total there are " + counter + " restaurant reviews");
  	
    }
    
       
    /***
     * A method which writes to a file the Yelp tips related only to the chosen businesses
     * 
     * @param restaurant_ids set of business IDs for which reviews are to be extracted
     * @throws IOException
     * @throws ParseException
     */
    public static void writeJSONRestaurantTips(Set<String> restaurant_ids) throws IOException, ParseException
    {
    	JSONParser parser = new JSONParser();
    	JSONArray jsonArrayTips = (JSONArray) parser.parse(new FileReader(Framework.RAWDATA_PATH+"yelp_academic_dataset_tip.json"));
    	
    	//Output file
        FileWriter file = new FileWriter(Framework.DATA_PATH+"Yelp_restaurant_tips.json");
        
        int counter = 0;
    	    	
        //For each tip
    	for (Object o : jsonArrayTips) 
    	{
    		JSONObject business = (JSONObject) o;

    		String business_id = (String) business.get("business_id");
    		String text = (String) business.get("text");
    		
    		if(restaurant_ids.contains(business_id)) 
    		{
        		counter++;
//        		System.out.println(counter);
        		
                JSONObject obj = new JSONObject();
                obj.put("business_id", business_id);
                obj.put("text", text);
                
                file.write(obj.toJSONString());
    		} 		
    	}
    	
    	file.close();
    	System.out.println("In total there are " + counter + " restaurant tips");
    }

}
