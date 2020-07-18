# SASOBUW
The Semi-Automatic Sentiment Ontology Building Using Word embeddings (SASOBUW) framework for developing (semi-automatic sentiment domain) ontologies for ABSA with the use of word embeddings. 

## Key features
 * Optional synset extraction step
 * 3 different hierarchy building methods

## Installation instructions
1. Download WordNet and put it into SASOBUW\src\main\resources\externalData
2. Download the DataSASOBUW.zip folder from https://1drv.ms/u/s!Ao_FuxrvQWwphb5r5w3_Jny0PbHhnw?e=bePwpB
3. Put the contents of DataSASOBUW.zip into SASOBUW\src\main\resources\data

## Directory explanation
* SASOBUW package edu.eur.absa.Yelp
  * YelpReader.java - a class that finds the relevant (restaurant-related) reviews from the Yelp dataset
  * TxtForEmbeddings.java - a class which performs the necessary NLP steps (sentence splitting, tokenization, POS-tagging, lemmatization etc.) for the Yelp restaurant reviews and tips
* SASOBUW package edu.eur.absa.OntBuilding
  * MainOntBuilder.java - a class that creates an ontology and where the main features and approaches for the building process can be chosen (e.g. GloVe or fastText embeddings, the hierarchy building method etc.)
  * GridSearchParametersMain.java - a class where the parameter optimization can be done (e.g. the range and step size of the grid search can be chosen)
  * OntHelper.java - a class with methods to find terms, create concepts, build a hierarchy etc.
  * Ontology.java - a class with methods for the ontology (e.g. getting lexicalizations, adding classes or concepts etc.)

