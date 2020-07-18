package edu.eur.absa.OntBuilding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashSet;

import org.apache.jena.ontology.IntersectionClass;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.FileManager;
import edu.eur.absa.Framework;

/**
 * A class with all the methods for building an ontology
 * 
 * @author Ewelina Dera
 *
 */
public class Ontology
{
	//Needs to be changed for different domains
	public static final String namespace = "http://www.semanticweb.org/ewelina.dera/ontologies/RestaurantOntology";
	
	private OntModel ont = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);
	
	public Ontology(String OntFile) 
	{
		/* Use the FileManager to find the input file */
		InputStream in = FileManager.get().open(OntFile);
		if (in == null) {
			throw new IllegalArgumentException("File: " + OntFile + " not found");
		}
		/* Read the RDF/XML file */
		ont.read(in, null);
	}
	
	/***
	 * A method to create a class with the chosen parameters
	 * 
	 * @param classURI the name of the class
	 * @param lex true if the class has the lex property
	 * @param lexName the lexicalization for the lex property
	 * @param aspect true if the class has the aspect property
	 * @param aspects the aspect(s) for the aspect property
	 * @param parentURIs a list with the URIs of the class' parents
	 * @return the URI of the created class
	 */
	public String addClass(String classURI, boolean lex, String lexName, boolean aspect, HashSet<String> aspects, String... parentURIs ) 
	{
		String URI = namespace + "#" + classURI.replaceAll(" ", "");
		OntClass newClass = ont.createClass(URI);

		//Add the lex property
		if (lex) 
		{
			newClass.addProperty(ont.getProperty(namespace + "#lex"), lexName.toLowerCase());
		}

		//Add the given aspect properties
		if (aspect)
		{
			for (String asp : aspects)
			{
				newClass.addProperty(ont.getProperty(namespace + "#aspect"), asp);
			}
		}
		
		//Add the class' parents
		for (String parent : parentURIs) 
		{
			newClass.addSuperClass(ont.getResource(parent));
		}

		return newClass.getURI();
	}
	
	/***
	 * A method to create a class with the chosen parameters
	 * 
	 * @param classURI the name of the class
	 * @return the URI of the created class
	 */
	public String addClass(String classURI) 
	{
		String URI = namespace + "#" + classURI.replaceAll(" ", "");
		OntClass newClass = ont.createClass(URI);

		return newClass.getURI();
	}
	
	/***
	 * A method to create a class with the chosen parameters
	 * 
	 * @param classURI the name of the class
	 * @param parentURIs a list with the URIs of the class' parents
	 * @return the URI of the created class
	 */
	public String addClass(String classURI, String... parentURIs) 
	{
		String URI = namespace + "#" + classURI.replaceAll(" ", "");
		OntClass newClass = ont.createClass(URI);
		
		//Add the class' parents
		for (String parent : parentURIs) 
		{
			newClass.addSuperClass(ont.getResource(parent));
		}

		return newClass.getURI();
	}
	
	/***
	 * A method to create a class with the chosen parameters
	 * 
	 * @param lemma the name of the class
	 * @param lex true if the class has the lex property
	 * @param lexName the lexicalization for the lex property
	 * @param parentClasses a list with the URIs of the class' parents
	 * @return the URI of the created class
	 */
	public String addClass(String lemma, boolean lex, String lexName, String... parentClasses) 
	{
		String URI = namespace + "#" + lemma.substring(0, 1).toUpperCase() + lemma.substring(1).toLowerCase();
		OntClass newClass = ont.createClass(URI);
		
		//Add the lex property
		if (lex)
		{
			newClass.addProperty(ont.getProperty(namespace + "#lex"), lemma.toLowerCase());
		}
		
		//Add the class' parents
		for (String parentClass : parentClasses) 
		{
			newClass.addSuperClass(ont.getResource(namespace + "#" + parentClass));
		}
		
		return newClass.getURI();
	}
	
	/***
	 * A method to create a class with the chosen parameters
	 * 
	 * @param lemma the name of the class
	 * @param lex true if the class has the lex property
	 * @param lexName the lexicalization for the lex property
	 * @return the URI of the created class
	 */
	public String addClass(String lemma, boolean lex, String lexName) 
	{
		String URI = namespace + "#" + lemma.substring(0, 1).toUpperCase() + lemma.substring(1).toLowerCase();
		OntClass newClass = ont.createClass(URI);
		
		//Add the lex property
		if (lex)
		{
			newClass.addProperty(ont.getProperty(namespace + "#lex"), lemma.toLowerCase());
		}
		
		return newClass.getURI();
	}
	
	/***
	 * A method to save the ontology
	 * 
	 * @param ontologyFile the name of the file for the ontology
	 */
	public void save(String ontologyFile) 
	{
		try 
		{
			ont.write(new FileOutputStream(new File(Framework.EXTERNALDATA_PATH + ontologyFile)), "RDF/XML", null);
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		}
	}
	
	/***
	 * A method to add a lexicalization to a class
	 * 
	 * @param classURI class for which the lexicalization is to be added
	 * @param lex lexicalization to be added
	 */
	public void addLexicalization(String classURI, String lex) 
	{
		Resource concept = ont.getResource(classURI);
		concept.addProperty(ont.getProperty(namespace + "#lex"), lex.toLowerCase());
	}
	
	/***
	 * A method to get the lexicalization of a certain class
	 * 
	 * @param classURI class for which the lexicalization is to be obtained
	 * @return set of lexicalizations
	 */
	public HashSet<String> getLexicalizations(String classURI){
		return getObjects(classURI, namespace + "#lex");
	}
	
	/***
	 * A method to get the object (from the subject and predicate)
	 * 
	 * @param subjectURI
	 * @param predicateURI
	 * @return set of objects
	 */
	public HashSet<String> getObjects(String subjectURI, String predicateURI)
	{
		StmtIterator iter = ont.listStatements(new SimpleSelector(ont.getResource(subjectURI), ont.getProperty(predicateURI),(RDFNode)null));
		HashSet<String> targetTypes = new HashSet<String>();
		
		while (iter.hasNext()) 
		{
			Statement stmt = iter.nextStatement();
			RDFNode object = stmt.getObject();
			if (object.isResource())
			{
				targetTypes.add(object.asResource().getURI());
			} 
			else if (object.isLiteral())
			{
				targetTypes.add(object.asLiteral().getLexicalForm());
			}
		}
		
		targetTypes.remove(null);
		return targetTypes;
	}
	
	/***
	 * A method to check if the ontology contains a certain class
	 * 
	 * @param className class to be checked
	 * @return true if the class is in the ontology
	 */
	public boolean containsClass(String className)
	{
		if (ont.getResource(namespace + "#" + className.substring(0, 1).toUpperCase() + className.substring(1).toLowerCase()) != null)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/***
	 * A method to set a superclass for aspect concepts
	 * 
	 * @param parentClass the class which is to be set as the parent
	 * @param childClass the class which is to be set as a child
	 * @param aspectParentURI the aspect (attribute or category) parent class
	 */
	public void setSuperClassAspects(String parentClass, String childClass, String aspectParentURI)
	{
		Resource parent = ont.getResource(namespace + "#" + parentClass.substring(0, 1).toUpperCase() + parentClass.substring(1).toLowerCase());
		Resource aspectParent = ont.getResource(aspectParentURI);
		OntClass child = getOntClass(childClass);
		
		boolean condition = false;
		
		//If parentClass is aspectParentClass
		if(getLexicalizations(aspectParent.getURI()).contains(parentClass.toLowerCase()))
		{
			condition = true;
		}
		
		if (condition)
		{
			
		}
		else
		{
			child.addSuperClass(parent);
			child.removeSuperClass(aspectParent);
//			System.out.println("Added " + parent.getURI() + " as parent of " + child.getURI());
		}
	}
	
	/***
	 * A method to set a superclass for aspect concepts, when using the triples-based hierarchy approach
	 * 
	 * @param parentClass the class which is to be set as the parent
	 * @param childClass the class which is to be set as a child
	 * @param aspectParentURI the aspect (attribute or category) parent class
	 */
	public void setSuperClassAspectsTriples(String parentClass, String childClass, String aspectParentURI)
	{
		Resource parent = ont.getResource(namespace + "#" + parentClass.substring(0, 1).toUpperCase() + parentClass.substring(1).toLowerCase());	
		Resource aspectParent = ont.getResource(aspectParentURI);
		OntClass childOntClass = getOntClass(namespace + "#" + childClass.substring(0, 1).toUpperCase() + childClass.substring(1).toLowerCase());

		OntClass parentOntClass = null; 
		
		HashSet<String> lexicalizations = getLexicalizations(aspectParentURI);
		
		for (String lex : lexicalizations)
		{		
			if (lex.equals(parentClass))
			{
				parentOntClass = getOntClass(aspectParentURI);
			}
			else
			{
				parentOntClass = getOntClass(namespace + "#" + parentClass.substring(0, 1).toUpperCase() + parentClass.substring(1).toLowerCase());
			}
		}		
		
		boolean condition = false;
		
		//If parentClass is aspectParentURI
		if(getLexicalizations(aspectParent.getURI()).contains(parentClass.toLowerCase()))
		{
			condition = true;
		}
		
		if (condition)
		{
		}
		else
		{
			childOntClass.addSuperClass(parent);
			childOntClass.removeSuperClass(aspectParent);
//			System.out.println("Added " + parent.getURI() + " as parent of " + childOntClass.getURI());
		}		
	}
	
	/***
	 * A method to set a superclass for sentiment concepts
	 * 
	 * @param childClass the class which is to be set as a child
	 * @param positive true if the sentiment is positive; otherwise false
	 * @param pos part-of-speech tag
	 * @param parentURI the class which is to be set as a parent
	 */
	public void setSuperClassSentiments(String childClass, boolean positive, String pos, String parentURI)
	{				
		String temp = parentURI;
		
		if (pos.equals("verb") && positive)
		{
			temp = temp.replace("ActionMention", "");
			temp = temp + "PositiveAction";
			childClass = childClass.replace("-verb", "");
		}
		else if (pos.equals("verb") && !positive)
		{
			temp = temp.replace("ActionMention", "");
			temp = temp + "NegativeAction";
			childClass = childClass.replace("-verb", "");
		}
		else if (pos.equals("noun") && positive)
		{
			temp = temp.replace("EntityMention", "");
			temp = temp + "PositiveEntity";
			childClass = childClass.replace("-noun", "");
		}
		else if ((pos.equals("noun") && !positive))
		{
			temp = temp.replace("EntityMention", "");
			temp = temp + "NegativeEntity";
			childClass = childClass.replace("-noun", "");
		}
		else if (pos.equals("adj") && positive)
		{
			temp = temp.replace("PropertyMention", "");
			temp = temp + "PositiveProperty";
			childClass = childClass.replace("-adj", "");
		}
		else if (pos.equals("adj") && !positive)
		{
			temp = temp.replace("PropertyMention", "");
			temp = temp + "NegativeProperty";
			childClass = childClass.replace("-adj", "");
		}
		
		Resource parent = ont.getResource(temp);

		OntClass child = getOntClass(childClass);

		child.addSuperClass(parent);
//		System.out.println("Added " + parent.getURI() + " as parent of " + child.getURI());
	}
	
	/***
	 * A method to set a superclass for Type-3 sentiment concepts
	 * 
	 * @param parentClass the class which is to be set as a parent
	 * @param type3Sentiment the class which is to be set as a (Type-3) child
	 * @param positive true if the sentiment is positive; otherwise false
	 * @param pos part-of-speech tag
	 */
	public void setType3SuperClassSentiments(String parentClass, String type3Sentiment, boolean positive, String pos)
	{	
		String temp = "";
		String parent = parentClass.replace("-noun", "");	//Parent of a Type-3 sentiments is always a noun
		String type3Sent = type3Sentiment;
		
		if (pos.equals("verb"))
		{
			temp = "Action";
			type3Sent = type3Sent.replace("-verb", "");
		}
		else if (pos.equals("noun"))
		{
			temp = "Entity";
			type3Sent = type3Sent.replace("-noun", "");
		}
		else if (pos.equals("adj")) 
		{
			temp = "Property";
			type3Sent = type3Sent.replace("-adj", "");
		}
		
		String newSentimentClass = namespace + "#" + type3Sent.substring(0, 1).toUpperCase() + type3Sent.substring(1).toLowerCase() + parent.substring(0, 1).toUpperCase() + parent.substring(1).toLowerCase();
		parent = namespace + "#" + parent.substring(0, 1).toUpperCase() + parent.substring(1).toLowerCase();;
		
		OntClass newClass = ont.createClass(newSentimentClass);
		
		String superClass = "";
			
		if(positive)
		{
			superClass = namespace + "#" + "Positive";
		}
		else
		{
			superClass = namespace + "#" + "Negative";
		}
	
		OntClass sentiment = getOntClass(type3Sent);

		OntClass aspect = getOntClass(parent);

		IntersectionClass intersection = newClass.convertToIntersectionClass(ont.createList(new RDFNode[] {sentiment, aspect}));

		Resource sentimentSuperClass = ont.getResource(superClass);
		newClass.setSuperClass(sentimentSuperClass);

		Resource mention = ont.getResource(namespace + "#" + temp + "Mention");
		sentiment.addSuperClass(mention);

//		System.out.println("Added " + newSentimentClass + " as a child of " + sentimentSuperClass.getURI());
//		System.out.println("and " + type3Sent + " as a child of " + mention.getURI());
	}
	
	/***
	 * A method which returns the OntClass of the given class
	 * 
	 * @param className class for which the OntClass is to be returned
	 * @return
	 */
	public OntClass getOntClass(String className) 
	{
		OntClass temp = null;
		
		//If className is a URI
		if (className.contains("#"))
		{
			temp = ont.getOntClass(className);
		}
		else
		{
			temp = ont.getOntClass(namespace + "#" + className.substring(0, 1).toUpperCase() + className.substring(1).toLowerCase());
		}
		
		return temp;
	}

	/***
	 * A class which return the OntModel
	 * 
	 * @return OntModel
	 */
	public OntModel getOntModel()
	{
		return ont;
	}
	

}
